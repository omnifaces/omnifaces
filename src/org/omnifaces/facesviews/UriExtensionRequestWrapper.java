/*
 * Copyright 2013 OmniFaces.
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
package org.omnifaces.facesviews;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * This wraps a request to an extensionless JSF view and provides an extension for
 * all methods that reveal the servlet path. Additional the path info is set to null.
 * <p>
 * This is needed since JSF implementations inspect the request to determine if a
 * prefix (path) or suffix (extension) mapping was used. If the request is neither
 * (in effect, an "exact and extensionless mapping), JSF will get confused and not
 * be able to derive view IDs etc correctly.
 * 
 * @author Arjan Tijms
 * @since 1.4
 */
public class UriExtensionRequestWrapper extends HttpServletRequestWrapper {
	
	private final String extension;

	public UriExtensionRequestWrapper(HttpServletRequest request, String extension) {
		super(request);
		this.extension = extension;
	}
	
	@Override
	public String getServletPath() {
		
		String servletPath = super.getServletPath();
		if (servletPath.endsWith(extension)) {
			return servletPath;
		}
		
		return servletPath + extension;
	}
	
	@Override
	public String getRequestURI() {
		
		String requestURI = super.getRequestURI();
		if (requestURI.endsWith(extension)) {
			return requestURI;
		}
		
		return requestURI + extension;
	}
	
	@Override
	public StringBuffer getRequestURL() {
		
		StringBuffer requestURL = super.getRequestURL();
		int extensionPos = requestURL.lastIndexOf(extension);
		if (extensionPos != -1 && requestURL.length() - extension.length() == extensionPos) {
			return requestURL;
		}
		
		return requestURL.append(extension);
	}
	
	@Override
	public String getPathInfo() {
		// Since we simulate that the request mapped to an extension and not to a prefix path, there
		// can be no path info.
		return null;
	}
	
}
