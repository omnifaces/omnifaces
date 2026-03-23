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

import org.omnifaces.util.Beans;

/**
 * <p>
 * This SSE channel manager holds all application and session scoped SSE channel identifiers registered by
 * <code>&lt;o:sse&gt;</code>.
 *
 * @author Bauke Scholtz
 * @see Sse
 * @since 5.2
 */
@SessionScoped
public class SseChannelManager extends PushChannelManager {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final long serialVersionUID = 1L;

    private static final String ERROR_VIEW_SCOPE_UNAVAILABLE =
        "o:sse view scope is unavailable."
            + " Perhaps you need to explicitly register the SseChannelManager.ViewScope as a CDI managed bean?";

    // Properties -----------------------------------------------------------------------------------------------------

    private static final ConcurrentHashMap<String, String> APPLICATION_SCOPE = new ConcurrentHashMap<>(ESTIMATED_CHANNELS_PER_APPLICATION, 1);

    @Inject
    private SseSessionManager sseSessions;

    @Inject
    private SseUserManager sseUsers;

    // Actions --------------------------------------------------------------------------------------------------------

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
     * This helps the SSE channel manager to hold view scoped SSE channel identifiers registered by
     * <code>&lt;o:sse&gt;</code>.
     * <p>
     * Since this class is {@code public} it can be externally registered into environment-specific CDI bean
     * management facility.
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
         * @return The view scoped channels.
         */
        protected Map<String, String> getChannels() {
            return channels;
        }

        /**
         * When current view scope is about to be destroyed, deregister all view scoped channels and explicitly
         * complete any open SSE connections associated with it to avoid stale connections.
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
     * For internal usage only. This makes it possible to resolve the session and view scoped channel ID during push
     * send time in {@link SsePushContext}.
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
