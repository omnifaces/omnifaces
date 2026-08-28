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

import java.io.Externalizable;

import jakarta.faces.application.Resource;
import jakarta.faces.application.ResourceHandler;

import org.omnifaces.renderer.CorsAwareResourceRenderer;

/**
 * <p>
 * This {@link Resource} implementation is a marker class which signals that the given resource is served from a CDN host. It offers a method to return the
 * local URL which can be used as fallback in case the CDN request errors out.
 * <p>
 * It is intended to be returned by a {@link ResourceHandler} which you write yourself and which uploads your own resources to your own CDN host, so that the
 * CDN content stays byte for byte identical to the local content. The {@link CombinedResourceHandler} will then render an <code>onerror</code> handler which
 * falls back to the local URL, and the {@link CorsAwareResourceRenderer} will then render the <code>integrity</code> attribute.
 * <p>
 * The {@link CDNResourceHandler} does not return this type, it returns a {@link RemappedResource}. Its CDN URLs point to third party hosts whose content is not
 * byte for byte identical to the local content, and an integrity hash computed from the local content would therefore cause the browser to block the resource.
 *
 * @author Bauke Scholtz
 * @since 2.7
 * @see CDNResourceHandler
 * @see CombinedResourceHandler
 * @see CorsAwareResourceRenderer
 */
public class CDNResource extends RemappedResource {

    /**
     * Do not use this constructor. It's merely there for {@link Externalizable}.
     */
    public CDNResource() {
        // Keep default c'tor alive for Externalizable.
    }

    /**
     * Constructs a new CDN resource which remaps the given wrapped resource to the given CDN URL. The CDN URL is available by {@link #getRequestPath()}. The
     * local URL is available by {@link #getLocalRequestPath()}.
     *
     * @param resource The resource to be remapped.
     * @param cdnURL The CDN URL of the resource.
     */
    public CDNResource(Resource resource, String cdnURL) {
        super(resource, cdnURL);
    }

    /**
     * Returns the CDN URL. I.e. the remapped request path pointing a CDN host.
     *
     * @return The CDN URL.
     */
    @Override
    public String getRequestPath() {
        return super.getRequestPath();
    }

    /**
     * Returns the local URL. I.e. the original request path pointing the local host.
     *
     * @return The local URL.
     */
    public String getLocalRequestPath() {
        var wrapped = getWrapped();
        return wrapped != null ? wrapped.getRequestPath() : null;
    }

}
