/*
 * Copyright 2016 OmniFaces.
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
import static org.omnifaces.cdi.push.SocketChannelManager.ESTIMATED_TOTAL_CHANNELS;
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
import javax.faces.event.PostAddToViewEvent;
import javax.faces.event.PreRenderViewEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;

/**
 * <p>
 * This JSF listener for {@link UIViewRoot} ensures that the necessary JavaScript code to initialize, open or close
 * the <code>WebSocket</code> is properly rendered.
 *
 * @author Bauke Scholtz
 * @see Socket
 * @since 2.3
 */
public class SocketFacesListener implements SystemEventListener {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String SCRIPT_INIT = "OmniFaces.Push.init('%s','%s',%s,%s);";
	private static final String SCRIPT_OPEN = "OmniFaces.Push.open('%s');";
	private static final String SCRIPT_CLOSE = "OmniFaces.Push.close('%s');";

	// Variables ------------------------------------------------------------------------------------------------------

	private Integer port;
	private String channel;
	private String uri;
	private String functions;
	private ValueExpression connectedExpression;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct an instance of socket event listener based on the given channel, uri, functions and connected expression.
	 * @param port The port number.
	 * @param channel The channel name.
	 * @param uri The uri of the web socket representing the channel identifier, which is composed of channel name and
	 * scope identifier, separated by a question mark.
	 * All open websockets on the same uri will receive the same push notification from the server.
	 * @param functions The onopen, onmessage and onclose functions.
	 * @param connectedExpression The connected expression.
	 */
	public SocketFacesListener(Integer port, String channel, String uri, String functions, ValueExpression connectedExpression) {
		this.port = port;
		this.channel = channel;
		this.uri = uri;
		this.functions = functions;
		this.connectedExpression = connectedExpression;
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
	 * resource. Else if event is an instance of {@link PreRenderViewEvent}, and the socket is new, then render the
	 * <code>init()</code> script, or if it has just switched the <code>connected</code> attribute, then render either
	 * the <code>open()</code> script or the <code>close()</code> script. During an ajax request with partial rendering,
	 * it's added as <code>&lt;eval&gt;</code> by partial response writer, else it's just added as a script component
	 * with <code>target="body"</code>. Those scripts will in turn hit {@link SocketEndpoint}.
	 */
	@Override
	public void processEvent(SystemEvent event) {
		if (event instanceof PostAddToViewEvent) {
			addScriptResourceToHead("omnifaces", "omnifaces.js");
		}
		else if (event instanceof PreRenderViewEvent) {
			FacesContext context = FacesContext.getCurrentInstance();
			boolean connected = connectedExpression == null || TRUE.equals(connectedExpression.getValue(context.getELContext()));
			Boolean switched = hasSwitched(context, channel, connected);
			String script = null;

			if (switched == null) {
				String host = (port != null ? ":" + port : "") + getRequestContextPath(context);
				script = String.format(SCRIPT_INIT, host, uri, functions, connected);
			}
			else if (switched) {
				script = String.format(connected ? SCRIPT_OPEN : SCRIPT_CLOSE, channel);
			}

			if (script != null) {
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
	 * Helper to remember which channels are opened on the view and returns <code>null</code> if it is new, or
	 * <code>true</code> or <code>false</code> if it has switched its <code>connected</code> attribute.
	 */
	private static Boolean hasSwitched(FacesContext context, String channel, boolean connected) {
		Map<String, Boolean> channels = getViewAttribute(context, Socket.class.getName());

		if (channels == null) {
			channels = new HashMap<>(ESTIMATED_TOTAL_CHANNELS);
			setViewAttribute(context, Socket.class.getName(), channels);
		}

		Boolean previouslyConnected = channels.put(channel, connected);
		return (previouslyConnected == null) ? null : (previouslyConnected != connected);
	}

}