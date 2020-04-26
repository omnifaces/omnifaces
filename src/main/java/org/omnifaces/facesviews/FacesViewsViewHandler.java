/*
 * Copyright 2020 OmniFaces
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
package org.omnifaces.facesviews;

import static java.util.logging.Level.INFO;
import static java.util.stream.Collectors.joining;
import static javax.faces.application.ProjectStage.Production;
import static javax.servlet.RequestDispatcher.FORWARD_SERVLET_PATH;
import static org.omnifaces.facesviews.FacesViews.FACES_VIEWS_ORIGINAL_SERVLET_PATH;
import static org.omnifaces.facesviews.FacesViews.getFacesServletExtensions;
import static org.omnifaces.facesviews.FacesViews.getMappedResources;
import static org.omnifaces.facesviews.FacesViews.isMultiViewsEnabled;
import static org.omnifaces.facesviews.FacesViews.isScannedViewsAlwaysExtensionless;
import static org.omnifaces.facesviews.FacesViews.stripWelcomeFilePrefix;
import static org.omnifaces.util.Faces.getServletContext;
import static org.omnifaces.util.FacesLocal.getRequestAttribute;
import static org.omnifaces.util.FacesLocal.getRequestPathInfo;
import static org.omnifaces.util.FacesLocal.getServletContext;
import static org.omnifaces.util.Messages.addGlobalWarn;
import static org.omnifaces.util.ResourcePaths.getExtension;
import static org.omnifaces.util.ResourcePaths.isExtensionless;
import static org.omnifaces.util.ResourcePaths.PATH_SEPARATOR;
import static org.omnifaces.util.ResourcePaths.stripTrailingSlash;
import static org.omnifaces.util.Utils.coalesce;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

import javax.faces.application.Application;
import javax.faces.application.ViewHandler;
import javax.faces.application.ViewHandlerWrapper;
import javax.faces.context.FacesContext;
import javax.faces.event.PostConstructApplicationEvent;
import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;

import org.omnifaces.ApplicationProcessor;
import org.omnifaces.component.output.PathParam;

/**
 * View handler that renders an action URL extensionless if a resource is a mapped one, and faces views has been set to
 * always render extensionless or if the current request is extensionless, otherwise as-is.
 * <p>
 * <i>Implementation note</i>: this is installed by {@link ApplicationProcessor} during the {@link PostConstructApplicationEvent}, in
 * which it's guaranteed that Faces initialization (typically done via a {@link ServletContextListener}) has
 * been done. Setting a view handler programmatically requires the Faces {@link Application} to be present
 * which isn't the case before Faces initialization has been done.
 * <p>
 * Additionally, the view handler needs to be set BEFORE the first faces request is processed. Putting
 * the view handler setting code in a {@link Filter#init(javax.servlet.FilterConfig)} method only works
 * when all init methods are called during startup, OR when the filter filters every request.
 * <p>
 * For a guide on FacesViews, please see the <a href="package-summary.html">package summary</a>.
 *
 * @author Arjan Tijms
 * @since 1.3
 * @see FacesViews
 * @see ApplicationProcessor
 */
public class FacesViewsViewHandler extends ViewHandlerWrapper {

        private static final Logger logger = Logger.getLogger(FacesViewsViewHandler.class.getName());
        
        private static final String ERROR_MULTI_VIEW_NOT_CONFIGURED = 
                "Multi view was not configured for the view id '%s', but path parameters were defined for it.";
        
	private final boolean extensionless;

	public FacesViewsViewHandler(ViewHandler wrapped) {
		super(wrapped);
		extensionless = isScannedViewsAlwaysExtensionless(getServletContext());
	}

	@Override
	public String deriveViewId(FacesContext context, String viewId) {
		if (isExtensionless(viewId)) {
			String physicalViewId = getMappedResources(getServletContext()).get(viewId);

			if (physicalViewId != null) {
				return viewId + getExtension(physicalViewId);
			}
		}

		return super.deriveViewId(context, viewId);
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
			String[] uriAndQueryString = actionURL.split("\\?", 2);
			String uri = stripWelcomeFilePrefix(servletContext, removeExtensionIfNecessary(servletContext, uriAndQueryString[0], viewId));
			String queryString = uriAndQueryString.length > 1 ? ("?" + uriAndQueryString[1]) : "";
			String pathInfo = context.getViewRoot().getViewId().equals(viewId) ? coalesce(getRequestPathInfo(context), "") : "";
			return (pathInfo.isEmpty() ? uri : (stripTrailingSlash(uri) + pathInfo)) + queryString;
		}

