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

import static java.util.Collections.unmodifiableMap;
import static org.omnifaces.component.output.PathParam.PATH_PARAM_NAME_ATTRIBUTE_PREFIX;
import static org.omnifaces.util.FacesLocal.getServletContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.faces.application.ConfigurableNavigationHandler;
import jakarta.faces.application.ConfigurableNavigationHandlerWrapper;
import jakarta.faces.application.NavigationCase;
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
public class DynamicRoutesNavigationHandler extends ConfigurableNavigationHandlerWrapper {

    /**
     * Construct the dynamic routes navigation handler.
     *
     * @param wrapped The navigation handler to be wrapped.
     */
    public DynamicRoutesNavigationHandler(ConfigurableNavigationHandler wrapped) {
        super(wrapped);
    }

    @Override
    public NavigationCase getNavigationCase(FacesContext context, String fromAction, String outcome) {
        NavigationCase navigationCase = super.getNavigationCase(context, fromAction, outcome);
        return navigationCase != null ? navigationCase : getDynamicRouteNavigationCase(context, fromAction, outcome);
    }

    @Override
    public NavigationCase getNavigationCase(FacesContext context, String fromAction, String outcome, String toFlowDocumentId) {
        NavigationCase navigationCase = super.getNavigationCase(context, fromAction, outcome, toFlowDocumentId);
        return navigationCase != null ? navigationCase : getDynamicRouteNavigationCase(context, fromAction, outcome);
    }

    private static NavigationCase getDynamicRouteNavigationCase(FacesContext context, String fromAction, String outcome) {
        if (outcome == null || outcome.isEmpty() || outcome.charAt(0) != '/') {
            return null;
        }

        DynamicRoutes dynamicRoutes = DynamicRoutes.get(getServletContext(context));

        if (dynamicRoutes == null || dynamicRoutes.isEmpty()) {
            return null;
        }

        DynamicRoutes.Match match = dynamicRoutes.match(outcome);

        if (match == null) {
            return null;
        }

        Map<String, List<String>> parameters = new LinkedHashMap<>();

        for (var param : match.params().entrySet()) {
            parameters.put(PATH_PARAM_NAME_ATTRIBUTE_PREFIX + param.getKey(), List.of(param.getValue()));
        }

        var fromViewId = context.getViewRoot() != null ? context.getViewRoot().getViewId() : null;
        return new NavigationCase(fromViewId, fromAction, outcome, null, match.viewId(), null, unmodifiableMap(parameters), false, false);
    }

}
