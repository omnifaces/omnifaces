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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class ValidateAllIT extends OmniFacesIT {

	@FindBy(id="form1:input1")
	private WebElement form1Input1;

	@FindBy(id="form1:input1_m")
	private WebElement form1Input1Message;

	@FindBy(id="form1:input2")
	private WebElement form1Input2;

	@FindBy(id="form1:input2_m")
	private WebElement form1Input2Message;

	@FindBy(id="form1:input3")
	private WebElement form1Input3;

	@FindBy(id="form1:input3_m")
	private WebElement form1Input3Message;

	@FindBy(id="form1:submit")
	private WebElement form1Submit;

	@FindBy(id="form1:all_m")
	private WebElement form1AllMessage;

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

	@FindBy(id="form2:input3")
	private WebElement form2Input3;

	@FindBy(id="form2:input3_m")
	private WebElement form2Input3Message;

	@FindBy(id="form2:submit")
	private WebElement form2Submit;

	@FindBy(id="form2:all_m")
	private WebElement form2AllMessage;

	@FindBy(id="form2:ok")
	private WebElement form2Ok;


	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return buildWebArchive(ValidateAllIT.class)
			.createDeployment();
	}

	@Test
	void testForm1() {
		guardAjax(form1Submit::click);

		assertEquals("", form1Input1Message.getText());
		assertEquals("", form1Input2Message.getText());
		assertEquals("", form1Input3Message.getText());
		assertEquals("Input1, Input2, Input3: Please fill out all of those fields", form1AllMessage.getText());
		assertEquals("", form1Ok.getText());

		form1Input1.sendKeys("bruh");

		guardAjax(form1Submit::click);
		assertEquals("", form1Input1Message.getText());
		assertEquals("", form1Input2Message.getText());
		assertEquals("", form1Input3Message.getText());
		assertEquals("Input1, Input2, Input3: Please fill out all of those fields", form1AllMessage.getText());
		assertEquals("", form1Ok.getText());

		form1Input2.sendKeys("bruh");

		guardAjax(form1Submit::click);
		assertEquals("", form1Input1Message.getText());
		assertEquals("", form1Input2Message.getText());
		assertEquals("", form1Input3Message.getText());
		assertEquals("Input1, Input2, Input3: Please fill out all of those fields", form1AllMessage.getText());
		assertEquals("", form1Ok.getText());

		form1Input3.sendKeys("bruh");

		guardAjax(form1Submit::click);
		assertEquals("", form1Input1Message.getText());
		assertEquals("", form1Input2Message.getText());
		assertEquals("", form1Input3Message.getText());
		assertEquals("", form1AllMessage.getText());
		assertEquals("OK!", form1Ok.getText());
	}

	@Test
	void testForm2() {
		guardAjax(form2Submit::click);

		assertEquals("", form2Input1Message.getText());
		assertEquals("", form2Input2Message.getText());
		assertEquals("", form2Input3Message.getText());
		assertEquals("Input1, Input2, Input3: Please fill out all of those fields", form2AllMessage.getText());
		assertEquals("", form2Ok.getText());

		form2Input1.sendKeys("bruh");

		guardAjax(form2Submit::click);
		assertEquals("", form2Input1Message.getText());
		assertEquals("", form2Input2Message.getText());
		assertEquals("", form2Input3Message.getText());
		assertEquals("Input1, Input2, Input3: Please fill out all of those fields", form2AllMessage.getText());
		assertEquals("", form2Ok.getText());

		form2Input2.sendKeys("bruh");

		guardAjax(form2Submit::click);
		assertEquals("", form2Input1Message.getText());
		assertEquals("", form2Input2Message.getText());
		assertEquals("", form2Input3Message.getText());
		assertEquals("Input1, Input2, Input3: Please fill out all of those fields", form2AllMessage.getText());
		assertEquals("", form2Ok.getText());

		form2Input3.sendKeys("bruh");

		guardAjax(form2Submit::click);
		assertEquals("", form2Input1Message.getText());
		assertEquals("", form2Input2Message.getText());
		assertEquals("", form2Input3Message.getText());
		assertEquals("", form2AllMessage.getText());
		assertEquals("OK!", form2Ok.getText());
	}
}