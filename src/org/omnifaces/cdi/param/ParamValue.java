/*
 * Copyright 2013 OmniFaces.
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
package org.omnifaces.cdi.param;

import org.omnifaces.cdi.Param;

/**
 * The type that's injected via the {@link Param} qualifier.
 * <p>
 * This acts as a wrapper for the actual value that is retrieved from the request and optionally converted.
 * 
 * @author Arjan Tijms
 * 
 * @param <V>
 *            The type of the actual value this class is wrapping.
 */
public class ParamValue<V> {

	private final V value;

	public ParamValue(V value) {
		this.value = value;
	}

	public V getValue() {
		return value;
	}

}
