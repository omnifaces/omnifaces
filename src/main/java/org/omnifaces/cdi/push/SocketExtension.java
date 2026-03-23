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
 * This enables automatic registration of the {@link SocketEndpoint} without requiring the
 * {@value Socket#PARAM_SOCKET_ENDPOINT_ENABLED} context parameter.
 *
 * @author Bauke Scholtz
 * @see Socket
 * @since 5.2
 */
public class SocketExtension implements Extension {

    private static volatile boolean pushContextInjected;

    /**
     * Collect injection points of type {@link PushContext} qualified with {@link Push}.
     * @param <T> The generic bean type.
     * @param event The process injection point event.
     */
    public <T> void collect(@Observes ProcessInjectionPoint<T, PushContext> event) {
        if (event.getInjectionPoint().getQualifiers().stream().anyMatch(q -> q.annotationType() == Push.class)) {
            pushContextInjected = true;
        }
    }

    /**
     * Returns whether a <code>&#64;Push PushContext</code> injection point was detected during bean discovery.
     * @return Whether a <code>&#64;Push PushContext</code> injection point was detected during bean discovery.
     */
    static boolean isPushContextInjected() {
        return pushContextInjected;
    }

}
