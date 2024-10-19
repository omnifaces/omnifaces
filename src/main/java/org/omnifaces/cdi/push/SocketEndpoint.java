/*
 * Copyright OmniFaces
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

import static jakarta.websocket.CloseReason.CloseCodes.GOING_AWAY;
import static jakarta.websocket.CloseReason.CloseCodes.VIOLATED_POLICY;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import static org.omnifaces.cdi.PushContext.URI_PREFIX;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.websocket.CloseReason;
import jakarta.websocket.CloseReason.CloseCodes;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;

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
     * not caused by {@link CloseCodes#GOING_AWAY} (i.e. "connection reset by peer"), then log it as {@link Level#SEVERE},
     * else as {@link Level#FINE}. Before OmniFaces 4.6, the {@link CloseCodes#GOING_AWAY} was not logged at all.
     * @param session The closed web socket session.
     * @param reason The close reason.
     */
    @Override
    public void onClose(Session session, CloseReason reason) {
        SocketSessionManager.getInstance().remove(session, reason); // @Inject in Endpoint doesn't work in Tomcat+Weld/OWB and CDI.current() during WS close doesn't work in WildFly.

        var throwable = (Throwable) session.getUserProperties().remove(Throwable.class.getName());

        if (throwable != null) {
            logger.log(reason.getCloseCode() == GOING_AWAY ? FINE : SEVERE, ERROR_EXCEPTION, throwable);
        }
    }

}