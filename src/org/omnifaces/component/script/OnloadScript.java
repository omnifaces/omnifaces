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
import java.io.StringWriter;

import javax.faces.FacesException;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ListenerFor;
import javax.faces.event.ListenersFor;
import javax.faces.event.PostAddToViewEvent;
import javax.faces.event.PostRestoreStateEvent;
import javax.faces.event.PreRenderViewEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;

import org.omnifaces.util.Ajax;
import org.omnifaces.util.Events;

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
public class OnloadScript extends ScriptFamily implements SystemEventListener {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The standard component type. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.script.OnloadScript";

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns <code>true</code> if the given source is an instance of {@link UIViewRoot}.
	 */
	@Override
	public boolean isListenerForSource(Object source) {
        return source instanceof UIViewRoot;
	}

	/**
	 * If the event is a {@link PostAddToViewEvent}, then relocate the component to end of body, so that we can make
	 * sure that the script is executed after all HTML DOM elements are been created. Else if the event is a
	 * {@link PostRestoreStateEvent} and the current request is an ajax request, then subscribe to the
	 * {@link PreRenderViewEvent} event.
	 */
	@Override
	public void processEvent(ComponentSystemEvent event) throws AbortProcessingException {
		FacesContext context = FacesContext.getCurrentInstance();

		if (event instanceof PostAddToViewEvent) {
			context.getViewRoot().addComponentResource(context, this, "body");
		}
		else if (event instanceof PostRestoreStateEvent && context.getPartialViewContext().isAjaxRequest()) {
			Events.subscribeToViewEvent(PreRenderViewEvent.class, this);
		}
	}

	/**
	 * If the event is a {@link PreRenderViewEvent} and the current request is an ajax request and this component is
	 * rendered and there are any children, then encode the children as {@link Ajax#oncomplete(String...)}.
	 */
	@Override
	public void processEvent(SystemEvent event) throws AbortProcessingException {
		if (!(event instanceof PreRenderViewEvent)) {
			return;
		}

		FacesContext context = FacesContext.getCurrentInstance();

		if (!context.getPartialViewContext().isAjaxRequest() || !isRendered() || getChildCount() == 0) {
			return;
		}

		ResponseWriter originalResponseWriter = context.getResponseWriter();
		StringWriter buffer = new StringWriter();
		context.setResponseWriter(originalResponseWriter.cloneWithWriter(buffer));

		try {
			encodeChildren(context);
		}
		catch (IOException e) {
			throw new FacesException(e);
		}

		context.setResponseWriter(originalResponseWriter);
		String script = buffer.toString().trim();

		if (!script.isEmpty()) {
			Ajax.oncomplete(script);
		}
	}

	/**
	 * If this component is rendered and there are any children, then start the <code>&lt;script&gt;</code> element.
	 */
	@Override
	public void encodeBegin(FacesContext context) throws IOException {
		if (!isRendered() || getChildCount() == 0) {
			return;
		}

		pushComponentToEL(context, this);
		ResponseWriter writer = context.getResponseWriter();
		writer.startElement("script", this);
		writer.writeAttribute("type", "text/javascript", "type");
	}

	/**
	 * If this component is rendered and there are any children, then end the <code>&lt;script&gt;</code> element.
	 */
	@Override
	public void encodeEnd(FacesContext context) throws IOException {
		if (!isRendered() || getChildCount() == 0) {
			return;
		}

		context.getResponseWriter().endElement("script");
		popComponentFromEL(context);
	}

}