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
package org.omnifaces.test.component.hashparam;

import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Verifies that a hash param value cannot break out of the ajax callback script <code>OmniFaces.HashParam.update('name',
 * 'value')</code> and execute, neither via a single quote closing the JavaScript string nor via a sequence closing the
 * surrounding CDATA section.
 */
public class HashParamXssIT extends OmniFacesIT {

	@FindBy(id="form:foo")
	private WebElement foo;

	@FindBy(id="form:submit")
	private WebElement submit;

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return createWebArchive(HashParamXssIT.class);
	}

	@Test
	public void testXss() {
		submitAndAssertNoXss("'-(window.__hashParamXss=1)-'");
		submitAndAssertNoXss("]]><img src=x onerror=window.__hashParamXss=1>");
	}

	private void submitAndAssertNoXss(String payload) {
		foo.clear();
		foo.sendKeys(payload);
		guardAjax(submit).click();
		assertNull(executeScript("return window.__hashParamXss"), "XSS payload did not execute: " + payload);
	}

}
