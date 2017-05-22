/*
 * Copyright 2017 OmniFaces
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
package org.omnifaces.util.cache;

import java.io.Serializable;

/**
 * Interface that abstracts a simple get and put operation for a concrete cache implementation.
 * <p>
 * Note that this takes Strings for both key and value since it's not intended as a general cache solution, but is
 * something specific for the {@link Cache} component which caches rendered output.
 *
 * @since 1.1
 * @author Arjan Tijms
 *
 */
public interface Cache extends Serializable {

	/**
	 * Gets a value from the cache
	 *
	 * @param key
	 *            the key under which a value was previously stored
	 * @return The previously stored value, or null if no such value exists
	 */
	String get(String key);

	/**
	 * Gets a value from the cache
	 *
	 * @param key
	 *            the key under which a value was previously stored
	 * @return The previously stored value, or null if no such value exists
	 */
	Serializable getObject(String key);

	/**
	 * Stores a value in the cache
	 *
	 * @param key
	 *            the key under which a value is to be stored
	 * @param value
	 *            the value that is to be stored
	 */
	void put(String key, String value);

	/**
	 * Stores a value in the cache
	 *
	 * @param key
	 *            the key under which a value is to be stored
	 * @param value
	 *            the value that is to be stored
	 * @param timeToLive
	 *            the amount of time in seconds for which the cached value is valid from the time it's being added to
	 *            the cache. It's provider specific whether the cache implementation will actually remove (evict) the
	 *            entry after this time has elapsed or will only perform a check upon accessing the cache entry.
	 *            Whatever method the implementation chooses; after this time is elapsed a call to
	 *            {@link Cache#get(String)} should return null.
	 */
	void putObject(String key, Serializable value, int timeToLive);

	/**
	 * Stores a value in the cache
	 *
	 * @param key
	 *            the key under which a value is to be stored
	 * @param value
	 *            the value that is to be stored
	 * @param timeToLive
	 *            the amount of time in seconds for which the cached value is valid from the time it's being added to
	 *            the cache. It's provider specific whether the cache implementation will actually remove (evict) the
	 *            entry after this time has elapsed or will only perform a check upon accessing the cache entry.
	 *            Whatever method the implementation chooses; after this time is elapsed a call to
	 *            {@link Cache#get(String)} should return null.
	 */
	void put(String key, String value, int timeToLive);

	/**
	 * Gets a named attribute from the cache entry identified by the key parameter.
	 * <p>
	 * This in effect implements a 2-level multi-map, which the single main value stored in the first level, and the
	 * optional attributes stored in the second level.
	 *
	 * @param key
	 *            key that identifies the first level cache entry
	 * @param name
	 *            name of the attribute in the second level
	 * @return the value associated with the {key, name} hierarchy.
	 * @since 1.2
	 */
	Serializable getAttribute(String key, String name);

	/**
	 * Stores a named attribute in the cache entry identified by the key parameter.
	 *
	 * @param key
	 *            key that identifies the first level cache entry
	 * @param name
	 *            name of the attribute in the second level
	 * @param value
	 *            value associated with the {key, name} hierarchy.
	 * @param timeToLive
	 *            the amount of time in seconds for which the cached value is valid. Only used when there's no first
	 *            level entry yet.
	 * @since 1.2
	 */
	void putAttribute(String key, String name, Serializable value, int timeToLive);

	/**
	 * Removes a value from the cache
	 *
	 * @param key
	 *            the key under which a value is to be stored
	 */
	void remove(String key);

}