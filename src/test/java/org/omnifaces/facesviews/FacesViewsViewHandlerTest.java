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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Covers {@link FacesViewsViewHandler#buildPathParamsURL(String, String, boolean, Map, boolean, List)}, which is where a dynamic route and a MultiViews view
 * interact and therefore where the URL is easiest to get wrong.
 */
class FacesViewsViewHandlerTest {

    @Test
    void testPlainViewIsRenderedAsIs() {
        assertEquals("/ctx/foo", buildURL("/ctx/foo", null, false, emptyMap(), false, emptyList()));
    }

    @Test
    void testMultiViewsAppendsTheUnnamedPathParams() {
        assertEquals("/ctx/foo/bar/42", buildURL("/ctx/foo", null, false, emptyMap(), true, List.of("bar", "42")));
    }

    /**
     * The URL rendered by the wrapped view handler still carries the path info of the current request, which the newly supplied path parameters replace rather
     * than extend.
     */
    @Test
    void testMultiViewsReplacesThePathInfoOfTheCurrentRequest() {
        assertEquals("/ctx/foo/new", buildURL("/ctx/foo/old", "/old", false, emptyMap(), true, List.of("new")));
    }

    @Test
    void testDynamicRouteSubstitutesTheNamedSegments() {
        assertEquals("/ctx/organizations/123/members", buildURL("/ctx/organizations/[id]/members", null, true, Map.of("id", "123"), false, emptyList()));
    }

    /**
     * A dynamic route which is also a MultiViews view substitutes its named segments and appends the unnamed path parameters, and must not retain the path info
     * of the current request while doing so.
     */
    @Test
    void testDynamicRouteAndMultiViewsCompose() {
        assertEquals(
            "/ctx/nl/products/999/reviews/9",
            buildURL("/ctx/[locale]/products/[sku]/reviews/2", "/2", true, Map.of("locale", "nl", "sku", "999"), true, List.of("9"))
        );
    }

    /**
     * Unnamed path parameters are meaningless on a view which is not a MultiViews view, so they are dropped rather than rendered into a URL that cannot
     * resolve.
     */
    @Test
    void testDynamicRouteWithoutMultiViewsDropsTheUnnamedPathParams() {
        assertEquals("/ctx/organizations/123/members", buildURL("/ctx/organizations/[id]/members", null, true, Map.of("id", "123"), false, List.of("9")));
    }

    @Test
    void testQueryStringAndFragmentArePreserved() {
        assertEquals(
            "/ctx/organizations/123/members?foo=bar#baz",
            buildURL("/ctx/organizations/[id]/members?foo=bar#baz", null, true, Map.of("id", "123"), false, emptyList())
        );
        assertEquals("/ctx/foo/42?foo=bar", buildURL("/ctx/foo?foo=bar", null, false, emptyMap(), true, List.of("42")));
    }

    @Test
    void testSegmentValuesAreEncoded() {
        assertEquals("/ctx/organizations/a%20b/members", buildURL("/ctx/organizations/[id]/members", null, true, Map.of("id", "a b"), false, emptyList()));
    }

    /**
     * A welcome file of a dynamic route renders with a trailing slash, so its segment is the last one and must still be substituted.
     */
    @Test
    void testWelcomeFileOfADynamicRouteIsSubstituted() {
        assertEquals("/ctx/organizations/123", buildURL("/ctx/organizations/[id]/", null, true, Map.of("id", "123"), false, emptyList()));
    }

    @Test
    void testUnsuppliedSegmentIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> buildURL("/ctx/organizations/[id]/members", null, true, emptyMap(), false, emptyList()));
        assertThrows(IllegalArgumentException.class, () -> buildURL("/ctx/organizations/[id]/members", null, true, emptyMap(), true, List.of("9")));
    }

    private static String buildURL(
        String bookmarkableURL, String requestPathInfo, boolean dynamicRoute, Map<String, String> namedPathParams, boolean multiViews, List<String> pathParams
    )
    {
        return FacesViewsViewHandler.buildPathParamsURL(bookmarkableURL, requestPathInfo, dynamicRoute, namedPathParams, multiViews, pathParams);
    }

}
