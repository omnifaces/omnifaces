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

import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.websocket.CloseReason.CloseCodes.UNEXPECTED_CONDITION;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.FacesException;
import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.HandshakeResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;

import org.omnifaces.config.BeanManager;

/**
 * <p>
 * The web socket server endpoint of <code>&lt;o:socket&gt;</code>.
 * This is programmatically registered once a <code>&lt;o:socket&gt;</code> is used for first time in application.
 *
 * @author Bauke Scholtz
 * @see Socket
 * @since 2.3
 */
public class SocketEndpoint extends Endpoint {

	public static final String URI_TEMPLATE = PushContext.URI_PREFIX + "/{channel}";

	private static final Logger logger = Logger.getLogger(SocketEndpoint.class.getName());
	private static final String ERROR_UNKNOWN_CHANNEL = "Unknown channel '%s'.";
	private static final String ERROR_EXCEPTION = "SocketEndpoint: An exception occurred during processing web socket request.";

	/**
	 * Add given web socket session to the push context associated with its channel.
	 * @param session The opened web socket session.
	 * @param config The endpoint configuration.
	 * @throws IllegalArgumentException When the channel is not known as a registered channel (i.e. it's nowhere used in an o:socket).
	 */
	@Override
	public void onOpen(Session session, EndpointConfig config) {
		String channel = session.getPathParameters().get("channel");

		if (!HttpSessionAwareConfigurator.isRegisteredChannel(config, channel)) {
			try {
				session.close(new CloseReason(UNEXPECTED_CONDITION, String.format(ERROR_UNKNOWN_CHANNEL, channel)));
				return;
			}
			catch (IOException e) {
				throw new FacesException(e);
			}
		}

		HttpSessionAwareConfigurator.alignMaxIdleTimeout(config, session);
		BeanManager.INSTANCE.getReference(SocketPushContext.class).add(session, channel); // @Inject in Endpoint doesn't work in Tomcat+Weld.
	}

	/**
	 * Delegate exceptions to logger.
	 * @param session The errored web socket session.
	 * @param throwable The cause.
	 */
	@Override
	public void onError(Session session, Throwable throwable) {
		logger.log(Level.SEVERE, ERROR_EXCEPTION, throwable);
	}

	/**
	 * Remove given web socket session from the push context.
	 * @param session The closed web socket session.
	 * @param reason The close reason.
	 */
	@Override
	public void onClose(Session session, CloseReason reason) {
		BeanManager.INSTANCE.getReference(SocketPushContext.class).remove(session); // @Inject in Endpoint doesn't work in Tomcat+Weld.
	}

	// Nested classes -------------------------------------------------------------------------------------------------

	static class HttpSessionAwareConfigurator extends Configurator {

		@Override
		public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
			Object httpSession = request.getHttpSession();

			if (httpSession != null) { // May be null when session is cleared after server restart while socket is still open in client side.
				config.getUserProperties().put("httpSession", httpSession);
			}
		}

		@SuppressWarnings("unchecked")
		static boolean isRegisteredChannel(EndpointConfig config, String channel) {
			HttpSession httpSession = (HttpSession) config.getUserProperties().get("httpSession");
			Set<String> registeredChannels = (httpSession != null) ? (Set<String>) httpSession.getAttribute(Socket.class.getName()) : null;
			return registeredChannels != null && registeredChannels.contains(channel);
		}

		static void alignMaxIdleTimeout(EndpointConfig config, Session session) {
			HttpSession httpSession = (HttpSession) config.getUserProperties().get("httpSession");
			session.setMaxIdleTimeout(SECONDS.toMillis(httpSession.getMaxInactiveInterval()));
		}

	}

}