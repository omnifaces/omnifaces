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

import org.junit.jupiter.api.Test;
import org.omnifaces.el.functions.Strings;

public class TestStrings {

    @Test
    void testPrettyURL() {
        String textWithDiacritics = "TO͇̹ͅNȳ̳ TH̘Ë͖́̉ ͠P̯͍̭O̚N̐Y̡ H̸̡̪̯ͨ͊̽̅̾̎Ȩ̬̩̾͛ͪ̈́̀́͘ ̶̧̨̱̹̭̯ͧ̾ͬC̷̙̲̝͖ͭ̏ͥͮ͟Oͮ͏̮̪̝͍M̲̖͊̒ͪͩͬ̚̚͜Ȇ̴̟̟͙̞ͩ͌͝S̨̥̫͎̭ͯ̿̔̀ͅ";
        String expectedText = "tony-the-pony-he-comes";

        assertEquals(expectedText, Strings.prettyURL(textWithDiacritics));
    }

    @Test
    void testStripTags() {
        String textWithTags = "<div><p>Text with <strong>lots</strong> of "
            + "<a href=\"http://example.com\" title=\"Link\">HTML</a> tags<br />"
            + "<img src=\"tags.jpg\">. Random math: <code>x/y with y > 0</code> "
            + "</p></div>";
        String expectedText = "Text with lots of HTML tags. Random math: x/y with y > 0";

        assertEquals(expectedText, Strings.stripTags(textWithTags));
    }

}