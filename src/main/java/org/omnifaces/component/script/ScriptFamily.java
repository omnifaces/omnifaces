/*
 * Copyright 2020 OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.component.script;

import static org.omnifaces.util.Renderers.writeAttribute;

import java.io.IOException;

import jakarta.faces.component.UIComponentBase;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.event.ComponentSystemEvent;
import jakarta.faces.event.PostAddToViewEvent;
import jakarta.faces.event.PostRestoreStateEvent;

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

	/**
	 * If this component is rendered, then start the <code>&lt;script&gt;</code> element.
	 */
	@Override
	public void encodeBegin(FacesContext context) throws IOException {
		if (getRendererType() != null) {
			super.encodeBegin(context);
			return;
		}

		pushComponentToEL(context, this);

		if (isRendered()) {
			ResponseWriter writer = context.getResponseWriter();

			if (getId() != null || !getClientBehaviors().isEmpty()) {
				writer.startElement("input", this);
				writeAttribute(writer, "type", "hidden");
				writeAttribute(writer, "id", getClientId(context));
				writer.endElement("input");
			}

			writer.startElement("script", this);
			writeAttribute(writer, "type", "text/javascript");
		}
	}

	/**
	 * If this component is rendered, then end the <code>&lt;script&gt;</code> element.
	 */
	@Override
	public void encodeEnd(FacesContext context) throws IOException {
		if (getRendererType() != null) {
			super.encodeEnd(context);
			return;
		}

		if (isRendered()) {
			context.getResponseWriter().endElement("script");
		}

		popComponentFromEL(context);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Move this ScriptFamily component to end of body and returns <code>true</code> if done so. This method
	 * needs to be called from {@link #processEvent(ComponentSystemEvent)} during {@link PostAddToViewEvent} or
	 * {@link PostRestoreStateEvent}. This has basically the same effect as setting <code>target="body"</code> on a
	 * component resource.
	 * @param event The involved event, which can be either {@link PostAddToViewEvent} or {@link PostRestoreStateEvent}.
	 * @return <code>true</code> if the move has taken place.
	 */
	protected boolean moveToBody(ComponentSystemEvent event) {
		if (!(event instanceof PostAddToViewEvent || event instanceof PostRestoreStateEvent)) {
			return false;
		}

		FacesContext context = event.getFacesContext();
		UIViewRoot view = context.getViewRoot();

		if (context.isPostback() ? !view.getComponentResources(context, "body").contains(this) : event instanceof PostAddToViewEvent) {
			view.addComponentResource(context, this, "body");
			return true;
		}
		else {
			return false;
		}
	}

}