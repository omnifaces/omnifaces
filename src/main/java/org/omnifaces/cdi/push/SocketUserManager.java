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
import static org.omnifaces.cdi.push.SocketChannelManager.ESTIMATED_CHANNELS_PER_APPLICATION;
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
 * This web socket user manager holds all web socket users registered by <code>&lt;o:socket&gt;</code>.
 *
 * @author Bauke Scholtz
 * @see Socket
 * @since 2.3
 */
@ApplicationScoped
public class SocketUserManager {

	// Constants ------------------------------------------------------------------------------------------------------

	/** An average developer will unlikely declare multiple user-targeted channels in same session. */
	private static final int ESTIMATED_USER_CHANNELS_PER_SESSION = 1;

	/** An average user will unlikely login in multiple sessions. */
	private static final int ESTIMATED_SESSIONS_PER_USER = 1;

	// Properties -----------------------------------------------------------------------------------------------------

	private final ConcurrentMap<String, ConcurrentMap<String, Set<String>>> userChannels = new ConcurrentHashMap<>();
	private final ConcurrentMap<Serializable, Set<String>> applicationUsers = new ConcurrentHashMap<>(); // An user can have more than one session (multiple browsers, account sharing).

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Register application user based on given user and application user ID.
	 * @param user The user.
	 * @param userId The application user ID.
	 */
	protected void register(Serializable user, String userId) {
		synchronized (applicationUsers) {
			if (!applicationUsers.containsKey(user)) {
				applicationUsers.putIfAbsent(user, synchronizedSet(new HashSet<String>(ESTIMATED_SESSIONS_PER_USER)));
			}

			applicationUsers.get(user).add(userId);
		}
	}

	/**
	 * Add user channel ID associated with given application user ID and channel name.
	 * @param userId The application user ID.
	 * @param channel The channel name.
	 * @param channelId The channel identifier.
	 */
	protected void addChannelId(String userId, String channel, String channelId) {
		if (!userChannels.containsKey(userId)) {
			userChannels.putIfAbsent(userId, new ConcurrentHashMap<String, Set<String>>(ESTIMATED_CHANNELS_PER_APPLICATION)); // size=3 as an average developer will unlikely declare more push channels in same application.
		}

		ConcurrentMap<String, Set<String>> channelIds = userChannels.get(userId);

		if (!channelIds.containsKey(channel)) {
			channelIds.putIfAbsent(channel, synchronizedSet(new HashSet<String>(ESTIMATED_USER_CHANNELS_PER_SESSION)));
		}

		channelIds.get(channel).add(channelId);
	}

	/**
	 * Resolve the user associated with given channel name and ID.
	 * @param channel The channel name.
	 * @param channelId The channel identifier.
	 * @return The user associated with given channel name and ID.
	 */
	protected Serializable getUser(String channel, String channelId) {
		for (Entry<Serializable, Set<String>> applicationUser : applicationUsers.entrySet()) {
			for (String userId : applicationUser.getValue()) { // "Normally" this contains only 1 entry, so it isn't that inefficient as it looks like.
				if (getApplicationUserChannelIds(userId, channel).contains(channelId)) {
					return applicationUser.getKey();
				}
			}
		}

		return null;
	}

	/**
	 * Resolve the user-specific channel IDs associated with given user and channel name.
	 * @param user The user.
	 * @param channel The channel name.
	 * @return The user-specific channel IDs associated with given user and channel name.
	 */
	protected Set<String> getChannelIds(Serializable user, String channel) {
		Set<String> channelIds = new HashSet<>(ESTIMATED_CHANNELS_PER_APPLICATION);
		Set<String> userIds = applicationUsers.get(user);

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
	protected void deregister(Serializable user, String userId) {
		userChannels.remove(userId);

		synchronized (applicationUsers) {
			Set<String> userIds = applicationUsers.get(user);
			userIds.remove(userId);

			if (userIds.isEmpty()) {
				applicationUsers.remove(user);
			}
		}
	}

	// Internal (static because package private methods in CDI beans are subject to memory leaks) ---------------------

	/**
	 * For internal usage only. This makes it possible to save and restore user specific channels during server
	 * restart/failover in {@link SocketChannelManager}.
	 */
	static ConcurrentMap<String, ConcurrentMap<String, Set<String>>> getUserChannels() {
		return getInstance(SocketUserManager.class).userChannels;
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	private Set<String> getApplicationUserChannelIds(String userId, String channel) {
		Map<String, Set<String>> channels = userChannels.get(userId);

		if (channels != null) {
			Set<String> channelIds = channels.get(channel);

			if (channelIds != null) {
				return channelIds;
			}
		}

		return emptySet();
	}

}