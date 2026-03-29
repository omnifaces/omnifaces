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

import static java.util.Collections.emptyMap;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.SessionScoped;

/**
 * <p>
 * Base class for push channel managers that hold application, session and view scoped channel identifiers. The subclasses must be {@link SessionScoped}.
 *
 * @author Bauke Scholtz
 * @see PushSessionManager
 * @see PushUserManager
 * @since 5.2
 */
abstract class PushChannelManager implements Serializable {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final long serialVersionUID = 1L;

    private static final String ERROR_INVALID_SCOPE = "%s 'scope' attribute '%s' does not represent a valid scope. It may not be an EL expression and allowed"
        + " values are 'application', 'session' and 'view', case insensitive. The default is 'application'. When"
        + " 'user' attribute is specified, then scope defaults to 'session' and may not be 'application'.";
    private static final String ERROR_DUPLICATE_CHANNEL = "%s channel '%s' is already registered on a different scope. Choose an unique channel name for a"
        + " different channel (or shutdown all browsers and restart the server if you were just testing).";

    /** A good developer will unlikely declare multiple application scoped push channels in same application (a global JS listener is more efficient). */
    static final int ESTIMATED_CHANNELS_PER_APPLICATION = 1;

    /** A good developer will unlikely declare multiple session scoped push channels in same session (a global JS listener is more efficient). */
    static final int ESTIMATED_CHANNELS_PER_SESSION = 1;

    /** A good developer will unlikely declare multiple view scoped channels in same view (a global JS listener is more efficient). */
    static final int ESTIMATED_CHANNELS_PER_VIEW = 1;

    /** A good developer will unlikely allow the session to have more than one user (bad security practice, but technically not impossible). */
    private static final int ESTIMATED_USERS_PER_SESSION = 1;

    /** A good developer will unlikely declare more than three push channels in same application (one for each scope with each a global JS listener). */
    static final int ESTIMATED_TOTAL_CHANNELS = ESTIMATED_CHANNELS_PER_APPLICATION + ESTIMATED_CHANNELS_PER_SESSION + ESTIMATED_CHANNELS_PER_VIEW;

    static final Map<String, String> EMPTY_SCOPE = emptyMap();

    enum Scope {

        APPLICATION,
        SESSION,
        VIEW;

        static Scope of(String value, Serializable user, String componentName) {
            if (value == null) {
                return user == null ? APPLICATION : SESSION;
            }

            for (var scope : values()) {
                if (scope.name().equalsIgnoreCase(value) && (user == null || scope != APPLICATION)) {
                    return scope;
                }
            }

            throw new IllegalArgumentException(ERROR_INVALID_SCOPE.formatted(componentName, value));
        }

    }

    // Properties -----------------------------------------------------------------------------------------------------

    private final ConcurrentHashMap<String, String> sessionScopedChannels = new ConcurrentHashMap<>(ESTIMATED_CHANNELS_PER_SESSION, 1);
    private final ConcurrentHashMap<Serializable, String> sessionUsers = new ConcurrentHashMap<>(ESTIMATED_USERS_PER_SESSION, 1);

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * Register given channel on given scope and return the channel identifier.
     * 
     * @param channel The push channel.
     * @param scope The push scope. Supported values are <code>application</code>, <code>session</code> and <code>view</code>, case insensitive. If
     * <code>null</code>, the default is <code>application</code>.
     * @param user The user object representing the owner of the given channel. If not <code>null</code>, then scope may not be <code>application</code>.
     * @return The push channel identifier.
     * @throws IllegalArgumentException When the scope is invalid or when channel already exists on a different scope.
     */
    protected String register(String channel, String scope, Serializable user) {
        return switch (Scope.of(scope, user, getClass().getSimpleName())) {
            case APPLICATION -> register(null, channel, getApplicationScope(), sessionScopedChannels, getViewScopedChannels(false));
            case SESSION -> register(user, channel, sessionScopedChannels, getApplicationScope(), getViewScopedChannels(false));
            case VIEW -> register(user, channel, getViewScopedChannels(true), getApplicationScope(), sessionScopedChannels);
        };
    }

    @SafeVarargs
    private String register(Serializable user, String channel, Map<String, String> targetScope, Map<String, String>... otherScopes) {
        if (!targetScope.containsKey(channel)) {
            for (var otherScope : otherScopes) {
                if (otherScope.containsKey(channel)) {
                    throw new IllegalArgumentException(ERROR_DUPLICATE_CHANNEL.formatted(getClass().getSimpleName(), channel));
                }
            }

            ((ConcurrentHashMap<String, String>) targetScope).putIfAbsent(channel, channel + "?" + UUID.randomUUID());
        }

        var channelId = targetScope.get(channel);

        if (user != null) {
            if (!sessionUsers.containsKey(user) && sessionUsers.putIfAbsent(user, UUID.randomUUID().toString()) == null) {
                getPushUsers().register(user, sessionUsers.get(user));
            }

            getPushUsers().addChannelId(sessionUsers.get(user), channel, channelId);
        }

        getPushSessions().register(channelId);
        return channelId;
    }

