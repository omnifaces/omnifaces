/*
 * Copyright 2016 OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnifaces.resourcehandler;

import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.faces.application.ResourceHandler.RESOURCE_IDENTIFIER;
import static org.omnifaces.util.Faces.getMapping;
import static org.omnifaces.util.Faces.getRequestContextPath;
import static org.omnifaces.util.Faces.getRequestDomainURL;
import static org.omnifaces.util.Faces.isPrefixMapping;
import static org.omnifaces.util.Utils.formatRFC1123;
import static org.omnifaces.util.Utils.parseRFC1123;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.faces.application.Resource;
import javax.faces.context.FacesContext;

import org.omnifaces.util.Hacks;

/**
 * This {@link Resource} implementation represents a cacheable dynamic resource which doesn't necessarily exist as a
 * regular classpath resource.
 *
 * @author Bauke Scholtz
 * @since 2.0
 */
public abstract class DynamicResource extends Resource {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final int RESPONSE_HEADERS_SIZE = 4;

	// Properties -----------------------------------------------------------------------------------------------------

	private long lastModified;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Constructs a new dynamic resource based on the given resource name, library name and content type.
	 * @param resourceName The resource name.
	 * @param libraryName The library name.
	 * @param contentType The content type.
	 */
	public DynamicResource(String resourceName, String libraryName, String contentType) {
		setResourceName(resourceName);
		setLibraryName(libraryName);
		setContentType(contentType);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public String getRequestPath() {
		String mapping = getMapping();
		String path = RESOURCE_IDENTIFIER + "/" + getResourceName();
		return getRequestContextPath()
			+ (isPrefixMapping(mapping) ? (mapping + path) : (path + mapping))
			+ "?ln=" + getLibraryName()
			+ "&v=" + getLastModified();
	}

	@Override
	public URL getURL() {
		try {
			// Yes, this returns a HTTP URL, not a classpath URL. There's no other way anyway as dynamic resources are not present in classpath.
			return new URL(getRequestDomainURL() + getRequestPath());
		}
		catch (MalformedURLException e) {
			// This exception should never occur.
			throw new UnsupportedOperationException(e);
		}
	}

	@Override
	public Map<String, String> getResponseHeaders() {
		Map<String, String> responseHeaders = new HashMap<>(RESPONSE_HEADERS_SIZE);
		responseHeaders.put("Last-Modified", formatRFC1123(new Date(getLastModified())));
		responseHeaders.put("Expires", formatRFC1123(new Date(System.currentTimeMillis() + Hacks.getDefaultResourceMaxAge())));
		responseHeaders.put("Etag", String.format("W/\"%d-%d\"", getResourceName().hashCode(), getLastModified()));
		responseHeaders.put("Pragma", ""); // Explicitly set empty pragma to prevent some containers from setting it.
		return responseHeaders;
	}

	/**
	 * Returns the "last modified" timestamp of this resource.
	 * @return The "last modified" timestamp of this resource.
	 */
	public long getLastModified() {
		return lastModified;
	}

	/**
	 * Sets the "last modified" timestamp of this resource.
	 * @param lastModified The "last modified" timestamp of this resource.
	 */
	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	@Override
	public boolean userAgentNeedsUpdate(FacesContext context) {
		String ifModifiedSince = context.getExternalContext().getRequestHeaderMap().get("If-Modified-Since");

		if (ifModifiedSince != null) {
			try {
				return getLastModified() > parseRFC1123(ifModifiedSince).getTime() + SECONDS.toMillis(1); // RFC1123 doesn't store millis.
			}
			catch (ParseException ignore) {
				return true;
			}
		}

		return true;
	}

}