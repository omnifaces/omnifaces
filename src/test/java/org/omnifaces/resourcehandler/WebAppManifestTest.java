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
package org.omnifaces.resourcehandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.omnifaces.resourcehandler.WebAppManifest.findViewIdsMatchingURLs;

import java.util.Set;
import java.util.stream.Stream;

import jakarta.faces.application.ViewHandler;
import jakarta.faces.context.FacesContext;

import org.junit.jupiter.api.Test;

/**
 * Covers {@link WebAppManifest#findViewIdsMatchingURLs(FacesContext, ViewHandler, Set)}, which backs the cacheable view IDs of the service worker. It asks the
 * view handler for the URL of every scanned view, which is where a view below a dynamic route segment is fatal: it has none.
 */
class WebAppManifestTest {

    private static final String CONTEXT_PATH = "/ctx";
    private static final String WELCOME_FILE_URL = CONTEXT_PATH + "/";

    @Test
    void welcomeFileViewIsCollected() {
        assertEquals(Set.of("/index.xhtml"), findViewIds("/index.xhtml"));
    }

    @Test
    void viewWhichIsNoWelcomeFileIsSkipped() {
        assertEquals(Set.of("/index.xhtml"), findViewIds("/index.xhtml", "/about.xhtml"));
    }

    /**
     * The URL of such a view is only known within a request supplying a value for its segment, so the view handler throws when asked for one outside such a
     * request. It can never equal a welcome file URL either, so it is skipped before it is asked.
     */
    @Test
    void viewBelowDynamicRouteSegmentIsSkippedRatherThanAsked() {
        assertEquals(Set.of("/index.xhtml"), findViewIds("/index.xhtml", "/[organization]/index.xhtml"));
    }

    @Test
    void viewBelowDynamicRouteSegmentIsSkippedWhenItIsTheOnlyOne() {
        assertEquals(Set.of(), findViewIds("/[organization]/index.xhtml"));
    }

    private static Set<String> findViewIds(String... viewIds) {
        var context = mock(FacesContext.class);
        var viewHandler = mock(ViewHandler.class);
        when(viewHandler.getViews(any(), eq("/"))).thenReturn(Stream.of(viewIds));
        when(viewHandler.getActionURL(any(), any())).thenAnswer(invocation -> actionURL(invocation.getArgument(1)));
        return findViewIdsMatchingURLs(context, viewHandler, Set.of(WELCOME_FILE_URL));
    }

    /**
     * Mimics {@code FacesViewsViewHandler}, which refuses to render a URL still carrying an unsubstituted segment.
     */
    private static String actionURL(String viewId) {
        if (viewId.contains("[")) {
            throw new IllegalArgumentException("Dynamic route segment of view '" + viewId + "' has no value.");
        }

        return "/index.xhtml".equals(viewId) ? WELCOME_FILE_URL : CONTEXT_PATH + viewId;
    }

}
