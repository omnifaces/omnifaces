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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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

import org.omnifaces.util.Events;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Utils;

/**
 * This {@link ResourceHandler} implementation will remove all separate script and stylesheet resources which have the
 * <code>target</code> attribute set to <code>"head"</code> from the <code>UIViewRoot</code> and create a combined one
 * for all scripts and another combined one for all stylesheets.
 * <p>
 * To get it to run, this handler needs be registered as follows in <tt>faces-config.xml</tt>:
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
 * <h3>Configuration</h3>
 * <p>
 * The following context parameters are available:
 * <table>
 * <tr><td nowrap>
 * <code>{@value org.omnifaces.resourcehandler.CombinedResourceHandler#EXCLUDED_RESOURCES_PARAM_NAME}</code>
 * </td><td>
 * Comma separated string of resource identifiers of <code>&lt;h:head&gt;</code> resources which needs to be excluded
 * from combining. For example:
 * <br/><code>&lt;param-value&gt;primefaces:primefaces.css, javax.faces:jsf.js&lt;/param-value&gt;</code><br/>
 * Any combined resource will be included <i>after</i> any of those excluded resources.
 * </td></tr>
 * <tr><td nowrap>
 * <code>{@value org.omnifaces.resourcehandler.CombinedResourceHandler#SUPPRESSED_RESOURCES_PARAM_NAME}</code>
 * </td><td>
 * Comma separated string of resource identifiers of <code>&lt;h:head&gt;</code> resources which needs to be suppressed
 * and removed. For example:
 * <br/><code>&lt;param-value&gt;skinning.ecss, primefaces:jquery/jquery.js&lt;/param-value&gt;</code>
 * </td></tr>
 * </table>
 * <p>
 * Here, the "resource identifier" is the unique combination of library name and resource name, separated by a colon,
 * exactly the syntax as you would use in <code>#{resource}</code> in EL. If there is no library name, then just omit
 * the colon. Valid examples of resource identifiers are <tt>filename.ext</tt>, <tt>folder/filename.ext</tt>,
 * <tt>library:filename.ext</tt> and <tt>library:folder/filename.ext</tt>.
 *
 * @author Bauke Scholtz
 */
public class CombinedResourceHandler extends ResourceHandlerWrapper implements SystemEventListener {

	// Constants ------------------------------------------------------------------------------------------------------

	/** The default library name of a combined resource. Make sure that this is never used for other libraries. */
	public static final String LIBRARY_NAME = "omnifaces.combined";

	/** The context parameter name to specify resource identifiers which needs to be excluded from combining. */
    public static final String EXCLUDED_RESOURCES_PARAM_NAME =
    	"org.omnifaces.COMBINED_RESOURCE_HANDLER_EXCLUDED_RESOURCES";

	/** The context parameter name to specify resource identifiers which needs to be suppressed and removed. */
    public static final String SUPPRESSED_RESOURCES_PARAM_NAME =
    	"org.omnifaces.COMBINED_RESOURCE_HANDLER_SUPPRESSED_RESOURCES";

	private static final String TARGET_HEAD = "head";
	private static final String ATTRIBUTE_RESOURCE_LIBRARY = "library";
	private static final String ATTRIBUTE_RESOURCE_NAME = "name";
	private static final String RENDERER_TYPE_STYLESHEET = "javax.faces.resource.Stylesheet";
	private static final String RENDERER_TYPE_SCRIPT = "javax.faces.resource.Script";
	private static final String EXTENSION_STYLESHEET = ".css";
	private static final String EXTENSION_SCRIPT = ".js";

	// Properties -----------------------------------------------------------------------------------------------------

