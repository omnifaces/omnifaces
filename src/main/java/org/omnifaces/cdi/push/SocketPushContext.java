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

import static java.util.Collections.synchronizedSet;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.Session;

import org.omnifaces.cdi.PushContext;
import org.omnifaces.util.Json;

/**
 * <p>
 * Concrete implementation of {@link PushContext} which is used by {@link SocketEndpoint}.
 * <p>
 * <strong>Do not inject this! Inject {@link PushContext} interface instead.</strong>
 *
 * @author Bauke Scholtz
 * @see SocketEndpoint
 * @since 2.3
 */
@ApplicationScoped
public class SocketPushContext implements PushContext {

	private static final ConcurrentMap<String, Set<Session>> SESSIONS = new ConcurrentHashMap<>();

	/**
	 * On open, add given web socket session to the mapping associated with given channel.
	 * @param session The opened web socket session.
	 * @param channel The push channel name.
	 */
	protected void add(Session session, String channel) {
		session.getUserProperties().put("channel", channel);

		if (!SESSIONS.containsKey(channel)) {
			SESSIONS.putIfAbsent(channel, synchronizedSet(new HashSet<Session>()));
		}

		SESSIONS.get(channel).add(session);
	}

	/**
	 * Encode the given message object as JSON and send it to all open web socket sessions associated with the given
	 * channel name.
	 */
	@Override
	public void send(String channel, Object message) {
		String json = Json.encode(message);
		Set<Session> sessions = SESSIONS.get(channel);

		if (sessions != null) {
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
	protected void remove(Session session) {
		String channel = (String) session.getUserProperties().get("channel");

		if (channel != null) {
			Set<Session> sessions = SESSIONS.get(channel);

			if (sessions != null) {
				sessions.remove(session);
			}
		}
	}

}