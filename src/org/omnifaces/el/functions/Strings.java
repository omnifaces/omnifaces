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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.text.Normalizer;
import java.text.Normalizer.Form;

import org.omnifaces.util.Faces;

/**
 * Collection of EL functions for string manipulation.
 *
 * @author Bauke Scholtz
 */
public final class Strings {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String ERROR_UNSUPPORTED_ENCODING = "UTF-8 is apparently not supported on this machine.";

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
			text = text.substring(0, size).trim() + "...";
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
		if (string == null) {
			return null;
		}

		try {
			return URLEncoder.encode(string, "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			throw new UnsupportedOperationException(ERROR_UNSUPPORTED_ENCODING);
		}
	}

	/**
	 * Escapes the given string according the JavaScript code rules. This escapes among others the special characters,
	 * the whitespace, the quotes and the unicode characters. Useful whenever you want to use a Java string variable as
	 * a JavaScript string variable.
	 * @param string The string to be escaped according the JavaScript code rules.
	 * @return The escaped string according the JavaScript code rules.
	 */
	public static String escapeJS(String string) {
		if (string == null) {
			return null;
		}

		StringBuilder builder = new StringBuilder(string.length());

		for (char c : string.toCharArray()) {
			if (c > 0xfff) {
				builder.append("\\u" + Integer.toHexString(c));
			}
			else if (c > 0xff) {
				builder.append("\\u0" + Integer.toHexString(c));
			}
			else if (c > 0x7f) {
				builder.append("\\u00" + Integer.toHexString(c));
			}
			else if (c < 32) {
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
						if (c > 0xf) {
							builder.append("\\u00" + Integer.toHexString(c));
						}
						else {
							builder.append("\\u000" + Integer.toHexString(c));
						}

						break;
				}
			}
			else {
				switch (c) {
					case '\'':
						builder.append('\\').append('\'');
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

		return builder.toString();
	}

	/**
	 * Format the given string with 1 parameter. The locale is obtained by {@link Faces#getLocale()}. Design notice:
	 * There are five formatX() methods, each taking 1 to 5 format parameters because EL functions does not support
	 * varargs methods nor overloaded function names.
	 * @param pattern The format pattern.
	 * @param param1 The first parameter.
	 * @return The formatted string.
	 * @see MessageFormat
	 */
	public static String format1(String pattern, String param1) {
		return format(pattern, param1);
	}

	/**
	 * @see #format1(String, String)
	 */
	public static String format2(String pattern, String param1, String param2) {
		return format(pattern, param1, param2);
	}

	/**
	 * @see #format1(String, String)
	 */
	public static String format3(String pattern, String param1, String param2, String param3) {
		return format(pattern, param1, param2, param3);
	}

	/**
	 * @see #format1(String, String)
	 */
	public static String format4(String pattern, String param1, String param2, String param3, String param4) {
		return format(pattern, param1, param2, param3, param4);
	}

	/**
	 * @see #format1(String, String)
	 */
	public static String format5
		(String pattern, String param1, String param2, String param3, String param4, String param5)
	{
		return format(pattern, param1, param2, param3, param4, param5);
	}

	/**
	 * The main string format method taking varargs.
	 */
	private static String format(String pattern, String... params) {
		StringBuffer result = new StringBuffer();
		new MessageFormat(pattern, Faces.getLocale()).format(params, result, null);
		return result.toString();
	}

}