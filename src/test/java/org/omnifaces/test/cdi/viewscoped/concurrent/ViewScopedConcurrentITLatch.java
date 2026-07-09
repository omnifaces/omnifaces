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
package org.omnifaces.test.cdi.viewscoped.concurrent;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.CountDownLatch;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Holds every concurrent request until all of them have registered their view scope in the session. This makes the LRU
 * eviction of the OmniFaces view scope storage deterministic: by the time the last request arrives, the storages of all
 * but the three most recent ones have been evicted, while all of them are still in flight.
 */
@ApplicationScoped
public class ViewScopedConcurrentITLatch {

    /** The number of concurrent requests, deliberately larger than the maximum active view scopes of 3. */
    public static final int CONCURRENT_REQUESTS = 10;

    private static final int TIMEOUT_IN_SECONDS = 30;

    private final CountDownLatch latch = new CountDownLatch(CONCURRENT_REQUESTS);

    /**
     * Awaits until all concurrent requests have arrived here. This is a no-op once they all have, so a request which
     * arrives here again during the same test run will simply continue.
     */
    public void awaitOtherRequests() {
        latch.countDown();

        try {
            latch.await(TIMEOUT_IN_SECONDS, SECONDS);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

}
