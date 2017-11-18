/*
 * Copyright 2017 OmniFaces
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

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.omnifaces.util.Faces;

/**
 * <p>
 * Collection of EL functions for date and time: <code>of:formatDate()</code>, <code>of:formatDateWithTimezone()</code>,
 * <code>of:addXxx()</code> like <code>of:addDays()</code>, <code>of:xxxBetween()</code> like <code>of:daysBetween()</code>,
 * <code>of:getMonths()</code>, <code>of:getShortMonths()</code>, <code>of:getDaysOfWeek()</code>, <code>of:getShortDaysOfWeek()</code>,
 * <code>of:getMonth()</code>, <code>of:getShortMonth()</code>, <code>of:getDayOfWeek()</code> and <code>of:getShortDayOfWeek()</code>.
 *
 * @author Bauke Scholtz
 */
public final class Dates {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final Map<Locale, Map<String, Integer>> MONTHS_CACHE = new ConcurrentHashMap<>(3);
	private static final Map<Locale, Map<String, Integer>> SHORT_MONTHS_CACHE = new ConcurrentHashMap<>(3);
	private static final Map<Locale, Map<String, Integer>> DAYS_OF_WEEK_CACHE = new ConcurrentHashMap<>(3);
	private static final Map<Locale, Map<String, Integer>> SHORT_DAYS_OF_WEEK_CACHE = new ConcurrentHashMap<>(3);
	private static final TimeZone TIMEZONE_DEFAULT = TimeZone.getDefault();
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
	 * @see #formatDateWithTimezone(Date, String, Object)
	 */
	public static String formatDate(Date date, String pattern) {
		return formatDate(date, pattern, TIMEZONE_DEFAULT);
	}

	/**
	 * Format the given date in the given pattern with the given timezone. This is useful when you want to format dates
	 * in for example the <code>title</code> attribute of an UI component, or the <code>itemLabel</code> attribute of
	 * select item, or wherever you can't use the <code>&lt;f:convertDateTime&gt;</code> tag. The format locale will be
	 * set to the one as obtained by {@link Faces#getLocale()}.
	 * @param date The date to be formatted in the given pattern.
	 * @param pattern The pattern to format the given date in.
	 * @param timezone The timezone to format the given date with, can be either timezone ID as string or
	 * {@link TimeZone} object.
	 * @return The date which is formatted in the given pattern.
	 * @throws NullPointerException When the pattern is <code>null</code>.
	 */
	public static String formatDateWithTimezone(Date date, String pattern, Object timezone) {
		return formatDate(date, pattern,
			(timezone instanceof TimeZone) ? ((TimeZone) timezone) : TimeZone.getTimeZone(String.valueOf(timezone)));
	}

	/**
	 * Helper method taking {@link TimeZone} instead of {@link String}.
	 */
	private static String formatDate(Date date, String pattern, TimeZone timezone) {
		if (date == null) {
			return null;
		}

		DateFormat formatter = new SimpleDateFormat(pattern, getLocale());
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
		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.setTime(date);
		calendar.setTimeZone(TIMEZONE_UTC);
		calendar.add(field, units);
		return calendar.getTime();
	}

	// Calculating ----------------------------------------------------------------------------------------------------

	/**
	 * Returns the amount of years between two given dates.
	 * This will be negative when the end date is before the start date.
	 * @param start The start date.
	 * @param end The end date.
	 * @return The amount of years between two given dates.
	 * @throws NullPointerException When a date is <code>null</code>.
	 */
	public static int yearsBetween(Date start, Date end) {
		return dateDiff(start, end, Calendar.YEAR);
	}

	/**
	 * Returns the amount of months between two given dates.
	 * This will be negative when the end date is before the start date.
	 * @param start The start date.
	 * @param end The end date.
	 * @return The amount of months between two given dates.
	 * @throws NullPointerException When a date is <code>null</code>.
	 */
	public static int monthsBetween(Date start, Date end) {
		return dateDiff(start, end, Calendar.MONTH);
	}

	/**
	 * Returns the amount of weeks between two given dates.
	 * This will be negative when the end date is before the start date.
	 * @param start The start date.
	 * @param end The end date.
	 * @return The amount of weeks between two given dates.
	 * @throws NullPointerException When a date is <code>null</code>.
	 */
	public static int weeksBetween(Date start, Date end) {
		return dateDiff(start, end, Calendar.WEEK_OF_YEAR);
	}

