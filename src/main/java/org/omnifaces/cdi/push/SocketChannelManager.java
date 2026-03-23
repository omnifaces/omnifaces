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

import org.omnifaces.cdi.push.SocketEvent.Switched;
import org.omnifaces.util.Beans;

/**
 * <p>
 * This web socket channel manager holds all application and session scoped web socket channel identifiers registered by
 * <code>&lt;o:socket&gt;</code>.
 *
 * @author Bauke Scholtz
 * @see Socket
 * @since 2.3
 */
@SessionScoped
public class SocketChannelManager extends PushChannelManager {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final long serialVersionUID = 1L;

    private static final String ERROR_VIEW_SCOPE_UNAVAILABLE =
        "o:socket view scope is unavailable."
            + " Perhaps you need to explicitly register the SocketChannelManager.ViewScope as a CDI managed bean?";

    // Properties -----------------------------------------------------------------------------------------------------

    private static final ConcurrentHashMap<String, String> APPLICATION_SCOPE = new ConcurrentHashMap<>(ESTIMATED_CHANNELS_PER_APPLICATION, 1);

    @Inject
    private SocketSessionManager socketSessions;

    @Inject
    private SocketUserManager socketUsers;

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * Switch the user on the given channel on the given scope from the given old user to the given new user.
     * @param channel The web socket channel.
     * @param scope The web socket scope. Supported values are <code>application</code>, <code>session</code> and
     * <code>view</code>, case insensitive. If <code>null</code>, the default is <code>application</code>.
     * @param oldUser The user object representing the old owner of the given channel. If not <code>null</code>, then scope
     * may not be <code>application</code>.
     * @param newUser The user object representing the new owner of the given channel. If not <code>null</code>, then scope
     * may not be <code>application</code>.
     */
    protected void switchUser(String channel, String scope, Serializable oldUser, Serializable newUser) {
        if (oldUser != null) {
            var userId = getSessionUsers().remove(oldUser);

            if (userId != null) {
                socketUsers.deregister(oldUser, userId);
            }
        }

        register(channel, scope, newUser);
        fireEvent(new SocketEvent(channel, newUser, oldUser, null), Switched.LITERAL);
    }

    /**
     * When current session scope is about to be destroyed, deregister all session scope channels and explicitly close
     * any open web sockets associated with it to avoid stale websockets. If any, also deregister session users.
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
        return socketSessions;
    }

    @Override
    protected PushUserManager getPushUsers() {
        return socketUsers;
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

    @Override
    protected String getComponentName() {
        return "o:socket";
    }

    // Nested classes -------------------------------------------------------------------------------------------------

    /**
     * This helps the web socket channel manager to hold view scoped web socket channel identifiers registered by
     * <code>&lt;o:socket&gt;</code>.
     * <p>
     * Since OmniFaces 4.6 this class is {@code public} instead of {@code protected} so it can be externally registered
     * into environment-specific CDI bean management facility.
     * @author Bauke Scholtz
     * @see SocketChannelManager
     * @since 2.3
     */
    @ViewScoped
    public static class ViewScope implements Serializable {

        private static final long serialVersionUID = 1L;

        private final ConcurrentHashMap<String, String> channels = new ConcurrentHashMap<>(ESTIMATED_CHANNELS_PER_VIEW, 1);

        /**
         * Returns the view scoped channels.
         * @return The view scoped channels.
         */
        protected Map<String, String> getChannels() {
            return channels;
        }

        /**
         * When current view scope is about to be destroyed, deregister all view scoped channels and explicitly close
         * any open web sockets associated with it to avoid stale websockets.
         */
        @PreDestroy
        protected void deregisterViewScope() {
            SocketSessionManager.getInstance().deregister(channels.values());
        }

    }

    // Internal -------------------------------------------------------------------------------------------------------

    /**
     * Internal usage only. Awkward workaround for it being unavailable via @Inject in Faces components and listeners.
     */
    static SocketChannelManager getInstance() {
        return getReference(SocketChannelManager.class);
    }

    /**
     * For internal usage only. This makes it possible to resolve the session and view scoped channel ID during push
     * send time in {@link SocketPushContext}.
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
