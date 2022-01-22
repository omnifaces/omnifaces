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
package org.omnifaces.resourcehandler;

import static java.lang.String.format;
import static org.omnifaces.util.FacesLocal.getContextAttribute;
import static org.omnifaces.util.FacesLocal.getRequest;
import static org.omnifaces.util.FacesLocal.getRequestServletPath;
import static org.omnifaces.util.FacesLocal.getResource;
import static org.omnifaces.util.Platform.getFacesServletRegistration;
import static org.omnifaces.util.Utils.isEmpty;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.faces.application.ResourceHandler;
import javax.faces.application.ViewResource;
import javax.faces.context.FacesContext;
import javax.faces.webapp.FacesServlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

import org.omnifaces.ApplicationListener;
import org.omnifaces.util.Faces;
import org.omnifaces.viewhandler.OmniViewHandler;

/**
 * This {@link ResourceHandler} basically turns any concrete non-Facelets file into a JSF view, so that you can use EL expressions and even JSF components in them.
 * The response content type will default to {@link Faces#getMimeType(String)} which is configureable in <code>web.xml</code> and overrideable via <code>&lt;f:view contentType="..."&gt;</code>.
 * <p>
 * Real world examples are <code>/sitemap.xml</code> and <code>/robots.txt</code>.
 *
 * <h3>Installation</h3>
 * <p>
 * To get it to run, this handler needs be registered as follows in <code>faces-config.xml</code>:
 * <pre>
 * &lt;application&gt;
 *     &lt;resource-handler&gt;org.omnifaces.resourcehandler.ViewResourceHandler&lt;/resource-handler&gt;
 * &lt;/application&gt;
 * </pre>
 * <p>
 * To configure the JSF view resources, a {@value org.omnifaces.resourcehandler.ViewResourceHandler#PARAM_NAME_VIEW_RESOURCES}
 * context parameter has to be provided wherein the view resources are specified as a comma separated string of context-relative URIs.
 * <p>
 * Here is an example configuration:
 * <pre>
 * &lt;context-param&gt;
 *     &lt;param-name&gt;org.omnifaces.VIEW_RESOURCE_HANDLER_URIS&lt;/param-name&gt;
 *     &lt;param-value&gt;/sitemap.xml, /products/sitemap.xml, /reviews/sitemap.xml, /robots.txt&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * </pre>
 * <p>
 * Wildcards in URIs are at the moment not supported.
 * <p>
 * The {@link OmniViewHandler} will take care of rendering the view.
 *
 *
 * @author Bauke Scholtz
 * @since 3.10
 * @see DefaultResourceHandler
 * @see OmniViewHandler
 */
public class ViewResourceHandler extends DefaultResourceHandler {

	/** The context parameter name to specify URIs to treat as JSF views. */
	public static final String PARAM_NAME_VIEW_RESOURCES = "org.omnifaces.VIEW_RESOURCE_HANDLER_URIS";

	private static final String ERROR_MISSING_FORWARD_SLASH = "View resource '%s' must start with a forward slash '/'.";
	private static final String ERROR_UNKNOWN_VIEW_RESOURCE = "View resource '%s' does not exist.";

	private static final Set<String> VIEW_RESOURCES = new HashSet<>();
	private static final ViewResource VIEW_RESOURCE = new ViewResource() {
		@Override
		public URL getURL() {
			try {
				FacesContext context = Faces.getContext();
				return getResource(context, getRequestServletPath(context));
			}
			catch (MalformedURLException e) {
				throw new IllegalStateException(e);
			}
		}
	};

	/**
	 * This will map the {@link FacesServlet} to the URIs specified in {@value org.omnifaces.resourcehandler.ViewResourceHandler#PARAM_NAME_VIEW_RESOURCES}
	 * context parameter.
	 * This is invoked by {@link ApplicationListener}, because the faces servlet registration has to be available for adding new mappings.
	 * @param servletContext The involved servlet context.
	 * @throws MalformedURLException When one of the URIs specified in context parameter is malformed.
	 */
	public static void addFacesServletMappingsIfNecessary(ServletContext servletContext) throws MalformedURLException {
		String viewResourcesParam = servletContext.getInitParameter(PARAM_NAME_VIEW_RESOURCES);

		if (isEmpty(viewResourcesParam)) {
			return;
		}

		ServletRegistration facesServletRegistration = getFacesServletRegistration(servletContext);

		if (facesServletRegistration != null) {
			Collection<String> existingMappings = facesServletRegistration.getMappings();

			for (String viewResource : viewResourcesParam.split("\\s*,\\s*")) {
				if (!viewResource.startsWith("/")) {
					throw new IllegalArgumentException(format(ERROR_MISSING_FORWARD_SLASH, viewResource));
				}

				if (servletContext.getResource(viewResource) == null) {
					throw new IllegalArgumentException(format(ERROR_UNKNOWN_VIEW_RESOURCE, viewResource));
				}

				VIEW_RESOURCES.add(viewResource);

				if (!existingMappings.contains(viewResource)) {
					facesServletRegistration.addMapping(viewResource);
				}
			}
		}
	}

	/**
	 * Returns <code>true</code> if the current HTTP request is requesting for a view resource managed by this resource handler.
	 * @param context The involved faces context.
	 * @return <code>true</code> if the current HTTP request is requesting for a view resource managed by this resource handler.
	 */
	public static boolean isViewResourceRequest(FacesContext context) {
		return !VIEW_RESOURCES.isEmpty() && getContextAttribute(context, ViewResourceHandler.class.getName(), () -> getRequest(context) != null && VIEW_RESOURCES.contains(getRequest(context).getServletPath()));
	}

	/**
	 * Creates a new instance of this view resource handler which wraps the given resource handler.
	 * @param wrapped The resource handler to be wrapped.
	 */
	public ViewResourceHandler(ResourceHandler wrapped) {
		super(wrapped);
	}

	/**
	 * This override ensures that {@link Faces#getRequestServletPath()} is returned as concrete resource rather than the provided <code>resourceName</code>
	 * when the {@link #isViewResourceRequest(FacesContext)} returns true.
	 */
	@Override
	public ViewResource createViewResource(FacesContext context, String resourceName) {
		if (isViewResourceRequest(context)) {
			return VIEW_RESOURCE;
		}
		else {
			return super.createViewResource(context, resourceName);
		}
	}

}