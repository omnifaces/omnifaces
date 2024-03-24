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
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisabledIfSystemProperty(named = "profile.id", matches = "piranha-.*", disabledReason = "piranha returns different baseURL on getRequestURL (ip6-localhost instead of localhost)")
public class PWAResourceHandlerIT extends OmniFacesIT {

    private static final String EXPECTED_MANIFEST = "{\"categories\":[],\"dir\":\"auto\",\"display\":\"browser\","
        + "\"icons\":[{\"sizes\":\"512x512\",\"src\":\"\\/PWAResourceHandlerIT\\/jakarta.faces.resource\\/icon.png.xhtml?v=1\",\"type\":\"image\\/png\"}],"
        + "\"lang\":\"en\",\"name\":\"PWAResourceHandlerIT\",\"orientation\":\"any\",\"prefer_related_applications\":false,\"related_applications\":[],"
        + "\"start_url\":\"{baseURL}\"}";

    @FindBy(css="link[rel=manifest]")
    private WebElement manifest;

    @FindBy(id="form:ajaxSubmit")
    private WebElement ajaxSubmit;

    @FindBy(id="form:viewScopedBeanHashCode")
    private WebElement viewScopedBeanHashCode;

    @FindBy(id="form:viewScopedBeanInstances")
    private WebElement viewScopedBeanInstances;

    @Deployment(testable=false)
    public static WebArchive createDeployment() {
        return createWebArchive(PWAResourceHandlerIT.class);
    }

    @Test
    @Order(1)
    void verifyManifest() {
//        String instances = viewScopedBeanInstances.getText();
//        assertEquals("1", instances, "This is the first time the page is opened, so there should be only 1 view scoped bean instance");
        assertEquals("use-credentials", manifest.getAttribute("crossorigin"));

        browser.get(manifest.getAttribute("href"));

        assertEquals(EXPECTED_MANIFEST.replace("{baseURL}", baseURL.toString().replace("/", "\\/")), stripTags(browser.getPageSource())
            .replaceAll("\\?v=[0-9]{13,}", "?v=1")); // Normalize any version query string on icon resource.
    }

    @Test
    @Order(2)
    void verifyServiceWorkerScript() {
//        String instances = viewScopedBeanInstances.getText();
//        assertEquals("2", instances, "This is the second time the page is opened, so there should be 2 view scoped bean instances");

        browser.get(manifest.getAttribute("href").replace(MANIFEST_RESOURCE_NAME, SERVICEWORKER_RESOURCE_NAME));
        String serviceWorkerScript = browser.getPageSource();
        assertTrue(serviceWorkerScript.contains("/PWAResourceHandlerIT.xhtml"), serviceWorkerScript + " contains '/PWAResourceHandlerIT.xhtml'");
    }

    @Test
    @Order(3)
    void verifyViewScopedBeanAfterAjaxSubmit() {
//        String instances = viewScopedBeanInstances.getText();
//        assertEquals("3", instances, "This is the third time the page is opened, so there should be 3 view scoped bean instances");

        String hashCode = viewScopedBeanHashCode.getText();
        guardAjax(ajaxSubmit::click);
        assertEquals(hashCode, viewScopedBeanHashCode.getText(), "It is still the same instance after 1st ajax submit");
//        assertEquals(instances, viewScopedBeanInstances.getText(), "No additional instances have been created after 1st ajax submit");

        guardAjax(ajaxSubmit::click);
        assertEquals(hashCode, viewScopedBeanHashCode.getText(), "It is still the same instance after 2nd ajax submit");
//        assertEquals(instances, viewScopedBeanInstances.getText(), "No additional instances have been created after 2nd ajax submit");

        guardAjax(ajaxSubmit::click);
        assertEquals(hashCode, viewScopedBeanHashCode.getText(), "It is still the same instance after 3rd ajax submit");
//        assertEquals(instances, viewScopedBeanInstances.getText(), "No additional instances have been created after 3rd ajax submit");
    }

    // TODO: see outcommented lines. This broke since migration from htmlunit to chrome?

}