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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class DynamicRoutesTest {

    @Test
    void testDynamicSegmentIsExposedByName() {
        var routes = new DynamicRoutes();
        routes.add("/organizations/[id]/members", "/organizations/[id]/members.xhtml", false, false);

        var match = routes.match("/organizations/123/members");
        assertEquals("/organizations/[id]/members.xhtml", match.viewId());
        assertEquals(Map.of("id", "123"), match.params());
        assertNull(match.pathInfo());
    }

    @Test
    void testWelcomeFileAnswersForItsOwnDirectory() {
        var routes = new DynamicRoutes();
        routes.add("/organizations/[id]/index", "/organizations/[id]/index.xhtml", false, true);

        var match = routes.match("/organizations/123");
        assertEquals("/organizations/[id]/index.xhtml", match.viewId());
        assertEquals(Map.of("id", "123"), match.params());

        var explicit = routes.match("/organizations/123/index");
        assertEquals("/organizations/[id]/index.xhtml", explicit.viewId());
        assertEquals(Map.of("id", "123"), explicit.params());
    }

    /**
     * Only a directory name declares a dynamic route segment, so a bracketed file name is scanned literally and does not make the view a dynamic route.
     */
    @Test
    void testBracketedFileNameIsNotADynamicRoute() {
        assertFalse(DynamicRoutes.isDynamicRoute("/organizations/[id]"));
        assertTrue(DynamicRoutes.isDynamicRoute("/organizations/[id]/members"));
    }

    @Test
    void testNearestStaticAncestorIsExposedAsFilterPrefix() {
        var routes = new DynamicRoutes();
        routes.add("/organizations/[id]/members", "/organizations/[id]/members.xhtml", false, false);
        routes.add("/[locale]/products/[sku]/index", "/[locale]/products/[sku]/index.xhtml", false, true);

        assertEquals(Set.of("/organizations/*", "/*"), routes.getFilterPrefixes());
    }

    @Test
    void testMultipleDynamicSegmentsAreExposedByTheirOwnName() {
        var routes = new DynamicRoutes();
        routes.add("/[locale]/products/[sku]/index", "/[locale]/products/[sku]/index.xhtml", false, true);

        var match = routes.match("/nl/products/12345");
        assertEquals(Map.of("locale", "nl", "sku", "12345"), match.params());
    }

    /**
     * A literal child is preferred over a dynamic child at every level, which is exactly what makes a literal sibling unreachable as a value of the dynamic
     * segment.
     */
    @Test
    void testLiteralSegmentIsPreferredOverDynamicSegment() {
        var routes = new DynamicRoutes();
        routes.add("/a/[x]/p", "/a/[x]/p.xhtml", false, false);
        routes.add("/a/lit/[y]/p", "/a/lit/[y]/p.xhtml", false, false);

        var match = routes.match("/a/lit/5/p");
        assertEquals("/a/lit/[y]/p.xhtml", match.viewId());
        assertEquals(Map.of("y", "5"), match.params());
    }

    /**
     * When the preferred literal branch does not consume the whole path, the walk backtracks and retries the same segment as a dynamic one.
     */
    @Test
    void testFailedLiteralBranchBacktracksToDynamicSegment() {
        var routes = new DynamicRoutes();
        routes.add("/a/[x]/p", "/a/[x]/p.xhtml", false, false);
        routes.add("/a/lit/[y]/q", "/a/lit/[y]/q.xhtml", false, false);

        var match = routes.match("/a/lit/p");
        assertEquals("/a/[x]/p.xhtml", match.viewId());
        assertEquals(Map.of("x", "lit"), match.params());
    }

    /**
     * A dynamic route and MultiViews compose: the named prefix resolves as usual and the unconsumed remainder becomes path info.
     */
    @Test
    void testMultiViewsViewSwallowsTheRemainderAsPathInfo() {
        var routes = new DynamicRoutes();
        routes.add("/[locale]/products/[sku]/reviews", "/[locale]/products/[sku]/reviews.xhtml", true, false);

        var match = routes.match("/nl/products/12345/reviews/2");
        assertEquals("/[locale]/products/[sku]/reviews.xhtml", match.viewId());
        assertEquals(Map.of("locale", "nl", "sku", "12345"), match.params());
        assertEquals("/2", match.pathInfo());
    }

    @Test
    void testNonMultiViewsViewRequiresThePathToBeFullyConsumed() {
        var routes = new DynamicRoutes();
        routes.add("/organizations/[id]/members", "/organizations/[id]/members.xhtml", false, false);

        assertNull(routes.match("/organizations/123/members/extra"));
    }

    @Test
    void testUnresolvablePathDoesNotMatch() {
        var routes = new DynamicRoutes();
        routes.add("/organizations/[id]/members", "/organizations/[id]/members.xhtml", false, false);

        assertNull(routes.match("/organizations/123"));
        assertNull(routes.match("/elsewhere/123/members"));
        assertNull(routes.match("/organizations"));
    }

    @Test
    void testSiblingDynamicSegmentsWithDifferentNamesAreRejected() {
        var routes = new DynamicRoutes();
        routes.add("/organizations/[id]/members", "/organizations/[id]/members.xhtml", false, false);

        assertThrows(IllegalStateException.class, () -> routes.add("/organizations/[slug]/other", "/organizations/[slug]/other.xhtml", false, false));
    }

    @Test
    void testSameDynamicSegmentNameTwiceInOnePathIsRejected() {
        var routes = new DynamicRoutes();

        assertThrows(IllegalStateException.class, () -> routes.add("/[id]/foo/[id]/bar", "/[id]/foo/[id]/bar.xhtml", false, false));
    }

    @Test
    void testMalformedBracketPairIsRejected() {
        var routes = new DynamicRoutes();

        assertThrows(IllegalStateException.class, () -> routes.add("/organizations/[id/members", "/organizations/[id/members.xhtml", false, false));
        assertThrows(IllegalStateException.class, () -> routes.add("/organizations/]id[/members", "/organizations/]id[/members.xhtml", false, false));
        assertThrows(IllegalStateException.class, () -> routes.add("/organizations/[]/members", "/organizations/[]/members.xhtml", false, false));
    }

    @Test
    void testInterpolationSubstitutesEverySegmentByName() {
        assertEquals("/organizations/123/members", DynamicRoutes.interpolate("/organizations/[id]/members", Map.of("id", "123")));
        assertEquals("/ctx/nl/products/12345/", DynamicRoutes.interpolate("/ctx/[locale]/products/[sku]/", Map.of("locale", "nl", "sku", "12345")));
        assertEquals("/organizations/members", DynamicRoutes.interpolate("/organizations/members", Map.of()));
    }

    /**
     * A welcome file renders with a trailing slash, so the segment before it is the last one and must still be substituted.
     */
    @Test
    void testInterpolationPreservesTheTrailingSlash() {
        assertEquals("/organizations/123/", DynamicRoutes.interpolate("/organizations/[id]/", Map.of("id", "123")));
    }

    @Test
    void testInterpolationEncodesTheSegmentValue() {
        assertEquals("/organizations/a%20b/members", DynamicRoutes.interpolate("/organizations/[id]/members", Map.of("id", "a b")));
    }

    /**
     * Square brackets are gen-delims per RFC 3986 which containers may refuse outright, so an unresolvable segment must fail loudly rather than render.
     */
    @Test
    void testInterpolationRejectsAMissingSegmentValue() {
        assertThrows(IllegalArgumentException.class, () -> DynamicRoutes.interpolate("/organizations/[id]/members", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> DynamicRoutes.interpolate("/organizations/[id]/members", null));
    }

    /**
     * An empty value would render an empty path segment, which resolves to a different URL than intended, so it is as unusable as a missing one.
     */
    @Test
    void testInterpolationRejectsAnEmptySegmentValue() {
        assertThrows(IllegalArgumentException.class, () -> DynamicRoutes.interpolate("/organizations/[id]/members", Map.of("id", "")));
    }

    @Test
    void testOnlyPathsWithBracketedSegmentsAreDynamicRoutes() {
        assertTrue(DynamicRoutes.isDynamicRoute("/organizations/[id]/members"));
        assertTrue(DynamicRoutes.isDynamicRoute("/[locale]/products/[sku]/index"));
        assertTrue(DynamicRoutes.isDynamicRoute("/a/[x]/[y]/b"));
        assertFalse(DynamicRoutes.isDynamicRoute("/organizations/settings/members"));
        assertFalse(DynamicRoutes.isDynamicRoute("/index"));
    }

    /**
     * The brackets must wrap a whole directory name, so a partially bracketed segment, an empty pair, or a bracketed last segment are all literal.
     */
    @Test
    void testPartiallyBracketedSegmentIsNotADynamicRoute() {
        assertFalse(DynamicRoutes.isDynamicRoute("/a/[x]y/b"));
        assertFalse(DynamicRoutes.isDynamicRoute("/a/y[x]/b"));
        assertFalse(DynamicRoutes.isDynamicRoute("/a/[]/b"));
        assertFalse(DynamicRoutes.isDynamicRoute("/[x]"));
        assertFalse(DynamicRoutes.isDynamicRoute(""));
    }

}
