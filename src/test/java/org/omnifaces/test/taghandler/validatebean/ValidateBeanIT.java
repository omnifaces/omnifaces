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
package org.omnifaces.test.taghandler.validatebean;

import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omnifaces.test.OmniFacesIT.FacesConfig.withMessageBundle;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class ValidateBeanIT extends OmniFacesIT {

	@FindBy(id="messages")
	private WebElement messages;

	@FindBy(id="validateByCommand:input")
	private WebElement input;

	@FindBy(id="validateByCommand:validateDefaultByCommand")
	private WebElement validateDefaultByCommand;

	@FindBy(id="validateByCommand:validateGroupByCommand")
	private WebElement validateGroupByCommand;

	@FindBy(id="validateByCommand:validateDefaultAndGroupByCommand")
	private WebElement validateDefaultAndGroupByCommand;

	@FindBy(id="validateByCommand:validateDisabledByCommand")
	private WebElement validateDisabledByCommand;

	@FindBy(id="validateByInput:validateDefaultByInput")
	private WebElement validateDefaultByInput;

	@FindBy(id="validateByInput:validateGroupByInput")
	private WebElement validateGroupByInput;

	@FindBy(id="validateByInput:validateDefaultAndGroupByInput")
	private WebElement validateDefaultAndGroupByInput;

	@FindBy(id="validateByInput:validateDisabledByInput")
	private WebElement validateDisabledByInput;

	@FindBy(id="validateDefaultWithMessageForViolating:input")
	private WebElement validateDefaultWithMessageForViolatingInput;

	@FindBy(id="validateDefaultWithMessageForViolating:inputMessage")
	private WebElement validateDefaultWithMessageForViolatingInputMessage;

	@FindBy(id="validateDefaultWithMessageForViolating:formMessage")
	private WebElement validateDefaultWithMessageForViolatingFormMessage;

	@FindBy(id="validateDefaultWithMessageForViolating:command")
	private WebElement validateDefaultWithMessageForViolatingCommand;

	@FindBy(id="validateClassLevelDefault:number1")
	private WebElement validateClassLevelDefaultNumber1;

	@FindBy(id="validateClassLevelDefault:number2")
	private WebElement validateClassLevelDefaultNumber2;

	@FindBy(id="validateClassLevelDefault:command")
	private WebElement validateClassLevelDefaultCommand;

	@FindBy(id="validateClassLevelActual:number1")
	private WebElement validateClassLevelActualNumber1;

	@FindBy(id="validateClassLevelActual:number2")
	private WebElement validateClassLevelActualNumber2;

	@FindBy(id="validateClassLevelActual:command")
	private WebElement validateClassLevelActualCommand;

	@FindBy(id="validateClassLevelByCopier:number1")
	private WebElement validateClassLevelByCopierNumber1;

	@FindBy(id="validateClassLevelByCopier:number2")
	private WebElement validateClassLevelByCopierNumber2;

	@FindBy(id="validateClassLevelByCopier:command")
	private WebElement validateClassLevelByCopierCommand;

	@FindBy(id="validateClassLevelWithMessageFormat:number1")
	private WebElement validateClassLevelWithMessageFormatNumber1;

	@FindBy(id="validateClassLevelWithMessageFormat:number2")
	private WebElement validateClassLevelWithMessageFormatNumber2;

	@FindBy(id="validateClassLevelWithMessageFormat:globalMessage")
	private WebElement validateClassLevelWithMessageFormatMessage;

	@FindBy(id="validateClassLevelWithMessageFormat:command")
	private WebElement validateClassLevelWithMessageFormatCommand;

	@FindBy(id="validateClassLevelWithMessageForForm:number1")
	private WebElement validateClassLevelWithMessageForFormNumber1;

	@FindBy(id="validateClassLevelWithMessageForForm:number2")
	private WebElement validateClassLevelWithMessageForFormNumber2;

	@FindBy(id="validateClassLevelWithMessageForForm:formMessage")
	private WebElement validateClassLevelWithMessageForFormMessage;

	@FindBy(id="validateClassLevelWithMessageForForm:command")
	private WebElement validateClassLevelWithMessageForFormCommand;

	@FindBy(id="validateClassLevelWithMessageForAll:number1")
	private WebElement validateClassLevelWithMessageForAllNumber1;

	@FindBy(id="validateClassLevelWithMessageForAll:number1Message")
	private WebElement validateClassLevelWithMessageForAllNumber1Message;

	@FindBy(id="validateClassLevelWithMessageForAll:number2")
	private WebElement validateClassLevelWithMessageForAllNumber2;

	@FindBy(id="validateClassLevelWithMessageForAll:number2Message")
	private WebElement validateClassLevelWithMessageForAllNumber2Message;

	@FindBy(id="validateClassLevelWithMessageForAll:command")
	private WebElement validateClassLevelWithMessageForAllCommand;

	@FindBy(id="validateClassLevelWithMessageForGlobal:number1")
	private WebElement validateClassLevelWithMessageForGlobalNumber1;

	@FindBy(id="validateClassLevelWithMessageForGlobal:number2")
	private WebElement validateClassLevelWithMessageForGlobalNumber2;

	@FindBy(id="validateClassLevelWithMessageForGlobal:globalMessage")
	private WebElement validateClassLevelWithMessageForGlobalMessage;

	@FindBy(id="validateClassLevelWithMessageForGlobal:command")
	private WebElement validateClassLevelWithMessageForGlobalCommand;

	@FindBy(id="validateClassLevelWithMessageForViolating:number1")
	private WebElement validateClassLevelWithMessageForViolatingNumber1;

	@FindBy(id="validateClassLevelWithMessageForViolating:number1Message")
	private WebElement validateClassLevelWithMessageForViolatingNumber1Message;

	@FindBy(id="validateClassLevelWithMessageForViolating:number2")
	private WebElement validateClassLevelWithMessageForViolatingNumber2;

	@FindBy(id="validateClassLevelWithMessageForViolating:number2Message")
	private WebElement validateClassLevelWithMessageForViolatingNumber2Message;

	@FindBy(id="validateClassLevelWithMessageForViolating:formMessage")
	private WebElement validateClassLevelWithMessageForViolatingFormMessage;

	@FindBy(id="validateClassLevelWithMessageForViolating:command")
	private WebElement validateClassLevelWithMessageForViolatingCommand;

	@FindBy(id="validateClassLevelActualWithMessageForViolating:number1")
	private WebElement validateClassLevelActualWithMessageForViolatingNumber1;

	@FindBy(id="validateClassLevelActualWithMessageForViolating:number1Message")
	private WebElement validateClassLevelActualWithMessageForViolatingNumber1Message;

	@FindBy(id="validateClassLevelActualWithMessageForViolating:number2")
	private WebElement validateClassLevelActualWithMessageForViolatingNumber2;

	@FindBy(id="validateClassLevelActualWithMessageForViolating:number2Message")
	private WebElement validateClassLevelActualWithMessageForViolatingNumber2Message;

	@FindBy(id="validateClassLevelActualWithMessageForViolating:formMessage")
	private WebElement validateClassLevelActualWithMessageForViolatingFormMessage;

	@FindBy(id="validateClassLevelActualWithMessageForViolating:command")
	private WebElement validateClassLevelActualWithMessageForViolatingCommand;

	@FindBy(id="validateConvertedEntityActualWithMessageForViolating:input")
	private WebElement validateConvertedEntityActualWithMessageForViolatingInput;

	@FindBy(id="validateConvertedEntityActualWithMessageForViolating:inputMessage")
	private WebElement validateConvertedEntityActualWithMessageForViolatingInputMessage;

	@FindBy(id="validateConvertedEntityActualWithMessageForViolating:formMessage")
	private WebElement validateConvertedEntityActualWithMessageForViolatingFormMessage;

	@FindBy(id="validateConvertedEntityActualWithMessageForViolating:command")
	private WebElement validateConvertedEntityActualWithMessageForViolatingCommand;

	@FindBy(id="validateNestedClassLevelWithMessageForViolating:number1")
	private WebElement validateNestedClassLevelWithMessageForViolatingNumber1;

	@FindBy(id="validateNestedClassLevelWithMessageForViolating:number1Message")
	private WebElement validateNestedClassLevelWithMessageForViolatingNumber1Message;

	@FindBy(id="validateNestedClassLevelWithMessageForViolating:number2")
	private WebElement validateNestedClassLevelWithMessageForViolatingNumber2;

	@FindBy(id="validateNestedClassLevelWithMessageForViolating:number2Message")
	private WebElement validateNestedClassLevelWithMessageForViolatingNumber2Message;

	@FindBy(id="validateNestedClassLevelWithMessageForViolating:formMessage")
	private WebElement validateNestedClassLevelWithMessageForViolatingFormMessage;

	@FindBy(id="validateNestedClassLevelWithMessageForViolating:command")
	private WebElement validateNestedClassLevelWithMessageForViolatingCommand;

	@FindBy(id="validateNestedClassLevelActualWithMessageForViolating:number1")
	private WebElement validateNestedClassLevelActualWithMessageForViolatingNumber1;

	@FindBy(id="validateNestedClassLevelActualWithMessageForViolating:number1Message")
	private WebElement validateNestedClassLevelActualWithMessageForViolatingNumber1Message;

	@FindBy(id="validateNestedClassLevelActualWithMessageForViolating:number2")
	private WebElement validateNestedClassLevelActualWithMessageForViolatingNumber2;

	@FindBy(id="validateNestedClassLevelActualWithMessageForViolating:number2Message")
	private WebElement validateNestedClassLevelActualWithMessageForViolatingNumber2Message;

	@FindBy(id="validateNestedClassLevelActualWithMessageForViolating:formMessage")
	private WebElement validateNestedClassLevelActualWithMessageForViolatingFormMessage;

	@FindBy(id="validateNestedClassLevelActualWithMessageForViolating:command")
	private WebElement validateNestedClassLevelActualWithMessageForViolatingCommand;

	@FindBy(id="validateNestedListClassLevelWithMessageForViolating:list:0:number1")
	private WebElement validateNestedListClassLevelWithMessageForViolatingList0Number1;

	@FindBy(id="validateNestedListClassLevelWithMessageForViolating:list:0:number1Message")
	private WebElement validateNestedListClassLevelWithMessageForViolatingList0Number1Message;

	@FindBy(id="validateNestedListClassLevelWithMessageForViolating:list:0:number2")
	private WebElement validateNestedListClassLevelWithMessageForViolatingList0Number2;

	@FindBy(id="validateNestedListClassLevelWithMessageForViolating:list:0:number2Message")
	private WebElement validateNestedListClassLevelWithMessageForViolatingList0Number2Message;

	@FindBy(id="validateNestedListClassLevelWithMessageForViolating:list:1:number1")
	private WebElement validateNestedListClassLevelWithMessageForViolatingList1Number1;

	@FindBy(id="validateNestedListClassLevelWithMessageForViolating:list:1:number1Message")
	private WebElement validateNestedListClassLevelWithMessageForViolatingList1Number1Message;

	@FindBy(id="validateNestedListClassLevelWithMessageForViolating:list:1:number2")
	private WebElement validateNestedListClassLevelWithMessageForViolatingList1Number2;

	@FindBy(id="validateNestedListClassLevelWithMessageForViolating:list:1:number2Message")
	private WebElement validateNestedListClassLevelWithMessageForViolatingList1Number2Message;

	@FindBy(id="validateNestedListClassLevelWithMessageForViolating:formMessage")
	private WebElement validateNestedListClassLevelWithMessageForViolatingFormMessage;

	@FindBy(id="validateNestedListClassLevelWithMessageForViolating:command")
	private WebElement validateNestedListClassLevelWithMessageForViolatingCommand;

	@FindBy(id="validateNestedListClassLevelActualWithMessageForViolating:list:0:number1")
	private WebElement validateNestedListClassLevelActualWithMessageForViolatingList0Number1;

	@FindBy(id="validateNestedListClassLevelActualWithMessageForViolating:list:0:number1Message")
	private WebElement validateNestedListClassLevelActualWithMessageForViolatingList0Number1Message;

	@FindBy(id="validateNestedListClassLevelActualWithMessageForViolating:list:0:number2")
	private WebElement validateNestedListClassLevelActualWithMessageForViolatingList0Number2;

	@FindBy(id="validateNestedListClassLevelActualWithMessageForViolating:list:0:number2Message")
	private WebElement validateNestedListClassLevelActualWithMessageForViolatingList0Number2Message;

	@FindBy(id="validateNestedListClassLevelActualWithMessageForViolating:list:1:number1")
	private WebElement validateNestedListClassLevelActualWithMessageForViolatingList1Number1;

	@FindBy(id="validateNestedListClassLevelActualWithMessageForViolating:list:1:number1Message")
	private WebElement validateNestedListClassLevelActualWithMessageForViolatingList1Number1Message;

	@FindBy(id="validateNestedListClassLevelActualWithMessageForViolating:list:1:number2")
	private WebElement validateNestedListClassLevelActualWithMessageForViolatingList1Number2;

	@FindBy(id="validateNestedListClassLevelActualWithMessageForViolating:list:1:number2Message")
	private WebElement validateNestedListClassLevelActualWithMessageForViolatingList1Number2Message;

	@FindBy(id="validateNestedListClassLevelActualWithMessageForViolating:formMessage")
	private WebElement validateNestedListClassLevelActualWithMessageForViolatingFormMessage;

	@FindBy(id="validateNestedListClassLevelActualWithMessageForViolating:command")
	private WebElement validateNestedListClassLevelActualWithMessageForViolatingCommand;

	@FindBy(id="validateDoubleNestedListClassLevelWithMessageForViolating:nested:0:list:0:number1")
	private WebElement validateDoubleNestedListClassLevelWithMessageForViolatingList0Number1;

	@FindBy(id="validateDoubleNestedListClassLevelWithMessageForViolating:nested:0:list:0:number1Message")
	private WebElement validateDoubleNestedListClassLevelWithMessageForViolatingList0Number1Message;

	@FindBy(id="validateDoubleNestedListClassLevelWithMessageForViolating:nested:0:list:0:number2")
	private WebElement validateDoubleNestedListClassLevelWithMessageForViolatingList0Number2;

	@FindBy(id="validateDoubleNestedListClassLevelWithMessageForViolating:nested:0:list:0:number2Message")
	private WebElement validateDoubleNestedListClassLevelWithMessageForViolatingList0Number2Message;

	@FindBy(id="validateDoubleNestedListClassLevelWithMessageForViolating:nested:0:list:1:number1")
	private WebElement validateDoubleNestedListClassLevelWithMessageForViolatingList1Number1;

	@FindBy(id="validateDoubleNestedListClassLevelWithMessageForViolating:nested:0:list:1:number1Message")
	private WebElement validateDoubleNestedListClassLevelWithMessageForViolatingList1Number1Message;

	@FindBy(id="validateDoubleNestedListClassLevelWithMessageForViolating:nested:0:list:1:number2")
	private WebElement validateDoubleNestedListClassLevelWithMessageForViolatingList1Number2;

	@FindBy(id="validateDoubleNestedListClassLevelWithMessageForViolating:nested:0:list:1:number2Message")
	private WebElement validateDoubleNestedListClassLevelWithMessageForViolatingList1Number2Message;

	@FindBy(id="validateDoubleNestedListClassLevelWithMessageForViolating:formMessage")
	private WebElement validateDoubleNestedListClassLevelWithMessageForViolatingFormMessage;

	@FindBy(id="validateDoubleNestedListClassLevelWithMessageForViolating:command")
	private WebElement validateDoubleNestedListClassLevelWithMessageForViolatingCommand;

	@FindBy(id="validateDoubleNestedListClassLevelActualWithMessageForViolating:nested:0:list:0:number1")
	private WebElement validateDoubleNestedListClassLevelActualWithMessageForViolatingList0Number1;

	@FindBy(id="validateDoubleNestedListClassLevelActualWithMessageForViolating:nested:0:list:0:number1Message")
	private WebElement validateDoubleNestedListClassLevelActualWithMessageForViolatingList0Number1Message;

	@FindBy(id="validateDoubleNestedListClassLevelActualWithMessageForViolating:nested:0:list:0:number2")
	private WebElement validateDoubleNestedListClassLevelActualWithMessageForViolatingList0Number2;

	@FindBy(id="validateDoubleNestedListClassLevelActualWithMessageForViolating:nested:0:list:0:number2Message")
	private WebElement validateDoubleNestedListClassLevelActualWithMessageForViolatingList0Number2Message;

	@FindBy(id="validateDoubleNestedListClassLevelActualWithMessageForViolating:nested:0:list:1:number1")
	private WebElement validateDoubleNestedListClassLevelActualWithMessageForViolatingList1Number1;

	@FindBy(id="validateDoubleNestedListClassLevelActualWithMessageForViolating:nested:0:list:1:number1Message")
	private WebElement validateDoubleNestedListClassLevelActualWithMessageForViolatingList1Number1Message;

	@FindBy(id="validateDoubleNestedListClassLevelActualWithMessageForViolating:nested:0:list:1:number2")
	private WebElement validateDoubleNestedListClassLevelActualWithMessageForViolatingList1Number2;

	@FindBy(id="validateDoubleNestedListClassLevelActualWithMessageForViolating:nested:0:list:1:number2Message")
	private WebElement validateDoubleNestedListClassLevelActualWithMessageForViolatingList1Number2Message;

	@FindBy(id="validateDoubleNestedListClassLevelActualWithMessageForViolating:formMessage")
	private WebElement validateDoubleNestedListClassLevelActualWithMessageForViolatingFormMessage;

	@FindBy(id="validateDoubleNestedListClassLevelActualWithMessageForViolating:command")
	private WebElement validateDoubleNestedListClassLevelActualWithMessageForViolatingCommand;

	@FindBy(id="validateClassLevelWithInputEntityComposite:composite:number1")
	private WebElement validateClassLevelWithInputEntityCompositeNumber1;

	@FindBy(id="validateClassLevelWithInputEntityComposite:composite:number1Message")
	private WebElement validateClassLevelWithInputEntityCompositeNumber1Message;

	@FindBy(id="validateClassLevelWithInputEntityComposite:composite:number2")
	private WebElement validateClassLevelWithInputEntityCompositeNumber2;

	@FindBy(id="validateClassLevelWithInputEntityComposite:composite:number2Message")
	private WebElement validateClassLevelWithInputEntityCompositeNumber2Message;

	@FindBy(id="validateClassLevelWithInputEntityComposite:formMessage")
	private WebElement validateClassLevelWithInputEntityCompositeFormMessage;

	@FindBy(id="validateClassLevelWithInputEntityComposite:command")
	private WebElement validateClassLevelWithInputEntityCompositeCommand;

	@FindBy(id="validateClassLevelWithFormEntityComposite:form:number1")
	private WebElement validateClassLevelWithFormEntityCompositeNumber1;

	@FindBy(id="validateClassLevelWithFormEntityComposite:form:number1Message")
	private WebElement validateClassLevelWithFormEntityCompositeNumber1Message;

	@FindBy(id="validateClassLevelWithFormEntityComposite:form:number2")
	private WebElement validateClassLevelWithFormEntityCompositeNumber2;

	@FindBy(id="validateClassLevelWithFormEntityComposite:form:number2Message")
	private WebElement validateClassLevelWithFormEntityCompositeNumber2Message;

	@FindBy(id="validateClassLevelWithFormEntityComposite:form:formMessage")
	private WebElement validateClassLevelWithFormEntityCompositeFormMessage;

	@FindBy(id="validateClassLevelWithFormEntityComposite:form:command")
	private WebElement validateClassLevelWithFormEntityCompositeCommand;

    @FindBy(id="validateBeanWithCustomTypeAsProperty:flightNumbers:0:input")
    private WebElement validateBeanWithCustomTypeAsPropertyFlightNumber1;

    @FindBy(id="validateBeanWithCustomTypeAsProperty:flightNumbers:0:message")
    private WebElement validateBeanWithCustomTypeAsPropertyFlightNumber1Message;

    @FindBy(id="validateBeanWithCustomTypeAsProperty:flightNumbers:1:input")
    private WebElement validateBeanWithCustomTypeAsPropertyFlightNumber2;

    @FindBy(id="validateBeanWithCustomTypeAsProperty:flightNumbers:1:message")
    private WebElement validateBeanWithCustomTypeAsPropertyFlightNumber2Message;

    @FindBy(id="validateBeanWithCustomTypeAsProperty:command")
    private WebElement validateBeanWithCustomTypeAsPropertyCommand;

    @FindBy(id="validateBeanWithCustomTypeAsProperty:messages")
    private WebElement validateBeanWithCustomTypeAsPropertyMessages;

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return buildWebArchive(ValidateBeanIT.class)
			.withFacesConfig(withMessageBundle)
			.createDeployment();
	}

	@Test
	public void validateByCommand() {
		input.sendKeys("x");
		guardAjax(validateDefaultByCommand).click();
		assertEquals("inputLabel: default", messages.getText());

		input.clear();
		input.sendKeys("xx");
		guardAjax(validateDefaultByCommand).click();
		assertEquals("actionSuccess", messages.getText());

		input.clear();
		input.sendKeys("x");
		guardAjax(validateGroupByCommand).click();
		assertEquals("inputLabel: group", messages.getText());

		input.clear();
		input.sendKeys("xx");
		guardAjax(validateGroupByCommand).click();
		assertEquals("actionSuccess", messages.getText());

		input.clear();
		input.sendKeys("x");
		guardAjax(validateDefaultAndGroupByCommand).click();
		String message = messages.getText();
		assertTrue(message.contains("inputLabel: default") && message.contains("inputLabel: group")); // It's unordered.

		input.clear();
		input.sendKeys("xx");
		guardAjax(validateDefaultAndGroupByCommand).click();
		assertEquals("actionSuccess", messages.getText());

		input.clear();
		input.sendKeys("x");
		guardAjax(validateDisabledByCommand).click();
		assertEquals("actionSuccess", messages.getText());

		input.clear();
		input.sendKeys("xx");
		guardAjax(validateDisabledByCommand).click();
		assertEquals("actionSuccess", messages.getText());
	}

	@Test
	public void validateByInput() {
		validateDefaultByInput.sendKeys("x");
		triggerOnchange(validateDefaultByInput, messages);
		assertEquals("validateDefaultByInputLabel: default", messages.getText());

		validateDefaultByInput.clear();
		validateDefaultByInput.sendKeys("xx");
		triggerOnchange(validateDefaultByInput, messages);
		assertEquals("actionSuccess", messages.getText());

		validateGroupByInput.sendKeys("x");
		triggerOnchange(validateGroupByInput, messages);
		assertEquals("validateGroupByInputLabel: group", messages.getText());

		validateGroupByInput.clear();
		validateGroupByInput.sendKeys("xx");
		triggerOnchange(validateGroupByInput, messages);
		assertEquals("actionSuccess", messages.getText());

		validateDefaultAndGroupByInput.sendKeys("x");
		triggerOnchange(validateDefaultAndGroupByInput, messages);
		String message = messages.getText();
		assertTrue(message.contains("validateDefaultAndGroupByInputLabel: default") && message.contains("validateDefaultAndGroupByInputLabel: group")); // It's unordered.

		validateDefaultAndGroupByInput.clear();
		validateDefaultAndGroupByInput.sendKeys("xx");
		triggerOnchange(validateDefaultAndGroupByInput, messages);
		assertEquals("actionSuccess", messages.getText());

		validateDisabledByInput.sendKeys("x");
		triggerOnchange(validateDisabledByInput, messages);
		assertEquals("actionSuccess", messages.getText());

		validateDisabledByInput.clear();
		validateDisabledByInput.sendKeys("xx");
		triggerOnchange(validateDisabledByInput, messages);
		assertEquals("actionSuccess", messages.getText());
	}

	@Test
	public void validateDefaultWithMessageForViolating() {
		validateDefaultWithMessageForViolatingInput.sendKeys("x");
		guardAjax(validateDefaultWithMessageForViolatingCommand).click();
		assertEquals("inputLabel: default", validateDefaultWithMessageForViolatingInputMessage.getText());
		assertEquals("", validateDefaultWithMessageForViolatingFormMessage.getText()); // Should NOT equal "may not be null" coming from @NotNull unused.

		validateDefaultWithMessageForViolatingInput.clear();
		validateDefaultWithMessageForViolatingInput.sendKeys("xx");
		guardAjax(validateDefaultWithMessageForViolatingCommand).click();
		assertEquals("actionSuccess", messages.getText());
	}

	@Test
	public void validateClassLevelDefault() {
		validateClassLevelDefaultNumber1.sendKeys("2");
		validateClassLevelDefaultNumber2.sendKeys("1");
		guardAjax(validateClassLevelDefaultCommand).click();
		assertEquals("number1Label, number2Label: invalidEntity", messages.getText());

		validateClassLevelDefaultNumber2.sendKeys("0"); // So it becomes 10
		guardAjax(validateClassLevelDefaultCommand).click();
		assertEquals("actionSuccess", messages.getText());
	}

	@Test
	public void validateClassLevelActual() {
		validateClassLevelActualNumber1.sendKeys("2");
		validateClassLevelActualNumber2.sendKeys("1");
		guardAjax(validateClassLevelActualCommand).click();
		assertEquals("invalidEntity actionValidationFailed", messages.getText());

		validateClassLevelActualNumber2.sendKeys("0"); // So it becomes 10
		guardAjax(validateClassLevelActualCommand).click();
		assertEquals("actionSuccess", messages.getText());
	}

	@Test
	public void validateClassLevelByCopier() {
		validateClassLevelByCopierNumber1.sendKeys("2");
		validateClassLevelByCopierNumber2.sendKeys("1");
		guardAjax(validateClassLevelByCopierCommand).click();
		assertEquals("number1Label, number2Label: invalidEntity", messages.getText());

		validateClassLevelByCopierNumber2.sendKeys("0"); // So it becomes 10
		guardAjax(validateClassLevelByCopierCommand).click();
		assertEquals("actionSuccess", messages.getText());
	}

	@Test
	public void validateClassLevelWithMessageFormat() {
		validateClassLevelWithMessageFormatNumber1.sendKeys("2");
		validateClassLevelWithMessageFormatNumber2.sendKeys("1");
		guardAjax(validateClassLevelWithMessageFormatCommand).click();
		assertEquals("Numbers: invalidEntity", messages.getText());

		validateClassLevelWithMessageFormatNumber2.sendKeys("0"); // So it becomes 10
		guardAjax(validateClassLevelWithMessageFormatCommand).click();
		assertEquals("actionSuccess", messages.getText());
	}

	@Test
	public void validateClassLevelWithMessageForForm() {
		validateClassLevelWithMessageForFormNumber1.sendKeys("2");
		validateClassLevelWithMessageForFormNumber2.sendKeys("1");
		guardAjax(validateClassLevelWithMessageForFormCommand).click();
		assertEquals("number1Label, number2Label: invalidEntity", validateClassLevelWithMessageForFormMessage.getText());

		validateClassLevelWithMessageForFormNumber2.sendKeys("0"); // So it becomes 10
		guardAjax(validateClassLevelWithMessageForFormCommand).click();
		assertEquals("", validateClassLevelWithMessageForFormMessage.getText());
	}

	@Test
	public void validateClassLevelWithMessageForAll() {
		validateClassLevelWithMessageForAllNumber1.sendKeys("2");
		validateClassLevelWithMessageForAllNumber2.sendKeys("1");
		guardAjax(validateClassLevelWithMessageForAllCommand).click();
		assertEquals("number1Label, number2Label: invalidEntity", validateClassLevelWithMessageForAllNumber1Message.getText());
		assertEquals("number1Label, number2Label: invalidEntity", validateClassLevelWithMessageForAllNumber2Message.getText());

		validateClassLevelWithMessageForAllNumber2.sendKeys("0"); // So it becomes 10
		guardAjax(validateClassLevelWithMessageForAllCommand).click();
		assertEquals("", validateClassLevelWithMessageForAllNumber1Message.getText());
		assertEquals("", validateClassLevelWithMessageForAllNumber2Message.getText());
	}

	@Test
	public void validateClassLevelWithMessageForGlobal() {
		validateClassLevelWithMessageForGlobalNumber1.sendKeys("2");
		validateClassLevelWithMessageForGlobalNumber2.sendKeys("1");
		guardAjax(validateClassLevelWithMessageForGlobalCommand).click();
		assertEquals("number1Label, number2Label: invalidEntity", validateClassLevelWithMessageForGlobalMessage.getText());

		validateClassLevelWithMessageForGlobalNumber2.sendKeys("0"); // So it becomes 10
		guardAjax(validateClassLevelWithMessageForGlobalCommand).click();
		assertEquals("actionSuccess", validateClassLevelWithMessageForGlobalMessage.getText());
	}

	@Test
	public void validateClassLevelWithMessageForViolating() {
		validateClassLevelWithMessageForViolatingNumber1.sendKeys("2");
		validateClassLevelWithMessageForViolatingNumber2.sendKeys("1");
		guardAjax(validateClassLevelWithMessageForViolatingCommand).click();
		assertEquals("number1Label: invalidEntity", validateClassLevelWithMessageForViolatingNumber1Message.getText());
		assertEquals("", validateClassLevelWithMessageForViolatingNumber2Message.getText());
		assertEquals("", validateClassLevelWithMessageForViolatingFormMessage.getText());
		assertEquals("", messages.getText());

		validateClassLevelWithMessageForViolatingNumber2.sendKeys("0"); // So it becomes 10
		guardAjax(validateClassLevelWithMessageForViolatingCommand).click();
		assertEquals("", validateClassLevelWithMessageForViolatingNumber1Message.getText());
		assertEquals("", validateClassLevelWithMessageForViolatingNumber2Message.getText());
		assertEquals("", validateClassLevelWithMessageForViolatingFormMessage.getText());
		assertEquals("actionSuccess", messages.getText());
	}

	@Test
	public void validateClassLevelActualWithMessageForViolating() {
		validateClassLevelActualWithMessageForViolatingNumber1.sendKeys("2");
		validateClassLevelActualWithMessageForViolatingNumber2.sendKeys("1");
		guardAjax(validateClassLevelActualWithMessageForViolatingCommand).click();
		assertEquals("number1Label: invalidEntity", validateClassLevelActualWithMessageForViolatingNumber1Message.getText());
		assertEquals("", validateClassLevelActualWithMessageForViolatingNumber2Message.getText());
		assertEquals("", validateClassLevelActualWithMessageForViolatingFormMessage.getText());
		assertEquals("actionValidationFailed", messages.getText());

		validateClassLevelActualWithMessageForViolatingNumber2.sendKeys("0"); // So it becomes 10
		guardAjax(validateClassLevelActualWithMessageForViolatingCommand).click();
		assertEquals("", validateClassLevelActualWithMessageForViolatingNumber1Message.getText());
		assertEquals("", validateClassLevelActualWithMessageForViolatingNumber2Message.getText());
		assertEquals("", validateClassLevelActualWithMessageForViolatingFormMessage.getText());
		assertEquals("actionSuccess", messages.getText());
	}

	@Test
	public void validateConvertedEntityActualWithMessageForViolating() {
		guardAjax(validateConvertedEntityActualWithMessageForViolatingCommand).click();
		assertEquals("inputLabel: please fill out", validateConvertedEntityActualWithMessageForViolatingInputMessage.getText());
		assertEquals("", validateConvertedEntityActualWithMessageForViolatingFormMessage.getText());
		assertEquals("actionValidationFailed", messages.getText());

		validateConvertedEntityActualWithMessageForViolatingInput.sendKeys("value");
		guardAjax(validateConvertedEntityActualWithMessageForViolatingCommand).click();
		assertEquals("", validateConvertedEntityActualWithMessageForViolatingInputMessage.getText());
		assertEquals("", validateConvertedEntityActualWithMessageForViolatingFormMessage.getText());
		assertEquals("actionSuccess", messages.getText());
	}

	@Test
	public void validateNestedClassLevelWithMessageForViolating() {
		validateNestedClassLevelWithMessageForViolatingNumber1.sendKeys("2");
		validateNestedClassLevelWithMessageForViolatingNumber2.sendKeys("1");
		guardAjax(validateNestedClassLevelWithMessageForViolatingCommand).click();
		assertEquals("number1Label: invalidEntity", validateNestedClassLevelWithMessageForViolatingNumber1Message.getText());
		assertEquals("", validateNestedClassLevelWithMessageForViolatingNumber2Message.getText());
		assertEquals("", validateNestedClassLevelWithMessageForViolatingFormMessage.getText());
		assertEquals("", messages.getText());

		validateNestedClassLevelWithMessageForViolatingNumber2.sendKeys("0"); // So it becomes 10
		guardAjax(validateNestedClassLevelWithMessageForViolatingCommand).click();
		assertEquals("", validateNestedClassLevelWithMessageForViolatingNumber1Message.getText());
		assertEquals("", validateNestedClassLevelWithMessageForViolatingNumber2Message.getText());
		assertEquals("", validateNestedClassLevelWithMessageForViolatingFormMessage.getText());
		assertEquals("actionSuccess", messages.getText());
	}

	@Test
	public void validateNestedClassLevelActualWithMessageForViolating() {
		validateNestedClassLevelActualWithMessageForViolatingNumber1.sendKeys("2");
		validateNestedClassLevelActualWithMessageForViolatingNumber2.sendKeys("1");
		guardAjax(validateNestedClassLevelActualWithMessageForViolatingCommand).click();
		assertEquals("number1Label: invalidEntity", validateNestedClassLevelActualWithMessageForViolatingNumber1Message.getText());
		assertEquals("", validateNestedClassLevelActualWithMessageForViolatingNumber2Message.getText());
		assertEquals("", validateNestedClassLevelActualWithMessageForViolatingFormMessage.getText());
		assertEquals("actionValidationFailed", messages.getText());

		validateNestedClassLevelActualWithMessageForViolatingNumber2.sendKeys("0"); // So it becomes 10
		guardAjax(validateNestedClassLevelActualWithMessageForViolatingCommand).click();
		assertEquals("", validateNestedClassLevelActualWithMessageForViolatingNumber1Message.getText());
		assertEquals("", validateNestedClassLevelActualWithMessageForViolatingNumber2Message.getText());
		assertEquals("", validateNestedClassLevelActualWithMessageForViolatingFormMessage.getText());
		assertEquals("actionSuccess", messages.getText());
	}

	@Test
	public void validateNestedListClassLevelWithMessageForViolating() {
		validateNestedListClassLevelWithMessageForViolatingList0Number1.sendKeys("2");
		validateNestedListClassLevelWithMessageForViolatingList0Number2.sendKeys("1");
		validateNestedListClassLevelWithMessageForViolatingList1Number1.sendKeys("2");
		validateNestedListClassLevelWithMessageForViolatingList1Number2.sendKeys("1");
		guardAjax(validateNestedListClassLevelWithMessageForViolatingCommand).click();
		assertEquals("number1Label: invalidEntity", validateNestedListClassLevelWithMessageForViolatingList0Number1Message.getText());
		assertEquals("", validateNestedListClassLevelWithMessageForViolatingList0Number2Message.getText());
		assertEquals("number1Label: invalidEntity", validateNestedListClassLevelWithMessageForViolatingList1Number1Message.getText());
		assertEquals("", validateNestedListClassLevelWithMessageForViolatingList0Number2Message.getText());
		assertEquals("", validateNestedListClassLevelWithMessageForViolatingFormMessage.getText());
		assertEquals("", messages.getText());

		validateNestedListClassLevelWithMessageForViolatingList0Number2.sendKeys("0"); // So it becomes 10
		validateNestedListClassLevelWithMessageForViolatingList1Number2.sendKeys("0"); // So it becomes 10
		guardAjax(validateNestedListClassLevelWithMessageForViolatingCommand).click();
		assertEquals("", validateNestedListClassLevelWithMessageForViolatingList0Number1Message.getText());
		assertEquals("", validateNestedListClassLevelWithMessageForViolatingList0Number2Message.getText());
		assertEquals("", validateNestedListClassLevelWithMessageForViolatingList1Number1Message.getText());
		assertEquals("", validateNestedListClassLevelWithMessageForViolatingList1Number2Message.getText());
		assertEquals("", validateNestedListClassLevelWithMessageForViolatingFormMessage.getText());
		assertEquals("actionSuccess", messages.getText());
	}

	@Test
	public void validateNestedListClassLevelWithMessagesForViolating() {
		validateNestedListClassLevelWithMessageForViolatingList0Number1.sendKeys("1"); // So custom property path is not set in ValidateBeanITEntityValidator.
		validateNestedListClassLevelWithMessageForViolatingList0Number2.sendKeys("1");
		validateNestedListClassLevelWithMessageForViolatingList1Number1.sendKeys("1"); // So custom property path is not set in ValidateBeanITEntityValidator.
		validateNestedListClassLevelWithMessageForViolatingList1Number2.sendKeys("1");
		guardAjax(validateNestedListClassLevelWithMessageForViolatingCommand).click();
		assertEquals("number1Label: invalidEntity", validateNestedListClassLevelWithMessageForViolatingList0Number1Message.getText());
		assertEquals("number2Label: invalidEntity", validateNestedListClassLevelWithMessageForViolatingList0Number2Message.getText());
		assertEquals("number1Label: invalidEntity", validateNestedListClassLevelWithMessageForViolatingList1Number1Message.getText());
		assertEquals("number2Label: invalidEntity", validateNestedListClassLevelWithMessageForViolatingList0Number2Message.getText());
		assertEquals("", validateNestedListClassLevelWithMessageForViolatingFormMessage.getText());
		assertEquals("", messages.getText());

		validateNestedListClassLevelWithMessageForViolatingList0Number2.sendKeys("0"); // So it becomes 10
		validateNestedListClassLevelWithMessageForViolatingList1Number2.sendKeys("0"); // So it becomes 10
		guardAjax(validateNestedListClassLevelWithMessageForViolatingCommand).click();
		assertEquals("", validateNestedListClassLevelWithMessageForViolatingList0Number1Message.getText());
		assertEquals("", validateNestedListClassLevelWithMessageForViolatingList0Number2Message.getText());
		assertEquals("", validateNestedListClassLevelWithMessageForViolatingList1Number1Message.getText());
		assertEquals("", validateNestedListClassLevelWithMessageForViolatingList1Number2Message.getText());
		assertEquals("", validateNestedListClassLevelWithMessageForViolatingFormMessage.getText());
		assertEquals("actionSuccess", messages.getText());
	}

	@Test
	public void validateNestedListClassLevelActualWithMessageForViolating() {
		validateNestedListClassLevelActualWithMessageForViolatingList0Number1.sendKeys("2");
		validateNestedListClassLevelActualWithMessageForViolatingList0Number2.sendKeys("1");
		validateNestedListClassLevelActualWithMessageForViolatingList1Number1.sendKeys("2");
		validateNestedListClassLevelActualWithMessageForViolatingList1Number2.sendKeys("1");
		guardAjax(validateNestedListClassLevelActualWithMessageForViolatingCommand).click();
		assertEquals("number1Label: invalidEntity", validateNestedListClassLevelActualWithMessageForViolatingList0Number1Message.getText());
		assertEquals("", validateNestedListClassLevelActualWithMessageForViolatingList0Number2Message.getText());
		assertEquals("number1Label: invalidEntity", validateNestedListClassLevelActualWithMessageForViolatingList1Number1Message.getText());
		assertEquals("", validateNestedListClassLevelActualWithMessageForViolatingList0Number2Message.getText());
		assertEquals("", validateNestedListClassLevelActualWithMessageForViolatingFormMessage.getText());
		assertEquals("actionValidationFailed", messages.getText());

		validateNestedListClassLevelActualWithMessageForViolatingList0Number2.sendKeys("0"); // So it becomes 10
		validateNestedListClassLevelActualWithMessageForViolatingList1Number2.sendKeys("0"); // So it becomes 10
		guardAjax(validateNestedListClassLevelActualWithMessageForViolatingCommand).click();
		assertEquals("", validateNestedListClassLevelActualWithMessageForViolatingList0Number1Message.getText());
		assertEquals("", validateNestedListClassLevelActualWithMessageForViolatingList0Number2Message.getText());
		assertEquals("", validateNestedListClassLevelActualWithMessageForViolatingList1Number1Message.getText());
		assertEquals("", validateNestedListClassLevelActualWithMessageForViolatingList1Number2Message.getText());
		assertEquals("", validateNestedListClassLevelActualWithMessageForViolatingFormMessage.getText());
		assertEquals("actionSuccess", messages.getText());
	}

	@Test
	public void validateNestedListClassLevelActualWithMessagesForViolating() {
		validateNestedListClassLevelActualWithMessageForViolatingList0Number1.sendKeys("1"); // So custom property path is not set in ValidateBeanITEntityValidator.
		validateNestedListClassLevelActualWithMessageForViolatingList0Number2.sendKeys("1");
		validateNestedListClassLevelActualWithMessageForViolatingList1Number1.sendKeys("1"); // So custom property path is not set in ValidateBeanITEntityValidator.
		validateNestedListClassLevelActualWithMessageForViolatingList1Number2.sendKeys("1");
		guardAjax(validateNestedListClassLevelActualWithMessageForViolatingCommand).click();
		assertEquals("number1Label: invalidEntity", validateNestedListClassLevelActualWithMessageForViolatingList0Number1Message.getText());
		assertEquals("number2Label: invalidEntity", validateNestedListClassLevelActualWithMessageForViolatingList0Number2Message.getText());
		assertEquals("number1Label: invalidEntity", validateNestedListClassLevelActualWithMessageForViolatingList1Number1Message.getText());
		assertEquals("number2Label: invalidEntity", validateNestedListClassLevelActualWithMessageForViolatingList0Number2Message.getText());
		assertEquals("", validateNestedListClassLevelActualWithMessageForViolatingFormMessage.getText());
		assertEquals("actionValidationFailed", messages.getText());

		validateNestedListClassLevelActualWithMessageForViolatingList0Number2.sendKeys("0"); // So it becomes 10
		validateNestedListClassLevelActualWithMessageForViolatingList1Number2.sendKeys("0"); // So it becomes 10
		guardAjax(validateNestedListClassLevelActualWithMessageForViolatingCommand).click();
		assertEquals("", validateNestedListClassLevelActualWithMessageForViolatingList0Number1Message.getText());
		assertEquals("", validateNestedListClassLevelActualWithMessageForViolatingList0Number2Message.getText());
		assertEquals("", validateNestedListClassLevelActualWithMessageForViolatingList1Number1Message.getText());
		assertEquals("", validateNestedListClassLevelActualWithMessageForViolatingList1Number2Message.getText());
		assertEquals("", validateNestedListClassLevelActualWithMessageForViolatingFormMessage.getText());
		assertEquals("actionSuccess", messages.getText());
	}

	@Test
	public void validateDoubleNestedListClassLevelWithMessageForViolating() {
		validateDoubleNestedListClassLevelWithMessageForViolatingList0Number1.sendKeys("2");
		validateDoubleNestedListClassLevelWithMessageForViolatingList0Number2.sendKeys("1");
		validateDoubleNestedListClassLevelWithMessageForViolatingList1Number1.sendKeys("2");
		validateDoubleNestedListClassLevelWithMessageForViolatingList1Number2.sendKeys("1");
		guardAjax(validateDoubleNestedListClassLevelWithMessageForViolatingCommand).click();
		assertEquals("number1Label: invalidEntity", validateDoubleNestedListClassLevelWithMessageForViolatingList0Number1Message.getText());
		assertEquals("", validateDoubleNestedListClassLevelWithMessageForViolatingList0Number2Message.getText());
		assertEquals("number1Label: invalidEntity", validateDoubleNestedListClassLevelWithMessageForViolatingList1Number1Message.getText());
		assertEquals("", validateDoubleNestedListClassLevelWithMessageForViolatingList0Number2Message.getText());
		assertEquals("", validateDoubleNestedListClassLevelWithMessageForViolatingFormMessage.getText());
		assertEquals("", messages.getText());

		validateDoubleNestedListClassLevelWithMessageForViolatingList0Number2.sendKeys("0"); // So it becomes 10
		validateDoubleNestedListClassLevelWithMessageForViolatingList1Number2.sendKeys("0"); // So it becomes 10
		guardAjax(validateDoubleNestedListClassLevelWithMessageForViolatingCommand).click();
		assertEquals("", validateDoubleNestedListClassLevelWithMessageForViolatingList0Number1Message.getText());
		assertEquals("", validateDoubleNestedListClassLevelWithMessageForViolatingList0Number2Message.getText());
		assertEquals("", validateDoubleNestedListClassLevelWithMessageForViolatingList1Number1Message.getText());
		assertEquals("", validateDoubleNestedListClassLevelWithMessageForViolatingList1Number2Message.getText());
		assertEquals("", validateDoubleNestedListClassLevelWithMessageForViolatingFormMessage.getText());
		assertEquals("actionSuccess", messages.getText());
	}

	@Test
	public void validateDoubleNestedListClassLevelWithMessagesForViolating() {
		validateDoubleNestedListClassLevelWithMessageForViolatingList0Number1.sendKeys("1"); // So custom property path is not set in ValidateBeanITEntityValidator.
		validateDoubleNestedListClassLevelWithMessageForViolatingList0Number2.sendKeys("1");
		validateDoubleNestedListClassLevelWithMessageForViolatingList1Number1.sendKeys("1"); // So custom property path is not set in ValidateBeanITEntityValidator.
		validateDoubleNestedListClassLevelWithMessageForViolatingList1Number2.sendKeys("1");
		guardAjax(validateDoubleNestedListClassLevelWithMessageForViolatingCommand).click();
		assertEquals("number1Label: invalidEntity", validateDoubleNestedListClassLevelWithMessageForViolatingList0Number1Message.getText());
		assertEquals("number2Label: invalidEntity", validateDoubleNestedListClassLevelWithMessageForViolatingList0Number2Message.getText());
		assertEquals("number1Label: invalidEntity", validateDoubleNestedListClassLevelWithMessageForViolatingList1Number1Message.getText());
		assertEquals("number2Label: invalidEntity", validateDoubleNestedListClassLevelWithMessageForViolatingList0Number2Message.getText());
		assertEquals("", validateDoubleNestedListClassLevelWithMessageForViolatingFormMessage.getText());
		assertEquals("", messages.getText());

		validateDoubleNestedListClassLevelWithMessageForViolatingList0Number2.sendKeys("0"); // So it becomes 10
		validateDoubleNestedListClassLevelWithMessageForViolatingList1Number2.sendKeys("0"); // So it becomes 10
		guardAjax(validateDoubleNestedListClassLevelWithMessageForViolatingCommand).click();
		assertEquals("", validateDoubleNestedListClassLevelWithMessageForViolatingList0Number1Message.getText());
		assertEquals("", validateDoubleNestedListClassLevelWithMessageForViolatingList0Number2Message.getText());
		assertEquals("", validateDoubleNestedListClassLevelWithMessageForViolatingList1Number1Message.getText());
		assertEquals("", validateDoubleNestedListClassLevelWithMessageForViolatingList1Number2Message.getText());
		assertEquals("", validateDoubleNestedListClassLevelWithMessageForViolatingFormMessage.getText());
		assertEquals("actionSuccess", messages.getText());
	}

	@Test
	public void validateDoubleNestedListClassLevelActualWithMessageForViolating() {
		validateDoubleNestedListClassLevelActualWithMessageForViolatingList0Number1.sendKeys("2");
		validateDoubleNestedListClassLevelActualWithMessageForViolatingList0Number2.sendKeys("1");
		validateDoubleNestedListClassLevelActualWithMessageForViolatingList1Number1.sendKeys("2");
		validateDoubleNestedListClassLevelActualWithMessageForViolatingList1Number2.sendKeys("1");
		guardAjax(validateDoubleNestedListClassLevelActualWithMessageForViolatingCommand).click();
		assertEquals("number1Label: invalidEntity", validateDoubleNestedListClassLevelActualWithMessageForViolatingList0Number1Message.getText());
		assertEquals("", validateDoubleNestedListClassLevelActualWithMessageForViolatingList0Number2Message.getText());
		assertEquals("number1Label: invalidEntity", validateDoubleNestedListClassLevelActualWithMessageForViolatingList1Number1Message.getText());
		assertEquals("", validateDoubleNestedListClassLevelActualWithMessageForViolatingList0Number2Message.getText());
		assertEquals("", validateDoubleNestedListClassLevelActualWithMessageForViolatingFormMessage.getText());
		assertEquals("actionValidationFailed", messages.getText());

		validateDoubleNestedListClassLevelActualWithMessageForViolatingList0Number2.sendKeys("0"); // So it becomes 10
		validateDoubleNestedListClassLevelActualWithMessageForViolatingList1Number2.sendKeys("0"); // So it becomes 10
		guardAjax(validateDoubleNestedListClassLevelActualWithMessageForViolatingCommand).click();
		assertEquals("", validateDoubleNestedListClassLevelActualWithMessageForViolatingList0Number1Message.getText());
		assertEquals("", validateDoubleNestedListClassLevelActualWithMessageForViolatingList0Number2Message.getText());
		assertEquals("", validateDoubleNestedListClassLevelActualWithMessageForViolatingList1Number1Message.getText());
		assertEquals("", validateDoubleNestedListClassLevelActualWithMessageForViolatingList1Number2Message.getText());
		assertEquals("", validateDoubleNestedListClassLevelActualWithMessageForViolatingFormMessage.getText());
		assertEquals("actionSuccess", messages.getText());
	}

	@Test
	public void validateDoubleNestedListClassLevelActualWithMessagesForViolating() {
		validateDoubleNestedListClassLevelActualWithMessageForViolatingList0Number1.sendKeys("1"); // So custom property path is not set in ValidateBeanITEntityValidator.
		validateDoubleNestedListClassLevelActualWithMessageForViolatingList0Number2.sendKeys("1");
		validateDoubleNestedListClassLevelActualWithMessageForViolatingList1Number1.sendKeys("1"); // So custom property path is not set in ValidateBeanITEntityValidator.
		validateDoubleNestedListClassLevelActualWithMessageForViolatingList1Number2.sendKeys("1");
		guardAjax(validateDoubleNestedListClassLevelActualWithMessageForViolatingCommand).click();
		assertEquals("number1Label: invalidEntity", validateDoubleNestedListClassLevelActualWithMessageForViolatingList0Number1Message.getText());
		assertEquals("number2Label: invalidEntity", validateDoubleNestedListClassLevelActualWithMessageForViolatingList0Number2Message.getText());
		assertEquals("number1Label: invalidEntity", validateDoubleNestedListClassLevelActualWithMessageForViolatingList1Number1Message.getText());
		assertEquals("number2Label: invalidEntity", validateDoubleNestedListClassLevelActualWithMessageForViolatingList0Number2Message.getText());
		assertEquals("", validateDoubleNestedListClassLevelActualWithMessageForViolatingFormMessage.getText());
		assertEquals("actionValidationFailed", messages.getText());

		validateDoubleNestedListClassLevelActualWithMessageForViolatingList0Number2.sendKeys("0"); // So it becomes 10
		validateDoubleNestedListClassLevelActualWithMessageForViolatingList1Number2.sendKeys("0"); // So it becomes 10
		guardAjax(validateDoubleNestedListClassLevelActualWithMessageForViolatingCommand).click();
		assertEquals("", validateDoubleNestedListClassLevelActualWithMessageForViolatingList0Number1Message.getText());
		assertEquals("", validateDoubleNestedListClassLevelActualWithMessageForViolatingList0Number2Message.getText());
		assertEquals("", validateDoubleNestedListClassLevelActualWithMessageForViolatingList1Number1Message.getText());
		assertEquals("", validateDoubleNestedListClassLevelActualWithMessageForViolatingList1Number2Message.getText());
		assertEquals("", validateDoubleNestedListClassLevelActualWithMessageForViolatingFormMessage.getText());
		assertEquals("actionSuccess", messages.getText());
	}

	@Test
	public void validateClassLevelWithInputEntityComposite() {
		validateClassLevelWithInputEntityCompositeNumber1.sendKeys("2");
		validateClassLevelWithInputEntityCompositeNumber2.sendKeys("1");
		guardAjax(validateClassLevelWithInputEntityCompositeCommand).click();
		assertEquals("number1Label: invalidEntity", validateClassLevelWithInputEntityCompositeNumber1Message.getText());
		assertEquals("", validateClassLevelWithInputEntityCompositeNumber2Message.getText());
		assertEquals("", validateClassLevelWithInputEntityCompositeFormMessage.getText());
		assertEquals("", messages.getText());

		validateClassLevelWithInputEntityCompositeNumber2.sendKeys("0"); // So it becomes 10
		guardAjax(validateClassLevelWithInputEntityCompositeCommand).click();
		assertEquals("", validateClassLevelWithInputEntityCompositeNumber1Message.getText());
		assertEquals("", validateClassLevelWithInputEntityCompositeNumber2Message.getText());
		assertEquals("", validateClassLevelWithInputEntityCompositeFormMessage.getText());
		assertEquals("actionSuccess", messages.getText());
	}

	@Test
	public void validateClassLevelWithFormEntityComposite() {
		validateClassLevelWithFormEntityCompositeNumber1.sendKeys("2");
		validateClassLevelWithFormEntityCompositeNumber2.sendKeys("1");
		guardAjax(validateClassLevelWithFormEntityCompositeCommand).click();
		assertEquals("number1Label: invalidEntity", validateClassLevelWithFormEntityCompositeNumber1Message.getText());
		assertEquals("", validateClassLevelWithFormEntityCompositeNumber2Message.getText());
		assertEquals("", validateClassLevelWithFormEntityCompositeFormMessage.getText());
		assertEquals("", messages.getText());

		validateClassLevelWithFormEntityCompositeNumber2.sendKeys("0"); // So it becomes 10
		guardAjax(validateClassLevelWithFormEntityCompositeCommand).click();
		assertEquals("", validateClassLevelWithFormEntityCompositeNumber1Message.getText());
		assertEquals("", validateClassLevelWithFormEntityCompositeNumber2Message.getText());
		assertEquals("", validateClassLevelWithFormEntityCompositeFormMessage.getText());
		assertEquals("actionSuccess", messages.getText());
	}

    @Test
    void validateBeanWithCustomTypeAsProperty() {
        open("ValidateBeanITWithCustomTypeAsProperty.xhtml");
        validateBeanWithCustomTypeAsPropertyFlightNumber2.sendKeys("AA11");
        guardAjax(validateBeanWithCustomTypeAsPropertyCommand).click();
        assertEquals("", validateBeanWithCustomTypeAsPropertyFlightNumber1Message.getText());
        assertEquals("flightNumberLabel: Invalid flight number", validateBeanWithCustomTypeAsPropertyFlightNumber2Message.getText());
        assertEquals("", validateBeanWithCustomTypeAsPropertyMessages.getText());

        validateBeanWithCustomTypeAsPropertyFlightNumber1.clear();
        validateBeanWithCustomTypeAsPropertyFlightNumber1.sendKeys("AA11");
        guardAjax(validateBeanWithCustomTypeAsPropertyCommand).click();
        assertEquals("flightNumberLabel: Invalid flight number", validateBeanWithCustomTypeAsPropertyFlightNumber1Message.getText());
        assertEquals("flightNumberLabel: Invalid flight number", validateBeanWithCustomTypeAsPropertyFlightNumber2Message.getText());
        assertEquals("", validateBeanWithCustomTypeAsPropertyMessages.getText());

        validateBeanWithCustomTypeAsPropertyFlightNumber2.clear();
        validateBeanWithCustomTypeAsPropertyFlightNumber2.sendKeys("AA22");
        guardAjax(validateBeanWithCustomTypeAsPropertyCommand).click();
        assertEquals("flightNumberLabel: Invalid flight number", validateBeanWithCustomTypeAsPropertyFlightNumber1Message.getText());
        assertEquals("", validateBeanWithCustomTypeAsPropertyFlightNumber2Message.getText());
        assertEquals("", validateBeanWithCustomTypeAsPropertyMessages.getText());

        validateBeanWithCustomTypeAsPropertyFlightNumber1.clear();
        validateBeanWithCustomTypeAsPropertyFlightNumber1.sendKeys("AA33");
        guardAjax(validateBeanWithCustomTypeAsPropertyCommand).click();
        assertEquals("", validateBeanWithCustomTypeAsPropertyFlightNumber1Message.getText());
        assertEquals("", validateBeanWithCustomTypeAsPropertyFlightNumber2Message.getText());
        assertEquals("actionSuccess", validateBeanWithCustomTypeAsPropertyMessages.getText());
    }
}