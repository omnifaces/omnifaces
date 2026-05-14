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
package org.omnifaces.test.context.ompartialviewcontext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class OmniPartialViewContextIT extends OmniFacesIT {

    @FindBy(id = "form:input")
    private WebElement input;

    @FindBy(id = "form:submit")
    private WebElement submit;

    @FindBy(id = "form:submitWithData")
    private WebElement submitWithData;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return buildWebArchive(OmniPartialViewContextIT.class).createDeployment();
    }

    @Test
    void test() {
        // Validation failure: OmniFaces.Ajax.validationFailed must be true.
        guardAjax(submit::click);
        assertEquals(Boolean.TRUE, executeScript("return OmniFaces.Ajax.validationFailed"));
        assertNull(executeScript("return OmniFaces.Ajax.data"));

        // Validation success: OmniFaces.Ajax.validationFailed must be false.
        input.sendKeys("test");
        guardAjax(submit::click);
        assertEquals(Boolean.FALSE, executeScript("return OmniFaces.Ajax.validationFailed"));
        assertNull(executeScript("return OmniFaces.Ajax.data"));

        // With Ajax.data(): both validationFailed and data must be present.
        guardAjax(submitWithData::click);
        assertEquals(Boolean.FALSE, executeScript("return OmniFaces.Ajax.validationFailed"));
        assertEquals("bar", executeScript("return OmniFaces.Ajax.data.foo"));

        // Console must remain free of errors caused by the always-emitted eval.
        assertTrue(consoleErrors.isEmpty(), () -> "Unexpected console errors: " + consoleErrors);
    }

}
