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
import static org.omnifaces.util.Faces.getViewAttribute;
import static org.omnifaces.util.Faces.getViewRoot;
import static org.omnifaces.util.Faces.setViewAttribute;

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
import javax.faces.component.UIViewRoot;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ComponentSystemEventListener;
import javax.faces.event.PreDestroyViewMapEvent;
import javax.inject.Inject;

import org.omnifaces.cdi.push.event.Closed;
import org.omnifaces.cdi.push.event.Opened;

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

	private static final ConcurrentMap<String, String> applicationScopeIds = new ConcurrentHashMap<>(3); // size=3 as an average developer will unlikely declare more push channels in same application.
	private final ConcurrentMap<String, String> sessionScopeIds = new ConcurrentHashMap<>(3); // size=3 as an average developer will unlikely declare more push channels in same application.

	private static final ConcurrentMap<String, ConcurrentMap<String, Set<String>>> applicationUserChannelIds = new ConcurrentHashMap<>();
	private static final ConcurrentMap<Serializable, Set<String>> applicationUserIds = new ConcurrentHashMap<>(); // An user can have more than one session (multiple browsers, account sharing).
	private final ConcurrentMap<Serializable, String> sessionUserIds = new ConcurrentHashMap<>(1); // A session can have more than one user (bad security practice, but technically not impossible).

	@Inject
	private SocketSessionManager manager;

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
			case APPLICATION: return register(null, channel, applicationScopeIds, sessionScopeIds, getViewScopeIds(false));
			case SESSION: return register(user, channel, sessionScopeIds, applicationScopeIds, getViewScopeIds(false));
			case VIEW: return register(user, channel, getViewScopeIds(true), applicationScopeIds, sessionScopeIds);
			default: throw new UnsupportedOperationException();
		}
	}

	@SafeVarargs
	private final String register(Serializable user, String channel, ConcurrentMap<String, String> targetScope, Map<String, String>... otherScopes) {
		if (!targetScope.containsKey(channel)) {
			for (Map<String, String> otherScope : otherScopes) {
				if (otherScope.containsKey(channel)) {
					return null;
				}
			}

			targetScope.putIfAbsent(channel, channel + "?" + UUID.randomUUID().toString());
		}

		String channelId = targetScope.get(channel);

		if (user != null && !sessionUserIds.containsKey(user)) {
			sessionUserIds.putIfAbsent(user, UUID.randomUUID().toString());
			String userId = sessionUserIds.get(user);
			registerUserChannelId(userId, channel, channelId);
			registerApplicationUser(user, userId);
		}

		manager.register(channelId);
		return channelId;
	}

	private void registerUserChannelId(String userId, String channel, String channelId) {
		if (!applicationUserChannelIds.containsKey(userId)) {
			applicationUserChannelIds.putIfAbsent(userId, new ConcurrentHashMap<String, Set<String>>(3)); // size=3 as an average developer will unlikely declare more push channels in same application.
		}

		ConcurrentMap<String, Set<String>> channelIds = applicationUserChannelIds.get(userId);

		if (!channelIds.containsKey(channel)) {
			channelIds.putIfAbsent(channel, synchronizedSet(new HashSet<String>(1))); // size=1 as an average developer will unlikely declare multiple user-targeted channels in same session.
		}

		channelIds.get(channel).add(channelId);
	}

	private void registerApplicationUser(Serializable user, String userId) {
		synchronized (applicationUserIds) {
			if (!applicationUserIds.containsKey(user)) {
				applicationUserIds.putIfAbsent(user, synchronizedSet(new HashSet<String>(1))); // size=1 as an average user will unlikely login in multiple sessions.
			}

			applicationUserIds.get(user).add(userId);
		}
	}

	private void deregisterApplicationUser(Serializable user, String userId) {
		synchronized (applicationUserIds) {
			Set<String> userIds = applicationUserIds.get(user);
			userIds.remove(userId);

			if (userIds.isEmpty()) {
				applicationUserIds.remove(user);
			}
		}
	}

	private void deregisterUserChannelIds(String userId) {
		applicationUserChannelIds.remove(userId);
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

		manager.deregister(sessionScopeIds.values());
	}

	// Nested classes -------------------------------------------------------------------------------------------------

	/**
	 * When current view scope is about to be destroyed, deregister all view scope channels and explicitly close
	 * any open web sockets associated with it to avoid stale websockets. This component system event listener is
	 * intented to be registered on the {@link UIViewRoot}.
	 *
	 * @author Bauke Scholtz
	 * @see SocketChannelManager
	 * @since 2.3
	 */
	static class DeregisterViewScopeChannels implements ComponentSystemEventListener {

		@Override
		public void processEvent(ComponentSystemEvent event) throws AbortProcessingException {
			SocketSessionManager.getInstance().deregister(getViewScopeIds(false).values());
		}

	}

	// Internal -------------------------------------------------------------------------------------------------------

	/**
	 * For internal usage only. This makes it possible to remember session scope channel IDs during injection time of
	 * {@link SocketPushContext} (the CDI session scope is not necessarily active during push send time).
	 */
	ConcurrentMap<String, String> getSessionScopeIds() {
		return sessionScopeIds;
	}

	/**
	 * For internal usage only. This makes it possible to remember view scope channel IDs during injection time of
	 * {@link SocketPushContext} (the CDI view scope is not necessarily active during push send time).
	 */
	static ConcurrentMap<String, String> getViewScopeIds(boolean create) {
		ConcurrentMap<String, String> viewScopeIds = getViewAttribute(SocketChannelManager.class.getName());

		if (viewScopeIds == null) {
			viewScopeIds = new ConcurrentHashMap<>(1); // size=1 as an average developer will unlikely declare multiple view scoped channels in same view.

			if (create) {
				setViewAttribute(SocketChannelManager.class.getName(), viewScopeIds);
				getViewRoot().subscribeToEvent(PreDestroyViewMapEvent.class, new DeregisterViewScopeChannels());
			}
		}

		return viewScopeIds;
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
				channelId = applicationScopeIds.get(channel);
			}
		}

		return channelId;
	}

	/**
	 * For internal usage only. This makes it possible to resolve the user-specific channel IDs during push send time in
	 * {@link SocketPushContext}.
	 */
	static Set<String> getUserChannelIds(Serializable user, String channel) {
		Set<String> userChannelIds = new HashSet<>(3);
		Set<String> userIds = applicationUserIds.get(user);

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
		for (Entry<Serializable, Set<String>> applicationUserId : applicationUserIds.entrySet()) {
			for (String userId : applicationUserId.getValue()) { // "Normally" this contains only 1 entry, so it isn't that inefficient as it looks like.
				if (getApplicationUserChannelIds(userId, channel).contains(channelId)) {
					return applicationUserId.getKey();
				}
			}
		}

		return null;
	}

	private static Set<String> getApplicationUserChannelIds(String userId, String channel) {
		Map<String, Set<String>> userChannels = applicationUserChannelIds.get(userId);

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
		output.writeObject(applicationScopeIds);
		Map<String, ConcurrentMap<String, Set<String>>> sessionUserChannelIds = new HashMap<>(sessionUserIds.size());

		for (String userId : sessionUserIds.values()) {
			sessionUserChannelIds.put(userId, applicationUserChannelIds.get(userId));
		}

		output.writeObject(sessionUserChannelIds);
	}

	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
		input.defaultReadObject();

		// Below is just in case server restarts with session persistence or failovers/synchronizes from another server.
		applicationScopeIds.putAll((Map<String, String>) input.readObject());
		Map<String, ConcurrentMap<String, Set<String>>> sessionUserChannelIds = (Map<String, ConcurrentMap<String, Set<String>>>) input.readObject();

		for (Entry<Serializable, String> sessionUserId : sessionUserIds.entrySet()) {
			String userId = sessionUserId.getValue();
			registerApplicationUser(sessionUserId.getKey(), userId);
			applicationUserChannelIds.put(userId, sessionUserChannelIds.get(userId));
		}

		// Below awkwardness is because SocketChannelManager can't be injected in SocketSessionManager (CDI session scope
		// is not necessarily active during WS session). So it can't just ask us for channel IDs and we have to tell it.
		// And, for application scope IDs we make sure they're re-registered after server restart/failover.
		manager.register(sessionScopeIds.values());
		manager.register(applicationScopeIds.values());
	}

}