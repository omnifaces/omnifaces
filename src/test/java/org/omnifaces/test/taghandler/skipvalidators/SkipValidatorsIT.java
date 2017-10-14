/*
 * Copyright 2017 OmniFaces
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
package org.omnifaces.test.taghandler.skipvalidators;

import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.junit.Assert.assertEquals;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class SkipValidatorsIT extends OmniFacesIT {

	@FindBy(id="form:toggleRequired")
	private WebElement toggleRequired;

	@FindBy(id="form:input")
	private WebElement input;

	@FindBy(id="form:message")
	private WebElement message;

	@FindBy(id="form:submit")
	private WebElement submit;

	@FindBy(id="form:validationFailed")
	private WebElement validationFailed;

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return buildWebArchive(SkipValidatorsIT.class)
			.createDeployment();
	}

	@Test
	public void test() {
		guardAjax(submit).click();
		assertEquals("required!", message.getText());
		assertEquals("true", validationFailed.getText());

		guardAjax(toggleRequired).click();
		assertEquals("", message.getText());
		assertEquals("false", validationFailed.getText());
		guardAjax(submit).click();
		assertEquals("", message.getText());
		assertEquals("false", validationFailed.getText());

		guardAjax(toggleRequired).click();
		assertEquals("", message.getText());
		assertEquals("false", validationFailed.getText());
		guardAjax(submit).click();
		assertEquals("required!", message.getText());
		assertEquals("true", validationFailed.getText());

		guardAjax(toggleRequired).click();
		assertEquals("", message.getText());
		assertEquals("false", validationFailed.getText());
		guardAjax(submit).click();
		assertEquals("", message.getText());
		assertEquals("false", validationFailed.getText());

		guardAjax(toggleRequired).click();
		assertEquals("", message.getText());
		assertEquals("false", validationFailed.getText());
		guardAjax(submit).click();
		assertEquals("required!", message.getText());
		assertEquals("true", validationFailed.getText());
	}

}