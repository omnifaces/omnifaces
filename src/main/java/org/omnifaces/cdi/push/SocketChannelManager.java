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

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static org.omnifaces.util.Beans.fireEvent;
import static org.omnifaces.util.Beans.getReference;

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

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;

import org.omnifaces.cdi.push.SocketEvent.Switched;
import org.omnifaces.util.Beans;

/**
 * <p>
 * This web socket channel manager holds all application and session scoped web socket channel identifiers registered by
 * <code>&lt;o:socket&gt;</code>.
 *
 * @author Bauke Scholtz
 * @see Socket
 * @since 2.3
 */
@SessionScoped
public class SocketChannelManager implements Serializable {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;

	private static final String ERROR_INVALID_SCOPE =
		"o:socket 'scope' attribute '%s' does not represent a valid scope. It may not be an EL expression and allowed"
			+ " values are 'application', 'session' and 'view', case insensitive. The default is 'application'. When"
			+ " 'user' attribute is specified, then scope defaults to 'session' and may not be 'application'.";
	private static final String ERROR_DUPLICATE_CHANNEL =
		"o:socket channel '%s' is already registered on a different scope. Choose an unique channel name for a"
			+ " different channel (or shutdown all browsers and restart the server if you were just testing).";

	/** A good developer will unlikely declare multiple application scoped push channels in same application (a global JS listener is more efficient). */
	private static final int ESTIMATED_CHANNELS_PER_APPLICATION = 1;

	/** A good developer will unlikely declare multiple session scoped push channels in same session (a global JS listener is more efficient). */
	private static final int ESTIMATED_CHANNELS_PER_SESSION = 1;

	/** A good developer will unlikely declare multiple view scoped channels in same view (a global JS listener is more efficient). */
	private static final int ESTIMATED_CHANNELS_PER_VIEW = 1;

	/** A good developer will unlikely allow the session to have more than one user (bad security practice, but technically not impossible). */
	private static final int ESTIMATED_USERS_PER_SESSION = 1;

	/** A good developer will unlikely declare more than three push channels in same application (one for each scope with each a global JS listener). */
	static final int ESTIMATED_TOTAL_CHANNELS = ESTIMATED_CHANNELS_PER_APPLICATION + ESTIMATED_CHANNELS_PER_SESSION + ESTIMATED_CHANNELS_PER_VIEW;

	static final Map<String, String> EMPTY_SCOPE = emptyMap();

	private enum Scope {
		APPLICATION, SESSION, VIEW;

		static Scope of(String value, Serializable user) {
			if (value == null) {
				return (user == null) ? APPLICATION : SESSION;
			}

			for (Scope scope : values()) {
				if (scope.name().equalsIgnoreCase(value) && (user == null || scope != APPLICATION)) {
					return scope;
				}
			}

			throw new IllegalArgumentException(format(ERROR_INVALID_SCOPE, value));
		}
	}

	// Properties -----------------------------------------------------------------------------------------------------

	private static final ConcurrentHashMap<String, String> APPLICATION_SCOPE = new ConcurrentHashMap<>(ESTIMATED_CHANNELS_PER_APPLICATION);
	private final ConcurrentHashMap<String, String> sessionScopedChannels = new ConcurrentHashMap<>(ESTIMATED_CHANNELS_PER_SESSION);
	private final ConcurrentHashMap<Serializable, String> sessionUsers = new ConcurrentHashMap<>(ESTIMATED_USERS_PER_SESSION);

	@Inject
	private SocketSessionManager socketSessions;

