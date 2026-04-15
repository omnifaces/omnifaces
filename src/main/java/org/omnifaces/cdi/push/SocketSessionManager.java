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

import static jakarta.websocket.CloseReason.CloseCodes.NORMAL_CLOSURE;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.WARNING;
import static org.omnifaces.cdi.push.SocketEndpoint.PARAM_CHANNEL;
import static org.omnifaces.util.Beans.getReference;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Inject;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;

import org.omnifaces.cdi.push.SocketEvent.Closed;
import org.omnifaces.cdi.push.SocketEvent.Opened;
import org.omnifaces.util.Beans;
import org.omnifaces.util.Hacks;

/**
 * <p>
 * This web socket session manager holds all web socket sessions by their channel identifier.
 *
 * @author Bauke Scholtz
 * @see SocketEndpoint
 * @since 2.3
 */
@ApplicationScoped
public class SocketSessionManager extends PushSessionManager<Session> {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final Logger logger = Logger.getLogger(SocketSessionManager.class.getName());

    private static final CloseReason REASON_EXPIRED = new CloseReason(NORMAL_CLOSURE, "Expired");
    private static final long TOMCAT_WEB_SOCKET_RETRY_TIMEOUT = 10; // Milliseconds.
    private static final long TOMCAT_WEB_SOCKET_MAX_RETRIES = 100; // So, that's retrying for about 1 second.
    private static final String WARNING_TOMCAT_WEB_SOCKET_BOMBED = "Tomcat cannot handle concurrent push messages."
        + " A push message has been sent only after %s retries of " + TOMCAT_WEB_SOCKET_RETRY_TIMEOUT + "ms apart."
        + " Consider rate limiting sending push messages. For example, once every 500ms.";
    private static final String ERROR_TOMCAT_WEB_SOCKET_BOMBED = "Tomcat cannot handle concurrent push messages."
        + " A push message could NOT be sent after %s retries of " + TOMCAT_WEB_SOCKET_RETRY_TIMEOUT + "ms apart."
        + " Consider rate limiting sending push messages. For example, once every 500ms.";

    private static volatile SocketSessionManager instance;

    // Properties -----------------------------------------------------------------------------------------------------

    @Inject
    private SocketUserManager socketUsers;

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * On open, add given web socket session to the mapping associated with its channel identifier and returns <code>true</code> if it's accepted (i.e. the
     * channel identifier is known) and the same session hasn't been added before, otherwise <code>false</code>.
     *
     * @param session The opened web socket session.
     * @return <code>true</code> if given web socket session is accepted and is new, otherwise <code>false</code>.
     */
    protected boolean add(Session session) {
        var channelId = getChannelId(session);

        if (addSession(channelId, session)) {
            var user = socketUsers.getUser(getChannel(session), channelId);

            if (user != null) {
                session.getUserProperties().put("user", user);
            }

            fireEvent(session, null, Opened.LITERAL);
            return true;
        }

        return false;
    }

    /**
     * On close, remove given web socket session from the mapping.
     *
     * @param session The closed web socket session.
     * @param reason The close reason.
     */
    protected void remove(Session session, CloseReason reason) {
        if (removeSession(getChannelId(session), session)) {
            fireEvent(session, reason, Closed.LITERAL);
        }
    }

    // Template methods -----------------------------------------------------------------------------------------------

    @Override
    protected Future<Void> sendToSession(Session session, String message) {
        return sendToSession(session, message, true);
    }

    @Override
    protected boolean isOpen(Session session) {
        return session.isOpen();
    }

    @Override
    protected void closeSession(Session session) {
        if (session.isOpen()) {
            try {
                session.close(REASON_EXPIRED);
            }
            catch (IOException ignore) {
                logger.log(FINEST, "Ignoring thrown exception; there is nothing more we could do here.", ignore);
            }
        }
    }

    // Internal -------------------------------------------------------------------------------------------------------

    /**
     * Internal usage only. Awkward workaround for it being unavailable via {@code @Inject} in endpoint in Tomcat+Weld/OWB. The instance is refreshed on every
     * successful CDI lookup so that hot-redeploys are picked up automatically. When CDI is unavailable (e.g. during {@code SocketEndpoint#onClose()} in
     * WildFly), the last cached instance is returned.
     */
    static SocketSessionManager getInstance() {
        try {
            var current = getReference(SocketSessionManager.class);

            if (current != null) {
                instance = current;
            }
        }
        catch (Exception ignore) {
            // CDI unavailable (e.g. during onClose in WildFly), fall back to last cached instance.
        }

        return instance;
    }

    // Helpers --------------------------------------------------------------------------------------------------------

    private Future<Void> sendToSession(Session session, String text, boolean retrySendTomcatWebSocket) {
        try {
            return session.getAsyncRemote().sendText(text);
        }
        catch (IllegalStateException e) {
            if (Hacks.isTomcatWebSocketBombed(session, e)) {
                if (retrySendTomcatWebSocket) {
                    return CompletableFuture.supplyAsync(() -> retrySendTomcatWebSocket(session, text));
                }
                else {
                    return null;
                }
            }
            else {
                throw e;
            }
        }
    }

    private Void retrySendTomcatWebSocket(Session session, String text) {
        var retries = 0;
        Exception cause = null;

        try {
            while (++retries < TOMCAT_WEB_SOCKET_MAX_RETRIES) {
                Thread.sleep(TOMCAT_WEB_SOCKET_RETRY_TIMEOUT);

                if (!session.isOpen()) {
                    throw new IllegalStateException("Too bad, session is now closed");
                }

                var result = sendToSession(session, text, false);

                if (result != null) {
                    if (logger.isLoggable(WARNING)) {
                        logger.log(WARNING, WARNING_TOMCAT_WEB_SOCKET_BOMBED.formatted(retries));
                    }

                    return result.get();
                }
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cause = e;
        }
        catch (Exception e) {
            cause = e;
        }

        throw new UnsupportedOperationException(ERROR_TOMCAT_WEB_SOCKET_BOMBED.formatted(retries), cause);
    }

    private static String getChannel(Session session) {
        return session.getPathParameters().get(PARAM_CHANNEL);
    }

    private static String getChannelId(Session session) {
        return getChannel(session) + "?" + session.getQueryString();
    }

    private static void fireEvent(Session session, CloseReason reason, AnnotationLiteral<?> qualifier) {
        var user = (Serializable) session.getUserProperties().get("user");
        Beans.fireEvent(new SocketEvent(getChannel(session), user, null, reason != null ? reason.getCloseCode() : null), qualifier);
    }

}
