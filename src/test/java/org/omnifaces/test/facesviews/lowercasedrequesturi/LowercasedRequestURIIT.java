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
package org.omnifaces.test.facesviews.lowercasedrequesturi;

import static org.jboss.arquillian.graphene.Graphene.guardHttp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.omnifaces.test.OmniFacesIT.WebXml.withFacesViewsLowercasedRequestURI;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LowercasedRequestURIIT extends OmniFacesIT {

	@FindBy(id="linkWithExtensionOutcome")
	private WebElement linkWithExtensionOutcome;

	@FindBy(id="linkWithExtensionlessOutcome")
	private WebElement linkWithExtensionlessOutcome;

	@FindBy(id="lowercasedLinkWithExtensionOutcome")
	private WebElement lowercasedLinkWithExtensionOutcome;

	@FindBy(id="lowercasedLinkWithExtensionlessOutcome")
	private WebElement lowercasedLinkWithExtensionlessOutcome;

	@FindBy(id="form")
	private WebElement form;

	@FindBy(id="form:submit")
	private WebElement formSubmit;

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return buildWebArchive(LowercasedRequestURIIT.class)
			.withWebXml(withFacesViewsLowercasedRequestURI)
			.createDeployment();
	}

	@Test
	public void test() {
		open("lowercasedrequesturiit");
		verify200("LowercasedRequestURIIT", "lowercasedrequesturiit");

		guardHttp(formSubmit).click();
		verify200("LowercasedRequestURIIT", "lowercasedrequesturiit");

		open("lowercasedrequesturiit.xhtml");
		verify200("LowercasedRequestURIIT", "lowercasedrequesturiit");

		guardHttp(formSubmit).click();
		verify200("LowercasedRequestURIIT", "lowercasedrequesturiit");

		open("lowercasedrequesturiit/");
		verify404("lowercasedrequesturiit/");

		open("LowercasedRequestURIIT/");
		verify404("LowercasedRequestURIIT/");

		open("LowercasedRequestURIIT");
		verify404("LowercasedRequestURIIT");
	}

	@Test
	public void testSubFolder() {
		open("subfolder/lowercasedrequesturiit");
		verify200("LowercasedRequestURIITInSubFolder", "subfolder/lowercasedrequesturiit");

		guardHttp(formSubmit).click();
		verify200("LowercasedRequestURIITInSubFolder", "subfolder/lowercasedrequesturiit");

		open("subfolder/lowercasedrequesturiit.xhtml");
		verify200("LowercasedRequestURIITInSubFolder", "subfolder/lowercasedrequesturiit");

		guardHttp(formSubmit).click();
		verify200("LowercasedRequestURIITInSubFolder", "subfolder/lowercasedrequesturiit");

		guardHttp(formSubmit).click();
		verify200("LowercasedRequestURIITInSubFolder", "subfolder/lowercasedrequesturiit");

		open("subfolder/lowercasedrequesturiit/");
		verify404("subfolder/lowercasedrequesturiit/");
	}

	private void verify200(String title, String path) {
		assertEquals(title, browser.getTitle());
		assertEquals(baseURL + path, stripJsessionid(browser.getCurrentUrl()));
		assertEquals(baseURL + path, stripJsessionid(linkWithExtensionOutcome.getAttribute("href")));
		assertEquals(baseURL + path, stripJsessionid(linkWithExtensionlessOutcome.getAttribute("href")));
		assertEquals(baseURL + path, stripJsessionid(lowercasedLinkWithExtensionOutcome.getAttribute("href")));
		assertEquals(baseURL + path, stripJsessionid(lowercasedLinkWithExtensionlessOutcome.getAttribute("href")));
		assertEquals("/LowercasedRequestURIIT/" + path, stripJsessionid(form.getAttribute("action")));
	}

	private void verify404(String path) {
		assertEquals("404", browser.getTitle());
		assertEquals(baseURL + path, stripJsessionid(browser.getCurrentUrl()));
	}

}