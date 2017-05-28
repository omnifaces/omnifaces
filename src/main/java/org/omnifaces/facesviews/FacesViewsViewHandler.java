/*
 * Copyright 2017 OmniFaces
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
import static org.omnifaces.facesviews.FacesViews.FACES_VIEWS_ORIGINAL_SERVLET_PATH;
import static org.omnifaces.facesviews.FacesViews.getFacesServletExtensions;
import static org.omnifaces.facesviews.FacesViews.getMappedResources;
import static org.omnifaces.facesviews.FacesViews.getViewHandlerMode;
import static org.omnifaces.facesviews.FacesViews.isScannedViewsAlwaysExtensionless;
import static org.omnifaces.facesviews.FacesViews.stripWelcomeFilePrefix;
import static org.omnifaces.facesviews.ViewHandlerMode.BUILD_WITH_PARENT_QUERY_PARAMETERS;
import static org.omnifaces.util.Faces.getServletContext;
import static org.omnifaces.util.FacesLocal.getRequestAttribute;
import static org.omnifaces.util.FacesLocal.getRequestContextPath;
import static org.omnifaces.util.FacesLocal.getRequestPathInfo;
import static org.omnifaces.util.FacesLocal.getServletContext;
import static org.omnifaces.util.ResourcePaths.getExtension;
import static org.omnifaces.util.ResourcePaths.isExtensionless;
import static org.omnifaces.util.ResourcePaths.stripExtension;
import static org.omnifaces.util.ResourcePaths.stripTrailingSlash;
import static org.omnifaces.util.Utils.coalesce;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.faces.application.ViewHandler;
import javax.faces.application.ViewHandlerWrapper;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

/**
 * View handler that renders an action URL extensionless if a resource is a mapped one, and faces views has been set to
 * always render extensionless or if the current request is extensionless, otherwise as-is.
 * <p>
 * For a guide on FacesViews, please see the <a href="package-summary.html">package summary</a>.
 *
 * @author Arjan Tijms
 * @since 1.3
 * @see FacesViews
 * @see FacesViewsViewHandlerInstaller
 * @see ViewHandlerMode
 */
public class FacesViewsViewHandler extends ViewHandlerWrapper {

	private final ViewHandler wrapped;
	private final ViewHandlerMode mode;
	private final boolean extensionless;

	public FacesViewsViewHandler(ViewHandler viewHandler) {
		wrapped = viewHandler;
		mode = getViewHandlerMode(getServletContext());
		extensionless = isScannedViewsAlwaysExtensionless(getServletContext());
	}

	@Override
	public String deriveLogicalViewId(FacesContext context, String viewId) {
		Map<String, String> mappedResources = getMappedResources(getServletContext(context));
		return mappedResources.containsKey(viewId) ? super.deriveLogicalViewId(context, viewId) : viewId;
	}

	@Override
	public String getActionURL(FacesContext context, String viewId) {
		String actionURL = super.getActionURL(context, viewId);
		ServletContext servletContext = getServletContext(context);
		Map<String, String> mappedResources = getMappedResources(servletContext);

		if (mappedResources.containsKey(viewId) && (extensionless || isOriginalViewExtensionless(context))) {
			// User has requested to always render extensionless, or the requested viewId was mapped and the current
			// request is extensionless; render the action URL extensionless as well.

			String pathInfo = context.getViewRoot().getViewId().equals(viewId) ? coalesce(getRequestPathInfo(context), "") : "";
			String queryString = getQueryString(actionURL);

			if (mode == BUILD_WITH_PARENT_QUERY_PARAMETERS) {
				return getRequestContextPath(context) + stripExtension(viewId) + pathInfo + queryString;
			}
			else {
				actionURL = removeExtension(servletContext, actionURL, viewId);
				return (pathInfo.isEmpty() ? actionURL : (stripTrailingSlash(actionURL) + pathInfo)) + queryString;
			}
		}

		// Not a resource we mapped or not a forwarded one, take the version from the parent view handler.
		return actionURL;
	}

	@Override
	public ViewHandler getWrapped() {
		return wrapped;
	}

	private static boolean isOriginalViewExtensionless(FacesContext context) {
		String originalViewId = getRequestAttribute(context, FORWARD_SERVLET_PATH);

		if (originalViewId == null) {
			originalViewId = getRequestAttribute(context, FACES_VIEWS_ORIGINAL_SERVLET_PATH);
		}

		return isExtensionless(originalViewId);
	}

	private static String removeExtension(ServletContext servletContext, String actionURL, String viewId) {
		Set<String> extensions = getFacesServletExtensions(servletContext);

		if (!isExtensionless(viewId)) {
			String viewIdExtension = getExtension(viewId);

			// TODO Is this necessary? Which cases does this cover?
			if (!extensions.contains(viewIdExtension)) {
				extensions = new HashSet<>(extensions);
				extensions.add(viewIdExtension);
			}
		}

		String resource = actionURL.split("\\?", 2)[0];

		for (String extension : extensions) {
			if (resource.endsWith(extension)) {
				return stripWelcomeFilePrefix(servletContext, resource.substring(0, resource.length() - extension.length()));
			}
		}

		return actionURL;
	}

	private static String getQueryString(String resource) {
		int questionMarkPos = resource.indexOf('?');
		return (questionMarkPos != -1) ? resource.substring(questionMarkPos) : "";
	}

}