	@Inject
	private SocketUserManager socketUsers;

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Register given channel on given scope and returns the web socket channel identifier.
	 * @param channel The web socket channel.
	 * @param scope The web socket scope. Supported values are <code>application</code>, <code>session</code> and
	 * <code>view</code>, case insensitive. If <code>null</code>, the default is <code>application</code>.
	 * @param user The user object representing the owner of the given channel. If not <code>null</code>, then scope
	 * may not be <code>application</code>.
	 * @return The web socket channel identifier. This can be used as web socket URI.
	 * @throws IllegalArgumentException When the scope is invalid or when channel already exists on a different scope.
	 */
	@SuppressWarnings("unchecked")
	protected String register(String channel, String scope, Serializable user) {
		switch (Scope.of(scope, user)) {
			case APPLICATION: return register(null, channel, APPLICATION_SCOPE, sessionScopedChannels, getViewScopedChannels(false));
			case SESSION: return register(user, channel, sessionScopedChannels, APPLICATION_SCOPE, getViewScopedChannels(false));
			case VIEW: return register(user, channel, getViewScopedChannels(true), APPLICATION_SCOPE, sessionScopedChannels);
			default: throw new UnsupportedOperationException();
		}
	}

	@SuppressWarnings("unchecked")
	private String register(Serializable user, String channel, Map<String, String> targetScope, Map<String, String>... otherScopes) {
		if (!targetScope.containsKey(channel)) {
			for (Map<String, String> otherScope : otherScopes) {
				if (otherScope.containsKey(channel)) {
					throw new IllegalArgumentException(format(ERROR_DUPLICATE_CHANNEL, channel));
				}
			}

			((ConcurrentHashMap<String, String>) targetScope).putIfAbsent(channel, channel + "?" + UUID.randomUUID().toString());
		}

		String channelId = targetScope.get(channel);

		if (user != null) {
			if (!sessionUsers.containsKey(user) && sessionUsers.putIfAbsent(user, UUID.randomUUID().toString()) == null) {
				socketUsers.register(user, sessionUsers.get(user));
			}

			socketUsers.addChannelId(sessionUsers.get(user), channel, channelId);
		}

		socketSessions.register(channelId);
		return channelId;
	}

	/**
	 * Switch the user on the given channel on the given scope from the given old user to the given new user.
	 * @param channel The web socket channel.
	 * @param scope The web socket scope. Supported values are <code>application</code>, <code>session</code> and
	 * <code>view</code>, case insensitive. If <code>null</code>, the default is <code>application</code>.
	 * @param oldUser The user object representing the old owner of the given channel. If not <code>null</code>, then scope
	 * may not be <code>application</code>.
	 * @param newUser The user object representing the new owner of the given channel. If not <code>null</code>, then scope
	 * may not be <code>application</code>.
	 */
	protected void switchUser(String channel, String scope, Serializable oldUser, Serializable newUser) {
		if (oldUser != null) {
			String userId = sessionUsers.remove(oldUser);

			if (userId != null) {
				socketUsers.deregister(oldUser, userId);
			}
		}

		register(channel, scope, newUser);
		fireEvent(new SocketEvent(channel, newUser, oldUser, null), Switched.LITERAL);
	}

	/**
	 * When current session scope is about to be destroyed, deregister all session scope channels and explicitly close
	 * any open web sockets associated with it to avoid stale websockets. If any, also deregister session users.
	 */
	@PreDestroy
	protected void deregisterSessionScope() {
		for (Entry<Serializable, String> sessionUser : sessionUsers.entrySet()) {
			socketUsers.deregister(sessionUser.getKey(), sessionUser.getValue());
		}

		socketSessions.deregister(sessionScopedChannels.values());
	}

	// Nested classes -------------------------------------------------------------------------------------------------

	/**
	 * This helps the web socket channel manager to hold view scoped web socket channel identifiers registered by
	 * <code>&lt;o:socket&gt;</code>.
	 * @author Bauke Scholtz
	 * @see SocketChannelManager
	 * @since 2.3
	 */
	@ViewScoped
	protected static class ViewScope implements Serializable {

		private static final long serialVersionUID = 1L;
		private ConcurrentHashMap<String, String> channels = new ConcurrentHashMap<>(ESTIMATED_CHANNELS_PER_VIEW);

