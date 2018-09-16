/*
 * Copyright 2018 OmniFaces
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

import static java.util.stream.Collectors.toList;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NOT_MODIFIED;
import static org.omnifaces.util.Faces.getMapping;
import static org.omnifaces.util.Faces.getRequestContextPath;
import static org.omnifaces.util.Faces.isPrefixMapping;
import static org.omnifaces.util.FacesLocal.getRequestURI;
import static org.omnifaces.util.Utils.stream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.webapp.FacesServlet;

import org.omnifaces.util.FacesLocal;
import org.omnifaces.util.Hacks;

/**
 * <p>
 * This {@link ResourceHandler} implementation allows the developer to map JSF resources on an URL pattern of
 * <code>/javax.faces.resource/*</code> (basically, the value of {@link ResourceHandler#RESOURCE_IDENTIFIER}) without
 * the need for an additional {@link FacesServlet} prefix or suffix URL pattern in the default produced resource URLs,
 * such as <code>/javax.faces.resource/faces/css/style.css</code> or
 * <code>/javax.faces.resource/css/style.css.xhtml</code>. This resource handler will produce unmapped URLs like
 * <code>/javax.faces.resource/css/style.css</code>. This has the major advantage that the developer don't need the
 * <code>#{resource}</code> EL expression anymore in order to properly reference relative URLs to images in CSS files.
 * <p>
 * So, given the following folder structure,
 * <pre>
 * WebContent
 *  `-- resources
 *       `-- css
 *            |-- images
 *            |    `-- background.png
 *            `-- style.css
 * </pre>
 * <p>And the following CSS file reference (note: the <code>library</code> is <strong>not</strong> supported by the
 * <code>UnmappedResourceHandler</code>! this is a technical limitation, just exclusively use <code>name</code>):
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
 * <p>
 * This has in turn the advantage that you don't need to modify the background image or font face URLs in CSS files from
 * 3rd party libraries such as Twitter Bootstrap, FontAwesome, etcetera.
 *
 * <h3>Installation</h3>
 * <p>
 * To get it to run, this handler needs be registered as follows in <code>faces-config.xml</code>:
 * <pre>
 * &lt;application&gt;
 *     &lt;resource-handler&gt;org.omnifaces.resourcehandler.UnmappedResourceHandler&lt;/resource-handler&gt;
 * &lt;/application&gt;
 * </pre>
 * <p>
 * And the {@link FacesServlet} needs to have an additional mapping <code>/javax.faces.resource/*</code> in
 * <code>web.xml</code>. You can just add it as a new <code>&lt;url-pattern&gt;</code> entry to the existing mapping
 * of the {@link FacesServlet}. For example, assuming that you've already a mapping on <code>*.xhtml</code>:
 * <pre>
 * &lt;servlet-mapping&gt;
 *     ...
 *     &lt;url-pattern&gt;*.xhtml&lt;/url-pattern&gt;
 *     &lt;url-pattern&gt;/javax.faces.resource/*&lt;/url-pattern&gt;
 * &lt;/servlet-mapping&gt;
 * </pre>
 *
 * <h3>CombinedResourceHandler</h3>
 * <p>
 * If you're also using the {@link CombinedResourceHandler} or any other custom resource handler, then you need to
 * ensure that this is in <code>faces-config.xml</code> declared <strong>before</strong> the
 * <code>UnmappedResourceHandler</code>. Thus, like so:
 * <pre>
 * &lt;application&gt;
 *     &lt;resource-handler&gt;org.omnifaces.resourcehandler.CombinedResourceHandler&lt;/resource-handler&gt;
 *     &lt;resource-handler&gt;org.omnifaces.resourcehandler.UnmappedResourceHandler&lt;/resource-handler&gt;
 * &lt;/application&gt;
 * </pre>
 * <p>
 * Otherwise the combined resource handler will still produce mapped URLs. In essence, the one which is later
 * registered wraps the previously registered one.
 * This resource handler handle only harmless resources.
 * Blacklist could be adapted by add context-param to web.xml (or fragment), this is the common JSF configuration parameter
 * <p>
 *     <code>
 *         &lt;context-param&gt;<br/>
 *         &lt;param-name&gt;javax.faces.RESOURCE_EXCLUDES&lt;/param-name&gt;<br/>
 *         &lt;param-value&gt;.class .jsp .jspx .properties .xhtml .groovy&lt;/param-value&gt;<br/>
 *         &lt;/context-param&gt;
 *      </code>
 * </p>
 *
 * @author Bauke Scholtz
 * @author Eugen Fischer
 * @since 1.4
 * @see RemappedResource
 * @see DefaultResourceHandler
 */
