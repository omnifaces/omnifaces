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
package org.omnifaces.test.resourcehandler.combinedresourcehandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omnifaces.resourcehandler.CombinedResourceHandler.LIBRARY_NAME;
import static org.omnifaces.test.CustomCDNResourceHandler.FOREIGN_CDN_HOST;
import static org.omnifaces.test.OmniFacesIT.FacesConfig.withCombinedAndCustomCDNResourceHandler;

import java.util.List;
import java.util.function.Predicate;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.resourcehandler.CDNResource;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * When a combined resource is decorated into a CDN resource, then the combined resource handler arranges a fallback which loads the local URL in case the CDN
 * request errors out. These make the CDN request fail and assert the outcome in the browser: the resources still execute, and each local URL is loaded without
 * a <code>crossorigin</code> attribute and with the <code>integrity</code> attribute of the local content.
 * <p>
 * The fallback URL is same origin and must therefore never get a <code>crossorigin</code> attribute, while it must still get an <code>integrity</code>
 * attribute, as subresource integrity does not require CORS for same origin resources. See {@link org.omnifaces.renderer.CorsAwareResourceRenderer} for the
 * rationale. A wrong <code>integrity</code> would make the browser refuse the fallback, so the scripts executing at all is itself the assertion that the hash
 * is the one of the local content.
 */
public class CombinedResourceHandlerCDNFallbackIT extends OmniFacesIT {

    private static final String INTEGRITY_PREFIX = "sha384-";

    @FindBy(id = "headWithTarget")
    private WebElement headWithTarget;

    @FindBy(id = "deferredInHead")
    private WebElement deferredInHead;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return buildWebArchive(CombinedResourceHandlerCDNFallbackIT.class)
            .withFacesConfig(withCombinedAndCustomCDNResourceHandler)
            .createDeployment();
    }

    @Test
    void combinedResourcesExecuteWhenCdnRequestSucceeds() {
        assertResourcesExecuted();
    }

    @Test
    void combinedResourcesStillExecuteWhenCdnRequestFails() {
        openWithFailingCDN();
        assertResourcesExecuted();
    }

    /**
     * Both the combined script and the deferred combined script are loaded from the local URL after the CDN request for them has errored out, and each of those
     * local URLs is loaded without a <code>crossorigin</code> attribute and with an <code>integrity</code> attribute.
     */
    @Test
    void everyFallbackHasNoCrossoriginButDoesHaveIntegrity() {
        openWithFailingCDN();
        assertResourcesExecuted();
        var fallbacks = getFallbackScripts();
        assertEquals(2, fallbacks.size(), () -> "both the combined script and the deferred combined script must have fallen back, but got " + fallbacks);

        for (var fallback : fallbacks) {
            assertNull(fallback.getDomAttribute("crossorigin"), "fallback must not have crossorigin");
            assertIntegrity(fallback);
        }
    }

    /**
     * The CDN URLs which errored out were themselves requested with an <code>integrity</code> attribute, as the CDN content of a {@link CDNResource} is by
     * contract byte for byte identical to the local content it is hashed from.
     */
    @Test
    void everyCdnResourceHasIntegrity() {
        openWithFailingCDN();
        assertResourcesExecuted();
        var cdnResources = getCdnScripts();
        assertEquals(2, cdnResources.size(), () -> "both combined resources must have been requested from the CDN host, but got " + cdnResources);
        cdnResources.forEach(CombinedResourceHandlerCDNFallbackIT::assertIntegrity);
    }

    private static void assertIntegrity(WebElement element) {
        var integrity = element.getDomAttribute("integrity");
        assertTrue(
            integrity != null && integrity.startsWith(INTEGRITY_PREFIX), () -> "must have " + INTEGRITY_PREFIX + " integrity, was " + integrity
        );
    }

    private void openWithFailingCDN() {
        openWithQueryString("failCDN=true");
    }

    private void assertResourcesExecuted() {
        waitUntilTextContent(deferredInHead);
        assertEquals("1,headWithTarget", headWithTarget.getText());
        assertEquals("2,deferredInHead", deferredInHead.getText());
    }

    /**
     * Returns the script elements which load a combined resource from the local host, which is where the fallback loads it from after the CDN request has
     * errored out.
     */
    private List<WebElement> getFallbackScripts() {
        return findScripts(src -> src.contains("ln=" + LIBRARY_NAME));
    }

    /**
     * Returns the script elements which point at the CDN host. Their URL is remapped to the bare resource name and therefore carries no library query
     * parameter, hence they are told apart by host.
     */
    private List<WebElement> getCdnScripts() {
        return findScripts(src -> src.startsWith(FOREIGN_CDN_HOST));
    }

    private List<WebElement> findScripts(Predicate<String> srcMatches) {
        return browser.findElements(By.cssSelector("script[src]")).stream()
            .filter(script -> srcMatches.test(String.valueOf(script.getDomAttribute("src"))))
            .toList();
    }

}
