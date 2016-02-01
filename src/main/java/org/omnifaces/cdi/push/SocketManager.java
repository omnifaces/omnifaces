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

import static java.util.Collections.synchronizedSet;
import static org.omnifaces.cdi.push.SocketEndpoint.PARAM_CHANNEL;
import static org.omnifaces.util.Beans.getReference;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.Session;

import org.omnifaces.util.Json;

/**
 * <p>
 * The web socket manager. It holds all web socket sessions by their channel/scope identifier.
 *
 * @author Bauke Scholtz
 * @see SocketEndpoint
 * @since 2.3
 */
@ApplicationScoped
public class SocketManager {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final ConcurrentMap<String, Set<Session>> SESSIONS = new ConcurrentHashMap<>();
	private static SocketManager instance;

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the CDI managed instance of this class.
	 * @return The CDI managed instance of this class.
	 */
	public static SocketManager getInstance() {
		if (instance == null) {
			instance = getReference(SocketManager.class); // Awkward workaround for it being unavailable via @Inject in endpoint in Tomcat+Weld/OWB.
		}

		return instance;
	}

	/**
	 * On open, add given web socket session to the mapping associated with its channel identifier.
	 * @param channel The web socket channel identifier.
	 * @param session The opened web socket session.
	 */
	public void add(String channel, Session session) {
		session.getUserProperties().put(PARAM_CHANNEL, channel);

		if (!SESSIONS.containsKey(channel)) {
			SESSIONS.putIfAbsent(channel, synchronizedSet(new HashSet<Session>()));
		}

		SESSIONS.get(channel).add(session);
	}

	/**
	 * Encode the given message object as JSON and send it to all open web socket sessions associated with given web
	 * socket channel identifier.
	 * @param channel The web socket channel identifier.
	 * @param message The push message object.
	 */
	public void send(String channel, Object message) {
		Set<Session> sessions = SESSIONS.get(channel);

		if (sessions != null) {
			String json = Json.encode(message);

			synchronized(sessions) {
				for (Session session : sessions) {
					if (session.isOpen()) {
						session.getAsyncRemote().sendText(json);
					}
				}
			}
		}
	}

	/**
	 * On close, remove given web socket session from the mapping.
	 * @param session The closed web socket session.
	 */
	public void remove(Session session) {
		String channel = (String) session.getUserProperties().get(PARAM_CHANNEL);

		if (channel != null) {
			Set<Session> sessions = SESSIONS.get(channel);

			if (sessions != null) {
				sessions.remove(session);
			}
		}
	}

}