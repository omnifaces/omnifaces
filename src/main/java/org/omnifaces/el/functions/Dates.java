/*
 * Copyright OmniFaces
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

import static java.time.format.TextStyle.FULL;
import static java.time.format.TextStyle.SHORT;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;
import static org.omnifaces.util.Faces.getLocale;
import static org.omnifaces.util.Utils.fromZonedDateTime;
import static org.omnifaces.util.Utils.getZoneId;
import static org.omnifaces.util.Utils.toZoneId;
import static org.omnifaces.util.Utils.toZonedDateTime;

import java.text.DateFormatSymbols;
import java.time.DayOfWeek;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import org.omnifaces.util.Faces;

/**
 * <p>
 * Collection of EL functions for date and time: <code>of:formatDate()</code>, <code>of:formatDateWithTimezone()</code>,
 * <code>of:addXxx()</code> like <code>of:addDays()</code>, <code>of:xxxBetween()</code> like <code>of:daysBetween()</code>,
 * <code>of:getMonths()</code>, <code>of:getShortMonths()</code>, <code>of:getDaysOfWeek()</code>, <code>of:getShortDaysOfWeek()</code>,
 * <code>of:getMonth()</code>, <code>of:getShortMonth()</code>, <code>of:getDayOfWeek()</code> and <code>of:getShortDayOfWeek()</code>.
 * <p>
 * Historical note: before OmniFaces 3.6, these functions accepted <code>java.util.Date</code> and <code>java.util.TimeZone</code> only.
 * Since OmniFaces 3.6, these functions <em>also</em> accept <code>java.time.Temporal</code> and <code>java.time.ZoneId</code>.
 *
 * @author Bauke Scholtz
 */
public final class Dates {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final Map<Locale, Map<String, Integer>> MONTHS_CACHE = new ConcurrentHashMap<>(3);
	private static final Map<Locale, Map<String, Integer>> SHORT_MONTHS_CACHE = new ConcurrentHashMap<>(3);
	private static final Map<Locale, Map<String, Integer>> DAYS_OF_WEEK_CACHE = new ConcurrentHashMap<>(3);
	private static final Map<Locale, Map<String, Integer>> SHORT_DAYS_OF_WEEK_CACHE = new ConcurrentHashMap<>(3);

	// Constructors ---------------------------------------------------------------------------------------------------

	private Dates() {
		// Hide constructor.
	}

	// Formatting -----------------------------------------------------------------------------------------------------

	/**
	 * Format the given date in the given pattern with the default timezone. This is useful when you want to format
	 * dates in for example the <code>title</code> attribute of an UI component, or the <code>itemLabel</code> attribute
	 * of select item, or wherever you can't use the <code>&lt;f:convertDateTime&gt;</code> tag. The format locale will
	 * be set to the one as obtained by {@link Faces#getLocale()}.
	 * @param <D> The date type, can be {@link Date}, {@link Calendar} or {@link Temporal}.
	 * @param date The date to be formatted in the given pattern.
	 * @param pattern The pattern to format the given date in.
	 * @return The date which is formatted in the given pattern.
	 * @throws NullPointerException When the pattern is <code>null</code>.
	 * @throws IllegalArgumentException When date is not {@link Date}, {@link Calendar} or {@link Temporal}.
	 * @see #formatDateWithTimezone(Object, String, Object)
	 */
	public static <D> String formatDate(D date, String pattern) {
		return formatDateWithTimezone(date, pattern, getZoneId(date));
	}

