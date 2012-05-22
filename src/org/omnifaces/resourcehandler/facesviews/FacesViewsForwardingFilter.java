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

package org.omnifaces.resourcehandler.facesviews;

import static java.util.Collections.unmodifiableMap;
import static org.omnifaces.resourcehandler.facesviews.FacesViewsResolver.FACES_VIEWS_RESOURCES_PARAM_NAME;
import static org.omnifaces.resourcehandler.facesviews.FacesViewsUtils.WEB_INF_VIEWS;
import static org.omnifaces.resourcehandler.facesviews.FacesViewsUtils.getApplication;
import static org.omnifaces.resourcehandler.facesviews.FacesViewsUtils.getApplicationAttribute;
import static org.omnifaces.resourcehandler.facesviews.FacesViewsUtils.isExtensionless;
import static org.omnifaces.resourcehandler.facesviews.FacesViewsUtils.scanViews;

import java.io.IOException;
import java.util.Map;

import javax.faces.application.Application;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.omnifaces.filter.HttpFilter;

/**
 * This filter forwards request to a FacesServlet using an extension on which this Servlet is mapped.
 * <p>
 * A filter like this is needed for extentionless requests, since the FacesServlet in at least JSF 2.1 and before
 * does not take into account any other mapping than prefix- and extension (suffix) mapping.
 * 
 * @author Arjan Tijms
 *
 */
public class FacesViewsForwardingFilter extends HttpFilter {
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        
        // Mostly for pre-Servlet 3.0: scan the views if the auto-configure listener hasn't done this yet.
        tryScanAndStoreViews(filterConfig.getServletContext());
        
        // Register a view handler that transforms a view id with extension back to an extensionless one.
        
        // Note that Filter#init is used here, since it loads after the ServletContextListener that initializes JSF itself,
        // and thus guarantees the {@link Application} instance needed for installing the FacesViewHandler is available.
        
        Application application = getApplication();        
        application.setViewHandler(new FacesViewsViewHandler(application.getViewHandler()));
    }

    @Override
    public void doFilter(HttpServletRequest request, HttpServletResponse response, HttpSession session, FilterChain chain) throws ServletException,
            IOException {
        
        ServletContext context = request.getServletContext();
        Map<String, String> resources = getApplicationAttribute(context, FACES_VIEWS_RESOURCES_PARAM_NAME);

        String resource = request.getServletPath();
        if (isExtensionless(resource) && resources.containsKey(resource)) {
            
            // Forward the resource (view) using its original extension, on which the Facelets Servlet
            // is mapped. Technically it matters most that the Facelets Servlet picks up the
            // request, and the exact extension of even prefix is perhaps less relevant.
            String forwardURI = resource + FacesViewsUtils.getExtension(resources.get(resource));
            
            // Get the request dispatcher
            RequestDispatcher requestDispatcher = context.getRequestDispatcher(forwardURI);
            if (requestDispatcher != null) {
                // Forward the request to FacesServlet
                requestDispatcher.forward(request, response);
                return;
            }
        }
        
        chain.doFilter(request, response);

    }
    
    private void tryScanAndStoreViews(ServletContext context) {
        if (getApplicationAttribute(context, FACES_VIEWS_RESOURCES_PARAM_NAME) == null) {
            Map<String, String> views = scanViews(context, context.getResourcePaths(WEB_INF_VIEWS));
            if (!views.isEmpty()) {
                context.setAttribute(FACES_VIEWS_RESOURCES_PARAM_NAME, unmodifiableMap(views));
            }
        }
        
    }

}
