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

    @Test
    void testReplaceFirstLiteralHandlesRegexMetacharacters() {
        // Target is literal, not a regex: metacharacters must match themselves.
        assertEquals("/ctx/a+b(c)/view", Utils.replaceFirstLiteral("/ctx/a+b(c)/View", "/a+b(c)/View", "/a+b(c)/view"));

        // A '.' in the target must not act as a wildcard, so a non-matching value stays untouched.
        assertEquals("/ctx/aXbc/rest", Utils.replaceFirstLiteral("/ctx/aXbc/rest", "/a.bc", "/z"));

        // Replacement is literal too: '$' and '\' must not be interpreted as group references or escapes.
        assertEquals("/ctx/f$0o\\1", Utils.replaceFirstLiteral("/ctx/FOO", "/FOO", "/f$0o\\1"));
    }

}
