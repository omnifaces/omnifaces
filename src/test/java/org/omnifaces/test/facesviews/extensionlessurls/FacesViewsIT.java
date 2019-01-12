/*
 * Copyright 2019 OmniFaces
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
package org.omnifaces.test.facesviews.extensionlessurls;

import static org.jboss.arquillian.graphene.Graphene.guardHttp;
import static org.junit.Assert.assertEquals;
import static org.omnifaces.test.OmniFacesIT.WebXml.withFacesViews;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
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
	public void testWelcomeFile() {
		verify200("FacesViewsIT", "");

		guardHttp(formSubmit).click();
		verify200("FacesViewsIT", "");

		open("FacesViewsIT.xhtml");
		verify200("FacesViewsIT", "");

		guardHttp(formSubmit).click();
		verify200("FacesViewsIT", "");

		open("FacesViewsIT/");
		verify404("FacesViewsIT/");
	}

	@Test
	public void testOtherPage() {
		open("FacesViewsITOtherPage");
		verify200("FacesViewsITOtherPage", "FacesViewsITOtherPage");

		guardHttp(formSubmit).click();
		verify200("FacesViewsITOtherPage", "FacesViewsITOtherPage");

		open("FacesViewsITOtherPage.xhtml");
		verify200("FacesViewsITOtherPage", "FacesViewsITOtherPage");

		guardHttp(formSubmit).click();
		verify200("FacesViewsITOtherPage", "FacesViewsITOtherPage");

		open("FacesViewsITOtherPage/");
		verify404("FacesViewsITOtherPage/");
	}

	@Test
	public void testFolderWithPeriod() {
		open("folder.with.period/FacesViewsITOtherPageInFolderWithPeriod");
		verify200("FacesViewsITOtherPageInFolderWithPeriod", "folder.with.period/FacesViewsITOtherPageInFolderWithPeriod");

		guardHttp(formSubmit).click();
		verify200("FacesViewsITOtherPageInFolderWithPeriod", "folder.with.period/FacesViewsITOtherPageInFolderWithPeriod");

		open("folder.with.period/FacesViewsITOtherPageInFolderWithPeriod.xhtml");
		verify200("FacesViewsITOtherPageInFolderWithPeriod", "folder.with.period/FacesViewsITOtherPageInFolderWithPeriod");

		guardHttp(formSubmit).click();
		verify200("FacesViewsITOtherPageInFolderWithPeriod", "folder.with.period/FacesViewsITOtherPageInFolderWithPeriod");

		open("folder.with.period/FacesViewsITOtherPageInFolderWithPeriod/");
		verify404("folder.with.period/FacesViewsITOtherPageInFolderWithPeriod/");
	}

	@Test
	public void testNonExistingPage() {
		open("FacesViewsITNonExistingPage");
		verify404("FacesViewsITNonExistingPage");

		open("FacesViewsITNonExistingPage");
		verify404("FacesViewsITNonExistingPage");

		open("FacesViewsITNonExistingPage.xhtml");
		verify404("FacesViewsITNonExistingPage.xhtml");
	}

	private void verify200(String title, String path) {
		assertEquals(title, browser.getTitle());
		assertEquals(baseURL + path, stripJsessionid(browser.getCurrentUrl()));
		assertEquals(baseURL + path, stripJsessionid(linkWithExtensionOutcome.getAttribute("href")));
		assertEquals(baseURL + path, stripJsessionid(linkWithExtensionlessOutcome.getAttribute("href")));
		assertEquals("/FacesViewsIT/" + path, stripJsessionid(form.getAttribute("action")));
	}

	private void verify404(String path) {
		assertEquals("404", browser.getTitle());
		assertEquals(baseURL + path, stripJsessionid(browser.getCurrentUrl()));
	}

}