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
package org.omnifaces.test.exceptionhandler.viewexpiredexceptionhandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.omnifaces.test.OmniFacesIT.FacesConfig.withViewExpiredExceptionHandler;
import static org.omnifaces.test.OmniFacesIT.WebXml.distributable;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class ViewExpiredExceptionHandlerIT extends OmniFacesIT {

	@FindBy(id="form:wasViewExpired")
	private WebElement wasViewExpired;

	@FindBy(id="form:submit")
	private WebElement submit;

	@FindBy(id="expire")
	private WebElement expire;

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return buildWebArchive(ViewExpiredExceptionHandlerIT.class)
			.withWebXml(distributable) // Should trigger Mojarra issue 4431 (NPE while obtaining flash scope in new session).
			.withFacesConfig(withViewExpiredExceptionHandler)
			.createDeployment();
	}

	@Test
	void test() {
		assertEquals("false", wasViewExpired.getText());

		guardAjax(submit::click);
		assertEquals("false", wasViewExpired.getText());

		clearMessages(wasViewExpired);
		expire.click();
		waitUntilTextContent(wasViewExpired);
		assertEquals("expired", wasViewExpired.getText());

		guardAjax(submit::click);
		assertEquals("true", wasViewExpired.getText());

		guardAjax(submit::click);
		assertEquals("false", wasViewExpired.getText());
	}
}