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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap.SimpleEntry;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import org.omnifaces.util.Callback.SerializableBiConsumer;
import org.omnifaces.util.Lazy;

/**
 * Minimal implementation of LRU cache with support for eviction listener.
 * Inspired by <a href="https://github.com/ben-manes/concurrentlinkedhashmap">ConcurrentLinkedHashMap</a>.
 *
 * @author Bauke Scholtz
 * @param <K> The generic map key type.
 * @param <V> The generic map value type.
 * @since 4.4
 */
public class LruCache<K, V> implements ConcurrentMap<K, V>, Serializable {

	private static final long serialVersionUID = 1L;

	private int maximumCapacity;
	private SerializableBiConsumer<K, V> evictionListener;

	private transient ConcurrentLinkedQueue<K> recentlyUsedKeys;
	private transient ConcurrentHashMap<K, V> entries;
	private transient Lazy<Set<K>> keySet;
	private transient Lazy<Collection<V>> values;
	private transient Lazy<Set<Entry<K, V>>> entrySet;
	private transient Lock readLock;
	private transient Lock writeLock;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct LRU cache with given maximum capacity.
	 * @param maximumCapacity The maximum capacity.
	 * @throws IllegalArgumentException when maximum capacity is less than 2.
	 */
	public LruCache(int maximumCapacity) {
		construct(maximumCapacity, null);
	}

	/**
	 * Construct LRU cache with given maximum capacity and eviction listener.
	 * @param maximumCapacity The maximum capacity.
	 * @param evictionListener The eviction listener.
	 * @throws IllegalArgumentException when maximum capacity is less than 2.
	 */
	public LruCache(int maximumCapacity, SerializableBiConsumer<K, V> evictionListener) {
		construct(maximumCapacity, evictionListener);
	}

	// Internal -------------------------------------------------------------------------------------------------------

	private void construct(int maximumCapacity, SerializableBiConsumer<K, V> evictionListener) {
		if (maximumCapacity < 2) {
			throw new IllegalArgumentException();
		}

		this.maximumCapacity = maximumCapacity;
		this.evictionListener = evictionListener;
		this.recentlyUsedKeys = new ConcurrentLinkedQueue<>();
		this.entries = new ConcurrentHashMap<>(maximumCapacity);
		this.keySet = new Lazy<>(KeySet::new);
		this.values = new Lazy<>(Values::new);
		this.entrySet = new Lazy<>(EntrySet::new);

		ReadWriteLock lock = new ReentrantReadWriteLock();
		this.readLock = lock.readLock();
		this.writeLock = lock.writeLock();
	}

	private V performConcurrently(Lock lock, Supplier<V> valueSupplier) {
		lock.lock();

		try {
			return valueSupplier.get();
		}
		finally {
			lock.unlock();
		}
	}

	// Read methods ---------------------------------------------------------------------------------------------------

	@Override
	@SuppressWarnings("unchecked")
	public V get(Object key) {
		return performConcurrently(readLock, () -> {
			recentlyUsedKeys.remove(key);
			recentlyUsedKeys.add((K) key);
			return entries.get(key);
		});
	}

	// Write methods --------------------------------------------------------------------------------------------------

	@Override
	public V put(K key, V value) {
		return put(key, value, false);
	}

	@Override
	public V putIfAbsent(K key, V value) {
		return put(key, value, true);
	}

	@SuppressWarnings("unchecked")
	private V put(K key, V value, boolean onlyIfAbsent) {
		Entry<K, V>[] evictedEntry = new Entry[1];
		V previousValue = performConcurrently(writeLock, () -> {
			recentlyUsedKeys.remove(key);

			if (recentlyUsedKeys.size() == maximumCapacity) {
				K leastRecentlyUsedKey = recentlyUsedKeys.poll();
				evictedEntry[0] = new SimpleEntry<>(leastRecentlyUsedKey, entries.remove(leastRecentlyUsedKey));
			}

			recentlyUsedKeys.add(key);
			return (onlyIfAbsent && containsKey(key)) ? entries.get(key) : entries.put(key, value);
		});

		if (evictedEntry[0] != null) {
			evictionListener.accept(evictedEntry[0].getKey(), evictedEntry[0].getValue());
		}

		return previousValue;
	}

	@Override
	public V remove(Object key) {
		return performConcurrently(writeLock, () -> {
			recentlyUsedKeys.remove(key);
			return entries.remove(key);
		});
	}

	@Override
	public void clear() {
		performConcurrently(writeLock, () -> {
			recentlyUsedKeys.clear();
			entries.clear();
			return null;
		});
	}

	// Delegate methods -----------------------------------------------------------------------------------------------

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

	@Override
	public Set<K> keySet() {
		return keySet.get();
	}

	@Override
	public Collection<V> values() {
		return values.get();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return entrySet.get();
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

	private final class KeySet extends AbstractSet<K> {

		@Override
		public int size() {
			return LruCache.this.size();
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

	private final class Values extends AbstractCollection<V> {

		@Override
		public int size() {
			return LruCache.this.size();
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

	private final class EntrySet extends AbstractSet<Entry<K, V>> {

		@Override
		public int size() {
			return LruCache.this.size();
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

	// Serialization --------------------------------------------------------------------------------------------------

	private void writeObject(ObjectOutputStream output) throws IOException {
		output.defaultWriteObject();
		output.writeInt(maximumCapacity);
		output.writeObject(evictionListener);
		output.writeObject(recentlyUsedKeys.stream().collect(toMap(identity(), entries::get, (l, r) -> l, LinkedHashMap::new)));
	}

	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
		input.defaultReadObject();
		construct(input.readInt(), (SerializableBiConsumer<K, V>) input.readObject());
		putAll((Map<K, V>) input.readObject());
	}

}
