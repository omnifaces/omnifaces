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
import static org.omnifaces.test.OmniFacesIT.FacesConfig.withCombinedResourceHandler;
import static org.omnifaces.util.Utils.serializeURLSafe;

import java.util.List;
import java.util.regex.Pattern;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class CombinedResourceHandlerIT extends OmniFacesIT {

    private static final String HEAD_COMBINED_SCRIPT_NAME = serializeURLSafe(
        "omnifaces:omnifaces.js|jakarta.faces:faces.js|headWithTarget.js|bodyWithTargetHead.js"
    );
    private static final String DEFERRED_COMBINED_SCRIPT_NAME = serializeURLSafe("deferredInHead.js|deferredInBody.js");
    private static final String CRITICAL_COMBINED_STYLESHEET_NAME = serializeURLSafe("critical.css");
    private static final String HEAD_COMBINED_STYLESHEET_NAME = serializeURLSafe("main.css|screen.css");
    private static final String HEAD_PRINT_STYLESHEET_NAME = "print";

    private static final Pattern RESOURCE_NAME = Pattern.compile("/jakarta\\.faces\\.resource/([^/.]+)\\.(?:js|css)\\.xhtml");

    @FindBy(css = "script[src*='ln=omnifaces.combined']")
    private List<WebElement> combinedScripts;

    @FindBy(css = "link[rel=stylesheet][href*='ln=omnifaces.combined']")
    private List<WebElement> combinedStylesheets;

    @FindBy(css = "link[rel=stylesheet][href*='print.css']")
    private List<WebElement> printStylesheets;

    @FindBy(id = "bodyWithTargetBody")
    private WebElement bodyWithTargetBody;

    @FindBy(id = "headWithoutTarget")
    private WebElement headWithoutTarget;

    @FindBy(id = "headWithTarget")
    private WebElement headWithTarget;

    @FindBy(id = "bodyWithTargetHead")
    private WebElement bodyWithTargetHead;

    @FindBy(id = "bodyWithoutTarget")
    private WebElement bodyWithoutTarget;

    @FindBy(id = "deferredInHead")
    private WebElement deferredInHead;

    @FindBy(id = "deferredInBody")
    private WebElement deferredInBody;

    @FindBy(id = "nonAjax:submit")
    private WebElement nonAjaxSubmit;

    @FindBy(id = "nonAjax:rebuild")
    private WebElement nonAjaxRebuild;

    @FindBy(id = "ajax:submit")
    private WebElement ajaxSubmit;

    @FindBy(id = "ajax:rebuild")
    private WebElement ajaxRebuild;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return buildWebArchive(CombinedResourceHandlerIT.class)
            .withFacesConfig(withCombinedResourceHandler)
            .createDeployment();
    }

    @Test
    void nonAjax() {
        verifyElements();
        guardHttp(nonAjaxSubmit::click);
        verifyElements();
        guardHttp(nonAjaxRebuild::click);
        verifyElements();
    }

    @Test
    void ajax() {
        verifyElements();
        guardAjax(ajaxSubmit::click);
        verifyElements();
        guardAjax(ajaxRebuild::click);
        verifyElements();
    }

    @Test
    void mixed() {
        verifyElements();
        guardHttp(nonAjaxSubmit::click);
        verifyElements();
        guardAjax(ajaxSubmit::click);
        verifyElements();
        guardHttp(nonAjaxRebuild::click);
        verifyElements();
        guardAjax(ajaxSubmit::click);
        verifyElements();
        guardHttp(nonAjaxSubmit::click);
        verifyElements();
        guardAjax(ajaxRebuild::click);
        verifyElements();
        guardHttp(nonAjaxSubmit::click);
        verifyElements();
        guardAjax(ajaxSubmit::click);
        verifyElements();
    }

    private void verifyElements() {
        waitUntilTextContent(deferredInBody); // Wait until last o:deferredScript is finished.

        assertEquals(2, combinedScripts.size());
        assertEquals(HEAD_COMBINED_SCRIPT_NAME, extractResourceName(combinedScripts.get(0), "src"));
        assertEquals(DEFERRED_COMBINED_SCRIPT_NAME, extractResourceName(combinedScripts.get(1), "src"));
        assertEquals(2, combinedStylesheets.size());
        assertEquals(CRITICAL_COMBINED_STYLESHEET_NAME, extractResourceName(combinedStylesheets.get(0), "href"));
        assertEquals(HEAD_COMBINED_STYLESHEET_NAME, extractResourceName(combinedStylesheets.get(1), "href"));
        assertEquals(1, printStylesheets.size());
        assertEquals(HEAD_PRINT_STYLESHEET_NAME, extractResourceName(printStylesheets.get(0), "href"));

        assertEquals("1,bodyWithTargetBody", bodyWithTargetBody.getText());
        assertEquals("2,headWithoutTarget", headWithoutTarget.getText());
        assertEquals("3,headWithTarget", headWithTarget.getText());
        assertEquals("4,bodyWithTargetHead", bodyWithTargetHead.getText());
        assertEquals("5,bodyWithoutTarget", bodyWithoutTarget.getText());
        assertEquals("6,deferredInHead", deferredInHead.getText());
        assertEquals("7,deferredInBody", deferredInBody.getText());
    }

    private static String extractResourceName(WebElement element, String attributeName) {
        var url = element.getAttribute(attributeName);
        var matcher = RESOURCE_NAME.matcher(url);
        assertTrue(matcher.find(), () -> url + " matches " + RESOURCE_NAME);
        return matcher.group(1);
    }

}
