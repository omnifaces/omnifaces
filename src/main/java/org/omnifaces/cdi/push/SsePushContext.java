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

import static java.util.Collections.singleton;
import static org.omnifaces.cdi.push.SseChannelManager.getChannelId;
import static org.omnifaces.util.Beans.getReference;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;
import org.omnifaces.cdi.push.Notification.Message;
import org.omnifaces.cdi.push.PushChannelManager.ScopedChannels;
import org.omnifaces.util.Json;

/**
 * <p>
 * This is a concrete SSE implementation of {@link PushContext} interface which is to be injected by <code>&#64;</code>{@link Push}<code>(type=SSE)</code> or
 * <code>&#64;</code>{@link Push}<code>(type=NOTIFICATION)</code>.
 *
 * @author Bauke Scholtz
 * @see Push
 * @see Sse
 * @since 5.2
 */
public class SsePushContext implements PushContext {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final long serialVersionUID = 1L;

    private static final String ERROR_INVALID_NOTIFICATION_MESSAGE = "The message must be a Notification.Message instance when using @Push(type=NOTIFICATION)."
        + " Use Notification.createNotificationMessage(title, body) to create one.";

    // Variables ------------------------------------------------------------------------------------------------------

    private final String channel;
    private final boolean notification;
    private volatile ScopedChannels scopedChannels;
    private transient SseSessionManager sseSessions;
    private transient SseUserManager sseUsers;

    // Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Creates an SSE push context whereby the mutable maps of session and view scope channel identifiers are referenced as soon as an HTTP session exists, so
     * they are still available when another thread invokes {@link #send(Object)} during which the session and view scope is not necessarily active anymore.
     */
    SsePushContext(String channel, boolean notification, SseSessionManager sseSessions, SseUserManager sseUsers) {
        this.channel = channel;
        this.notification = notification;
        scopedChannels = ScopedChannels.resolve(null, SseChannelManager.class);
        this.sseSessions = sseSessions;
        this.sseUsers = sseUsers;
    }

    // Actions --------------------------------------------------------------------------------------------------------

    @Override
    public Set<Future<Void>> send(Object message) {
        validateMessage(message);
        return getSseSessions().send(resolveChannelId(), Json.encode(message));
    }

    @Override
    public <S extends Serializable> Set<Future<Void>> send(Object message, S user) {
        return send(message, singleton(user)).get(user);
    }

    @Override
    public <S extends Serializable> Map<S, Set<Future<Void>>> send(Object message, Collection<S> users) {
        validateMessage(message);
        var resultsByUser = new HashMap<S, Set<Future<Void>>>(users.size(), 1);
        var json = Json.encode(message);

        for (S user : users) {
            var channelIds = getSseUsers().getChannelIds(user, channel);
            var results = new HashSet<Future<Void>>(channelIds.size(), 1);

            for (var channelId : channelIds) {
                results.addAll(getSseSessions().send(channelId, json));
            }

            resultsByUser.put(user, results);
        }

        return resultsByUser;
    }

    // Helpers --------------------------------------------------------------------------------------------------------

    private void validateMessage(Object message) {
        if (notification && !(message instanceof Message)) {
            throw new IllegalArgumentException(ERROR_INVALID_NOTIFICATION_MESSAGE);
        }
    }

    /**
     * Resolve the channel identifier against the referenced session and view scope channel identifiers, falling back to the application scope. The scoped
     * channels are re-resolved only when the channel is unknown, so that an application scoped channel never pays for a lookup it does not need. They may be
     * absent after deserialization.
     */
    private String resolveChannelId() {
        var current = scopedChannels;
        var channelId = current != null ? getChannelId(channel, current.sessionScope(), current.viewScope()) : null;

        if (channelId == null) {
            var resolved = ScopedChannels.resolve(current, SseChannelManager.class);

            if (resolved != current) {
                scopedChannels = resolved;
                channelId = getChannelId(channel, resolved.sessionScope(), resolved.viewScope());
            }
        }

        return channelId;
    }

    // Lazy getters in case this gets serialized ----------------------------------------------------------------------

    private SseSessionManager getSseSessions() {
        if (sseSessions == null) {
            sseSessions = getReference(SseSessionManager.class);
        }

        return sseSessions;
    }

    private SseUserManager getSseUsers() {
        if (sseUsers == null) {
            sseUsers = getReference(SseUserManager.class);
        }

        return sseUsers;
    }

}