	/**
	 * Returns the amount of days between two given dates.
	 * This will be negative when the end date is before the start date.
	 * @param start The start date.
	 * @param end The end date.
	 * @return The amount of days between two given dates.
	 * @throws NullPointerException When a date is <code>null</code>.
	 */
	public static int daysBetween(Date start, Date end) {
		return dateDiff(start, end, Calendar.DAY_OF_MONTH);
	}

	/**
	 * Helper method which converts the given dates to UTC calendar without time and returns the unit difference of the
	 * given calendar field.
	 */
	private static int dateDiff(Date startDate, Date endDate, int field) {
		Calendar start = toUTCCalendarWithoutTime(startDate);
		Calendar end = toUTCCalendarWithoutTime(endDate);
		int elapsed = 0;

		if (start.before(end)) {
			while (start.before(end)) {
				start.add(field, 1);
				elapsed++;
			}
		}
		else if (start.after(end)) {
			while (start.after(end)) {
				start.add(field, -1);
				elapsed--;
			}
		}

		return elapsed;
	}

	/**
	 * Helper method to convert given date to an UTC calendar without time part (to prevent potential DST issues).
	 */
	private static Calendar toUTCCalendarWithoutTime(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.setTime(date);
		calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar;
	}

	/**
	 * Returns the amount of hours between two given dates.
	 * This will be negative when the end date is before the start date.
	 * @param start The start date.
	 * @param end The end date.
	 * @return The amount of hours between two given dates.
	 * @throws NullPointerException When a date is <code>null</code>.
	 */
	public static long hoursBetween(Date start, Date end) {
		return timeDiff(start, end, TimeUnit.HOURS);
	}

	/**
	 * Returns the amount of minutes between two given dates.
	 * This will be negative when the end date is before the start date.
	 * @param start The start date.
	 * @param end The end date.
	 * @return The amount of minutes between two given dates.
	 * @throws NullPointerException When a date is <code>null</code>.
	 */
	public static long minutesBetween(Date start, Date end) {
		return timeDiff(start, end, TimeUnit.MINUTES);
	}

	/**
	 * Returns the amount of seconds between two given dates.
	 * This will be negative when the end date is before the start date.
	 * @param start The start date.
	 * @param end The end date.
	 * @return The amount of seconds between two given dates.
	 * @throws NullPointerException When a date is <code>null</code>.
	 */
	public static long secondsBetween(Date start, Date end) {
		return timeDiff(start, end, TimeUnit.SECONDS);
	}

