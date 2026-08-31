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
import static org.omnifaces.util.Beans.getInstance;
import static org.omnifaces.util.Beans.isActive;
import static org.omnifaces.util.Faces.getViewRoot;
import static org.omnifaces.util.Faces.hasContext;
import static org.omnifaces.util.Faces.hasSession;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionBindingListener;

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

    /** The HTTP session attribute name under which the session and view scoped channel IDs owned by the session are held. */
    static final String SESSION_SCOPE_CHANNEL_IDS = "org.omnifaces.cdi.push.SESSION_SCOPE_CHANNEL_IDS";

    /** Mirror of {@value #SESSION_SCOPE_CHANNEL_IDS} keyed by HTTP session ID, see {@link SessionScopeChannelIds}. */
    private static final ConcurrentHashMap<String, SessionScopeChannelIds> CHANNEL_IDS_BY_SESSION_ID = new ConcurrentHashMap<>();

    static final Map<String, String> EMPTY_SCOPE = emptyMap();

    enum Scope {

        APPLICATION,
        SESSION,
        VIEW;

        static Scope of(String value, Serializable user, String tagName) {
            if (value == null) {
                return user == null ? APPLICATION : SESSION;
            }

            for (var scope : values()) {
                if (scope.name().equalsIgnoreCase(value) && (user == null || scope != APPLICATION)) {
                    return scope;
                }
            }

            throw new IllegalArgumentException(ERROR_INVALID_SCOPE.formatted(tagName, value));
        }

    }

    // Properties -----------------------------------------------------------------------------------------------------

    private final ConcurrentHashMap<String, String> sessionScopedChannels = new ConcurrentHashMap<>(ESTIMATED_CHANNELS_PER_SESSION, 1);
    private final ConcurrentHashMap<Serializable, String> sessionUsers = new ConcurrentHashMap<>(ESTIMATED_USERS_PER_SESSION, 1);

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * Register given channel on given scope and return the channel identifier.
     *
     * @param tagName The name of the tag which declared the given channel, for use in any error message.
     * @param channel The push channel.
     * @param scope The resolved push scope.
     * @param user The user object representing the owner of the given channel. If not <code>null</code>, then scope may not be <code>application</code>.
     * @return The push channel identifier.
     * @throws IllegalArgumentException When channel already exists on a different scope.
     */
    protected String register(String tagName, String channel, Scope scope, Serializable user) {
        var channelId = switch (scope) {
            case APPLICATION -> register(tagName, null, channel, getApplicationScope(), sessionScopedChannels, getViewScopedChannels(false));
            case SESSION -> register(tagName, user, channel, sessionScopedChannels, getApplicationScope(), getViewScopedChannels(false));
            case VIEW -> register(tagName, user, channel, getViewScopedChannels(true), getApplicationScope(), sessionScopedChannels);
        };

        if (scope != Scope.APPLICATION) {
            registerChannelIdInSession(channelId); // Session and view scoped channels may only be connected to by the owning HTTP session.
        }

        return channelId;
    }

    /**
     * Register given channel on application scope and return the channel identifier, without resolving the {@link SessionScoped} channel manager. Application
     * scoped channels are by design not bound to any HTTP session, so declaring one may not implicitly create one. This may only be used while no HTTP session
     * exists, as the session and view scope are then by definition empty and thus cannot already hold a channel of the same name.
     *
     * @param channel The push channel.
     * @param applicationScope The application scope channel IDs.
     * @param pushSessions The push session manager.
     * @return The push channel identifier.
     */
    protected static String registerApplicationScopedChannel(
        String channel, ConcurrentHashMap<String, String> applicationScope,
        PushSessionManager<?> pushSessions
    )
    {
        var channelId = applicationScope.computeIfAbsent(channel, PushChannelManager::newChannelId);
        pushSessions.register(channelId);
        return channelId;
    }

    private static String newChannelId(String channel) {
        return channel + "?" + UUID.randomUUID();
    }

    private static void registerChannelIdInSession(String channelId) {
        var context = FacesContext.getCurrentInstance();

        if (context == null) {
            return;
        }

        var httpSession = (HttpSession) context.getExternalContext().getSession(true);

        synchronized (httpSession) {
            // The type check also covers a session which was persisted or replicated by an OmniFaces version holding a plain set under this attribute.
            var channelIds = httpSession.getAttribute(SESSION_SCOPE_CHANNEL_IDS) instanceof SessionScopeChannelIds existingChannelIds
                ? existingChannelIds
                : null;

            if (channelIds == null) {
                channelIds = new SessionScopeChannelIds();
                httpSession.setAttribute(SESSION_SCOPE_CHANNEL_IDS, channelIds);
            }

            channelIds.add(channelId);
            channelIds.bind(httpSession.getId());
        }
    }

    /**
     * Returns whether the given session or view scoped channel identifier was registered by the given HTTP session.
     *
     * @param httpSession The HTTP session of the incoming push connection, may be <code>null</code>.
     * @param channelId The channel identifier to check.
     * @return Whether the given channel identifier was registered by the given HTTP session.
     */
    static boolean isChannelIdRegisteredInSession(HttpSession httpSession, String channelId) {
        return httpSession != null
            && httpSession.getAttribute(SESSION_SCOPE_CHANNEL_IDS) instanceof SessionScopeChannelIds channelIds
            && channelIds.contains(channelId);
    }

    /**
     * Returns whether the given session or view scoped channel identifier was registered by the HTTP session identified by any of the given identifiers. This
     * exists for containers which do not expose the HTTP session during the web socket handshake, such as Quarkus, where the caller can only present the raw
     * cookie values. Those values are matched against known session identifiers and never trusted as such, so presenting an arbitrary one does not grant
     * access.
     *
     * @param sessionIds The candidate HTTP session identifiers presented by the incoming push connection.
     * @param channelId The channel identifier to check.
     * @return Whether the given channel identifier was registered by any of the identified HTTP sessions.
     */
    static boolean isChannelIdRegisteredInSession(Collection<String> sessionIds, String channelId) {
        for (var sessionId : sessionIds) {
            var channelIds = CHANNEL_IDS_BY_SESSION_ID.get(sessionId);

            if (channelIds != null && channelIds.contains(channelId)) {
                return true;
            }
        }

        return false;
    }

    @SafeVarargs
    private String register(String tagName, Serializable user, String channel, Map<String, String> targetScope, Map<String, String>... otherScopes) {
        if (!targetScope.containsKey(channel)) {
            for (var otherScope : otherScopes) {
                if (otherScope.containsKey(channel)) {
                    throw new IllegalArgumentException(ERROR_DUPLICATE_CHANNEL.formatted(tagName, channel));
                }
            }

            ((ConcurrentHashMap<String, String>) targetScope).putIfAbsent(channel, newChannelId(channel));
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

    // Nested classes -------------------------------------------------------------------------------------------------

    /**
     * The session and view scoped channel identifiers of one push channel manager, referenced by a push context so that they remain reachable when another
     * thread sends a push message during which the session and view scope are not necessarily active anymore. Both scopes are resolved and published together,
     * so that a push context never observes one of them without the other.
     */
    record ScopedChannels(Map<String, String> sessionScope, Map<String, String> viewScope) implements Serializable {

        private static final long serialVersionUID = 1L;

        private static final ScopedChannels UNRESOLVED = new ScopedChannels(EMPTY_SCOPE, EMPTY_SCOPE);

        /**
         * Returns the session and view scoped channels of the given push channel manager, reusing the given current ones when they are already resolved. This
         * never implicitly creates an HTTP session; as long as the channel manager has not been created, which is by definition the case as long as no HTTP
         * session exists, this returns unresolved channels and the caller may retry on a later invocation.
         *
         * @param current The currently known scoped channels, if any.
         * @param channelManagerType The push channel manager type.
         * @return The session and view scoped channels of the given push channel manager.
         */
        static ScopedChannels resolve(ScopedChannels current, Class<? extends PushChannelManager> channelManagerType) {
            if (current != null && !current.isUnresolved()) {
                return current;
            }

            var channelManager = isSessionScopeResolvable() ? getInstance(channelManagerType, false) : null;

            if (channelManager == null) {
                return UNRESOLVED;
            }

            return new ScopedChannels(
                channelManager.getSessionScopedChannels(),
                isViewScopeResolvable() ? channelManager.getViewScopedChannels(true) : EMPTY_SCOPE
            );
        }

        private boolean isUnresolved() {
            return sessionScope == EMPTY_SCOPE || (viewScope == EMPTY_SCOPE && isViewScopeResolvable());
        }

        /**
         * The session scope may only be resolved when that cannot implicitly create an HTTP session. Within a faces context the HTTP session itself gives the
         * only portable answer; outside of it the CDI session context is consulted, whose lookup does not create an HTTP session as long as it may not create
         * the bean.
         */
        private static boolean isSessionScopeResolvable() {
            return (!hasContext() || hasSession()) && isActive(SessionScoped.class);
        }

        private static boolean isViewScopeResolvable() {
            return hasContext() && getViewRoot() != null;
        }

    }

    /**
     * The session and view scoped channel identifiers owned by one HTTP session. It is held as HTTP session attribute so that it is automatically cleaned up
     * when the session expires, and mirrored in {@link PushChannelManager#CHANNEL_IDS_BY_SESSION_ID} so that it remains reachable in containers which do not
     * expose the HTTP session during the web socket handshake. The mirror is not replicated across nodes; {@link #bind(String)} restores it on the next channel
     * registration, which also covers the container having changed the session identifier in the meanwhile.
     */
    private static final class SessionScopeChannelIds extends CopyOnWriteArraySet<String> implements HttpSessionBindingListener {

        private static final long serialVersionUID = 1L;

        private transient volatile String sessionId; // Volatile because valueUnbound() runs on the session reaper thread.

        private void bind(String currentSessionId) {
            if (!currentSessionId.equals(sessionId)) {
                unbind();
                sessionId = currentSessionId;
                CHANNEL_IDS_BY_SESSION_ID.put(sessionId, this);
            }
        }

        @Override
        public void valueUnbound(HttpSessionBindingEvent event) {
            unbind();
        }

        private void unbind() {
            if (sessionId != null) {
                CHANNEL_IDS_BY_SESSION_ID.remove(sessionId, this);
                sessionId = null;
            }
        }

    }

}