	private ResourceHandler wrapped;
	private Set<String> excludedResources;
	private Set<String> suppressedResources;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Creates a new instance of this combined resource handler which wraps the given resource handler. This will also
	 * immediately register this resource handler as a pre render view event listener, so that it can do the job of
	 * removing the CSS/JS resources and adding combined ones.
	 * @param wrapped The resource handler to be wrapped.
	 */
	public CombinedResourceHandler(ResourceHandler wrapped) {
		this.wrapped = wrapped;
		this.excludedResources = initResources(EXCLUDED_RESOURCES_PARAM_NAME);
		this.suppressedResources = initResources(SUPPRESSED_RESOURCES_PARAM_NAME);
		this.excludedResources.addAll(suppressedResources);
		Events.subscribeToEvent(PreRenderViewEvent.class, this);
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
	 * component resource component pointing to the combined resource info and add it to the head.
	 * </ul>
	 */
	@Override
	public void processEvent(SystemEvent event) throws AbortProcessingException {
		FacesContext context = FacesContext.getCurrentInstance();
		UIViewRoot viewRoot = context.getViewRoot();

		if (viewRoot.getAttributes().get(getClass().getName()) == Boolean.TRUE) {
			return; // No need to repeat the job.
		}

		CombinedResourceInfo.Builder stylesheets = new CombinedResourceInfo.Builder();
		CombinedResourceInfo.Builder scripts = new CombinedResourceInfo.Builder();
		List<UIComponent> componentResourcesToRemove = new ArrayList<UIComponent>();

		for (UIComponent componentResource : viewRoot.getComponentResources(context, TARGET_HEAD)) {
			String library = (String) componentResource.getAttributes().get(ATTRIBUTE_RESOURCE_LIBRARY);

			if (LIBRARY_NAME.equals(library)) {
				return; // MyFaces somehow doesn't store custom view attributes. Prevent it from repeating the job.
			}

			String name = (String) componentResource.getAttributes().get(ATTRIBUTE_RESOURCE_NAME);

			if (name == null) {
				continue; // It's likely an inline script, they can't be combined.
			}

			String resourceIdentifier = (library != null ? (library + ":") : "") + name;

			if (excludedResources.isEmpty() || !excludedResources.contains(resourceIdentifier)) {
				if (componentResource.getRendererType().equals(RENDERER_TYPE_STYLESHEET)) {
					stylesheets.add(library, name);
					componentResourcesToRemove.add(componentResource);
				}
				else if (componentResource.getRendererType().equals(RENDERER_TYPE_SCRIPT)) {
					scripts.add(library, name);
					componentResourcesToRemove.add(componentResource);
				}
			}
			else if (suppressedResources.contains(resourceIdentifier)) {
				componentResourcesToRemove.add(componentResource);
			}
		}

		for (UIComponent componentResourceToRemove : componentResourcesToRemove) {
			viewRoot.removeComponentResource(context, componentResourceToRemove, TARGET_HEAD);
		}

		if (!stylesheets.isEmpty()) {
			addComponentResource(context, stylesheets.create(), EXTENSION_STYLESHEET, RENDERER_TYPE_STYLESHEET);
		}

		if (!scripts.isEmpty()) {
			addComponentResource(context, scripts.create(), EXTENSION_SCRIPT, RENDERER_TYPE_SCRIPT);
		}

		viewRoot.getAttributes().put(getClass().getName(), Boolean.TRUE); // Indicate that job is done on this view.
	}

	@Override
	public Resource createResource(String resourceName, String libraryName) {
		if (LIBRARY_NAME.equals(libraryName)) {
			return new CombinedResource(resourceName);
		}
		else {
			return wrapped.createResource(resourceName, libraryName);
		}
	}

	@Override
	public void handleResourceRequest(FacesContext context) throws IOException {
		if (LIBRARY_NAME.equals(context.getExternalContext().getRequestParameterMap().get("ln"))) {
			streamResource(context, new CombinedResource(context));
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
	private static Set<String> initResources(String name) {
		Set<String> resources = new HashSet<String>(1);
		String configuredResources = Faces.getInitParameter(name);

		if (configuredResources != null) {
			resources.addAll(Arrays.asList(configuredResources.split("\\s*,\\s*")));
		}

		return resources;
	}

	/**
	 * Create a component resource of the given name, extension and renderer type.
	 * @param context The current faces context.
	 * @param name The name of the combined resource.
	 * @param extension The extension of the combined resource.
	 * @param rendererType The renderer type of the combined resource.
	 */
	private static void addComponentResource(FacesContext context, String name, String extension, String rendererType) {
		UIOutput component = new UIOutput();
		component.setRendererType(rendererType);
		component.getAttributes().put(ATTRIBUTE_RESOURCE_LIBRARY, LIBRARY_NAME);
		component.getAttributes().put(ATTRIBUTE_RESOURCE_NAME, name + extension);
		context.getViewRoot().addComponentResource(context, component, TARGET_HEAD);
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

		InputStream input = resource.getInputStream();

		if (input == null) {
			externalContext.setResponseStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		externalContext.setResponseContentType(resource.getContentType());

		for (Entry<String, String> header : resource.getResponseHeaders().entrySet()) {
			externalContext.setResponseHeader(header.getKey(), header.getValue());
		}

		Utils.stream(input, externalContext.getResponseOutputStream());
	}

}