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
package org.omnifaces.component.script;

import java.io.IOException;
import java.util.Collection;

import javax.faces.application.ResourceDependencies;
import javax.faces.application.ResourceDependency;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialViewContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ListenerFor;
import javax.faces.event.ListenersFor;
import javax.faces.event.PostAddToViewEvent;
import javax.faces.event.PostRestoreStateEvent;

/**
 * <strong>OnloadScript</strong> is an extension to <code>&lt;h:outputScript&gt;</code> which will be executed in the
 * end of the HTML body (thus when all HTML elements are initialized in the HTML DOM tree) and will re-execute its
 * script body on every ajax request. This is particularly useful if you want to re-execute a specific helper script
 * to manipulate the HTML DOM tree, such as (re-)adding fancy tooltips, performing highlights, etcetera, also after
 * changes in the HTML DOM tree on ajax responses.
 * <p>
 * You can put it anywhere in the view, it will always be relocated to the end of body.
 * <pre>
 * &lt;o:onloadScript&gt;alert('OnloadScript is invoked!');&lt;/o:onloadScript&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 */
@FacesComponent(OnloadScript.COMPONENT_TYPE)
@ListenersFor({
	@ListenerFor(systemEventClass=PostAddToViewEvent.class),
	@ListenerFor(systemEventClass=PostRestoreStateEvent.class)
})
@ResourceDependencies({
	@ResourceDependency(library="javax.faces", name="jsf.js", target="head"), // Required for jsf.ajax.addOnEvent.
	@ResourceDependency(library="omnifaces", name="omnifaces.js", target="head") // Specifically: ajax.js.
})
public class OnloadScript extends UIOutput {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The standard component type. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.script.OnloadScript";

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String AJAX_SCRIPT_START = "OmniFaces.Ajax.addRunOnceOnSuccess(function(){";
	private static final String AJAX_SCRIPT_END = "});";

	// UIComponent overrides ------------------------------------------------------------------------------------------

	/**
	 * Returns <code>true</code>.
	 */
	@Override
	public boolean getRendersChildren() {
		return true;
	}

	/**
	 * If the event is a {@link PostAddToViewEvent}, then relocate the component to end of body, so that we can make
	 * sure that the script is executed after all HTML DOM elements are been created. If the event is a
	 * {@link PostRestoreStateEvent} and the current request is an ajax request, then add the client ID of this
	 * component to the collection of ajax render IDs, so that we can make sure that the script is executed on every
	 * ajax response as well.
	 */
	@Override
	public void processEvent(ComponentSystemEvent event) throws AbortProcessingException {
		FacesContext context = FacesContext.getCurrentInstance();

		if (event instanceof PostAddToViewEvent) {
			context.getViewRoot().addComponentResource(context, this, "body");
		}
		else if (event instanceof PostRestoreStateEvent) {
			PartialViewContext ajaxContext = context.getPartialViewContext();

			if (ajaxContext.isAjaxRequest()) {
				Collection<String> renderIds = ajaxContext.getRenderIds();
				String clientId = getClientId(context);

				if (!renderIds.contains(clientId)) {
					renderIds.add(clientId);
				}
			}
		}
	}

	/**
	 * If there are any children and the current request is an ajax request, then add the script body as a callback
	 * function to <code>OmniFaces.Ajax.addRunOnceOnSuccess()</code>. The entire <code>&lt;script&gt;</code> element
	 * is wrapped in a <code>&lt;span&gt;</code> element with an ID, so that it can be ajax-updated.
	 */
	@Override
	public void encodeChildren(FacesContext context) throws IOException {
		if (!isRendered()) {
			return;
		}

		ResponseWriter writer = context.getResponseWriter();
		writer.startElement("span", this);
		writer.writeAttribute("id", getClientId(context), "id");
		writer.startElement("script", null);
		writer.writeAttribute("type", "text/javascript", "type");

		if (getChildCount() > 0) {
			boolean ajaxRequest = context.getPartialViewContext().isAjaxRequest();

			if (ajaxRequest) {
				writer.write(AJAX_SCRIPT_START);
			}

			for (UIComponent child : getChildren()) {
				child.encodeAll(context);
			}

			if (ajaxRequest) {
				writer.write(AJAX_SCRIPT_END);
			}
		}

		writer.endElement("script");
		writer.endElement("span");
	}

}