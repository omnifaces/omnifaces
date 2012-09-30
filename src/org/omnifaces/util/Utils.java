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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import javax.xml.bind.DatatypeConverter;

/**
 * Collection of general utility methods that do not fit in one of the more specific classes.
 *
 * @author Arjan Tijms
 * @author Bauke Scholtz
 *
 */
public final class Utils {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final int DEFAULT_STREAM_BUFFER_SIZE = 10240;
	private static final String PATTERN_RFC1123_DATE = "EEE, dd MMM yyyy HH:mm:ss zzz";
	private static final TimeZone TIMEZONE_GMT = TimeZone.getTimeZone("GMT");

	// Constructors ---------------------------------------------------------------------------------------------------

	private Utils() {
		// Hide constructor.
	}

	// Lang -----------------------------------------------------------------------------------------------------------

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
	 * Returns true if the given collection is null or is empty.
	 * @param collection The collection to be checked on emptiness.
	 * @return True if the given collection is null or is empty.
	 */
	public static boolean isEmpty(Collection<?> collection) {
		return collection == null || collection.isEmpty();
	}

	/**
	 * Returns true if the given value is null or is empty. Types of String, Collection, Map and Array are
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
		else if (value instanceof Collection<?>) {
			return ((Collection<?>) value).isEmpty();
		}
		else if (value instanceof Map<?, ?>) {
			return ((Map<?, ?>) value).isEmpty();
		}
		else if (value.getClass().isArray()) {
			return Array.getLength(value) == 0;
		}
		else {
		    return value.toString() == null || value.toString().isEmpty();
		}
	}

	/**
	 * Returns the first non-<code>null</code> object of the argument list, or <code>null</code> if there is no such
	 * element.
	 * @param <T> The generic object type.
	 * @param objects The argument list of objects to be tested for non-<code>null</code>.
	 * @return The first non-<code>null</code> object of the argument list, or <code>null</code> if there is no such
	 * element.
	 */
	public static <T> T coalesce(T... objects) {
		for (T object : objects) {
			if (object != null) {
				return object;
			}
		}

		return null;
	}

	/**
	 * Returns <code>true</code> if the given object equals one of the given objects.
	 * @param <T> The generic object type.
	 * @param object The object to be checked if it equals one of the given objects.
	 * @param objects The argument list of objects to be tested for equality.
	 * @return <code>true</code> if the given object equals one of the given objects.
	 */
	public static <T> boolean isOneOf(T object, T... objects) {
		for (Object other : objects) {
			if (object == null ? other == null : object.equals(other)) {
				return true;
			}
		}

		return false;
	}

	// I/O ------------------------------------------------------------------------------------------------------------

	/**
	 * Stream the given input to the given output by NIO {@link ByteBuffer}. Both the input and output streams will
	 * implicitly be closed after streaming, regardless of whether an exception is been thrown or not.
	 * @param input The input stream.
	 * @param output The output stream.
	 * @return The length of the written bytes.
	 * @throws IOException When an I/O error occurs.
	 */
	public static long stream(InputStream input, OutputStream output) throws IOException {
		ReadableByteChannel inputChannel = null;
		WritableByteChannel outputChannel = null;

		try {
			inputChannel = Channels.newChannel(input);
			outputChannel = Channels.newChannel(output);
			ByteBuffer buffer = ByteBuffer.allocateDirect(DEFAULT_STREAM_BUFFER_SIZE);
			long size = 0;

			while (inputChannel.read(buffer) != -1) {
				buffer.flip();
				size += outputChannel.write(buffer);
				buffer.clear();
			}

			return size;
		}
		finally {
			close(outputChannel);
			close(inputChannel);
		}
	}

	/**
	 * Check if the given resource is not <code>null</code> and then close it, whereby any caught {@link IOException}
	 * is been returned instead of thrown, so that the caller can if necessary handle (log) or just ignore it without
	 * the need to put another try-catch.
	 * @param resource The closeable resource to be closed.
	 * @return The caught {@link IOException}, or <code>null</code> if none is been thrown.
	 */
	public static IOException close(Closeable resource) {
		if (resource != null) {
			try {
				resource.close();
			}
			catch (IOException e) {
				return e;
			}
		}

		return null;
	}

	// Collections ----------------------------------------------------------------------------------------------------

