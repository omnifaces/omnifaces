/*
 * Copyright 2020 OmniFaces
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.omnifaces.test.OmniFacesIT.FacesConfig.withMessageBundle;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
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

		validateClassLevelDefaultNumber2.sendKeys("0");
		guardAjax(validateClassLevelDefaultCommand).click();
		assertEquals("actionSuccess", messages.getText());
	}

	@Test
	public void validateClassLevelActual() {
		validateClassLevelActualNumber1.sendKeys("2");
		validateClassLevelActualNumber2.sendKeys("1");
		guardAjax(validateClassLevelActualCommand).click();
		assertEquals("invalidEntity actionValidationFailed", messages.getText());

		validateClassLevelActualNumber2.sendKeys("0");
		guardAjax(validateClassLevelActualCommand).click();
		assertEquals("actionSuccess", messages.getText());
	}

	@Test
	public void validateClassLevelByCopier() {
		validateClassLevelByCopierNumber1.sendKeys("2");
		validateClassLevelByCopierNumber2.sendKeys("1");
		guardAjax(validateClassLevelByCopierCommand).click();
		assertEquals("number1Label, number2Label: invalidEntity", messages.getText());

		validateClassLevelByCopierNumber2.sendKeys("0");
		guardAjax(validateClassLevelByCopierCommand).click();
		assertEquals("actionSuccess", messages.getText());
	}

	@Test
	public void validateClassLevelWithMessageForForm() {
		validateClassLevelWithMessageForFormNumber1.sendKeys("2");
		validateClassLevelWithMessageForFormNumber2.sendKeys("1");
		guardAjax(validateClassLevelWithMessageForFormCommand).click();
		assertEquals("number1Label, number2Label: invalidEntity", validateClassLevelWithMessageForFormMessage.getText());

		validateClassLevelWithMessageForFormNumber2.sendKeys("0");
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

		validateClassLevelWithMessageForAllNumber2.sendKeys("0");
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

		validateClassLevelWithMessageForGlobalNumber2.sendKeys("0");
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

		validateClassLevelWithMessageForViolatingNumber2.sendKeys("0");
		guardAjax(validateClassLevelWithMessageForViolatingCommand).click();
		assertEquals("", validateClassLevelWithMessageForViolatingNumber1Message.getText());
		assertEquals("", validateClassLevelWithMessageForViolatingNumber2Message.getText());
		assertEquals("", validateClassLevelWithMessageForViolatingFormMessage.getText());
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

		validateClassLevelWithInputEntityCompositeNumber2.sendKeys("0");
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

		validateClassLevelWithFormEntityCompositeNumber2.sendKeys("0");
		guardAjax(validateClassLevelWithFormEntityCompositeCommand).click();
		assertEquals("", validateClassLevelWithFormEntityCompositeNumber1Message.getText());
		assertEquals("", validateClassLevelWithFormEntityCompositeNumber2Message.getText());
		assertEquals("", validateClassLevelWithFormEntityCompositeFormMessage.getText());
		assertEquals("actionSuccess", messages.getText());
	}

}