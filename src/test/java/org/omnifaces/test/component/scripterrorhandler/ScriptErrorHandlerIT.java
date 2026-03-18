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
package org.omnifaces.test.component.scripterrorhandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class ScriptErrorHandlerIT extends OmniFacesIT {

    @FindBy(id="triggerError")
    private WebElement triggerError;

    @FindBy(id="triggerRejection")
    private WebElement triggerRejection;

    @FindBy(id="poll:pollResult")
    private WebElement pollResult;

    @FindBy(id="poll:reset")
    private WebElement reset;

    @FindBy(id="errorMessage")
    private WebElement errorMessage;

    @FindBy(id="errorName")
    private WebElement errorName;

    @FindBy(id="pageURL")
    private WebElement pageURL;

    @Deployment(testable=false)
    public static WebArchive createDeployment() {
        return createWebArchive(ScriptErrorHandlerIT.class);
    }

    @Test
    void testWindowOnerror() {
        guardAjax(reset::click); // Reset any state from previous tests.
        triggerError.click();
        pollUntilErrorReceived();

        assertTrue(errorMessage.getText().contains("nonExistentFunction"), "Error message should contain function name");
        assertEquals("ReferenceError", errorName.getText(), "Error name should be ReferenceError");
        assertTrue(pageURL.getText().contains("ScriptErrorHandlerIT.xhtml"), "Page URL should contain test page name");
    }

    @Test
    void testUnhandledRejection() {
        guardAjax(reset::click); // Reset any state from previous tests.
        triggerRejection.click();
        pollUntilErrorReceived();

        assertEquals("testRejection", errorMessage.getText(), "Error message should match rejection reason");
        assertEquals("Error", errorName.getText(), "Error name should be Error");
        assertTrue(pageURL.getText().contains("ScriptErrorHandlerIT.xhtml"), "Page URL should contain test page name");
    }

    private void pollUntilErrorReceived() {
        waitUntilTextContent("errorMessage", pollResult::click);
    }

}
