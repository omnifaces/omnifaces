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

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.stream.IntStream.range;
import static java.util.stream.IntStream.rangeClosed;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omnifaces.test.Concurrency.testThreadSafety;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omnifaces.util.cache.LruCache;

class LruCacheTest {

    private static final int SIZE = 100;
    private static final int ITERATIONS = (SIZE * SIZE) + 1;

    private Map<String, String> lruCache;
    private Set<String> evicted;

    @BeforeEach
    void setup() {
        evicted = ConcurrentHashMap.newKeySet();
        lruCache = new LruCache<>(SIZE, (k, v) -> evicted.add(k));
        range(0, SIZE).forEach(i -> lruCache.put("k" + (i * SIZE), "v" + i));
    }

    @Test
    void testPutThreadSafety() {
        testThreadSafety(i -> lruCache.put("k" + i, "v" + i), ITERATIONS);

        if (evicted.size() > ITERATIONS - SIZE) {
            // Very sometimes a setup key is evicted while being re-put by another thread, just inevitable nature of the test using overlapping keys.
            // Remove any such keys from the eviction set, but only if they are indeed setup keys (multiples of SIZE).
            var reAddedAfterEviction = evicted.stream().filter(lruCache::containsKey).toList();

            if (!reAddedAfterEviction.isEmpty() && reAddedAfterEviction.stream().allMatch(k -> Integer.parseInt(k.substring(1)) % SIZE == 0)) {
                reAddedAfterEviction.forEach(evicted::remove);
            }
        }

        assertAll(
            () -> assertEquals(SIZE, lruCache.size(), "size must be still " + SIZE),
            () -> assertEquals(ITERATIONS - SIZE, evicted.size(), "there must be " + (ITERATIONS - SIZE) + " evictions")
        );
    }

    @Test
    void testGetThreadSafety() {
        testThreadSafety(i -> lruCache.get("k" + i), ITERATIONS);

        assertAll(
            () -> assertEquals(SIZE, lruCache.size(), "size must be still " + SIZE),
            () -> assertEquals(0, evicted.size())
        );
    }

    @Test
    void testRemoveThreadSafety() {
        testThreadSafety(i -> lruCache.remove("k" + i), ITERATIONS);

        assertAll(
            () -> assertEquals(0, lruCache.size(), "size must be 0"),
            () -> assertEquals(0, evicted.size())
        );
    }

    @Test
    void testMixedThreadSafetyWithSameKey() {
        testThreadSafety((i, tasks) -> {
            var k = "k" + i;
            tasks.add(runAsync(() -> lruCache.put(k, "v" + i)));
            tasks.add(runAsync(() -> lruCache.get(k)));
            tasks.add(runAsync(() -> lruCache.remove(k)));
        }, ITERATIONS);

        assertAll(
            () -> assertTrue(lruCache.size() <= SIZE, lruCache.size() + "may not be greater than " + SIZE), // On i9-10900X this seems to have lower limit of
                                                                                                            // SIZE - 10.
            () -> assertTrue(evicted.size() <= (ITERATIONS - SIZE), evicted.size() + " may not be more than " + (ITERATIONS - SIZE)) // On i9-10900X this seems
                                                                                                                                     // to be around 3% of
                                                                                                                                     // ITERATIONS.
        );
    }

    @Test
    void testImmutableViewsThreadSafety() {
        // The default values are too big for this test.
        final var CACHE_SIZE = 8;
        final var ROUNDS = 10;
        final var MAX_VALUE = 32;
        final var cache = new LruCache<Integer, Integer>(CACHE_SIZE);

        testThreadSafety((i, tasks) -> {
            rangeClosed(0, MAX_VALUE).boxed().forEach(index -> {
                tasks.add(runAsync(() -> {
                    cache.put(index, index);
                    assertDoesNotThrow(
                        () -> (cache.size() + "," + cache.entrySet().size()),
                        "calling size() and entrySet().size() at same time may not throw ConcurrentModificationException"
                    );
                }));
            });
        }, ROUNDS);
    }

    @Test
    void testEvictionSequence() {
        lruCache.get("k0");
        lruCache.put("k1", "v1");
        lruCache.put("k" + (2 * SIZE), "v2");
        lruCache.remove("k" + (3 * SIZE));
        lruCache.put("k4", "v4");
        lruCache.put("k5", "v5");

        assertAll(
            () -> assertEquals(SIZE, lruCache.size(), "size must be still " + SIZE),
            () -> assertEquals(
                Set.of("k" + SIZE, "k" + (4 * SIZE)), evicted, "k0 should not be evicted as it was explicitly accessed; "
                    + "k" + (2 * SIZE) + " should not be evicted as it was explicitly replaced; "
                    + "k" + (3 * SIZE) + " should not be evicted as it was explicitly removed"
            )
        );
    }

