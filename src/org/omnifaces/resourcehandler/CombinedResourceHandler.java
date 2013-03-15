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
package org.omnifaces.resourcehandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.application.ResourceHandlerWrapper;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.PreRenderViewEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;
import javax.servlet.http.HttpServletResponse;

import org.omnifaces.renderer.InlineScriptRenderer;
import org.omnifaces.renderer.InlineStylesheetRenderer;
import org.omnifaces.util.Events;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Hacks;
import org.omnifaces.util.Utils;

/**
 * This {@link ResourceHandler} implementation will remove all separate script and stylesheet resources which have the
 * <code>target</code> attribute set to <code>"head"</code> from the <code>UIViewRoot</code> and create a combined one
 * for all scripts and another combined one for all stylesheets, when the JSF project stage is <strong>not</strong> set
 * to <code>Development</code>.
 * <p>
 * To get it to run, this handler needs be registered as follows in <code>faces-config.xml</code>:
 * <pre>
 * &lt;application&gt;
 *   &lt;resource-handler&gt;org.omnifaces.resourcehandler.CombinedResourceHandler&lt;/resource-handler&gt;
 * &lt;/application&gt;
 * </pre>
 * <p>
 * Note that the <code>target</code> attribute of <code>&lt;h:outputStylesheet&gt;</code> already defaults to
 * <code>"head"</code> but the one of <code>&lt;h:outputScript&gt;</code> not. So if you have placed this inside the
 * <code>&lt;h:head&gt;</code>, then you would still need to explicitly set its <code>target</code> attribute to
 * <code>"head"</code>, otherwise it will be treated as an inline script and not be combined. This is a design
 * limitation.
 * <pre>
 * &lt;h:head&gt;
 *   &lt;h:outputStylesheet name="style.css" /&gt;
 *   &lt;h:outputScript name="script.js" target="head" /&gt;
 * &lt;/h:head&gt;
 * </pre>
 * <p>
 * If you want them to appear <em>after</em> any auto-included resources, then move the declarations to the
 * <code>&lt;h:body&gt;</code>.
 * <pre>
 * &lt;h:body&gt;
 *   &lt;h:outputStylesheet name="style.css" /&gt;
 *   &lt;h:outputScript name="script.js" target="head" /&gt;
 * &lt;/h:body&gt;
 * </pre>
 * <h3>Configuration</h3>
 * <p>
 * The following context parameters are available:
 * <table>
 * <tr><td nowrap>
 * <code>{@value org.omnifaces.resourcehandler.CombinedResourceHandler#PARAM_NAME_EXCLUDED_RESOURCES}</code>
 * </td><td>
 * Comma separated string of resource identifiers of <code>&lt;h:head&gt;</code> resources which needs to be excluded
 * from combining. For example:
 * <br/><code>&lt;param-value&gt;primefaces:primefaces.css, javax.faces:jsf.js&lt;/param-value&gt;</code><br/>
 * Any combined resource will be included <i>after</i> any of those excluded resources.
 * </td></tr>
 * <tr><td nowrap>
 * <code>{@value org.omnifaces.resourcehandler.CombinedResourceHandler#PARAM_NAME_SUPPRESSED_RESOURCES}</code>
 * </td><td>
 * Comma separated string of resource identifiers of <code>&lt;h:head&gt;</code> resources which needs to be suppressed
 * and removed. For example:
 * <br/><code>&lt;param-value&gt;skinning.ecss, primefaces:jquery/jquery.js&lt;/param-value&gt;</code>
 * </td></tr>
 * <tr><td nowrap>
 * <code>{@value org.omnifaces.resourcehandler.CombinedResourceHandler#PARAM_NAME_INLINE_CSS}</code>
 * </td><td>
 * Set to <code>true</code> if you want to render the combined CSS resources inline (embedded in HTML) instead of as a
 * resource.
 * </td></tr>
 * <tr><td nowrap>
 * <code>{@value org.omnifaces.resourcehandler.CombinedResourceHandler#PARAM_NAME_INLINE_JS}</code>
 * </td><td>
 * Set to <code>true</code> if you want to render the combined JS resources inline (embedded in HTML) instead of as a
 * resource.
 * </td></tr>
 * </table>
 * <p>
 * Here, the "resource identifier" is the unique combination of library name and resource name, separated by a colon,
 * exactly the syntax as you would use in <code>#{resource}</code> in EL. If there is no library name, then just omit
 * the colon. Valid examples of resource identifiers are <code>filename.ext</code>, <code>folder/filename.ext</code>,
 * <code>library:filename.ext</code> and <code>library:folder/filename.ext</code>.
 * <p>
 * Note that this combined resource handler is <strong>not</strong> able to combine resources which are <em>not</em>
 * been added as a component resource, but are been hardcoded in some renderer (such as <code>theme.css</code> in case
 * of PrimeFaces and several JavaScript files in case of RichFaces), or are been definied using plain HTML
 * <code>&lt;link&gt;</code> or <code>&lt;script&gt;</code> elements. Also, when you're using RichFaces with the context
 * parameter <code>org.richfaces.resourceOptimization.enabled</code> set to <code>true</code>, then the to-be-combined
 * resource cannot be resolved by a classpath URL due to RichFaces design limitations, so this combined resource handler
 * will use an internal workaround to get it to work anyway, but this involves firing a HTTP request for every resource.
 * The impact should however be relatively negligible as this is performed on localhost.
 * <h3>CDNResourceHandler</h3>
 * <p>
 * If you're also using the {@link CDNResourceHandler} or, at least, have configured its context parameter
 * {@value org.omnifaces.resourcehandler.CDNResourceHandler#PARAM_NAME_CDN_RESOURCES}, then those CDN resources will
 * automatically be added to the set of excluded resources.
 *
 * @author Bauke Scholtz
 */
