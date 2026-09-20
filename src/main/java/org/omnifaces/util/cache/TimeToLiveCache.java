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

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.Serializable;
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

    private static final long serialVersionUID = 1L;

    private final Integer defaultTimeToLive;
    private Map<String, CacheEntry> cacheStore;

    protected TimeToLiveCache(Integer defaultTimeToLive) {
        this.defaultTimeToLive = defaultTimeToLive;
    }

    @Override
    public String get(String key) {
        return (String) getObject(key);
    }

    @Override
    public Serializable getObject(String key) {
        var entry = getValidEntry(key);
        return entry != null ? entry.getValue() : null;
    }

    @Override
    public void put(String key, String value) {
        if (defaultTimeToLive != null) {
            put(key, value, defaultTimeToLive);
        }
        else {
            put(key, value, -1);
        }
    }

    @Override
    public void put(String key, String value, int timeToLive) {
        putObject(key, value, timeToLive);
    }

    @Override
    public void putObject(String key, Serializable value, int timeToLive) {
        cacheStore.compute(key, (k, entry) -> {
            if (entry == null || !entry.isValid()) {
                return new CacheEntry(value, timeToLiveToDate(timeToLive));
            }

            entry.setValue(value);
            entry.setValidTill(timeToLiveToDate(timeToLive));
            return entry;
        });
    }

    @Override
    public void putAttribute(String key, String name, Serializable value, int timeToLive) {
        cacheStore.compute(key, (k, entry) -> {
            // NOTE: timeToLive is only used when a new entry is created
            var validEntry = entry == null || !entry.isValid() ? new CacheEntry(null, timeToLiveToDate(timeToLive)) : entry;
            validEntry.getAttributes().put(name, value);
            return validEntry;
        });
    }

    @Override
    public Serializable getAttribute(String key, String name) {
        var entry = getValidEntry(key);
        return entry != null ? entry.getAttributes().get(name) : null;
    }

    /**
     * Returns the entry of the given key, removing it first when it has expired.
     */
    private CacheEntry getValidEntry(String key) {
        return cacheStore.computeIfPresent(key, (k, entry) -> entry.isValid() ? entry : null);
    }

    @Override
    public void remove(String key) {
        cacheStore.remove(key);
    }

    @Override
    public void clear() {
        cacheStore.clear();
    }

    protected void setCacheStore(Map<String, CacheEntry> cacheStore) {
        this.cacheStore = cacheStore;
    }

    private static Date timeToLiveToDate(int timeToLive) {
        if (timeToLive != -1) {
            return new Date(currentTimeMillis() + SECONDS.toMillis(timeToLive));
        }
        else {
            return null;
        }
    }

}
