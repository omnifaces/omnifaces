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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableMap;
import static java.util.regex.Pattern.quote;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
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
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import javax.xml.bind.DatatypeConverter;

/**
 * <p>
 * Collection of general utility methods that do not fit in one of the more specific classes.
 *
 * <h3>This class is not listed in showcase! Should I use it?</h3>
 * <p>
 * This class is indeed intented for internal usage only. We won't add methods here on user request. We only add methods
 * here once we encounter non-DRY code in OmniFaces codebase. The methods may be renamed/changed without notice.
 * <p>
 * We don't stop you from using it if you think you find it useful, but you'd really better pick e.g. Google Guava or
 * perhaps the good 'ol Apache Commons. This Utils class exists because OmniFaces intends to be free of 3rd party
 * dependencies.
 *
 * @author Arjan Tijms
 * @author Bauke Scholtz
 */
public final class Utils {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final int DEFAULT_STREAM_BUFFER_SIZE = 10240;
	private static final String PATTERN_RFC1123_DATE = "EEE, dd MMM yyyy HH:mm:ss zzz";
	private static final TimeZone TIMEZONE_GMT = TimeZone.getTimeZone("GMT");
	private static final Pattern PATTERN_ISO639_ISO3166_LOCALE = Pattern.compile("[a-z]{2,3}(_[A-Z]{2})?");
	private static final int BASE64_SEGMENT_LENGTH = 4;
	private static final int UNICODE_3_BYTES = 0xfff;
	private static final int UNICODE_2_BYTES = 0xff;
	private static final int UNICODE_1_BYTE = 0xf;
	private static final int UNICODE_END_PRINTABLE_ASCII = 0x7f;
	private static final int UNICODE_BEGIN_PRINTABLE_ASCII = 0x20;
	private static final Map<Class<?>, Object> PRIMITIVE_DEFAULTS = createPrimitiveDefaults();
	private static Map<Class<?>, Object> createPrimitiveDefaults() {
		Map<Class<?>, Object> primitiveDefaults = new HashMap<>();
		primitiveDefaults.put(boolean.class, false);
		primitiveDefaults.put(byte.class, (byte) 0);
		primitiveDefaults.put(short.class, (short) 0);
		primitiveDefaults.put(char.class, (char) 0);
		primitiveDefaults.put(int.class, 0);
		primitiveDefaults.put(long.class, (long) 0);
		primitiveDefaults.put(float.class, (float) 0);
		primitiveDefaults.put(double.class, (double) 0);
		return unmodifiableMap(primitiveDefaults);
	}

	private static final String ERROR_UNSUPPORTED_ENCODING = "UTF-8 is apparently not supported on this platform.";

	// Constructors ---------------------------------------------------------------------------------------------------

	private Utils() {
		// Hide constructor.
	}

	// Lang -----------------------------------------------------------------------------------------------------------

	/**
	 * Returns <code>true</code> if the given string is null or is empty.
	 * @param string The string to be checked on emptiness.
	 * @return <code>true</code> if the given string is null or is empty.
	 */
	public static boolean isEmpty(String string) {
		return string == null || string.isEmpty();
	}

	/**
	 * Returns <code>true</code> if the given collection is null or is empty.
	 * @param collection The collection to be checked on emptiness.
	 * @return <code>true</code> if the given collection is null or is empty.
	 */
	public static boolean isEmpty(Collection<?> collection) {
		return collection == null || collection.isEmpty();
	}

	/**
	 * Returns <code>true</code> if the given map is null or is empty.
	 * @param map The map to be checked on emptiness.
	 * @return <code>true</code> if the given map is null or is empty.
	 */
	public static boolean isEmpty(Map<?, ?> map) {
		return map == null || map.isEmpty();
	}

	/**
	 * Returns <code>true</code> if the given object is null or an empty array or has an empty toString() result.
	 * @param value The value to be checked on emptiness.
	 * @return <code>true</code> if the given object is null or an empty array or has an empty toString() result.
	 */
	public static boolean isEmpty(Object value) {
		if (value == null) {
			return true;
		}
		else if (value.getClass().isArray()) {
			return Array.getLength(value) == 0;
		}
		else {
			return value.toString() == null || value.toString().isEmpty();
		}
	}

