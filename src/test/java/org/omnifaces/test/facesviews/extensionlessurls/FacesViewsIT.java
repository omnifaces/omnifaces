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
package org.omnifaces.test.facesviews.extensionlessurls;

import static org.jboss.arquillian.graphene.Graphene.guardHttp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.omnifaces.test.OmniFacesIT.WebXml.withFacesViews;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class FacesViewsIT extends OmniFacesIT {

	@FindBy(id="linkWithExtensionOutcome")
	private WebElement linkWithExtensionOutcome;

	@FindBy(id="linkWithExtensionlessOutcome")
	private WebElement linkWithExtensionlessOutcome;

	@FindBy(id="form")
	private WebElement form;

	@FindBy(id="form:submit")
	private WebElement formSubmit;

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return buildWebArchive(FacesViewsIT.class)
			.withWebXml(withFacesViews)
			.createDeployment();
	}

	@Test
	void testWelcomeFile() {
		verify200("FacesViewsIT", "");

		guardHttp(formSubmit).click();
		verify200("FacesViewsIT", "");

		open("FacesViewsIT.xhtml");
		verify200("FacesViewsIT", "");

		open("FacesViewsIT.jsf");
		verify200("FacesViewsIT", "");

		guardHttp(formSubmit).click();
		verify200("FacesViewsIT", "");

		open("FacesViewsIT/");
		verify404("FacesViewsIT/");
	}

	@Test
	void testOtherPage() {
		open("FacesViewsITOtherPage");
		verify200("FacesViewsITOtherPage", "FacesViewsITOtherPage");

		guardHttp(formSubmit).click();
		verify200("FacesViewsITOtherPage", "FacesViewsITOtherPage");

		open("FacesViewsITOtherPage.xhtml");
		verify200("FacesViewsITOtherPage", "FacesViewsITOtherPage");

		guardHttp(formSubmit).click();
		verify200("FacesViewsITOtherPage", "FacesViewsITOtherPage");

		open("FacesViewsITOtherPage.jsf");
		verify200("FacesViewsITOtherPage", "FacesViewsITOtherPage");

		guardHttp(formSubmit).click();
		verify200("FacesViewsITOtherPage", "FacesViewsITOtherPage");

		open("FacesViewsITOtherPage/");
		verify404("FacesViewsITOtherPage/");
	}

	@Test
	void testFolderWithPeriod() {
		open("folder.with.period/FacesViewsITOtherPageInFolderWithPeriod");
		verify200("FacesViewsITOtherPageInFolderWithPeriod", "folder.with.period/FacesViewsITOtherPageInFolderWithPeriod");

		guardHttp(formSubmit).click();
		verify200("FacesViewsITOtherPageInFolderWithPeriod", "folder.with.period/FacesViewsITOtherPageInFolderWithPeriod");

		open("folder.with.period/FacesViewsITOtherPageInFolderWithPeriod.xhtml");
		verify200("FacesViewsITOtherPageInFolderWithPeriod", "folder.with.period/FacesViewsITOtherPageInFolderWithPeriod");

		guardHttp(formSubmit).click();
		verify200("FacesViewsITOtherPageInFolderWithPeriod", "folder.with.period/FacesViewsITOtherPageInFolderWithPeriod");

		open("folder.with.period/FacesViewsITOtherPageInFolderWithPeriod.jsf");
		verify200("FacesViewsITOtherPageInFolderWithPeriod", "folder.with.period/FacesViewsITOtherPageInFolderWithPeriod");

		guardHttp(formSubmit).click();
		verify200("FacesViewsITOtherPageInFolderWithPeriod", "folder.with.period/FacesViewsITOtherPageInFolderWithPeriod");

		open("folder.with.period/FacesViewsITOtherPageInFolderWithPeriod/");
		verify404("folder.with.period/FacesViewsITOtherPageInFolderWithPeriod/");
	}

	@Test
	void testNonExistingPage() {
		open("FacesViewsITNonExistingPage");
		verify404("FacesViewsITNonExistingPage");

		open("FacesViewsITNonExistingPage.xhtml");
		verify404("FacesViewsITNonExistingPage.xhtml");

		if (!isFaces4Used()) { // Since Mojarra 4 this one incorrectly throws 500 caused by ViewHandlingStrategyNotFoundException
			open("FacesViewsITNonExistingPage.jsf");
			verify404("FacesViewsITNonExistingPage.jsf");
		}
	}

	@Test
	void testExcludedFolder() {
		open("excludedfolder/FacesViewsITOtherPageInExcludedFolder.xhtml");
		verify200("FacesViewsITOtherPageInExcludedFolder", "excludedfolder/FacesViewsITOtherPageInExcludedFolder.xhtml");

		open("excludedfolder/FacesViewsITOtherPageInExcludedFolder.jsf");
		verify200("FacesViewsITOtherPageInExcludedFolder", "excludedfolder/FacesViewsITOtherPageInExcludedFolder.jsf");

		open("excludedfolder/FacesViewsITOtherPageInExcludedFolder");
		verify404("excludedfolder/FacesViewsITOtherPageInExcludedFolder");

		open("excludedfolder/FacesViewsITOtherPageInExcludedFolder/");
		verify404("excludedfolder/FacesViewsITOtherPageInExcludedFolder/");
	}

	private void verify200(String title, String path) {
		assertEquals(title, browser.getTitle());
		assertEquals("/FacesViewsIT/" + path, stripHostAndJsessionid(browser.getCurrentUrl()));
		assertEquals("/FacesViewsIT/" + path, stripHostAndJsessionid(linkWithExtensionOutcome.getAttribute("href")));
		assertEquals("/FacesViewsIT/" + path, stripHostAndJsessionid(linkWithExtensionlessOutcome.getAttribute("href")));
		assertEquals("/FacesViewsIT/" + path, stripHostAndJsessionid(form.getAttribute("action")));
	}

	private void verify404(String path) {
		assertEquals("404", browser.getTitle());
		assertEquals("/FacesViewsIT/" + path, stripHostAndJsessionid(browser.getCurrentUrl()));
	}

}