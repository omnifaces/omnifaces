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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.omnifaces.el.functions.Strings;

class StringsTest {

    @Test
    void testPrettyURL() {
        var textWithDiacritics = "TO͇̹ͅNȳ̳ TH̘Ë͖́̉ ͠P̯͍̭O̚N̐Y̡ H̸̡̪̯ͨ͊̽̅̾̎Ȩ̬̩̾͛ͪ̈́̀́͘ ̶̧̨̱̹̭̯ͧ̾ͬC̷̙̲̝͖ͭ̏ͥͮ͟Oͮ͏̮̪̝͍M̲̖͊̒ͪͩͬ̚̚͜Ȇ̴̟̟͙̞ͩ͌͝S̨̥̫͎̭ͯ̿̔̀ͅ";
        var expectedText = "tony-the-pony-he-comes";

        assertEquals(expectedText, Strings.prettyURL(textWithDiacritics));
    }

    @Test
    void testStripTags() {
        var textWithTags = "<div><p>Text with <strong>lots</strong> of "
            + "<a href=\"http://example.com\" title=\"Link\">HTML</a> tags<br />"
            + "<img src=\"tags.jpg\">. Random math: <code>x/y with y > 0</code> "
            + "</p></div>";
        var expectedText = "Text with lots of HTML tags. Random math: x/y with y > 0";

        assertEquals(expectedText, Strings.stripTags(textWithTags));
    }

    @Test
    void testFlagEmoji_ValidCodes() {
        assertEquals("🇺🇸", Strings.flagEmoji("US"), "US should map to United States flag");
        assertEquals("🇳🇱", Strings.flagEmoji("nl"), "Lowercase nl should map to Netherlands flag");
        assertEquals("🇧🇷", Strings.flagEmoji("Br"), "Mixed case Br should map to Brazil flag");
    }

    @Test
    void testFlagEmoji_NullAndEmpty() {
        assertNull(Strings.flagEmoji(null), "Null input should return null");
        assertNull(Strings.flagEmoji(""), "Empty string should return null");
    }

    @ParameterizedTest
    @ValueSource(strings = { "A", "USA", "12", "U!", "  " })
    void testFlagEmoji_InvalidFormat_ShouldThrowException(String invalidCode) {
        assertThrows(
            IllegalArgumentException.class, () -> Strings.flagEmoji(invalidCode),
            "Invalid format or non-letter code should throw IllegalArgumentException"
        );
    }

    /**
     * This test ensures that the 'Turkish I' problem doesn't break the emoji math. In Turkish locale, "i".toUpperCase() is "İ" (U+0130), not "I".
     */
    @Test
    void testFlagEmoji_TurkishLocaleCompatibility() {
        var originalDefaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            assertEquals("🇮🇳", Strings.flagEmoji("in"), "Should work correctly even in Turkish locale");
        }
        finally {
            Locale.setDefault(originalDefaultLocale);
        }
    }

}
