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
package org.omnifaces.servlet;

import static java.lang.Boolean.FALSE;
import static java.util.Arrays.stream;
import static java.util.Collections.list;
import static java.util.Optional.ofNullable;
import static org.omnifaces.util.Reflection.invokeStaticMethod;
import static org.omnifaces.util.Reflection.toClassOrNull;
import static org.omnifaces.util.Utils.isOneOf;
import static org.omnifaces.util.Utils.splitAndTrim;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.omnifaces.io.ResettableBufferedOutputStream;

/**
 * This HTTP servlet response wrapper will compress the response with the given algorithm when the given threshold has
 * exceeded and the response content type matches one of the given mimetypes.
 *
 * @author Bauke Scholtz
 * @since 4.5
 */
public class CompressedHttpServletResponse extends HttpServletResponseOutputWrapper {

    // Public constants -----------------------------------------------------------------------------------------------

    /**
     * Available compression algorithms.
     */
    @SuppressWarnings("unchecked")
    public enum Algorithm {

        /**
         * It will use one of the following Brotli compression output streams, whichever is available first:
         * <ul>
         * <li>{@code com.aayushatharva.brotli4j.encoder.BrotliOutputStream}</li>
         * <li>{@code com.nixxcode.jvmbrotli.enc.BrotliOutputStream}</li>
         * </ul>
         */
        BROTLI("br",
            load("com.aayushatharva.brotli4j.Brotli4jLoader#ensureAvailability", "com.aayushatharva.brotli4j.encoder.BrotliOutputStream"),
            load("com.nixxcode.jvmbrotli.common.BrotliLoader#isBrotliAvailable", "com.nixxcode.jvmbrotli.enc.BrotliOutputStream")
        ),

        /**
         * It will use {@link GZIPOutputStream} as compression output stream.
         */
        GZIP("gzip", GZIPOutputStream.class),

        /**
         * It will use {@link DeflaterOutputStream} as compression output stream.
         */
        DEFLATE("deflate", DeflaterOutputStream.class);

        private final String encodingDirective;
        private final Optional<Class<? extends OutputStream>> outputStreamClass;

        Algorithm(String encodingDirective, Class<? extends OutputStream>... outputStreamClasses) {
            this.encodingDirective = encodingDirective;
            this.outputStreamClass = stream(outputStreamClasses).filter(Objects::nonNull).findFirst();
        }

        /**
         * Returns the encoding directive. This basically represents the unique identifier of the algorithm in the HTTP
         * {@code Accept-Encoding} header as well as the HTTP {@code Content-Encoding} header.
         * @return The directive.
         */
        public String getEncodingDirective() {
            return encodingDirective;
        }

        /**
         * Returns the output stream class being used.
         * @return The output stream class being used.
         */
        public Class<? extends OutputStream> getOutputStreamClass() {
            return outputStreamClass.get();
        }

        /**
         * Returns {@code true} if this algorithm is available.
         * @return {@code true} if this algorithm is available.
         */
        public boolean isAvailable() {
            return outputStreamClass.isPresent();
        }

        /**
         * Returns {@code true} if the given request accepts this algorithm.
         * @param request The involved HTTP servlet request.
         * @return {@code true} if the given request accepts this algorithm.
         */
        public boolean accepts(HttpServletRequest request) {
            return isAvailable() && list(request.getHeaders("Accept-Encoding")).stream()
                    .flatMap(value -> splitAndTrim(value, ","))
                    .anyMatch(encodingDirective::equals);
        }

        /**
         * Returns an output stream which is compressed using this algorithm for the given HTTP servlet response.
         * @param response The HTTP servlet response to be compressed with this algorithm.
         * @return An output stream which is compressed using this algorithm.
         * @throws UnsupportedOperationException When the output stream cannot be constructed for some reason.
         */
        public OutputStream createOutputStream(HttpServletResponse response) {
            try {
                return getOutputStreamClass().getConstructor(OutputStream.class).newInstance(response.getOutputStream());
            } catch (Exception e) {
                throw new UnsupportedOperationException(e);
            }
        }

        /**
         * Returns the best algorithm matching the given HTTP servlet request.
         * @param request The HTTP servlet request to find the best algorithm for.
         * @return The best algorithm matching the given HTTP servlet request.
         */
        public static Optional<Algorithm> find(HttpServletRequest request) {
            return stream(values()).filter(algorithm -> algorithm.accepts(request)).findFirst();
        }

        @SuppressWarnings("rawtypes")
        private static Class load(String loaderSignature, String outputStreamClassName) {
            if (loaderSignature == null) {
                return toClassOrNull(outputStreamClassName);
            }

            var loader = loaderSignature.split("#");
            return ofNullable(toClassOrNull(loader[0]))
                    .filter(loaderClass -> invokeStaticMethod(loaderClass, loader[1]) != FALSE)
                    .map($ -> toClassOrNull(outputStreamClassName)).orElse(null);
        }
    }

