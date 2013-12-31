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

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static javax.faces.application.ProjectStage.Development;
import static javax.faces.application.ProjectStage.PROJECT_STAGE_PARAM_NAME;
import static javax.faces.view.facelets.ResourceResolver.FACELETS_RESOURCE_RESOLVER_PARAM_NAME;
import static org.omnifaces.facesviews.FacesServletDispatchMethod.DO_FILTER;
import static org.omnifaces.facesviews.FacesViews.FACES_VIEWS_ENABLED_PARAM_NAME;
import static org.omnifaces.facesviews.FacesViews.FACES_VIEWS_RESOURCES;
import static org.omnifaces.facesviews.FacesViews.FACES_VIEWS_RESOURCES_EXTENSIONS;
import static org.omnifaces.facesviews.FacesViews.FACES_VIEWS_REVERSE_RESOURCES;
import static org.omnifaces.facesviews.FacesViews.getFacesServletDispatchMethod;
import static org.omnifaces.facesviews.FacesViews.getPublicRootPaths;
import static org.omnifaces.facesviews.FacesViews.isFilterAfterDeclaredFilters;
import static org.omnifaces.facesviews.FacesViews.scanViewsFromRootPaths;
import static org.omnifaces.util.ResourcePaths.isExtensionless;
import static org.omnifaces.util.Utils.reverse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * Convenience class for Servlet 3.0 users, which will auto-register most artifacts
 * required for auto-mapping and extensionless view to work.
 * <p>
 * For a guide on FacesViews, please see the <a href="package-summary.html">package summary</a>.
 *
 * @author Arjan Tijms
 * @since 1.4
 */
public class FacesViewsInitializer implements ServletContainerInitializer {

	@Override
	public void onStartup(Set<Class<?>> c, ServletContext servletContext) throws ServletException {

		if (!"false".equals(servletContext.getInitParameter(FACES_VIEWS_ENABLED_PARAM_NAME))) {

			// Scan our dedicated directory for Faces resources that need to be mapped
			Map<String, String> collectedViews = new HashMap<String, String>();
			Set<String> collectedExtensions = new HashSet<String>();
			scanViewsFromRootPaths(servletContext, collectedViews, collectedExtensions);

			if (!collectedViews.isEmpty()) {

				// Store the resources and extensions that were found in application scope, where others can find it.
				servletContext.setAttribute(FACES_VIEWS_RESOURCES, unmodifiableMap(collectedViews));
				servletContext.setAttribute(FACES_VIEWS_REVERSE_RESOURCES, unmodifiableMap(reverse(collectedViews)));
				servletContext.setAttribute(FACES_VIEWS_RESOURCES_EXTENSIONS, unmodifiableSet(collectedExtensions));

				// Register 3 artifacts with the Servlet container and JSF that help implement this feature:

				// 1. A Filter that forwards extensionless requests to an extension mapped request, e.g. /index to
				// /index.xhtml
				// (The FacesServlet doesn't work well with the exact mapping that we use for extensionless URLs).
				FilterRegistration facesViewsRegistration = servletContext.addFilter(FacesViewsForwardingFilter.class.getName(),
						FacesViewsForwardingFilter.class);

				// 2. A Facelets resource resolver that resolves requests like /index.xhtml to
				// /WEB-INF/faces-views/index.xhtml
				servletContext.setInitParameter(FACELETS_RESOURCE_RESOLVER_PARAM_NAME, FacesViewsResolver.class.getName());

				// 3. A ViewHandler that transforms the forwarded extension based URL back to an extensionless one, e.g.
				// /index.xhtml to /index
				// See FacesViewsForwardingFilter#init


				if (Development.name().equals(servletContext.getInitParameter(PROJECT_STAGE_PARAM_NAME)) &&
					getFacesServletDispatchMethod(servletContext) != DO_FILTER) {

					// In development mode map this Filter to "*", so we can catch requests to extensionless resources that
			        // have been dynamically added. Note that resources with mapped extensions are already handled by the FacesViewsResolver.
			        // Adding resources with new extensions still requires a restart.

					// Development mode only works when the dispatch mode is not DO_FILTER, since DO_FILTER mode depends
					// on the Faces Servlet being "exact"-mapped on the view resources.

					facesViewsRegistration.addMappingForUrlPatterns(null, isFilterAfterDeclaredFilters(servletContext), "/*");
				} else {

					// In non-development mode, only map this Filter to specific resources

					// Map the forwarding filter to all the resources we found.
					for (String resource : collectedViews.keySet()) {
						String pattern = isExtensionless(resource) ? (resource + "/*") : resource;
						facesViewsRegistration.addMappingForUrlPatterns(null, isFilterAfterDeclaredFilters(servletContext), pattern);
					}

					// Additionally map the filter to all paths that were scanned and which are also directly
					// accessible. This is to give the filter an opportunity to block these.
					for (String path : getPublicRootPaths(servletContext)) {
						String pattern = path + "*";
						facesViewsRegistration.addMappingForUrlPatterns(null, false, pattern);
					}
				}

				// We now need to map the Faces Servlet to the extensions we found, but at this point in time
				// this Faces Servlet might not be created yet, so we do this part in FacesViewInitializedListener.
			}
		}
	}

}