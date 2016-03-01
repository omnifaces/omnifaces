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
import static java.util.Collections.emptySet;
import static java.util.Collections.synchronizedSet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PreDestroy;
import javax.enterprise.context.SessionScoped;
import javax.faces.view.ViewScoped;

import org.omnifaces.cdi.push.event.Closed;
import org.omnifaces.cdi.push.event.Opened;
import org.omnifaces.util.Beans;

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

	private static final long serialVersionUID = 2L;
	static final Map<String, String> EMPTY_SCOPE = emptyMap();
	private static final int ONE_ENTRY = 1;
	private static final int THREE_ENTRIES = 3;

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

	private static final ConcurrentMap<String, ConcurrentMap<String, Set<String>>> APP_USER_CHANNEL_IDS = new ConcurrentHashMap<>();
	private static final ConcurrentMap<Serializable, Set<String>> APP_USER_IDS = new ConcurrentHashMap<>(); // An user can have more than one session (multiple browsers, account sharing).
	private final ConcurrentMap<Serializable, String> sessionUserIds = new ConcurrentHashMap<>(ONE_ENTRY); // A session can have more than one user (bad security practice, but technically not impossible).

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
	public String register(String channel, String scope, Serializable user) {
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
				registerApplicationUser(user, sessionUserIds.get(user));
			}

			registerUserChannelId(sessionUserIds.get(user), channel, channelId);
		}

		SocketSessionManager.register(channelId);
		return channelId;
	}

	private static void registerUserChannelId(String userId, String channel, String channelId) {
		if (!APP_USER_CHANNEL_IDS.containsKey(userId)) {
			APP_USER_CHANNEL_IDS.putIfAbsent(userId, new ConcurrentHashMap<String, Set<String>>(THREE_ENTRIES)); // size=3 as an average developer will unlikely declare more push channels in same application.
		}

		ConcurrentMap<String, Set<String>> channelIds = APP_USER_CHANNEL_IDS.get(userId);

		if (!channelIds.containsKey(channel)) {
			channelIds.putIfAbsent(channel, synchronizedSet(new HashSet<String>(ONE_ENTRY))); // size=1 as an average developer will unlikely declare multiple user-targeted channels in same session.
		}

		channelIds.get(channel).add(channelId);
	}

	private static void registerApplicationUser(Serializable user, String userId) {
		synchronized (APP_USER_IDS) {
			if (!APP_USER_IDS.containsKey(user)) {
				APP_USER_IDS.putIfAbsent(user, synchronizedSet(new HashSet<String>(ONE_ENTRY))); // size=1 as an average user will unlikely login in multiple sessions.
			}

			APP_USER_IDS.get(user).add(userId);
		}
	}

	private static void deregisterApplicationUser(Serializable user, String userId) {
		synchronized (APP_USER_IDS) {
			Set<String> userIds = APP_USER_IDS.get(user);
			userIds.remove(userId);

			if (userIds.isEmpty()) {
				APP_USER_IDS.remove(user);
			}
		}
	}

	private static void deregisterUserChannelIds(String userId) {
		APP_USER_CHANNEL_IDS.remove(userId);
	}

	/**
	 * When current session scope is about to be destroyed, deregister all session scope channels and explicitly close
	 * any open web sockets associated with it to avoid stale websockets. If any, also deregister user IDs.
	 */
	@PreDestroy
	public void deregisterSessionScopeChannels() {
		for (Entry<Serializable, String> sessionUserId : sessionUserIds.entrySet()) {
			String userId = sessionUserId.getValue();
			deregisterUserChannelIds(userId);
			deregisterApplicationUser(sessionUserId.getKey(), userId);
		}

		SocketSessionManager.deregister(sessionScopeIds.values());
	}

	// Nested classes -------------------------------------------------------------------------------------------------

	/**
	 * The web socket channel manager for view scoped web socket channels.
	 * @author Bauke Scholtz
	 * @see SocketChannelManager
	 * @since 2.3
	 */
	@ViewScoped
	public static class SocketChannelManagerViewScopeIds implements Serializable {

		private static final long serialVersionUID = 1L;
		private ConcurrentMap<String, String> viewScopeIds = new ConcurrentHashMap<>(ONE_ENTRY); // size=1 as an average developer will unlikely declare multiple view scoped channels in same view.

		/**
		 * When current view scope is about to be destroyed, deregister all view scope channels and explicitly close
		 * any open web sockets associated with it to avoid stale websockets.
		 */
		@PreDestroy
		public void deregisterViewScopeChannels() {
			SocketSessionManager.getInstance();
			SocketSessionManager.deregister(viewScopeIds.values());
		}

	}

	// Internal -------------------------------------------------------------------------------------------------------

	/**
	 * For internal usage only. This makes it possible to remember session scope channel IDs during injection time of
	 * {@link SocketPushContext} (the CDI session scope is not necessarily active during push send time).
	 */
	Map<String, String> getSessionScopeIds() {
		return sessionScopeIds;
	}

	/**
	 * For internal usage only. This makes it possible to remember view scope channel IDs during injection time of
	 * {@link SocketPushContext} (the CDI view scope is not necessarily active during push send time).
	 */
	static Map<String, String> getViewScopeIds(boolean create) {
		SocketChannelManagerViewScopeIds bean = Beans.getInstance(SocketChannelManagerViewScopeIds.class, create);
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

	/**
	 * For internal usage only. This makes it possible to resolve the user-specific channel IDs during push send time in
	 * {@link SocketPushContext}.
	 */
	static Set<String> getUserChannelIds(Serializable user, String channel) {
		Set<String> userChannelIds = new HashSet<>(THREE_ENTRIES);
		Set<String> userIds = APP_USER_IDS.get(user);

		if (userIds != null) {
			for (String userId : userIds) {
				userChannelIds.addAll(getApplicationUserChannelIds(userId, channel));
			}
		}

		return userChannelIds;
	}

	/**
	 * For internal usage only. This makes it possible to resolve the user associated with given channel ID during
	 * firing the {@link Opened} and {@link Closed} events in {@link SocketSessionManager}.
	 */
	static Serializable getUser(String channel, String channelId) {
		for (Entry<Serializable, Set<String>> applicationUserId : APP_USER_IDS.entrySet()) {
			for (String userId : applicationUserId.getValue()) { // "Normally" this contains only 1 entry, so it isn't that inefficient as it looks like.
				if (getApplicationUserChannelIds(userId, channel).contains(channelId)) {
					return applicationUserId.getKey();
				}
			}
		}

		return null;
	}

	private static Set<String> getApplicationUserChannelIds(String userId, String channel) {
		Map<String, Set<String>> userChannels = APP_USER_CHANNEL_IDS.get(userId);

		if (userChannels != null) {
			Set<String> channelIds = userChannels.get(channel);

			if (channelIds != null) {
				return channelIds;
			}
		}

		return emptySet();
	}

	// Serialization --------------------------------------------------------------------------------------------------

	private void writeObject(ObjectOutputStream output) throws IOException {
		output.defaultWriteObject();

		// All of below is just in case server restarts with session persistence or failovers/synchronizes to another server.
		output.writeObject(APP_SCOPE_IDS);
		Map<String, ConcurrentMap<String, Set<String>>> sessionUserChannelIds = new HashMap<>(sessionUserIds.size());

		for (String userId : sessionUserIds.values()) {
			sessionUserChannelIds.put(userId, APP_USER_CHANNEL_IDS.get(userId));
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
			registerApplicationUser(sessionUserId.getKey(), userId);
			APP_USER_CHANNEL_IDS.put(userId, sessionUserChannelIds.get(userId));
		}

		// Below awkwardness is because SocketChannelManager can't be injected in SocketSessionManager (CDI session scope
		// is not necessarily active during WS session). So it can't just ask us for channel IDs and we have to tell it.
		// And, for application scope IDs we make sure they're re-registered after server restart/failover.
		// TODO: This fails in OWB (but works fine in Weld). Check CDI spec on this.
		SocketSessionManager.register(sessionScopeIds.values());
		SocketSessionManager.register(APP_SCOPE_IDS.values());
	}

}