	/**
	 * Format the given date in the given pattern with the given timezone. This is useful when you want to format dates
	 * in for example the <code>title</code> attribute of an UI component, or the <code>itemLabel</code> attribute of
	 * select item, or wherever you can't use the <code>&lt;f:convertDateTime&gt;</code> tag. The format locale will be
	 * set to the one as obtained by {@link Faces#getLocale()}.
	 * @param <D> The date type, can be {@link Date}, {@link Calendar} or {@link Temporal}.
	 * @param <Z> The timezone type, can be either {@link String}, {@link TimeZone} or {@link ZoneId}.
	 * @param date The date to be formatted in the given pattern.
	 * @param pattern The pattern to format the given date in.
	 * @param timezone The timezone to format the given date with.
	 * @return The date which is formatted in the given pattern.
	 * @throws NullPointerException When the pattern is <code>null</code>.
	 * @throws IllegalArgumentException When date is not {@link Date}, {@link Calendar} or {@link Temporal},
	 * or when timezone is not {@link String}, {@link TimeZone} or {@link ZoneId}.
	 */
	public static <D, Z> String formatDateWithTimezone(D date, String pattern, Z timezone) {
		if (date == null) {
			return null;
		}

		return DateTimeFormatter.ofPattern(pattern, getLocale()).withZone(toZoneId(timezone)).format(toZonedDateTime(date));
	}

	// Manipulating ---------------------------------------------------------------------------------------------------

	/**
	 * Returns a new date instance which is a sum of the given date and the given amount of years.
	 * @param <D> The date type, can be {@link Date}, {@link Calendar} or {@link Temporal}.
	 * @param date The date to add the given amount of years to.
	 * @param years The amount of years to be added to the given date. It can be negative.
	 * @return A new date instance which is a sum of the given date and the given amount of years.
	 * @throws NullPointerException When the date is <code>null</code>.
	 * @throws IllegalArgumentException When date is not {@link Date}, {@link Calendar} or {@link Temporal}.
	 */
	public static <D> D addYears(D date, int years) {
		return add(date, years, ChronoUnit.YEARS);
	}

	/**
	 * Returns a new date instance which is a sum of the given date and the given amount of months.
	 * @param <D> The date type, can be {@link Date}, {@link Calendar} or {@link Temporal}.
	 * @param date The date to add the given amount of months to.
	 * @param months The amount of months to be added to the given date. It can be negative.
	 * @return A new date instance which is a sum of the given date and the given amount of months.
	 * @throws NullPointerException When the date is <code>null</code>.
	 * @throws IllegalArgumentException When date is not {@link Date}, {@link Calendar} or {@link Temporal}.
	 */
	public static <D> D addMonths(D date, int months) {
		return add(date, months, ChronoUnit.MONTHS);
	}

	/**
	 * Returns a new date instance which is a sum of the given date and the given amount of weeks.
	 * @param <D> The date type, can be {@link Date}, {@link Calendar} or {@link Temporal}.
	 * @param date The date to add the given amount of weeks to.
	 * @param weeks The amount of weeks to be added to the given date. It can be negative.
	 * @return A new date instance which is a sum of the given date and the given amount of weeks.
	 * @throws NullPointerException When the date is <code>null</code>.
	 * @throws IllegalArgumentException When date is not {@link Date}, {@link Calendar} or {@link Temporal}.
	 */
	public static <D> D addWeeks(D date, int weeks) {
		return add(date, weeks, ChronoUnit.WEEKS);
	}

	/**
	 * Returns a new date instance which is a sum of the given date and the given amount of days.
	 * @param <D> The date type, can be {@link Date}, {@link Calendar} or {@link Temporal}.
	 * @param date The date to add the given amount of days to.
	 * @param days The amount of days to be added to the given date. It can be negative.
	 * @return A new date instance which is a sum of the given date and the given amount of days.
	 * @throws NullPointerException When the date is <code>null</code>.
	 * @throws IllegalArgumentException When date is not {@link Date}, {@link Calendar} or {@link Temporal}.
	 */
	public static <D> D addDays(D date, int days) {
		return add(date, days, ChronoUnit.DAYS);
	}

	/**
	 * Returns a new date instance which is a sum of the given date and the given amount of hours.
	 * @param <D> The date type, can be {@link Date}, {@link Calendar} or {@link Temporal}.
	 * @param date The date to add the given amount of hours to.
	 * @param hours The amount of hours to be added to the given date. It can be negative.
	 * @return A new date instance which is a sum of the given date and the given amount of hours.
	 * @throws NullPointerException When the date is <code>null</code>.
	 * @throws IllegalArgumentException When date is not {@link Date}, {@link Calendar} or {@link Temporal}.
	 */
	public static <D> D addHours(D date, int hours) {
		return add(date, hours, ChronoUnit.HOURS);
	}

