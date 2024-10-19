package org.omnifaces.test.util.cache;

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static java.util.stream.IntStream.rangeClosed;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omnifaces.util.cache.LruCache;

class TestLruCache {

    private static final Logger logger = Logger.getLogger(TestLruCache.class.getName());

    private static final int SIZE = 1000;
    private static final int ITERATIONS = (SIZE * SIZE) + 1;
    private static final int LAST_EXISTING_KEY = SIZE * SIZE - SIZE;

    private Map<String, String> lruCache;
    private Set<String> evicted;

    @BeforeEach
    void setup() {
        evicted = ConcurrentHashMap.newKeySet();
        lruCache = new LruCache<>(SIZE, (k,v) -> evicted.add(k));
        range(0, SIZE).forEach(i -> lruCache.put("k" + (i * SIZE), "v" + i));
    }

    @Test
    void testPutThreadSafety() {
        testThreadSafety(i -> lruCache.put("k" + i, "v" + i));

        if (evicted.size() == ITERATIONS - SIZE + 1) {
            // Very sometimes the last existing key is evicted while put by another thread, just inevitable nature of the test using overlapping keys, we'll want to remove the known key from the eviction set.
            var keyOfLastIteration = "k" + LAST_EXISTING_KEY;
            List<String> leftIntersecting = evicted.stream().filter(lruCache::containsKey).collect(toList());
            List<String> rightIntersecting = lruCache.keySet().stream().filter(evicted::contains).collect(toList());

            if (leftIntersecting.size() == 1 && leftIntersecting.contains(keyOfLastIteration) && leftIntersecting.equals(rightIntersecting)) { // Just to ensure it's indeed that one.
                logger.warning("Last existing key evicted while put by another thread: " + keyOfLastIteration);
                evicted.remove(keyOfLastIteration);
            }
        }

        assertAll(
            () -> assertEquals(SIZE, lruCache.size(), "size must be still " + SIZE),
            () -> assertEquals(ITERATIONS - SIZE, evicted.size(), "there must be " + (ITERATIONS - SIZE) + " evictions")
        );
    }

    @Test
    void testGetThreadSafety() {
        testThreadSafety(i -> lruCache.get("k" + i));

        assertAll(
            () -> assertEquals(SIZE, lruCache.size(), "size must be still " + SIZE),
            () -> assertEquals(0, evicted.size())
        );
    }

    @Test
    void testRemoveThreadSafety() {
        testThreadSafety(i -> lruCache.remove("k" + i));

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
        });

        assertAll(
            () -> assertTrue(lruCache.size() <= SIZE, lruCache.size() + "may not be greater than " + SIZE), // On i9-10900X this seems to have lower limit of SIZE - 10.
            () -> assertTrue(evicted.size() <= (ITERATIONS - SIZE), evicted.size() + " may not be more than " + (ITERATIONS - SIZE)) // On i9-10900X this seems to be around 3% of ITERATIONS.
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
                    assertDoesNotThrow(() -> (cache.size() + "," + cache.entrySet().size()), "calling size() and entrySet().size() at same time may not throw ConcurrentModificationException");
                }));
            });
        }, ROUNDS);
    }

    private static void testThreadSafety(Consumer<Integer> task) {
        testThreadSafety((i, tasks) -> tasks.add(runAsync(() -> task.accept(i))));
    }

    private static void testThreadSafety(BiConsumer<Integer, Set<CompletableFuture<Void>>> task) {
        testThreadSafety(task, ITERATIONS);
    }

    private static void testThreadSafety(BiConsumer<Integer, Set<CompletableFuture<Void>>> task, int iterations) {
        Set<CompletableFuture<Void>> tasks = ConcurrentHashMap.newKeySet();
        range(0, iterations).forEach(i -> task.accept(i, tasks));
        awaitCompletion(tasks);
    }

    private static void awaitCompletion(Set<CompletableFuture<Void>> tasks) {
        tasks.forEach(t -> {
            try {
                t.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Test
    void testEvictionSequence() {
        lruCache.get("k0");
        lruCache.put("k1", "v1");
        lruCache.put("k2000", "v2");
        lruCache.remove("k3000");
        lruCache.put("k4", "v4");
        lruCache.put("k5", "v5");

        assertAll(
            () -> assertEquals(SIZE, lruCache.size(), "size must be still " + SIZE),
            () -> assertEquals(Set.of("k1000", "k4000"), evicted, "k0 should not be evicted as it was explicitly accessed; "
                                                                + "k2000 should not be evicted as it was explicitly replaced; "
                                                                + "k3000 should not be evicted as it was explicitly removed")
        );
    }

    @Test
    void testImmutableViews() {
        assertAll(
            () -> assertThrows(UnsupportedOperationException.class, () -> lruCache.keySet().add("k"), "lruCache.keySet().add(\"k0\")"),
            () -> assertThrows(UnsupportedOperationException.class, () -> lruCache.keySet().remove("k0"), "lruCache.keySet().remove(\"k0\")"),
            () -> assertThrows(UnsupportedOperationException.class, () -> lruCache.values().add("v"), "lruCache.values().add(\"v\")"),
            () -> assertThrows(UnsupportedOperationException.class, () -> lruCache.values().remove("v0"), "lruCache.values().remove(\"v0\")"),
            () -> assertThrows(UnsupportedOperationException.class, () -> lruCache.entrySet().add(new SimpleEntry<>("k", "v")), "lruCache.entrySet().remove(new SimpleEntry<>(\"k\", \"v\"))"),
            () -> assertThrows(UnsupportedOperationException.class, () -> lruCache.entrySet().remove(new SimpleEntry<>("k0", "v0")), "lruCache.entrySet().remove(new SimpleEntry<>(\"k0\", \"v0\"))")
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