    // Properties -----------------------------------------------------------------------------------------------------

    private Algorithm algorithm;
    private int threshold;
    private Set<String> mimetypes;
    private long contentLength;
    private String vary;
    private boolean dontCompress;
    private boolean closing;
    private CompressThresholdOutputStream output;

    // Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Construct a new compressed HTTP servlet response based on the given response, algorithm, threshold and mimetypes.
     * @param response The HTTP servlet response.
     * @param algorithm The compression algorithm to use.
     * @param threshold The compression buffer threshold.
     * @param mimetypes The mimetypes which needs to be compressed.
     */
    public CompressedHttpServletResponse(HttpServletResponse response, Algorithm algorithm, int threshold, Set<String> mimetypes) {
        super(response);
        this.algorithm = algorithm;
        this.threshold = threshold;
        this.mimetypes = mimetypes;
    }

    // Actions --------------------------------------------------------------------------------------------------------

    @Override
    public void setContentLength(int contentLength) {
        setContentLengthLong(contentLength);
    }

    @Override
    public void setContentLengthLong(long contentLength) {
        // Get hold of content length locally to avoid it from being set on responses which will actually be compressed.
        this.contentLength = contentLength;
    }

    @Override
    public void setHeader(String name, String value) {
        super.setHeader(name, value);

        if (name != null) {
            var lowerCasedName = name.toLowerCase();

            if ("vary".equals(lowerCasedName)) {
                vary = value;
            }
            else if ("content-range".equals(lowerCasedName)) {
                dontCompress = value != null;
            }
            else if ("cache-control".equals(lowerCasedName)) {
                dontCompress = value != null && isCacheControlNoTransform(value);
            }
        }
    }

    @Override
    public void addHeader(String name, String value) {
        super.addHeader(name, value);

        if (name != null && value != null) {
            var lowerCasedName = name.toLowerCase();

            if ("vary".equals(lowerCasedName)) {
                vary = (vary != null ? vary + "," : "") + value;
            }
            else if ("content-range".equals(lowerCasedName)) {
                dontCompress = true;
            }
            else if ("cache-control".equals(lowerCasedName)) {
                dontCompress = dontCompress || isCacheControlNoTransform(value);
            }
        }
    }

    private static boolean isCacheControlNoTransform(String value) {
        return splitAndTrim(value.toLowerCase(), ",").anyMatch("no-transform"::equals);
    }

    @Override
    public void flushBuffer() throws IOException {
        if (isCommitted()) {
            super.flushBuffer();
        }
    }

    @Override
    public void reset() {
        super.reset();

        if (!isCommitted()) {
            contentLength = 0;
            vary = null;
            dontCompress = false;

            if (output != null) {
                output.reset();
            }
        }
    }

    @Override
    public void close() throws IOException {
        closing = true;
        super.close();
        closing = false;
    }

    @Override
    protected OutputStream createOutputStream() {
        output = new CompressThresholdOutputStream(threshold);
        return output;
    }

    // Inner classes --------------------------------------------------------------------------------------------------

    /**
     * This output stream will switch to HTTP response compression when the given threshold is exceeded.
     * <p>
     * This is an inner class because it needs to be able to manipulate the response headers once the decision whether
     * to compress or not has been made.
     *
     * @author Bauke Scholtz
     */
    private class CompressThresholdOutputStream extends ResettableBufferedOutputStream {

        // Constructors -----------------------------------------------------------------------------------------------

        public CompressThresholdOutputStream(int threshold) {
            super(threshold);
        }

        // Actions ----------------------------------------------------------------------------------------------------

        /**
         * Create compressed output stream if necessary. That is, when the given <code>doCompress</code> argument is
         * <code>true</code>, the current response does not have the <code>Cache-Control: no-transform</code> or
         * <code>Content-Range</code> headers, the current response is not committed, the content type is not
         * <code>null</code> and the content type matches one of the mimetypes.
         */
        @Override
        public OutputStream createOutputStream(boolean doCompress) throws IOException {
            var originalResponse = (HttpServletResponse) getResponse();

            if (doCompress && !dontCompress && (closing || !isCommitted())) {
                var contentType = getContentType();

                if (contentType != null && mimetypes.contains(contentType.split(";", 2)[0])) {
                    addHeader("Content-Encoding", algorithm.getEncodingDirective());
                    setHeader("Vary", (!isOneOf(vary, null, "*") ? vary + "," : "") + "Accept-Encoding");
                    return algorithm.createOutputStream(originalResponse);
                }
            }

            if (!doCompress) {
                setContentLength(getWrittenBytes());
            }

            if (contentLength > 0) {
                originalResponse.setHeader("Content-Length", String.valueOf(contentLength));
            }

            return originalResponse.getOutputStream();
        }
    }
}