/*
 * Copyright 2015 OmniFaces.
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
package org.omnifaces.cdi.push;

import static java.lang.Boolean.TRUE;
import static org.omnifaces.util.Ajax.oncomplete;
import static org.omnifaces.util.Components.addScriptResourceToHead;
import static org.omnifaces.util.Components.addScriptToBody;
import static org.omnifaces.util.FacesLocal.getRequestContextPath;
import static org.omnifaces.util.FacesLocal.getViewAttribute;
import static org.omnifaces.util.FacesLocal.isAjaxRequestWithPartialRendering;
import static org.omnifaces.util.FacesLocal.setViewAttribute;

import java.util.HashMap;
import java.util.Map;

import javax.el.ValueExpression;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.PostAddToViewEvent;
import javax.faces.event.PreRenderViewEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;

/**
 * This system event listener for {@link UIViewRoot} ensures that the necessary scripts for <code>&lt;o:socket&gt;</code>
 * are properly rendered.
 *
 * @author Bauke Scholtz
 * @see Socket
 * @since 2.3
 */
public class SocketEventListener implements SystemEventListener {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String SCRIPT_OPEN = "OmniFaces.Push.open('%s','%s',%s);";
	private static final String SCRIPT_CLOSE = "OmniFaces.Push.close('%s');";

	// Variables ------------------------------------------------------------------------------------------------------

	private Integer port;
	private String channel;
	private String functions;
	private ValueExpression disabledExpression;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct an instance of socket event listener based on the given channel name, functions and disabled expression.
	 * @param port The port number.
	 * @param channel The channel name.
	 * @param functions The onmessage and onclose functions.
	 * @param disabledExpression The disabled expression.
	 */
	public SocketEventListener(Integer port, String channel, String functions, ValueExpression disabledExpression) {
		this.port = port;
		this.channel = channel;
		this.functions = functions;
		this.disabledExpression = disabledExpression;
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Only listens on {@link UIViewRoot}.
	 */
	@Override
	public boolean isListenerForSource(Object source) {
		return source instanceof UIViewRoot;
	}

	/**
	 * If event is an instance of {@link PostAddToViewEvent}, then add the main <code>omnifaces.js</code> script
	 * resource. Else if event is an instance of {@link PreRenderViewEvent}, and the socket is new and not disabled, or
	 * has just switched the <code>disabled</code> attribute, then render either the <code>open()</code> script or the
	 * <code>close()</code> script. During an ajax request with partial rendering, it's added as
	 * <code>&lt;eval&gt;</code> by partial response writer, else it's just added as a script component with
	 * <code>target="body"</code>.
	 */
	@Override
	public void processEvent(SystemEvent event) throws AbortProcessingException {
		if (event instanceof PostAddToViewEvent) {
			addScriptResourceToHead("omnifaces", "omnifaces.js");
		}
		else if (event instanceof PreRenderViewEvent) {
			FacesContext context = FacesContext.getCurrentInstance();
			boolean disabled = disabledExpression != null && TRUE.equals(disabledExpression.getValue(context.getELContext()));

			if (hasSwitched(context, channel, disabled)) {
				String script = disabled
					? String.format(SCRIPT_CLOSE, channel)
					: String.format(SCRIPT_OPEN, (port != null ? ":" + port : "") + getRequestContextPath(context), channel, functions);

				if (isAjaxRequestWithPartialRendering(context)) {
					oncomplete(script);
				}
				else {
					addScriptToBody(script);
				}
			}
		}
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Helper to remember which channels are opened on the view and returns <code>true</code> if it is new and not
	 * disabled, or has switched its <code>disabled</code> attribute.
	 */
	private static boolean hasSwitched(FacesContext context, String channel, boolean disabled) {
		Map<String, Boolean> channels = getViewAttribute(context, Socket.class.getName());

		if (channels == null) {
			channels = new HashMap<>();
			setViewAttribute(context, Socket.class.getName(), channels);
		}

		Boolean previouslyDisabled = channels.put(channel, disabled);
		return (previouslyDisabled == null) ? !disabled : (previouslyDisabled != disabled);
	}

}