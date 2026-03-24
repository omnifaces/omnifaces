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

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessInjectionPoint;

import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;

/**
 * <p>
 * CDI extension that detects <code>&#64;Inject &#64;Push PushContext</code> injection points during bean discovery.
 * This enables automatic registration of the {@link SocketEndpoint} and/or {@link SseServlet} without requiring
 * explicit configuration.
 *
 * @author Bauke Scholtz
 * @see Socket
 * @see Sse
 * @since 5.2
 */
public class PushExtension implements Extension {

    // Variables ------------------------------------------------------------------------------------------------------

    private static volatile boolean socketActivated;
    private static volatile boolean sseActivated;

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * Collect injection points of type {@link PushContext} qualified with {@link Push} and detect whether Web Socket
     * and/or SSE push is used.
     * @param <T> The generic bean type.
     * @param event The process injection point event.
     */
    public <T> void collect(@Observes ProcessInjectionPoint<T, PushContext> event) {
        for (var qualifier : event.getInjectionPoint().getQualifiers()) {
            if (qualifier.annotationType() == Push.class) {
                switch (((Push) qualifier).type()) {
                    case SSE, NOTIFICATION -> sseActivated = true;
                    case SOCKET -> socketActivated = true;
                }
            }
        }
    }

    // Getters --------------------------------------------------------------------------------------------------------

    /**
     * Returns whether a <code>&#64;Push PushContext</code> (Web Socket) injection point was detected during bean
     * discovery.
     * @return Whether a Web Socket push injection point was detected during bean discovery.
     */
    static boolean isSocketActivated() {
        return socketActivated;
    }

    /**
     * Returns whether a <code>&#64;Push(type=SSE)</code> or <code>&#64;Push(type=NOTIFICATION)</code>
     * {@link PushContext} injection point was detected during bean discovery.
     * @return Whether an SSE push injection point was detected during bean discovery.
     */
    static boolean isSseActivated() {
        return sseActivated;
    }

}
