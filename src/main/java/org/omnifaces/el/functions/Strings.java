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
package org.omnifaces.el.functions;

import static org.omnifaces.util.Faces.getLocale;
import static org.omnifaces.util.Utils.isEmpty;

import java.text.MessageFormat;
import java.text.Normalizer;
import java.text.Normalizer.Form;

import org.omnifaces.util.Faces;
import org.omnifaces.util.Utils;

/**
 * <p>
 * Collection of EL functions for string manipulation: <code>of:abbreviate()</code>, <code>of:capitalize()</code>, <code>of:concat()</code>,
 * <code>of:prettyURL()</code>, <code>of:encodeURL()</code>, <code>of:escapeJS()</code> and <code>of:formatX()</code>.
 * <p>
 * Instead of <code>of:formatX()</code>, you can also use <code>&lt;o:outputFormat&gt;</code>.
 *
 * @author Bauke Scholtz
 */
public final class Strings {

	// Constructors ---------------------------------------------------------------------------------------------------

	private Strings() {
		// Hide constructor.
	}

	// Utility --------------------------------------------------------------------------------------------------------

	/**
	 * Abbreviate the given text on the given size limit with ellipsis.
	 * @param text The text to be abbreviated.
	 * @param size The size limit of the text.
	 * @return The abbreviated text, or the unmodified text if it is shorter than the size.
	 */
	public static String abbreviate(String text, int size) {
		if (text == null) {
			return null;
		}

		if (text.length() > size) {
			return text.substring(0, size).trim() + "...";
		}

		return text;
	}

	/**
	 * Concatenate the string representation of the given objects. This is useful when you don't know beforehand if one
	 * of the both hands is a string or is <code>null</code>, otherwise the new EL 2.2
	 * <code>#{bean.string1.concat(bean.string2)}</code> can just be used.
	 * @param left The left hand.
	 * @param right The right hand.
	 * @return The concatenated strings.
	 */
	public static String concat(Object left, Object right) {
		if (left == null && right == null) {
			return null;
		}

		if (left == null) {
			return right.toString();
		}

		if (right == null) {
			return left.toString();
		}

		return left.toString() + right.toString();
	}

	/**
	 * Capitalize the given string, i.e. uppercase the first character.
	 * @param string The string to be capitalized.
	 * @return The capitalized string.
	 * @since 1.1
	 */
	public static String capitalize(String string) {
		if (string == null || string.isEmpty()) {
			return string;
		}

		return new StringBuilder()
			.append(Character.toTitleCase(string.charAt(0)))
			.append(string.substring(1))
			.toString();
	}

	/**
	 * Parenthesize the given object. This will only wrap the given object in parenthesis when it's not empty or zero.
	 * @param object The object to be parenthesized.
	 * @return The parenthesized object.
	 * @since 3.0
	 */
	public static String parenthesize(Object object) {
		if (isEmpty(object) || "0".equals(object.toString())) {
			return null;
		}

		return format("({0})", object);
	}

	/**
	 * Replace all matches of the given pattern on the given string with the given replacement.
	 * @param value The string to be replaced.
	 * @param pattern The regular expression pattern to be tested.
	 * @param replacement The string to be substituted for each match.
	 * @return True if the given string matches the given pattern.
	 * @see String#replaceAll(String, String)
	 * @since 1.5
	 */
	public static String replaceAll(String value, String pattern, String replacement) {
		if (value == null) {
			return null;
		}

		return value.replaceAll(pattern, replacement);
	}

	/**
	 * Returns true if the given string matches the given pattern.
	 * @param value The string to be matched.
	 * @param pattern The regular expression pattern to be tested.
	 * @return True if the given string matches the given pattern.
	 * @see String#matches(String)
	 * @since 1.5
	 */
	public static boolean matches(String value, String pattern) {
		return value != null && value.matches(pattern);
	}

	/**
	 * URL-prettify the given string. It performs the following tasks:
	 * <ul>
	 * <li>Lowercase the string.
	 * <li>Remove combining diacritical marks.
	 * <li>Replace non-alphanumeric characters by hyphens.
	 * </ul>
	 * This is useful when populating links with dynamic paths obtained from user controlled variables, such as blog
	 * titles.
	 * @param string The string to be prettified.
	 * @return The prettified string.
	 */
	public static String prettyURL(String string) {
		if (string == null) {
			return null;
		}

		return Normalizer.normalize(string.toLowerCase(), Form.NFD)
			.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
			.replaceAll("[^\\p{Alnum}]+", "-");
	}

