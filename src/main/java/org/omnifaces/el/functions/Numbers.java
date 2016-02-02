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

import static org.omnifaces.util.Faces.getLocale;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;

import org.omnifaces.util.Faces;

/**
 * <p>
 * Collection of EL functions for number formatting: <code>of:formatBytes()</code>, <code>of:formatCurrency()</code>,
 * <code>of:formatNumber()</code>, <code>of:formatNumberDefault()</code>, <code>of:formatPercent()</code> and
 * <code>of:formatThousands()</code>.
 *
 * @author Bauke Scholtz
 * @since 1.2
 */
public final class Numbers {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final int BYTES_1K = 1024;
	private static final long NUMBER_1K = 1000;

	// Constructors ---------------------------------------------------------------------------------------------------

	private Numbers() {
		// Hide constructor.
	}

	// Utility --------------------------------------------------------------------------------------------------------

	/**
	 * Format the given bytes to nearest 10<sup>n</sup> with IEC binary unit (KiB, MiB, etc) with rounding precision of
	 * 1 fraction. For example:
	 * <ul>
	 * <li>1023 bytes will appear as 1023 B
	 * <li>1024 bytes will appear as 1.0 KiB
	 * <li>500000 bytes will appear as 488.3 KiB
	 * <li>1048576 bytes will appear as 1.0 MiB
	 * </ul>
	 * The format locale will be set to the one as obtained by {@link Faces#getLocale()}.
	 * @param bytes The bytes to be formatted.
	 * @return The formatted bytes.
	 */
	public static String formatBytes(Long bytes) {
		if (bytes == null) {
			return "0 B";
		}

		if (bytes < BYTES_1K) {
			return bytes + " B";
		}

		int exp = (int) (Math.log(bytes) / Math.log(BYTES_1K));
		return String.format(getLocale(), "%.1f %ciB", bytes / Math.pow(BYTES_1K, exp), "KMGTPE".charAt(exp - 1));
	}

	/**
	 * Format the given number as currency with the given symbol. This is useful when you want to format numbers as
	 * currency in for example the <code>title</code> attribute of an UI component, or the <code>itemLabel</code>
	 * attribute of select item, or wherever you can't use the <code>&lt;f:convertNumber&gt;</code> tag. The format
	 * locale will be set to the one as obtained by {@link Faces#getLocale()}.
	 * @param number The number to be formatted as currency.
	 * @param currencySymbol The currency symbol to be used.
	 * @return The number which is formatted as currency with the given symbol.
	 * @throws NullPointerException When the currency symbol is <code>null</code>.
	 */
	public static String formatCurrency(Number number, String currencySymbol) {
		if (number == null) {
			return null;
		}

		DecimalFormat formatter = (DecimalFormat) NumberFormat.getCurrencyInstance(getLocale());
		DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
		symbols.setCurrencySymbol(currencySymbol);
		formatter.setDecimalFormatSymbols(symbols);
		return formatter.format(number);
	}

	/**
	 * Format the given number in the given pattern. This is useful when you want to format numbers in for example the
	 * <code>title</code> attribute of an UI component, or the <code>itemLabel</code> attribute of select item, or
	 * wherever you can't use the <code>&lt;f:convertNumber&gt;</code> tag. The format locale will be set to the one as
	 * obtained by {@link Faces#getLocale()}.
	 * @param number The number to be formatted in the given pattern.
	 * @param pattern The pattern to format the given number in.
	 * @return The number which is formatted in the given pattern.
	 * @throws NullPointerException When the pattern is <code>null</code>.
	 */
	public static String formatNumber(Number number, String pattern) {
		if (number == null) {
			return null;
		}

		DecimalFormat formatter = (DecimalFormat) NumberFormat.getNumberInstance(getLocale());
		formatter.applyPattern(pattern);
		return formatter.format(number);
	}

	/**
	 * Format the given number in the locale-default pattern. This is useful when you want to format numbers in for
	 * example the <code>title</code> attribute of an UI component, or the <code>itemLabel</code> attribute of select
	 * item, or wherever you can't use the <code>&lt;f:convertNumber&gt;</code> tag. The format locale will be set to
	 * the one as obtained by {@link Faces#getLocale()}.
	 * @param number The number to be formatted in the locale-default pattern.
	 * @return The number which is formatted in the locale-default pattern.
	 * @since 1.3
	 */
	public static String formatNumberDefault(Number number) {
		if (number == null) {
			return null;
		}

		return NumberFormat.getNumberInstance(getLocale()).format(number);
	}

	/**
	 * Format the given number as percentage. This is useful when you want to format numbers as
	 * percentage in for example the <code>title</code> attribute of an UI component, or the <code>itemLabel</code>
	 * attribute of select item, or wherever you can't use the <code>&lt;f:convertNumber&gt;</code> tag. The format
	 * locale will be set to the one as obtained by {@link Faces#getLocale()}.
	 * @param number The number to be formatted as percentage.
	 * @return The number which is formatted as percentage.
	 * @since 1.6
	 */
	public static String formatPercent(Number number) {
		if (number == null) {
			return null;
		}

		return NumberFormat.getPercentInstance(getLocale()).format(number);
	}

	/**
	 * Format the given number to nearest 10<sup>n</sup> (rounded to thousands), immediately suffixed (without space)
	 * with lowercased IEC unit (k, m, g, t, p and e), rounding half up with a precision of 3 digits, whereafter
	 * trailing zeroes in fraction part are stripped. Numbers lower than thousand are not affected.
	 * For example (with English locale):
	 * <ul>
	 * <li>999.999 will appear as 999.999
	 * <li>1000 will appear as 1k
	 * <li>1004 will appear as 1k
	 * <li>1005 will appear as 1.01k
	 * <li>1594 will appear as 1.59k
	 * <li>1595 will appear as 1.6k
	 * <li>9000 will appear as 9k
	 * <li>9900 will appear as 9.9k
	 * <li>9994 will appear as 9.99k
	 * <li>9995 will appear as 10k
	 * <li>99990 will appear as 100k
	 * <li>9994999 will appear as 9.99m
	 * <li>9995000 will appear as 10m
	 * </ul>
	 * The format locale will be set to the one as obtained by {@link Faces#getLocale()}.
	 * If the value is <code>null</code>, <code>NaN</code> or infinity, then this will return <code>null</code>.
	 * @param number The number to be formatted.
	 * @return The formatted number.
	 * @since 2.3
	 */
	public static String formatThousands(Number number) {
		if (number == null) {
			return null;
		}

		BigDecimal decimal;

		try {
			decimal = new BigDecimal(number.toString());
		}
		catch (NumberFormatException e) {
			return null;
		}

		if (decimal.longValue() < NUMBER_1K) {
			return number.toString();
		}

		int exp = (int) (Math.log(decimal.longValue()) / Math.log(NUMBER_1K));
		BigDecimal divided = decimal.divide(BigDecimal.valueOf(Math.pow(NUMBER_1K, exp)));
		int maxfractions = 3 - String.valueOf(divided.longValue()).length();
		return String.format(getLocale(), "%." + maxfractions + "f", divided).replaceAll("\\D?0+$", "") + "kmgtpe".charAt(exp - 1);
	}

}