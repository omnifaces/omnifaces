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

import static java.util.Collections.emptySet;
import static java.util.Collections.synchronizedSet;
import static javax.websocket.CloseReason.CloseCodes.GOING_AWAY;
import static org.omnifaces.cdi.push.SocketChannelManager.getUser;
import static org.omnifaces.cdi.push.SocketEndpoint.PARAM_CHANNEL;
import static org.omnifaces.util.Beans.fireEvent;
import static org.omnifaces.util.Beans.getReference;
import static org.omnifaces.util.Utils.isEmpty;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.util.AnnotationLiteral;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCode;
import javax.websocket.Session;

import org.omnifaces.cdi.push.event.Closed;
import org.omnifaces.cdi.push.event.Opened;
import org.omnifaces.util.Json;

/**
 * <p>
 * The web socket session manager. It holds all web socket sessions by their channel identifier.
 *
 * @author Bauke Scholtz
 * @see SocketEndpoint
 * @since 2.3
 */
@ApplicationScoped
public class SocketSessionManager {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final ConcurrentMap<String, Set<Session>> channelSessions = new ConcurrentHashMap<>();

	private static final CloseReason REASON_SESSION_EXPIRED = new CloseReason(GOING_AWAY, "Session expired");
	private static final AnnotationLiteral<Opened> SESSION_OPENED = new AnnotationLiteral<Opened>() {
		private static final long serialVersionUID = 1L;
	};
	private static final AnnotationLiteral<Closed> SESSION_CLOSED = new AnnotationLiteral<Closed>() {
		private static final long serialVersionUID = 1L;
	};

	private static SocketSessionManager instance;

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the CDI managed instance of this class.
	 * @return The CDI managed instance of this class.
	 */
	public static SocketSessionManager getInstance() {
		if (instance == null) {
			instance = getReference(SocketSessionManager.class); // Awkward workaround for it being unavailable via @Inject in endpoint in Tomcat+Weld/OWB.
		}

		return instance;
	}

	/**
	 * Register given channel identifier.
	 * @param channelId The channel identifier to register.
	 */
	public void register(String channelId) {
		if (!channelSessions.containsKey(channelId)) {
			channelSessions.putIfAbsent(channelId, synchronizedSet(new HashSet<Session>()));
		}
	}

	/**
	 * Register given channel identifiers.
	 * @param channelIds The channel identifiers to register.
	 */
	public void register(Iterable<String> channelIds) {
		for (String channelId : channelIds) {
			register(channelId);
		}
	}

	/**
	 * On open, add given web socket session to the mapping associated with its channel identifier and returns
	 * <code>true</code> if it's accepted (i.e. the channel identifier is known) and the same session hasn't been added
	 * before, otherwise <code>false</code>.
	 * @param session The opened web socket session.
	 * @return <code>true</code> if given web socket session is accepted and is new, otherwise <code>false</code>.
	 */
	public boolean add(Session session) {
		String channelId = getChannelId(session);
		Set<Session> sessions = channelSessions.get(channelId);

		if (sessions != null && sessions.add(session)) {
			String channel = getChannel(session);
			Serializable user = getUser(channel, channelId);
			session.getUserProperties().put("user", user);
			fireEvent(new SocketEvent(channel, user, null), SESSION_OPENED);
			return true;
		}

		return false;
	}

	/**
	 * Encode the given message object as JSON and send it to all open web socket sessions associated with given web
	 * socket channel identifier.
	 * @param channelId The web socket channel identifier.
	 * @param message The push message object.
	 * @return The results of the send operation. If it returns an empty set, then there was no open session associated
	 * with given channel identifier. The returned futures will return <code>null</code> on {@link Future#get()} if the
	 * message was successfully delivered and otherwise throw {@link ExecutionException}.
	 */
	public Set<Future<Void>> send(String channelId, Object message) {
		Set<Session> sessions = (channelId != null) ? channelSessions.get(channelId) : null;

		if (!isEmpty(sessions)) {
			Set<Future<Void>> results = new HashSet<>(sessions.size());
			String json = Json.encode(message);

			synchronized(sessions) {
				for (Session session : sessions) {
					if (session.isOpen()) {
						results.add(session.getAsyncRemote().sendText(json));
					}
				}
			}

			return results;
		}

		return emptySet();
	}

	/**
	 * On close, remove given web socket session from the mapping.
	 * @param session The closed web socket session.
	 * @param closeCode The close code.
	 */
	public void remove(Session session, CloseCode closeCode) {
		Set<Session> sessions = channelSessions.get(getChannelId(session));

		if (sessions != null && sessions.remove(session)) {
			Serializable user = (Serializable) session.getUserProperties().get("user");
			fireEvent(new SocketEvent(getChannel(session), user, closeCode), SESSION_CLOSED);
		}
	}

	/**
	 * Deregister given channel identifiers and explicitly close all open web socket sessions associated with it.
	 * @param channelIds The channel identifiers to deregister.
	 */
	public void deregister(Iterable<String> channelIds) {
		for (String channelId : channelIds) {
			Set<Session> sessions = channelSessions.remove(channelId);

			if (sessions != null) {
				for (Session session : sessions) {
					if (session.isOpen()) {
						try {
							session.close(REASON_SESSION_EXPIRED);
						}
						catch (IOException ignore) {
							continue;
						}
					}
				}
			}
		}
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	private static String getChannel(Session session) {
		return session.getPathParameters().get(PARAM_CHANNEL);
	}

	private static String getChannelId(Session session) {
		return getChannel(session) + "?" + session.getQueryString();
	}

}