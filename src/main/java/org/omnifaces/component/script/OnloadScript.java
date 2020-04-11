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

import static org.omnifaces.util.Ajax.oncomplete;
import static org.omnifaces.util.Events.subscribeToViewEvent;
import static org.omnifaces.util.FacesLocal.isAjaxRequestWithPartialRendering;

import java.io.IOException;
import java.io.StringWriter;

import jakarta.faces.FacesException;
import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.context.ResponseWriterWrapper;
import jakarta.faces.event.ComponentSystemEvent;
import jakarta.faces.event.ListenerFor;
import jakarta.faces.event.PostAddToViewEvent;
import jakarta.faces.event.PostRestoreStateEvent;
import jakarta.faces.event.PreRenderViewEvent;
import jakarta.faces.event.SystemEvent;
import jakarta.faces.event.SystemEventListener;

import org.omnifaces.util.Ajax;

/**
 * <p>
 * The <code>&lt;o:onloadScript</code> is a component that extends the standard <code>&lt;h:outputScript&gt;</code>
 * which will be executed in the end of the HTML body (thus when all HTML elements are initialized in the HTML DOM tree)
 * and will re-execute its script body on every ajax request. This is particularly useful if you want to re-execute a
 * specific helper script to manipulate the HTML DOM tree, such as (re-)adding fancy tooltips, performing highlights,
 * etcetera, also after changes in the HTML DOM tree on ajax responses.
 * <p>
 * You can put it anywhere in the view, it will always be relocated to the end of body.
 * <pre>
 * &lt;o:onloadScript&gt;alert('OnloadScript is invoked!');&lt;/o:onloadScript&gt;
 * </pre>
 * <p>
 * The <code>&lt;o:onloadScript&gt;</code> is implicitly relocated to the end of the <code>&lt;body&gt;</code>,
 * exactly like as <code>&lt;h:outputScript target="body"&gt;</code> does. So it's always executed when the entire
 * <code>&lt;body&gt;</code> is finished populating and thus you don't need a <code>window.onload</code> or a
 * <code>$(document).ready()</code> in there. Again, the difference with <code>&lt;h:outputScript target="body"&gt;</code>
 * is that the <code>&lt;o:onloadScript&gt;</code> is also executed on every ajax request.
 *
 * @author Bauke Scholtz
 * @see ScriptFamily
 */
@FacesComponent(OnloadScript.COMPONENT_TYPE)
@ListenerFor(systemEventClass=PostAddToViewEvent.class)
@ListenerFor(systemEventClass=PostRestoreStateEvent.class)
public class OnloadScript extends ScriptFamily implements SystemEventListener {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The standard component type. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.script.OnloadScript";

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Move this component to body using {@link #moveToBody(ComponentSystemEvent)}, and if the event is a
	 * {@link PostRestoreStateEvent}, then subscribe this component to {@link PreRenderViewEvent}, which will invoke
	 * {@link #processEvent(SystemEvent)}.
	 */
	@Override
	public void processEvent(ComponentSystemEvent event) {
		moveToBody(event);

		if (event instanceof PostRestoreStateEvent) {
			subscribeToViewEvent(PreRenderViewEvent.class, this);
		}
	}

	/**
	 * Returns <code>true</code> if the given source is an instance of {@link OnloadScript} or {@link UIViewRoot}.
	 */
	@Override
	public boolean isListenerForSource(Object source) {
		return source instanceof OnloadScript || source instanceof UIViewRoot;
	}

	/**
	 * If the event is a {@link PreRenderViewEvent}, and this component is rendered, and the current request is an ajax
	 * request with partial rendering, then encode the children as {@link Ajax#oncomplete(String...)}.
	 */
	@Override
	public void processEvent(SystemEvent event) {
		if (!(event instanceof PreRenderViewEvent) || !isRendered()) {
			return;
		}

		FacesContext context = getFacesContext();

		if (!isAjaxRequestWithPartialRendering(context)) {
			return;
		}

		pushComponentToEL(context, this);
		StringWriter buffer = new StringWriter();
		ResponseWriter originalResponseWriter = context.getResponseWriter();
		String encoding = context.getExternalContext().getRequestCharacterEncoding();
		context.getExternalContext().setResponseCharacterEncoding(encoding);
		ResponseWriter writer = context.getRenderKit().createResponseWriter(buffer, null, encoding);
		context.setResponseWriter(new ResponseWriterWrapper(writer) {
			@Override
			public void writeText(Object text, String property) throws IOException {
				getWrapped().write(text.toString()); // So, don't escape HTML.
			}
		});

		try {
			encodeChildren(context);
		}
		catch (IOException e) {
			throw new FacesException(e);
		}
		finally {
			popComponentFromEL(context);

			if (originalResponseWriter != null) {
				context.setResponseWriter(originalResponseWriter);
			}
		}

		String script = buffer.toString().trim();

		if (!script.isEmpty()) {
			oncomplete(script);
		}
	}

	/**
	 * If the current request is not an ajax request with partial rendering, then encode begin.
	 */
	@Override
	public void encodeBegin(FacesContext context) throws IOException {
		if (!isAjaxRequestWithPartialRendering(context)) {
			super.encodeBegin(context);
		}
	}

	/**
	 * If the current request is not an ajax request with partial rendering, then encode end.
	 */
	@Override
	public void encodeEnd(FacesContext context) throws IOException {
		if (!isAjaxRequestWithPartialRendering(context)) {
			super.encodeEnd(context);
		}
	}

}