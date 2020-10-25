/*
 * Copyright 2020 OmniFaces
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
package org.omnifaces.util;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

/**
 * implements lazy-initialized object primarily for final and transient fields
 * Utilizes double-checked locking for optimization
 * <p>
 * Example:
 * <pre>
 * {@code
 * private final Lazy<Object> lazy = new Lazy<>(Object::new);
 * Object lazyInstance = lazy.get();
 * }
 * </pre>
 *
 * <a href="https://github.com/flowlogix/flowlogix/blob/master/jakarta-ee/jee-examples/src/main/java/com/flowlogix/examples/LazyExample.java"
 * target="_blank">Example Code (GitHub)</a>
 *
 * @author lprimak
 * @param <TT> type of object
 */
@RequiredArgsConstructor
public final class Lazy<TT> {
    private TT delegate;
    private volatile boolean initialized;
    private final Supplier<TT> initFunction;
    private final Lock lock = new ReentrantLock();

    /**
     *
     * @return underlying object, initialize within Tenant Control when necessary
     */
    public TT get() {
        boolean localInitialized = this.initialized;
        if (!localInitialized) {
            lock.lock();
            try {
                if (!initialized) {
                    delegate = initFunction.get();
                }
                initialized = true;

            } finally {
                lock.unlock();
            }
        }
        return delegate;
    }
}
