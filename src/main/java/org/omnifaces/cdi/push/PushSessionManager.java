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
import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * <p>
 * Base class for push session managers that hold transport-specific sessions by channel identifier. The subclasses must be {@link ApplicationScoped}.
 *
 * @param <S> The transport session type.
 * @author Bauke Scholtz
 * @see PushChannelManager
 * @since 5.2
 */
abstract class PushSessionManager<S> {

    // Properties -----------------------------------------------------------------------------------------------------

    private final ConcurrentHashMap<String, Collection<S>> sessions = new ConcurrentHashMap<>();

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * Register given channel identifier.
     *
     * @param channelId The channel identifier to register.
     */
    protected void register(String channelId) {
        sessions.computeIfAbsent(channelId, $ -> new ConcurrentLinkedQueue<>());
    }

    /**
     * Register given channel identifiers.
     *
     * @param channelIds The channel identifiers to register.
     */
    protected void register(Iterable<String> channelIds) {
        channelIds.forEach(this::register);
    }

    /**
     * Send the given message to all open sessions associated with given channel identifier.
     *
     * @param channelId The channel identifier.
     * @param message The push message string.
     * @return The results of the send operation. If it returns an empty set, then there was no open session associated with given channel identifier. The
     * returned futures will return <code>null</code> on {@link Future#get()} if the message was successfully delivered and otherwise throw
     * {@link ExecutionException}.
     */
    protected Set<Future<Void>> send(String channelId, String message) {
        var channelSessions = channelId != null ? sessions.get(channelId) : null;

        if (channelSessions != null) {
            return channelSessions.stream()
                .filter(this::isOpen)
                .map(session -> sendToSession(session, message))
                .filter(Objects::nonNull)
                .collect(toUnmodifiableSet());
        }

        return emptySet();
    }

    /**
     * Deregister given channel identifiers and explicitly close all open sessions associated with them.
     *
     * @param channelIds The channel identifiers to deregister.
     */
    protected void deregister(Iterable<String> channelIds) {
        for (var channelId : channelIds) {
            var channelSessions = sessions.remove(channelId);

            if (channelSessions != null) {
                channelSessions.forEach(this::closeSession);
            }
        }
    }

    // Template methods -----------------------------------------------------------------------------------------------

    /**
     * Send the given message to the given session.
     *
     * @param session The transport session.
     * @param message The push message string.
     * @return The result of the send operation, or <code>null</code> if the session is no longer usable.
     */
    protected abstract Future<Void> sendToSession(S session, String message);

    /**
     * Returns whether the given session is still open.
     *
     * @param session The transport session.
     * @return <code>true</code> if the session is open.
     */
    protected boolean isOpen(S session) {
        return true;
    }

    /**
     * Close the given session.
     *
     * @param session The transport session to close.
     */
    protected abstract void closeSession(S session);

    // Internal -------------------------------------------------------------------------------------------------------

    /**
     * Returns whether the given channel identifier is registered.
     *
     * @param channelId The channel identifier.
     * @return <code>true</code> if the channel identifier is registered, otherwise <code>false</code>.
     */
    protected boolean isRegistered(String channelId) {
        return sessions.containsKey(channelId);
    }

    /**
     * Add a session to the collection associated with the given channel identifier.
     *
     * @param channelId The channel identifier.
     * @param session The transport session to add.
     * @return <code>true</code> if the channel identifier is known and the session was added, otherwise <code>false</code>.
     */
    protected boolean addSession(String channelId, S session) {
        var channelSessions = sessions.get(channelId);

        if (channelSessions != null) {
            channelSessions.add(session);
            return true;
        }

        return false;
    }

    /**
     * Remove a session from the collection associated with the given channel identifier.
     *
     * @param channelId The channel identifier.
     * @param session The transport session to remove.
     * @return <code>true</code> if the session was present and removed, otherwise <code>false</code>.
     */
    protected boolean removeSession(String channelId, S session) {
        var channelSessions = sessions.get(channelId);

        if (channelSessions != null) {
            return channelSessions.remove(session);
        }

        return false;
    }

}
