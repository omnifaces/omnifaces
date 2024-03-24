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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.omnifaces.el.functions.Numbers;

class TestNumbers {

    private static final String[][] NUMBERS_FORMATTED_AS_THOUSANDS = {
        { "1.6666", "1.67" },
        { "999.4", "999" },
        { "999.5", "1k" },
        { "1004", "1k" },
        { "1005", "1.01k" },
        { "1594", "1.59k" },
        { "1595", "1.6k" },
        { "9000", "9k" },
        { "9900", "9.9k" },
        { "9994", "9.99k" },
        { "9995", "10k" },
        { "99990", "100k" },
        { "9994999", "9.99M" },
        { "9995000", "10M" },
        { "0E-30", "0" },
        { "-12858", "-12.9k" },
    };

    private static final String[][] NUMBERS_FORMATTED_AS_BYTES = {
        { "1023", "1023 B" },
        { "1024", "1.0 KiB" },
        { "1075", "1.0 KiB" },
        { "1076", "1.1 KiB" },
        { "500000", "488.3 KiB" },
        { "1048576", "1.0 MiB" }
    };

    @Test
    void testFormatThousandsInEnglish() {
        Locale originalLocale = Locale.getDefault();

        try {
            Locale.setDefault(Locale.ENGLISH);

            for (String[] numberFormattedAsThousands : NUMBERS_FORMATTED_AS_THOUSANDS) {
                BigDecimal number = new BigDecimal(numberFormattedAsThousands[0]);
                String expectedResult = numberFormattedAsThousands[1];
                String actualResult = Numbers.formatThousands(number);
                assertEquals(expectedResult, actualResult);
            }
        }
        finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    void testFormatThousandsInGerman() {
        Locale originalLocale = Locale.getDefault();

        try {
            Locale.setDefault(Locale.GERMAN);

            for (String[] numberFormattedAsThousands : NUMBERS_FORMATTED_AS_THOUSANDS) {
                BigDecimal number = new BigDecimal(numberFormattedAsThousands[0]);
                String expectedResult = numberFormattedAsThousands[1].replace('.', ',');
                String actualResult = Numbers.formatThousands(number);
                assertEquals(expectedResult, actualResult);
            }
        }
        finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    void testFormatBytesInEnglish() {
        Locale originalLocale = Locale.getDefault();

        try {
            Locale.setDefault(Locale.ENGLISH);

            for (String[] numberFormattedAsBytes : NUMBERS_FORMATTED_AS_BYTES) {
                Long number = Long.valueOf(numberFormattedAsBytes[0]);
                String expectedResult = numberFormattedAsBytes[1];
                String actualResult = Numbers.formatBytes(number);
                assertEquals(expectedResult, actualResult);
            }
        }
        finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    void testFormatBytesInGerman() {
        Locale originalLocale = Locale.getDefault();

        try {
            Locale.setDefault(Locale.GERMAN);

            for (String[] numberFormattedAsBytes : NUMBERS_FORMATTED_AS_BYTES) {
                Long number = Long.valueOf(numberFormattedAsBytes[0]);
                String expectedResult = numberFormattedAsBytes[1].replace('.', ',');
                String actualResult = Numbers.formatBytes(number);
                assertEquals(expectedResult, actualResult);
            }
        }
        finally {
            Locale.setDefault(originalLocale);
        }
    }

}
