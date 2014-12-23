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
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ListenerFor;
import javax.faces.event.ListenersFor;
import javax.faces.event.PostAddToViewEvent;
import javax.faces.event.PostRestoreStateEvent;

import org.omnifaces.renderer.DeferredScriptRenderer;
import org.omnifaces.resourcehandler.ResourceIdentifier;
import org.omnifaces.util.Hacks;

/**
 * <strong>DeferredScript</strong> is a component which defers the loading of the given script resource to the window
 * load event. In other words, the given script resource is only loaded when the window is really finished with loading.
 * So, the enduser can start working with the webpage without waiting for the additional scripts to be loaded. Usually,
 * it are those kind of scripts which are just for progressive enhancement and thus not essential for the functioning
 * of the webpage.
 * <p>
 * This will give bonus points with among others the Google PageSpeed tool, on the contrary to placing the script at
 * bottom of body, or using <code>defer="true"</code> or even <code>async="true"</code>.
 *
 * @author Bauke Scholtz
 * @since 1.8
 */
@FacesComponent(DeferredScript.COMPONENT_TYPE)
@ResourceDependency(library="omnifaces", name="omnifaces.js", target="head")
@ListenersFor({
	@ListenerFor(systemEventClass=PostAddToViewEvent.class),
	@ListenerFor(systemEventClass=PostRestoreStateEvent.class)
})
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
		if (moveToBody(event, this)) {
			Hacks.setScriptResourceRendered(getFacesContext(), new ResourceIdentifier(this));
		}
	}

}
