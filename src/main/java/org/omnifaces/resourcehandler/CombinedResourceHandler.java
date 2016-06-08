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

import static java.lang.Boolean.parseBoolean;
import static org.omnifaces.util.Events.subscribeToApplicationEvent;
import static org.omnifaces.util.Faces.evaluateExpressionGet;
import static org.omnifaces.util.Faces.getInitParameter;
import static org.omnifaces.util.Faces.isDevelopment;
import static org.omnifaces.util.Renderers.RENDERER_TYPE_CSS;
import static org.omnifaces.util.Renderers.RENDERER_TYPE_JS;
import static org.omnifaces.util.Utils.isNumber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.PreRenderViewEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;

import org.omnifaces.component.output.cache.Cache;
import org.omnifaces.component.script.DeferredScript;
import org.omnifaces.renderer.DeferredScriptRenderer;
import org.omnifaces.renderer.InlineScriptRenderer;
import org.omnifaces.renderer.InlineStylesheetRenderer;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Hacks;

/**
 * <p>
 * This {@link ResourceHandler} implementation will remove all separate script and stylesheet resources which have the
 * <code>target</code> attribute set to <code>"head"</code> from the {@link UIViewRoot} and create a combined one
 * for all scripts and another combined one for all stylesheets. In most cases your application's pages will load
 * considerably. Optionally, the combined resource files can be cached on the server during non-development stage,
 * giving your application another boost (at the expense of some heap memory on the server side).
 *
 * <h3>Installation</h3>
 * <p>
 * To get it to run, this handler needs be registered as follows in <code>faces-config.xml</code>:
 * <pre>
 * &lt;application&gt;
 *     &lt;resource-handler&gt;org.omnifaces.resourcehandler.CombinedResourceHandler&lt;/resource-handler&gt;
 * &lt;/application&gt;
 * </pre>
 *
 * <h3>Usage</h3>
 * <p>
 * Noted should be that the <code>target</code> attribute of <code>&lt;h:outputStylesheet&gt;</code> already defaults to
 * <code>"head"</code> but the one of <code>&lt;h:outputScript&gt;</code> not. So if you have placed this inside the
 * <code>&lt;h:head&gt;</code>, then you would still need to explicitly set its <code>target</code> attribute to
 * <code>"head"</code>, otherwise it will be treated as an inline script and not be combined. This is a design
 * limitation. This is not necessary for <code>&lt;o:deferredScript&gt;</code>.
 * <pre>
 * &lt;h:head&gt;
 *     ...
 *     &lt;h:outputStylesheet name="style.css" /&gt;
 *     &lt;h:outputScript name="script.js" target="head" /&gt;
 *     &lt;o:deferredScript name="onload.js" /&gt;
 * &lt;/h:head&gt;
 * </pre>
 * <p>
 * If you want them to appear <em>after</em> any auto-included resources of standard JSF implementation or JSF component
 * libraries, then move the declarations to top of the <code>&lt;h:body&gt;</code>. This is not necessary for
 * <code>&lt;o:deferredScript&gt;</code>.
 * <pre>
 * &lt;h:body&gt;
 *     &lt;h:outputStylesheet name="style.css" /&gt;
 *     &lt;h:outputScript name="script.js" target="head" /&gt;
 *     ...
 * &lt;/h:body&gt;
 * </pre>
 * <p>
 * The generated combined resource URL also includes the "<code>v</code>" request parameter which is the last modified
 * time of the newest individual resource in minutes, so that the browser will always be forced to request the latest
 * version whenever one of the individual resources has changed.
 *
 * <h3>Caching</h3>
 * <p>
 * Optionally you can activate server-side caching of the combined resource content by specifying the below context
 * parameter in <code>web.xml</code> with the amount of seconds to cache the combined resource content.
 * <pre>
 * &lt;context-param&gt;
 *     &lt;param-name&gt;org.omnifaces.COMBINED_RESOURCE_HANDLER_CACHE_TTL&lt;/param-name&gt;
 *     &lt;param-value&gt;3628800&lt;/param-value&gt; &lt;!-- 6 weeks --&gt;
 * &lt;/context-param&gt;
 * </pre>
 * <p>
 * This is only considered when the JSF project stage is <strong>not</strong> set to <code>Development</code> as per
 * {@link Faces#isDevelopment()}.
 * <p>
 * This can speed up the initial page load considerably. In general, subsequent page loads are served from the browser
 * cache, so caching doesn't make a difference on postbacks, but only on initial requests. The combined resource content
 * is by default cached in an application scoped cache in heap space. This can be customized as per instructions in
 * {@link Cache} javadoc. As to the heap space consumption, note that without caching the same amount of heap space is
 * allocated and freed for each request that can't be served from the browser cache, so chances are you won't notice the
 * memory penalty of caching.
 *
 * <h3>Configuration</h3>
 * <p>
 * The following context parameters are available:
 * <table summary="All available context parameters">
 * <tr><td class="colFirst">
 * <code>{@value org.omnifaces.resourcehandler.CombinedResourceHandler#PARAM_NAME_EXCLUDED_RESOURCES}</code>
 * </td><td>
 * Comma separated string of resource identifiers of <code>&lt;h:head&gt;</code> resources which needs to be excluded
 * from combining. For example:
 * <br><code>&lt;param-value&gt;primefaces:primefaces.css, javax.faces:jsf.js&lt;/param-value&gt;</code>
 * <br>Any combined resource will be included <i>after</i> any of those excluded resources.
 * </td></tr>
 * <tr><td class="colFirst">
 * <code>{@value org.omnifaces.resourcehandler.CombinedResourceHandler#PARAM_NAME_SUPPRESSED_RESOURCES}</code>
 * </td><td>
 * Comma separated string of resource identifiers of <code>&lt;h:head&gt;</code> resources which needs to be suppressed
 * and removed. For example:
 * <br><code>&lt;param-value&gt;skinning.ecss, primefaces:jquery/jquery.js&lt;/param-value&gt;</code>
 * </td></tr>
 * <tr><td class="colFirst">
 * <code>{@value org.omnifaces.resourcehandler.CombinedResourceHandler#PARAM_NAME_INLINE_CSS}</code>
 * </td><td>
 * Set to <code>true</code> if you want to render the combined CSS resources inline (embedded in HTML) instead of as a
 * resource.
 * </td></tr>
 * <tr><td class="colFirst">
 * <code>{@value org.omnifaces.resourcehandler.CombinedResourceHandler#PARAM_NAME_INLINE_JS}</code>
 * </td><td>
 * Set to <code>true</code> if you want to render the combined JS resources inline (embedded in HTML) instead of as a
 * resource.
 * </td></tr>
 * <tr><td class="colFirst">
 * <code>{@value org.omnifaces.resourcehandler.CombinedResourceHandler#PARAM_NAME_CACHE_TTL}</code>
 * </td><td>
 * Set with a value greater than 0 to activate server-side caching of the combined resource files. The value is
 * interpreted as cache TTL (time to live) in seconds and is only effective when the JSF project stage is
 * <strong>not</strong> set to <code>Development</code> as per {@link Faces#isDevelopment()}. Combined resource files
 * are removed from the cache if they are older than this parameter indicates (and regenerated if newly requested).
 * The default value is 0 (i.e. not cached). For global cache settings refer {@link Cache} javadoc.
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
 *
 * <h3>Conditionally disable combined resource handler</h3>
 * <p>
 * If you'd like to supply a context parameter which conditionally disables the combined resource handler, then set the
 * context parameter {@value org.omnifaces.resourcehandler.CombinedResourceHandler#PARAM_NAME_DISABLED} accordingly.
 * <pre>
 * &lt;context-param&gt;
 *     &lt;param-name&gt;org.omnifaces.COMBINED_RESOURCE_HANDLER_DISABLED&lt;/param-name&gt;
 *     &lt;param-value&gt;true&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * &lt;!-- or --&gt;
 * &lt;context-param&gt;
 *     &lt;param-name&gt;org.omnifaces.COMBINED_RESOURCE_HANDLER_DISABLED&lt;/param-name&gt;
 *     &lt;param-value&gt;#{facesContext.application.projectStage eq 'Development'}&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * &lt;!-- or --&gt;
 * &lt;context-param&gt;
 *     &lt;param-name&gt;org.omnifaces.COMBINED_RESOURCE_HANDLER_DISABLED&lt;/param-name&gt;
 *     &lt;param-value&gt;#{someApplicationScopedBean.someBooleanProperty}&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * </pre>
 * <p>The EL expression is resolved on a per-request basis.</p>
 *
 * <h3>CDNResourceHandler</h3>
 * <p>
 * If you're also using the {@link CDNResourceHandler} or, at least, have configured its context parameter
 * {@value org.omnifaces.resourcehandler.CDNResourceHandler#PARAM_NAME_CDN_RESOURCES}, then those CDN resources will
 * automatically be added to the set of excluded resources.
 *
 * @author Bauke Scholtz
 * @author Stephan Rauh {@literal <www.beyondjava.net>}
 *
 * @see CombinedResource
 * @see CombinedResourceInfo
 * @see CombinedResourceInputStream
 * @see DynamicResource
 * @see DefaultResourceHandler
 */
