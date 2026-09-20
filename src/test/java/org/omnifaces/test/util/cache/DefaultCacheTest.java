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
package org.omnifaces.test.util.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.omnifaces.test.Concurrency.testThreadSafety;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omnifaces.util.cache.Cache;
import org.omnifaces.util.cache.DefaultCache;

class DefaultCacheTest {

    private static final int NEVER_EXPIRES = -1;
    private static final int ALREADY_EXPIRED = 0;
    private static final String KEY = "key";

    private Cache cache;

    @BeforeEach
    void setup() {
        cache = new DefaultCache(null, null);
    }

    @Test
    void testValueIsReturnedUntilItExpires() {
        cache.putObject(KEY, "value", NEVER_EXPIRES);
        assertEquals("value", cache.getObject(KEY), "stored value is returned");

        cache.putObject(KEY, "expired", ALREADY_EXPIRED);
        assertNull(cache.getObject(KEY), "expired value is not returned");
    }

    @Test
    void testExpiredEntryIsReplacedInsteadOfRefreshed() {
        cache.putObject(KEY, "expired", ALREADY_EXPIRED);
        cache.putObject(KEY, "value", NEVER_EXPIRES);

        assertEquals("value", cache.getObject(KEY), "value put over an expired entry is returned");
    }

    @Test
    void testAttributeIsReturnedUntilItsEntryExpires() {
        cache.putAttribute(KEY, "name", "value", NEVER_EXPIRES);
        assertEquals("value", cache.getAttribute(KEY, "name"), "stored attribute is returned");

        cache.putAttribute("expiring", "name", "value", ALREADY_EXPIRED);
        assertNull(cache.getAttribute("expiring", "name"), "attribute of an expired entry is not returned");
    }

    @Test
    void testConcurrentPutAndGetOnSameKeyKeepsOneEntry() {
        testThreadSafety(i -> {
            cache.putObject(KEY, "value" + i, NEVER_EXPIRES);
            cache.getObject(KEY);
        });

        assertNotNull(cache.getObject(KEY), "one of the concurrently put values is returned");
    }

}
