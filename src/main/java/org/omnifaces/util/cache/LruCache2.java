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

import java.io.Serializable;
import java.util.AbstractCollection;
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
public class LruCache2<K, V> implements ConcurrentMap<K, V>, Serializable {

	private static final long serialVersionUID = 1L;

	private int maximumCapacity;
	private SerializableBiConsumer<K, V> evictionListener;
	private LinkedHashMap<K, V> entries;

	private final transient Lock lock = new ReentrantLock();

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct LRU cache with given maximum capacity.
	 * @param maximumCapacity The maximum capacity.
	 * @throws IllegalArgumentException when maximum capacity is less than 2.
	 */
	public LruCache2(int maximumCapacity) {
		this(maximumCapacity, (key, value) -> {});
	}

	/**
	 * Construct LRU cache with given maximum capacity and eviction listener.
	 * @param maximumCapacity The maximum capacity.
	 * @param evictionListener The eviction listener.
	 * @throws IllegalArgumentException when maximum capacity is less than 2.
	 */
	public LruCache2(int maximumCapacity, SerializableBiConsumer<K, V> evictionListener) {
		if (maximumCapacity < 2) {
			throw new IllegalArgumentException();
		}

		this.maximumCapacity = maximumCapacity;
		this.evictionListener = evictionListener;
		this.entries = new LinkedHashMap<>(maximumCapacity);
	}

	// Internal -------------------------------------------------------------------------------------------------------

	private V withLock(Supplier<V> valueSupplier) {
		lock.lock();

		try {
			return valueSupplier.get();
		}
		finally {
			lock.unlock();
		}
	}

	// Mutation methods -----------------------------------------------------------------------------------------------

	@Override
	@SuppressWarnings("unchecked")
	public V get(Object key) {
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
	public V remove(Object key) {
		return withLock(() -> entries.remove(key));
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

	@Override
	public void putAll(Map<? extends K, ? extends V> map) {
		map.forEach(this::put);
	}

	// Readonly views -------------------------------------------------------------------------------------------------

	@Override
	public Set<K> keySet() {
		return new ReadOnlyKeySet();
	}

	@Override
	public Collection<V> values() {
		return new ReadOnlyValues();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return new ReadOnlyEntrySet();
	}

	// We won't support these internally unused methods to keep it simple ---------------------------------------------

	/**
	 * @throws UnsupportedOperationException as it is not implemented to keep it simple.
	 */
	@Override
	public boolean remove(Object key, Object value) {
		throw new UnsupportedOperationException("use containsKey+remove instead");
	}

	/**
	 * @throws UnsupportedOperationException as it is not implemented to keep it simple.
	 */
	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		throw new UnsupportedOperationException("use containsKey+put instead");
	}

	/**
	 * @throws UnsupportedOperationException as it is not implemented to keep it simple.
	 */
	@Override
	public V replace(K key, V value) {
		throw new UnsupportedOperationException("use containsKey+put instead");
	}

	// Inner classes --------------------------------------------------------------------------------------------------

	private final class ReadOnlyKeySet extends AbstractSet<K> {

		@Override
		public int size() {
			return LruCache2.this.size();
		}

		@Override
		public Iterator<K> iterator() {
			return new KeyIterator();
		}
	}

	private final class KeyIterator implements Iterator<K> {

		final Iterator<K> iterator = entries.keySet().iterator();

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public K next() {
			return iterator.next();
		}
	}

	private final class ReadOnlyValues extends AbstractCollection<V> {

		@Override
		public int size() {
			return LruCache2.this.size();
		}

		@Override
		public Iterator<V> iterator() {
			return new ValueIterator();
		}
	}

	private final class ValueIterator implements Iterator<V> {

		private final Iterator<V> iterator = entries.values().iterator();

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public V next() {
			return iterator.next();
		}
	}

	private final class ReadOnlyEntrySet extends AbstractSet<Entry<K, V>> {

		@Override
		public int size() {
			return LruCache2.this.size();
		}

		@Override
		public Iterator<Entry<K, V>> iterator() {
			return new EntryIterator();
		}
	}

	private final class EntryIterator implements Iterator<Entry<K, V>> {

		private final Iterator<Entry<K, V>> iterator = entries.entrySet().iterator();

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public Entry<K, V> next() {
			return iterator.next();
		}
	}

}
