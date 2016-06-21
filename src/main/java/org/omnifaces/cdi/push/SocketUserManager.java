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

	/** A good developer will unlikely declare multiple user-targeted channels in same application (a global JS listener is more efficient). */
	private static final int ESTIMATED_USER_CHANNELS_PER_APPLICATION = 1;

	/** A good developer will unlikely declare an user-targeted view scoped channel in same session (as this implies possibility of multiple users per session). */
	private static final int ESTIMATED_USER_CHANNELS_PER_SESSION = 1;

	/** An average user will unlikely simultaneously login in more than two sessions (desktop/mobile). */
	private static final int ESTIMATED_SESSIONS_PER_USER = 2;

	/** An average user will unlikely have more than one user-targeted channel in same session. */
	private static final int ESTIMATED_CHANNELS_IDS_PER_USER = ESTIMATED_SESSIONS_PER_USER * ESTIMATED_USER_CHANNELS_PER_APPLICATION * ESTIMATED_USER_CHANNELS_PER_SESSION;

	// Properties -----------------------------------------------------------------------------------------------------

	private final ConcurrentMap<String, ConcurrentMap<String, Set<String>>> userChannels = new ConcurrentHashMap<>();
	private final ConcurrentMap<Serializable, Set<String>> applicationUsers = new ConcurrentHashMap<>(); // An user can have more than one session (multiple browsers, account sharing).

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Register application user based on given user and session based user ID.
	 * @param user The user.
	 * @param userId The session based user ID.
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
	 * Add user channel ID associated with given session based user ID and channel name.
	 * @param userId The session based user ID.
	 * @param channel The channel name.
	 * @param channelId The channel identifier.
	 */
	protected void addChannelId(String userId, String channel, String channelId) {
		if (!userChannels.containsKey(userId)) {
			userChannels.putIfAbsent(userId, new ConcurrentHashMap<String, Set<String>>(ESTIMATED_USER_CHANNELS_PER_APPLICATION));
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
		Set<String> channelIds = new HashSet<>(ESTIMATED_CHANNELS_IDS_PER_USER);
		Set<String> userIds = applicationUsers.get(user);

		if (userIds != null) {
			for (String userId : userIds) {
				channelIds.addAll(getApplicationUserChannelIds(userId, channel));
			}
		}

		return channelIds;
	}

	/**
	 * Deregister application user associated with given user and session based user ID.
	 * @param user The user.
	 * @param userId The session based user ID.
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

	// Internal -------------------------------------------------------------------------------------------------------

	/**
	 * For internal usage only. This makes it possible to save and restore user specific channels during server
	 * restart/failover in {@link SocketChannelManager}.
	 * This should actually be package private, but package private methods in CDI beans are subject to memory leaks.
	 * @return User specific channels.
	 */
	protected ConcurrentMap<String, ConcurrentMap<String, Set<String>>> getUserChannels() {
		return userChannels;
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