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
import static org.omnifaces.util.Utils.parseLocale;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import org.omnifaces.util.Faces;

/**
 * <p>
 * Collection of EL functions for number formatting: <code>of:formatBytes()</code>, <code>of:formatCurrency()</code>,
 * <code>of:formatNumber()</code>, <code>of:formatNumberDefault()</code>, <code>of:formatPercent()</code>,
 * <code>of:formatThousands()</code> and <code>of:formatThousandsUnit()</code>.
 *
 * @author Bauke Scholtz
 * @since 1.2
 */
public final class Numbers {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final int BYTES_1K = 1024;
	private static final int NUMBER_1K = 1000;
	private static final int PRECISION = 3;

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
		return formatBaseUnit(bytes, BYTES_1K, 1, true, "B");
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
	 * Format the given number in the default pattern of the default locale. This is useful when you want to format
	 * numbers in for example the <code>title</code> attribute of an UI component, or the <code>itemLabel</code>
	 * attribute of select item, or wherever you can't use the <code>&lt;f:convertNumber&gt;</code> tag. The default
	 * locale is the one as obtained by {@link Faces#getLocale()}.
	 * @param number The number to be formatted in the default pattern of the default locale.
	 * @return The number which is formatted in the default pattern of the default locale.
	 * @since 1.3
	 */
	public static String formatNumberDefault(Number number) {
		return formatNumberDefaultForLocale(number, getLocale());
	}

	/**
	 * Format the given number in the default pattern of the given locale. This is useful when you want to format
	 * numbers in for example the <code>title</code> attribute of an UI component, or the <code>itemLabel</code>
	 * attribute of select item, or wherever you can't use the <code>&lt;f:convertNumber&gt;</code> tag. The given
	 * locale can be a {@link Locale} object or a string representation.
	 * @param number The number to be formatted in the default pattern of the given locale.
	 * @param locale The locale to obtain the default pattern from.
	 * @return The number which is formatted in the default pattern of the given locale.
	 * @since 2.3
	 */
	public static String formatNumberDefaultForLocale(Number number, Object locale) {
		if (number == null) {
			return null;
		}

		return NumberFormat.getNumberInstance(parseLocale(locale)).format(number);
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
	 * with metric unit (k, M, G, T, P or E), rounding half up with a precision of 3 digits, whereafter trailing zeroes
	 * in fraction part are stripped.
	 * For example (with English locale):
	 * <ul>
	 * <li>1.6666 will appear as 1.67
	 * <li>999.4 will appear as 999
	 * <li>999.5 will appear as 1k
	 * <li>1004 will appear as 1k
	 * <li>1005 will appear as 1.01k
	 * <li>1594 will appear as 1.59k
	 * <li>1595 will appear as 1.6k
	 * <li>9000 will appear as 9k
	 * <li>9900 will appear as 9.9k
	 * <li>9994 will appear as 9.99k
	 * <li>9995 will appear as 10k
	 * <li>99990 will appear as 100k
	 * <li>9994999 will appear as 9.99M
	 * <li>9995000 will appear as 10M
	 * </ul>
	 * The format locale will be set to the one as obtained by {@link Faces#getLocale()}.
	 * If the value is <code>null</code>, <code>NaN</code> or infinity, then this will return <code>null</code>.
	 * @param number The number to be formatted.
	 * @return The formatted number.
	 * @since 2.3
	 */
	public static String formatThousands(Number number) {
		return formatThousandsUnit(number, null);
	}

	/**
	 * Format the given number to nearest 10<sup>n</sup> (rounded to thousands), suffixed with a space, the metric unit
	 * prefix (k, M, G, T, P or E) and the given unit, rounding half up with a precision of 3 digits, whereafter
	 * trailing zeroes in fraction part are stripped.
	 * For example (with English locale and unit <code>B</code>):
	 * <ul>
	 * <li>1.6666 will appear as 1.67 B
	 * <li>999.4 will appear as 999 B
	 * <li>999.5 will appear as 1 kB
	 * <li>1004 will appear as 1 kB
	 * <li>1005 will appear as 1.01 kB
	 * <li>1594 will appear as 1.59 kB
	 * <li>1595 will appear as 1.6 kB
	 * <li>9000 will appear as 9 kB
	 * <li>9900 will appear as 9.9 kB
	 * <li>9994 will appear as 9.99 kB
	 * <li>9995 will appear as 10 kB
	 * <li>99990 will appear as 100 kB
	 * <li>9994999 will appear as 9.99 MB
	 * <li>9995000 will appear as 10 MB
	 * </ul>
	 * The format locale will be set to the one as obtained by {@link Faces#getLocale()}.
	 * If the value is <code>null</code>, <code>NaN</code> or infinity, then this will return <code>null</code>.
	 * @param number The number to be formatted.
	 * @param unit The unit used in the format. E.g. <code>B</code> for Bytes, <code>W</code> for Watt, etc. If the unit
	 * is <code>null</code>, then this method will behave exactly as described in {@link #formatThousands(Number)}.
	 * @return The formatted number with unit.
	 * @since 2.3
	 */
	public static String formatThousandsUnit(Number number, String unit) {
		return formatBaseUnit(number, NUMBER_1K, null, false, unit);
	}

	/**
	 * @param number Number to be formatted.
	 * @param base Rounding base.
	 * @param fractions Fraction length. If null, then precision of 3 digits will be assumed and all trailing zeroes will be stripped.
	 * @param iec IEC or metric. If IEC, then unit prefix "Ki", "Mi", "Gi", etc will be used, else "k", "M", "G", etc.
	 * @param unit Unit suffix. If null, then there is no space separator between number and unit prefix.
	 */
	private static String formatBaseUnit(Number number, int base, Integer fractions, boolean iec, String unit) {
		if (number == null) {
			return null;
		}

		BigDecimal decimal;

		try {
			decimal = (number instanceof BigDecimal) ? ((BigDecimal) number) : new BigDecimal(number.toString());
		}
		catch (NumberFormatException e) {
			return null;
		}

		return formatBase(decimal, base, fractions, iec, unit);
	}

	private static String formatBase(BigDecimal decimal, int base, Integer fractions, boolean iec, String unit) {
		int exponent = (int) (Math.log(decimal.longValue()) / Math.log(base));
		BigDecimal divided = decimal.divide(BigDecimal.valueOf(Math.pow(base, exponent)));
		int maxfractions = (fractions != null) ? fractions : (PRECISION - String.valueOf(divided.longValue()).length());
		BigDecimal formatted;

		try {
			DecimalFormat formatter = (DecimalFormat) NumberFormat.getNumberInstance(getLocale());
			formatter.setParseBigDecimal(true);
			formatted = (BigDecimal) formatter.parse(String.format(getLocale(), "%." + maxfractions + "f", divided));
		}
		catch (ParseException e) {
			throw new IllegalStateException(e);
		}

		if (formatted.longValue() >= base) { // E.g. 999.5 becomes 1000 which needs to be reformatted as 1k.
			return formatBase(formatted, base, fractions, iec, unit);
		}
		else {
			return formatUnit(formatted, iec, unit, exponent, maxfractions > 0 && fractions == null);
		}
	}

	private static String formatUnit(BigDecimal decimal, boolean iec, String unit, int exponent, boolean stripZeroes) {
		String formatted = decimal.toString();

		if (stripZeroes) {
			formatted = formatted.replaceAll("\\D?0+$", "");
		}

		String separator = (unit == null) ? "" : " ";
		String unitPrefix = ((exponent > 0) ? ((iec ? "K" : "k") + "MGTPE").charAt(exponent - 1) : "") + (iec ? "i" : "");
		String unitString = (unit == null) ? "" : unit;
		return formatted + separator + unitPrefix + unitString;
	}

}