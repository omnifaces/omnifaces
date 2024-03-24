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
package org.omnifaces.test.cdi.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.omnifaces.cdi.config.DateProducer;
import org.omnifaces.cdi.config.DateProducer.TemporalDate;

public class TestDateProducer {

    private static TimeZone originalTimeZone;
    private static ZoneOffset testZoneOffset;

    @BeforeAll
    public static void setTestTimeZone() {
        originalTimeZone = TimeZone.getDefault();
        testZoneOffset = ZoneOffset.ofHours(-3);
        TimeZone.setDefault(TimeZone.getTimeZone(testZoneOffset));
    }

    @AfterAll
    public static void restoreTestTimeZone() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    void testNow() {
        Date oldNow = new Date();
        TemporalDate newNow = new DateProducer.TemporalDate();
        String oldNowAsString = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(oldNow);
        String newNowAsString = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(newNow);

        assertEquals(oldNowAsString, newNowAsString, "format");
        assertEquals(testZoneOffset, ZoneOffset.ofTotalSeconds(newNow.get(ChronoField.OFFSET_SECONDS)), "offset");
    }

    @Test
    void testZonedDateTime() {
        Date oldNow = new Date();
        ZonedDateTime newNowAsZoned = new DateProducer.TemporalDate().getZonedDateTime();
        String oldNowAsString = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(oldNow);
        String newNowAsString = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(newNowAsZoned);

        assertEquals(oldNowAsString, newNowAsString, "format");
        assertEquals(testZoneOffset, newNowAsZoned.getOffset(), "offset");
    }

    @Test
    void testInstant() {
        Date oldNow = new Date();
        Instant newNowAsInstant = new DateProducer.TemporalDate().getInstant();
        long oldTime = oldNow.getTime();
        long newTime = newNowAsInstant.toEpochMilli();
        long timeDiff = Math.abs(oldTime - newTime);

        assertTrue(timeDiff < TimeUnit.SECONDS.toMillis(1), timeDiff + " is less than 1s");
    }

    @Test
    void testTime() {
        Date oldNow = new Date();
        TemporalDate newNow = new DateProducer.TemporalDate();
        long oldTime = oldNow.getTime();
        long newTime = newNow.getTime();
        long timeDiff = Math.abs(oldTime - newTime);

        assertTrue(timeDiff < TimeUnit.SECONDS.toMillis(1), timeDiff + " is less than 1s");
    }

}
