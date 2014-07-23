/*
 * Copyright 2014 OmniFaces.
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
package org.omnifaces.component.script;

import javax.faces.application.ResourceDependency;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialViewContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ListenerFor;
import javax.faces.event.PostAddToViewEvent;

import org.omnifaces.renderer.DeferredScriptRenderer;
import org.omnifaces.resourcehandler.ResourceIdentifier;
import org.omnifaces.util.Hacks;

/**
 * <p>
 * The <code>&lt;o:deferredScript&gt;</code> is a component based on the standard <code>&lt;h:outputScript&gt;</code>
 * which defers the loading of the given script resource to the window load event. In other words, the given script
 * resource is only loaded when the window is really finished with loading. So, the enduser can start working with the
 * webpage without waiting for the additional scripts to be loaded. Usually, it are those kind of scripts which are just
 * for progressive enhancement and thus not essential for the functioning of the webpage.
 * <p>
 * This will give bonus points with among others the Google PageSpeed tool, on the contrary to placing the script at
 * bottom of body, or using <code>defer="true"</code> or even <code>async="true"</code>.
 *
 * <h3>Usage</h3>
 * <p>
 * Just use it the same way as a <code>&lt;h:outputScript&gt;</code>, with a <code>library</code> and <code>name</code>.
 * <pre>
 * &lt;o:deferredScript library="yourlibrary" name="scripts/filename.js" /&gt;
 * </pre>
 * <p>
 * You can use the optional <code>onbegin</code>, <code>onsuccess</code> and <code>onerror</code> attributes
 * to declare JavaScript code which needs to be executed respectively right before the script is loaded,
 * right after the script is successfully loaded, and/or when the script loading failed.
 *
 * @author Bauke Scholtz
 * @since 1.8
 */
@FacesComponent(DeferredScript.COMPONENT_TYPE)
@ResourceDependency(library="omnifaces", name="omnifaces.js", target="head")
@ListenerFor(systemEventClass=PostAddToViewEvent.class)
public class DeferredScript extends ScriptFamily {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The standard component type. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.script.DeferredScript";

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new {@link DeferredScript} component whereby the renderer type is set to
	 * {@link DeferredScriptRenderer#RENDERER_TYPE}.
	 */
	public DeferredScript() {
		setRendererType(DeferredScriptRenderer.RENDERER_TYPE);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public void processEvent(ComponentSystemEvent event) throws AbortProcessingException {
		if (event instanceof PostAddToViewEvent) {
			FacesContext context = FacesContext.getCurrentInstance();
			PartialViewContext ajaxContext = context.getPartialViewContext();
			UIViewRoot view = context.getViewRoot();

			boolean ajaxRequest = ajaxContext.isAjaxRequest();
			boolean ajaxRenderAll = ajaxContext.isRenderAll();
			boolean alreadyAdded = view.getComponentResources(context, "body").contains(this);

			if (!(ajaxRequest && !ajaxRenderAll) || !alreadyAdded) {
				view.addComponentResource(context, this, "body");
				Hacks.setScriptResourceRendered(context, new ResourceIdentifier(this));
			}
		}
	}

}