	/**
	 * URL-encode the given string using UTF-8. This is useful for cases where you can't use
	 * <code>&lt;f:param&gt;</code>.
	 * @param string The string to be URL-encoded.
	 * @return The URL-encoded string.
	 * @throws UnsupportedOperationException When this platform does not support UTF-8.
	 */
	public static String encodeURL(String string) {
		return Utils.encodeURL(string);
	}

	/**
	 * URI-encode the given string using UTF-8. This is useful for cases where you need to embed path parameters in URLs.
	 * @param string The string to be URI-encoded.
	 * @return The URI-encoded string.
	 * @throws UnsupportedOperationException When this platform does not support UTF-8.
	 * @since 3.5
	 */
	public static String encodeURI(String string) {
		return Utils.encodeURI(string);
	}

	/**
	 * Escapes the given string according the JavaScript code rules. This escapes among others the special characters,
	 * the whitespace, the quotes and the unicode characters. Useful whenever you want to use a Java string variable as
	 * a JavaScript string variable.
	 * @param string The string to be escaped according the JavaScript code rules.
	 * @return The escaped string according the JavaScript code rules.
	 */
	public static String escapeJS(String string) {
		return Utils.escapeJS(string, true);
	}

	/**
	 * Format the given string with 1 parameter using {@link MessageFormat} API. The locale is obtained by
	 * {@link Faces#getLocale()}. Design notice: There are five formatX() methods, each taking 1 to 5 format parameters
	 * because EL functions do not support varargs methods nor overloaded function names.
	 * @param pattern The format pattern.
	 * @param param1 The first parameter.
	 * @return The formatted string.
	 * @see MessageFormat
	 */
	public static String format1(String pattern, Object param1) {
		return format(pattern, param1);
	}

	/**
	 * Format the given string with 2 parameters using {@link MessageFormat} API. The locale is obtained by
	 * {@link Faces#getLocale()}. Design notice: There are five formatX() methods, each taking 1 to 5 format parameters
	 * because EL functions do not support varargs methods nor overloaded function names.
	 * @param pattern The format pattern.
	 * @param param1 The first parameter.
	 * @param param2 The second parameter.
	 * @return The formatted string.
	 * @see #format1(String, Object)
	 */
	public static String format2(String pattern, Object param1, Object param2) {
		return format(pattern, param1, param2);
	}

	/**
	 * Format the given string with 3 parameters using {@link MessageFormat} API. The locale is obtained by
	 * {@link Faces#getLocale()}. Design notice: There are five formatX() methods, each taking 1 to 5 format parameters
	 * because EL functions do not support varargs methods nor overloaded function names.
	 * @param pattern The format pattern.
	 * @param param1 The first parameter.
	 * @param param2 The second parameter.
	 * @param param3 The third parameter.
	 * @return The formatted string.
	 * @see #format1(String, Object)
	 */
	public static String format3(String pattern, Object param1, Object param2, Object param3) {
		return format(pattern, param1, param2, param3);
	}

	/**
	 * Format the given string with 4 parameters using {@link MessageFormat} API. The locale is obtained by
	 * {@link Faces#getLocale()}. Design notice: There are five formatX() methods, each taking 1 to 5 format parameters
	 * because EL functions do not support varargs methods nor overloaded function names.
	 * @param pattern The format pattern.
	 * @param param1 The first parameter.
	 * @param param2 The second parameter.
	 * @param param3 The third parameter.
	 * @param param4 The fourth parameter.
	 * @return The formatted string.
	 * @see #format1(String, Object)
	 */
	public static String format4(String pattern, Object param1, Object param2, Object param3, Object param4) {
		return format(pattern, param1, param2, param3, param4);
	}

	/**
	 * Format the given string with 5 parameters using {@link MessageFormat} API. The locale is obtained by
	 * {@link Faces#getLocale()}. Design notice: There are five formatX() methods, each taking 1 to 5 format parameters
	 * because EL functions do not support varargs methods nor overloaded function names.
	 * @param pattern The format pattern.
	 * @param param1 The first parameter.
	 * @param param2 The second parameter.
	 * @param param3 The third parameter.
	 * @param param4 The fourth parameter.
	 * @param param5 The fifth parameter.
	 * @return The formatted string.
	 * @see #format1(String, Object)
	 */
	public static String format5
		(String pattern, Object param1, Object param2, Object param3, Object param4, Object param5)
	{
		return format(pattern, param1, param2, param3, param4, param5);
	}

	/**
	 * The main string format method taking varargs.
	 */
	private static String format(String pattern, Object... params) {
		StringBuffer result = new StringBuffer();
		new MessageFormat(pattern, getLocale()).format(params, result, null);
		return result.toString();
	}

}