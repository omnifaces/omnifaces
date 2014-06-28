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

import static org.omnifaces.util.Faces.getMapping;
import static org.omnifaces.util.Faces.getMimeType;
import static org.omnifaces.util.Faces.getRequestContextPath;
import static org.omnifaces.util.Faces.getRequestDomainURL;
import static org.omnifaces.util.Faces.isPrefixMapping;
import static org.omnifaces.util.Utils.formatRFC1123;
import static org.omnifaces.util.Utils.parseRFC1123;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

/**
 * This {@link Resource} implementation holds all the necessary information about combined resources in order to
 * properly serve combined resources on a single HTTP request.
 * @author Bauke Scholtz
 */
final class CombinedResource extends Resource {

	// Properties -----------------------------------------------------------------------------------------------------

	private CombinedResourceInfo info;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Constructs a new combined resource based on the given resource name. This constructor is only used by
	 * {@link CombinedResourceHandler#createResource(String, String)}.
	 * @param name The resource name of the combined resource.
	 */
	public CombinedResource(String name) {
		String[] resourcePathParts = name.split("\\.", 2)[0].split("/");
		String resourceId = resourcePathParts[resourcePathParts.length - 1];
		info = CombinedResourceInfo.get(resourceId);
		setResourceName(name);
		setLibraryName(CombinedResourceHandler.LIBRARY_NAME);
		setContentType(getMimeType(name));
	}

	/**
	 * Constructs a new combined resource based on the current request. The resource name will be extracted from the
	 * request information and be passed to the other constructor {@link #CombinedResource(String)}. This constructor
	 * is only used by {@link CombinedResourceHandler#handleResourceRequest(FacesContext)}.
	 * @param context The faces context involved in the current request.
	 * @throws IllegalArgumentException If the resource name does not represent a valid combined resource. Specifically
	 * this exception is handled by {@link CombinedResourceHandler#handleResourceRequest(FacesContext)} which should
	 * return a 404.
	 */
	public CombinedResource(FacesContext context) {
		this(getResourceName(context));

		if (info == null || info.getResources().isEmpty()) {
			throw new IllegalArgumentException();
		}
	}

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public String getRequestPath() {
		String mapping = getMapping();
		String path = ResourceHandler.RESOURCE_IDENTIFIER + "/" + getResourceName();
		return getRequestContextPath()
			+ (isPrefixMapping(mapping) ? (mapping + path) : (path + mapping))
			+ "?ln=" + CombinedResourceHandler.LIBRARY_NAME
			+ "&v=" + (info.getLastModified() / 60000); // To force browser refresh whenever a resource changes.
	}

	@Override
	public URL getURL() {
		try {
			// Yes, this returns a HTTP URL, not a classpath URL. There's no other way anyway.
			return new URL(getRequestDomainURL() + getRequestPath());
		}
		catch (MalformedURLException e) {
			// This exception should never occur.
			throw new RuntimeException(e);
		}
	}

	@Override
	public Map<String, String> getResponseHeaders() {
		Map<String, String> responseHeaders = new HashMap<>(3);
		long lastModified = info.getLastModified();
		responseHeaders.put("Last-Modified", formatRFC1123(new Date(lastModified)));
		responseHeaders.put("Expires", formatRFC1123(new Date(System.currentTimeMillis() + info.getMaxAge())));
		responseHeaders.put("Etag", String.format("W/\"%d-%d\"", info.getContentLength(), lastModified));
		responseHeaders.put("Pragma", ""); // Explicitly set empty pragma to prevent some containers from setting it.
		return responseHeaders;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		if (!info.getResources().isEmpty()) {
			return new CombinedResourceInputStream(info.getResources());
		}
		else {
			return null;
		}
	}

	@Override
	public boolean userAgentNeedsUpdate(FacesContext context) {
		String ifModifiedSince = context.getExternalContext().getRequestHeaderMap().get("If-Modified-Since");

		if (ifModifiedSince != null) {
			try {
				return info.getLastModified() > parseRFC1123(ifModifiedSince).getTime();
			}
			catch (ParseException ignore) {
				return true;
			}
		}

		return true;
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Determines and returns the resource name of the current resource request.
	 * @param context The involved faces context.
	 * @return The resource name of the current resource request (without any faces servlet mapping).
	 */
	private static String getResourceName(FacesContext context) {
		ExternalContext externalContext = context.getExternalContext();
		String path = externalContext.getRequestPathInfo();

		if (path == null) {
			path = externalContext.getRequestServletPath();
			return path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));
		}
		else {
			return path.substring(path.lastIndexOf('/') + 1);
		}
	}

}