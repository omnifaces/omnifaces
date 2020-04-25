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
package org.omnifaces.test.el.functions;

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_DATE;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.time.format.DateTimeFormatter.ISO_TIME;
import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Map.Entry;
import java.util.TimeZone;

import org.junit.Test;
import org.omnifaces.el.functions.Dates;
import org.omnifaces.util.Utils;

public class TestDates {

	private static final ZoneId ZP2 = ZoneOffset.ofHours(2);
	private static final ZoneId CUR = ZoneId.of("America/Curacao");

	@Test
	public void testZonedDateTimeUtilities() { // Used under the covers by these EL functions.
		testZonedDateTimeUtilities(new java.sql.Date(System.currentTimeMillis()));
		testZonedDateTimeUtilities(new java.sql.Time(System.currentTimeMillis()));
		testZonedDateTimeUtilities(new java.sql.Timestamp(System.currentTimeMillis()));
		testZonedDateTimeUtilities(new java.util.Date());
		testZonedDateTimeUtilities(Calendar.getInstance());
		testZonedDateTimeUtilities(Calendar.getInstance(TimeZone.getTimeZone(UTC)));
		testZonedDateTimeUtilities(Calendar.getInstance(TimeZone.getTimeZone(ZP2)));
		testZonedDateTimeUtilities(Calendar.getInstance(TimeZone.getTimeZone(CUR)));
		testZonedDateTimeUtilities(Instant.now());
		testZonedDateTimeUtilities(ZonedDateTime.now());
		testZonedDateTimeUtilities(ZonedDateTime.now(UTC));
		testZonedDateTimeUtilities(ZonedDateTime.now(ZP2));
		testZonedDateTimeUtilities(ZonedDateTime.now(CUR));
		testZonedDateTimeUtilities(OffsetDateTime.now());
		testZonedDateTimeUtilities(OffsetDateTime.now(UTC));
		testZonedDateTimeUtilities(OffsetDateTime.now(ZP2));
		testZonedDateTimeUtilities(OffsetDateTime.now(CUR));
		testZonedDateTimeUtilities(LocalDateTime.now());
		testZonedDateTimeUtilities(LocalDateTime.now(UTC));
		testZonedDateTimeUtilities(LocalDateTime.now(ZP2));
		testZonedDateTimeUtilities(LocalDateTime.now(CUR));
		testZonedDateTimeUtilities(LocalDate.now());
		testZonedDateTimeUtilities(LocalDate.now(UTC));
		testZonedDateTimeUtilities(LocalDate.now(ZP2));
		testZonedDateTimeUtilities(LocalDate.now(CUR));
		testZonedDateTimeUtilities(OffsetTime.now());
		testZonedDateTimeUtilities(OffsetTime.now(UTC));
		testZonedDateTimeUtilities(OffsetTime.now(ZP2));
		testZonedDateTimeUtilities(OffsetTime.now(CUR));
		testZonedDateTimeUtilities(LocalTime.now());
		testZonedDateTimeUtilities(LocalTime.now(UTC));
		testZonedDateTimeUtilities(LocalTime.now(ZP2));
		testZonedDateTimeUtilities(LocalTime.now(CUR));
	}

	private static <D> void testZonedDateTimeUtilities(D date) {
		ZonedDateTime zonedDateTime = Utils.toZonedDateTime(date);
		D thisShouldBeExactlyTheSameAsOriginal = Utils.fromZonedDateTime(zonedDateTime, date.getClass());
		assertEquals("Date conversion is repeatable", date, thisShouldBeExactlyTheSameAsOriginal);
	}

