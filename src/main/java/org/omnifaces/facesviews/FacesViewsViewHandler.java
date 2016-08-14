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

import static javax.servlet.RequestDispatcher.FORWARD_SERVLET_PATH;
import static org.omnifaces.facesviews.FacesViews.FACES_VIEWS_ORIGINAL_PATH_INFO;
import static org.omnifaces.facesviews.FacesViews.FACES_VIEWS_ORIGINAL_SERVLET_PATH;
import static org.omnifaces.facesviews.FacesViews.FACES_VIEWS_RESOURCES;
import static org.omnifaces.facesviews.FacesViews.getFacesServletExtensions;
import static org.omnifaces.facesviews.FacesViews.getViewHandlerMode;
import static org.omnifaces.facesviews.FacesViews.isScannedViewsAlwaysExtensionless;
import static org.omnifaces.util.FacesLocal.getApplicationAttribute;
import static org.omnifaces.util.FacesLocal.getRequestAttribute;
import static org.omnifaces.util.FacesLocal.getRequestContextPath;
import static org.omnifaces.util.FacesLocal.getServletContext;
import static org.omnifaces.util.ResourcePaths.getExtension;
import static org.omnifaces.util.ResourcePaths.isExtensionless;
import static org.omnifaces.util.ResourcePaths.stripExtension;
import static org.omnifaces.util.Utils.coalesce;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.faces.application.ViewHandler;
import javax.faces.application.ViewHandlerWrapper;
import javax.faces.context.FacesContext;

/**
 * View handler that renders an action URL extensionless if a resource is a mapped one, and faces views has been set to always
 * render extensionless or if the current request is extensionless, otherwise as-is.
 *
 * <p>
 * For a guide on FacesViews, please see the <a href="package-summary.html">package summary</a>.
 *
 * @author Arjan Tijms
 *
 */
public class FacesViewsViewHandler extends ViewHandlerWrapper {

	private final ViewHandler wrapped;

	public FacesViewsViewHandler(ViewHandler viewHandler) {
		wrapped = viewHandler;
	}

	@Override
	public String deriveLogicalViewId(FacesContext context, String viewId) {
		Map<String, String> mappedResources = getApplicationAttribute(context, FACES_VIEWS_RESOURCES);
		return mappedResources.containsKey(viewId) ? super.deriveLogicalViewId(context, viewId) : viewId;
	}

	@Override
	public String getActionURL(FacesContext context, String viewId) {

		String actionURL = super.getActionURL(context, viewId);

		Map<String, String> mappedResources = getApplicationAttribute(context, FACES_VIEWS_RESOURCES);
		if (mappedResources.containsKey(viewId) && (isScannedViewsAlwaysExtensionless(context) || isOriginalViewExtensionless(context))) {
			String pathInfo = coalesce((String) getRequestAttribute(context, FACES_VIEWS_ORIGINAL_PATH_INFO), "");

			// User has requested to always render extensionless, or the requested viewId was mapped and the current
			// request is extensionless; render the action URL extensionless as well.

			switch (getViewHandlerMode(getServletContext(context))) {
				case STRIP_EXTENSION_FROM_PARENT:
					return removeExtension(context, actionURL, viewId) + pathInfo;
				case BUILD_WITH_PARENT_QUERY_PARAMETERS:
					return getRequestContextPath(context) + stripExtension(viewId) + pathInfo + getQueryParameters(actionURL);
			}
		}

		// Not a resource we mapped or not a forwarded one, take the version from the parent view handler
		return actionURL;
	}

	private boolean isOriginalViewExtensionless(FacesContext context) {
		String originalViewId = getRequestAttribute(context, FORWARD_SERVLET_PATH);
		if (originalViewId == null) {
			originalViewId = getRequestAttribute(context, FACES_VIEWS_ORIGINAL_SERVLET_PATH);
		}

		return isExtensionless(originalViewId);
	}

	public String removeExtension(FacesContext context, String resource, String viewId) {

		Set<String> extensions = getFacesServletExtensions(context);

		if (!isExtensionless(viewId)) {
			String viewIdExtension = getExtension(viewId);
			if (!extensions.contains(viewIdExtension)) {
				extensions = new HashSet<>(extensions);
				extensions.add(viewIdExtension);
			}
		}

		int lastSlashPos = resource.lastIndexOf('/');
		int lastQuestionMarkPos = resource.lastIndexOf('?'); // so we don't remove "extension" from parameter value
		for (String extension : extensions) {

			int extensionPos = resource.lastIndexOf(extension);
			if (extensionPos > lastSlashPos && (lastQuestionMarkPos == -1 || extensionPos < lastQuestionMarkPos)) {
				return resource.substring(0, extensionPos) + resource.substring(extensionPos + extension.length());
			}

		}

		return resource;
	}

	/**
	 * Extracts the query string from a resource.
	 *
	 * @param resource
	 *            A URL string
	 * @return the query string part of the URL
	 */
	public static String getQueryParameters(final String resource) {
		String queryParameters = "";
		int questionMarkPos = resource.indexOf('?');
		if (questionMarkPos != -1) {
			queryParameters = resource.substring(questionMarkPos);
		}
		return queryParameters;
	}

	@Override
	public ViewHandler getWrapped() {
		return wrapped;
	}

}