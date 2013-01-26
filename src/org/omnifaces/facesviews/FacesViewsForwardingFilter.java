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

package org.omnifaces.facesviews;

import static javax.faces.application.ProjectStage.Development;
import static org.omnifaces.facesviews.FacesViewsUtils.FACES_VIEWS_RESOURCES;
import static org.omnifaces.facesviews.FacesViewsUtils.getApplication;
import static org.omnifaces.facesviews.FacesViewsUtils.getApplicationAttribute;
import static org.omnifaces.facesviews.FacesViewsUtils.isExtensionless;
import static org.omnifaces.facesviews.FacesViewsUtils.scanAndStoreViews;
import static org.omnifaces.facesviews.FacesViewsUtils.tryScanAndStoreViews;

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
 * A filter like this is needed for extensionless requests, since the FacesServlet in at least JSF 2.1 and before
 * does not take into account any other mapping than prefix- and extension (suffix) mapping.
 * <p>
 * For a guide on FacesViews, please see the <a href="package-summary.html">package summary</a>.
 *
 * @author Arjan Tijms
 *
 */
public class FacesViewsForwardingFilter extends HttpFilter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    	
    	super.init(filterConfig);

        // Mostly for pre-Servlet 3.0: scan the views if the auto-configure listener hasn't done this yet.
        tryScanAndStoreViews(filterConfig.getServletContext());

        // Register a view handler that transforms a view id with extension back to an extensionless one.

        // Note that Filter#init is used here, since it loads after the ServletContextListener that initializes JSF itself,
        // and thus guarantees the {@link Application} instance needed for installing the FacesViewHandler is available.

        Application application = getApplication();
        application.setViewHandler(new FacesViewsViewHandler(application.getViewHandler()));
        
        // In development mode additionally map this Filter to "*", so we can catch requests to extensionless resources that 
        // have been dynamically added. Note that resources with mapped extensions are already handled by the FacesViewsResolver.
        // Adding resources with new extensions still requires a restart.
        if (application.getProjectStage() == Development) {
        	filterConfig.getServletContext().getFilterRegistration(FacesViewsForwardingFilter.class.getName())
						.addMappingForUrlPatterns(null, false, "*");
        }
    }

    @Override
    public void doFilter(HttpServletRequest request, HttpServletResponse response, HttpSession session, FilterChain chain) throws ServletException,
            IOException {

        String resource = request.getServletPath();
        if (isExtensionless(resource)) {
        	
        	ServletContext context = getServletContext();
            Map<String, String> resources = getApplicationAttribute(context, FACES_VIEWS_RESOURCES);
        	
        	if (getApplication().getProjectStage() == Development && !resources.containsKey(resource)) {
        		// Check if the resource was dynamically added by scanning the faces-views location(s) again.
        		resources = scanAndStoreViews(context);
        	}
        	
        	if (resources.containsKey(resource)) {
	            // Forward the resource (view) using its original extension, on which the Facelets Servlet
	            // is mapped. Technically it matters most that the Facelets Servlet picks up the
	            // request, and the exact extension or even prefix is perhaps less relevant.
	            String forwardURI = resource + FacesViewsUtils.getExtension(resources.get(resource));
	
	            // Get the request dispatcher
	            RequestDispatcher requestDispatcher = context.getRequestDispatcher(forwardURI);
	            if (requestDispatcher != null) {
	                // Forward the request to FacesServlet
	                requestDispatcher.forward(request, response);
	                return;
	            }
        	}
        }

        chain.doFilter(request, response);
    }

}