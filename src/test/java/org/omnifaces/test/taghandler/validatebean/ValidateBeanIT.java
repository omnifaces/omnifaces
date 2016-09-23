/*
 * Copyright 2016 OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.test.taghandler.validatebean;

import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.jboss.arquillian.graphene.Graphene.waitGui;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.omnifaces.test.OmniFacesIT.ArchiveBuilder.buildWebArchive;
import static org.omnifaces.test.OmniFacesIT.FacesConfig.withMessageBundle;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.JavascriptExecutor;
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
		assertEquals("default", messages.getText());

		input.clear();
		input.sendKeys("xx");
		guardAjax(validateDefaultByCommand).click();
		assertEquals("actionSuccess", messages.getText());

		input.clear();
		input.sendKeys("x");
		guardAjax(validateGroupByCommand).click();
		assertEquals("group", messages.getText());

		input.clear();
		input.sendKeys("xx");
		guardAjax(validateGroupByCommand).click();
		assertEquals("actionSuccess", messages.getText());

		input.clear();
		input.sendKeys("x");
		guardAjax(validateDefaultAndGroupByCommand).click();
		String message = messages.getText();
		assertTrue(message.contains("default") && message.contains("group")); // It's unordered.

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
		triggerOnchange(validateDefaultByInput);
		assertEquals("default", messages.getText());

		validateDefaultByInput.clear();
		validateDefaultByInput.sendKeys("xx");
		triggerOnchange(validateDefaultByInput);
		assertEquals("actionSuccess", messages.getText());

		validateGroupByInput.sendKeys("x");
		triggerOnchange(validateGroupByInput);
		assertEquals("group", messages.getText());

		validateGroupByInput.clear();
		validateGroupByInput.sendKeys("xx");
		triggerOnchange(validateGroupByInput);
		assertEquals("actionSuccess", messages.getText());

		validateDefaultAndGroupByInput.sendKeys("x");
		triggerOnchange(validateDefaultAndGroupByInput);
		String message = messages.getText();
		assertTrue(message.contains("default") && message.contains("group")); // It's unordered.

		validateDefaultAndGroupByInput.clear();
		validateDefaultAndGroupByInput.sendKeys("xx");
		triggerOnchange(validateDefaultAndGroupByInput);
		assertEquals("actionSuccess", messages.getText());

		validateDisabledByInput.sendKeys("x");
		triggerOnchange(validateDisabledByInput);
		assertEquals("actionSuccess", messages.getText());

		validateDisabledByInput.clear();
		validateDisabledByInput.sendKeys("xx");
		triggerOnchange(validateDisabledByInput);
		assertEquals("actionSuccess", messages.getText());
	}

	private void triggerOnchange(WebElement element) {
		((JavascriptExecutor) browser).executeScript(""
			+ "document.getElementById('messages').innerHTML='';"
			+ "document.getElementById('" + element.getAttribute("id") + "').onchange();"
		);
		waitGui(browser).until().element(messages).text().not().equalTo("");
	}

	@Test
	public void validateClassLevelCopy() {
		validateClassLevelDefaultNumber1.sendKeys("2");
		validateClassLevelDefaultNumber2.sendKeys("1");
		guardAjax(validateClassLevelDefaultCommand).click();
		assertEquals("invalidEntity", messages.getText());

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
		assertEquals("invalidEntity", messages.getText());

		validateClassLevelByCopierNumber2.sendKeys("0");
		guardAjax(validateClassLevelByCopierCommand).click();
		assertEquals("actionSuccess", messages.getText());
	}

}