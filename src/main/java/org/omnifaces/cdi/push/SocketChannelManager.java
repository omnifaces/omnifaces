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

import static java.util.Collections.emptyMap;
import static org.omnifaces.cdi.push.SocketUserManager.getUserChannelIds;
import static org.omnifaces.util.Beans.getInstance;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PreDestroy;
import javax.enterprise.context.SessionScoped;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;

/**
 * <p>
 * The web socket channel manager. It holds all web socket channels registered by <code>&lt;o:socket&gt;</code>.
 *
 * @author Bauke Scholtz
 * @see Socket
 * @since 2.3
 */
@SessionScoped
public class SocketChannelManager implements Serializable {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;

	static final Map<String, String> EMPTY_SCOPE = emptyMap();
	static final int ONE_ENTRY = 1;
	static final int THREE_ENTRIES = 3;

	private enum Scope {
		APPLICATION, SESSION, VIEW;

		public static Scope of(String value, Serializable user) {
			if (value == null) {
				return (user == null) ? APPLICATION : SESSION;
			}

			for (Scope scope : values()) {
				if (scope.name().equalsIgnoreCase(value) && (user == null || scope != APPLICATION)) {
					return scope;
				}
			}

			throw new IllegalArgumentException();
		}
	}

	// Properties -----------------------------------------------------------------------------------------------------

	private static final ConcurrentMap<String, String> APP_SCOPE_IDS = new ConcurrentHashMap<>(THREE_ENTRIES); // size=3 as an average developer will unlikely declare more push channels in same application.
	private final ConcurrentMap<String, String> sessionScopeIds = new ConcurrentHashMap<>(THREE_ENTRIES); // size=3 as an average developer will unlikely declare more push channels in same application.
	private final ConcurrentMap<Serializable, String> sessionUserIds = new ConcurrentHashMap<>(ONE_ENTRY); // A session can have more than one user (bad security practice, but technically not impossible).

	@Inject
	private SocketSessionManager sessionManager;

	@Inject
	private SocketUserManager userManager;

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Register given channel on given scope and returns the web socket channel identifier if channel does not already
	 * exist on a different scope, else <code>null</code>.
	 * @param channel The web socket channel.
	 * @param scope The web socket scope. Supported values are <code>application</code>, <code>session</code> and
	 * <code>view</code>, case insensitive. If <code>null</code>, the default is <code>application</code>.
	 * @param user The user object representing the owner of the given channel. If not <code>null</code>, then scope
	 * may not be <code>application</code>.
	 * @return The web socket channel identifier if channel does not already exist on a different scope, else
	 * <code>null</code>. This can be used as web socket URI.
	 * @throws IllegalArgumentException When the scope is invalid.
	 */
	protected String register(String channel, String scope, Serializable user) {
		switch (Scope.of(scope, user)) {
			case APPLICATION: return register(null, channel, APP_SCOPE_IDS, sessionScopeIds, getViewScopeIds(false));
			case SESSION: return register(user, channel, sessionScopeIds, APP_SCOPE_IDS, getViewScopeIds(false));
			case VIEW: return register(user, channel, getViewScopeIds(true), APP_SCOPE_IDS, sessionScopeIds);
			default: throw new UnsupportedOperationException();
		}
	}

	@SafeVarargs
	private final String register(Serializable user, String channel, Map<String, String> targetScope, Map<String, String>... otherScopes) {
		if (!targetScope.containsKey(channel)) {
			for (Map<String, String> otherScope : otherScopes) {
				if (otherScope.containsKey(channel)) {
					return null;
				}
			}

			((ConcurrentMap<String, String>) targetScope).putIfAbsent(channel, channel + "?" + UUID.randomUUID().toString());
		}

		String channelId = targetScope.get(channel);

		if (user != null) {
			if (!sessionUserIds.containsKey(user)) {
				sessionUserIds.putIfAbsent(user, UUID.randomUUID().toString());
				userManager.registerUser(user, sessionUserIds.get(user));
			}

			userManager.registerUserChannelId(sessionUserIds.get(user), channel, channelId);
		}

		sessionManager.register(channelId);
		return channelId;
	}

