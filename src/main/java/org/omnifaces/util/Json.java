/*
 * Copyright 2018 OmniFaces
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

import static java.lang.String.format;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A simple JSON encoder.
 *
 * @author Bauke Scholtz
 * @since 1.2
 */
public final class Json {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String ERROR_INVALID_BEAN = "Cannot introspect object of type '%s' as bean.";
	private static final String ERROR_INVALID_GETTER = "Cannot invoke getter of property '%s' of bean '%s'.";

	// Constructors ---------------------------------------------------------------------------------------------------

	private Json() {
		// Hide constructor.
	}

	// Encode ---------------------------------------------------------------------------------------------------------

	/**
	 * Encodes the given object as JSON. This supports the standard types {@link Boolean}, {@link Number},
	 * {@link CharSequence} and {@link Date}. If the given object type does not match any of them, then it will attempt
	 * to inspect the object as a javabean whereby the public properties (with public getters) will be encoded as a JS
	 * object. It also supports {@link Collection}s, {@link Map}s and arrays of them, even nested ones. The {@link Date}
	 * is formatted in RFC 1123 format, so you can if necessary just pass it straight to <code>new Date()</code> in
	 * JavaScript.
	 * @param object The object to be encoded as JSON.
	 * @return The JSON-encoded representation of the given object.
	 * @throws IllegalArgumentException When the given object or one of its properties cannot be inspected as a bean.
	 */
	public static String encode(Object object) {
		StringBuilder builder = new StringBuilder();
		encode(object, builder);
		return builder.toString();
	}

	/**
	 * Method allowing tail recursion (prevents potential stack overflow on deeply nested structures).
	 */
	private static void encode(Object object, StringBuilder builder) {
		if (object == null) {
			builder.append("null");
		}
		else if (object instanceof Boolean || object instanceof Number) {
			builder.append(object.toString());
		}
		else if (object instanceof CharSequence) {
			builder.append('"').append(Utils.escapeJS(object.toString(), false)).append('"');
		}
		else if (object instanceof Date) {
			builder.append('"').append(Utils.formatRFC1123((Date) object)).append('"');
		}
		else if (object instanceof Collection<?>) {
			encodeCollection((Collection<?>) object, builder);
		}
		else if (object.getClass().isArray()) {
			encodeArray(object, builder);
		}
		else if (object instanceof Map<?, ?>) {
			encodeMap((Map<?, ?>) object, builder);
		}
		else if (object instanceof Class<?>) {
			encode(((Class<?>) object).getName(), builder);
		}
		else {
			encodeBean(object, builder);
		}
	}

	/**
	 * Encode a Java collection as JS array.
	 */
	private static void encodeCollection(Collection<?> collection, StringBuilder builder) {
		builder.append('[');
		int i = 0;

		for (Object element : collection) {
			if (i++ > 0) {
				builder.append(',');
			}

			encode(element, builder);
		}

		builder.append(']');
	}

	/**
	 * Encode a Java array as JS array.
	 */
	private static void encodeArray(Object array, StringBuilder builder) {
		builder.append('[');
		int length = Array.getLength(array);

		for (int i = 0; i < length; i++) {
			if (i > 0) {
				builder.append(',');
			}

			encode(Array.get(array, i), builder);
		}

		builder.append(']');
	}

	/**
	 * Encode a Java map as JS object.
	 */
	private static void encodeMap(Map<?, ?> map, StringBuilder builder) {
		builder.append('{');
		int i = 0;

		for (Entry<?, ?> entry : map.entrySet()) {
			if (i++ > 0) {
				builder.append(',');
			}

			encode(String.valueOf(entry.getKey()), builder);
			builder.append(':');
			encode(entry.getValue(), builder);
		}

		builder.append('}');
	}

	/**
	 * Encode a Java bean as JS object.
	 */
	private static void encodeBean(Object bean, StringBuilder builder) {
		BeanInfo beanInfo;

		try {
			beanInfo = Introspector.getBeanInfo(bean.getClass());
		}
		catch (IntrospectionException e) {
			throw new IllegalArgumentException(
				format(ERROR_INVALID_BEAN, bean.getClass()), e);
		}

		builder.append('{');
		int i = 0;

		for (PropertyDescriptor property : beanInfo.getPropertyDescriptors()) {
			if (property.getReadMethod() == null || "class".equals(property.getName())) {
				continue;
			}

			Object value;

			try {
				value = property.getReadMethod().invoke(bean);
			}
			catch (Exception e) {
				throw new IllegalArgumentException(
					format(ERROR_INVALID_GETTER, property.getName(), bean.getClass()), e);
			}

			if (value != null) {
				if (i++ > 0) {
					builder.append(',');
				}

				encode(property.getName(), builder);
				builder.append(':');
				encode(value, builder);
			}
		}

		builder.append('}');
	}

}