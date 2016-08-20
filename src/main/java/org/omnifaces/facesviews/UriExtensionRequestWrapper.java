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
 * <p>
 * For a guide on FacesViews, please see the <a href="package-summary.html">package summary</a>.
 *
 * @author Arjan Tijms
 * @since 1.4
 * @see FacesViews
 * @see FacesViewsForwardingFilter
 */
public class UriExtensionRequestWrapper extends HttpServletRequestWrapper {

	private final String servletPath;

	public UriExtensionRequestWrapper(HttpServletRequest request, String extension) {
		super(request);
		String servletPath = super.getServletPath();
		this.servletPath = servletPath.endsWith(extension) ? servletPath : servletPath + extension;
	}

	@Override
	public String getServletPath() {
		return servletPath;
	}

	@Override
	public String getPathInfo() {
		// Since we simulate that the request is mapped to an extension and not to a prefix path, there
		// can be no path info.
		return null;
	}

}