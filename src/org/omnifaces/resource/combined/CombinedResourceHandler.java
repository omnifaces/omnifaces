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
package org.omnifaces.resource.combined;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

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

import org.omnifaces.util.Utils;

/**
 * This {@link ResourceHandler} implementation will remove all separate script and stylesheet resources from the head
 * and create a combined one for all scripts and another combined one for all stylesheets.
 * <p>
 * This handler must be registered as follows in <tt>faces-config.xml</tt>:
 * <pre>
 * &lt;application&gt;
 *   &lt;resource-handler&gt;org.omnifaces.resource.combined.CombinedResourceHandler&lt;/resource-handler&gt;
 * &lt;/application&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 */
public class CombinedResourceHandler extends ResourceHandlerWrapper implements SystemEventListener {

	// Constants ------------------------------------------------------------------------------------------------------

	/** The default library name of a combined resource. Make sure that this is never used for other libraries. */
	public static final String LIBRARY_NAME = "omnifaces.combined";

	private static final String TARGET_HEAD = "head";
	private static final String ATTRIBUTE_RESOURCE_LIBRARY = "library";
	private static final String ATTRIBUTE_RESOURCE_NAME = "name";
	private static final String RENDERER_TYPE_STYLESHEET = "javax.faces.resource.Stylesheet";
	private static final String RENDERER_TYPE_SCRIPT = "javax.faces.resource.Script";
	private static final String EXTENSION_STYLESHEET = ".css";
	private static final String EXTENSION_SCRIPT = ".js";

	// Properties -----------------------------------------------------------------------------------------------------

	private ResourceHandler wrapped;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Creates a new instance of this combined resource handler which wraps the given resource handler. This will also
	 * immediately register this resource handler as a pre render view event listener, so that it can do the job of
	 * removing the CSS/JS resources and adding combined ones.
	 * @param wrapped The resource handler to be wrapped.
	 */
	public CombinedResourceHandler(ResourceHandler wrapped) {
		this.wrapped = wrapped;
		FacesContext.getCurrentInstance().getApplication().subscribeToEvent(PreRenderViewEvent.class, this);
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
	 * Only on non-postback requests, perform the following actions:
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

		List<UIComponent> resources = viewRoot.getComponentResources(context, TARGET_HEAD);
		CombinedResourceInfo.Builder stylesheets = new CombinedResourceInfo.Builder();
		CombinedResourceInfo.Builder scripts = new CombinedResourceInfo.Builder();

		for (Iterator<UIComponent> iter = resources.iterator(); iter.hasNext();) {
			UIComponent resource = iter.next();
			String library = (String) resource.getAttributes().get(ATTRIBUTE_RESOURCE_LIBRARY);
			String name = (String) resource.getAttributes().get(ATTRIBUTE_RESOURCE_NAME);

			if (resource.getRendererType().equals(RENDERER_TYPE_STYLESHEET)) {
				stylesheets.add(library, name);
				iter.remove();
			}
			else if (resource.getRendererType().equals(RENDERER_TYPE_SCRIPT)) {
				scripts.add(library, name);
				iter.remove();
			}
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
			return super.createResource(resourceName, libraryName);
		}
	}

	@Override
	public void handleResourceRequest(FacesContext context) throws IOException {
		if (LIBRARY_NAME.equals(context.getExternalContext().getRequestParameterMap().get("ln"))) {
			streamResource(context, new CombinedResource(context));
		}
		else {
			super.handleResourceRequest(context);
		}
	}

	@Override
	public ResourceHandler getWrapped() {
		return wrapped;
	}

	// Helpers --------------------------------------------------------------------------------------------------------

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
		component.getAttributes().put(ATTRIBUTE_RESOURCE_LIBRARY, CombinedResourceHandler.LIBRARY_NAME);
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

		if (resource.getContentType() != null) {
			externalContext.setResponseContentType(resource.getContentType());
		}

		for (Entry<String, String> header : resource.getResponseHeaders().entrySet()) {
			externalContext.setResponseHeader(header.getKey(), header.getValue());
		}

		Utils.stream(input, externalContext.getResponseOutputStream());
	}

}