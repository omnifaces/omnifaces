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
package org.omnifaces.util.cache;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.AbstractMap.SimpleEntry;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.omnifaces.util.Callback.SerializableBiConsumer;

/**
 * Minimal implementation of thread safe LRU cache with support for eviction listener.
 * Inspired by <a href="https://github.com/ben-manes/concurrentlinkedhashmap">ConcurrentLinkedHashMap</a>.
 *
 * @author Bauke Scholtz
 * @param <K> The generic map key type.
 * @param <V> The generic map value type.
 * @since 4.4
 */
public class LruCache<K extends Serializable, V extends Serializable> implements ConcurrentMap<K, V>, Serializable {

    private static final long serialVersionUID = 1L;

    private static final String ERROR_NULL_KEY_DISALLOWED = "key may not be null";
    private static final String ERROR_NULL_VALUE_DISALLOWED = "value may not be null";

    private final int maximumCapacity;
    private final SerializableBiConsumer<K, V> evictionListener;
    private final LinkedHashMap<K, V> entries;

    private final transient Lock lock = new ReentrantLock();

    // Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Construct LRU cache with given maximum capacity.
     * @param maximumCapacity The maximum capacity.
     * @throws IllegalArgumentException when maximum capacity is less than 2.
     */
    public LruCache(int maximumCapacity) {
        this(maximumCapacity, (key, value) -> {});
    }

    /**
     * Construct LRU cache with given maximum capacity and eviction listener.
     * @param maximumCapacity The maximum capacity.
     * @param evictionListener The eviction listener.
     * @throws IllegalArgumentException when maximum capacity is less than 2.
     */
    public LruCache(int maximumCapacity, SerializableBiConsumer<K, V> evictionListener) {
        if (maximumCapacity < 2) {
            throw new IllegalArgumentException("It does not make sense having a maximum capacity less than 2.");
        }

        requireNonNull(evictionListener, "Use the other constructor when you do not have an eviction listener.");

        this.maximumCapacity = maximumCapacity;
        this.evictionListener = evictionListener;
        this.entries = new LinkedHashMap<>(maximumCapacity);
    }

    // Internal -------------------------------------------------------------------------------------------------------

    private <R> R withLock(Supplier<R> task) {
        lock.lock();

        try {
            return task.get();
        }
        finally {
            lock.unlock();
        }
    }

    // Mutation methods -----------------------------------------------------------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        requireNonNull(key, ERROR_NULL_KEY_DISALLOWED);
        return withLock(() -> {
            V value = entries.remove(key);
            if (value != null) {
                entries.put((K) key, value);
                return value;
            }
            return null;
        });
    }

    @Override
    public V put(K key, V value) {
        return put(key, value, false);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return put(key, value, true);
    }

    private V put(K key, V value, boolean onlyIfAbsent) {
        requireNonNull(key, ERROR_NULL_KEY_DISALLOWED);
        requireNonNull(value, ERROR_NULL_VALUE_DISALLOWED);
        Set<Entry<K, V>> evictedEntries = new HashSet<>(1);
        V previousValue = withLock(() -> {
            V existingValue = entries.remove(key);

            while (entries.size() >= maximumCapacity) {
                K leastRecentlyUsedKey = entries.keySet().iterator().next();
                evictedEntries.add(new SimpleEntry<>(leastRecentlyUsedKey, entries.remove(leastRecentlyUsedKey)));
            }

            entries.put(key, (onlyIfAbsent && existingValue != null) ? existingValue : value);
            return existingValue;
        });

        evictedEntries.forEach(evictedEntry -> evictionListener.accept(evictedEntry.getKey(), evictedEntry.getValue()));
        return previousValue;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        map.forEach(this::put);
    }

    @Override
    public V remove(Object key) {
        requireNonNull(key, ERROR_NULL_KEY_DISALLOWED);
        return withLock(() -> entries.remove(key));
    }

    @Override
    public boolean remove(Object key, Object value) {
        requireNonNull(key, ERROR_NULL_KEY_DISALLOWED);
        requireNonNull(value, ERROR_NULL_VALUE_DISALLOWED);
        return withLock(() -> value.equals(entries.get(key)) && entries.remove(key) != null);
    }

    @Override
    public V replace(K key, V value) {
        requireNonNull(key, ERROR_NULL_KEY_DISALLOWED);
        requireNonNull(value, ERROR_NULL_VALUE_DISALLOWED);
        return withLock(() -> entries.containsKey(key) ? put(key, value, false) : null);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        requireNonNull(key, ERROR_NULL_KEY_DISALLOWED);
        requireNonNull(oldValue, ERROR_NULL_VALUE_DISALLOWED);
        requireNonNull(newValue, ERROR_NULL_VALUE_DISALLOWED);
        return withLock(() -> oldValue.equals(entries.get(key)) && put(key, newValue, false) != null);
    }

    @Override
    public void clear() {
        withLock(() -> { entries.clear(); return null; });
    }

    // Readonly methods -----------------------------------------------------------------------------------------------

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return entries.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return entries.containsValue(value);
    }

    // Readonly views -------------------------------------------------------------------------------------------------

    @Override
    public Set<K> keySet() {
        return new ReadOnlyCollection<>(entries::keySet);
    }

    @Override
    public Collection<V> values() {
        return new ReadOnlyCollection<>(entries::values);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new ReadOnlyCollection<>(entries::entrySet);
    }

    // Inner classes --------------------------------------------------------------------------------------------------

    private final class ReadOnlyCollection<E> extends AbstractSet<E> {

        private final Supplier<Collection<E>> collectionSupplier;

        public ReadOnlyCollection(Supplier<Collection<E>> collectionSupplier) {
            this.collectionSupplier = collectionSupplier;
        }

        @Override
        public int size() {
            return entries.size();
        }

        @Override
        public Iterator<E> iterator() {
            return new ReadOnlyIterator<>(collectionSupplier.get().iterator());
        }
    }

    private final class ReadOnlyIterator<E> implements Iterator<E> {

        private final Iterator<E> iterator;

        private ReadOnlyIterator(Iterator<E> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public E next() {
            return iterator.next();
        }
    }

}