public class UnmappedResourceHandler extends DefaultResourceHandler {


	private final static List<Pattern> EXCLUDE_RESOURCES = initExclusionPatterns();

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Creates a new instance of this unmapped resource handler which wraps the given resource handler.
	 * @param wrapped The resource handler to be wrapped.
	 */
	public UnmappedResourceHandler(final ResourceHandler wrapped) {
		super(wrapped);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * If the given resource is not <code>null</code>, then decorate it as an unmapped resource.
	 */
	@Override
	public Resource decorateResource(final Resource resource) {
		if (resource == null ) {
			return null;
		}

		final String path = resource.getRequestPath();
		return isResourceRequest(path) ? new RemappedResource(resource, unmapRequestPath(path)) : resource;
	}

	@Override
	public boolean isResourceRequest(final FacesContext context) {
		return isResourceRequest(getRequestURI(context)) || super.isResourceRequest(context);
	}

	@Override
	public void handleResourceRequest(final FacesContext context) throws IOException {
		final String requestURI = FacesLocal.getRequestURI(context);
		if(isNotExcluded(requestURI)) {

			final Resource resource = createResource(context);

			if (resource == null) {
				super.handleResourceRequest(context);
				return;
			}

			final ExternalContext externalContext = context.getExternalContext();

			if (!resource.userAgentNeedsUpdate(context)) {
				externalContext.setResponseStatus(SC_NOT_MODIFIED);
				return;
			}

			final InputStream inputStream = resource.getInputStream();

			if (inputStream == null) {
				externalContext.setResponseStatus(SC_NOT_FOUND);
				return;
			}

			externalContext.setResponseContentType(resource.getContentType());

			for (final Entry<String, String> header : resource.getResponseHeaders().entrySet()) {
				externalContext.setResponseHeader(header.getKey(), header.getValue());
			}

			stream(inputStream, externalContext.getResponseOutputStream());
		}else{
			getWrapped().handleResourceRequest(context);
		}
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	private static boolean isResourceRequest(final String path) {
		return path.startsWith(getRequestContextPath() + RESOURCE_IDENTIFIER) && isNotExcluded(path);
	}

	private static String unmapRequestPath(final String path) {
		final String mapping = getMapping();

		if (isPrefixMapping(mapping)) {
			return path.replaceFirst(mapping, "");
		}
		else if (path.contains("?")) {
			return path.replace(mapping + "?", "?");
		}
		else if (path.endsWith(mapping)) {
			return path.substring(0, path.length() - mapping.length());
		}
		else {
			return path;
		}
	}

	private static Resource createResource(final FacesContext context) {
		if (Hacks.isPrimeFacesDynamicResourceRequest(context)) {
			return null;
		}

		final String pathInfo = context.getExternalContext().getRequestPathInfo();
		final String resourceName = (pathInfo != null) ? pathInfo.substring(1) : "";

		if (resourceName.isEmpty()) {
			return null;
		}

		final String libraryName = context.getExternalContext().getRequestParameterMap().get("ln");
		return context.getApplication().getResourceHandler().createResource(resourceName, libraryName);
	}
	
	private static boolean isNotExcluded(final String resourceId) {
		for (final Pattern pattern : EXCLUDE_RESOURCES) {
			if (pattern.matcher(resourceId).matches()) {
				return false;
			}
		}
		return true;
	}

	private static List<Pattern> initExclusionPatterns() {
		return configuredExclusions().stream().map(pattern -> Pattern.compile(".*\\" + pattern)).collect(toList());
	}

	/**
	 * Lookup web.xml context-param {@link javax.faces.application.ResourceHandler#RESOURCE_EXCLUDES_PARAM_NAME}.<br/>
	 * If exists use as space separated configuration list,<br/>
	 * otherwise fallback to {@link javax.faces.application.ResourceHandler#RESOURCE_EXCLUDES_DEFAULT_VALUE}
	 */
	private static List<String> configuredExclusions() {
		final String exclusions = Optional
				.ofNullable(getContextParameter())
				.orElseGet(() -> ResourceHandler.RESOURCE_EXCLUDES_DEFAULT_VALUE);
		return Arrays.asList(exclusions.split(" "));
	}

	private static String getContextParameter() {
		final String parameterValue = FacesContext.getCurrentInstance().getExternalContext()
				.getInitParameter(ResourceHandler.RESOURCE_EXCLUDES_PARAM_NAME);
		if(null == parameterValue ||parameterValue.isEmpty() ){
			return null;
		}
		return parameterValue;
	}
}
