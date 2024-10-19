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

import static java.util.Collections.emptySet;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;

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

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Set<String>>> userChannels = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Serializable, Set<String>> applicationUsers = new ConcurrentHashMap<>(); // An user can have more than one session (multiple browsers, account sharing).

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * Register application user based on given user and session based user ID.
     * @param user The user.
     * @param userId The session based user ID.
     */
    protected void register(Serializable user, String userId) {
        applicationUsers.computeIfAbsent(user, $ -> newKeySet(ESTIMATED_CHANNELS_IDS_PER_USER)).add(userId);
    }

    /**
     * Add user channel ID associated with given session based user ID and channel name.
     * @param userId The session based user ID.
     * @param channel The channel name.
     * @param channelId The channel identifier.
     */
    protected void addChannelId(String userId, String channel, String channelId) {
        userChannels
                .computeIfAbsent(userId, $ -> new ConcurrentHashMap<>(ESTIMATED_USER_CHANNELS_PER_APPLICATION))
                .computeIfAbsent(channel, $ -> newKeySet(ESTIMATED_USER_CHANNELS_PER_SESSION))
                .add(channelId);
    }

    /**
     * Resolve the user associated with given channel name and ID.
     * @param channel The channel name.
     * @param channelId The channel identifier.
     * @return The user associated with given channel name and ID.
     */
    protected Serializable getUser(String channel, String channelId) {
        for (var applicationUser : applicationUsers.entrySet()) {
            for (var userId : applicationUser.getValue()) { // "Normally" this contains only 1 entry, so it isn't that inefficient as it looks like.
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
        var channelIds = new HashSet<String>(ESTIMATED_CHANNELS_IDS_PER_USER);
        var userIds = applicationUsers.get(user);

        if (userIds != null) {
            for (var userId : userIds) {
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
        applicationUsers.computeIfPresent(user, ($, userIds) -> {
            userIds.remove(userId);
            return userIds.isEmpty() ? null : userIds;
        });
    }

    // Internal -------------------------------------------------------------------------------------------------------

    /**
     * For internal usage only. This makes it possible to save and restore user specific channels during server
     * restart/failover in {@link SocketChannelManager}.
     * This should actually be package private, but package private methods in CDI beans are subject to memory leaks.
     * @return User specific channels.
     */
    protected ConcurrentHashMap<String, ConcurrentHashMap<String, Set<String>>> getUserChannels() {
        return userChannels;
    }

    // Helpers --------------------------------------------------------------------------------------------------------

    private Set<String> getApplicationUserChannelIds(String userId, String channel) {
        var channels = userChannels.get(userId);
        return channels != null ? channels.getOrDefault(channel, emptySet()) : emptySet();
    }
}