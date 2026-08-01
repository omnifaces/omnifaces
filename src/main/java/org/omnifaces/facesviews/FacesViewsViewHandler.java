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
package org.omnifaces.facesviews;

import static jakarta.servlet.RequestDispatcher.FORWARD_SERVLET_PATH;
import static java.lang.Boolean.TRUE;
import static java.util.logging.Level.WARNING;
import static org.omnifaces.facesviews.FacesViews.FACES_VIEWS_DYNAMIC_ROUTE_PARAMS;
import static org.omnifaces.facesviews.FacesViews.FACES_VIEWS_ORIGINAL_SERVLET_PATH;
import static org.omnifaces.facesviews.FacesViews.getFacesServletExtensions;
import static org.omnifaces.facesviews.FacesViews.getMappedResources;
import static org.omnifaces.facesviews.FacesViews.isLowercasedRequestURI;
import static org.omnifaces.facesviews.FacesViews.isScannedViewsAlwaysExtensionless;
import static org.omnifaces.facesviews.FacesViews.stripWelcomeFilePrefix;
import static org.omnifaces.util.Faces.getServletContext;
import static org.omnifaces.util.FacesLocal.getRequestAttribute;
import static org.omnifaces.util.FacesLocal.getRequestPathInfo;
import static org.omnifaces.util.FacesLocal.getServletContext;
import static org.omnifaces.util.FacesLocal.isDevelopment;
import static org.omnifaces.util.FacesLocal.removeRequestAttribute;
import static org.omnifaces.util.FacesLocal.setRequestAttribute;
import static org.omnifaces.util.Messages.addGlobalWarn;
import static org.omnifaces.util.ResourcePaths.PATH_SEPARATOR;
import static org.omnifaces.util.ResourcePaths.getExtension;
import static org.omnifaces.util.ResourcePaths.isExtensionless;
import static org.omnifaces.util.ResourcePaths.stripTrailingSlash;
import static org.omnifaces.util.Utils.coalesce;
import static org.omnifaces.util.Utils.encodeURI;
import static org.omnifaces.util.Utils.isEmpty;
import static org.omnifaces.util.Utils.replaceFirstLiteral;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jakarta.faces.application.Application;
import jakarta.faces.application.ViewHandler;
import jakarta.faces.application.ViewHandlerWrapper;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.PostConstructApplicationEvent;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextListener;

import org.omnifaces.ApplicationProcessor;
import org.omnifaces.component.output.PathParam;

/**
 * View handler that renders an action URL extensionless if a resource is a mapped one, and faces views has been set to always render extensionless or if the
 * current request is extensionless, otherwise as-is.
 * <p>
 * <i>Implementation note</i>: this is installed by {@link ApplicationProcessor} during the {@link PostConstructApplicationEvent}, in which it's guaranteed that
 * Faces initialization (typically done via a {@link ServletContextListener}) has been done. Setting a view handler programmatically requires the Faces
 * {@link Application} to be present which isn't the case before Faces initialization has been done.
 * <p>
 * Additionally, the view handler needs to be set BEFORE the first faces request is processed. Putting the view handler setting code in a
 * {@link Filter#init(jakarta.servlet.FilterConfig)} method only works when all init methods are called during startup, OR when the filter filters every
 * request.
 * <p>
 * For a guide on FacesViews, please see {@link FacesViews}.
 *
 * @author Arjan Tijms
 * @since 1.3
 * @see FacesViews
 * @see ApplicationProcessor
 */
public class FacesViewsViewHandler extends ViewHandlerWrapper {

    private static final Logger logger = Logger.getLogger(FacesViewsViewHandler.class.getName());

    private static final String RENDERING_BOOKMARKABLE_URL = "org.omnifaces.facesviews.rendering_bookmarkable_url";

    private static final String ERROR_MULTI_VIEW_NOT_CONFIGURED = "MultiViews was not configured for the view id '%s', but path parameters were defined for it.";

