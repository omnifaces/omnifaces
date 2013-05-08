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
package org.omnifaces.util;

import static java.util.Collections.emptyList;
import static java.util.regex.Pattern.quote;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
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
	private static final String ERROR_UNSUPPORTED_ENCODING = "UTF-8 is apparently not supported on this machine.";

	// Constructors ---------------------------------------------------------------------------------------------------

	private Utils() {
		// Hide constructor.
	}

	// Lang -----------------------------------------------------------------------------------------------------------

	/**
	 * Returns true if the given string is null or is empty.
	 *
	 * @param string The string to be checked on emptiness.
	 * @return True if the given string is null or is empty.
	 */
	public static boolean isEmpty(String string) {
		return string == null || string.isEmpty();
	}

	/**
	 * Returns true if the given array is null or is empty.
	 *
	 * @param array The array to be checked on emptiness.
	 * @return True if the given array is null or is empty.
	 */
	public static boolean isEmpty(Object[] array) {
		return array == null || array.length == 0;
	}

	/**
	 * Returns true if the given collection is null or is empty.
	 *
	 * @param collection The collection to be checked on emptiness.
	 * @return True if the given collection is null or is empty.
	 */
	public static boolean isEmpty(Collection<?> collection) {
		return collection == null || collection.isEmpty();
	}

	/**
	 * Returns true if the given map is null or is empty.
	 *
	 * @param map The map to be checked on emptiness.
	 * @return True if the given map is null or is empty.
	 */
	public static boolean isEmpty(Map<?, ?> map) {
		return map == null || map.isEmpty();
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

	/**
	 * Returns <code>true</code> if the given string starts with one of the given prefixes.
	 * @param string The object to be checked if it starts with one of the given prefixes.
	 * @param prefixes The argument list of prefixes to be checked
	 * @return <code>true</code> if the given string starts with one of the given prefixes.
	 * @since 1.4
	 */
	public static boolean startsWithOneOf(String string, String... prefixes) {
		for (String prefix : prefixes) {
			if (string.startsWith(prefix)) {
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
	
	/**
	 * Converts an iterable into a list.
	 * <p>
	 * This method makes NO guarantee to whether changes to the source iterable are
	 * reflected in the returned list or not. For instance if the given iterable
	 * already is a list, it's returned directly.
	 * 
	 * @param iterable The iterable to be converted.
	 * @return The list representation of the given iterable, possibly the same instance as that iterable.
	 * @since 1.5
	 */
	public static <T> List<T> iterableToList(Iterable<T> iterable) {
		
		List<T> list = null;
		
		if (iterable instanceof List) {
			list = (List<T>) iterable;
		} else if (iterable instanceof Collection) {
			list = new ArrayList<T>((Collection<T>) iterable);
		} else {
			list = new ArrayList<T>();
			Iterator<T> iterator = iterable.iterator();
			while (iterator.hasNext()) {
				list.add(iterator.next());
			}
		}
		
		return list;
	}

	/**
	 * Converts comma separated values in a string into a list with those values.
	 * <p>
	 * E.g. a string with "foo, bar, kaz" will be converted into a <code>List</code>
	 * with values:
	 * <ul>
	 * <li>"foo"</li>
	 * <li>"bar"</li>
	 * <li>"kaz"</li>
	 * </ul>
	 *
	 * Note that whitespace will be stripped. Empty entries are not supported. This method defaults to
	 * using a comma (<code>","</code>) as delimiter. See {@link Utils#csvToList(String, String)} for when
	 * a different delimiter is needed.
	 *
	 * @param values string with comma separated values
	 * @return a list with all values encountered in the <code>values</code> argument, can be the empty list.
	 * @since 1.4
	 */
	public static List<String> csvToList(String values) {
		return csvToList(values, ",");
	}

	/**
	 * Converts comma separated values in a string into a list with those values.
	 * <p>
	 * E.g. a string with "foo, bar, kaz" will be converted into a <code>List</code>
	 * with values:
	 * <ul>
	 * <li>"foo"</li>
	 * <li>"bar"</li>
	 * <li>"kaz"</li>
	 * </ul>
	 *
	 * Note that whitespace will be stripped. Empty entries are not supported.
	 *
	 * @param values string with comma separated values
	 * @param delimiter the delimiter used to separate the actual values in the <code>values</code> parameter.
	 * @return a list with all values encountered in the <code>values</code> argument, can be the empty list.
	 * @since 1.4
	 */
	public static List<String> csvToList(String values, String delimiter) {

		if (isEmpty(values)) {
			return emptyList();
		}

		List<String> list = new ArrayList<String>();

		for (String value : values.split(quote(delimiter))) {
			String trimmedValue = value.trim();
			if (!isEmpty(trimmedValue)) {
				list.add(trimmedValue);
			}
		}

		return list;
	}

	/**
	 * Returns a new map that contains the reverse of the given map.
	 * <p>
	 * The reverse of a map means that every value X becomes a key X' with as corresponding
	 * value Y' the key Y that was originally associated with the value X.
	 *
	 * @param source the map that is to be reversed
	 * @return the reverse of the given map
	 */
	public static <T> Map<T, T> reverse(Map<T, T> source) {
		Map<T, T> target = new HashMap<T, T>();
		for (Entry<T, T> entry : source.entrySet()) {
			target.put(entry.getValue(), entry.getKey());
		}

		return target;
	}

	// Dates ----------------------------------------------------------------------------------------------------------

	/**
	 * Formats the given {@link Date} to a string in RFC1123 format. This format is used in HTTP headers and in
	 * JavaScript <code>Date</code> constructor.
	 * @param date The <code>Date</code> to be formatted to a string in RFC1123 format.
	 * @return The formatted string.
	 * @since 1.2
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
	 * @since 1.2
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

	/**
	 * URL-encode the given string using UTF-8.
	 * @param string The string to be URL-encoded using UTF-8.
	 * @return The given string, URL-encoded using UTF-8, or <code>null</code> if <code>null</code> was given.
	 * @throws UnsupportedOperationException When UTF-8 is not supported.
	 * @since 1.4
	 */
	public static String encodeURL(String string) {
		if (string == null) {
			return null;
		}

		try {
			return URLEncoder.encode(string, "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			throw new UnsupportedOperationException(ERROR_UNSUPPORTED_ENCODING, e);
		}
	}

	/**
	 * URL-decode the given string using UTF-8.
	 * @param string The string to be URL-decode using UTF-8.
	 * @return The given string, URL-decode using UTF-8, or <code>null</code> if <code>null</code> was given.
	 * @throws UnsupportedOperationException When UTF-8 is not supported.
	 * @since 1.4
	 */
	public static String decodeURL(String string) {
		if (string == null) {
			return null;
		}

		try {
			return URLDecoder.decode(string, "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			throw new UnsupportedOperationException(ERROR_UNSUPPORTED_ENCODING, e);
		}
	}

}