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

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Meta data for a value that's stored in a cache. Can be used for cache implementations that don't support both validity
 * and attributes natively. Cache implementations are not required to use this type.
 *
 * @since 1.1
 * @author Arjan Tijms
 *
 */
public class CacheEntry implements Serializable {

	private static final long serialVersionUID = -4602586599152573869L;

	private Object value;
	private Date validTill;
	private Map<String, Object> attributes;

	public CacheEntry(Object value, Date validTill) {
		super();
		this.value = value;
		this.validTill = validTill;
	}

	/**
	 * Returns the value for which this object is keeping meta data
	 *
	 * @return The value for which meta data is kept
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Sets the value for which this object is keeping meta data
	 *
	 * @param value
	 *            The value for which meta data is kept
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * Returns the date that's the last moment in time the value obtained via getValue() is valid. After that moment,
	 * the value should not be used.
	 *
	 * @return date indicating last moment value hold by this object is valid.
	 */
	public Date getValidTill() {
		return validTill;
	}

	/**
	 * Sets the date that's the last moment in time the value obtained via getValue() is valid. After that moment, the
	 * value should not be used.
	 *
	 * @param validTill
	 *            date indicating last moment value hold by this object is valid.
	 * @since 1.2
	 */
	public void setValidTill(Date validTill) {
		this.validTill = validTill;
	}

	/**
	 * Returns whether this entry holds a valid value. If false is returned, the value should not be used and the cache
	 * implementation should try to remove this entry and its associated value from the cache.
	 *
	 * @return true if this entry is still valid, false otherwise.
	 */
	public boolean isValid() {
		return validTill == null ? true : new Date().before(validTill);
	}

	/**
	 * Gets a map of attributes associated with this entry.
	 * <p>
	 * Attributes are general key,value pairs, that are currently mainly used to store the result of EL expressions that
	 * appear in the rendering represented by the main value this entry represents.
	 *
	 * @return a map of attributes associated with this entry.
	 * @since 1.2
	 */
	public Map<String, Object> getAttributes() {
		if (attributes == null) {
			// NOTE: lazy initialization means the map can be created multiple times
			// in case of concurrent access (likely with application scoped caches on
			// popular pages). We assume here that it being cached data that can be created
			// by one request or the other, eventually one request will create the lasting one
			// and the Maps that are lost don't matter.
			attributes = new HashMap<>();
		}

		return attributes;
	}

}