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
package org.omnifaces.test.resourcehandler.pwaresourcehandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omnifaces.el.functions.Strings.stripTags;
import static org.omnifaces.resourcehandler.PWAResourceHandler.MANIFEST_RESOURCE_NAME;
import static org.omnifaces.resourcehandler.PWAResourceHandler.SERVICEWORKER_RESOURCE_NAME;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.omnifaces.config.OmniFaces;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PWAResourceHandlerIT extends OmniFacesIT {

    private static final String EXPECTED_MANIFEST = "{\"categories\":[],\"dir\":\"auto\",\"display\":\"browser\","
        + "\"icons\":[{\"sizes\":\"512x512\",\"src\":\"{contextPath}\\/jakarta.faces.resource\\/icon.png.xhtml?v=1\",\"type\":\"image\\/png\"}],"
        + "\"lang\":\"en\",\"name\":\"PWAResourceHandlerIT\",\"prefer_related_applications\":false,\"related_applications\":[],\"screenshots\":[],\"shortcuts\":[],"
        + "\"start_url\":\"{baseURL}\"}";

    @FindBy(css = "link[rel=manifest]")
    private WebElement manifest;

    @FindBy(id = "form:ajaxSubmit")
    private WebElement ajaxSubmit;

    @FindBy(id = "form:viewScopedBeanHashCode")
    private WebElement viewScopedBeanHashCode;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return createWebArchive(PWAResourceHandlerIT.class);
    }

    @Test
    @Order(1)
    void verifyManifest() {
        assertEquals("use-credentials", manifest.getAttribute("crossorigin"));

        String manifestBody;
        if (isFirefox()) {
            // Firefox would render the JSON manifest via its built-in JSON viewer when navigated to via browser.get(), so page source would contain the
            // viewer DOM instead of the raw JSON. Fetch via in-page XHR (sync) to keep cookies/session and bypass the viewer.
            manifestBody = (String) executeScript(
                "var x = new XMLHttpRequest(); x.open('GET', arguments[0], false); x.send(); return x.responseText;",
                manifest.getAttribute("href")
            );
        }
        else {
            browser.get(manifest.getAttribute("href"));
            manifestBody = stripTags(browser.getPageSource());
        }

        assertEquals(
            EXPECTED_MANIFEST.replace("{contextPath}", contextPath.replace("/", "\\/")).replace("{baseURL}", baseURL.toString().replace("/", "\\/")),
            manifestBody.replaceAll("\\?v=[0-9]{13,}", "?v=1") // Normalize any version query string on icon resource.
        );
    }

    /**
     * The cacheable resources must carry the cache bust version, so that the cache entries of one deployment cannot be matched by the requests of the next one,
     * and so that the cache version changes along and prunes them on activate.
     */
    @Test
    @Order(2)
    void verifyServiceWorkerScript() {
        browser.get(manifest.getAttribute("href").replace(MANIFEST_RESOURCE_NAME, SERVICEWORKER_RESOURCE_NAME));
        String serviceWorkerScript = browser.getPageSource();
        assertTrue(serviceWorkerScript.contains("/PWAResourceHandlerIT.xhtml"), serviceWorkerScript + " contains '/PWAResourceHandlerIT.xhtml'");
        assertTrue(serviceWorkerScript.contains("v=" + OmniFaces.getVersion()), serviceWorkerScript + " contains 'v=" + OmniFaces.getVersion() + "'");
    }

    @Test
    @Order(3)
    void verifyViewScopedBeanAfterAjaxSubmit() {
        String hashCode = viewScopedBeanHashCode.getText();
        guardAjax(ajaxSubmit::click);
        assertEquals(hashCode, viewScopedBeanHashCode.getText(), "It is still the same instance after 1st ajax submit");

        guardAjax(ajaxSubmit::click);
        assertEquals(hashCode, viewScopedBeanHashCode.getText(), "It is still the same instance after 2nd ajax submit");

        guardAjax(ajaxSubmit::click);
        assertEquals(hashCode, viewScopedBeanHashCode.getText(), "It is still the same instance after 3rd ajax submit");
    }

}
