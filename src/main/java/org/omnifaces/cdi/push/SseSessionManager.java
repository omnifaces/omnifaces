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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.servlet.AsyncContext;

/**
 * <p>
 * This SSE session manager holds all SSE connections by their channel identifier.
 *
 * @author Bauke Scholtz
 * @see SseServlet
 * @since 5.2
 */
@ApplicationScoped
public class SseSessionManager extends PushSessionManager<AsyncContext> {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final Logger logger = Logger.getLogger(SseSessionManager.class.getName());

    private static final byte[] SSE_DATA_PREFIX = "data: ".getBytes(UTF_8);
    private static final byte[] SSE_DATA_SUFFIX = "\n\n".getBytes(UTF_8);

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * On open, add given async context to the mapping associated with given channel identifier and return
     * <code>true</code> if it's accepted (i.e. the channel identifier is known), otherwise <code>false</code>.
     * @param channelId The channel identifier.
     * @param asyncContext The opened async context.
     * @return <code>true</code> if given async context is accepted, otherwise <code>false</code>.
     */
    protected boolean add(String channelId, AsyncContext asyncContext) {
        return addSession(channelId, asyncContext);
    }

    /**
     * Remove given async context from the mapping associated with given channel identifier.
     * @param channelId The channel identifier.
     * @param asyncContext The async context to remove.
     */
    protected void remove(String channelId, AsyncContext asyncContext) {
        removeSession(channelId, asyncContext);
    }

    // Template methods -----------------------------------------------------------------------------------------------

    @Override
    protected Future<Void> sendToSession(AsyncContext asyncContext, String message) {
        try {
            var output = asyncContext.getResponse().getOutputStream();

            synchronized (output) {
                output.write(SSE_DATA_PREFIX);
                output.write(message.getBytes(UTF_8));
                output.write(SSE_DATA_SUFFIX);
                output.flush();
            }

            return CompletableFuture.completedFuture(null);
        }
        catch (IOException e) {
            logger.log(FINEST, "Ignoring thrown exception; the SSE connection is apparently closed.", e);
            closeSession(asyncContext);
            return null;
        }
    }

    @Override
    protected void closeSession(AsyncContext asyncContext) {
        try {
            asyncContext.complete();
        }
        catch (IllegalStateException ignore) {
            logger.log(FINEST, "Ignoring thrown exception; there is nothing more we could do here.", ignore);
        }
    }

}