	/**
	 * Returns a new date instance which is a sum of the given date and the given amount of minutes.
	 * @param <D> The date type, can be {@link Date}, {@link Calendar} or {@link Temporal}.
	 * @param date The date to add the given amount of minutes to.
	 * @param minutes The amount of minutes to be added to the given date. It can be negative.
	 * @return A new date instance which is a sum of the given date and the given amount of minutes.
	 * @throws NullPointerException When the date is <code>null</code>.
	 * @throws IllegalArgumentException When date is not {@link Date}, {@link Calendar} or {@link Temporal}.
	 */
	public static <D> D addMinutes(D date, int minutes) {
		return add(date, minutes, ChronoUnit.MINUTES);
	}

	/**
	 * Returns a new date instance which is a sum of the given date and the given amount of seconds.
	 * @param <D> The date type, can be {@link Date}, {@link Calendar} or {@link Temporal}.
	 * @param date The date to add the given amount of seconds to.
	 * @param seconds The amount of seconds to be added to the given date. It can be negative.
	 * @return A new date instance which is a sum of the given date and the given amount of seconds.
	 * @throws NullPointerException When the date is <code>null</code>.
	 * @throws IllegalArgumentException When date is not {@link Date}, {@link Calendar} or {@link Temporal}.
	 */
	public static <D> D addSeconds(D date, int seconds) {
		return add(date, seconds, ChronoUnit.SECONDS);
	}

	/**
	 * Helper method which converts the given date to {@link ZonedDateTime} and adds the given amount to the given
	 * chrono unit.
	 */
	@SuppressWarnings("unchecked")
	private static <D> D add(D date, int amount, ChronoUnit unit) {
		return (D) fromZonedDateTime(toZonedDateTime(date).plus(amount, unit), date.getClass());
	}

	// Calculating ----------------------------------------------------------------------------------------------------

	/**
	 * Returns the amount of years between two given dates.
	 * This will be negative when the end date is before the start date.
	 * @param <D> The date type, can be {@link Date}, {@link Calendar} or {@link Temporal}.
	 * @param start The start date.
	 * @param end The end date.
	 * @return The amount of years between two given dates.
	 * @throws NullPointerException When a date is <code>null</code>.
	 * @throws IllegalArgumentException When date is not {@link Date}, {@link Calendar} or {@link Temporal}.
	 */
	public static <D> int yearsBetween(D start, D end) {
		return dateDiff(start, end, ChronoUnit.YEARS);
	}

	/**
	 * Returns the amount of months between two given dates.
	 * This will be negative when the end date is before the start date.
	 * @param <D> The date type, can be {@link Date}, {@link Calendar} or {@link Temporal}.
	 * @param start The start date.
	 * @param end The end date.
	 * @return The amount of months between two given dates.
	 * @throws NullPointerException When a date is <code>null</code>.
	 * @throws IllegalArgumentException When date is not {@link Date}, {@link Calendar} or {@link Temporal}.
	 */
	public static <D> int monthsBetween(D start, D end) {
		return dateDiff(start, end, ChronoUnit.MONTHS);
	}

	/**
	 * Returns the amount of weeks between two given dates.
	 * This will be negative when the end date is before the start date.
	 * @param <D> The date type, can be {@link Date}, {@link Calendar} or {@link Temporal}.
	 * @param start The start date.
	 * @param end The end date.
	 * @return The amount of weeks between two given dates.
	 * @throws NullPointerException When a date is <code>null</code>.
	 * @throws IllegalArgumentException When date is not {@link Date}, {@link Calendar} or {@link Temporal}.
	 */
	public static <D> int weeksBetween(D start, D end) {
		return dateDiff(start, end, ChronoUnit.WEEKS);
	}

	/**
	 * Returns the amount of days between two given dates.
	 * This will be negative when the end date is before the start date.
	 * @param <D> The date type, can be {@link Date}, {@link Calendar} or {@link Temporal}.
	 * @param start The start date.
	 * @param end The end date.
	 * @return The amount of days between two given dates.
	 * @throws NullPointerException When a date is <code>null</code>.
	 * @throws IllegalArgumentException When date is not {@link Date}, {@link Calendar} or {@link Temporal}.
	 */
	public static <D> int daysBetween(D start, D end) {
		return dateDiff(start, end, ChronoUnit.DAYS);
	}

