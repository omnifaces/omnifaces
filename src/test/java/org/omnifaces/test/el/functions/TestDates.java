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
package org.omnifaces.test.el.functions;

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_DATE;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.time.format.DateTimeFormatter.ISO_TIME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Map.Entry;
import java.util.TimeZone;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.omnifaces.cdi.config.DateProducer;
import org.omnifaces.el.functions.Dates;
import org.omnifaces.util.Utils;

public class TestDates {

	private static final ZonedDateTime MY_BIRTH_DATE = ZonedDateTime.of(1978, 3, 26, 12, 35, 0, 0, UTC);
	private static final ZoneId ZP2 = ZoneOffset.ofHours(2);
	private static final ZoneId CUR = ZoneId.of("America/Curacao");

	@BeforeAll
	public static void init() {
		TimeZone.setDefault(TimeZone.getTimeZone(UTC));
	}

	@Test
	public void testZonedDateTimeUtilitiesWithDate() {
		testZonedDateTimeUtilities(new java.sql.Date(System.currentTimeMillis()));
		testZonedDateTimeUtilities(new java.sql.Time(System.currentTimeMillis()));
		testZonedDateTimeUtilities(new java.sql.Timestamp(System.currentTimeMillis()));
		testZonedDateTimeUtilities(new java.util.Date());
	}

	@Test
	public void testZonedDateTimeUtilitiesWithCalendar() {
		testZonedDateTimeUtilities(Calendar.getInstance());
		testZonedDateTimeUtilities(Calendar.getInstance(TimeZone.getTimeZone(UTC)));
		testZonedDateTimeUtilities(Calendar.getInstance(TimeZone.getTimeZone(ZP2)));
		testZonedDateTimeUtilities(Calendar.getInstance(TimeZone.getTimeZone(CUR)));
	}

	@Test
	public void testZonedDateTimeUtilitiesWithInstant() {
		testZonedDateTimeUtilities(Instant.now());
	}

	@Test
	public void testZonedDateTimeUtilitiesWithZonedDateTime() {
		testZonedDateTimeUtilities(ZonedDateTime.now());
		testZonedDateTimeUtilities(ZonedDateTime.now(UTC));
		testZonedDateTimeUtilities(ZonedDateTime.now(ZP2));
		testZonedDateTimeUtilities(ZonedDateTime.now(CUR));
	}

	@Test
	public void testZonedDateTimeUtilitiesWithOffsetDateTime() {
		testZonedDateTimeUtilities(OffsetDateTime.now());
		testZonedDateTimeUtilities(OffsetDateTime.now(UTC));
		testZonedDateTimeUtilities(OffsetDateTime.now(ZP2));
		testZonedDateTimeUtilities(OffsetDateTime.now(CUR));
	}

	@Test
	public void testZonedDateTimeUtilitiesWithLocalDateTime() {
		testZonedDateTimeUtilities(LocalDateTime.now());
		testZonedDateTimeUtilities(LocalDateTime.now(UTC));
		testZonedDateTimeUtilities(LocalDateTime.now(ZP2));
		testZonedDateTimeUtilities(LocalDateTime.now(CUR));
	}

	@Test
	public void testZonedDateTimeUtilitiesWithLocalDate() {
		testZonedDateTimeUtilities(LocalDate.now());
		testZonedDateTimeUtilities(LocalDate.now(UTC));
		testZonedDateTimeUtilities(LocalDate.now(ZP2));
		testZonedDateTimeUtilities(LocalDate.now(CUR));
	}

	@Test
	public void testZonedDateTimeUtilitiesWithYearMonth() {
		testZonedDateTimeUtilities(YearMonth.now());
		testZonedDateTimeUtilities(YearMonth.now(UTC));
		testZonedDateTimeUtilities(YearMonth.now(ZP2));
		testZonedDateTimeUtilities(YearMonth.now(CUR));
	}

	@Test
	public void testZonedDateTimeUtilitiesWithOffsetTime() {
		testZonedDateTimeUtilities(OffsetTime.now());
		testZonedDateTimeUtilities(OffsetTime.now(UTC));
		testZonedDateTimeUtilities(OffsetTime.now(ZP2));
		testZonedDateTimeUtilities(OffsetTime.now(CUR));
	}

	@Test
	public void testZonedDateTimeUtilitiesWithLocalTime() {
		testZonedDateTimeUtilities(LocalTime.now());
		testZonedDateTimeUtilities(LocalTime.now(UTC));
		testZonedDateTimeUtilities(LocalTime.now(ZP2));
		testZonedDateTimeUtilities(LocalTime.now(CUR));
	}

	@Test
	public void testZonedDateTimeUtilitiesWithTemporalDate() {
		testZonedDateTimeUtilities(new DateProducer().getNow());
	}

	private static <D> void testZonedDateTimeUtilities(D date) { // Used under the covers by these EL functions.
		ZonedDateTime zonedDateTime = Utils.toZonedDateTime(date);
		D thisShouldBeExactlyTheSameAsOriginal = Utils.fromZonedDateTime(zonedDateTime, date.getClass());
		assertEquals(date, thisShouldBeExactlyTheSameAsOriginal, "Date conversion is repeatable");
	}