		/**
		 * Returns the view scoped channels.
		 * @return The view scoped channels.
		 */
		protected Map<String, String> getChannels() {
			return channels;
		}

		/**
		 * When current view scope is about to be destroyed, deregister all view scoped channels and explicitly close
		 * any open web sockets associated with it to avoid stale websockets.
		 */
		@PreDestroy
		protected void deregisterViewScope() {
			SocketSessionManager.getInstance().deregister(channels.values());
		}

	}

	// Internal -------------------------------------------------------------------------------------------------------

	/**
	 * For internal usage only. This makes it possible to reference session scope channel IDs during injection time of
	 * {@link SocketPushContext} (the CDI session scope is not necessarily active during push send time).
	 * This should actually be package private, but package private methods in CDI beans are subject to memory leaks.
	 * @return Session scope channel IDs.
	 */
	protected Map<String, String> getSessionScopedChannels() {
		return sessionScopedChannels;
	}

	/**
	 * For internal usage only. This makes it possible to reference view scope channel IDs during injection time of
	 * {@link SocketPushContext} (the JSF view scope is not necessarily active during push send time).
	 * This should actually be package private, but package private methods in CDI beans are subject to memory leaks.
	 * @param create Whether or not to auto-create the entry in JSF view scope.
	 * @return View scope channel IDs.
	 */
	protected Map<String, String> getViewScopedChannels(boolean create) {
		ViewScope bean = Beans.getInstance(ViewScope.class, create);
		return (bean == null) ? EMPTY_SCOPE : bean.getChannels();
	}

	/**
	 * Internal usage only. Awkward workaround for it being unavailable via @Inject in JSF components and listeners.
	 */
	static SocketChannelManager getInstance() {
		return getReference(SocketChannelManager.class);
	}

	/**
	 * For internal usage only. This makes it possible to resolve the session and view scoped channel ID during push
	 * send time in {@link SocketPushContext}.
	 */
	static String getChannelId(String channel, Map<String, String> sessionScope, Map<String, String> viewScope) {
		String channelId = viewScope.get(channel);

		if (channelId == null) {
			channelId = sessionScope.get(channel);

			if (channelId == null) {
				channelId = APPLICATION_SCOPE.get(channel);
			}
		}

		return channelId;
	}

	// Serialization --------------------------------------------------------------------------------------------------

	private void writeObject(ObjectOutputStream output) throws IOException {
		output.defaultWriteObject();

		// All of below is just in case server restarts with session persistence or failovers/synchronizes to another server.
		output.writeObject(APPLICATION_SCOPE);
		HashMap<String, ConcurrentHashMap<String, Set<String>>> sessionUserChannels = new HashMap<>(sessionUsers.size());

		for (String userId : sessionUsers.values()) {
			sessionUserChannels.put(userId, socketUsers.getUserChannels().get(userId));
		}

		output.writeObject(sessionUserChannels);
	}

	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
		input.defaultReadObject();

		// Below is just in case server restarts with session persistence or failovers/synchronizes from another server.
		APPLICATION_SCOPE.putAll((Map<String, String>) input.readObject());
		Map<String, ConcurrentHashMap<String, Set<String>>> sessionUserChannels = (Map<String, ConcurrentHashMap<String, Set<String>>>) input.readObject();

		for (Entry<Serializable, String> sessionUser : sessionUsers.entrySet()) {
			String userId = sessionUser.getValue();
			socketUsers.register(sessionUser.getKey(), userId);
			socketUsers.getUserChannels().put(userId, sessionUserChannels.get(userId));
		}

		// Below awkwardness is because SocketChannelManager can't be injected in SocketSessionManager (CDI session scope
		// is not necessarily active during WS session). So it can't just ask us for channel IDs and we have to tell it.
		// And, for application scope IDs we make sure they're re-registered after server restart/failover.
		socketSessions.register(sessionScopedChannels.values());
		socketSessions.register(APPLICATION_SCOPE.values());
	}

}