	/**
	 * Returns <code>true</code> if at least one value is empty.
	 * @param values the values to be checked on emptiness
	 * @return <code>true</code> if any value is empty and <code>false</code> if no values are empty
	 * @since 1.8
	 */
	public static boolean isAnyEmpty(Object... values) {
		for (Object value : values) {
			if (isEmpty(value)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns <code>true</code> if the given string is null or is empty or contains whitespace only. In addition to
	 * {@link #isEmpty(String)}, this thus also returns <code>true</code> when <code>string.trim().isEmpty()</code>
	 * returns <code>true</code>.
	 * @param string The string to be checked on blankness.
	 * @return True if the given string is null or is empty or contains whitespace only.
	 * @since 1.5
	 */
	public static boolean isBlank(String string) {
		return isEmpty(string) || string.trim().isEmpty();
	}

	/**
	 * Returns <code>true</code> if the given string is parseable as a number. I.e. it is not null, nor blank and contains solely
	 * digits. I.e., it won't throw a <code>NumberFormatException</code> when parsing as <code>Long</code>.
	 * @param string The string to be checked as number.
	 * @return <code>true</code> if the given string is parseable as a number.
	 * @since 1.5.
	 */
	public static boolean isNumber(String string) {
		try {
			// Performance tests taught that this approach is in general faster than regex or char-by-char checking.
			Long.parseLong(string);
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	/**
	 * Returns <code>true</code> if the given string is parseable as a decimal. I.e. it is not null, nor blank and contains solely
	 * digits. I.e., it won't throw a <code>NumberFormatException</code> when parsing as <code>Double</code>.
	 * @param string The string to be checked as decimal.
	 * @return <code>true</code> if the given string is parseable as a decimal.
	 * @since 1.5.
	 */
	public static boolean isDecimal(String string) {
		try {
			// Performance tests taught that this approach is in general faster than regex or char-by-char checking.
			Double.parseDouble(string);
			return true;
		}
		catch (Exception e) {
			return false;
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
	@SafeVarargs
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
	@SafeVarargs
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

	/**
	 * Returns <code>true</code> if an instance of the given class could also be an instance of one of the given classes.
	 * @param cls The class to be checked if it could also be an instance of one of the given classes.
	 * @param classes The argument list of classes to be tested.
	 * @return <code>true</code> if the given class could also be an instance of one of the given classes.
	 * @since 2.0
	 */
	public static boolean isOneInstanceOf(Class<?> cls, Class<?>... classes) {
		for (Class<?> other : classes) {
			if (cls == null ? other == null : other.isAssignableFrom(cls)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns <code>true</code> if the given class has at least one of the given annotations.
	 * @param cls The class to be checked if it has at least one of the given annotations.
	 * @param annotations The argument list of annotations to be tested on the given class.
	 * @return <code>true</code> if the given clazz would be an instance of one of the given clazzes.
	 * @since 2.0
	 */
	@SafeVarargs
	public static boolean isOneAnnotationPresent(Class<?> cls, Class<? extends Annotation>... annotations) {
		for (Class<? extends Annotation> annotation : annotations) {
			if (cls.isAnnotationPresent(annotation)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns the default value of the given class, covering primitives.
	 * E.g. if given class is <code>int.class</code>, then it will return <code>0</code>. Autoboxing will do the rest.
	 * Non-primitives and <code>void.class</code> will return <code>null</code>.
	 * @param cls The class to obtain the default value for.
	 * @return The default value of the given class, covering primitives.
	 * @since 2.4
	 */
	public static Object getDefaultValue(Class<?> cls) {
		return cls.isPrimitive() ? PRIMITIVE_DEFAULTS.get(cls) : null;
	}

	// I/O ------------------------------------------------------------------------------------------------------------

	/**
	 * Stream the given input to the given output via NIO {@link Channels} and a directly allocated NIO
	 * {@link ByteBuffer}. Both the input and output streams will implicitly be closed after streaming,
	 * regardless of whether an exception is been thrown or not.
	 * @param input The input stream.
	 * @param output The output stream.
	 * @return The length of the written bytes.
	 * @throws IOException When an I/O error occurs.
	 */
	public static long stream(InputStream input, OutputStream output) throws IOException {
		try (ReadableByteChannel inputChannel = Channels.newChannel(input);
			WritableByteChannel outputChannel = Channels.newChannel(output))
		{
			ByteBuffer buffer = ByteBuffer.allocateDirect(DEFAULT_STREAM_BUFFER_SIZE);
			long size = 0;

			while (inputChannel.read(buffer) != -1) {
				buffer.flip();
				size += outputChannel.write(buffer);
				buffer.clear();
			}

			return size;
		}
	}

	/**
	 * Stream a specified range of the given file to the given output via NIO {@link Channels} and a directly allocated
	 * NIO {@link ByteBuffer}. The output stream will only implicitly be closed after streaming when the specified range
	 * represents the whole file, regardless of whether an exception is been thrown or not.
	 * @param file The file.
	 * @param output The output stream.
	 * @param start The start position (offset).
	 * @param length The (intented) length of written bytes.
	 * @return The (actual) length of the written bytes. This may be smaller when the given length is too large.
	 * @throws IOException When an I/O error occurs.
	 * @since 2.2
	 */
	public static long stream(File file, OutputStream output, long start, long length) throws IOException {
		if (start == 0 && length >= file.length()) {
			return stream(new FileInputStream(file), output);
		}

		try (FileChannel fileChannel = (FileChannel) Files.newByteChannel(file.toPath(), StandardOpenOption.READ)) {
			WritableByteChannel outputChannel = Channels.newChannel(output);
			ByteBuffer buffer = ByteBuffer.allocateDirect(DEFAULT_STREAM_BUFFER_SIZE);
			long size = 0;

			while (fileChannel.read(buffer, start + size) != -1) {
				buffer.flip();

				if (size + buffer.limit() > length) {
					buffer.limit((int) (length - size));
				}

				size += outputChannel.write(buffer);

				if (size >= length) {
					break;
				}

				buffer.clear();
			}

			return size;
		}
	}

	/**
	 * Read the given input stream into a byte array. The given input stream will implicitly be closed after streaming,
	 * regardless of whether an exception is been thrown or not.
	 * @param input The input stream.
	 * @return The input stream as a byte array.
	 * @throws IOException When an I/O error occurs.
	 * @since 2.0
	 */
	public static byte[] toByteArray(InputStream input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		stream(input, output);
		return output.toByteArray();
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

	/**
	 * Returns <code>true</code> if the given object is serializable.
	 * @param object The object to be tested.
	 * @return <code>true</code> if the given object is serializable.
	 * @since 2.4
	 */
	public static boolean isSerializable(Object object) {
		try (ObjectOutputStream output = new ObjectOutputStream(new NullOutputStream())) {
			output.writeObject(object);
			return true;
		}
		catch (IOException e) {
			return false;
		}
	}

	private static final class NullOutputStream extends OutputStream {
		@Override
		public void write(int b) throws IOException {
			// NOOP.
		}
		@Override
		public void write(byte[] b) throws IOException {
			// NOOP.
		}
		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			// NOOP.
		}
	}

	// Collections ----------------------------------------------------------------------------------------------------

	/**
	 * Creates an unmodifiable set based on the given values. If one of the values is an instance of an array or a
	 * collection, then each of its values will also be merged into the set. Nested arrays or collections will result
	 * in a {@link ClassCastException}.
	 * @param <E> The expected set element type.
	 * @param values The values to create an unmodifiable set for.
	 * @return An unmodifiable set based on the given values.
	 * @throws ClassCastException When one of the values or one of the arrays or collections is of wrong type.
	 * @since 1.1
	 */
	@SuppressWarnings("unchecked")
	public static <E> Set<E> unmodifiableSet(Object... values) {
		Set<E> set = new HashSet<>();

		for (Object value : values) {
			if (value instanceof Object[]) {
				for (Object item : (Object[]) value) {
					set.add((E) item);
				}
			}
			else if (value instanceof Collection<?>) {
				for (Object item : (Collection<?>) value) {
					set.add((E) item);
				}
			}
			else {
				set.add((E) value);
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
	 * @param <E> The generic iterable element type.
	 * @param iterable The iterable to be converted.
	 * @return The list representation of the given iterable, possibly the same instance as that iterable.
	 * @since 1.5
	 */
	public static <E> List<E> iterableToList(Iterable<E> iterable) {

		List<E> list = null;

		if (iterable instanceof List) {
			list = (List<E>) iterable;
		} else if (iterable instanceof Collection) {
			list = new ArrayList<>((Collection<E>) iterable);
		} else {
			list = new ArrayList<>();
			Iterator<E> iterator = iterable.iterator();
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

		List<String> list = new ArrayList<>();

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
	 * @param <T> The generic map key/value type.
	 * @param source the map that is to be reversed
	 * @return the reverse of the given map
	 */
	public static <T> Map<T, T> reverse(Map<T, T> source) {
		Map<T, T> target = new HashMap<>();
		for (Entry<T, T> entry : source.entrySet()) {
			target.put(entry.getValue(), entry.getKey());
		}

		return target;
	}

	/**
	 * Checks if the given collection contains an object with the given class name.
	 *
	 * @param objects collection of objects to check
	 * @param className name of the class to be checked for
	 * @return true if the collection contains at least one object with the given class name, false otherwise
	 * @since 1.6
	 */
	public static boolean containsByClassName(Collection<?> objects, String className) {
		for (Object object : objects) {
			if (object.getClass().getName().equals(className)) {
				return true;
			}
		}

		return false;
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

	// Locale ---------------------------------------------------------------------------------------------------------

	/**
	 * Parses the given object representing the locale to a {@link Locale} object.
	 * If it is <code>null</code>, then return <code>null</code>.
	 * Else if it is already an instance of <code>Locale</code>, then just return it.
	 * Else if it is in pattern ISO 639 alpha-2/3, optionally followed by "_" and ISO 3166-1 alpha-2 country code, then
	 * split the language/country and construct a new <code>Locale</code> with it.
	 * Else parse it via {@link Locale#forLanguageTag(String)} and return it.
	 * @param locale The object representing the locale.
	 * @return The parsed <code>Locale</code>.
	 * @since 2.3
	 */
	public static Locale parseLocale(Object locale) {
		if (locale == null) {
			return null;
		}
		else if (locale instanceof Locale) {
			return (Locale) locale;
		}
		else {
			String localeString = locale.toString();

			if (PATTERN_ISO639_ISO3166_LOCALE.matcher(localeString).matches()) {
				String[] languageAndCountry = localeString.split("_");
				String language = languageAndCountry[0];
				String country = languageAndCountry.length > 1 ? languageAndCountry[1] : "";
				return new Locale(language, country);
			}
			else {
				return Locale.forLanguageTag(localeString);
			}
		}
	}

	// Encoding/decoding ----------------------------------------------------------------------------------------------

	/**
	 * Serialize the given string to the short possible unique URL-safe representation. The current implementation will
	 * decode the given string with UTF-8 and then compress it with ZLIB using "best compression" algorithm and then
	 * Base64-encode the resulting bytes without the <code>=</code> padding, whereafter the Base64 characters
	 * <code>+</code> and <code>/</code> are been replaced by respectively <code>-</code> and <code>_</code> to make it
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
			InputStream raw = new ByteArrayInputStream(string.getBytes(UTF_8));
			ByteArrayOutputStream deflated = new ByteArrayOutputStream();
			stream(raw, new DeflaterOutputStream(deflated, new Deflater(Deflater.BEST_COMPRESSION)));
			String base64 = DatatypeConverter.printBase64Binary(deflated.toByteArray());
			return base64.replace('+', '-').replace('/', '_').replace("=", "");
		}
		catch (IOException e) {
			// This will occur when ZLIB and/or UTF-8 are not supported, but this is not to be expected these days.
			throw new UnsupportedOperationException(e);
		}
	}

	/**
	 * Unserialize the given serialized URL-safe string. This does the inverse of {@link #serializeURLSafe(String)}.
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
			String base64 = string.replace('-', '+').replace('_', '/') + "===".substring(0, string.length() % BASE64_SEGMENT_LENGTH);
			InputStream deflated = new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(base64));
			return new String(toByteArray(new InflaterInputStream(deflated)), UTF_8);
		}
		catch (UnsupportedEncodingException e) {
			// This will occur when UTF-8 is not supported, but this is not to be expected these days.
			throw new UnsupportedOperationException(e);
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
	 * @throws UnsupportedOperationException When this platform does not support UTF-8.
	 * @since 1.4
	 */
	public static String encodeURL(String string) {
		if (string == null) {
			return null;
		}

		try {
			return URLEncoder.encode(string, UTF_8.name());
		}
		catch (UnsupportedEncodingException e) {
			throw new UnsupportedOperationException(ERROR_UNSUPPORTED_ENCODING, e);
		}
	}

	/**
	 * URL-decode the given string using UTF-8.
	 * @param string The string to be URL-decode using UTF-8.
	 * @return The given string, URL-decode using UTF-8, or <code>null</code> if <code>null</code> was given.
	 * @throws UnsupportedOperationException When this platform does not support UTF-8.
	 * @since 1.4
	 */
	public static String decodeURL(String string) {
		if (string == null) {
			return null;
		}

		try {
			return URLDecoder.decode(string, UTF_8.name());
		}
		catch (UnsupportedEncodingException e) {
			throw new UnsupportedOperationException(ERROR_UNSUPPORTED_ENCODING, e);
		}
	}

	/**
	 * URI-encode the given string using UTF-8. URIs (paths and filenames) have different encoding rules as compared to
	 * URL query string parameters. {@link URLEncoder} is actually only for www (HTML) form based query string parameter
	 * values (as used when a webbrowser submits a HTML form). URI encoding has a lot in common with URL encoding, but
	 * the space has to be %20 and some chars doesn't necessarily need to be encoded.
	 * @param string The string to be URI-encoded using UTF-8.
	 * @return The given string, URI-encoded using UTF-8, or <code>null</code> if <code>null</code> was given.
	 * @throws UnsupportedOperationException When this platform does not support UTF-8.
	 * @since 2.4
	 */
	public static String encodeURI(String string) {
		if (string == null) {
			return null;
		}

		return encodeURL(string)
			.replace("+", "%20")
			.replace("%21", "!")
			.replace("%27", "'")
			.replace("%28", "(")
			.replace("%29", ")")
			.replace("%7E", "~");
	}

	// Escaping/unescaping --------------------------------------------------------------------------------------------

	/**
	 * Escapes the given string according the JavaScript code rules. This escapes among others the special characters,
	 * the whitespace, the quotes and the unicode characters. Useful whenever you want to use a Java string variable as
	 * a JavaScript string variable.
	 * @param string The string to be escaped according the JavaScript code rules.
	 * @param escapeSingleQuote Whether to escape single quotes as well or not. Set to <code>false</code> if you want
	 * to escape it for usage in JSON.
	 * @return The escaped string according the JavaScript code rules.
	 */
	public static String escapeJS(String string, boolean escapeSingleQuote) {
		if (string == null) {
			return null;
		}

		StringBuilder builder = new StringBuilder(string.length());

		for (char c : string.toCharArray()) {
			if (c > UNICODE_3_BYTES) {
				builder.append("\\u").append(Integer.toHexString(c));
			}
			else if (c > UNICODE_2_BYTES) {
				builder.append("\\u0").append(Integer.toHexString(c));
			}
			else if (c > UNICODE_END_PRINTABLE_ASCII) {
				builder.append("\\u00").append(Integer.toHexString(c));
			}
			else if (c < UNICODE_BEGIN_PRINTABLE_ASCII) {
				escapeJSControlCharacter(builder, c);
			}
			else {
				escapeJSASCIICharacter(builder, c, escapeSingleQuote);
			}
		}

		return builder.toString();
	}

	private static void escapeJSControlCharacter(StringBuilder builder, char c) {
		switch (c) {
			case '\b':
				builder.append('\\').append('b');
				break;
			case '\n':
				builder.append('\\').append('n');
				break;
			case '\t':
				builder.append('\\').append('t');
				break;
			case '\f':
				builder.append('\\').append('f');
				break;
			case '\r':
				builder.append('\\').append('r');
				break;
			default:
				if (c > UNICODE_1_BYTE) {
					builder.append("\\u00").append(Integer.toHexString(c));
				}
				else {
					builder.append("\\u000").append(Integer.toHexString(c));
				}

				break;
		}
	}

	private static void escapeJSASCIICharacter(StringBuilder builder, char c, boolean escapeSingleQuote) {
		switch (c) {
			case '\'':
				if (escapeSingleQuote) {
					builder.append('\\');
				}
				builder.append('\'');
				break;
			case '"':
				builder.append('\\').append('"');
				break;
			case '\\':
				builder.append('\\').append('\\');
				break;
			case '/':
				builder.append('\\').append('/');
				break;
			default:
				builder.append(c);
				break;
		}
	}

}