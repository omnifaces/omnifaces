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
package org.omnifaces.test.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.omnifaces.util.Utils;

class UtilsTest {

    private static final String[] URL_SAFE_SERIALIZATION_EXAMPLES = {
        "omnifaces:omnifaces.js", // This example normally generates 1-padded base64 encoded string
        "javax.faces:jsf.js|omnifaces:omnifaces.js", // This example normally generates 0-padded base64 encoded string
        "jakarta.faces:faces.js|omnifaces:omnifaces.js", // This example normally generates 2-padded base64 encoded string
    };

    private static final String RFC_3986_RESERVED_CHARACTERS = ":/?#[]@!$&'()*+,;=";
    private static final String RFC_3986_RESERVED_CHARACTERS_ENCODED = "%3A%2F%3F%23%5B%5D%40%21%24%26%27%28%29%2A%2B%2C%3B%3D";
    private static final String RFC_3986_UNRESERVED_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";

    @Test
    void testSerializeURLSafe() {
        for (var originalString : URL_SAFE_SERIALIZATION_EXAMPLES) {
            var serializedString = Utils.serializeURLSafe(originalString);
            assertTrue(
                !serializedString.contains("="),
                "The serialized string '" + serializedString + "' based on '" + originalString + "' may not contain the '=' character"
            );
            var unserializedString = Utils.unserializeURLSafe(serializedString);
            assertEquals(originalString, unserializedString, "The unserialized string must exactly match the original string '" + originalString + "'");
        }
    }

    void testEncodeURI() {
        assertEquals(RFC_3986_RESERVED_CHARACTERS_ENCODED, Utils.encodeURI(RFC_3986_RESERVED_CHARACTERS));
        assertEquals(RFC_3986_UNRESERVED_CHARACTERS, Utils.encodeURI(RFC_3986_UNRESERVED_CHARACTERS));
    }

    @Test
    void testReplaceFirstLiteral() {
        // Basic replacement of a substring within a context-path-prefixed value.
        assertEquals("/ctx/foo/bar", Utils.replaceFirstLiteral("/ctx/Foo/Bar", "/Foo/Bar", "/foo/bar"));

        // Target absent leaves the value unchanged.
        assertEquals("/ctx/Foo/Bar", Utils.replaceFirstLiteral("/ctx/Foo/Bar", "/Baz", "/baz"));

        // Only the first occurrence is replaced.
        assertEquals("/foo?redirect=/Foo", Utils.replaceFirstLiteral("/Foo?redirect=/Foo", "/Foo", "/foo"));
    }

    /**
     * Unlike {@link Utils#replaceFirstLiteral(String, String, String)} this one is anchored at the start, which is what a getter prefix needs.
     */
    @Test
    void testReplacePrefix() {
        // Basic prefix replacement.
        assertEquals("Budget", Utils.replacePrefix("getBudget", "get", ""));

        // A later occurrence of the very same substring must be left alone.
        assertEquals("Widgets", Utils.replacePrefix("getWidgets", "get", ""));
        assertEquals("isBudget", Utils.replacePrefix("isBudget", "get", ""));

        // Absent prefix leaves the value unchanged.
        assertEquals("isApproved", Utils.replacePrefix("isApproved", "get", ""));

        // Replacement is literal too.
        assertEquals("setValue", Utils.replacePrefix("getValue", "get", "set"));
    }

    @Test
    void testReplaceFirstLiteralHandlesRegexMetacharacters() {
        // Target is literal, not a regex: metacharacters must match themselves.
        assertEquals("/ctx/a+b(c)/view", Utils.replaceFirstLiteral("/ctx/a+b(c)/View", "/a+b(c)/View", "/a+b(c)/view"));

        // A '.' in the target must not act as a wildcard, so a non-matching value stays untouched.
        assertEquals("/ctx/aXbc/rest", Utils.replaceFirstLiteral("/ctx/aXbc/rest", "/a.bc", "/z"));

        // Replacement is literal too: '$' and '\' must not be interpreted as group references or escapes.
        assertEquals("/ctx/f$0o\\1", Utils.replaceFirstLiteral("/ctx/FOO", "/FOO", "/f$0o\\1"));
    }

    /**
     * The HTTP date of RFC 9110 is a fixed length format which requires a two digit day of month. This is why {@link Utils#formatRFC1123(java.util.Date)}
     * cannot use {@link java.time.format.DateTimeFormatter#RFC_1123_DATE_TIME}, as that one implements the more lenient RFC 1123 which also allows a single
     * digit day of month.
     */
    @Test
    void testFormatRFC1123PadsDayOfMonthToTwoDigits() {
        // 3 June 2026, i.e. a day of month below 10, must be emitted as "03" and not as "3".
        assertEquals("Wed, 03 Jun 2026 11:05:30 GMT", Utils.formatRFC1123(dateOf("2026-06-03T11:05:30Z")));

        // A day of month above 10 is unaffected.
        assertEquals("Sun, 28 Jun 2026 11:05:30 GMT", Utils.formatRFC1123(dateOf("2026-06-28T11:05:30Z")));
    }

    /**
     * {@link Utils#formatRFC1123(java.util.Date)} cannot use {@link java.util.Date#toInstant()} as {@link java.sql.Date} and {@link java.sql.Time} throw an
     * {@link UnsupportedOperationException} on it, while they are perfectly valid arguments, e.g. when reached via {@link org.omnifaces.util.Json} while
     * serializing a bean holding a JPA entity.
     */
    @Test
    void testFormatRFC1123AcceptsSqlDates() {
        var millis = dateOf("2026-06-03T11:05:30Z").getTime();
        var expected = "Wed, 03 Jun 2026 11:05:30 GMT";

        assertEquals(expected, Utils.formatRFC1123(new java.util.Date(millis)));
        assertEquals(expected, Utils.formatRFC1123(new java.sql.Date(millis)));
        assertEquals(expected, Utils.formatRFC1123(new java.sql.Time(millis)));
        assertEquals(expected, Utils.formatRFC1123(new java.sql.Timestamp(millis)));
    }

    /**
     * {@link Utils#parseRFC1123(String)} must keep accepting whatever {@link Utils#formatRFC1123(java.util.Date)} emits, as both are used on the very same HTTP
     * date headers.
     */
    @Test
    void testFormatRFC1123RoundTrips() throws ParseException {
        for (var text : new String[] { "1970-01-01T00:00:00Z", "2026-06-03T11:05:30Z", "2026-12-31T23:59:59Z" }) {
            var date = dateOf(text);
            assertEquals(date.getTime(), Utils.parseRFC1123(Utils.formatRFC1123(date)).getTime(), text);
        }
    }

    private static java.util.Date dateOf(String isoInstant) {
        return java.util.Date.from(Instant.parse(isoInstant));
    }

    /**
     * An OffsetTime carries its own offset, so the date it is paired with must be today in that offset rather than today in the default zone of the JVM. The
     * two offsets below are 26 hours apart and their local dates therefore always differ, so at any instant at least one of them differs from the default zone.
     */
    @Test
    void testToZonedDateTimeDatesAnOffsetTimeInItsOwnOffset() {
        for (var offset : List.of(ZoneOffset.ofHours(14), ZoneOffset.ofHours(-12))) {
            var zonedDateTime = Utils.toZonedDateTime(OffsetTime.of(LocalTime.NOON, offset));

            assertEquals(LocalDate.now(offset), zonedDateTime.toLocalDate(), "date at " + offset);
        }
    }

}
