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
package org.omnifaces.facesviews;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.omnifaces.facesviews.FacesViewsViewHandler.replaceFirstLiteral;

import org.junit.jupiter.api.Test;

class FacesViewsViewHandlerTest {

    @Test
    void testReplaceFirstLiteral() {
        // Basic replacement of the view id portion within a context-path-prefixed action URL.
        assertEquals("/ctx/foo/bar", replaceFirstLiteral("/ctx/Foo/Bar", "/Foo/Bar", "/foo/bar"));

        // Target absent leaves the value unchanged.
        assertEquals("/ctx/Foo/Bar", replaceFirstLiteral("/ctx/Foo/Bar", "/Baz", "/baz"));

        // Only the first occurrence is replaced (protects a coincidental match in a query string suffix).
        assertEquals("/foo?redirect=/Foo", replaceFirstLiteral("/Foo?redirect=/Foo", "/Foo", "/foo"));
    }

    @Test
    void testReplaceFirstLiteralHandlesRegexMetacharacters() {
        // Target is literal, not a regex: metacharacters must match themselves.
        assertEquals("/ctx/a+b(c)/view", replaceFirstLiteral("/ctx/a+b(c)/View", "/a+b(c)/View", "/a+b(c)/view"));

        // A '.' in the target must not act as a wildcard, so a non-matching path stays untouched.
        assertEquals("/ctx/aXbc/rest", replaceFirstLiteral("/ctx/aXbc/rest", "/a.bc", "/z"));

        // Replacement is literal too: '$' and '\' must not be interpreted as group references or escapes.
        assertEquals("/ctx/f$0o\\1", replaceFirstLiteral("/ctx/FOO", "/FOO", "/f$0o\\1"));
    }

}
