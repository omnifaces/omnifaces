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

import static org.omnifaces.util.Beans.fireEvent;
import static org.omnifaces.util.Beans.getReference;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;

import org.omnifaces.cdi.push.SseEvent.Switched;
import org.omnifaces.util.Beans;

/**
 * <p>
 * This SSE channel manager holds all application and session scoped SSE channel identifiers registered by <code>&lt;o:sse&gt;</code>.
 *
 * @author Bauke Scholtz
 * @see Sse
 * @since 5.2
 */
@SessionScoped
public class SseChannelManager extends PushChannelManager {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final long serialVersionUID = 1L;

    private static final String ERROR_VIEW_SCOPE_UNAVAILABLE = "o:sse view scope is unavailable."
        + " Perhaps you need to explicitly register the SseChannelManager.ViewScope as a CDI managed bean?";

    // Properties -----------------------------------------------------------------------------------------------------

    private static final ConcurrentHashMap<String, String> APPLICATION_SCOPE = new ConcurrentHashMap<>(ESTIMATED_CHANNELS_PER_APPLICATION, 1);

    @Inject
    private SseSessionManager sseSessions;

    @Inject
    private SseUserManager sseUsers;

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * Returns whether the given channel identifier represents an application scoped SSE channel, which is by design not bound to any HTTP session.
     *
     * @param channelId The channel identifier to check.
     * @return Whether the given channel identifier represents an application scoped SSE channel.
     */
    static boolean isApplicationScopedChannelId(String channelId) {
        return APPLICATION_SCOPE.containsValue(channelId);
    }

    /**
     * Switch the user on the given channel on the given scope from the given old user to the given new user.
     *
     * @param channel The SSE channel.
     * @param scope The SSE scope. Supported values are <code>application</code>, <code>session</code> and <code>view</code>, case insensitive. If
     * <code>null</code>, the default is <code>application</code>.
     * @param oldUser The user object representing the old owner of the given channel. If not <code>null</code>, then scope may not be <code>application</code>.
     * @param newUser The user object representing the new owner of the given channel. If not <code>null</code>, then scope may not be <code>application</code>.
     */
    protected void switchUser(String channel, String scope, Serializable oldUser, Serializable newUser) {
        if (oldUser != null) {
            var userId = getSessionUsers().remove(oldUser);

            if (userId != null) {
                sseUsers.deregister(oldUser, userId);
            }
        }

        register(channel, scope, newUser);
        fireEvent(new SseEvent(channel, newUser, oldUser), Switched.LITERAL);
    }

    /**
     * When current session scope is about to be destroyed, deregister all session scope channels and explicitly close any open SSE connections associated with
     * it to avoid stale connections. If any, also deregister session users.
     */
    @PreDestroy
    @Override
    protected void deregisterSessionScope() {
        super.deregisterSessionScope();
    }

    // Template methods -----------------------------------------------------------------------------------------------

    @Override
    protected ConcurrentHashMap<String, String> getApplicationScope() {
        return APPLICATION_SCOPE;
    }

    @Override
    protected PushSessionManager<?> getPushSessions() {
        return sseSessions;
    }

    @Override
    protected PushUserManager getPushUsers() {
        return sseUsers;
    }

    @Override
    protected Map<String, String> getViewScopedChannels(boolean create) {
        var bean = Beans.getInstance(ViewScope.class, create);

        if (bean != null) {
            return bean.getChannels();
        }
        else if (!create) {
            return EMPTY_SCOPE;
        }
        else {
            throw new IllegalStateException(ERROR_VIEW_SCOPE_UNAVAILABLE);
        }
    }

    // Nested classes -------------------------------------------------------------------------------------------------

    /**
     * This helps the SSE channel manager to hold view scoped SSE channel identifiers registered by <code>&lt;o:sse&gt;</code>.
     * <p>
     * Since this class is {@code public} it can be externally registered into environment-specific CDI bean management facility.
     *
     * @author Bauke Scholtz
     * @see SseChannelManager
     * @since 5.2
     */
    @ViewScoped
    public static class ViewScope implements Serializable {

        private static final long serialVersionUID = 1L;

        private final ConcurrentHashMap<String, String> channels = new ConcurrentHashMap<>(ESTIMATED_CHANNELS_PER_VIEW, 1);

        /**
         * Returns the view scoped channels.
         *
         * @return The view scoped channels.
         */
        protected Map<String, String> getChannels() {
            return channels;
        }

        /**
         * When current view scope is about to be destroyed, deregister all view scoped channels and explicitly complete any open SSE connections associated
         * with it to avoid stale connections.
         */
        @PreDestroy
        protected void deregisterViewScope() {
            getReference(SseSessionManager.class).deregister(channels.values());
        }

    }

    // Internal -------------------------------------------------------------------------------------------------------

    /**
     * Internal usage only. Workaround for it being unavailable via {@code @Inject} in Faces components.
     */
    static SseChannelManager getInstance() {
        return getReference(SseChannelManager.class);
    }

    /**
     * For internal usage only. This makes it possible to resolve the session and view scoped channel ID during push send time in {@link SsePushContext}.
     */
    static String getChannelId(String channel, Map<String, String> sessionScope, Map<String, String> viewScope) {
        return PushChannelManager.getChannelId(channel, APPLICATION_SCOPE, sessionScope, viewScope);
    }

    // Serialization --------------------------------------------------------------------------------------------------

    private void writeObject(ObjectOutputStream output) throws IOException {
        output.defaultWriteObject();
        serializeState(output);
    }

    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        deserializeState(input);
    }

}
