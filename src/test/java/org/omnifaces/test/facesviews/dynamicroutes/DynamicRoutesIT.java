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
package org.omnifaces.test.facesviews.dynamicroutes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.omnifaces.test.OmniFacesIT.WebXml.withMultiViewsAndIndexWelcomeFile;

import java.util.regex.Pattern;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class DynamicRoutesIT extends OmniFacesIT {

    private static final Pattern PATTERN_BRACKETED_HREF = Pattern.compile("href=\"[^\"]*[\\[\\]]");

    @FindBy(id = "viewId")
    private WebElement viewId;

    @FindBy(id = "id")
    private WebElement id;

    @FindBy(id = "locale")
    private WebElement locale;

    @FindBy(id = "sku")
    private WebElement sku;

    @FindBy(id = "convertedSku")
    private WebElement convertedSku;

    @FindBy(id = "pathIndex0")
    private WebElement pathIndex0;

    @FindBy(id = "form")
    private WebElement form;

    @FindBy(id = "form:submit")
    private WebElement formSubmit;

    @FindBy(id = "link")
    private WebElement link;

    @FindBy(id = "selfLink")
    private WebElement selfLink;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return buildWebArchive(DynamicRoutesIT.class)
            .withWebXml(withMultiViewsAndIndexWelcomeFile)
            .createDeployment();
    }

    @Test
    void testDynamicSegmentIsExposedByName() {
        open("organizations/123/members");
        verify200("members", "organizations/123/members", "/organizations/[id]/members.xhtml");
        assertEquals("123", id.getText());
    }

    /**
     * The form action must render as the URL the request came in on, with the segment substituted, so that the postback arrives at the same view.
     */
    @Test
    void testPostbackReturnsToTheSameDynamicRoute() {
        open("organizations/123/members");
        guardHttp(formSubmit::click);
        verify200("members", "organizations/123/members", "/organizations/[id]/members.xhtml");
        assertEquals("123", id.getText());
    }

    /**
     * A dynamic route directory with a welcome file additionally answers for the bare directory path.
     */
    @Test
    void testWelcomeFileAnswersForTheBareDirectory() {
        open("organizations/123");
        verify200("organizationIndex", "organizations/123", "/organizations/[id]/index.xhtml", "organizations/123/");
        assertEquals("123", id.getText());
    }

    @Test
    void testNestedDynamicSegmentsAreExposedByTheirOwnName() {
        open("nl/products/12345");
        verify200("productIndex", "nl/products/12345", "/[locale]/products/[sku]/index.xhtml", "nl/products/12345/");
        assertEquals("nl", locale.getText());
        assertEquals("12345", sku.getText());
        assertEquals("12345", convertedSku.getText()); // Converted to a Product by the converter registered for its class.
    }

    /**
     * A dynamic route and MultiViews compose: the named segments resolve as usual and the remainder becomes a positional path parameter.
     */
    @Test
    void testDynamicSegmentsComposeWithMultiViews() {
        open("nl/products/12345/reviews/2");
        verify200("productReviews", "nl/products/12345/reviews/2", "/[locale]/products/[sku]/reviews.xhtml");
        assertEquals("nl", locale.getText());
        assertEquals("12345", sku.getText());
        assertEquals("2", pathIndex0.getText());

        // Linking to the very view being rendered must replace this request's path info, not append to it.
        assertEquals(contextPath + "/de/products/999/reviews/9", stripHostAndJsessionid(selfLink.getAttribute("href")));
    }

    /**
     * A literal directory is preferred over a dynamic one at the same level, which makes the literal sibling a value the dynamic segment can never take.
     */
    @Test
    void testLiteralSiblingIsPreferredOverTheDynamicSegment() {
        open("organizations/settings/members");
        verify200("settingsMembers", "organizations/settings/members", "/organizations/settings/members.xhtml");
        assertEquals("", id.getText());
    }

    /**
     * A path that matches no dynamic route still falls through to the MultiViews welcome file, and a path that does match must win over it. Both are only
     * meaningful because a root welcome file is present, which makes the MultiViews fallback active for every otherwise unmapped path.
     */
    @Test
    void testDynamicRouteTakesPrecedenceOverTheMultiViewsWelcomeFile() {
        open("nonexisting/foo");
        assertEquals("index", browser.getTitle());
        assertEquals("nonexisting", pathIndex0.getText());

        open("organizations/123/members");
        assertEquals("members", browser.getTitle());
        assertEquals("123", id.getText());
    }

    /**
     * A link to a dynamic route which does not supply every segment must never render an href containing square brackets, which are gen-delims per RFC 3986
     * that most containers refuse outright. Both implementations refuse to render it, Mojarra by propagating the failure and MyFaces by logging it and omitting
     * the href, so only the invariant itself is asserted here.
     */
    @Test
    void testLinkWithoutSuppliedSegmentNeverRendersBrackets() {
        open("unsuppliedSegment");
        assertFalse(PATTERN_BRACKETED_HREF.matcher(browser.getPageSource()).find(), "No rendered href may contain square brackets");
    }

    /**
     * The physical view is only reachable through its dynamic route, as its own path contains square brackets, which are gen-delims per RFC 3986 that must
     * never appear in a URL that OmniFaces hands out.
     */
    @Test
    void testBracketedPhysicalPathIsNotServed() {
        open("organizations/%5Bid%5D/members.xhtml");
        assertEquals("404", browser.getTitle());
    }

    private void verify200(String title, String path, String expectedViewId) {
        verify200(title, path, expectedViewId, path);
    }

    /**
     * A welcome file renders its action URL with a trailing slash, as it answers for its own directory, which is how FacesViews has always rendered one.
     */
    private void verify200(String title, String path, String expectedViewId, String expectedActionPath) {
        assertEquals(title, browser.getTitle());
        assertEquals(expectedViewId, viewId.getText());
        assertEquals(contextPath + "/" + path, stripHostAndJsessionid(browser.getCurrentUrl()));
        assertEquals(contextPath + "/" + expectedActionPath, stripHostAndJsessionid(form.getAttribute("action")));
        assertEquals(contextPath + "/organizations/471/members", stripHostAndJsessionid(link.getAttribute("href")));
        assertNoBrackets(form.getAttribute("action"));
        assertNoBrackets(link.getAttribute("href"));
        assertNoBrackets(browser.getCurrentUrl());
    }

    private static void assertNoBrackets(String url) {
        assertFalse(url.contains("[") || url.contains("]") || url.contains("%5B") || url.contains("%5D"), "URL must not contain square brackets: " + url);
    }

}
