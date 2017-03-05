/*
 * Copyright 2017 OmniFaces
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

import static java.lang.String.format;
import static org.omnifaces.cdi.push.SocketChannelManager.ESTIMATED_TOTAL_CHANNELS;
import static org.omnifaces.util.Ajax.oncomplete;
import static org.omnifaces.util.Components.addScriptToBody;
import static org.omnifaces.util.Components.findComponent;
import static org.omnifaces.util.Faces.getViewRoot;
import static org.omnifaces.util.FacesLocal.getViewAttribute;
import static org.omnifaces.util.FacesLocal.isAjaxRequestWithPartialRendering;
import static org.omnifaces.util.FacesLocal.setViewAttribute;

import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.PostAddToViewEvent;
import javax.faces.event.PreRenderViewEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;

/**
 * <p>
 * This JSF listener for {@link UIViewRoot} ensures that the necessary JavaScript code to open or close the
 * <code>WebSocket</code> is properly rendered.
 *
 * @author Bauke Scholtz
 * @see Socket
 * @since 2.3
 */
public class SocketFacesListener implements SystemEventListener {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String SCRIPT_OPEN = "OmniFaces.Push.open('%s');";
	private static final String SCRIPT_CLOSE = "OmniFaces.Push.close('%s');";

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Only listens on {@link UIViewRoot}.
	 */
	@Override
	public boolean isListenerForSource(Object source) {
		return source instanceof UIViewRoot;
	}

	/**
	 * If the socket has just switched the <code>connected</code> attribute, then render either the <code>open()</code>
	 * script or the <code>close()</code> script. During an ajax request with partial rendering, it's added as
	 * <code>&lt;eval&gt;</code> by partial response writer, else it's just added as a script component with
	 * <code>target="body"</code>. Those scripts will in turn hit {@link SocketEndpoint}.
	 */
	@Override
	public void processEvent(SystemEvent event) {
		if (!(event instanceof PreRenderViewEvent)) {
			return;
		}

		FacesContext context = FacesContext.getCurrentInstance();

		for (Entry<String, Boolean> entry : getSockets(context).entrySet()) {
			Socket socket = findComponent(entry.getKey());
			boolean connected = socket.isRendered() && socket.isConnected();
			boolean previouslyConnected = entry.setValue(connected);

			if (connected != previouslyConnected) {
				String script = format(connected ? SCRIPT_OPEN : SCRIPT_CLOSE, socket.getChannel());

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
	 * Subscribe this socket faces listener to the current view if necessary.
	 */
	static void subscribeIfNecessary() {
		UIViewRoot view = getViewRoot();
		List<SystemEventListener> listeners = view.getListenersForEventClass(PostAddToViewEvent.class);

		if (listeners != null) {
			for (SystemEventListener listener : listeners) {
				if (listener instanceof SocketFacesListener) {
					return;
				}
			}
		}

		view.subscribeToViewEvent(PreRenderViewEvent.class, new SocketFacesListener());
	}

	/**
	 * Register given socket and returns true if it's new.
	 */
	static boolean register(FacesContext context, Socket socket) {
		return getSockets(context).putIfAbsent(socket.getClientId(context), socket.isConnected()) == null;
	}

	/**
	 * Helper to remember which sockets are initialized on the view. The map key represents the client ID and the
	 * map value represents the last known value of the <code>connected</code> attribute.
	 */
	private static ConcurrentMap<String, Boolean> getSockets(FacesContext context) {
		ConcurrentMap<String, Boolean> sockets = getViewAttribute(context, Socket.class.getName());

		if (sockets == null) {
			sockets = new ConcurrentHashMap<>(ESTIMATED_TOTAL_CHANNELS);
			setViewAttribute(context, Socket.class.getName(), sockets);
		}

		return sockets;
	}

}