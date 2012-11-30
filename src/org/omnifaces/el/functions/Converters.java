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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Collection of EL functions for data conversion.
 *
 * @author Bauke Scholtz
 */
public final class Converters {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String ERROR_NOT_AN_ARRAY = "The given type '%' is not an array at all.";

	// Constructors ---------------------------------------------------------------------------------------------------

	private Converters() {
		// Hide constructor.
	}

	// Utility --------------------------------------------------------------------------------------------------------

	/**
	 * Converts a <code>Set&lt;E&gt;</code> to a <code>List&lt;E&gt;</code>. Useful when you want to iterate over a
	 * <code>Set</code> in for example <code>&lt;ui:repeat&gt;</code>.
	 * @param set The set to be converted to list of its entries.
	 * @return The converted list.
	 */
	public static <E> List<E> setToList(Set<E> set) {
		if (set == null) {
			return null;
		}

		return new ArrayList<E>(set);
	}

	/**
	 * Converts a <code>Map&lt;K, V&gt;</code> to a <code>List&lt;Map.Entry&lt;K, V&gt;&gt;</code>. Useful when you want
	 * to iterate over a <code>Map</code> in for example <code>&lt;ui:repeat&gt;</code>. Each of the entries has the
	 * usual <code>getKey()</code> and <code>getValue()</code> methods.
	 * @param map The map to be converted to list of its entries.
	 * @return The converted list.
	 */
	public static <K, V> List<Map.Entry<K, V>> mapToList(Map<K, V> map) {
		if (map == null) {
			return null;
		}

		return new ArrayList<Map.Entry<K, V>>(map.entrySet());
	}

	/**
	 * Joins all elements of the given array to a single string, separated by the given separator.
	 * @param array The array to be joined.
	 * @param separator The separator to be used. If null, then it defaults to empty string.
	 * @return All elements of the given array as a single string, separated by the given separator.
	 * @throws IllegalArgumentException When the given array is not an array at all.
	 * @since 1.3
	 */
	public static String joinArray(Object array, String separator) {
		if (array == null) {
			return null;
		}

		if (!array.getClass().isArray()) {
			throw new IllegalArgumentException(String.format(ERROR_NOT_AN_ARRAY, array.getClass()));
		}

		if (separator == null) {
			separator = "";
		}

		StringBuilder builder = new StringBuilder();

		for (int i = 0; i < Array.getLength(array); i++) {
			if (i > 0) {
				builder.append(separator);
			}

			builder.append(Array.get(array, i));
		}

		return builder.toString();
	}

	/**
	 * Joins all elements of the given collection to a single string, separated by the given separator.
	 * @param collection The collection to be joined.
	 * @param separator The separator to be used. If null, then it defaults to empty string.
	 * @return All elements of the given collection as a single string, separated by the given separator.
	 * @since 1.3
	 */
	public static <E> String joinCollection(Collection<E> collection, String separator) {
		if (collection == null) {
			return null;
		}

		if (separator == null) {
			separator = "";
		}

		StringBuilder builder = new StringBuilder();
		int i = 0;

		for (E element : collection) {
			if (i++ > 0) {
				builder.append(separator);
			}

			builder.append(element);
		}

		return builder.toString();
	}

	/**
	 * Joins all elements of the given map to a single string, separated by the given key-value pair separator and
	 * entry separator.
	 * @param map The map to be joined.
	 * @param pairSeparator The key-value pair separator to be used. If null, then it defaults to empty string.
	 * @param entrySeparator The entry separator to be used. If null, then it defaults to empty string.
	 * @return All elements of the given map as a single string, separated by the given separators.
	 * @since 1.3
	 */
	public static <K, V> String joinMap(Map<K, V> map, String pairSeparator, String entrySeparator) {
		if (map == null) {
			return null;
		}

		if (pairSeparator == null) {
			pairSeparator = "";
		}

		if (entrySeparator == null) {
			entrySeparator = "";
		}

		StringBuilder builder = new StringBuilder();
		int i = 0;

		for (Entry<K, V> entry : map.entrySet()) {
			if (i++ > 0) {
				builder.append(entrySeparator);
			}

			builder.append(entry.getKey()).append(pairSeparator).append(entry.getValue());
		}

		return builder.toString();
	}

	/**
	 * Print the stack trace of the given exception.
	 * @param exception The exception to print the stack trace for.
	 * @return The printed stack trace.
	 */
	public static String printStackTrace(Throwable exception) {
		if (exception == null) {
			return null;
		}

		StringWriter stringWriter = new StringWriter();
		exception.printStackTrace(new PrintWriter(stringWriter, true));
		return stringWriter.toString();
	}

}