public class CombinedResourceHandler extends DefaultResourceHandler implements SystemEventListener {

	// Constants ------------------------------------------------------------------------------------------------------

	/** The default library name of a combined resource. Make sure that this is never used for other libraries. */
	public static final String LIBRARY_NAME = "omnifaces.combined";

	/** The context parameter name to conditionally disable combined resource handler. @since 2.0 */
	public static final String PARAM_NAME_DISABLED =
		"org.omnifaces.COMBINED_RESOURCE_HANDLER_DISABLED";

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

	/** The context parameter name to specify cache TTL of combined resources. @since 2.1 */
	public static final String PARAM_NAME_CACHE_TTL =
		"org.omnifaces.COMBINED_RESOURCE_HANDLER_CACHE_TTL";

	private static final String ERROR_INVALID_CACHE_TTL_PARAM =
		"Context parameter '" + PARAM_NAME_CACHE_TTL + "' is in invalid syntax."
			+ " It must represent a valid time in seconds between 0 and " + Integer.MAX_VALUE + "."
			+ " Encountered an invalid value of '%s'.";

	private static final String TARGET_HEAD = "head";
	private static final String TARGET_BODY = "body";

	// Properties -----------------------------------------------------------------------------------------------------

	private String disabledParam;
	private Set<ResourceIdentifier> excludedResources;
	private Set<ResourceIdentifier> suppressedResources;
	private boolean inlineCSS;
	private boolean inlineJS;
	private Integer cacheTTL;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Creates a new instance of this combined resource handler which wraps the given resource handler. This will also
	 * register this resource handler as a pre render view event listener, so that it can do the job of removing the
	 * CSS/JS resources and adding combined ones.
	 * @param wrapped The resource handler to be wrapped.
	 */
	public CombinedResourceHandler(ResourceHandler wrapped) {
		super(wrapped);
		disabledParam = getInitParameter(PARAM_NAME_DISABLED);
		excludedResources = initResources(PARAM_NAME_EXCLUDED_RESOURCES);
		excludedResources.addAll(initCDNResources());
		suppressedResources = initResources(PARAM_NAME_SUPPRESSED_RESOURCES);
		excludedResources.addAll(suppressedResources);
		inlineCSS = parseBoolean(getInitParameter(PARAM_NAME_INLINE_CSS));
		inlineJS = parseBoolean(getInitParameter(PARAM_NAME_INLINE_JS));
		cacheTTL = initCacheTTL(getInitParameter(PARAM_NAME_CACHE_TTL));
		subscribeToApplicationEvent(PreRenderViewEvent.class, this);
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
		if (disabledParam != null && parseBoolean(String.valueOf(evaluateExpressionGet(disabledParam)))) {
			return;
		}

		FacesContext context = FacesContext.getCurrentInstance();
		UIViewRoot view = context.getViewRoot();
		CombinedResourceBuilder builder = new CombinedResourceBuilder();

		for (UIComponent component : view.getComponentResources(context, TARGET_HEAD)) {
			if (component.getAttributes().get("name") == null) {
				continue; // It's likely an inline script, they can't be combined as it might contain EL expressions.
			}

			builder.add(context, component, component.getRendererType(), new ResourceIdentifier(component), TARGET_HEAD);
		}

		for (UIComponent component : view.getComponentResources(context, TARGET_BODY)) {
			if (!(component instanceof DeferredScript)) {
				continue; // We currently only support deferred scripts. TODO: support body scripts as well?
			}

			builder.add(context, component, component.getRendererType(), new ResourceIdentifier(component), TARGET_BODY);
		}

		builder.create(context);
	}

