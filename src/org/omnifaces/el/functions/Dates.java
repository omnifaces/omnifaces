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

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.omnifaces.util.Faces;

/**
 * Collection of EL functions for date and time.
 *
 * @author Bauke Scholtz
 */
public final class Dates {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final Map<Locale, Map<String, Integer>> MONTHS_CACHE = new HashMap<Locale, Map<String, Integer>>(3);
	private static final Map<Locale, Map<String, Integer>> SHORT_MONTHS_CACHE = new HashMap<Locale, Map<String, Integer>>(3);
	private static final TimeZone TIMEZONE_UTC = TimeZone.getTimeZone("UTC");

	// Constructors ---------------------------------------------------------------------------------------------------

	private Dates() {
		// Hide constructor.
	}

	// Formatting -----------------------------------------------------------------------------------------------------

	/**
	 * Format the given date in the given pattern with system default timezone. This is useful when you want to format
	 * dates in for example the <code>title</code> attribute of an UI component, or the <code>itemLabel</code> attribute
	 * of select item, or wherever you can't use the <code>&lt;f:convertDateTime&gt;</code> tag. The format locale will
	 * be set to the one as obtained by {@link Faces#getLocale()}.
	 * @param date The date to be formatted in the given pattern.
	 * @param pattern The pattern to format the given date in.
	 * @return The date which is formatted in the given pattern.
	 * @throws NullPointerException When the pattern is <code>null</code>.
	 * @see #formatDateWithTimezone(Date, String, String)
	 */
	public static String formatDate(Date date, String pattern) {
		return formatDate(date, pattern, TimeZone.getDefault());
	}

	/**
	 * Format the given date in the given pattern with the given timezone. This is useful when you want to format dates
	 * in for example the <code>title</code> attribute of an UI component, or the <code>itemLabel</code> attribute of
	 * select item, or wherever you can't use the <code>&lt;f:convertDateTime&gt;</code> tag. The format locale will be
	 * set to the one as obtained by {@link Faces#getLocale()}.
	 * @param date The date to be formatted in the given pattern.
	 * @param pattern The pattern to format the given date in.
	 * @param timezone The timezone to format the given date with.
	 * @return The date which is formatted in the given pattern.
	 * @throws NullPointerException When the pattern is <code>null</code>.
	 */
	public static String formatDateWithTimezone(Date date, String pattern, String timezone) {
		return formatDate(date, pattern, TimeZone.getTimeZone(timezone));
	}

	/**
	 * Helper method taking {@link TimeZone} instead of {@link String}.
	 */
	private static String formatDate(Date date, String pattern, TimeZone timezone) {
		if (date == null) {
			return null;
		}

		if (pattern == null) {
			throw new NullPointerException("pattern");
		}

		DateFormat formatter = new SimpleDateFormat(pattern, Faces.getLocale());
		formatter.setTimeZone(timezone);
		return formatter.format(date);
	}

	// Manipulating ---------------------------------------------------------------------------------------------------

	/**
	 * Returns a new date instance which is a sum of the given date and the given amount of years.
	 * @param date The date to add the given amount of years to.
	 * @param years The amount of years to be added to the given date. It can be negative.
	 * @return A new date instance which is a sum of the given date and the given amount of years.
	 * @throws NullPointerException When the date is <code>null</code>.
	 */
	public static Date addYears(Date date, int years) {
		return add(date, years, Calendar.YEAR);
	}

	/**
	 * Returns a new date instance which is a sum of the given date and the given amount of months.
	 * @param date The date to add the given amount of months to.
	 * @param months The amount of months to be added to the given date. It can be negative.
	 * @return A new date instance which is a sum of the given date and the given amount of months.
	 * @throws NullPointerException When the date is <code>null</code>.
	 */
	public static Date addMonths(Date date, int months) {
		return add(date, months, Calendar.MONTH);
	}

	/**
	 * Returns a new date instance which is a sum of the given date and the given amount of weeks.
	 * @param date The date to add the given amount of weeks to.
	 * @param weeks The amount of weeks to be added to the given date. It can be negative.
	 * @return A new date instance which is a sum of the given date and the given amount of weeks.
	 * @throws NullPointerException When the date is <code>null</code>.
	 */
	public static Date addWeeks(Date date, int weeks) {
		return add(date, weeks, Calendar.WEEK_OF_YEAR);
	}