	@Test
	public void testFormatDateWithDate() {
		long myBirthDateAsEpochMilli = MY_BIRTH_DATE.toInstant().toEpochMilli();
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
	}

	@Test
	public void testFormatDateWithCalendar() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(MY_BIRTH_DATE.toInstant().toEpochMilli());
		testFormatDate(calendar, null);
		testFormatDate(calendar, ZP2);
		testFormatDate(calendar, CUR);
	}

	@Test
	public void testFormatDateWithInstant() {
		testFormatDate(MY_BIRTH_DATE.toInstant(), null);
		testFormatDate(MY_BIRTH_DATE.toInstant(), ZP2);
		testFormatDate(MY_BIRTH_DATE.toInstant(), CUR);
	}

	@Test
	public void testFormatDateWithZonedDateTime() {
		testFormatDate(MY_BIRTH_DATE, null);
		testFormatDate(MY_BIRTH_DATE, ZP2);
		testFormatDate(MY_BIRTH_DATE, CUR);
	}

	@Test
	public void testFormatDateWithOffsetDateTime() {
		testFormatDate(MY_BIRTH_DATE.toOffsetDateTime(), null);
		testFormatDate(MY_BIRTH_DATE.toOffsetDateTime(), ZP2);
		testFormatDate(MY_BIRTH_DATE.toOffsetDateTime(), CUR);
	}

	@Test
	public void testFormatDateWithLocalDateTime() {
		testFormatDate(MY_BIRTH_DATE.toLocalDateTime(), null);
		testFormatDate(MY_BIRTH_DATE.toLocalDateTime(), ZP2);
		testFormatDate(MY_BIRTH_DATE.toLocalDateTime(), CUR);
	}

	@Test
	public void testFormatDateWithLocalDate() {
		testFormatDate(MY_BIRTH_DATE.toLocalDate(), null);
		testFormatDate(MY_BIRTH_DATE.toLocalDate(), ZP2);
		testFormatDate(MY_BIRTH_DATE.toLocalDate(), CUR);
	}

	@Test
	public void testFormatDateWithOffsetTime() {
		testFormatDate(MY_BIRTH_DATE.toOffsetDateTime().toOffsetTime(), null);
		testFormatDate(MY_BIRTH_DATE.toOffsetDateTime().toOffsetTime(), ZP2);
		testFormatDate(MY_BIRTH_DATE.toOffsetDateTime().toOffsetTime(), CUR);
	}

	@Test
	public void testFormatDateWithLocalTime() {
		testFormatDate(MY_BIRTH_DATE.toLocalTime(), null);
		testFormatDate(MY_BIRTH_DATE.toLocalTime(), ZP2);
		testFormatDate(MY_BIRTH_DATE.toLocalTime(), CUR);
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

		assertEquals(expectedResult, actualResult, "Date format");
	}

	@Test
	public void testAddWeeks() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime expectedResult = now.plusWeeks(10);
		LocalDateTime actualResult = Dates.addWeeks(now, 10);
		assertEquals(expectedResult, actualResult, "10 weeks have been added");
	}

	@Test
	public void testWeeksBetween() {
		LocalDateTime start = LocalDateTime.now();
		LocalDateTime end = start.plusWeeks(10);
		int weeksBetween = Dates.weeksBetween(start, end);
		assertEquals(10, weeksBetween, "Diff is 10 weeks");

		end = end.minusDays(1);
		weeksBetween = Dates.weeksBetween(start, end);
		assertEquals(9, weeksBetween, "Diff is 9 weeks");
	}

	@Test
	public void testAddMinutes() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime expectedResult = now.plusMinutes(10);
		LocalDateTime actualResult = Dates.addMinutes(now, 10);
		assertEquals(expectedResult, actualResult, "10 minutes have been added");
	}

	@Test
	public void testMinutesBetween() {
		LocalDateTime start = LocalDateTime.now();
		LocalDateTime end = start.plusMinutes(10);
		long minutesBetween = Dates.minutesBetween(start, end);
		assertEquals(10, minutesBetween, "Diff is 10 minutes");

		end = end.minusNanos(1);
		minutesBetween = Dates.minutesBetween(start, end);
		assertEquals(9, minutesBetween, "Diff is 9 minutes");
	}

	@Test
	public void testMonths() {
		assertEquals(12, Dates.getMonths().size(), "There are 12 months");

		int index = 1;
		for (Entry<String, Integer> month : Dates.getMonths().entrySet()) {
			assertEquals(index++, month.getValue().intValue(), "Month index");
		}
	}

	@Test
	public void testDaysOfWeek() {
		assertEquals(7, Dates.getDaysOfWeek().size(), "There are 7 days of week");

		int index = 1;
		for (Entry<String, Integer> dayOfWeek : Dates.getDaysOfWeek().entrySet()) {
			assertEquals(index++, dayOfWeek.getValue().intValue(), "Day of week index");
		}
	}

}
