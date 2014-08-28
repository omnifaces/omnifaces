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
package org.omnifaces.resourcehandler;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NOT_MODIFIED;
import static org.omnifaces.util.Faces.getMapping;
import static org.omnifaces.util.Faces.isPrefixMapping;
import static org.omnifaces.util.Utils.stream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;

import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.webapp.FacesServlet;

import org.omnifaces.util.Hacks;

/**
 * The <strong>UnmappedResourceHandler</strong> allows the developer to map JSF resources on an URL pattern of
 * <code>/javax.faces.resource/*</code> without the need for an additional {@link FacesServlet} prefix or suffix URL
 * pattern in the default produced resource URLs, such as <code>/javax.faces.resource/faces/css/style.css</code> or
 * <code>/javax.faces.resource/css/style.css.xhtml</code>. This resource handler will produce unmapped URLs like
 * <code>/javax.faces.resource/css/style.css</code>. This has the major advantage that the developer don't need the
 * <code>#{resource}</code> EL expression anymore in order to properly reference relative URLs to images in CSS files.
 * Given the following folder structure,
 * <pre>
 * WebContent
 *  `-- resources
 *       `-- css
 *            |-- images
 *            |    `-- background.png
 *            `-- style.css
 * </pre>
 * <p>And the following CSS file reference (note: resource libraries are <strong>not</strong> supported by the
 * <code>UnmappedResourceHandler</code>! this is a technical limitation):
 * <pre>
 * &lt;h:outputStylesheet name="css/style.css" /&gt;
 * </pre>
 * <p>you can in <code>css/style.css</code> just use:
 * <pre>
 * body {
 *     background: url("images/background.png");
 * }
 * </pre>
 * <p>instead of
 * <pre>
 * body {
 *     background: url("#{resource['css/images/background.png']}");
 * }
 * </pre>
 * <h3>Configuration</h3>
 * <p>
 * To get it to run, this handler needs be registered as follows in <code>faces-config.xml</code>:
 * <pre>
 * &lt;application&gt;
 *   &lt;resource-handler&gt;org.omnifaces.resourcehandler.UnmappedResourceHandler&lt;/resource-handler&gt;
 * &lt;/application&gt;
 * </pre>
 * <p>
 * And the {@link FacesServlet} needs to have an additional mapping <code>/javax.faces.resource/*</code> in
 * <code>web.xml</code>. For example, assuming that you've already a mapping on <code>*.xhtml</code>:
 * <pre>
 * &lt;servlet-mapping&gt;
 *   &lt;servlet-name&gt;facesServlet&lt;/servlet-name&gt;
 *   &lt;url-pattern&gt;*.xhtml&lt;/url-pattern&gt;
 *   &lt;url-pattern&gt;/javax.faces.resource/*&lt;/url-pattern&gt;
 * &lt;/servlet-mapping&gt;
 * </pre>
 * <h3>CombinedResourceHandler</h3>
 * <p>
 * If you're also using the {@link CombinedResourceHandler} or any other custom resource handler, then you need to
 * ensure that this is in <code>faces-config.xml</code> declared <strong>before</strong> the
 * <code>UnmappedResourceHandler</code>. Thus, like so:
 * <pre>
 * &lt;application&gt;
 *   &lt;resource-handler&gt;org.omnifaces.resourcehandler.CombinedResourceHandler&lt;/resource-handler&gt;
 *   &lt;resource-handler&gt;org.omnifaces.resourcehandler.UnmappedResourceHandler&lt;/resource-handler&gt;
 * &lt;/application&gt;
 * </pre>
 * <p>Otherwise the combined resource handler will still produce mapped URLs. In essence, the one which is later
 * registered wraps the previously registered one.
 *
 * @author Bauke Scholtz
 * @since 1.4
 */
public class UnmappedResourceHandler extends DefaultResourceHandler {

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Creates a new instance of this unmapped resource handler which wraps the given resource handler.
	 * @param wrapped The resource handler to be wrapped.
	 */
	public UnmappedResourceHandler(ResourceHandler wrapped) {
		super(wrapped);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Delegate to {@link #createResource(String)} of the wrapped resource handler. If it returns
	 * non-<code>null</code>, then return an unmapped resource.
	 */
	@Override
	public Resource createResource(String resourceName) {
		return unmapIfNecessary(super.createResource(resourceName));
	}

	/**
	 * Delegate to {@link #createResource(String, String)} of the wrapped resource handler. If it returns
	 * non-<code>null</code>, then return an unmapped resource.
	 */
	@Override
	public Resource createResource(String resourceName, String libraryName) {
		return unmapIfNecessary(super.createResource(resourceName, libraryName));
	}

	/**
	 * Delegate to {@link #createResource(String, String, String)} of the wrapped resource handler. If it returns
	 * non-<code>null</code>, then return a new instanceof {@link UnmappedResource}.
	 */
	@Override
	public Resource createResource(String resourceName, String libraryName, String contentType) {
		return unmapIfNecessary(super.createResource(resourceName, libraryName, contentType));
	}

	private static Resource unmapIfNecessary(Resource resource) {
		return (resource == null) ? null : new DefaultResource(resource) {
			@Override
			public String getRequestPath() {
				String path = getWrapped().getRequestPath();
				String mapping = getMapping();

				if (isPrefixMapping(mapping)) {
					return path.replaceFirst(mapping, "");
				}
				else if (path.contains("?")) {
					return path.replace(mapping + "?", "?");
				}
				else {
					return path.substring(0, path.length() - mapping.length());
				}
			}
		};
	}

	/**
	 * Returns <code>true</code> if {@link ExternalContext#getRequestServletPath()} starts with value of
	 * {@link ResourceHandler#RESOURCE_IDENTIFIER}.
	 */
	@Override
	public boolean isResourceRequest(FacesContext context) {
		return RESOURCE_IDENTIFIER.equals(context.getExternalContext().getRequestServletPath());
	}

	@Override
	public void handleResourceRequest(FacesContext context) throws IOException {
		Resource resource = createResource(context);

		if (resource == null) {
			getWrapped().handleResourceRequest(context);
			return;
		}

		ExternalContext externalContext = context.getExternalContext();

		if (!resource.userAgentNeedsUpdate(context)) {
			externalContext.setResponseStatus(SC_NOT_MODIFIED);
			return;
		}

		InputStream inputStream = resource.getInputStream();

		if (inputStream == null) {
			externalContext.setResponseStatus(SC_NOT_FOUND);
			return;
		}

		externalContext.setResponseContentType(resource.getContentType());

		for (Entry<String, String> header : resource.getResponseHeaders().entrySet()) {
			externalContext.setResponseHeader(header.getKey(), header.getValue());
		}

		stream(inputStream, externalContext.getResponseOutputStream());
	}

	private Resource createResource(FacesContext context) {
		if (Hacks.isPrimeFacesDynamicResourceRequest(context)) {
			return null;
		}

		String pathInfo = context.getExternalContext().getRequestPathInfo();
		String resourceName = (pathInfo != null) ? pathInfo.substring(1) : "";

		if (resourceName.isEmpty()) {
			return null;
		}

		String libraryName = context.getExternalContext().getRequestParameterMap().get("ln");
		return context.getApplication().getResourceHandler().createResource(resourceName, libraryName);
	}

}
