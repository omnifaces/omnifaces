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
package org.omnifaces.resource.combined;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.omnifaces.util.Faces;

/**
 * This {@link Resource} implementation holds all the necessary information about combined resources in order to
 * properly serve combined resources on a single HTTP request.
 * @author Bauke Scholtz
 */
final class CombinedResource extends Resource {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String PATTERN_RFC1123_DATE = "EEE, dd MMM yyyy HH:mm:ss zzz";
	private static final TimeZone TIMEZONE_GMT = TimeZone.getTimeZone("GMT");
	private static final String HEADER_LAST_MODIFIED = "Last-Modified";
	private static final String HEADER_EXPIRES = "Expires";
	private static final String HEADER_ETAG = "ETag";
	private static final String FORMAT_ETAG = "W/\"%d-%d\"";
	private static final String HEADER_IF_MODIFIED_SINCE = "If-Modified-Since";

	private static final String ERROR_UNKNOWN_RESOURCE_NAME = "Unknown resource name: %s";
	private static final String ERROR_CANNOT_CREATE_URL = "Cannot create an URL. %s is an in-memory resource pointer.";

	// Properties -----------------------------------------------------------------------------------------------------

	private CombinedResourceInfo info;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Constructs a new combined resource based on the given resource name. This constructor is only used by
	 * {@link CombinedResourceHandler#createResource(String, String)}.
	 * @param name The resource name of the combined resource.
	 * @throws IllegalArgumentException If the resource name does not represent a valid combined resource.
	 */
	public CombinedResource(String name) {
		info = CombinedResourceInfo.get(name.split("\\.", 2)[0]);

		if (info == null) {
			throw new IllegalArgumentException(String.format(ERROR_UNKNOWN_RESOURCE_NAME, name));
		}

		setResourceName(name);
		setLibraryName(CombinedResourceHandler.LIBRARY_NAME);
		setContentType(Faces.getMimeType(name));
	}

	/**
	 * Constructs a new combined resource based on the current request. The resource name will be extracted from the
	 * request information and be passed to the other constructor {@link #CombinedResource(String)}. This constructor
	 * is only used by {@link CombinedResourceHandler#handleResourceRequest(FacesContext)}.
	 * @param context The faces context involved in the current request.
	 * @throws IllegalArgumentException If the resource name does not represent a valid combined resource.
	 */
	public CombinedResource(FacesContext context) {
		this(getResourceName(context));
	}

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public String getRequestPath() {
		String mapping = Faces.getMapping();
		String path = ResourceHandler.RESOURCE_IDENTIFIER + "/" + getResourceName();
		return Faces.getRequestContextPath()
			+ (Faces.isPrefixMapping(mapping) ? (mapping + path) : (path + mapping))
			+ "?ln=" + CombinedResourceHandler.LIBRARY_NAME
			+ "&amp;v=" + (Faces.<Long>evaluateExpressionGet("#{startup.time}") / 60000); // In minutes.
	}

	@Override
	public URL getURL() {
		// This method won't be used anyway. It's only used on Facelet templates and composite components.
		// If really necessary, we can always invent some custom protocol, but this is nonsense.
		throw new UnsupportedOperationException(String.format(ERROR_CANNOT_CREATE_URL, getResourceName()));
	}

	@Override
	public Map<String, String> getResponseHeaders() {
		Map<String, String> responseHeaders = new HashMap<String, String>(3);
		SimpleDateFormat sdf = new SimpleDateFormat(PATTERN_RFC1123_DATE, Locale.US);
		sdf.setTimeZone(TIMEZONE_GMT);
		responseHeaders.put(HEADER_LAST_MODIFIED, sdf.format(new Date(info.getLastModified())));
		responseHeaders.put(HEADER_EXPIRES, sdf.format(new Date(System.currentTimeMillis() + info.getMaxAge())));
		responseHeaders.put(HEADER_ETAG, String.format(FORMAT_ETAG, info.getContentLength(), info.getLastModified()));
		return responseHeaders;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return new CombinedResourceInputStream(info.getResources());
	}

	@Override
	public boolean userAgentNeedsUpdate(FacesContext context) {
		String ifModifiedSince = context.getExternalContext().getRequestHeaderMap().get(HEADER_IF_MODIFIED_SINCE);

		if (ifModifiedSince != null) {
			SimpleDateFormat sdf = new SimpleDateFormat(PATTERN_RFC1123_DATE, Locale.US);

			try {
				info.reload();
				return info.getLastModified() > sdf.parse(ifModifiedSince).getTime();
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