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

import java.util.logging.Logger;

import jakarta.enterprise.context.Dependent;

import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;

/**
 * <p>
 * This producer prepares the {@link PushContext} instance for injection by <code>&#64;</code>{@link Push}.
 *
 * @author Bauke Scholtz
 * @see Push
 * @since 2.3
 * @deprecated Since 5.2. Use {@link PushContextProducer} instead.
 */
@Dependent
@Deprecated(since = "5.2", forRemoval = true)
public class SocketPushContextProducer extends PushContextProducer {

    private static final Logger logger = Logger.getLogger(SocketPushContextProducer.class.getName());

    static {
        logger.warning("SocketPushContextProducer is deprecated since 5.2, please migrate to PushContextProducer.");
    }
}
