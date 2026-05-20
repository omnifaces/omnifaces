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
package org.omnifaces.test.util.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class ComponentsIT extends OmniFacesIT {

    @FindBy(id = "form:commandButtonAction")
    private WebElement commandButtonAction;

    @FindBy(id = "form:commandButtonActionListener")
    private WebElement commandButtonActionListener;

    @FindBy(id = "form:commandButtonAjaxListener")
    private WebElement commandButtonAjaxListener;

    @FindBy(id = "form:primeFacesCommandButtonAction")
    private WebElement primeFacesCommandButtonAction;

    @FindBy(id = "form:primeFacesCommandButtonActionListener")
    private WebElement primeFacesCommandButtonActionListener;

    @FindBy(id = "form:addScriptResource")
    private WebElement addScriptResource;

    @FindBy(id = "expressions")
    private WebElement expressions;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return buildWebArchive(ComponentsIT.class)
            .withPrimeFaces()
            .createDeployment();
    }

    @Test
    void testCommandButtonAction() {
        guardAjax(commandButtonAction::click);
        assertEquals("[#{componentsITBean.submit}]", expressions.getText());
    }

    @Test
    void testCommandButtonActionListener() {
        guardAjax(commandButtonActionListener::click);
        assertEquals("[#{componentsITBean.submit()}]", expressions.getText());
    }

    @Test
    void testCommandButtonAjaxListener() {
        guardAjax(commandButtonAjaxListener::click);
        assertEquals("[#{componentsITBean.submit()}]", expressions.getText());
    }

    @Test
    void testPrimeFacesCommandButtonAction() {
        guardPrimeFacesAjax(primeFacesCommandButtonAction::click);
        assertEquals("[#{componentsITBean.submit}]", expressions.getText());
    }

    @Test
    void testPrimeFacesCommandButtonActionListener() {
        guardPrimeFacesAjax(primeFacesCommandButtonActionListener::click);
        assertEquals("[#{componentsITBean.submit()}]", expressions.getText());
    }

    @Test
    void testAddScriptResource() {
        assertFalse((Boolean) executeScript("return !!window.OmniFaces"), "window.OmniFaces should NOT be available: " + consoleErrors);

        guardAjax(addScriptResource::click);
        assertTrue(
            networkResponses.keySet().stream().anyMatch(url -> url.contains("omnifaces.js")),
            "omnifaces.js should be loaded: " + networkResponses.keySet()
        );
        assertTrue((Boolean) executeScript("return !!window.OmniFaces"), "window.OmniFaces should be available: " + consoleErrors);

        guardAjax(addScriptResource::click);
        assertFalse(
            networkResponses.keySet().stream().anyMatch(url -> url.contains("omnifaces.js")),
            "omnifaces.js should NOT be reloaded: " + networkResponses.keySet()
        );
        assertTrue((Boolean) executeScript("return !!window.OmniFaces"), "window.OmniFaces should still be available: " + consoleErrors);

        guardAjax(addScriptResource::click);
        assertFalse(
            networkResponses.keySet().stream().anyMatch(url -> url.contains("omnifaces.js")),
            "omnifaces.js should NOT be reloaded: " + networkResponses.keySet()
        );
        assertTrue((Boolean) executeScript("return !!window.OmniFaces"), "window.OmniFaces should still be available: " + consoleErrors);
    }

}