public class CombinedResourceHandler extends ResourceHandlerWrapper implements SystemEventListener {

	// Constants ------------------------------------------------------------------------------------------------------

	/** The default library name of a combined resource. Make sure that this is never used for other libraries. */
	public static final String LIBRARY_NAME = "omnifaces.combined";

	/** The context parameter name to specify resource identifiers which needs to be excluded from combining. */
	public static final String PARAM_NAME_EXCLUDED_RESOURCES =
		"org.omnifaces.COMBINED_RESOURCE_HANDLER_EXCLUDED_RESOURCES";

	/** The context parameter name to specify resource identifiers which needs to be suppressed and removed. */
	public static final String PARAM_NAME_SUPPRESSED_RESOURCES =
		"org.omnifaces.COMBINED_RESOURCE_HANDLER_SUPPRESSED_RESOURCES";

	/** The context parameter name to enable rendering CSS inline instead of as resource link. */
	public static final String PARAM_NAME_INLINE_CSS =
		"org.omnifaces.COMBINED_RESOURCE_HANDLER_INLINE_CSS";

	/** The context parameter name to enable rendering JS inline instead of as resource link. */
	public static final String PARAM_NAME_INLINE_JS =
		"org.omnifaces.COMBINED_RESOURCE_HANDLER_INLINE_JS";

	private static final String TARGET_HEAD = "head";
	private static final String ATTRIBUTE_RESOURCE_LIBRARY = "library";
	private static final String ATTRIBUTE_RESOURCE_NAME = "name";

	// Properties -----------------------------------------------------------------------------------------------------

