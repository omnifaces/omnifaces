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
package org.omnifaces.util;

import java.util.Collection;
import java.util.Map;

/**
 * Collection of general utility methods that do not fit in one of the more JSF-specific classes.
 *
 * @author Arjan Tijms, Bauke Scholtz
 *
 */
public final class Utils {

	// Constructors ---------------------------------------------------------------------------------------------------

	private Utils() {
		// Hide constructor.
	}

	// Utility --------------------------------------------------------------------------------------------------------

	/**
	 * Returns true if the given string is null or is empty.
	 * @param string The string to be checked on emptiness.
	 * @return True if the given string is null or is empty.
	 */
	public static boolean isEmpty(String string) {
		return string == null || string.isEmpty();
	}

	/**
	 * Returns true if the given array is null or is empty.
	 * @param array The array to be checked on emptiness.
	 * @return True if the given array is null or is empty.
	 */
	public static boolean isEmpty(Object[] array) {
		return array == null || array.length == 0;
	}

	/**
	 * Returns true if the given value is null or is empty. Types of String, Object[], Collection and Map are
	 * recognized. If none is recognized, then examine the emptiness of the toString() representation instead.
	 * @param value The value to be checked on emptiness.
	 * @return True if the given value is null or is empty.
	 */
	public static boolean isEmpty(Object value) {
		if (value == null) {
			return true;
		}
		else if (value instanceof String) {
			return ((String) value).isEmpty();
		}
		else if (value instanceof Object[]) {
			return ((Object[]) value).length == 0;
		}
		else if (value instanceof Collection<?>) {
			return ((Collection<?>) value).isEmpty();
		}
		else if (value instanceof Map<?, ?>) {
			return ((Map<?, ?>) value).isEmpty();
		}
		else {
		    return value.toString() == null || value.toString().isEmpty();
		}
	}

}