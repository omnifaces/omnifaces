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

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple in-memory cache implementation that's used if the user did not configure an explicit caching provider.
 * 
 * @since 1.1
 * @author Arjan Tijms
 * 
 */
public class DefaultCache implements Cache {

	// Still a quick temp thing - will be replaced soon.
	private final Map<String, Object> cacheStore = new ConcurrentHashMap<String, Object>();

	@Override
	public String get(String key) {
		Object value = cacheStore.get(key);
		
		if (value instanceof String) {
			return (String) value;
		} else if (value instanceof CacheEntry) {
			CacheEntry entry = (CacheEntry) value;
			if (entry.isValid()) {
				return entry.getValue();
			} else {
				cacheStore.remove(key);
			}
		}
		
		return null;
	}

	@Override
	public void put(String key, String value) {
		cacheStore.put(key, value);
	}
	
	@Override
	public void put(String key, String value, int timeToLive) {
		cacheStore.put(key, new CacheEntry(value, new Date(currentTimeMillis() + SECONDS.toMillis(timeToLive))));
	}

}
