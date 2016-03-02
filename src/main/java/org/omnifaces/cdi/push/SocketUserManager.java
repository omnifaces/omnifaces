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
import static org.omnifaces.cdi.push.SocketChannelManager.ONE_ENTRY;
import static org.omnifaces.cdi.push.SocketChannelManager.THREE_ENTRIES;
import static org.omnifaces.util.Beans.getInstance;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.context.ApplicationScoped;

/**
 * <p>
 * The web socket user manager. It holds all web socket users registered by <code>&lt;o:socket&gt;</code>.
 *
 * @author Bauke Scholtz
 * @see Socket
 * @since 2.3
 */
@ApplicationScoped
public class SocketUserManager {

	// Properties -----------------------------------------------------------------------------------------------------

	private final ConcurrentMap<String, ConcurrentMap<String, Set<String>>> userChannelIds = new ConcurrentHashMap<>();
	private final ConcurrentMap<Serializable, Set<String>> applicationUserIds = new ConcurrentHashMap<>(); // An user can have more than one session (multiple browsers, account sharing).

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Register given user channel ID on given application user ID and channel name.
	 * @param userId The application user ID.
	 * @param channel The channel name.
	 * @param channelId The channel identifier.
	 */
	protected void registerUserChannelId(String userId, String channel, String channelId) {
		if (!userChannelIds.containsKey(userId)) {
			userChannelIds.putIfAbsent(userId, new ConcurrentHashMap<String, Set<String>>(THREE_ENTRIES)); // size=3 as an average developer will unlikely declare more push channels in same application.
		}

		ConcurrentMap<String, Set<String>> channelIds = userChannelIds.get(userId);

		if (!channelIds.containsKey(channel)) {
			channelIds.putIfAbsent(channel, synchronizedSet(new HashSet<String>(ONE_ENTRY))); // size=1 as an average developer will unlikely declare multiple user-targeted channels in same session.
		}

		channelIds.get(channel).add(channelId);
	}

	/**
	 * Register application user based on given user and application user ID.
	 * @param user The user.
	 * @param userId The application user ID.
	 */
	protected void registerUser(Serializable user, String userId) {
		synchronized (applicationUserIds) {
			if (!applicationUserIds.containsKey(user)) {
				applicationUserIds.putIfAbsent(user, synchronizedSet(new HashSet<String>(ONE_ENTRY))); // size=1 as an average user will unlikely login in multiple sessions.
			}

			applicationUserIds.get(user).add(userId);
		}
	}

	/**
	 * Resolve the user associated with given channel name and ID.
	 * @param channel The channel name.
	 * @param channelId The channel identifier.
	 */
	protected Serializable getUser(String channel, String channelId) {
		for (Entry<Serializable, Set<String>> applicationUserId : applicationUserIds.entrySet()) {
			for (String userId : applicationUserId.getValue()) { // "Normally" this contains only 1 entry, so it isn't that inefficient as it looks like.
				if (getApplicationUserChannelIds(userId, channel).contains(channelId)) {
					return applicationUserId.getKey();
				}
			}
		}

		return null;
	}

	/**
	 * Resolve the user-specific channel IDs based on given user and channel name.
	 * @param user The user.
	 * @param channel The channel name.
	 */
	protected Set<String> getUserChannelIds(Serializable user, String channel) {
		Set<String> channelIds = new HashSet<>(THREE_ENTRIES);
		Set<String> userIds = applicationUserIds.get(user);

		if (userIds != null) {
			for (String userId : userIds) {
				channelIds.addAll(getApplicationUserChannelIds(userId, channel));
			}
		}

		return channelIds;
	}

	/**
	 * Deregister application user associated with given user and application user ID.
	 * @param user The user.
	 * @param userId The application user ID.
	 */
	protected void deregisterUser(Serializable user, String userId) {
		synchronized (applicationUserIds) {
			Set<String> userIds = applicationUserIds.get(user);
			userIds.remove(userId);

			if (userIds.isEmpty()) {
				applicationUserIds.remove(user);
			}
		}
	}

	/**
	 * Deregister user channel IDs associated with given application user ID.
	 * @param userId The application user ID.
	 */
	protected void deregisterUserChannelIds(String userId) {
		userChannelIds.remove(userId);
	}

	// Internal (static because package private methods in CDI beans are subject to memory leaks) ---------------------

	/**
	 * For internal usage only. This makes it possible to save and restore user specific channel IDs during server
	 * restart/failover in {@link SocketChannelManager}.
	 */
	static ConcurrentMap<String, ConcurrentMap<String, Set<String>>> getUserChannelIds() {
		return getInstance(SocketUserManager.class).userChannelIds;
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	private Set<String> getApplicationUserChannelIds(String userId, String channel) {
		Map<String, Set<String>> userChannels = userChannelIds.get(userId);

		if (userChannels != null) {
			Set<String> channelIds = userChannels.get(channel);

			if (channelIds != null) {
				return channelIds;
			}
		}

		return emptySet();
	}

}