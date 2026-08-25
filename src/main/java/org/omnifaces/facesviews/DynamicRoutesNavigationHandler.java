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

import static java.lang.Boolean.parseBoolean;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static org.omnifaces.component.output.PathParam.PATH_PARAM_NAME_ATTRIBUTE_PREFIX;
import static org.omnifaces.facesviews.FacesViews.FACES_VIEWS_DYNAMIC_ROUTE_PARAMS;
import static org.omnifaces.util.FacesLocal.getServletContext;
import static org.omnifaces.util.FacesLocal.setRequestAttribute;
import static org.omnifaces.util.Reflection.findMethod;
import static org.omnifaces.util.Reflection.invokeMethod;
import static org.omnifaces.util.Servlets.toParameterMap;
import static org.omnifaces.util.Utils.coalesce;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.faces.FacesException;
import jakarta.faces.application.ConfigurableNavigationHandler;
import jakarta.faces.application.NavigationCase;
import jakarta.faces.application.NavigationHandler;
import jakarta.faces.context.FacesContext;

/**
 * Resolves an outcome which spells out a dynamic route with its segment values already filled in, such as
 * <code>&lt;h:link outcome="/organizations/123/members" /&gt;</code>, so that the concrete URL can be used as the outcome instead of the bracketed template
 * plus a <code>&lt;o:pathParam name&gt;</code> for every segment.
 * <p>
 * Such an outcome resolves to no view of its own, as the only view on disk is the bracketed one, so the standard navigation handler finds no navigation case
 * for it and the outcome target component renders without a target at all. This wrapper walks the scanned dynamic routes instead and, on a match, produces a
 * navigation case which points at the physical bracketed view and carries the resolved segment values as parameters named after
 * {@value org.omnifaces.component.output.PathParam#PATH_PARAM_NAME_ATTRIBUTE_PREFIX}. Both implementations merge the parameters of a navigation case into the
 * ones handed to {@link jakarta.faces.application.ViewHandler#getBookmarkableURL(FacesContext, String, Map, boolean)}, which is where
 * {@link FacesViewsViewHandler} substitutes them back into the URL.
 *
 * @author Bauke Scholtz
 * @since 5.5
 * @see FacesViews
 */
public class DynamicRoutesNavigationHandler extends ConfigurableNavigationHandler {

    private static final char QUERY_STRING_SEPARATOR = '?';
    private static final String REDIRECT_PARAM_NAME = "faces-redirect";
    private static final String INCLUDE_VIEW_PARAMS_PARAM_NAME = "includeViewParams";

    private final NavigationHandler wrapped;

    /**
     * Construct the dynamic routes navigation handler.
     *
     * @param wrapped The navigation handler to be wrapped.
     */
    public DynamicRoutesNavigationHandler(NavigationHandler wrapped) {
        this.wrapped = wrapped;
    }

    /**
     * Returns the wrapped navigation handler.
     *
     * @return The wrapped navigation handler.
     */
    public NavigationHandler getWrapped() {
        return wrapped;
    }

    @Override
    public Map<String, Set<NavigationCase>> getNavigationCases() {
        return wrapped instanceof ConfigurableNavigationHandler configurable ? configurable.getNavigationCases() : emptyMap();
    }

    /**
     * Obtains the navigation case of the wrapped navigation handler. Faces 4.1 declares this on {@link ConfigurableNavigationHandler} while Faces 5 merges it
     * into {@link NavigationHandler} and deprecates the former, so a navigation handler of either shape must be asked, the latter reflectively as the compile
     * baseline does not know that method yet.
     */
    private NavigationCase getWrappedNavigationCase(FacesContext context, String fromAction, String outcome) {
        if (wrapped instanceof ConfigurableNavigationHandler configurable) {
            return configurable.getNavigationCase(context, fromAction, outcome);
        }

        var method = findMethod(wrapped, "getNavigationCase", context, fromAction, outcome);
        return method == null ? null : invokeMethod(wrapped, method, context, fromAction, outcome);
    }

    /**
     * Obtains the navigation case of the wrapped navigation handler for an outcome which is known to resolve to a dynamic route, discarding the global faces
     * messages it adds when it finds none of its own. A Faces implementation warns about an outcome it cannot resolve when the project stage is Development,
     * which does not hold for such an outcome, as the dynamic route resolves it.
     */
    private NavigationCase getWrappedNavigationCaseForDynamicRoute(FacesContext context, String fromAction, String outcome) {
        var globalMessageCount = context.getMessageList(null).size();
        NavigationCase navigationCase = getWrappedNavigationCase(context, fromAction, outcome);

        if (navigationCase == null) {
            dropGlobalMessagesAddedSince(context, globalMessageCount);
        }

        return navigationCase;
    }

