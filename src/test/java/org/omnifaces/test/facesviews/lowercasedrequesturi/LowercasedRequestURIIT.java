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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.omnifaces.test.OmniFacesIT.WebXml.withFacesViewsLowercasedRequestURI;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

@DisabledIfSystemProperty(named = "profile.id", matches = "piranha-.*", disabledReason = "piranha doesn't correctly interpret error-page in web.xml and instead uses own one")
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
	void test() {
		open("lowercasedrequesturiit");
		verify200("LowercasedRequestURIIT", "lowercasedrequesturiit");

		guardHttp(formSubmit::click);
		verify200("LowercasedRequestURIIT", "lowercasedrequesturiit");

		guardHttp(formSubmit::click);
		verify200("LowercasedRequestURIIT", "lowercasedrequesturiit");

		open("lowercasedrequesturiit.xhtml");
		verify200("LowercasedRequestURIIT", "lowercasedrequesturiit");

		guardHttp(formSubmit::click);
		verify200("LowercasedRequestURIIT", "lowercasedrequesturiit");

		guardHttp(formSubmit::click);
		verify200("LowercasedRequestURIIT", "lowercasedrequesturiit");

		open("lowercasedrequesturiit/");
		verify404("lowercasedrequesturiit/");

		open("LowercasedRequestURIIT");
		verify404("LowercasedRequestURIIT");

		open("LowercasedRequestURIIT/");
		verify404("LowercasedRequestURIIT/");

		open("LowercasedRequestURIIT.xhtml");
		verify404("LowercasedRequestURIIT.xhtml");
	}

	@Test
	void testSubFolder() {
		open("subfolder/lowercasedrequesturiit");
		verify200("LowercasedRequestURIITInSubFolder", "subfolder/lowercasedrequesturiit");

		guardHttp(formSubmit::click);
		verify200("LowercasedRequestURIITInSubFolder", "subfolder/lowercasedrequesturiit");

		guardHttp(formSubmit::click);
		verify200("LowercasedRequestURIITInSubFolder", "subfolder/lowercasedrequesturiit");

		open("subfolder/lowercasedrequesturiit.xhtml");
		verify200("LowercasedRequestURIITInSubFolder", "subfolder/lowercasedrequesturiit");

		guardHttp(formSubmit::click);
		verify200("LowercasedRequestURIITInSubFolder", "subfolder/lowercasedrequesturiit");

		guardHttp(formSubmit::click);
		verify200("LowercasedRequestURIITInSubFolder", "subfolder/lowercasedrequesturiit");

		open("subfolder/lowercasedrequesturiit/");
		verify404("subfolder/lowercasedrequesturiit/");

		open("subfolder/LowercasedRequestURIIT");
		verify404("subfolder/LowercasedRequestURIIT");

		open("subfolder/LowercasedRequestURIIT/");
		verify404("subfolder/LowercasedRequestURIIT/");

		open("subfolder/LowercasedRequestURIIT.xhtml");
		verify404("subfolder/LowercasedRequestURIIT.xhtml");
	}

	@Test
	void testAlreadyLowercasedViewId() {
		open("alreadylowercasedviewid");
		verify200("AlreadyLowercasedViewId", "alreadylowercasedviewid");

		guardHttp(formSubmit::click);
		verify200("AlreadyLowercasedViewId", "alreadylowercasedviewid");

		guardHttp(formSubmit::click);
		verify200("AlreadyLowercasedViewId", "alreadylowercasedviewid");

		open("alreadylowercasedviewid.xhtml");
		verify200("AlreadyLowercasedViewId", "alreadylowercasedviewid");

		guardHttp(formSubmit::click);
		verify200("AlreadyLowercasedViewId", "alreadylowercasedviewid");

		guardHttp(formSubmit::click);
		verify200("AlreadyLowercasedViewId", "alreadylowercasedviewid");

		open("alreadylowercasedviewid/");
		verify404("alreadylowercasedviewid/");
	}

	private void verify200(String title, String path) {
		assertEquals(title, browser.getTitle());
		assertEquals("/LowercasedRequestURIIT/" + path, stripHostAndJsessionid(browser.getCurrentUrl()));
		assertEquals("/LowercasedRequestURIIT/" + path, stripHostAndJsessionid(linkWithExtensionOutcome.getAttribute("href")));
		assertEquals("/LowercasedRequestURIIT/" + path, stripHostAndJsessionid(linkWithExtensionlessOutcome.getAttribute("href")));
		assertEquals("/LowercasedRequestURIIT/" + path, stripHostAndJsessionid(lowercasedLinkWithExtensionOutcome.getAttribute("href")));
		assertEquals("/LowercasedRequestURIIT/" + path, stripHostAndJsessionid(lowercasedLinkWithExtensionlessOutcome.getAttribute("href")));
		assertEquals("/LowercasedRequestURIIT/" + path, stripHostAndJsessionid(form.getAttribute("action")));
	}

	private void verify404(String path) {
		assertEquals("404", browser.getTitle());
		assertEquals("/LowercasedRequestURIIT/" + path, stripHostAndJsessionid(browser.getCurrentUrl()));
	}

}