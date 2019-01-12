/*
 * Copyright 2019 OmniFaces
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
import static javax.faces.component.visit.VisitHint.SKIP_ITERATION;
import static org.omnifaces.cdi.push.SocketChannelManager.ESTIMATED_TOTAL_CHANNELS;
import static org.omnifaces.util.Ajax.oncomplete;
import static org.omnifaces.util.Components.addScriptToBody;
import static org.omnifaces.util.Components.forEachComponent;
import static org.omnifaces.util.Faces.getViewRoot;
import static org.omnifaces.util.FacesLocal.getViewAttribute;
import static org.omnifaces.util.FacesLocal.isAjaxRequest;
import static org.omnifaces.util.FacesLocal.isAjaxRequestWithPartialRendering;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

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
		Map<String, Entry<Serializable, Boolean>> sockets = getSockets(context);

		if (!isAjaxRequest(context)) {
			sockets.clear();
		}

		forEachComponent(context).ofTypes(Socket.class).withHints(SKIP_ITERATION).<Socket>invoke(socket -> {
			if (!sockets.containsKey(socket.getChannel())) {
				return;
			}

			boolean connected = socket.isRendered() && socket.isConnected();
			boolean previouslyConnected = sockets.get(socket.getChannel()).setValue(connected);

			if (connected != previouslyConnected) {
				String script = format(connected ? SCRIPT_OPEN : SCRIPT_CLOSE, socket.getChannel());

				if (isAjaxRequestWithPartialRendering(context)) {
					oncomplete(script);
				}
				else {
					addScriptToBody(script);
				}
			}
		});
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
	 * Register given socket and returns true if it's new. Note that this method is in first place not invoked when
	 * <code>socket.isRendered()</code> returns <code>false</code>, so this check is not done here.
	 */
	static boolean register(FacesContext context, Socket socket) {
		Entry<Serializable, Boolean> currentlyConnectedUser = new AbstractMap.SimpleEntry<>(socket.getUser(), socket.isConnected());
		Entry<Serializable, Boolean> previouslyConnectedUser = getSockets(context).put(socket.getChannel(), currentlyConnectedUser);

		if (previouslyConnectedUser != null && !Objects.equals(previouslyConnectedUser.getKey(), socket.getUser())) {
			SocketChannelManager.getInstance().switchUser(socket.getChannel(), socket.getScope(), previouslyConnectedUser.getKey(), socket.getUser());
		}

		return previouslyConnectedUser == null;
	}

	/**
	 * Helper to remember which sockets are initialized on the view. The map key represents the <code>channel</code>
	 * and the map value represents the last known value of the <code>user</code> and <code>connected</code> attributes.
	 */
	private static Map<String, Entry<Serializable, Boolean>> getSockets(FacesContext context) {
		return getViewAttribute(context, Socket.class.getName(), () -> new HashMap<>(ESTIMATED_TOTAL_CHANNELS));
	}

}