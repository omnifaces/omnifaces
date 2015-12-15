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
import static java.util.Collections.synchronizedSet;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.omnifaces.util.FacesLocal.getSessionAttribute;
import static org.omnifaces.util.FacesLocal.getViewAttribute;
import static org.omnifaces.util.FacesLocal.setSessionAttribute;
import static org.omnifaces.util.FacesLocal.setViewAttribute;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.faces.FacesException;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.websocket.DeploymentException;
import javax.websocket.EndpointConfig;
import javax.websocket.HandshakeResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;

/**
 * <p>
 * Socket configurator and helper.
 *
 * @author Bauke Scholtz
 */
public class SocketConfigurator extends Configurator {

	// Constants ------------------------------------------------------------------------------------------------------

	/** The context parameter name to explicitly register web socket endpoint during startup (only required in some containers). */
	public static final String PARAM_SOCKET_ENDPOINT_ALWAYS_ENABLED = "org.omnifaces.SOCKET_ENDPOINT_ALWAYS_ENABLED";

	private static final String KEY = Socket.class.getName();
	private static final String ERROR_ENDPOINT_REGISTRATION = "o:socket endpoint cannot be registered.";

	// Actions and helpers --------------------------------------------------------------------------------------------

	/**
	 * Register web socket endpoint if necessary, i.e. when it's not registered yet, or is explicitly enabled via
	 * context param.
	 * @param context The involved servlet context.
	 * @param checkParam Whether to check the context param or not.
	 */
	public static void registerEndpointIfNecessary(ServletContext context, boolean checkParam) {
		Boolean registered = (Boolean) context.getAttribute(KEY);

		if (TRUE.equals(registered)) {
			return;
		}
		else if (checkParam && !TRUE.equals(Boolean.valueOf(context.getInitParameter(PARAM_SOCKET_ENDPOINT_ALWAYS_ENABLED)))) {
			return;
		}

		try {
			ServerContainer serverContainer = (ServerContainer) context.getAttribute(ServerContainer.class.getName());
			ServerEndpointConfig serverEndpointConfig = ServerEndpointConfig.Builder
				.create(SocketEndpoint.class, SocketEndpoint.URI_TEMPLATE).configurator(new SocketConfigurator()).build();
			serverContainer.addEndpoint(serverEndpointConfig);
			context.setAttribute(KEY, TRUE);
		}
		catch (DeploymentException e) {
			throw new FacesException(ERROR_ENDPOINT_REGISTRATION, e);
		}
	}

	/**
	 * Register web socket channel name in HTTP session.
	 */
	static void registerChannel(FacesContext context, String channel) {
		Set<String> registeredChannels = getSessionAttribute(context, KEY);

		if (registeredChannels == null) {
			registeredChannels = synchronizedSet(new HashSet<String>());
			setSessionAttribute(context, KEY, registeredChannels);
		}

		registeredChannels.add(channel);
	}

	/**
	 * Helper to remember which channels are opened on the view and returns <code>true</code> if it is new and not
	 * disabled, or has switched its <code>disabled</code> attribute.
	 */
	static boolean hasSwitched(FacesContext context, String channel, boolean disabled) {
		Map<String, Boolean> channels = getViewAttribute(context, KEY);

		if (channels == null) {
			channels = new HashMap<>();
			setViewAttribute(context, KEY, channels);
		}

		Boolean previouslyDisabled = channels.put(channel, disabled);
		return (previouslyDisabled == null) ? !disabled : (previouslyDisabled != disabled);
	}

	/**
	 * On handshake, store the HTTP session in server endpoint config.
	 */
	@Override
	public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
		Object httpSession = request.getHttpSession();

		if (httpSession != null) { // May be null when session is cleared after server restart while socket is still open in client side.
			config.getUserProperties().put(KEY, httpSession);
		}
	}

	/**
	 * Returns <code>true</code> if web socket channel is registered in HTTP session.
	 */
	@SuppressWarnings("unchecked")
	static boolean isRegisteredChannel(EndpointConfig config, String channel) {
		HttpSession httpSession = (HttpSession) config.getUserProperties().get(KEY);
		Set<String> registeredChannels = (httpSession != null) ? (Set<String>) httpSession.getAttribute(KEY) : null;
		return registeredChannels != null && registeredChannels.contains(channel);
	}

	/**
	 * Align out the endpoint session timeout with HTTP session timeout.
	 */
	static void alignMaxIdleTimeout(EndpointConfig config, Session session) {
		HttpSession httpSession = (HttpSession) config.getUserProperties().get(KEY);
		session.setMaxIdleTimeout(SECONDS.toMillis(httpSession.getMaxInactiveInterval()));
	}

}