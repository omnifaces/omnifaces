/*
 * Copyright OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.resourcehandler;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.logging.Level.FINEST;
import static org.omnifaces.util.Faces.getRequestDomainURL;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.faces.application.Resource;

import org.omnifaces.util.Utils;

/**
 * <p>
 * This {@link InputStream} implementation takes care that all in the constructor given resources are been read in
 * sequence.
 *
 * @author Bauke Scholtz
 */
public final class CombinedResourceInputStream extends InputStream {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final Logger logger = Logger.getLogger(CombinedResourceInputStream.class.getName());

    private static final byte[] CRLF = { '\r', '\n' };

    // Properties -----------------------------------------------------------------------------------------------------

    private List<InputStream> streams;
    private Iterator<InputStream> streamIterator;
    private InputStream currentStream;
    private final boolean script;
    private boolean preamble;
    private byte[] pendingOutput;
    private int pendingOutputOffset;

    // Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Creates an instance of {@link CombinedResourceInputStream} based on the given resources. For each resource, the
     * {@link InputStream} will be obtained and hold in an iterable collection.
     * @param resources The resources to be read.
     * @param contentType The content type of the combined resource; must not be <code>null</code>. When this is a
     * JavaScript type (ending in <code>/javascript</code>), <code>"use strict"</code> directives will be stripped from
     * the preamble of each resource to prevent them from ending up in the middle of the combined output.
     * @throws NullPointerException When content type is <code>null</code>.
     * @throws IOException If something fails at I/O level.
     */
    public CombinedResourceInputStream(Set<Resource> resources, String contentType) throws IOException {
        script = requireNonNull(contentType, "contentType").endsWith("/javascript");
        streams = new ArrayList<>();
        String domainURL = getRequestDomainURL();

        for (var resource : resources) {
            InputStream stream;

            try {
                stream = resource.getInputStream();
            }
            catch (Exception richFacesDoesNotSupportThis) {
                logger.log(FINEST, "Ignoring thrown exception; this can only be caused by a buggy component library.", richFacesDoesNotSupportThis);
                stream = new URL(domainURL + resource.getRequestPath()).openStream();
            }

            streams.add(stream);
            streams.add(new ByteArrayInputStream(CRLF));
        }

        streamIterator = streams.iterator();
        advanceStream();
    }

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * For each resource, read until its {@link InputStream#read()} returns <code>-1</code> and then iterate to the
     * {@link InputStream} of the next resource, if any available, else return <code>-1</code>.
     */
    @Override
    public int read() throws IOException {
        if (pendingOutput != null && pendingOutputOffset < pendingOutput.length) {
            return pendingOutput[pendingOutputOffset++] & 0xFF;
        }

        pendingOutput = null;

        if (preamble) {
            int preambleRead = readPreamble();

            if (preambleRead != -1) {
                return preambleRead;
            }
        }

        int read;

        while ((read = currentStream.read()) == -1) {
            if (!advanceStream()) {
                break;
            }

            if (preamble) {
                int preambleRead = readPreamble();

                if (preambleRead != -1) {
                    return preambleRead;
                }
            }
        }

        return read;
    }

    /**
     * For each resource, read until its {@link InputStream#read()} returns <code>-1</code> and then iterate to the
     * {@link InputStream} of the next resource, if any available, else return <code>-1</code>.
     */
    @Override
    public int read(byte[] b, int offset, int length) throws IOException {
        if (preamble || (pendingOutput != null && pendingOutputOffset < pendingOutput.length)) {
            // Delegate to single-byte read during preamble scanning (only a handful of bytes).
            int singleByte = read();

            if (singleByte == -1) {
                return -1;
            }

            b[offset] = (byte) singleByte;
            int count = 1;

            while (count < length && (preamble || (pendingOutput != null && pendingOutputOffset < pendingOutput.length))) {
                singleByte = read();

                if (singleByte == -1) {
                    break;
                }

                b[offset + count++] = (byte) singleByte;
            }

            return count;
        }

        int read;

        while ((read = currentStream.read(b, offset, length)) == -1) {
            if (!advanceStream()) {
                break;
            }

            if (preamble) {
                return read(b, offset, length);
            }
        }

        return read;
    }

    /**
     * Closes the {@link InputStream} of each resource. Whenever the {@link InputStream#close()} throws an
     * {@link IOException} for the first time, it will be caught and be thrown after all resources have been closed.
     * Any {@link IOException} which is thrown by a subsequent close will be ignored by design.
     */
    @Override
    public void close() throws IOException {
        IOException caught = null;

        for (var stream : streams) {
            IOException e = Utils.close(stream);

            if (caught == null) {
                caught = e; // Don't throw it yet. We have to continue closing all other streams.
            }
        }

        if (caught != null) {
            throw caught;
        }
    }

    /**
     * Advances to the next stream in the iterator. When advancing to a JS resource stream (not a CRLF separator),
     * enables preamble scanning mode for <code>"use strict"</code> directive stripping.
     * @return <code>true</code> if there was a next stream to advance to, <code>false</code> otherwise.
     */
    private boolean advanceStream() {
        if (!streamIterator.hasNext()) {
            return false;
        }

        currentStream = streamIterator.next();

        if (script && !(currentStream instanceof ByteArrayInputStream)) {
            currentStream = new BufferedInputStream(currentStream);
            preamble = true;
        }

        return true;
    }

    /**
     * Scans the preamble of a JS resource stream for a <code>"use strict"</code> directive and strips it if found.
     * Non-quote bytes (whitespace, comment characters) pass through directly. When a quote is found, the content up to
     * the matching close quote is buffered and checked. If it's <code>"use strict"</code>, the directive and the rest
     * of the line are consumed. Otherwise, the buffered content is queued as pending output.
     * @return The next byte to output, or <code>-1</code> if the stream is exhausted.
     * @throws IOException If something fails at I/O level.
     */
    private int readPreamble() throws IOException {
        while (true) {
            int b = currentStream.read();

            if (b == -1) {
                preamble = false;
                return -1;
            }

            if (b != '\'' && b != '"') {
                return b;
            }

            var buf = new ByteArrayOutputStream();
            int c;

            while ((c = currentStream.read()) != -1 && c != b) {
                buf.write(c);
            }

            preamble = false;

            if ("use strict".equals(buf.toString(UTF_8))) {
                int next;

                while ((next = currentStream.read()) != -1) {
                    if (next == '\n') {
                        break;
                    }

                    if (next != ';' && next != ' ' && next != '\t' && next != '\r') {
                        return next; // Stop: real code follows on the same line.
                    }
                }

                return currentStream.read();
            }

            byte[] content = buf.toByteArray();
            pendingOutput = new byte[content.length + (c == -1 ? 1 : 2)];
            pendingOutput[0] = (byte) b;
            System.arraycopy(content, 0, pendingOutput, 1, content.length);

            if (c != -1) {
                pendingOutput[content.length + 1] = (byte) c;
            }

            pendingOutputOffset = 0;
            return pendingOutput[pendingOutputOffset++] & 0xFF;
        }
    }

}