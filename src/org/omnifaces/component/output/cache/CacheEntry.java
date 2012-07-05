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

import java.util.Date;

/**
 * Meta data for a value that's stored in a cache. Can be used for cache implementations that
 * don't support this concept natively.
 * 
 * @since 1.1
 * @author Arjan Tijms
 *
 */
public class CacheEntry {

	private final String value;
	private final Date validTill;

	public CacheEntry(String value, Date validTill) {
		super();
		this.value = value;
		this.validTill = validTill;
	}

	/**
	 * Returns the value for which this object is keeping meta data
	 * @return The value for which meta data is kept
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Returns the date that's the last moment in time the value obtained via getValue() is valid.
	 * After that moment, the value should not be used.
	 * 
	 * @return date indicating last moment value hold by this object is valid.
	 */
	public Date getValidTill() {
		return validTill;
	}
	
	/**
	 * Returns whether this entry holds a valid value. If false is returned, the value
	 * should not be used and the cache implementation should try to remove this entry
	 * and its associated value from the cache.
	 * 
	 * @return true if this entry is still valid, false otherwise.
	 */
	public boolean isValid() {
		return new Date().before(validTill);
	}

}
