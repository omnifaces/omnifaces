/*
 * Copyright 2019 OmniFaces
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

import java.io.Externalizable;

import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;

/**
 * This {@link Resource} implementation can be used as a marker class to signal the custom {@link ResourceHandler}
 * such as {@link CombinedResourceHandler} that the given resource actually returns a CDN URL, and offers a method to
 * return the local URL which can be used as fallback in case the CDN request errors out.
 *
 * @author Bauke Scholtz
 * @since 2.7
 */
public class CDNResource extends RemappedResource {

	/**
	 * Do not use this constructor. It's merely there for {@link Externalizable}.
	 */
	public CDNResource() {
		// Keep default c'tor alive for Externalizable.
	}

	/**
	 * Constructs a new CDN resource which remaps the given wrapped resource to the given CDN URL.
	 * The CDN URL is available by {@link #getRequestPath()}.
	 * The local URL is available by {@link #getLocalRequestPath()}.
	 * @param resource The resource to be remapped.
	 * @param cdnURL The CDN URL of the resource.
	 */
	public CDNResource(Resource resource, String cdnURL) {
		super(resource, cdnURL);
	}

	/**
	 * Returns the CDN URL. I.e. the remapped request path pointing a CDN host.
	 * @return The CDN URL.
	 */
	@Override
	public String getRequestPath() {
		return super.getRequestPath();
	}

	/**
	 * Returns the local URL. I.e. the original request path pointing the local host.
	 * @return The local URL.
	 */
	public String getLocalRequestPath() {
		Resource wrapped = getWrapped();
		return wrapped != null ? wrapped.getRequestPath() : null;
	}

}