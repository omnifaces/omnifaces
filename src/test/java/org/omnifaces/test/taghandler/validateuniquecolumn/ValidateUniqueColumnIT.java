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
package org.omnifaces.test.taghandler.validateuniquecolumn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class ValidateUniqueColumnIT extends OmniFacesIT {

    @FindBy(id = "form1:table:0:input")
    private WebElement form1Row0Input;

    @FindBy(id = "form1:table:0:message")
    private WebElement form1Row0Message;

    @FindBy(id = "form1:table:1:input")
    private WebElement form1Row1Input;

    @FindBy(id = "form1:table:1:message")
    private WebElement form1Row1Message;

    @FindBy(id = "form1:table:2:input")
    private WebElement form1Row2Input;

    @FindBy(id = "form1:table:2:message")
    private WebElement form1Row2Message;

    @FindBy(id = "form1:submit")
    private WebElement form1Submit;

    @FindBy(id = "form1:ok")
    private WebElement form1Ok;

    @FindBy(id = "form2:table:0:input")
    private WebElement form2Row0Input;

    @FindBy(id = "form2:table:0:message")
    private WebElement form2Row0Message;

    @FindBy(id = "form2:submit")
    private WebElement form2Submit;

    @FindBy(id = "form2:ok")
    private WebElement form2Ok;

    @FindBy(id = "form3:table:0:input")
    private WebElement form3Row0Input;

    @FindBy(id = "form3:table:0:message")
    private WebElement form3Row0Message;

    @FindBy(id = "form3:table:0:submit")
    private WebElement form3Row0Submit;

    @FindBy(id = "form3:ok")
    private WebElement form3Ok;

    @FindBy(id = "form4:otherMessage")
    private WebElement form4OtherMessage;

    @FindBy(id = "form4:table:0:input")
    private WebElement form4Row0Input;

    @FindBy(id = "form4:table:0:message")
    private WebElement form4Row0Message;

    @FindBy(id = "form4:table:1:input")
    private WebElement form4Row1Input;

    @FindBy(id = "form4:table:1:message")
    private WebElement form4Row1Message;

    @FindBy(id = "form4:table:2:message")
    private WebElement form4Row2Message;

    @FindBy(id = "form4:submit")
    private WebElement form4Submit;

    @FindBy(id = "form4:ok")
    private WebElement form4Ok;

    @FindBy(id = "form5:table:0:input")
    private WebElement form5Row0Input;

    @FindBy(id = "form5:table:0:message")
    private WebElement form5Row0Message;

    @FindBy(id = "form5:table:1:input")
    private WebElement form5Row1Input;

    @FindBy(id = "form5:table:1:message")
    private WebElement form5Row1Message;

    @FindBy(id = "form5:submit")
    private WebElement form5Submit;

    @FindBy(id = "form5:ok")
    private WebElement form5Ok;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return buildWebArchive(ValidateUniqueColumnIT.class).createDeployment();
    }

    /**
     * A value changed into a duplicate of another visible row must be rejected.
     */
    @Test
    void validateDuplicateInVisibleRow() {
        assertEquals("A", form1Row0Input.getAttribute("value"));
        assertEquals("B", form1Row1Input.getAttribute("value"));
        assertEquals("C", form1Row2Input.getAttribute("value"));

        form1Row1Input.clear();
        form1Row1Input.sendKeys("C");
        guardAjax(form1Submit::click);

        assertNotEquals("", form1Row1Message.getText());
        assertEquals("", form1Ok.getText());
    }

    /**
     * A value which stays unique must be accepted. This guards against invalidating a row which merely changed value
     * without colliding with any other row.
     */
    @Test
    void validateUniqueChangeIsAccepted() {
        form1Row1Input.clear();
        form1Row1Input.sendKeys("Z");
        guardAjax(form1Submit::click);

        assertEquals("", form1Row0Message.getText());
        assertEquals("", form1Row1Message.getText());
        assertEquals("", form1Row2Message.getText());
        assertEquals("OK!", form1Ok.getText());
    }

    /**
     * When more than one row is changed into the very same value within a single submit, then none of them may silently
     * pass.
     */
    @Test
    void validateMultipleRowsChangedIntoSameValue() {
        form1Row0Input.clear();
        form1Row0Input.sendKeys("X");
        form1Row1Input.clear();
        form1Row1Input.sendKeys("X");
        form1Row2Input.clear();
        form1Row2Input.sendKeys("X");
        guardAjax(form1Submit::click);

        assertEquals("", form1Ok.getText());
        assertNotEquals("", form1Row0Message.getText() + form1Row1Message.getText() + form1Row2Message.getText());
    }

    /**
     * Rows outside the current page are never submitted, so their value is only available in the data model.
     */
    @Test
    void validateDuplicateInPaginatedAwayRow() {
        assertEquals("A", form2Row0Input.getAttribute("value"));

        form2Row0Input.clear();
        form2Row0Input.sendKeys("D");
        guardAjax(form2Submit::click);

        assertNotEquals("", form2Row0Message.getText());
        assertEquals("", form2Ok.getText());
    }

    /**
     * An ajax request may execute only the changed row, e.g. a command button whose execute resolves to the input of its
     * own row. The remaining rows are then not submitted at all, so their value is likewise only available in the data
     * model.
     */
    @Test
    void validateDuplicateWhenOnlyChangedInputIsExecuted() {
        form3Row0Input.sendKeys("C"); // Turns "A" into "AC", which is the value of row 2.
        guardAjax(form3Row0Submit::click);

        assertNotEquals("", form3Row0Message.getText());
        assertEquals("", form3Ok.getText());
    }

    /**
     * A validation failure on an unrelated input in the same form may not disturb this validator: a genuine duplicate
     * must still be reported.
     */
    @Test
    void validateDuplicateWhenOtherInputInSameFormIsInvalid() {
        form4Row0Input.clear();
        form4Row0Input.sendKeys("C");
        guardAjax(form4Submit::click); // The required otherInput is left empty on purpose.

        assertNotEquals("", form4OtherMessage.getText());
        assertNotEquals("", form4Row0Message.getText());
        assertEquals("", form4Ok.getText());
    }

    /**
     * The counterpart of {@link #validateDuplicateWhenOtherInputInSameFormIsInvalid()}: a validation failure on an unrelated
     * input may not cause a duplicate to be reported on rows which are in fact still unique.
     */
    @Test
    void validateNoFalseDuplicateWhenOtherInputInSameFormIsInvalid() {
        form4Row1Input.clear();
        form4Row1Input.sendKeys("Z");
        guardAjax(form4Submit::click); // The required otherInput is left empty on purpose.

        assertNotEquals("", form4OtherMessage.getText());
        assertEquals("", form4Row0Message.getText());
        assertEquals("", form4Row1Message.getText());
        assertEquals("", form4Row2Message.getText());
    }

    /**
     * A synchronous postback submits all rows just like an ajax request which executes the whole form does.
     */
    @Test
    void validateDuplicateOnNonAjaxSubmit() {
        form5Row0Input.clear();
        form5Row0Input.sendKeys("C");
        guardHttp(form5Submit::click);

        assertNotEquals("", form5Row0Message.getText());
        assertEquals("", form5Ok.getText());
    }

    /**
     * The counterpart of {@link #validateDuplicateOnNonAjaxSubmit()}: a unique value must still be accepted.
     */
    @Test
    void validateUniqueChangeOnNonAjaxSubmitIsAccepted() {
        form5Row1Input.clear();
        form5Row1Input.sendKeys("Z");
        guardHttp(form5Submit::click);

        assertEquals("", form5Row0Message.getText());
        assertEquals("", form5Row1Message.getText());
        assertEquals("OK!", form5Ok.getText());
    }

}