		// Not a resource we mapped or not a forwarded one, take the version from the parent view handler.
		return actionURL;
	}

        /**
         * A method to create bookmarkable URLs via standard outcome target components that take into account
         * <code>&lt;o:pathParam&gt;</code> tags nested in the components. The path parameters will be rendered
         * in the order they were declared for a view id that is defined as a multi view and if the view was
         * not defined as a multi view then they won't be rendered at all. Additionally, declaring path parameters
         * for a non-multi view will be logged as a warning and a faces warning message will be added for any
         * stage different from <code>Production</code>.
         * 
         * @param context associated FacesContext.
         * @param viewId associated view id.
         * @param parameters associated parameters.
         * @param includeViewParams flag whether or not to include view parameters.
         * @return generated bookmarkable URL.
         */
        @Override
        public String getBookmarkableURL(FacesContext context, String viewId, Map<String, List<String>> parameters, boolean includeViewParams) {
                String pathParamKey = PathParam.PATH_PARAM_NAME_ATTRIBUTE_VALUE;
                if (!parameters.containsKey(pathParamKey)) {
                        return super.getBookmarkableURL(context, viewId, parameters, includeViewParams);
                }
                
                List<String> pathParamValues = parameters.get(PathParam.PATH_PARAM_NAME_ATTRIBUTE_VALUE);
                Map<String,List<String>> params = new LinkedHashMap<>(parameters);
                params.remove(pathParamKey);
                String bookmarkableURL = super.getBookmarkableURL(context, viewId, params, includeViewParams);

                if (isMultiViewsEnabled((ServletContext) context.getExternalContext().getContext(), viewId)) {
                        // This is a multi-view enabled viewId, so render the path parameters as well.
                        int queryDelimiterIndex = bookmarkableURL.indexOf("?");
                        boolean hasQueryString = queryDelimiterIndex != -1;
                        boolean hasTrailingSlash = bookmarkableURL.charAt(hasQueryString ? queryDelimiterIndex - 1 : bookmarkableURL.length() - 1) == PATH_SEPARATOR.charAt(0);
                        String pathParamsEncoded = context.getExternalContext().encodeActionURL(pathParamValues.stream()
                                .filter(Objects::nonNull)
                                .collect(joining(PATH_SEPARATOR, hasTrailingSlash ? "" : PATH_SEPARATOR, "")));
                        bookmarkableURL = hasQueryString 
                                ? bookmarkableURL.substring(0, queryDelimiterIndex) + pathParamsEncoded + bookmarkableURL.substring(queryDelimiterIndex) 
                                : bookmarkableURL + pathParamsEncoded;
                } else if (pathParamValues != null && !pathParamValues.isEmpty() && !context.isProjectStage(Production)) {
                        String message = String.format(ERROR_MULTI_VIEW_NOT_CONFIGURED, viewId);
                        addGlobalWarn(message);
                        
                        if (logger.isLoggable(INFO)) {
                            logger.log(INFO, message);
                        }
                }
                
                return bookmarkableURL;
        }
        
	private static boolean isOriginalViewExtensionless(FacesContext context) {
		String originalViewId = getRequestAttribute(context, FORWARD_SERVLET_PATH);

		if (originalViewId == null) {
			originalViewId = getRequestAttribute(context, FACES_VIEWS_ORIGINAL_SERVLET_PATH);
		}

		return originalViewId != null && isExtensionless(originalViewId);
	}

	private static String removeExtensionIfNecessary(ServletContext servletContext, String uri, String viewId) {
		Set<String> extensions = getFacesServletExtensions(servletContext);

		if (!isExtensionless(viewId)) {
			String viewIdExtension = getExtension(viewId);

			// TODO Is this necessary? Which cases does this cover?
			if (!extensions.contains(viewIdExtension)) {
				extensions = new HashSet<>(extensions);
				extensions.add(viewIdExtension);
			}
		}

		for (String extension : extensions) {
			if (uri.endsWith(extension)) {
				return uri.substring(0, uri.length() - extension.length());
			}
		}

		return uri;
	}

}