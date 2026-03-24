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

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.omnifaces.cdi.Push;
import org.omnifaces.util.Beans;

/**
 * <p>
 * This servlet handles SSE connections opened by <code>&lt;o:sse&gt;</code> and  by <code>&lt;o:notification&gt;</code>.
 * It is automatically registered during application startup when <code>&#64;</code>{@link Push}<code>(type=SSE)</code>
 * or <code>&#64;</code>{@link Push}<code>(type=NOTIFICATION)</code> qualified injection points are detected by
 * {@link PushExtension}.
 * <p>
 * The servlet is mapped to <code>/omnifaces.sse/*</code> where the channel identifier is passed as query string in the
 * format <code>/{channel}?{uuid}</code>. Each connection is held open using {@link AsyncContext} with an indefinite
 * timeout. Messages are sent as SSE data frames in <code>data: {json}\n\n</code> format.
 *
 * @author Bauke Scholtz
 * @see Sse
 * @see SseSessionManager
 * @since 5.2
 */
public class SseServlet extends HttpServlet {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final Logger logger = Logger.getLogger(SseServlet.class.getName());
    private static final long serialVersionUID = 1L;
    private static final byte[] SSE_NOT_FOUND_EVENT = "event: close\ndata: 404\n\n".getBytes(UTF_8);

    static final String PARAM_CHANNEL = "channel";

    // Actions --------------------------------------------------------------------------------------------------------

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/event-stream");
        response.setHeader("Cache-Control", "no-cache");

        var channel = getChannel(request);
        var channelId = channel + "?" + request.getQueryString();
        var sseSessions = Beans.getReference(SseSessionManager.class);

        if (channel == null || !sseSessions.isRegistered(channelId)) {
            response.getOutputStream().write(SSE_NOT_FOUND_EVENT);
            return;
        }

        response.setHeader("X-Accel-Buffering", "no"); // Disables response buffering in Nginx so SSE events are flushed immediately to the client.
        response.flushBuffer();

        var asyncContext = request.startAsync();
        asyncContext.setTimeout(0);
        sseSessions.add(channelId, channel, asyncContext);
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
                    logger.log(FINEST, "Ignoring thrown exception; can happen when the CDI context is no longer active.", e);
                }
            }
        });
    }

    // Helpers --------------------------------------------------------------------------------------------------------

    private static String getChannel(HttpServletRequest request) {
        var pathInfo = request.getPathInfo();
        return (pathInfo != null && pathInfo.length() > 1) ? pathInfo.substring(1) : null;
    }
}
