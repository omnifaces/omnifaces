/*
 * Copyright 2012 OmniFaces.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An LRU cache, based on {@link LinkedHashMap}. This cache has a fixed maximum number of elements which needs to be
 * specified by the <code>cacheSize</code> constructor argument. If the cache is full and another entry is added, the
 * LRU (least recently used) entry is dropped.
 * <p>
 * This class is thread-safe. All methods of this class are <code>synchronized</code>.
 *
 * @author Bauke Scholtz
 * @see LinkedHashMap
 * @link http://www.source-code.biz/snippets/java/6.htm
 */
public class LRUCache<K, V> {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final float LOAD_FACTOR = 0.75f;

	// Properties -----------------------------------------------------------------------------------------------------

	private Map<K, V> map;
	private int cacheSize;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Creates a new LRU cache.
	 * @param cacheSize The maximum number of entries that will be kept in this cache.
	 */
	public LRUCache(int cacheSize) {
		this.cacheSize = cacheSize;
		int capacity = (int) Math.ceil(cacheSize / LOAD_FACTOR) + 1;
		map = new LinkedHashMap<K, V>(capacity, LOAD_FACTOR, true) {

			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
				return size() > LRUCache.this.cacheSize;
			}
		};
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Retrieves an entry from the cache. The retrieved entry becomes the MRU (most recently used) entry.
	 * @param key The key whose associated value is to be returned.
	 * @return The value associated to this key, or <code>null</code> if no value with this key exists in the cache.
	 */
	public synchronized V get(K key) {
		return map.get(key);
	}

	/**
	 * Adds an entry to this cache. The new entry becomes the MRU (most recently used) entry. If an entry with the
	 * specified key already exists in the cache, it is replaced by the new entry and the old entry is returned. If the
	 * cache is full, the LRU (least recently used) entry is removed from the cache.
	 * @param key The key with which the specified value is to be associated.
	 * @param value A value to be associated with the specified key.
	 * @return The previous value associated with key, or <code>null</code> if there was no entry or its value was
	 * actually <code>null</code>.
	 */
	public synchronized V put(K key, V value) {
		return map.put(key, value);
	}

	/**
	 * Clears the cache.
	 */
	public synchronized void clear() {
		map.clear();
	}

	/**
	 * Returns the number of entries currently in the cache.
	 * @return The number of entries currently in the cache.
	 */
	public synchronized int size() {
		return map.size();
	}

}