    private static void dropGlobalMessagesAddedSince(FacesContext context, int globalMessageCount) {
        var iterator = context.getMessages(null);

        for (var i = 0; iterator.hasNext(); i++) {
            iterator.next();

            if (i >= globalMessageCount) {
                iterator.remove();
            }
        }
    }

    /**
     * An explicitly declared navigation case for an outcome which also resolves to a dynamic route takes precedence over that dynamic route.
     */
    @Override
    public NavigationCase getNavigationCase(FacesContext context, String fromAction, String outcome) {
        NavigationCase dynamicRouteNavigationCase = getDynamicRouteNavigationCase(context, fromAction, outcome);

        if (dynamicRouteNavigationCase == null) {
            return getWrappedNavigationCase(context, fromAction, outcome);
        }

        return coalesce(getWrappedNavigationCaseForDynamicRoute(context, fromAction, outcome), dynamicRouteNavigationCase);
    }

    @Override
    public NavigationCase getNavigationCase(FacesContext context, String fromAction, String outcome, String toFlowDocumentId) {
        return getNavigationCase(context, fromAction, outcome);
    }

    /**
     * The wrapped navigation handler resolves an action outcome by consulting its own navigation cases rather than this wrapper's, so a dynamic route would
     * otherwise be invisible to it and the navigation would silently not happen at all.
     */
    @Override
    public void handleNavigation(FacesContext context, String fromAction, String outcome) {
        NavigationCase navigationCase = getDynamicRouteNavigationCase(context, fromAction, outcome);

        if (navigationCase != null && getWrappedNavigationCaseForDynamicRoute(context, fromAction, outcome) == null) {
            performNavigation(context, navigationCase);
        }
        else {
            wrapped.handleNavigation(context, fromAction, outcome);
        }
    }

    private static void performNavigation(FacesContext context, NavigationCase navigationCase) {
        var viewHandler = context.getApplication().getViewHandler();
        var toViewId = navigationCase.getToViewId(context);

        if (!navigationCase.isRedirect()) {
            // The request did not pass through the forwarding filter, so publish the segment values it would otherwise have exposed, as both the injection of
            // a named path parameter and the rendering of the view's own action URL read them from there.
            setRequestAttribute(context, FACES_VIEWS_DYNAMIC_ROUTE_PARAMS, FacesViewsViewHandler.getNamedPathParams(navigationCase.getParameters()));
            context.setViewRoot(viewHandler.createView(context, toViewId));
            context.renderResponse();
            return;
        }

        var externalContext = context.getExternalContext();

        try {
            externalContext.redirect(
                viewHandler.getRedirectURL(context, toViewId, navigationCase.getParameters(), navigationCase.isIncludeViewParams())
            );
        }
        catch (IOException e) {
            throw new FacesException(e);
        }
    }

    private static NavigationCase getDynamicRouteNavigationCase(FacesContext context, String fromAction, String outcome) {
        if (outcome == null || outcome.isEmpty() || outcome.charAt(0) != '/') {
            return null;
        }

        DynamicRoutes dynamicRoutes = DynamicRoutes.get(getServletContext(context));

        if (dynamicRoutes == null || dynamicRoutes.isEmpty()) {
            return null;
        }

        var queryStringIndex = outcome.indexOf(QUERY_STRING_SEPARATOR);
        var path = queryStringIndex < 0 ? outcome : outcome.substring(0, queryStringIndex);
        DynamicRoutes.Match match = dynamicRoutes.match(path);

        if (match == null) {
            return null;
        }

        Map<String, List<String>> parameters = new LinkedHashMap<>();
        var redirect = false;
        var includeViewParams = false;

        if (queryStringIndex > -1) {
            for (var parameter : toParameterMap(outcome.substring(queryStringIndex + 1)).entrySet()) {
                switch (parameter.getKey()) {
                    case REDIRECT_PARAM_NAME -> redirect = parseBoolean(parameter.getValue().get(0));
                    case INCLUDE_VIEW_PARAMS_PARAM_NAME -> includeViewParams = parseBoolean(parameter.getValue().get(0));
                    default -> parameters.put(parameter.getKey(), parameter.getValue());
                }
            }
        }

        for (var param : match.params().entrySet()) {
            parameters.put(PATH_PARAM_NAME_ATTRIBUTE_PREFIX + param.getKey(), List.of(param.getValue()));
        }

        var fromViewId = context.getViewRoot() != null ? context.getViewRoot().getViewId() : null;
        return new NavigationCase(fromViewId, fromAction, outcome, null, match.viewId(), null, unmodifiableMap(parameters), redirect, includeViewParams);
    }

}
