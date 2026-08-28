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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omnifaces.test.OmniFacesIT.FacesConfig.withCombinedAndCustomCDNResourceHandler;

import java.util.regex.Pattern;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * When a combined resource is decorated into a CDN resource, then the combined resource handler adds an onerror handler which loads the local URL as fallback.
 * That fallback URL is same origin and must therefore never get a <code>crossorigin</code> attribute, while it must still get an <code>integrity</code>
 * attribute, as subresource integrity does not require CORS for same origin resources. See {@link org.omnifaces.renderer.CorsAwareResourceRenderer} for the
 * rationale.
 */
public class CombinedResourceHandlerCDNFallbackIT extends OmniFacesIT {

    private static final Pattern SCRIPT_FALLBACK = Pattern.compile(
        "document\\.write\\('<script src=\"[^\"]+\" integrity=\"sha384-[^\"]+\"></script>'\\)"
    );

    private static final Pattern DEFERRED_SCRIPT_FALLBACK = Pattern.compile(
        "OmniFaces\\.Util\\.loadScript\\('[^']+','','sha384-[^']+'\\)"
    );

    private static final Pattern DEFERRED_SCRIPT_CDN = Pattern.compile(
        "OmniFaces\\.DeferredScript\\.add\\('[^']+','','sha384-[^']+'"
    );

    @FindBy(css = "script[src*='ln=omnifaces.combined']")
    private WebElement combinedScript;

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
    void combinedScriptFallbackHasNoCrossoriginButDoesHaveIntegrity() {
        var onerror = combinedScript.getDomAttribute("onerror");
        assertTrue(SCRIPT_FALLBACK.matcher(onerror).matches(), () -> onerror + " matches " + SCRIPT_FALLBACK);
    }

    @Test
    void deferredScriptAndItsFallbackHaveNoCrossoriginButDoHaveIntegrity() {
        var script = getDeferredScript();
        assertTrue(DEFERRED_SCRIPT_CDN.matcher(script).find(), () -> script + " contains " + DEFERRED_SCRIPT_CDN);
        assertTrue(DEFERRED_SCRIPT_FALLBACK.matcher(script).find(), () -> script + " contains " + DEFERRED_SCRIPT_FALLBACK);
    }

    @Test
    void combinedResourcesStillExecute() {
        waitUntilTextContent(deferredInHead);
        assertEquals("1,headWithTarget", headWithTarget.getText());
        assertEquals("2,deferredInHead", deferredInHead.getText());
    }

    private String getDeferredScript() {
        for (var script : browser.findElements(By.tagName("script"))) {
            var content = script.getDomProperty("textContent");

            if (content != null && content.contains("OmniFaces.DeferredScript.add(")) {
                return content;
            }
        }

        throw new AssertionError("There is no inline script which calls OmniFaces.DeferredScript.add()");
    }

}
