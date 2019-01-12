/*
 * Copyright 2019 OmniFaces
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

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of Map that wraps another map. This allows interception of one or more method
 * on this wrapped map.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author Arjan Tijms
 */
public class MapWrapper<K, V> implements Map<K, V>, Serializable {

	private static final long serialVersionUID = 1L;

	private Map<K, V> map;

	/**
	 * Initializes the wrapper with its wrapped map.
	 * @param map the map to wrap.
	 */
	public MapWrapper(Map<K, V> map) {
		this.map = map;
	}

	@Override
	public void clear() {
		getWrapped().clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return getWrapped().containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return getWrapped().containsValue(value);
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return getWrapped().entrySet();
	}

	@Override
	public boolean equals(Object object) {
		return getWrapped().equals(object);
	}

	@Override
	public V get(Object key) {
		return getWrapped().get(key);
	}

	@Override
	public int hashCode() {
		return getWrapped().hashCode();
	}

	@Override
	public boolean isEmpty() {
		return getWrapped().isEmpty();
	}

	@Override
	public Set<K> keySet() {
		return getWrapped().keySet();
	}

	@Override
	public V put(K key, V value) {
		return getWrapped().put(key, value);
	}

	@Override
	public void putAll(Map< ? extends K, ? extends V> m) {
		getWrapped().putAll(m);
	}

	@Override
	public V remove(Object key) {
		return getWrapped().remove(key);
	}

	@Override
	public int size() {
		return getWrapped().size();
	}

	@Override
	public Collection<V> values() {
		return getWrapped().values();
	}

	public Map<K, V> getWrapped() {
		return map;
	}

}