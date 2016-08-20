/*
 * Copyright 2016 OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnifaces.component.script;

import javax.faces.component.UIComponentBase;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.PostAddToViewEvent;
import javax.faces.event.PostRestoreStateEvent;

/**
 * Base class which is to be shared between all components of the Script family.
 *
 * @author Bauke Scholtz
 */
public abstract class ScriptFamily extends UIComponentBase {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The standard component family. */
	public static final String COMPONENT_FAMILY = "org.omnifaces.component.script";

	// UIComponent overrides ------------------------------------------------------------------------------------------

	/**
	 * Returns {@link #COMPONENT_FAMILY}.
	 */
	@Override
	public String getFamily() {
		return COMPONENT_FAMILY;
	}

	/**
	 * Returns <code>true</code>.
	 */
	@Override
	public boolean getRendersChildren() {
		return true;
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Move the given ScriptFamily component to end of body and returns <code>true</code> if done so. This helper method
	 * needs to be called from {@link #processEvent(ComponentSystemEvent)} during {@link PostAddToViewEvent} or
	 * {@link PostRestoreStateEvent}. This has basically the same effect as setting <code>target="body"</code> on a
	 * component resource.
	 * @param event The involved event, which can be either {@link PostAddToViewEvent} or {@link PostRestoreStateEvent}.
	 * @param component The component to be moved to body.
	 * @return <code>true</code> if the move has taken place.
	 */
	protected static boolean moveToBody(ComponentSystemEvent event, ScriptFamily component) {
		if (!(event instanceof PostAddToViewEvent || event instanceof PostRestoreStateEvent)) {
			return false;
		}

		FacesContext context = component.getFacesContext();
		UIViewRoot view = context.getViewRoot();

		if (context.isPostback() ? !view.getComponentResources(context, "body").contains(component) : event instanceof PostAddToViewEvent) {
			view.addComponentResource(context, component, "body");
			return true;
		}
		else {
			return false;
		}
	}

}