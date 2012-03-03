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
package org.omnifaces.el.functions;

/**
 * Collection of EL functions for array manipulation.
 *
 * @author Bauke Scholtz
 */
public final class Arrays {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String ERROR_INVALID_ARRAY_SIZE = "The array size must be at least 0.";

	// Constructors ---------------------------------------------------------------------------------------------------

	private Arrays() {
		// Hide constructor.
	}

	// Utility --------------------------------------------------------------------------------------------------------

	/**
	 * Creates and returns a dummy object array of the given size. This is useful if you want to iterate <i>n</i> times
	 * over an <code>&lt;ui:repeat&gt;</code>, which doesn't support EL in <code>begin</code> and <code>end</code>
	 * attributes.
	 * @param size The size of the dummy object array.
	 * @return A dummy object array of the given size.
	 * @throws IllegalArgumentException When the size is less than 0.
	 */
	public static Object[] createArray(int size) {
		if (size < 0) {
			throw new IllegalArgumentException(ERROR_INVALID_ARRAY_SIZE);
		}

		return new Object[size];
	}

	/**
	 * Returns <code>true</code> if the string representation of an item of the given array equals to the string
	 * representation of the given item. This returns <code>false</code> if either the array or the item is null. This
	 * is useful if you want to for example check if <code>#{paramValues.foo}</code> contains a certain value.
	 * @param array The array whose items have to be compared.
	 * @param item The item to be compared.
	 * @return <code>true</code> if the string representation of an item of the given array equals to the string
	 * representation of the given item.
	 */
	public static boolean contains(Object[] array, Object item) {
		if (array == null || item == null) {
			return false;
		}

		for (Object object : array) {
			if (object != null && object.toString().equals(item.toString())) {
				return true;
			}
		}

		return false;
	}

}