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

import java.util.Iterator;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.PreRenderViewEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;

/**
 * This listener takes care that all separate script and stylesheet resources are removed from the head and combined
 * into a single resource, one for all scripts and other one for all stylesheets.
 * <p>
 * This listener must be registered as follows in <tt>faces-config.xml</tt> on the {@link PreRenderViewEvent} event.
 * <pre>
 * &lt;application&gt;
 *   &lt;system-event-listener&gt;
 *     &lt;system-event-listener-class&gt;org.omnifaces.resource.combined.CombinedResourceListener&lt;/system-event-listener-class&gt;
 *     &lt;system-event-class&gt;javax.faces.event.PreRenderViewEvent&lt;/system-event-class&gt;
 *   &lt;/system-event-listener&gt;
 * &lt;/application&gt;
 * </pre>
 * Don't forget to register the {@link CombinedResourceHandler} in <code>&lt;application&gt;</code> as well.
 * @author Bauke Scholtz
 */
public class CombinedResourceListener implements SystemEventListener {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String TARGET_HEAD = "head";
	private static final String ATTRIBUTE_RESOURCE_LIBRARY = "library";
	private static final String ATTRIBUTE_RESOURCE_NAME = "name";
	private static final String RENDERER_TYPE_STYLESHEET = "javax.faces.resource.Stylesheet";
	private static final String RENDERER_TYPE_SCRIPT = "javax.faces.resource.Script";
	private static final String EXTENSION_STYLESHEET = ".css";
	private static final String EXTENSION_SCRIPT = ".js";

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns true of the source is an instance of {@link UIViewRoot}.
	 */
	@Override
	public boolean isListenerForSource(Object source) {
		return (source instanceof UIViewRoot);
	}

	/**
	 * Only on non-postback requests, perform the following actions:
	 * <li>Collect all component resources from the head.
	 * <li>Check and collect the script and stylesheet resources separately and remove them from the head.
	 * <li>If there are any resources in the collection of script and/or stylesheet resources, then create a
	 * component resource component pointing to the combined resource info and add it to the head.
	 */
	@Override
	public void processEvent(SystemEvent event) throws AbortProcessingException {
		FacesContext context = FacesContext.getCurrentInstance();

		if (context.isPostback()) {
			return; // No need to repeat the job for (ajax) postbacks. The same view will be used anyway.
		}

		UIViewRoot viewRoot = context.getViewRoot();
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
			} else if (resource.getRendererType().equals(RENDERER_TYPE_SCRIPT)) {
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

}