    @Test
    void testImmutableViews() {
        var keySet = lruCache.keySet();
        var values = lruCache.values();
        var entrySet = lruCache.entrySet();
        var newEntry = new SimpleEntry<>("k", "v");
        var existingEntry = new SimpleEntry<>("k0", "v0");

        assertAll(
            () -> assertThrows(UnsupportedOperationException.class, () -> keySet.add("k"), "lruCache.keySet().add(\"k\")"),
            () -> assertThrows(UnsupportedOperationException.class, () -> keySet.remove("k0"), "lruCache.keySet().remove(\"k0\")"),
            () -> assertThrows(UnsupportedOperationException.class, () -> values.add("v"), "lruCache.values().add(\"v\")"),
            () -> assertThrows(UnsupportedOperationException.class, () -> values.remove("v0"), "lruCache.values().remove(\"v0\")"),
            () -> assertThrows(
                UnsupportedOperationException.class, () -> entrySet.add(newEntry),
                "lruCache.entrySet().add(new SimpleEntry<>(\"k\", \"v\"))"
            ),
            () -> assertThrows(
                UnsupportedOperationException.class, () -> entrySet.remove(existingEntry),
                "lruCache.entrySet().remove(new SimpleEntry<>(\"k0\", \"v0\"))"
            )
        );
    }

    @Test
    void testImmutableIterators() {
        assertAll(
            () -> assertThrows(UnsupportedOperationException.class, () -> removeFirstEntryFromIterator(lruCache::keySet), "lruCache.keySet().iterator()"),
            () -> assertThrows(UnsupportedOperationException.class, () -> removeFirstEntryFromIterator(lruCache::values), "lruCache.values().iterator()"),
            () -> assertThrows(UnsupportedOperationException.class, () -> removeFirstEntryFromIterator(lruCache::entrySet), "lruCache.entrySet().iterator()")
        );
    }

    private static void removeFirstEntryFromIterator(Supplier<Collection<?>> viewSupplier) {
        Iterator<?> iterator = viewSupplier.get().iterator();
        iterator.next();
        iterator.remove();
    }

    @Test
    void testNullKeyAndValueDisallowed() {
        var key = "key";
        var value = "value";

        assertAll(
            () -> assertThrows(NullPointerException.class, () -> lruCache.get(null), "lruCache.get(null)"),
            () -> assertThrows(NullPointerException.class, () -> lruCache.put(null, null), "lruCache.put(null, null)"),
            () -> assertThrows(NullPointerException.class, () -> lruCache.put(key, null), "lruCache.put(key, null)"),
            () -> assertThrows(NullPointerException.class, () -> lruCache.put(null, value), "lruCache.put(null, value)"),
            () -> assertThrows(NullPointerException.class, () -> lruCache.putIfAbsent(null, null), "lruCache.putIfAbsent(null, null)"),
            () -> assertThrows(NullPointerException.class, () -> lruCache.putIfAbsent(key, null), "lruCache.putIfAbsent(key, null)"),
            () -> assertThrows(NullPointerException.class, () -> lruCache.putIfAbsent(null, value), "lruCache.putIfAbsent(null, value)"),
            () -> assertThrows(NullPointerException.class, () -> lruCache.putAll(null), "lruCache.putAll(null)"),
            () -> assertThrows(NullPointerException.class, () -> lruCache.remove(null), "lruCache.remove(null)"),
            () -> assertThrows(NullPointerException.class, () -> lruCache.remove(null, null), "lruCache.remove(null, null)"),
            () -> assertThrows(NullPointerException.class, () -> lruCache.remove(key, null), "lruCache.remove(key, null)"),
            () -> assertThrows(NullPointerException.class, () -> lruCache.remove(null, value), "lruCache.remove(null, value)"),
            () -> assertThrows(NullPointerException.class, () -> lruCache.replace(null, null), "lruCache.replace(null, null)"),
            () -> assertThrows(NullPointerException.class, () -> lruCache.replace(key, null), "lruCache.replace(key, null)"),
            () -> assertThrows(NullPointerException.class, () -> lruCache.replace(null, value), "lruCache.replace(null, value)"),
            () -> assertThrows(NullPointerException.class, () -> lruCache.replace(null, null, null), "lruCache.replace(null, null, null)"),
            () -> assertThrows(NullPointerException.class, () -> lruCache.replace(key, null, null), "lruCache.replace(key, null, null)"),
            () -> assertThrows(NullPointerException.class, () -> lruCache.replace(null, value, null), "lruCache.replace(null, value, null)"),
            () -> assertThrows(NullPointerException.class, () -> lruCache.replace(null, null, value), "lruCache.replace(null, null, value)"),
            () -> assertThrows(NullPointerException.class, () -> lruCache.replace(key, value, null), "lruCache.replace(key, value, null)"),
            () -> assertThrows(NullPointerException.class, () -> lruCache.replace(key, null, value), "lruCache.replace(key, null, value)"),
            () -> assertThrows(NullPointerException.class, () -> lruCache.replace(null, value, value), "lruCache.replace(null, value, value)")
        );
    }

}