	/**
	 * When current session scope is about to be destroyed, deregister all session scope channels and explicitly close
	 * any open web sockets associated with it to avoid stale websockets. If any, also deregister user IDs.
	 */
	@PreDestroy
	protected void deregisterSessionScopeChannels() {
		for (Entry<Serializable, String> sessionUserId : sessionUserIds.entrySet()) {
			String userId = sessionUserId.getValue();
			userManager.deregisterUserChannelIds(userId);
			userManager.deregisterUser(sessionUserId.getKey(), userId);
		}

		sessionManager.deregister(sessionScopeIds.values());
	}

	// Nested classes -------------------------------------------------------------------------------------------------

	/**
	 * The web socket channel manager for view scoped web socket channels.
	 * @author Bauke Scholtz
	 * @see SocketChannelManager
	 * @since 2.3
	 */
	@ViewScoped
	protected static class SocketChannelManagerViewScopeIds implements Serializable {

		private static final long serialVersionUID = 1L;
		private ConcurrentMap<String, String> viewScopeIds = new ConcurrentHashMap<>(ONE_ENTRY); // size=1 as an average developer will unlikely declare multiple view scoped channels in same view.

		/**
		 * When current view scope is about to be destroyed, deregister all view scope channels and explicitly close
		 * any open web sockets associated with it to avoid stale websockets.
		 */
		@PreDestroy
		public void deregisterViewScopeChannels() {
			SocketSessionManager.getInstance().deregister(viewScopeIds.values());
		}

	}

	// Internal (static because package private methods in CDI beans are subject to memory leaks) ---------------------

	/**
	 * For internal usage only. This makes it possible to remember session scope channel IDs during injection time of
	 * {@link SocketPushContext} (the CDI session scope is not necessarily active during push send time).
	 */
	static Map<String, String> getSessionScopeIds() {
		return getInstance(SocketChannelManager.class).sessionScopeIds;
	}

	/**
	 * For internal usage only. This makes it possible to remember view scope channel IDs during injection time of
	 * {@link SocketPushContext} (the CDI view scope is not necessarily active during push send time).
	 */
	static Map<String, String> getViewScopeIds(boolean create) {
		SocketChannelManagerViewScopeIds bean = getInstance(SocketChannelManagerViewScopeIds.class, create);
		return (bean == null) ? EMPTY_SCOPE : bean.viewScopeIds;
	}

	/**
	 * For internal usage only. This makes it possible to resolve the session and view scope channel ID during push send
	 * time in {@link SocketPushContext}.
	 */
	static String getChannelId(String channel, Map<String, String> sessionScopeIds, Map<String, String> viewScopeIds) {
		String channelId = viewScopeIds.get(channel);

		if (channelId == null) {
			channelId = sessionScopeIds.get(channel);

			if (channelId == null) {
				channelId = APP_SCOPE_IDS.get(channel);
			}
		}

		return channelId;
	}

	// Serialization --------------------------------------------------------------------------------------------------

	private void writeObject(ObjectOutputStream output) throws IOException {
		output.defaultWriteObject();

		// All of below is just in case server restarts with session persistence or failovers/synchronizes to another server.
		output.writeObject(APP_SCOPE_IDS);
		Map<String, ConcurrentMap<String, Set<String>>> sessionUserChannelIds = new HashMap<>(sessionUserIds.size());

		for (String userId : sessionUserIds.values()) {
			sessionUserChannelIds.put(userId, getUserChannelIds().get(userId));
		}

		output.writeObject(sessionUserChannelIds);
	}

	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
		input.defaultReadObject();

		// Below is just in case server restarts with session persistence or failovers/synchronizes from another server.
		APP_SCOPE_IDS.putAll((Map<String, String>) input.readObject());
		Map<String, ConcurrentMap<String, Set<String>>> sessionUserChannelIds = (Map<String, ConcurrentMap<String, Set<String>>>) input.readObject();

		for (Entry<Serializable, String> sessionUserId : sessionUserIds.entrySet()) {
			String userId = sessionUserId.getValue();
			userManager.registerUser(sessionUserId.getKey(), userId);
			getUserChannelIds().put(userId, sessionUserChannelIds.get(userId));
		}

		// Below awkwardness is because SocketChannelManager can't be injected in SocketSessionManager (CDI session scope
		// is not necessarily active during WS session). So it can't just ask us for channel IDs and we have to tell it.
		// And, for application scope IDs we make sure they're re-registered after server restart/failover.
		sessionManager.register(sessionScopeIds.values());
		sessionManager.register(APP_SCOPE_IDS.values());
	}

}