	/**
	 * Helper method which converts the given dates to zoned dates and returns the unit difference of the
	 * given chrono unit.
	 */
	private static <D> int dateDiff(D start, D end, ChronoUnit unit) {
		return (int) unit.between(toZonedDateTime(start).truncatedTo(ChronoUnit.DAYS), toZonedDateTime(end).truncatedTo(ChronoUnit.DAYS));
	}

	/**
	 * Returns the amount of hours between two given dates.
	 * This will be negative when the end date is before the start date.
	 * @param <D> The date type, can be {@link Date}, {@link Calendar} or {@link Temporal}.
	 * @param start The start date.
	 * @param end The end date.
	 * @return The amount of hours between two given dates.
	 * @throws NullPointerException When a date is <code>null</code>.
	 * @throws IllegalArgumentException When date is not {@link Date}, {@link Calendar} or {@link Temporal}.
	 */
	public static <D> long hoursBetween(D start, D end) {
		return timeDiff(start, end, ChronoUnit.HOURS);
	}

	/**
	 * Returns the amount of minutes between two given dates.
	 * This will be negative when the end date is before the start date.
	 * @param <D> The date type, can be {@link Date}, {@link Calendar} or {@link Temporal}.
	 * @param start The start date.
	 * @param end The end date.
	 * @return The amount of minutes between two given dates.
	 * @throws NullPointerException When a date is <code>null</code>.
	 * @throws IllegalArgumentException When date is not {@link Date}, {@link Calendar} or {@link Temporal}.
	 */
	public static <D> long minutesBetween(D start, D end) {
		return timeDiff(start, end, ChronoUnit.MINUTES);
	}

	/**
	 * Returns the amount of seconds between two given dates.
	 * This will be negative when the end date is before the start date.
	 * @param <D> The date type, can be {@link Date}, {@link Calendar} or {@link Temporal}.
	 * @param start The start date.
	 * @param end The end date.
	 * @return The amount of seconds between two given dates.
	 * @throws NullPointerException When a date is <code>null</code>.
	 * @throws IllegalArgumentException When date is not {@link Date}, {@link Calendar} or {@link Temporal}.
	 */
	public static <D> long secondsBetween(D start, D end) {
		return timeDiff(start, end, ChronoUnit.SECONDS);
	}

	/**
	 * Helper method which calculates the time difference of the given two dates in given time unit.
	 */
	private static <D> long timeDiff(D start, D end, ChronoUnit unit) {
		return unit.between(toZonedDateTime(start), toZonedDateTime(end));
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
		return MONTHS_CACHE.computeIfAbsent(locale, k -> mapMonths(FULL, locale));
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
		return SHORT_MONTHS_CACHE.computeIfAbsent(locale, k -> mapMonths(SHORT, locale));
	}

	/**
	 * Helper method to map months.
	 */
	private static Map<String, Integer> mapMonths(TextStyle style, Locale locale) {
		return stream(Month.values()).collect(collectingAndThen(
			toMap(month -> month.getDisplayName(style, locale), Month::getValue, (l, r) -> l, LinkedHashMap::new),
			Collections::unmodifiableMap));
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
		return DAYS_OF_WEEK_CACHE.computeIfAbsent(locale, k -> mapDaysOfWeek(FULL, locale));
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
		return SHORT_DAYS_OF_WEEK_CACHE.computeIfAbsent(locale, k -> mapDaysOfWeek(SHORT, locale));
	}

	/**
	 * Helper method to map days of week.
	 */
	private static Map<String, Integer> mapDaysOfWeek(TextStyle style, Locale locale) {
		return stream(DayOfWeek.values()).collect(collectingAndThen(
			toMap(dayOfWeek -> dayOfWeek.getDisplayName(style, locale), DayOfWeek::getValue, (l, r) -> l, LinkedHashMap::new),
			Collections::unmodifiableMap));
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