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
package org.omnifaces.component.output.cache;

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
public interface Cache {

	/**
	 * Gets a value from the cache
	 * 
	 * @param key
	 *            the key under which a value was previously stored
	 * @return The previously stored value, or null if no such value exists
	 */
	String get(String key);

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
	 * 				the amount of time in seconds for which the cached value is valid from the time it's being added to the cache.
	 * 				It's provider specific whether the cache implementation will actually remove (evict) the entry after this time has
	 * 				elapsed or will only perform a check upon accessing the cache entry. Whatever method the implementation chooses; after
	 * 				this time is elapsed a call to {@link Cache#get(String)} should return null.
	 */
	void put(String key, String value, int timeToLive);
	
	/**
	 * Removes a value from the cache
	 * 
	 * @param key
	 *            the key under which a value is to be stored
	 */
	void remove(String key);
}
