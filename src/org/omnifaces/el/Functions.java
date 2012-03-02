package org.omnifaces.el;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Various useful EL functions.
 *
 * @author Bauke Scholtz
 */
public final class Functions {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String ERROR_UNSUPPORTED_ENCODING = "UTF-8 is apparently not supported on this machine.";
	private static final String ERROR_INVALID_ARRAY_SIZE = "The array size must be at least 0.";

	// Constructors ---------------------------------------------------------------------------------------------------

	private Functions() {
		// Hide constructor.
	}

	// Utility --------------------------------------------------------------------------------------------------------

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
	 * Creates and returns a dummy object array of the given size. This is useful if you want to iterate <i>n</i> times
	 * over an <code>&lt;ui:repeat&gt;</code>, which doesn't support EL in <code>begin</code> and <code>end</code>
	 * attributes.
	 * @param size The size of the dummy object array.
	 * @return A dummy object array of the given size.
	 * @throws IllegalArgumentException When the size is less than 0.
	 */
	public static Object[] createArray(int size) {
		if (size < 0) {
			throw new IllegalArgumentException(ERROR_INVALID_ARRAY_SIZE);
		}

		return new Object[size];
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
	 * Returns <code>true</code> if the string representation of an item of the given array equals to the string
	 * representation of the given item. This returns <code>false</code> if either the array or the item is null. This
	 * is useful if you want to for example check if <code>#{paramValues.foo}</code> contains a certain value.
	 * @param array The array whose items have to be compared.
	 * @param item The item to be compared.
	 * @return <code>true</code> if the string representation of an item of the given array equals to the string
	 * representation of the given item.
	 */
	public static boolean contains(Object[] array, Object item) {
		if (array == null || item == null) {
			return false;
		}

		for (Object object : array) {
			if (object != null && object.toString().equals(item.toString())) {
				return true;
			}
		}

		return false;
	}

}