/*
 * Copyright 2020 OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.util;

import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;

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
	 * {@link CharSequence}, {@link Date} and since OmniFaces 3.6 also {@link Enum} and {@link Temporal}. If the given
	 * object type does not match any of them, then it will attempt to inspect the object as a javabean whereby the
	 * public properties (with public getters) will be encoded as a JS object. It also supports {@link Collection}s,
	 * {@link Map}s and arrays of them, even nested ones. The {@link Date} and {@link Temporal} are formatted in
	 * RFC 1123 format, so you can if necessary just pass it straight to <code>new Date()</code> in JavaScript.
	 * @param object The object to be encoded as JSON.
	 * @return The JSON-encoded representation of the given object.
	 * @throws IllegalArgumentException When the given object or one of its properties cannot be inspected as a bean.
	 */
	public static String encode(Object object) {
		return encode(object, null);
	}

	/**
	 * Does the same as {@link #encode(Object)} but then with a custom property name formatter.
	 * @param object The object to be encoded as JSON.
	 * @param propertyNameFormatter The property name formatter. When this is null, then the property names are not
	 * adjusted.
	 * @return The JSON-encoded representation of the given object.
	 * @throws IllegalArgumentException When the given object or one of its properties cannot be inspected as a bean.
	 */
	public static String encode(Object object, UnaryOperator<String> propertyNameFormatter) {
		StringBuilder builder = new StringBuilder();
		encode(object, builder, propertyNameFormatter);
		return builder.toString();
	}

	/**
	 * Method allowing tail recursion (prevents potential stack overflow on deeply nested structures).
	 */
	private static void encode(Object object, StringBuilder builder, UnaryOperator<String> propertyNameFormatter) {
		if (object == null) {
			builder.append("null");
		}
		else if (object instanceof Boolean || object instanceof Number) {
			builder.append(object.toString());
		}
		else if (object instanceof CharSequence || object instanceof Enum<?>) {
			encodeString(object.toString(), builder);
		}
		else if (object instanceof Date) {
			builder.append('"').append(Utils.formatRFC1123((Date) object)).append('"');
		}
		else if (object instanceof Temporal) {
			builder.append('"').append(Utils.toZonedDateTime(object).format(RFC_1123_DATE_TIME)).append('"');
		}
		else if (object instanceof Collection<?>) {
			encodeCollection((Collection<?>) object, builder, propertyNameFormatter);
		}
		else if (object.getClass().isArray()) {
			encodeArray(object, builder, propertyNameFormatter);
		}
		else if (object instanceof Map<?, ?>) {
			encodeMap((Map<?, ?>) object, builder, propertyNameFormatter);
		}
		else if (object instanceof Class<?>) {
			encodeString(((Class<?>) object).getName(), builder);
		}
		else {
			encodeBean(object, builder, propertyNameFormatter);
		}
	}

	/**
	 * Encode a Java string as JS string.
	 */
	private static void encodeString(String string, StringBuilder builder) {
		builder.append('"').append(Utils.escapeJS(string, false)).append('"');
	}

	/**
	 * Encode a Java collection as JS array.
	 */
	private static void encodeCollection(Collection<?> collection, StringBuilder builder, UnaryOperator<String> propertyNameFormatter) {
		builder.append('[');
		int i = 0;

		for (Object element : collection) {
			if (i++ > 0) {
				builder.append(',');
			}

			encode(element, builder, propertyNameFormatter);
		}

		builder.append(']');
	}

	/**
	 * Encode a Java array as JS array.
	 */
	private static void encodeArray(Object array, StringBuilder builder, UnaryOperator<String> propertyNameFormatter) {
		builder.append('[');
		int length = Array.getLength(array);

		for (int i = 0; i < length; i++) {
			if (i > 0) {
				builder.append(',');
			}

			encode(Array.get(array, i), builder, propertyNameFormatter);
		}

		builder.append(']');
	}

	/**
	 * Encode a Java map as JS object.
	 */
	private static void encodeMap(Map<?, ?> map, StringBuilder builder, UnaryOperator<String> propertyNameFormatter) {
		builder.append('{');
		int i = 0;

		for (Entry<?, ?> entry : map.entrySet()) {
			if (i++ > 0) {
				builder.append(',');
			}

			encodePropertyName(String.valueOf(entry.getKey()), builder, propertyNameFormatter);
			builder.append(':');
			encode(entry.getValue(), builder, propertyNameFormatter);
		}

		builder.append('}');
	}

	/**
	 * Encode a Java bean as JS object.
	 */
	private static void encodeBean(Object bean, StringBuilder builder, UnaryOperator<String> propertyNameFormatter) {
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

				encodePropertyName(property.getName(), builder, propertyNameFormatter);
				builder.append(':');
				encode(value, builder, propertyNameFormatter);
			}
		}

		builder.append('}');
	}

	/**
	 * Encode a Java String as JS object property name.
	 */
	private static void encodePropertyName(String string, StringBuilder builder, UnaryOperator<String> propertyNameFormatter)
	{
		encodeString(propertyNameFormatter == null ? string : propertyNameFormatter.apply(string), builder);
	}

}