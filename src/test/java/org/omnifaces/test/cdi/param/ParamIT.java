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
package org.omnifaces.test.cdi.param;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class ParamIT extends OmniFacesIT {

    @FindBy(id="stringParam")
    private WebElement stringParam;

    @FindBy(id="requiredStringParam")
    private WebElement requiredStringParam;

    @FindBy(id="stringParamArray")
    private WebElement stringParamArray;

    @FindBy(id="stringParamList")
    private WebElement stringParamList;

    @FindBy(id="longParam")
    private WebElement longParam;

    @FindBy(id="longParamArray")
    private WebElement longParamArray;

    @FindBy(id="longParamList")
    private WebElement longParamList;

    @FindBy(id="longParamListTypes")
    private WebElement longParamListTypes;

    @FindBy(id="dateParam")
    private WebElement dateParam;

    @FindBy(id="entityParam")
    private WebElement entityParam;

    @FindBy(id="entityViewParam")
    private WebElement entityViewParam;

    @FindBy(id="initResult")
    private WebElement initResult;

    @FindBy(id="messages")
    private WebElement messages;

    @FindBy(id="viewRootMessages")
    private WebElement viewRootMessages;

    @FindBy(id="globalMessages")
    private WebElement globalMessages;

    @Deployment(testable=false)
    public static WebArchive createDeployment() {
        return buildWebArchive(ParamIT.class)
            .withWebXml(WebXml.withInterpretEmptyStringSubmittedValuesAsNull)
            .createDeployment();
    }

    @Test
    void testStringParam() {
        openWithQueryString("stringParam=foo");
        assertEquals("foo", stringParam.getText());
        assertEquals("initSuccess", initResult.getText());
        assertMessageEquals("");
    }

    @Test
    void testInvalidStringParam() {
        openWithQueryString("stringParam=f");
        assertEquals("", stringParam.getText());
        assertEquals("initValidationFailed", initResult.getText());
        assertMessageEquals("size must be between 2 and 2147483647");
    }

    @Test
    void testRequiredStringParam() {
        open("ParamITRequired.xhtml?requiredStringParam=foo");
        assertEquals("foo", requiredStringParam.getText());
        assertEquals("initSuccess", initResult.getText());
        assertEquals("", messages.getText());
    }

    @Test
    void testMissingRequiredStringParam() {
        open("ParamITRequired.xhtml");
        assertEquals("", requiredStringParam.getText());
        assertEquals("initValidationFailed", initResult.getText());
        assertEquals("requiredStringParam: Validation Error: Value is required.", messages.getText());
    }

    @Test
    void testEmptyRequiredStringParam() {
        open("ParamITRequired.xhtml?requiredStringParam=");
        assertEquals("", requiredStringParam.getText());
        assertEquals("initValidationFailed", initResult.getText());
        assertEquals("requiredStringParam: Validation Error: Value is required.", messages.getText());
    }


    @Test
    @DisabledIfSystemProperty(named = "profile.id", matches = "tomee-.*", disabledReason = "BVal doesn't support this. You really have to add @Inject to @Param.")
    void testStringParamOnBeanWithCustomAnnotation() {
        open("ParamITCustomAnnotation.xhtml?stringParam=foo");
        assertEquals("foo", stringParam.getText());
        assertEquals("initSuccess", initResult.getText());
        assertEquals("", messages.getText());
    }

    @Test
    void testInvalidStringParamOnBeanWithCustomAnnotation() {
        open("ParamITCustomAnnotation.xhtml?stringParam=f");
        assertEquals("", stringParam.getText());
        assertEquals("initValidationFailed", initResult.getText());
        assertEquals("size must be between 2 and 2147483647", messages.getText());
    }

    @Test
    void testStringParamArray() {
        openWithQueryString("stringParamArray=foo&stringParamArray=bar");
        assertEquals("[foo, bar]", stringParamArray.getText());
        assertEquals("initSuccess", initResult.getText());
        assertMessageEquals("");
    }

    @Test
    void testInvalidStringParamArray() {
        openWithQueryString("stringParamArray=foo&stringParamArray=b");
        assertEquals("", stringParamArray.getText());
        assertEquals("initValidationFailed", initResult.getText());
        assertMessageEquals("String param array: Validation Error: Length is less than allowable minimum of '2'");
    }

    @Test
    void testStringParamList() {
        openWithQueryString("stringParamList=foo&stringParamList=bar");
        assertEquals("[foo, bar]", stringParamList.getText());
        assertEquals("initSuccess", initResult.getText());
        assertMessageEquals("");
    }

    @Test
    void testInvalidStringParamList() {
        openWithQueryString("stringParamList=f&stringParamArray=bar");
        assertEquals("", stringParamList.getText());
        assertEquals("initValidationFailed", initResult.getText());
        assertMessageEquals("Invalid length");
    }

    @Test
    void testLongParam() {
        openWithQueryString("longParam=42");
        assertEquals("42", longParam.getText());
        assertEquals("initSuccess", initResult.getText());
        assertMessageEquals("");
    }

    @Test
    void testInvalidLongParamValue() {
        openWithQueryString("longParam=foo");
        assertEquals("", longParam.getText());
        assertEquals("initValidationFailed", initResult.getText());
        assertMessageEquals("longParam: 'foo' must be a number consisting of one or more digits.");
    }

    @Test
    void testInvalidLongParamRange() {
        openWithQueryString("longParam=13");
        assertEquals("", longParam.getText());
        assertEquals("initValidationFailed", initResult.getText());
        assertMessageEquals("Invalid range");
    }

    @Test
    void testLongParamArray() {
        openWithQueryString("longParamArray=42&longParamArray=64");
        assertEquals("[42, 64]", longParamArray.getText());
        assertEquals("initSuccess", initResult.getText());
        assertMessageEquals("");
    }

    @Test
    void testInvalidLongParamArrayValue() {
        openWithQueryString("longParamArray=42&longParamArray=foo");
        assertEquals("", longParamArray.getText());
        assertEquals("initValidationFailed", initResult.getText());
        assertMessageEquals("longParamArray: 'foo' must be a number consisting of one or more digits.");
    }

    @Test
    void testInvalidLongParamArrayRange() {
        openWithQueryString("longParamArray=24&longParamArray=64");
        assertEquals("", longParamArray.getText());
        assertEquals("initValidationFailed", initResult.getText());
        assertMessageEquals("Invalid range");
    }

    @Test
    void testLongParamList() {
        openWithQueryString("longParamList=42&longParamList=64");
        assertEquals("[42, 64]", longParamList.getText());
        assertEquals("[java.lang.Long, java.lang.Long]", longParamListTypes.getText()); // Ensure that implicit Faces converters are also invoked when List<T> is used instead of T[].
        assertEquals("initSuccess", initResult.getText());
        assertGlobalMessageEquals("");
    }

    @Test
    void testInvalidLongParamListValue() {
        openWithQueryString("longParamList=42&longParamList=foo");
        assertEquals("", longParamList.getText());
        assertEquals("initValidationFailed", initResult.getText());
        assertGlobalMessageEquals("Long param list: 'foo' must be a number consisting of one or more digits.");
    }

    @Test
    void testInvalidLongParamListRange() {
        openWithQueryString("longParamList=24&longParamList=64");
        assertEquals("", longParamList.getText());
        assertEquals("initValidationFailed", initResult.getText());
        assertGlobalMessageEquals("Long param list: Validation Error: Value is less than allowable minimum of '42'");
    }

    @Test
    void testDateParam() {
        openWithQueryString("dateParam=19780326");
        assertEquals("1978-03-26", dateParam.getText());
        assertEquals("initSuccess", initResult.getText());
        assertMessageEquals("");
    }

    @Test
    void testInvalidDateParam() {
        openWithQueryString("dateParam=foo");
        assertEquals("", dateParam.getText());
        assertEquals("initValidationFailed", initResult.getText());
        assertMessageEquals("dateParam: \"foo\" is not the date format we had in mind! Please use the format yyyyMMdd.");
    }

    @Test
    void testEntityParam() {
        openWithQueryString("entityParam=42");
        assertEquals("ParamITEntity[42]", entityParam.getText());
        assertEquals("initSuccess", initResult.getText());
        assertMessageEquals("");
    }

    @Test
    void testInvalidEntityParamValue() {
        openWithQueryString("entityParam=foo");
        assertEquals("", entityParam.getText());
        assertEquals("initValidationFailed", initResult.getText());
        assertMessageEquals("Cannot convert because it threw java.lang.NumberFormatException: For input string: \"foo\"");
    }

    @Test
    void testInvalidEntityParamRange() {
        openWithQueryString("entityParam=24");
        assertEquals("", entityParam.getText());
        assertEquals("initValidationFailed", initResult.getText());
        assertMessageEquals("That's not the right answer");
    }

    @Test
    void testEntityViewParam() { // This basically ensures that the "generic entity converter" is reusable across @Param and UIViewParameter.
        openWithQueryString("entityViewParam=42");
        assertEquals("ParamITEntity[42]", entityViewParam.getText());
        assertEquals("", messages.getText());
    }

    @Test
    void testInvalidEntityViewParamValue() {
        openWithQueryString("entityViewParam=bar");
        assertEquals("", entityViewParam.getText());
        assertEquals("Cannot convert because it threw java.lang.NumberFormatException: For input string: \"bar\"", messages.getText());
    }

    @Test
    void testInvalidEntityViewParamRange() {
        openWithQueryString("entityViewParam=24");
        assertEquals("", entityViewParam.getText());
        assertEquals("That's not the right answer", messages.getText());
    }

    void assertMessageEquals(String expectedMessage) {
        assertMessageEquals(expectedMessage, false);
    }

    void assertGlobalMessageEquals(String expectedMessage) {
        assertMessageEquals(expectedMessage, true);
    }

    void assertMessageEquals(String expectedMessage, boolean globalMessage) {
        assertEquals(expectedMessage, messages.getText());

        if (!expectedMessage.isEmpty()) {
            assertEquals(messages.getText(), (globalMessage ? globalMessages : viewRootMessages).getText());
        }
    }

}