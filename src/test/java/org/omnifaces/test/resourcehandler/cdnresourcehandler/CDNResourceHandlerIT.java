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
package org.omnifaces.test.resourcehandler.cdnresourcehandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omnifaces.test.OmniFacesIT.FacesConfig.withCDNResourceHandler;
import static org.omnifaces.test.OmniFacesIT.WebXml.withCDNResources;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class CDNResourceHandlerIT extends OmniFacesIT {

    @FindBy(css = "head link[href*='jquery-ui.css']")
    private WebElement cdnStylesheet;

    @FindBy(css = "head script[src*='jquery-2.2.4']")
    private WebElement cdnScript;

    @FindBy(css = "head script[src*='cdn-mapped.js']")
    private WebElement wildcardMappedScript;

    @FindBy(css = "head script[src*='keep-local.js']")
    private WebElement wildcardExcludedScript;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return buildWebArchive(CDNResourceHandlerIT.class)
            .withWebXml(withCDNResources)
            .withFacesConfig(withCDNResourceHandler)
            .createDeployment();
    }

    @Test
    void cdnResources() {
        assertEquals("https://code.jquery.com/ui/1.12.1/themes/base/jquery-ui.css", cdnStylesheet.getAttribute("href"));
        assertEquals("https://code.jquery.com/jquery-2.2.4.min.js", cdnScript.getAttribute("src"));
    }

    @Test
    void wildcardMappedResource() {
        assertEquals("https://cdn.example.com/wildcardlib/cdn-mapped.js", wildcardMappedScript.getAttribute("src"));
    }

    @Test
    void wildcardExcludedResource() {
        var src = wildcardExcludedScript.getAttribute("src");
        assertFalse(src.startsWith("https://cdn.example.com/"), () -> "Excluded resource should bypass CDN, but was: " + src);
        assertTrue(src.contains("/jakarta.faces.resource/keep-local.js"), () -> "Excluded resource should be served by Faces, but was: " + src);
    }

}