	/**
	 * Returns a new date instance which is a sum of the given date and the given amount of days.
	 * @param date The date to add the given amount of days to.
	 * @param days The amount of days to be added to the given date. It can be negative.
	 * @return A new date instance which is a sum of the given date and the given amount of days.
	 * @throws NullPointerException When the date is <code>null</code>.
	 */
	public static Date addDays(Date date, int days) {
		return add(date, days, Calendar.DAY_OF_MONTH);
	}

	/**
	 * Returns a new date instance which is a sum of the given date and the given amount of hours.
	 * @param date The date to add the given amount of hours to.
	 * @param hours The amount of hours to be added to the given date. It can be negative.
	 * @return A new date instance which is a sum of the given date and the given amount of hours.
	 * @throws NullPointerException When the date is <code>null</code>.
	 */
	public static Date addHours(Date date, int hours) {
		return add(date, hours, Calendar.HOUR_OF_DAY);
	}

	/**
	 * Returns a new date instance which is a sum of the given date and the given amount of minutes.
	 * @param date The date to add the given amount of minutes to.
	 * @param minutes The amount of minutes to be added to the given date. It can be negative.
	 * @return A new date instance which is a sum of the given date and the given amount of minutes.
	 * @throws NullPointerException When the date is <code>null</code>.
	 */
	public static Date addMinutes(Date date, int minutes) {
		return add(date, minutes, Calendar.MINUTE);
	}

	/**
	 * Returns a new date instance which is a sum of the given date and the given amount of seconds.
	 * @param date The date to add the given amount of seconds to.
	 * @param seconds The amount of seconds to be added to the given date. It can be negative.
	 * @return A new date instance which is a sum of the given date and the given amount of seconds.
	 * @throws NullPointerException When the date is <code>null</code>.
	 */
	public static Date addSeconds(Date date, int seconds) {
		return add(date, seconds, Calendar.SECOND);
	}

	/**
	 * Helper method which converts the given date to an UTC calendar and adds the given amount of units to the given
	 * calendar field.
	 */
	private static Date add(Date date, int units, int field) {
		if (date == null) {
			throw new NullPointerException("date");
		}

		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.setTime(date);
		calendar.setTimeZone(TIMEZONE_UTC);
		calendar.add(field, units);
		return calendar.getTime();
	}

	// Mappings -------------------------------------------------------------------------------------------------------

	/**
	 * Returns a mapping of month names by month numbers for the current locale. For example: "January=1", "February=2",
	 * etc. This is useful if you want to for example populate a <code>&lt;f:selectItems&gt;</code> which shows all
	 * months. The locale is obtained by {@link Faces#getLocale()}. The mapping is per locale stored in a local cache
	 * to improve retrieving performance.
	 * @return Month names for the current locale.
	 * @see DateFormatSymbols#getMonths()
	 */
	public static Map<String, Integer> getMonths() {
		Locale locale = Faces.getLocale();
		Map<String, Integer> months = MONTHS_CACHE.get(locale);

		if (months == null) {
			months = new LinkedHashMap<String, Integer>();

			for (String month : DateFormatSymbols.getInstance(Faces.getLocale()).getMonths()) {
				if (!month.isEmpty()) { // 13th month may or may not be empty, depending on default calendar.
					months.put(month, months.size() + 1);
				}
			}

			MONTHS_CACHE.put(locale, months);
		}

		return months;
	}

	/**
	 * Returns a mapping of short month names by month numbers for the current locale. For example: "Jan=1", "Feb=2",
	 * etc. This is useful if you want to for example populate a <code>&lt;f:selectItems&gt;</code> which shows all
	 * short months. The locale is obtained by {@link Faces#getLocale()}. The mapping is per locale stored in a local
	 * cache to improve retrieving performance.
	 * @return Short month names for the current locale.
	 * @see DateFormatSymbols#getShortMonths()
	 */
	public static Map<String, Integer> getShortMonths() {
		Locale locale = Faces.getLocale();
		Map<String, Integer> shortMonths = SHORT_MONTHS_CACHE.get(locale);

		if (shortMonths == null) {
			shortMonths = new LinkedHashMap<String, Integer>();

			for (String shortMonth : DateFormatSymbols.getInstance(Faces.getLocale()).getShortMonths()) {
				if (!shortMonth.isEmpty()) { // 13th month may or may not be empty, depending on default calendar.
					shortMonths.put(shortMonth, shortMonths.size() + 1);
				}
			}

			SHORT_MONTHS_CACHE.put(locale, shortMonths);
		}

		return shortMonths;
	}

}