    // Splits an URI into [path, suffix] where suffix starts at the first query string ('?'), fragment ('#') or matrix parameter (';') delimiter, if any.
    private static final Pattern PATTERN_URI_SUFFIX = Pattern.compile("(?=[?#;])");

    private final boolean extensionless;
    private final boolean lowercasedRequestURI;

    /**
     * Construct faces views view handler.
     *
     * @param wrapped The view handler to be wrapped.
     */
    public FacesViewsViewHandler(ViewHandler wrapped) {
        super(wrapped);
        ServletContext servletContext = getServletContext();
        extensionless = isScannedViewsAlwaysExtensionless(servletContext);
        lowercasedRequestURI = isLowercasedRequestURI(servletContext);
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
    public String getActionURL(FacesContext context, String viewId) {
        String actionURL = super.getActionURL(context, viewId);
        ServletContext servletContext = getServletContext(context);
        Map<String, String> mappedResources = getMappedResources(servletContext);
        String resourceName = lowercasedRequestURI ? viewId.toLowerCase() : viewId;

        if (mappedResources.containsKey(resourceName) && (extensionless || isOriginalViewExtensionless(context))) {
            // User has requested to always render extensionless, or the requested viewId was mapped and the current
            // request is extensionless; render the action URL extensionless as well.
            String source = lowercasedRequestURI ? replaceFirstLiteral(actionURL, viewId, resourceName) : actionURL;
            String[] uriAndRest = PATTERN_URI_SUFFIX.split(source, 2);
            String uri = stripWelcomeFilePrefix(servletContext, removeExtensionIfNecessary(servletContext, uriAndRest[0], viewId));
            var rest = uriAndRest.length > 1 ? uriAndRest[1] : "";
            var pathInfo = isCurrentView(context, viewId) ? coalesce(getRequestPathInfo(context), "") : "";
            uri = interpolateDynamicRouteIfNecessary(context, uri, viewId);
            return (pathInfo.isEmpty() ? uri : (stripTrailingSlash(uri) + pathInfo)) + rest;
        }

        // Not a resource we mapped or not a forwarded one, take the version from the parent view handler.
        return actionURL;
    }

    /**
     * An override to create bookmarkable URLs via standard outcome target components that take into account <code>&lt;o:pathParam&gt;</code> tags nested in the
     * components. The path parameters will be rendered in the order they were declared for a view id that is defined as a multi view and if the view was not
     * defined as a multi view then they won't be rendered at all. Additionally, declaring path parameters for a non-multi view will be logged as a warning and
     * a faces warning message will be added for <code>Development</code> stage.
     *
     * @see PathParam
     */
    @Override
    public String getBookmarkableURL(FacesContext context, String viewId, Map<String, List<String>> parameters, boolean includeViewParams) {
        List<String> pathParams = parameters.get(PathParam.PATH_PARAM_NAME_ATTRIBUTE_VALUE);
        Map<String, String> namedPathParams = getNamedPathParams(parameters);

        if (isEmpty(pathParams) && namedPathParams.isEmpty() && !DynamicRoutes.isDynamicRoute(viewId)) {
            return super.getBookmarkableURL(context, viewId, parameters, includeViewParams);
        }

        Map<String, List<String>> parametersWithoutPathParams = new LinkedHashMap<>(parameters);
        parametersWithoutPathParams.keySet().removeIf(FacesViewsViewHandler::isPathParam);
        String bookmarkableURL = getUninterpolatedBookmarkableURL(context, viewId, parametersWithoutPathParams, includeViewParams);
        var multiViews = MultiViews.isEnabled(getServletContext(context), viewId);

        if (!multiViews && !isEmpty(pathParams) && isDevelopment(context)) {
            String message = String.format(ERROR_MULTI_VIEW_NOT_CONFIGURED, viewId);
            addGlobalWarn(message);
            logger.log(WARNING, message);
        }

        return buildPathParamsURL(
            bookmarkableURL, getRequestPathInfo(context), DynamicRoutes.isDynamicRoute(viewId), namedPathParams, multiViews, pathParams
        );
    }

    /**
     * Assembles the bookmarkable URL from its parts: the URL as rendered by the wrapped view handler, with this request's path info removed, the dynamic route
     * segments substituted by the named path parameters, and the unnamed path parameters appended as new path info.
     * <p>
     * This is deliberately free of any {@link FacesContext} lookup so that it can be unit tested on its own, as the interplay between a dynamic route and a
     * MultiViews view is easy to get wrong and expensive to cover with integration tests alone.
     *
     * @param bookmarkableURL The URL as rendered by the wrapped view handler, still holding the bracketed segments.
     * @param requestPathInfo The path info of the current request, if any.
     * @param dynamicRoute Whether the target view is a dynamic route.
     * @param namedPathParams The values of the named path parameters, by segment name.
     * @param multiViews Whether the target view is a MultiViews view.
     * @param pathParams The values of the unnamed path parameters, in declaration order.
     * @return The assembled bookmarkable URL.
     * @throws IllegalArgumentException When a dynamic route segment has no value.
     */
    static String buildPathParamsURL(
        String bookmarkableURL, String requestPathInfo, boolean dynamicRoute, Map<String, String> namedPathParams, boolean multiViews, List<String> pathParams
    )
    {
        String[] uriAndRest = PATTERN_URI_SUFFIX.split(bookmarkableURL, 2);
        var rest = uriAndRest.length > 1 ? uriAndRest[1] : "";
        var uri = removePathInfo(uriAndRest[0], requestPathInfo);

        if (dynamicRoute) {
            uri = DynamicRoutes.interpolate(uri, namedPathParams);
        }

        if (multiViews) {
            return stripTrailingSlash(uri) + getPathInfo(pathParams) + rest;
        }

        return dynamicRoute ? (stripTrailingSlash(uri) + rest) : bookmarkableURL;
    }

    /**
     * An override to interpolate the dynamic route segments of a redirect URL, which the wrapped view handler resolves by re-entering
     * {@link #getActionURL(FacesContext, String)} for a view which is not the current one. A redirect has no outcome target component to hang a
     * <code>&lt;o:pathParam&gt;</code> on, so the segment values come from the navigation case parameters instead.
     */
    @Override
    public String getRedirectURL(FacesContext context, String viewId, Map<String, List<String>> parameters, boolean includeViewParams) {
        if (!DynamicRoutes.isDynamicRoute(viewId)) {
            return super.getRedirectURL(context, viewId, parameters, includeViewParams);
        }

        Map<String, List<String>> parametersWithoutPathParams = new LinkedHashMap<>(parameters);
        parametersWithoutPathParams.keySet().removeIf(FacesViewsViewHandler::isPathParam);
        setRequestAttribute(context, RENDERING_BOOKMARKABLE_URL, TRUE);

        try {
            var redirectURL = super.getRedirectURL(context, viewId, parametersWithoutPathParams, includeViewParams);
            String[] uriAndRest = PATTERN_URI_SUFFIX.split(redirectURL, 2);
            var rest = uriAndRest.length > 1 ? uriAndRest[1] : "";
            return DynamicRoutes.interpolate(uriAndRest[0], getNamedPathParams(parameters)) + rest;
        }
        finally {
            removeRequestAttribute(context, RENDERING_BOOKMARKABLE_URL);
        }
    }

    /**
     * Obtains the bookmarkable URL with the dynamic route segments still in place, so that the values of <code>&lt;o:pathParam name&gt;</code> rather than
     * those of the current request end up in it. The wrapped view handler resolves the action URL by re-entering this view handler, which would otherwise
     * interpolate a link to the very view it is rendered on with that view's own segment values.
     */
    private String getUninterpolatedBookmarkableURL(FacesContext context, String viewId, Map<String, List<String>> parameters, boolean includeViewParams) {
        if (!DynamicRoutes.isDynamicRoute(viewId)) {
            return super.getBookmarkableURL(context, viewId, parameters, includeViewParams);
        }

        setRequestAttribute(context, RENDERING_BOOKMARKABLE_URL, TRUE);

        try {
            return super.getBookmarkableURL(context, viewId, parameters, includeViewParams);
        }
        finally {
            removeRequestAttribute(context, RENDERING_BOOKMARKABLE_URL);
        }
    }

    /**
     * Replaces the bracketed segments of a dynamic route by the values resolved for the current request, so that the rendered URL is the one the request
     * actually came in on. Only the current view can be interpolated this way; a link to another dynamic route must name its segments explicitly with
     * <code>&lt;o:pathParam&gt;</code>.
     */
    private static String interpolateDynamicRouteIfNecessary(FacesContext context, String uri, String viewId) {
        if (!DynamicRoutes.isDynamicRoute(viewId) || TRUE.equals(getRequestAttribute(context, RENDERING_BOOKMARKABLE_URL))) {
            return uri; // Another view's segments are supplied by <o:pathParam name>, which getBookmarkableURL() substitutes afterwards.
        }

        // Only the current view can be interpolated from the request; for any other view this is a redirect, which has no way to supply the segments and must
        // therefore fail rather than emit a URL containing square brackets.
        Map<String, String> params = isCurrentView(context, viewId) ? getRequestAttribute(context, FACES_VIEWS_DYNAMIC_ROUTE_PARAMS) : null;
        return DynamicRoutes.interpolate(uri, params);
    }

    private static boolean isCurrentView(FacesContext context, String viewId) {
        return context.getViewRoot() != null && context.getViewRoot().getViewId().equals(viewId);
    }

    /**
     * Renders the unnamed path parameters as positional path info. This runs once per outcome target component, hence the plain loop.
     */
    private static String getPathInfo(List<String> pathParams) {
        var pathInfo = new StringBuilder();

        if (!isEmpty(pathParams)) {
            for (var pathParam : pathParams) {
                if (pathParam != null) {
                    pathInfo.append(PATH_SEPARATOR).append(encodeURI(pathParam));
                }
            }
        }

        return pathInfo.toString();
    }

    private static boolean isPathParam(String name) {
        return PathParam.PATH_PARAM_NAME_ATTRIBUTE_VALUE.equals(name) || name.startsWith(PathParam.PATH_PARAM_NAME_ATTRIBUTE_PREFIX);
    }

    /**
     * Extracts the values of the named path parameters from the given component or navigation case parameters, keyed by dynamic route segment name.
     */
    static Map<String, String> getNamedPathParams(Map<String, List<String>> parameters) {
        Map<String, String> namedPathParams = new LinkedHashMap<>();

        for (var entry : parameters.entrySet()) {
            if (entry.getKey().startsWith(PathParam.PATH_PARAM_NAME_ATTRIBUTE_PREFIX) && !isEmpty(entry.getValue())) {
                namedPathParams.put(entry.getKey().substring(PathParam.PATH_PARAM_NAME_ATTRIBUTE_PREFIX.length()), entry.getValue().get(0));
            }
        }

        return namedPathParams;
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

            // Defensive patch for development-stage hot reload: FacesViewsResourceHandler may rescan and add a view with a previously-unseen extension, which
            // won't be in the cached FACES_SERVLET_EXTENSIONS set (computed once from the FacesServlet's startup-time mappings). Without this, extensionless
            // rendering would break for such views until restart.
            if (!extensions.contains(viewIdExtension)) {
                extensions = new HashSet<>(extensions);
                extensions.add(viewIdExtension);
            }
        }

        for (var extension : extensions) {
            if (uri.endsWith(extension)) {
                return uri.substring(0, uri.length() - extension.length());
            }
        }

        return uri;
    }

    private static String removePathInfo(String uri, String pathInfo) {
        return (pathInfo != null && uri.endsWith(pathInfo)) ? uri.substring(0, uri.length() - pathInfo.length()) : uri;
    }

}
