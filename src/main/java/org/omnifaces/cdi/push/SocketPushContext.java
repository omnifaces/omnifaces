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
import static org.omnifaces.cdi.push.SocketChannelManager.getChannelId;
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
import org.omnifaces.cdi.push.PushChannelManager.ScopedChannels;
import org.omnifaces.util.Json;

/**
 * <p>
 * This is a concrete web socket implementation of {@link PushContext} interface which is to be injected by <code>&#64;</code>{@link Push}.
 *
 * @author Bauke Scholtz
 * @see Push
 * @see Socket
 * @since 2.3
 */
public class SocketPushContext implements PushContext {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final long serialVersionUID = 1L;

    // Variables ------------------------------------------------------------------------------------------------------

    private final String channel;
    private volatile ScopedChannels scopedChannels;
    private transient SocketSessionManager socketSessions;
    private transient SocketUserManager socketUsers;

    // Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Creates a web socket push context whereby the mutable maps of session and view scope channel identifiers are referenced as soon as an HTTP session
     * exists, so they are still available when another thread invokes {@link #send(Object)} during which the session and view scope is not necessarily active
     * anymore.
     */
    SocketPushContext(String channel, SocketSessionManager socketSessions, SocketUserManager socketUsers) {
        this.channel = channel;
        scopedChannels = ScopedChannels.resolve(null, SocketChannelManager.class);
        this.socketSessions = socketSessions;
        this.socketUsers = socketUsers;
    }

    // Actions --------------------------------------------------------------------------------------------------------

    @Override
    public Set<Future<Void>> send(Object message) {
        return getSocketSessions().send(resolveChannelId(), Json.encode(message));
    }

    @Override
    public <S extends Serializable> Set<Future<Void>> send(Object message, S user) {
        return send(message, singleton(user)).get(user);
    }

    @Override
    public <S extends Serializable> Map<S, Set<Future<Void>>> send(Object message, Collection<S> users) {
        var resultsByUser = new HashMap<S, Set<Future<Void>>>(users.size(), 1);
        var json = Json.encode(message);

        for (S user : users) {
            var channelIds = getSocketUsers().getChannelIds(user, channel);
            var results = new HashSet<Future<Void>>(channelIds.size(), 1);

            for (var channelId : channelIds) {
                results.addAll(getSocketSessions().send(channelId, json));
            }

            resultsByUser.put(user, results);
        }

        return resultsByUser;
    }

    // Helpers --------------------------------------------------------------------------------------------------------

    /**
     * Resolve the channel identifier against the referenced session and view scope channel identifiers, falling back to the application scope. The scoped
     * channels are re-resolved only when the channel is unknown, so that an application scoped channel never pays for a lookup it does not need. They may be
     * absent after deserialization.
     */
    private String resolveChannelId() {
        var current = scopedChannels;
        var channelId = current != null ? getChannelId(channel, current.sessionScope(), current.viewScope()) : null;

        if (channelId == null) {
            var resolved = ScopedChannels.resolve(current, SocketChannelManager.class);

            if (resolved != current) {
                scopedChannels = resolved;
                channelId = getChannelId(channel, resolved.sessionScope(), resolved.viewScope());
            }
        }

        return channelId;
    }

    // Lazy getters in case this gets serialized ----------------------------------------------------------------------

    private SocketSessionManager getSocketSessions() {
        if (socketSessions == null) {
            socketSessions = getReference(SocketSessionManager.class);
        }

        return socketSessions;
    }

    private SocketUserManager getSocketUsers() {
        if (socketUsers == null) {
            socketUsers = getReference(SocketUserManager.class);
        }

        return socketUsers;
    }

}
