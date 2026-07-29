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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.logging.Level.FINEST;

import java.io.IOException;
import java.util.logging.Logger;

import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.omnifaces.cdi.Push;
import org.omnifaces.util.Beans;

/**
 * <p>
 * This SSE endpoint handles connections opened by <code>&lt;o:sse&gt;</code> and <code>&lt;o:notification&gt;</code>. It is automatically registered during
 * application startup when <code>&#64;</code>{@link Push}<code>(type=SSE)</code> or <code>&#64;</code>{@link Push}<code>(type=NOTIFICATION)</code> qualified
 * injection points are detected by {@link PushExtension}.
 *
 * @author Bauke Scholtz
 * @see Sse
 * @see SseSessionManager
 * @since 5.2
 */
public class SseEndpoint extends HttpServlet {

    // Constants ------------------------------------------------------------------------------------------------------

    /** The URI path parameter name of the SSE channel. */
    static final String PARAM_CHANNEL = "channel";

    private static final Logger logger = Logger.getLogger(SseEndpoint.class.getName());
    private static final long serialVersionUID = 1L;
    private static final byte[] SSE_NOT_FOUND_EVENT = "event: close\ndata: 404\n\n".getBytes(UTF_8);
    private static final byte[] SSE_COMMENT_EVENT = ":\n\n".getBytes(UTF_8);

    // Properties -----------------------------------------------------------------------------------------------------

    private long idleTimeout;

    // Actions --------------------------------------------------------------------------------------------------------

    @Override
    public void init() {
        var value = getInitParameter(Sse.PARAM_SSE_ENDPOINT_IDLE_TIMEOUT);
        idleTimeout = value == null ? 0 : Long.parseLong(value);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/event-stream");
        response.setHeader("Cache-Control", "no-cache");

        var channel = getChannel(request);
        var channelId = channel + "?" + request.getQueryString();
        var sseSessions = Beans.getReference(SseSessionManager.class);

        if (channel == null || !sseSessions.isRegistered(channelId) || !isAuthorized(request, channelId)) {
            response.getOutputStream().write(SSE_NOT_FOUND_EVENT);
            return;
        }

        response.setHeader("X-Accel-Buffering", "no"); // Disables response buffering in Nginx so SSE events are flushed immediately to the client.
        response.flushBuffer(); // Commits the response head on containers which only deliver it while the request is still synchronous.

        var asyncContext = request.startAsync();
        asyncContext.setTimeout(idleTimeout);
        response.getOutputStream().write(SSE_COMMENT_EVENT); // Commits it on containers which don't commit an empty buffer. Clients ignore an SSE comment.
        response.getOutputStream().flush();

        if (!sseSessions.add(channelId, channel, asyncContext)) {
            sseSessions.closeSession(asyncContext);
            return;
        }

        asyncContext.addListener(new AsyncListener() {

            @Override
            public void onStartAsync(AsyncEvent event) {
                // NOOP.
            }

            @Override
            public void onComplete(AsyncEvent event) {
                removeSseSession();
            }

            @Override
            public void onError(AsyncEvent event) {
                removeSseSession();
            }

            @Override
            public void onTimeout(AsyncEvent event) {
                removeSseSession();
                asyncContext.complete();
            }

            private void removeSseSession() {
                try {
                    sseSessions.remove(channelId, asyncContext);
                }
                catch (IllegalStateException e) {
                    logger.log(FINEST, "Ignoring thrown exception; can happen when the CDI context is no longer active (e.g. server shutdown).", e);
                }
            }

        });
    }

    // Helpers --------------------------------------------------------------------------------------------------------

    private static String getChannel(HttpServletRequest request) {
        var pathInfo = request.getPathInfo();
        return (pathInfo != null && pathInfo.length() > 1) ? pathInfo.substring(1) : null;
    }

    /**
     * A session or view scoped channel may only be connected to by the HTTP session which registered it. Application scoped channels are by design not bound to
     * any HTTP session.
     */
    private static boolean isAuthorized(HttpServletRequest request, String channelId) {
        return SseChannelManager.isApplicationScopedChannelId(channelId)
            || PushChannelManager.isChannelIdRegisteredInSession(request.getSession(false), channelId);
    }

}
