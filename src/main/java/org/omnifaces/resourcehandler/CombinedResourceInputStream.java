/*
 * Copyright 2012 OmniFaces.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.resourcehandler;

import static org.omnifaces.util.Faces.getRequestDomainURL;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.Resource;
import javax.faces.context.FacesContext;

import org.omnifaces.component.output.cache.CacheFactory;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Utils;

/**
 * This {@link InputStream} implementation takes care that all in the constructor given resources are been read in sequence.
 * 
 * @author Bauke Scholtz
 */
final class CombinedResourceInputStream extends InputStream {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final String TIME_TO_LIVE_CONTEXT_PARAM = "org.omnifaces.CACHE_SETTING_APPLICATION_TTL";
    private final static String DEFAULT_SCOPE = "application";

    /* 16.02.2015 Caching added by Stephan Rauh, http://www.beyondjava.net */
    /** The context parameter name to specify whether the resources are to be cached or not. */
    public static final String PARAM_NAME_ACTIVATE_RESOURCE_CACHING = "org.omnifaces.COMBINED_RESOURCE_ACTIVATE_RESOURCE_CACHING";
    /* 16.02.2015 end of modification */

    private static final byte[] CRLF = { '\r', '\n' };

    // Properties -----------------------------------------------------------------------------------------------------

    private List<InputStream> streams;
    private Iterator<InputStream> streamIterator;
    private InputStream currentStream;

    static Map<String, byte[]> cachedResources = new HashMap<String, byte[]>();

    /* 16.02.2015 Caching added by Stephan Rauh, http://www.beyondjava.net */
    private byte[] combinedResource = null;
    private int pointer = 0;
    /* 16.02.2015 end of pull request */

    // Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Creates an instance of {@link CombinedResourceInputStream} based on the given resources. For each resource, the {@link InputStream}
     * will be obtained and hold in an iterable collection.
     * 
     * @param resources
     *            The resources to be read.
     * @throws IOException
     *             If something fails at I/O level.
     */
    public CombinedResourceInputStream(Set<Resource> resources) throws IOException {
        prepareStreaming(resources);

        /* 16.02.2015 Caching added by Stephan Rauh, http://www.beyondjava.net */
        if ("true".equals(Faces.getInitParameter(PARAM_NAME_ACTIVATE_RESOURCE_CACHING))) {
            combinedResource = prepareStreamingFromCache(streamIterator, resources);
            pointer = 0;
            currentStream = null;
        }
        else
        {
            streamIterator.hasNext(); // We assume it to be always true, see also CombinedResource#getInputStream().
            currentStream = streamIterator.next();
        }
        /* 16.02.2015 end of pull request */
    }

    /**
     * Collects the list of Stream that have to be read.
     * @param resources The resources to be read.
     * @throws IOException If something fails at I/O level.
     */
    private void prepareStreaming(Set<Resource> resources) throws IOException {
        streams = new ArrayList<>();
        String domainURL = getRequestDomainURL();

        for (Resource resource : resources) {
            InputStream stream;

            try {
                stream = resource.getInputStream();
            } catch (Exception richFacesDoesNotSupportThis) {
                stream = new URL(domainURL + resource.getRequestPath()).openStream();
            }

            streams.add(stream);
            streams.add(new ByteArrayInputStream(CRLF));
        }

        streamIterator = streams.iterator();
    }