	private ResourceHandler wrapped;
	private Set<ResourceIdentifier> excludedResources;
	private Set<ResourceIdentifier> suppressedResources;
	private boolean inlineCSS;
	private boolean inlineJS;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Creates a new instance of this combined resource handler which wraps the given resource handler. If the current
	 * JSF project stage is not set to <code>Development</code>, then the initialization will be performed and this
	 * will also register this resource handler as a pre render view event listener, so that it can do the job of
	 * removing the CSS/JS resources and adding combined ones.
	 * @param wrapped The resource handler to be wrapped.
	 */
	public CombinedResourceHandler(ResourceHandler wrapped) {
		this.wrapped = wrapped;

		if (!Faces.isDevelopment()) {
			excludedResources = initResources(PARAM_NAME_EXCLUDED_RESOURCES);
			excludedResources.addAll(initCDNResources());
			suppressedResources = initResources(PARAM_NAME_SUPPRESSED_RESOURCES);
			excludedResources.addAll(suppressedResources);
			inlineCSS = Boolean.valueOf(Faces.getInitParameter(PARAM_NAME_INLINE_CSS));
			inlineJS = Boolean.valueOf(Faces.getInitParameter(PARAM_NAME_INLINE_JS));
			Events.subscribeToEvent(PreRenderViewEvent.class, this);
		}
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns true if the source is an instance of {@link UIViewRoot}.
	 */
	@Override
	public boolean isListenerForSource(Object source) {
		return (source instanceof UIViewRoot);
	}

	/**
	 * Before rendering of a freshly created view, perform the following actions:
	 * <ul>
	 * <li>Collect all component resources from the head.
	 * <li>Check and collect the script and stylesheet resources separately and remove them from the head.
	 * <li>If there are any resources in the collection of script and/or stylesheet resources, then create a
	 * component resource component pointing to the combined resource info and add it to the head at the location of
	 * the first resource.
	 * </ul>
	 */
	@Override
	public void processEvent(SystemEvent event) throws AbortProcessingException {
		FacesContext context = FacesContext.getCurrentInstance();
		CombinedResourceBuilder builder = new CombinedResourceBuilder();

		for (UIComponent componentResource : context.getViewRoot().getComponentResources(context, TARGET_HEAD)) {
			String library = (String) componentResource.getAttributes().get(ATTRIBUTE_RESOURCE_LIBRARY);
			String name = (String) componentResource.getAttributes().get(ATTRIBUTE_RESOURCE_NAME);

			if (name == null) {
				continue; // It's likely an inline script, they can't be combined as it might contain EL expressions.
			}

			ResourceIdentifier resourceIdentifier = new ResourceIdentifier(library, name);
			builder.add(context, componentResource, componentResource.getRendererType(), resourceIdentifier);
		}

		builder.create(context);
	}

	@Override
	public Resource createResource(String resourceName) {
		return createResource(resourceName, null, null);
	}

	@Override
	public Resource createResource(String resourceName, String libraryName) {
		return createResource(resourceName, libraryName, null);
	}

	@Override
	public Resource createResource(String resourceName, String libraryName, String contentType) {
		if (LIBRARY_NAME.equals(libraryName)) {
			return new CombinedResource(resourceName);
		}
		else {
			return wrapped.createResource(resourceName, libraryName, contentType);
		}
	}

	@Override
	public void handleResourceRequest(FacesContext context) throws IOException {
		if (LIBRARY_NAME.equals(context.getExternalContext().getRequestParameterMap().get("ln"))) {
			try {
				streamResource(context, new CombinedResource(context));
			}
			catch (IllegalArgumentException e) {
				context.getExternalContext().responseSendError(HttpServletResponse.SC_NOT_FOUND, Faces.getRequestURI());
			}
		}
		else {
			wrapped.handleResourceRequest(context);
		}
	}

	@Override
	public ResourceHandler getWrapped() {
		return wrapped;
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Generic method to initialize set of resources based on given application initialization parameter name.
	 * @param name The application initialization parameter name.
	 * @return The set of resources which are set by the given application initialization parameter name, or an empty
	 * set if the parameter is not been set.
	 */
	private static Set<ResourceIdentifier> initResources(String name) {
		Set<ResourceIdentifier> resources = new HashSet<ResourceIdentifier>(1);
		String configuredResources = Faces.getInitParameter(name);

		if (configuredResources != null) {
			for (String resourceIdentifier : configuredResources.split("\\s*,\\s*")) {
				resources.add(new ResourceIdentifier(resourceIdentifier));
			}
		}

		return resources;
	}

	/**
	 * Initialize the set of CDN resources based on {@link CDNResourceHandler} configuration.
	 * @return The set of CDN resources.
	 */
	private static Set<ResourceIdentifier> initCDNResources() {
		Map<ResourceIdentifier, String> cdnResources = CDNResourceHandler.initCDNResources();
		return (cdnResources != null) ? cdnResources.keySet() : Collections.<ResourceIdentifier>emptySet();
	}

	/**
	 * Stream the given resource to the response associated with the given faces context.
	 * @param context The involved faces context.
	 * @param resource The resource to be streamed.
	 * @throws IOException If something fails at I/O level.
	 */
	private static void streamResource(FacesContext context, Resource resource) throws IOException {
		ExternalContext externalContext = context.getExternalContext();

		if (!resource.userAgentNeedsUpdate(context)) {
			externalContext.setResponseStatus(HttpServletResponse.SC_NOT_MODIFIED);
			return;
		}

		externalContext.setResponseContentType(resource.getContentType());

		for (Entry<String, String> header : resource.getResponseHeaders().entrySet()) {
			externalContext.setResponseHeader(header.getKey(), header.getValue());
		}

		Utils.stream(resource.getInputStream(), externalContext.getResponseOutputStream());
	}

	// Inner classes --------------------------------------------------------------------------------------------------

	/**
	 * General builder to collect, exclude and suppress stylesheet and script component resources.
	 *
	 * @author Bauke Scholtz
	 */
	private final class CombinedResourceBuilder {

		// Constants --------------------------------------------------------------------------------------------------

		private static final String RENDERER_TYPE_CSS = "javax.faces.resource.Stylesheet";
		private static final String RENDERER_TYPE_JS = "javax.faces.resource.Script";
		private static final String EXTENSION_CSS = ".css";
		private static final String EXTENSION_JS = ".js";

		// General stylesheet/script builder --------------------------------------------------------------------------

		private CombinedResourceBuilder stylesheets;
		private CombinedResourceBuilder scripts;
		private List<UIComponent> componentResourcesToRemove;

		public CombinedResourceBuilder() {
			stylesheets = new CombinedResourceBuilder(EXTENSION_CSS);
			scripts = new CombinedResourceBuilder(EXTENSION_JS);
			componentResourcesToRemove = new ArrayList<UIComponent>(3);
		}

		public void add(FacesContext context, UIComponent component, String rendererType, ResourceIdentifier id) {
			if (LIBRARY_NAME.equals(id.getLibrary())) {
				// Found an already combined resource. Extract and recombine it.
				CombinedResourceInfo info = CombinedResourceInfo.get(id.getName());

				if (info != null) {
					for (ResourceIdentifier combinedId : info.getResourceIdentifiers()) {
						add(context, null, rendererType, combinedId);
					}
				}

				componentResourcesToRemove.add(component);
			}
			else if (rendererType.equals(RENDERER_TYPE_CSS)) {
				stylesheets.add(component, id.getLibrary(), id.getName());
			}
			else if (rendererType.equals(RENDERER_TYPE_JS)) {
				scripts.add(component, id.getLibrary(), id.getName());
			}

			// WARNING: START OF HACK! --------------------------------------------------------------------------------
			// HACK for RichFaces4 because of its non-standard resource library handling. Resources with the extension
			// ".reslib" have special treatment by RichFaces specific resource library renderer. They represent multiple
			// resources which are supposed to be dynamically constructed/added with the purpose to keep resource
			// dependencies in RichFaces components "DRY". So far, it are usually only JS resources.
			else if (Hacks.isRichFacesResourceLibraryRenderer(rendererType)) {
				Set<ResourceIdentifier> resourceIdentifiers = Hacks.getRichFacesResourceLibraryResources(id);
				ResourceHandler handler = context.getApplication().getResourceHandler();

				for (ResourceIdentifier identifier : resourceIdentifiers) {
					rendererType = handler.getRendererTypeForResourceName(identifier.getName());
					add(context, null, rendererType, identifier);
				}

				componentResourcesToRemove.add(component);
			}
			// --------------------------------------------------------------------------------------------------------
		}

		public void create(FacesContext context) {
			stylesheets.create(context, inlineCSS ? InlineStylesheetRenderer.RENDERER_TYPE : RENDERER_TYPE_CSS);
			scripts.create(context, inlineJS ? InlineScriptRenderer.RENDERER_TYPE : RENDERER_TYPE_JS);
			removeComponentResources(context);
		}

		private void removeComponentResources(FacesContext context) {
			for (UIComponent resourceToRemove : componentResourcesToRemove) {
				if (resourceToRemove != null) {
					context.getViewRoot().removeComponentResource(context, resourceToRemove, TARGET_HEAD);
				}
			}
		}

		// Specific stylesheet/script builder -------------------------------------------------------------------------

		private String extension;
		private CombinedResourceInfo.Builder info;
		private UIComponent componentResource;

		private CombinedResourceBuilder(String extension) {
			this.extension = extension;
			info = new CombinedResourceInfo.Builder();
			componentResourcesToRemove = new ArrayList<UIComponent>(3);
		}

		private void add(UIComponent componentResource, String library, String name) {
			ResourceIdentifier resourceIdentifier = new ResourceIdentifier(library, name);

			if (excludedResources.isEmpty() || !excludedResources.contains(resourceIdentifier)) {
				info.add(resourceIdentifier);

				if (this.componentResource == null) {
					this.componentResource = componentResource;
				}
				else {
					componentResourcesToRemove.add(componentResource);
				}
			}
			else if (suppressedResources.contains(resourceIdentifier)) {
				componentResourcesToRemove.add(componentResource);
			}
		}

		private void create(FacesContext context, String rendererType) {
			if (!info.isEmpty()) {
				if (componentResource == null) {
					componentResource = new UIOutput();
					context.getViewRoot().addComponentResource(context, componentResource, TARGET_HEAD);
				}

				componentResource.getAttributes().put(ATTRIBUTE_RESOURCE_LIBRARY, LIBRARY_NAME);
				componentResource.getAttributes().put(ATTRIBUTE_RESOURCE_NAME, info.create() + extension);
				componentResource.setRendererType(rendererType);
			}

			removeComponentResources(context);
		}

	}

}