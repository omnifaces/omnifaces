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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.omnifaces.util.Faces;

/**
 * Collection of EL functions for data conversion.
 *
 * @author Bauke Scholtz
 */
public final class Converters {

	// Constructors ---------------------------------------------------------------------------------------------------

	private Converters() {
		// Hide constructor.
	}

	// Utility --------------------------------------------------------------------------------------------------------

	/**
	 * Format the given bytes to nearest 10<sup>n</sup> with IEC binary unit (KiB, MiB, etc) with rounding precision of
	 * 1 fraction. For example:
	 * <ul>
	 * <li>1023 bytes will appear as 1023 B
	 * <li>1024 bytes will appear as 1.0 KiB
	 * <li>500000 bytes will appear as 488.3 KiB
	 * <li>1048576 bytes will appear as 1.0 GiB
	 * </ul>
	 * The format locale will be set to the one as obtained by {@link Faces#getLocale()}.
	 * @param bytes The bytes to be formatted.
	 * @return The formatted bytes.
	 */
	public static String formatBytes(Long bytes) {
	    if (bytes < 1024) {
	    	return bytes + " B";
	    }

	    int exp = (int) (Math.log(bytes) / Math.log(1024));
	    return String.format(Faces.getLocale(), "%.1f %ciB", bytes / Math.pow(1024, exp), "KMGTPE".charAt(exp - 1));
	}

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
	 * Print the stack trace of the given exception.
	 * @param exception The exception to print the stack trace for.
	 * @return The printed stack trace.
	 */
	public static String printStackTrace(Throwable exception) {
		StringWriter stringWriter = new StringWriter();
		exception.printStackTrace(new PrintWriter(stringWriter, true));
		return stringWriter.toString();
	}

}