    /* 16.02.2015 Caching added by Stephan Rauh, http://www.beyondjava.net */
    // Eclipse doesn't detect that the resource are closed in the close() method
    /**
     * This method collects the resources eagerly and combines them into a byte array. The byte array is cached.
     * @param streamIterator The stream iterator iterates over the resources to be read.
     * @param resources The resources to be read.
     * @return a byte array containing the combined resources. Can't be null.
     * @throws IOException
            If something fails at I/O level.
     */
    @SuppressWarnings("resource")
    private static byte[] prepareStreamingFromCache(Iterator<InputStream> streamIterator, Set<Resource> resources)
            throws IOException {
        String key = "";

        for (Resource resource : resources) {
            key += resource.getLibraryName() + "/" + resource.getResourceName() + " ";
        }

        org.omnifaces.component.output.cache.Cache scopedCache = CacheFactory.getCache(FacesContext.getCurrentInstance(), DEFAULT_SCOPE);

        byte[] _combinedResource;
        synchronized(CombinedResourceHandler.class){
            _combinedResource = (byte[]) scopedCache.getObject(key);
        }
        
        if (null != _combinedResource) {
            return _combinedResource;
        }

        streamIterator.hasNext(); // We assume it to be always true, see also CombinedResource#getInputStream().
        InputStream currentStream = streamIterator.next();
        // Caching added by Stephan Rauh, www.beyondjava.net, Feb 02, 2015
        if (null == _combinedResource) {
            ByteArrayOutputStream collector = new ByteArrayOutputStream();
            int read = -1;

            while (true) {
                read = currentStream.read();
                if (read == -1) {
                    if (streamIterator.hasNext()) {
                        currentStream = streamIterator.next();
                    } else {
                        break;
                    }
                } else
                    collector.write(read);
            }
            _combinedResource = collector.toByteArray();
            synchronized(CombinedResourceHandler.class){
                if (null==scopedCache.getObject(key))
                    scopedCache.putObject(key, _combinedResource, getTimeToLiveOfCacheEntries());
            }
        }
        return _combinedResource;
    }

    /**
     * How many seconds are cache entries supposed to live in the cache? Default: 1 hour.
     * @return the number of seconds
     */
    private static int getTimeToLiveOfCacheEntries() {
        int timeToLive=3600; // one hour by default
        
        String ttl = Faces.getInitParameter(TIME_TO_LIVE_CONTEXT_PARAM);
        if (null !=ttl) {
            try {
                timeToLive=Integer.parseInt(ttl);
            }
            catch (Exception weirdEntry) {
                // this error has already been reported on startup, so we can safely ignore it here
            }
        
        }
        return timeToLive;
    }
    /* 16.02.2015 end of pull request */

    // Actions --------------------------------------------------------------------------------------------------------

    // Caching added by Stephan Rauh, www.beyondjava.net, Feb 02, 2015
    /**
     * For each resource, read until its {@link InputStream#read()} returns <code>-1</code> and then iterate to the {@link InputStream} of
     * the next resource, if any available, else return <code>-1</code>.
     */
    @Override
    public int read() throws IOException {
        // Hint on performance: this method will run "hot", i.e. will be inlined and compiled to assembler code by the JIT. So introducing
        // the additional methods doesn't
        // reduce performance.
        if (null != combinedResource)
            return readFromCache();
        else
            return readFromStreamIterator();
    }

    /**
     * For each resource, read until its {@link InputStream#read()} returns <code>-1</code> and then iterate to the {@link InputStream} of
     * the next resource, if any available, else return <code>-1</code>.
     */
    public int readFromStreamIterator() throws IOException {
        int read = -1;

        while ((read = currentStream.read()) == -1) {
            if (streamIterator.hasNext()) {
                currentStream = streamIterator.next();
            } else {
                break;
            }
        }

        return read;
    }

    public int readFromCache() throws IOException {
        // Caching added by Stephan Rauh, www.beyondjava.net, Feb 02, 2015
        if (pointer < combinedResource.length) {
            return combinedResource[pointer++];
        } else {
            return -1;
        }
    }

    /* 16.02.2015 end of pull request */

    /**
     * Closes the {@link InputStream} of each resource. Whenever the {@link InputStream#close()} throws an {@link IOException} for the first
     * time, it will be caught and be thrown after all resources have been closed. Any {@link IOException} which is thrown by a subsequent
     * close will be ignored by design.
     */
    @Override
    public void close() throws IOException {
        IOException caught = null;

        for (InputStream stream : streams) {
            IOException e = Utils.close(stream);

            if (caught == null) {
                caught = e; // Don't throw it yet. We have to continue closing all other streams.
            }
        }

        if (caught != null) {
            throw caught;
        }
    }
}