	@Test
	public void testFormatDate() {
		ZonedDateTime myBirthDateAsZonedDateTime = ZonedDateTime.of(1978, 3, 26, 12, 35, 0, 0, UTC);
		long myBirthDateAsEpochMilli = myBirthDateAsZonedDateTime.toInstant().toEpochMilli();
		TimeZone.setDefault(TimeZone.getTimeZone(UTC));

		testFormatDate(new java.sql.Date(myBirthDateAsEpochMilli), null);
		testFormatDate(new java.sql.Date(myBirthDateAsEpochMilli), ZP2);
		testFormatDate(new java.sql.Date(myBirthDateAsEpochMilli), CUR);
		testFormatDate(new java.sql.Time(myBirthDateAsEpochMilli), null);
		testFormatDate(new java.sql.Time(myBirthDateAsEpochMilli), ZP2);
		testFormatDate(new java.sql.Time(myBirthDateAsEpochMilli), CUR);
		testFormatDate(new java.sql.Timestamp(myBirthDateAsEpochMilli), null);
		testFormatDate(new java.sql.Timestamp(myBirthDateAsEpochMilli), ZP2);
		testFormatDate(new java.sql.Timestamp(myBirthDateAsEpochMilli), CUR);
		testFormatDate(new java.util.Date(myBirthDateAsEpochMilli), null);
		testFormatDate(new java.util.Date(myBirthDateAsEpochMilli), ZP2);
		testFormatDate(new java.util.Date(myBirthDateAsEpochMilli), CUR);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(myBirthDateAsEpochMilli);
		testFormatDate(calendar, null);
		testFormatDate(calendar, ZP2);
		testFormatDate(calendar, CUR);
		testFormatDate(myBirthDateAsZonedDateTime.toInstant(), null);
		testFormatDate(myBirthDateAsZonedDateTime.toInstant(), ZP2);
		testFormatDate(myBirthDateAsZonedDateTime.toInstant(), CUR);
		testFormatDate(myBirthDateAsZonedDateTime, null);
		testFormatDate(myBirthDateAsZonedDateTime, ZP2);
		testFormatDate(myBirthDateAsZonedDateTime, CUR);
		testFormatDate(myBirthDateAsZonedDateTime.toOffsetDateTime(), null);
		testFormatDate(myBirthDateAsZonedDateTime.toOffsetDateTime(), ZP2);
		testFormatDate(myBirthDateAsZonedDateTime.toOffsetDateTime(), CUR);
		testFormatDate(myBirthDateAsZonedDateTime.toLocalDateTime(), null);
		testFormatDate(myBirthDateAsZonedDateTime.toLocalDateTime(), ZP2);
		testFormatDate(myBirthDateAsZonedDateTime.toLocalDateTime(), CUR);
		testFormatDate(myBirthDateAsZonedDateTime.toLocalDate(), null);
		testFormatDate(myBirthDateAsZonedDateTime.toLocalDate(), ZP2);
		testFormatDate(myBirthDateAsZonedDateTime.toLocalDate(), CUR);
		testFormatDate(myBirthDateAsZonedDateTime.toOffsetDateTime().toOffsetTime(), null);
		testFormatDate(myBirthDateAsZonedDateTime.toOffsetDateTime().toOffsetTime(), ZP2);
		testFormatDate(myBirthDateAsZonedDateTime.toOffsetDateTime().toOffsetTime(), CUR);
		testFormatDate(myBirthDateAsZonedDateTime.toLocalTime(), null);
		testFormatDate(myBirthDateAsZonedDateTime.toLocalTime(), ZP2);
		testFormatDate(myBirthDateAsZonedDateTime.toLocalTime(), CUR);
	}

	private static <D, Z> void testFormatDate(D date, Z zone) {
		String pattern = "yyyy-MM-dd'T'HH:mm:ss";
		String expectedDate = "1978-03-26";
		String expectedTime = "12:35:00";

		String actualResult;

		if (zone == null) {
			actualResult = Dates.formatDate(date, pattern);
		}
		else {
			actualResult = Dates.formatDateWithTimezone(date, pattern, zone);
			expectedTime = LocalTime.parse(expectedTime).atDate(LocalDate.now()).atZone(UTC).withZoneSameInstant(Utils.toZoneId(zone)).toLocalTime().format(ISO_TIME);
		}

		String expectedResult;

		if (date instanceof OffsetTime || date instanceof LocalTime) {
			expectedResult = LocalDate.now().format(ISO_DATE) + "T" + expectedTime;
		}
		else if (date instanceof LocalDate) {
			expectedResult = LocalDate.parse(expectedDate).atStartOfDay(UTC).withZoneSameInstant(Utils.toZoneId(zone)).toLocalDateTime().format(ISO_DATE_TIME);
		}
		else {
			expectedResult = expectedDate + "T" + expectedTime;
		}

		assertEquals("Date format", expectedResult, actualResult);
	}

	@Test
	public void testAddWeeks() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime expectedResult = now.plusWeeks(10);
		LocalDateTime actualResult = Dates.addWeeks(now, 10);
		assertEquals("10 weeks have been added", expectedResult, actualResult);
	}

	@Test
	public void testWeeksBetween() {
		LocalDateTime start = LocalDateTime.now();
		LocalDateTime end = start.plusWeeks(10);
		int weeksBetween = Dates.weeksBetween(start, end);
		assertEquals("Diff is 10 weeks", 10, weeksBetween);

		end = end.minusDays(1);
		weeksBetween = Dates.weeksBetween(start, end);
		assertEquals("Diff is 9 weeks", 9, weeksBetween);
	}

	@Test
	public void testAddMinutes() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime expectedResult = now.plusMinutes(10);
		LocalDateTime actualResult = Dates.addMinutes(now, 10);
		assertEquals("10 minutes have been added", expectedResult, actualResult);
	}

	@Test
	public void testMinutesBetween() {
		LocalDateTime start = LocalDateTime.now();
		LocalDateTime end = start.plusMinutes(10);
		long minutesBetween = Dates.minutesBetween(start, end);
		assertEquals("Diff is 10 minutes", 10, minutesBetween);

		end = end.minusNanos(1);
		minutesBetween = Dates.minutesBetween(start, end);
		assertEquals("Diff is 9 minutes", 9, minutesBetween);
	}

	@Test
	public void testMonths() {
		assertEquals("There are 12 months", 12, Dates.getMonths().size());

		int index = 1;
		for (Entry<String, Integer> month : Dates.getMonths().entrySet()) {
			assertEquals("Month index", index++, month.getValue().intValue());
		}
	}

	@Test
	public void testDaysOfWeek() {
		assertEquals("There are 7 days of week", 7, Dates.getDaysOfWeek().size());

		int index = 1;
		for (Entry<String, Integer> dayOfWeek : Dates.getDaysOfWeek().entrySet()) {
			assertEquals("Day of week index", index++, dayOfWeek.getValue().intValue());
		}
	}

}
