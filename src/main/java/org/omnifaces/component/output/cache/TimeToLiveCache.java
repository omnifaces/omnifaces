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

/**
 * Base class that can be used by Map based caches that don't support time to live semantics and arbitrary attributes natively.
 *
 * @since 1.1
 * @author Arjan Tijms
 *
 */
public abstract class TimeToLiveCache implements Cache {

	private static final long serialVersionUID = 6637500586287606410L;

	private final Integer defaultTimeToLive;
	private Map<String, CacheEntry> cacheStore;

	public TimeToLiveCache(Integer defaultTimeToLive) {
		this.defaultTimeToLive = defaultTimeToLive;
	}

	@Override
	public String get(String key) {
		return (String) getObject(key);
	}

    public Object getObject(String key) {
        CacheEntry entry = cacheStore.get(key);

		if (entry != null) {
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
		if (defaultTimeToLive != null) {
			put(key, value, defaultTimeToLive);
		} else {
			put(key, value, -1);
		}
	}

	@Override
	public void put(String key, String value, int timeToLive) {
		putObject(key, value, timeToLive);
	}

    public void putObject(String key, Object value, int timeToLive) {
        CacheEntry entry = cacheStore.get(key);

		if (entry == null || !entry.isValid()) {
			cacheStore.put(key, new CacheEntry(value, timeToLiveToDate(timeToLive)));
		} else {
			entry.setValue(value);
			entry.setValidTill(timeToLiveToDate(timeToLive));
		}
    }

	@Override
	public void putAttribute(String key, String name, Object value, int timeToLive) {
		CacheEntry entry = cacheStore.get(key);

		if (entry == null || !entry.isValid()) {
			// NOTE: timeToLive is only used when a new entry is created
			entry = new CacheEntry(null, timeToLiveToDate(timeToLive));
			cacheStore.put(key, entry);
		}

		entry.getAttributes().put(name, value);
	}

	@Override
	public Object getAttribute(String key, String name) {
		CacheEntry entry = cacheStore.get(key);

		if (entry != null) {
			if (entry.isValid()) {
				return entry.getAttributes().get(name);
			} else {
				cacheStore.remove(key);
			}
		}

		return null;
	}

	@Override
	public void remove(String key) {
		cacheStore.remove(key);
	}

	protected void setCacheStore(Map<String, CacheEntry> cacheStore) {
		this.cacheStore = cacheStore;
	}

	private Date timeToLiveToDate(int timeToLive) {
		if (timeToLive != -1) {
			return new Date(currentTimeMillis() + SECONDS.toMillis(timeToLive));
		} else {
			return null;
		}
	}

}