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
import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.WARNING;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.omnifaces.cdi.push.SocketEndpoint.PARAM_CHANNEL;
import static org.omnifaces.util.Beans.getReference;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
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
public class SocketSessionManager {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final Logger logger = Logger.getLogger(SocketSessionManager.class.getName());

    private static final CloseReason REASON_EXPIRED = new CloseReason(NORMAL_CLOSURE, "Expired");
    private static final long TOMCAT_WEB_SOCKET_RETRY_TIMEOUT = 10; // Milliseconds.
    private static final long TOMCAT_WEB_SOCKET_MAX_RETRIES = 100; // So, that's retrying for about 1 second.
    private static final String WARNING_TOMCAT_WEB_SOCKET_BOMBED =
        "Tomcat cannot handle concurrent push messages."
            + " A push message has been sent only after %s retries of " + TOMCAT_WEB_SOCKET_RETRY_TIMEOUT + "ms apart."
            + " Consider rate limiting sending push messages. For example, once every 500ms.";
    private static final String ERROR_TOMCAT_WEB_SOCKET_BOMBED =
        "Tomcat cannot handle concurrent push messages."
            + " A push message could NOT be sent after %s retries of " + TOMCAT_WEB_SOCKET_RETRY_TIMEOUT + "ms apart."
            + " Consider rate limiting sending push messages. For example, once every 500ms.";

    private static SocketSessionManager instance;

    // Properties -----------------------------------------------------------------------------------------------------

    private final ConcurrentHashMap<String, Collection<Session>> socketSessions = new ConcurrentHashMap<>();

    @Inject
    private SocketUserManager socketUsers;

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * Register given channel identifier.
     * @param channelId The channel identifier to register.
     */
    protected void register(String channelId) {
        socketSessions.computeIfAbsent(channelId, $ -> new ConcurrentLinkedQueue<>());
    }

    /**
     * Register given channel identifiers.
     * @param channelIds The channel identifiers to register.
     */
    protected void register(Iterable<String> channelIds) {
        channelIds.forEach(this::register);
    }

    /**
     * On open, add given web socket session to the mapping associated with its channel identifier and returns
     * <code>true</code> if it's accepted (i.e. the channel identifier is known) and the same session hasn't been added
     * before, otherwise <code>false</code>.
     * @param session The opened web socket session.
     * @return <code>true</code> if given web socket session is accepted and is new, otherwise <code>false</code>.
     */
    protected boolean add(Session session) {
        var channelId = getChannelId(session);
        var sessions = socketSessions.get(channelId);

        if (sessions != null && sessions.add(session)) {
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
     * Send the given message to all open web socket sessions associated with given web socket channel identifier.
     * @param channelId The web socket channel identifier.
     * @param message The push message string.
     * @return The results of the send operation. If it returns an empty set, then there was no open session associated
     * with given channel identifier. The returned futures will return <code>null</code> on {@link Future#get()} if the
     * message was successfully delivered and otherwise throw {@link ExecutionException}.
     */
    protected Set<Future<Void>> send(String channelId, String message) {
        var sessions = channelId != null ? socketSessions.get(channelId) : null;

        if (sessions != null) {
            return sessions.stream()
                    .filter(Session::isOpen)
                    .map(session -> send(session, message, true))
                    .filter(Objects::nonNull)
                    .collect(toUnmodifiableSet());
        }

        return emptySet();
    }

    private Future<Void> send(Session session, String text, boolean retrySendTomcatWebSocket) {
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

                var result = send(session, text, false);

                if (result != null) {
                    if (logger.isLoggable(WARNING)) {
                        logger.log(WARNING, format(WARNING_TOMCAT_WEB_SOCKET_BOMBED, retries));
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

        throw new UnsupportedOperationException(format(ERROR_TOMCAT_WEB_SOCKET_BOMBED, retries), cause);
    }

    /**
     * On close, remove given web socket session from the mapping.
     * @param session The closed web socket session.
     * @param reason The close reason.
     */
    protected void remove(Session session, CloseReason reason) {
        var sessions = socketSessions.get(getChannelId(session));

        if (sessions != null && sessions.remove(session)) {
            fireEvent(session, reason, Closed.LITERAL);
        }
    }

    /**
     * Deregister given channel identifiers and explicitly close all open web socket sessions associated with it.
     * @param channelIds The channel identifiers to deregister.
     */
    protected void deregister(Iterable<String> channelIds) {
        for (var channelId : channelIds) {
            var sessions = socketSessions.remove(channelId);

            if (sessions != null) {
                sessions.forEach(SocketSessionManager::close);
            }
        }
    }

    /**
     * Close given web socket session.
     * @param session The web socket session to close.
     */
    private static void close(Session session) {
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
     * Internal usage only. Awkward workaround for it being unavailable via @Inject in endpoint in Tomcat+Weld/OWB.
     */
    static SocketSessionManager getInstance() {
        if (instance == null) {
            instance = getReference(SocketSessionManager.class);
        }

        return instance;
    }

    // Helpers --------------------------------------------------------------------------------------------------------

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