	/**
	 * Returns {@link #LIBRARY_NAME}.
	 */
	@Override
	public String getLibraryName() {
		return LIBRARY_NAME;
	}

	/**
	 * Returns a new {@link CombinedResource}.
	 */
	@Override
	public Resource createResourceFromLibrary(String resourceName, String contentType) {
		return new CombinedResource(resourceName, cacheTTL);
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Generic method to initialize set of resources based on given application initialization parameter name.
	 * @param name The application initialization parameter name.
	 * @return The set of resources which are set by the given application initialization parameter name, or an empty
	 * set if the parameter is not been set.
	 */
	private static Set<ResourceIdentifier> initResources(String name) {
		Set<ResourceIdentifier> resources = new HashSet<>(1);
		String configuredResources = getInitParameter(name);

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
	 * Initialize combined resource content cache TTL based on given application initialization parameter value.
	 */
	private static Integer initCacheTTL(String cacheTTLParam) {
		if (!isDevelopment() && cacheTTLParam != null) {
			if (isNumber(cacheTTLParam)) {
				int cacheTTL = Integer.valueOf(cacheTTLParam);

				if (cacheTTL > 0) {
					return cacheTTL;
				}
			}

			throw new IllegalArgumentException(String.format(ERROR_INVALID_CACHE_TTL_PARAM, cacheTTLParam));
		}
		else {
			return null;
		}
	}

	// Inner classes --------------------------------------------------------------------------------------------------

	/**
	 * General builder to collect, exclude and suppress stylesheet and script component resources.
	 *
	 * @author Bauke Scholtz
	 */
	private final class CombinedResourceBuilder {

		// Constants --------------------------------------------------------------------------------------------------

		private static final String EXTENSION_CSS = ".css";
		private static final String EXTENSION_JS = ".js";

		// General stylesheet/script builder --------------------------------------------------------------------------

		private CombinedResourceBuilder stylesheets;
		private CombinedResourceBuilder scripts;
		private Map<String, CombinedResourceBuilder> deferredScripts;
		private List<UIComponent> componentResourcesToRemove;

		public CombinedResourceBuilder() {
			stylesheets = new CombinedResourceBuilder(EXTENSION_CSS, TARGET_HEAD);
			scripts = new CombinedResourceBuilder(EXTENSION_JS, TARGET_HEAD);
			deferredScripts = new LinkedHashMap<>();
			componentResourcesToRemove = new ArrayList<>();
		}

		private void add(FacesContext context, UIComponent component, String rendererType, ResourceIdentifier id, String target) {
			if (LIBRARY_NAME.equals(id.getLibrary())) {
				addCombined(context, component, rendererType, id, target); // Found an already combined resource. Extract and recombine it.
			}
			else if (rendererType.equals(RENDERER_TYPE_CSS)) {
				addStylesheet(context, component, id);
			}
			else if (rendererType.equals(RENDERER_TYPE_JS)) {
				addScript(context, component, id);
			}
			else if (component instanceof DeferredScript) {
				addDeferredScript(component, id);
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
					add(context, null, handler.getRendererTypeForResourceName(identifier.getName()), identifier, target);
				}

				componentResourcesToRemove.add(component);
			}
			// --------------------------------------------------------------------------------------------------------
		}

		private void addCombined(FacesContext context, UIComponent component, String rendererType, ResourceIdentifier id, String target) {
			String[] resourcePathParts = id.getName().split("\\.", 2)[0].split("/");
			String resourceId = resourcePathParts[resourcePathParts.length - 1];
			CombinedResourceInfo info = CombinedResourceInfo.get(resourceId);
			boolean added = false;

			if (info != null) {
				for (ResourceIdentifier combinedId : info.getResourceIdentifiers()) {
					add(context, added ? null : component, rendererType, combinedId, target);
					added = true;
				}
			}

			if (!added) {
				componentResourcesToRemove.add(component);
			}
		}

		private void addStylesheet(FacesContext context, UIComponent component, ResourceIdentifier id) {
			if (stylesheets.add(component, id)) {
				Hacks.setStylesheetResourceRendered(context, id); // Prevents future forced additions by libs.
			}
		}

		private void addScript(FacesContext context, UIComponent component, ResourceIdentifier id) {
			if (Hacks.isScriptResourceRendered(context, id)) { // This is true when o:deferredScript is used.
				componentResourcesToRemove.add(component);
			}
			else if (scripts.add(component, id)) {
				Hacks.setScriptResourceRendered(context, id); // Prevents future forced additions by libs.
			}
		}

		private void addDeferredScript(UIComponent component, ResourceIdentifier id) {
			String group = (String) component.getAttributes().get("group");
			CombinedResourceBuilder builder = deferredScripts.get(group);

			if (builder == null) {
				builder = new CombinedResourceBuilder(EXTENSION_JS, TARGET_BODY);
				deferredScripts.put(group, builder);
			}

			builder.add(component, id);
		}

		public void create(FacesContext context) {
			stylesheets.create(context, inlineCSS ? InlineStylesheetRenderer.RENDERER_TYPE : RENDERER_TYPE_CSS);
			scripts.create(context, inlineJS ? InlineScriptRenderer.RENDERER_TYPE : RENDERER_TYPE_JS);

			for (CombinedResourceBuilder builder : deferredScripts.values()) {
				builder.create(context, DeferredScriptRenderer.RENDERER_TYPE);
			}

			removeComponentResources(context, componentResourcesToRemove, TARGET_HEAD);
		}

		// Specific stylesheet/script builder -------------------------------------------------------------------------

		private String extension;
		private String target;
		private CombinedResourceInfo.Builder infoBuilder;
		private UIComponent componentResource;

		private CombinedResourceBuilder(String extension, String target) {
			this.extension = extension;
			this.target = target;
			infoBuilder = new CombinedResourceInfo.Builder();
			componentResourcesToRemove = new ArrayList<>();
		}

		private boolean add(UIComponent componentResource, ResourceIdentifier resourceIdentifier) {
			if (componentResource != null && !componentResource.isRendered()) {
				componentResourcesToRemove.add(componentResource);
				return true;
			}
			else if (!containsResourceIdentifier(excludedResources, resourceIdentifier)) {
				infoBuilder.add(resourceIdentifier);

				if (this.componentResource == null) {
					this.componentResource = componentResource;
				}
				else {
					if (componentResource instanceof DeferredScript) {
						mergeAttribute(this.componentResource, componentResource, "onbegin");
						mergeAttribute(this.componentResource, componentResource, "onsuccess");
						mergeAttribute(this.componentResource, componentResource, "onerror");
					}
					componentResourcesToRemove.add(componentResource);
				}

				return true;
			}
			else if (containsResourceIdentifier(suppressedResources, resourceIdentifier)) {
				componentResourcesToRemove.add(componentResource);
				return true;
			}

			return false;
		}

		private boolean containsResourceIdentifier(Set<ResourceIdentifier> ids, ResourceIdentifier id) {
			return !ids.isEmpty() && (ids.contains(id) || ids.contains(new ResourceIdentifier(id.getLibrary(), "*")));
		}

		private void mergeAttribute(UIComponent originalComponent, UIComponent newComponent, String name) {
			String originalAttribute = getAttribute(originalComponent, name);
			String newAttribute = getAttribute(newComponent, name);
			String separator = (originalAttribute.isEmpty() || originalAttribute.endsWith(";") ? "" : ";");
			originalComponent.getAttributes().put(name, originalAttribute + separator + newAttribute);
		}

		private String getAttribute(UIComponent component, String name) {
			String attribute = (String) component.getAttributes().get(name);
			return (attribute == null) ? "" : attribute.trim();
		}

		private void create(FacesContext context, String rendererType) {
			if (!infoBuilder.isEmpty()) {
				if (componentResource == null) {
					componentResource = new UIOutput();
					context.getViewRoot().addComponentResource(context, componentResource, target);
				}

				componentResource.getAttributes().put("library", LIBRARY_NAME);
				componentResource.getAttributes().put("name", infoBuilder.create() + extension);
				componentResource.setRendererType(rendererType);

				if (RENDERER_TYPE_JS.equals(rendererType)) {
					componentResource.getPassThroughAttributes().put("crossorigin", "anonymous");
				}
			}

			removeComponentResources(context, componentResourcesToRemove, target);
		}

	}

	private static void removeComponentResources(FacesContext context, List<UIComponent> componentResourcesToRemove, String target) {
		UIViewRoot view = context.getViewRoot();

		for (UIComponent resourceToRemove : componentResourcesToRemove) {
			if (resourceToRemove != null) {
				resourceToRemove.setInView(false); // #135
				view.removeComponentResource(context, resourceToRemove, target);
			}
		}
	}

}