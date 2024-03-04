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

import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An in-memory cache implementation that's used if the user did not configure an explicit caching provider.
 * <p>
 * For the actual implementation, {@link com.github.benmanes.caffeine.cache.Cache} is used if a maximum capacity is requested,
 * otherwise a plain {@link ConcurrentHashMap} is used.
 * <p>
 * <b>See:</b> <a href="https://github.com/ben-manes/caffeine">https://github.com/ben-manes/caffeine</a>
 *
 * @author Arjan Tijms
 * @since 1.1
 */
public class DefaultCache extends TimeToLiveCache {

	private static final long serialVersionUID = 1L;

	public DefaultCache(Integer defaultTimeToLive, Integer maxCapacity) {
		super(defaultTimeToLive);
		setCacheStore(createCacheStore(maxCapacity));
	}

	private Map<String, CacheEntry> createCacheStore(Integer maxCapacity) {
		if (maxCapacity != null) {
			return (Map) Caffeine.newBuilder()
					.maximumSize(maxCapacity)
					.build()
					.asMap();
		} else {
			return new ConcurrentHashMap<>();
		}
	}

}