    /**
     * When current session scope is about to be destroyed, deregister all session scope channels and explicitly close any open sessions associated with it. If
     * any, also deregister session users.
     */
    protected void deregisterSessionScope() {
        for (var sessionUser : sessionUsers.entrySet()) {
            getPushUsers().deregister(sessionUser.getKey(), sessionUser.getValue());
        }

        getPushSessions().deregister(sessionScopedChannels.values());
    }

    // Template methods -----------------------------------------------------------------------------------------------

    /**
     * Returns the application scoped channels for this push transport.
     * 
     * @return The application scoped channels.
     */
    protected abstract ConcurrentHashMap<String, String> getApplicationScope();

    /**
     * Returns the push session manager for this push transport.
     * 
     * @return The push session manager.
     */
    protected abstract PushSessionManager<?> getPushSessions();

    /**
     * Returns the push user manager for this push transport.
     * 
     * @return The push user manager.
     */
    protected abstract PushUserManager getPushUsers();

    /**
     * Returns the view scoped channels for this push transport.
     * 
     * @param create Whether or not to auto-create the entry in Faces view scope.
     * @return View scope channel IDs.
     * @throws IllegalStateException When the Faces view scope is not available while {@code create} is {@code true}.
     */
    protected abstract Map<String, String> getViewScopedChannels(boolean create);

    // Internal -------------------------------------------------------------------------------------------------------

    /**
     * For internal usage only. This makes it possible to reference session scope channel IDs during injection time of push context implementations (the CDI
     * session scope is not necessarily active during push send time). This should actually be package private, but package private methods in CDI beans are
     * subject to memory leaks.
     * 
     * @return Session scope channel IDs.
     */
    protected Map<String, String> getSessionScopedChannels() {
        return sessionScopedChannels;
    }

    /**
     * For internal usage only. Returns the session user mappings. This should actually be package private, but package private methods in CDI beans are subject
     * to memory leaks.
     * 
     * @return Session user mappings.
     */
    protected ConcurrentHashMap<Serializable, String> getSessionUsers() {
        return sessionUsers;
    }

    /**
     * For internal usage only. This makes it possible to resolve the session and view scoped channel ID during push send time in push context implementations.
     * 
     * @param channel The channel name.
     * @param applicationScope The application scope channel IDs.
     * @param sessionScope The session scope channel IDs.
     * @param viewScope The view scope channel IDs.
     * @return The channel ID, or <code>null</code> if not found.
     */
    protected static String getChannelId(
        String channel, ConcurrentHashMap<String, String> applicationScope, Map<String, String> sessionScope,
        Map<String, String> viewScope
    )
    {
        var channelId = viewScope.get(channel);

        if (channelId == null) {
            channelId = sessionScope.get(channel);

            if (channelId == null) {
                channelId = applicationScope.get(channel);
            }
        }

        return channelId;
    }

    // Serialization --------------------------------------------------------------------------------------------------

    /**
     * Serialize the push channel state for session persistence or failover.
     * 
     * @param output The object output stream.
     * @throws IOException When an I/O error occurs.
     */
    protected void serializeState(ObjectOutputStream output) throws IOException {
        output.writeObject(getApplicationScope());
        var sessionUserChannels = new HashMap<String, ConcurrentHashMap<String, Set<String>>>(sessionUsers.size(), 1);

        for (var userId : sessionUsers.values()) {
            sessionUserChannels.put(userId, getPushUsers().getUserChannels().get(userId));
        }

        output.writeObject(sessionUserChannels);
    }

    /**
     * Deserialize the push channel state after session restore or failover.
     * 
     * @param input The object input stream.
     * @throws IOException When an I/O error occurs.
     * @throws ClassNotFoundException When a serialized class is not found.
     */
    @SuppressWarnings("unchecked")
    protected void deserializeState(ObjectInputStream input) throws IOException, ClassNotFoundException {
        getApplicationScope().putAll((Map<String, String>) input.readObject());
        var sessionUserChannels = (Map<String, ConcurrentHashMap<String, Set<String>>>) input.readObject();

        for (var sessionUser : sessionUsers.entrySet()) {
            var userId = sessionUser.getValue();
            getPushUsers().register(sessionUser.getKey(), userId);
            getPushUsers().getUserChannels().put(userId, sessionUserChannels.get(userId));
        }

        getPushSessions().register(sessionScopedChannels.values());
        getPushSessions().register(getApplicationScope().values());
    }

}
