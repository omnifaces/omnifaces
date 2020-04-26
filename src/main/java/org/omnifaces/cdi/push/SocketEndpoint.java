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
package org.omnifaces.cdi.push;

import static java.util.logging.Level.SEVERE;
import static javax.websocket.CloseReason.CloseCodes.GOING_AWAY;
import static javax.websocket.CloseReason.CloseCodes.VIOLATED_POLICY;
import static org.omnifaces.cdi.PushContext.URI_PREFIX;

import java.io.IOException;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

/**
 * <p>
 * This web socket server endpoint handles web socket requests coming from <code>&lt;o:socket&gt;</code>.
 *
 * @author Bauke Scholtz
 * @see Socket
 * @since 2.3
 */
public class SocketEndpoint extends Endpoint {

	// Constants ------------------------------------------------------------------------------------------------------

	/** The URI path parameter name of the web socket channel. */
	static final String PARAM_CHANNEL = "channel";

	/** The context-relative URI template where the web socket endpoint should listen on. */
	public static final String URI_TEMPLATE = URI_PREFIX + "/{" + PARAM_CHANNEL + "}";

	private static final Logger logger = Logger.getLogger(SocketEndpoint.class.getName());
	private static final CloseReason REASON_UNKNOWN_CHANNEL = new CloseReason(VIOLATED_POLICY, "Unknown channel");
	private static final String ERROR_EXCEPTION = "SocketEndpoint: An exception occurred during processing web socket request.";

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Add given web socket session to the {@link SocketSessionManager}. If web socket session is not accepted (i.e. the
	 * channel identifier is unknown), then immediately close with reason VIOLATED_POLICY (close code 1008).
	 * @param session The opened web socket session.
	 * @param config The endpoint configuration.
	 */
	@Override
	public void onOpen(Session session, EndpointConfig config) {
		if (SocketSessionManager.getInstance().add(session)) { // @Inject in Endpoint doesn't work in Tomcat+Weld/OWB.
			session.setMaxIdleTimeout(0);
		}
		else {
			try {
				session.close(REASON_UNKNOWN_CHANNEL);
			}
			catch (IOException e) {
				onError(session, e);
			}
		}
	}

	/**
	 * Delegate exception to onClose.
	 * @param session The errored web socket session.
	 * @param throwable The cause.
	 */
	@Override
	public void onError(Session session, Throwable throwable) {
		if (session.isOpen()) {
			session.getUserProperties().put(Throwable.class.getName(), throwable);
		}
	}

	/**
	 * Remove given web socket session from the {@link SocketSessionManager}. If there is any exception from onError which was
	 * not caused by GOING_AWAY, then log it. Tomcat &lt;= 8.0.30 is known to throw an unnecessary exception when client
	 * abruptly disconnects, see also <a href="https://bz.apache.org/bugzilla/show_bug.cgi?id=57489">issue 57489</a>.
	 * @param session The closed web socket session.
	 * @param reason The close reason.
	 */
	@Override
	public void onClose(Session session, CloseReason reason) {
		SocketSessionManager.getInstance().remove(session, reason); // @Inject in Endpoint doesn't work in Tomcat+Weld/OWB and CDI.current() during WS close doesn't work in WildFly.

		Throwable throwable = (Throwable) session.getUserProperties().remove(Throwable.class.getName());

		if (throwable != null && reason.getCloseCode() != GOING_AWAY) {
			logger.log(SEVERE, ERROR_EXCEPTION, throwable);
		}
	}

}