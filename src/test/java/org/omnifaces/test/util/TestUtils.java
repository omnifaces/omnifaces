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

class TestUtils {

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
        for (String originalString : URL_SAFE_SERIALIZATION_EXAMPLES) {
            var serializedString = Utils.serializeURLSafe(originalString);
            assertTrue(!serializedString.contains("="), "The serialized string '" + serializedString + "' based on '" + originalString + "' may not contain the '=' character");
            var unserializedString = Utils.unserializeURLSafe(serializedString);
            assertEquals(originalString, unserializedString, "The unserialized string must exactly match the original string '" + originalString + "'");
        }
    }

    void testEncodeURI() {
        assertEquals(RFC_3986_RESERVED_CHARACTERS_ENCODED, Utils.encodeURI(RFC_3986_RESERVED_CHARACTERS));
        assertEquals(RFC_3986_UNRESERVED_CHARACTERS, Utils.encodeURI(RFC_3986_UNRESERVED_CHARACTERS));
    }

}