	/**
	 * Helper method which calculates the time difference of the given two dates in given time unit.
	 */
	private static long timeDiff(Date startDate, Date endDate, TimeUnit timeUnit) {
		return timeUnit.convert(endDate.getTime() - startDate.getTime(), TimeUnit.MILLISECONDS);
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
		Locale locale = getLocale();
		Map<String, Integer> months = MONTHS_CACHE.get(locale);

		if (months == null) {
			months = mapMonths(DateFormatSymbols.getInstance(locale).getMonths());
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
		Locale locale = getLocale();
		return SHORT_MONTHS_CACHE.computeIfAbsent(locale, k -> mapMonths(DateFormatSymbols.getInstance(locale).getShortMonths()));
	}

	/**
	 * Helper method to map months.
	 */
	private static Map<String, Integer> mapMonths(String[] months) {
		Map<String, Integer> mapping = new LinkedHashMap<>();

		for (String month : months) {
			if (!month.isEmpty()) { // 13th month may or may not be empty, depending on default calendar.
				mapping.put(month, mapping.size() + 1);
			}
		}

		return Collections.unmodifiableMap(mapping);
	}

	/**
	 * Returns a mapping of day of week names in ISO 8601 order (Monday first) for the current locale. For example:
	 * "Monday=1", "Tuesday=2", etc. This is useful if you want to for example populate a <code>&lt;f:selectItems&gt;</code>
	 * which shows all days of week. The locale is obtained by {@link Faces#getLocale()}. The mapping is per locale
	 * stored in a local cache to improve retrieving performance.
	 * @return Day of week names for the current locale.
	 * @see DateFormatSymbols#getWeekdays()
	 */
	public static Map<String, Integer> getDaysOfWeek() {
		Locale locale = getLocale();
		return DAYS_OF_WEEK_CACHE.computeIfAbsent(locale, k -> mapDaysOfWeek(DateFormatSymbols.getInstance(locale).getWeekdays()));
	}

	/**
	 * Returns a mapping of short day of week names in ISO 8601 order (Monday first) for the current locale. For example:
	 * "Mon=1", "Tue=2", etc. This is useful if you want to for example populate a <code>&lt;f:selectItems&gt;</code>
	 * which shows all short days of week. The locale is obtained by {@link Faces#getLocale()}. The mapping is per locale
	 * stored in a local cache to improve retrieving performance.
	 * @return Short day of week names for the current locale.
	 * @see DateFormatSymbols#getShortWeekdays()
	 */
	public static Map<String, Integer> getShortDaysOfWeek() {
		Locale locale = getLocale();
		return SHORT_DAYS_OF_WEEK_CACHE.computeIfAbsent(locale, k -> mapDaysOfWeek(DateFormatSymbols.getInstance(locale).getShortWeekdays()));
	}

	/**
	 * Helper method to map days of week.
	 */
	private static Map<String, Integer> mapDaysOfWeek(String[] weekdays) {
		Map<String, Integer> mapping = new LinkedHashMap<>();
		mapping.put(weekdays[Calendar.MONDAY], mapping.size() + 1);
		mapping.put(weekdays[Calendar.TUESDAY], mapping.size() + 1);
		mapping.put(weekdays[Calendar.WEDNESDAY], mapping.size() + 1);
		mapping.put(weekdays[Calendar.THURSDAY], mapping.size() + 1);
		mapping.put(weekdays[Calendar.FRIDAY], mapping.size() + 1);
		mapping.put(weekdays[Calendar.SATURDAY], mapping.size() + 1);
		mapping.put(weekdays[Calendar.SUNDAY], mapping.size() + 1);
		return Collections.unmodifiableMap(mapping);
	}

	/**
	 * Returns the month name from the mapping associated with the given month number for the current locale. For
	 * example: "1=January", "2=February", etc. The locale is obtained by {@link Faces#getLocale()}.
	 * @param monthNumber The month number to return the month name from the mapping for.
	 * @return The month name form the mapping associated with the given month number.
	 * @since 1.4
	 */
	public static String getMonth(Integer monthNumber) {
		return getKey(getMonths(), monthNumber);
	}

	/**
	 * Returns the short month name from the mapping associated with the given month number for the current locale. For
	 * example: "1=Jan", "2=Feb", etc. The locale is obtained by {@link Faces#getLocale()}.
	 * @param monthNumber The month number to return the short month name from the mapping for.
	 * @return The short month name form the mapping associated with the given month number.
	 * @since 1.4
	 */
	public static String getShortMonth(Integer monthNumber) {
		return getKey(getShortMonths(), monthNumber);
	}

	/**
	 * Returns the day of week name from the mapping associated with the given day of week number in ISO 8601 order
	 * (Monday first) for the current locale. For example: "1=Monday", "2=Tuesday", etc. The locale is obtained by
	 * {@link Faces#getLocale()}.
	 * @param dayOfWeekNumber The day of week number to return the day of week name from the mapping for.
	 * @return The day of week name from the mapping associated with the given day of week number.
	 * @since 1.4
	 */
	public static String getDayOfWeek(Integer dayOfWeekNumber) {
		return getKey(getDaysOfWeek(), dayOfWeekNumber);
	}

	/**
	 * Returns the short day of week name from the mapping associated with the given day of week number in ISO 8601
	 * order (Monday first) for the current locale. For example: "1=Mon", "2=Tue", etc. The locale is obtained by
	 * {@link Faces#getLocale()}.
	 * @param dayOfWeekNumber The day of week number to return the short day of week name from the mapping for.
	 * @return The short day of week name from the mapping associated with the given day of week number.
	 * @since 1.4
	 */
	public static String getShortDayOfWeek(Integer dayOfWeekNumber) {
		return getKey(getShortDaysOfWeek(), dayOfWeekNumber);
	}

	/**
	 * Helper method to return the map key from the given map associated with given map value.
	 */
	private static <K, V> K getKey(Map<K, V> map, V value) {
		if (value == null) {
			return null; // None of the maps have a null value anyway.
		}

		for (Entry<K, V> entry : map.entrySet()) {
			if (value.equals(entry.getValue())) {
				return entry.getKey();
			}
		}

		return null;
	}

}