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

import static java.util.concurrent.TimeUnit.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	 * Format the given date in the given pattern. This is useful when you want to format dates in for example the
	 * <code>title</code> attribute of an UI component, or the <code>itemLabel</code> attribute of select item, or
	 * wherever you can't use the <code>&lt;f:convertDateTime&gt;</code> tag.
	 * @param date The date to be formatted in the given pattern.
	 * @param pattern The pattern to format the given date in.
	 * @return The date which is formatted in the given pattern.
	 * @throws NullPointerException When the pattern is <code>null</code>.
	 */
	public static String formatDate(Date date, String pattern) {
		if (date == null) {
			return null;
		}

		if (pattern == null) {
			throw new NullPointerException("pattern");
		}

		return new SimpleDateFormat(pattern).format(date);
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
	 * Converts seconds to days.
	 * @param seconds The seconds to be converted to days.
	 * @return Days converted from seconds.
	 */
	public static Long secondsToDays(Long seconds) {
		if (seconds == null) {
			return null;
		}

		return DAYS.convert(seconds, SECONDS);
	}

	/**
	 * Converts days to seconds.
	 * @param days The days to be converted to seconds.
	 * @return Seconds converted from days.
	 */
	public static Long daysToSeconds(Long days) {
		if (days == null) {
			return null;
		}

		return SECONDS.convert(days, DAYS);
	}

	/**
	 * Converts bytes to kilobytes.
	 * @param bytes The bytes to be converted to kilobytes.
	 * @return Kilobytes converted from bytes.
	 */
	public static Long bytesToKilobytes(Long bytes) {
		if (bytes == null) {
			return null;
		}

		return bytes >> 10;
	}

	/**
	 * Converts bytes to megabytes.
	 * @param bytes The bytes to be converted to megabytes.
	 * @return Megabytes converted from bytes.
	 */
	public static Long bytesToMegabytes(Long bytes) {
		if (bytes == null) {
			return null;
		}

		return bytes >> 20;
	}

}