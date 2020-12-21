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
package org.omnifaces.resourcehandler;

import static org.omnifaces.util.Faces.getResource;
import static org.omnifaces.util.FacesLocal.getContextAttribute;
import static org.omnifaces.util.FacesLocal.getRequestServletPath;
import static org.omnifaces.util.Platform.getFacesServletRegistration;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

import javax.faces.application.ResourceHandler;
import javax.faces.application.ViewResource;
import javax.faces.context.FacesContext;
import javax.faces.webapp.FacesServlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

import org.omnifaces.ApplicationListener;
import org.omnifaces.component.output.SitemapUrl;
import org.omnifaces.config.FacesConfigXml;

/**
 * This {@link ResourceHandler} basically turns the <code>/sitemap.xml</code> into a JSF view, so that you can use JSF components in there.
 * This will allow using among others the <code>&lt;o:sitemapUrl&gt;</code> component in the <code>/sitemap.xml</code> page.
 *
 * <h3>Installation</h3>
 * <p>
 * To get it to run, this handler needs be registered as follows in <code>faces-config.xml</code>:
 * <pre>
 * &lt;application&gt;
 *     &lt;resource-handler&gt;org.omnifaces.resourcehandler.SitemapResourceHandler&lt;/resource-handler&gt;
 * &lt;/application&gt;
 * </pre>
 *
 * <h3>Usage</h3>
 * <p>
 * Now you can use the <code>/sitemap.xml</code> as a JSF view and utilize among others the {@link SitemapUrl} component as <code>&lt;o:sitemapUrl&gt;</code>.
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 * &lt;urlset
 *     xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"
 *     xmlns:ui="http://xmlns.jcp.org/jsf/facelets"
 *     xmlns:o="http://omnifaces.org/ui"
 * &gt;
 *     &lt;ui:repeat value="#{sitemapBean.products}" var="product"&gt;
 *         &lt;o:sitemapUrl viewId="/product.xhtml" lastModified="#{product.lastModified}" changeFrequency="weekly" priority="1.0"&gt;
 *             &lt;o:param name="id" value="#{product}" converter="productConverter" /&gt;
 *         &lt;/o:sitemapUrl&gt;
 *     &lt;/ui:repeat&gt;
 * &lt;/urlset&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 * @since 3.10
 * @see DefaultResourceHandler
 * @see SitemapUrl
 */
public class SitemapResourceHandler extends DefaultResourceHandler {

	/**
	 * The URI of the <code>/sitemap.xml</code>.
	 */
	public static final String URI = "/sitemap.xml";

	/**
	 * The <code>/sitemap.xml</code> as a concrete view resource.
	 */
	private static final ViewResource RESOURCE = new ViewResource() {
		@Override
		public URL getURL() {
			try {
				return getResource(URI);
			}
			catch (MalformedURLException e) {
				throw new IllegalStateException(e);
			}
		}
	};

	/**
	 * This will map the {@link FacesServlet} to <code>/sitemap.xml</code> if the {@link SitemapResourceHandler} is registered in <code>faces-config.xml</code>.
	 * This is invoked by {@link ApplicationListener}, because the faces servlet registration has to be available for adding a new mapping.
	 * @param servletContext The involved servlet context.
	 */
	public static void addFacesServletMappingIfNecessary(ServletContext servletContext) {
		if (!isSitemapResourceHandlerRegistered()) {
			return;
		}

		ServletRegistration facesServletRegistration = getFacesServletRegistration(servletContext);

		if (facesServletRegistration != null) {
			Collection<String> existingMappings = facesServletRegistration.getMappings();

			if (!existingMappings.contains(URI)) {
				facesServletRegistration.addMapping(URI); // This will cause the createViewResource() to be triggered.
			}
		}
	}

	/**
	 * Returns <code>true</code> if the {@link SitemapResourceHandler} is registered in <code>faces-config.xml</code>.
	 * @return <code>true</code> if the {@link SitemapResourceHandler} is registered in <code>faces-config.xml</code>.
	 */
	public static boolean isSitemapResourceHandlerRegistered() {
		return FacesConfigXml.instance().getResourceHandlers().contains(SitemapResourceHandler.class);
	}

	/**
	 * Returns <code>true</code> if the current HTTP request is requesting for the <code>/sitemap.xml</code>.
	 * @param context The involved faces context.
	 * @return <code>true</code> if the current HTTP request is requesting for the <code>/sitemap.xml</code>.
	 */
	public static boolean isSitemapResourceRequest(FacesContext context) {
		return getContextAttribute(context, SitemapResourceHandler.class.getName(), () -> URI.equals(getRequestServletPath(context)));
	}

	/**
	 * Creates a new instance of this sitemap resource handler which wraps the given resource handler.
	 * @param wrapped The resource handler to be wrapped.
	 */
	public SitemapResourceHandler(ResourceHandler wrapped) {
		super(wrapped);
	}

	/**
	 * This override returns the <code>/sitemap.xml</code> as a concrete view resource when being requested.
	 * This will basically enable using JSF components in the page.
	 */
	@Override
	public ViewResource createViewResource(FacesContext context, String resourceName) {
		if (isSitemapResourceRequest(context)) {
			return RESOURCE;
		}
		else {
			return super.createViewResource(context, resourceName);
		}
	}

}