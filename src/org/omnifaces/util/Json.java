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
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.omnifaces.el.functions.Strings;

/**
 * A simple JSON encoder.
 *
 * @author Bauke Scholtz
 * @since 1.2
 */
public final class Json {

	// Constructors ---------------------------------------------------------------------------------------------------

	private Json() {
		// Hide constructor.
	}

	// Encode ---------------------------------------------------------------------------------------------------------

	/**
	 * Encodes the given object as JSON. This supports the standard types {@link Boolean}, {@link Number},
	 * {@link String} and {@link Date}. It also supports arrays, {@link Collection}s and {@link Map}s of them, even
	 * nested ones. The {@link Date} is formatted in RFC 1123 format, so you can if necessary just pass it straight to
	 * <code>new Date()</code> in JavaScript.
	 * <p>
	 * Complex objects like javabeans are not supported. Consider a "real" JSON library instead if you need this.
	 * @param object The object to be encoded as JSON.
	 * @return The JSON-encoded representation of the given object.
	 * @throws IllegalArgumentException When an unsupported type is encountered in the given object argument.
	 */
	public static String encode(Object object) {
		if (object == null) {
			return "null";
		}
		else if (object instanceof Boolean || object instanceof Number) {
			return object.toString();
		}
		else if (object instanceof String) {
			return '"' + Strings.escapeJS((String) object) + '"';
		}
		else if (object instanceof Date) {
			return '"' + Utils.formatRFC1123((Date) object) + '"';
		}
		else if (object instanceof Object[]) {
			StringBuilder builder = new StringBuilder();

			for (Object item : ((Object[]) object)) {
				if (builder.length() > 0) {
					builder.append(',');
				}

				builder.append(encode(item));
			}

			return builder.insert(0, '[').append(']').toString();
		}
		else if (object instanceof Collection<?>) {
			return encode(((Collection<?>) object).toArray());
		}
		else if (object instanceof Map<?, ?>) {
			StringBuilder builder = new StringBuilder();

			for (Entry<?, ?> entry : ((Map<?, ?>) object).entrySet()) {
				if (builder.length() > 0) {
					builder.append(',');
				}

				builder.append(encode(String.valueOf(entry.getKey()))).append(':').append(encode(entry.getValue()));
			}

			return builder.insert(0, '{').append('}').toString();
		}
		else {
			throw new IllegalArgumentException("Unsupported type: " + object.getClass());
		}
	}

}