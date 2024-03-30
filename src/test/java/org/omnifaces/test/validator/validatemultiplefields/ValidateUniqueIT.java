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
package org.omnifaces.test.validator.validatemultiplefields;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.omnifaces.test.OmniFacesIT.WebXml.withInterpretEmptyStringSubmittedValuesAsNull;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class ValidateUniqueIT extends OmniFacesIT {

    @FindBy(id="form1:input1")
    private WebElement form1Input1;

    @FindBy(id="form1:input1_m")
    private WebElement form1Input1Message;

    @FindBy(id="form1:input2")
    private WebElement form1Input2;

    @FindBy(id="form1:input2_m")
    private WebElement form1Input2Message;

    @FindBy(id="form1:submit")
    private WebElement form1Submit;

    @FindBy(id="form1:ok")
    private WebElement form1Ok;

    @FindBy(id="form2:input1")
    private WebElement form2Input1;

    @FindBy(id="form2:input1_m")
    private WebElement form2Input1Message;

    @FindBy(id="form2:input2")
    private WebElement form2Input2;

    @FindBy(id="form2:input2_m")
    private WebElement form2Input2Message;

    @FindBy(id="form2:submit")
    private WebElement form2Submit;

    @FindBy(id="form2:ok")
    private WebElement form2Ok;

    @Deployment(testable=false)
    public static WebArchive createDeployment() {
        return buildWebArchive(ValidateUniqueIT.class)
            .withWebXml(withInterpretEmptyStringSubmittedValuesAsNull)
            .createDeployment();
    }

    @Test
    void testForm1() {
        assertEquals("0123456789", form1Input1.getAttribute("value"));
        assertEquals("", form1Input2.getAttribute("value"));
        assertEquals("", form1Input1Message.getText());
        assertEquals("", form1Input2Message.getText());
        assertEquals("", form1Ok.getText());

        form1Input2.sendKeys("0123456789");
        guardAjax(form1Submit::click);

        assertEquals("0123456789", form1Input1.getAttribute("value"));
        assertEquals("0123456789", form1Input2.getAttribute("value"));
        assertEquals("", form1Input1Message.getText());
        assertEquals("form1:input1, form1:input2: Please fill out an unique value for all of those fields", form1Input2Message.getText());
        assertEquals("", form1Ok.getText());

        form1Input2.sendKeys("0");
        guardAjax(form1Submit::click);

        assertEquals("0123456789", form1Input1.getAttribute("value"));
        assertEquals("01234567890", form1Input2.getAttribute("value"));
        assertEquals("", form1Input1Message.getText());
        assertEquals("", form1Input2Message.getText());
        assertEquals("OK!", form1Ok.getText());
    }

    @Test
    void testForm2() {
        assertEquals("0123456789", form2Input1.getAttribute("value"));
        assertEquals("", form2Input2.getAttribute("value"));
        assertEquals("", form2Input1Message.getText());
        assertEquals("", form2Input2Message.getText());
        assertEquals("", form2Ok.getText());

        form2Input2.sendKeys("0123456789");
        guardAjax(form2Submit::click);

        assertEquals("0123456789", form2Input1.getAttribute("value"));
        assertEquals("0123456789", form2Input2.getAttribute("value"));
        assertEquals("", form2Input1Message.getText());
        assertEquals("form2:input1, form2:input2: Please fill out an unique value for all of those fields", form2Input2Message.getText());
        assertEquals("", form2Ok.getText());

        form2Input2.sendKeys("0");
        guardAjax(form2Submit::click);

        assertEquals("0123456789", form2Input1.getAttribute("value"));
        assertEquals("01234567890", form2Input2.getAttribute("value"));
        assertEquals("", form2Input1Message.getText());
        assertEquals("", form2Input2Message.getText());
        assertEquals("OK!", form2Ok.getText());
    }
}