	/**
	 * Creates an unmodifiable set based on the given values. If one of the values is an instance of an array or a
	 * collection, then each of its values will also be merged into the set. Nested arrays or collections will result
	 * in a {@link ClassCastException}.
	 * @param values The values to create an unmodifiable set for.
	 * @return An unmodifiable set based on the given values.
	 * @throws ClassCastException When one of the values or one of the arrays or collections is of wrong type.
	 * @since 1.1
	 */
	@SuppressWarnings("unchecked")
	public static <T> Set<T> unmodifiableSet(Object... values) {
		Set<T> set = new HashSet<T>();

		for (Object value : values) {
			if (value instanceof Object[]) {
				for (Object item : (Object[]) value) {
					set.add((T) item);
				}
			}
			else if (value instanceof Collection<?>) {
				for (Object item : (Collection<?>) value) {
					set.add((T) item);
				}
			}
			else {
				set.add((T) value);
			}
		}

		return Collections.unmodifiableSet(set);
	}

	// Dates ----------------------------------------------------------------------------------------------------------

	/**
	 * Formats the given {@link Date} to a string in RFC1123 format. This format is used in HTTP headers and in
	 * JavaScript <code>Date</code> constructor.
	 * @param date The <code>Date</code> to be formatted to a string in RFC1123 format.
	 * @return The formatted string.
	 */
	public static String formatRFC1123(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat(PATTERN_RFC1123_DATE, Locale.US);
		sdf.setTimeZone(TIMEZONE_GMT);
		return sdf.format(date);
	}

	/**
	 * Parses the given string in RFC1123 format to a {@link Date} object.
	 * @param string The string in RFC1123 format to be parsed to a <code>Date</code> object.
	 * @return The parsed <code>Date</code>.
	 * @throws ParseException When the given string is not in RFC1123 format.
	 */
	public static Date parseRFC1123(String string) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat(PATTERN_RFC1123_DATE, Locale.US);
		return sdf.parse(string);
	}

	// Encoding/decoding ----------------------------------------------------------------------------------------------

	/**
	 * Serialize the given string to the short possible unique URL-safe representation. The current implementation will
	 * decode the given string with UTF-8 and then compress it with ZLIB using "best compression" algorithm and then
	 * Base64-encode the resulting bytes whereafter the Base64 characters <code>/</code>, <code>+</code> and
	 * <code>=</code> are been replaced by respectively <code>~</code>, <code>-</code> and <code>_</code> to make it
	 * URL-safe (so that no platform-sensitive URL-encoding needs to be done when used in URLs).
	 * @param string The string to be serialized.
	 * @return The serialized URL-safe string, or <code>null</code> when the given string is itself <code>null</code>.
	 * @since 1.2
	 */
	public static String serializeURLSafe(String string) {
		if (string == null) {
			return null;
		}

		try {
			InputStream raw = new ByteArrayInputStream(string.getBytes("UTF-8"));
			ByteArrayOutputStream deflated = new ByteArrayOutputStream();
			stream(raw, new DeflaterOutputStream(deflated, new Deflater(Deflater.BEST_COMPRESSION)));
			String base64 = DatatypeConverter.printBase64Binary(deflated.toByteArray());
			return base64.replace('/', '~').replace('+', '-').replace('=', '_');
		}
		catch (IOException e) {
			// This will occur when ZLIB and/or UTF-8 are not supported, but this is not to be expected these days.
			throw new RuntimeException(e);
		}
	}

	/**
	 * Unserialize the given serialized URL-safe string. This does the reverse of {@link #serializeURLSafe(String)}.
	 * @param string The serialized URL-safe string to be unserialized.
	 * @return The unserialized string, or <code>null</code> when the given string is by itself <code>null</code>.
	 * @throws IllegalArgumentException When the given serialized URL-safe string is not in valid format as returned by
	 * {@link #serializeURLSafe(String)}.
	 * @since 1.2
	 */
	public static String unserializeURLSafe(String string) {
		if (string == null) {
			return null;
		}

		try {
			String base64 = string.replace('~', '/').replace('-', '+').replace('_', '=');
			InputStream deflated = new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(base64));
			ByteArrayOutputStream raw = new ByteArrayOutputStream();
			stream(new InflaterInputStream(deflated), raw);
			return new String(raw.toByteArray(), "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			// This will occur when UTF-8 is not supported, but this is not to be expected these days.
			throw new RuntimeException(e);
		}
		catch (Exception e) {
			// This will occur when the string is not in valid Base64 or ZLIB format.
			throw new IllegalArgumentException(e);
		}
	}

}