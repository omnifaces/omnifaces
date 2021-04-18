/*
 * Copyright 2021 OmniFaces
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
package org.omnifaces.test.facesviews.multiviews;

import static org.jboss.arquillian.graphene.Graphene.guardHttp;
import static org.junit.Assert.assertEquals;
import static org.omnifaces.test.OmniFacesIT.WebXml.withMultiViews;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class MultiViewsIT extends OmniFacesIT {

	@FindBy(id="firstPathParamAsString")
	private WebElement firstPathParamAsString;

	@FindBy(id="secondPathParamAsInteger")
	private WebElement secondPathParamAsInteger;

	@FindBy(id="form")
	private WebElement form;

	@FindBy(id="form:submit")
	private WebElement formSubmit;

	@FindBy(id="link")
	private WebElement link;

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return buildWebArchive(MultiViewsIT.class)
			.withWebXml(withMultiViews)
			.createDeployment();
	}

	@Test
	public void testWelcomeFileOnEmptyPath() {
		open("");
		verify200("MultiViewsITWelcomeFile", "", "", "");

		guardHttp(formSubmit).click();
		verify200("MultiViewsITWelcomeFile", "", "", "");

		guardHttp(link).click();
		verify200("MultiViewsITOtherPage", "MultiViewsITOtherPage/pathParam/471", "pathParam", "471");
	}

	@Test
	public void testWelcomeFileOnNamedPath() {
		open("MultiViewsITWelcomeFile");
		verify200("MultiViewsITWelcomeFile", "", "", ""); // So it should have autoredirected back to empty path.

		guardHttp(formSubmit).click();
		verify200("MultiViewsITWelcomeFile", "", "", "");

		guardHttp(link).click();
		verify200("MultiViewsITOtherPage", "MultiViewsITOtherPage/pathParam/471", "pathParam", "471");
	}

	@Test
	public void testWelcomeFileWithPathParameters() {
		if (isLiberty()) {
			// Unfortunately, OpenLiberty doesn't seem to deal well with path parameters in a MultiViews welcome file.
			// TODO: investigate and fix?
			return;
		}

		open("foo/42");
		verify200("MultiViewsITWelcomeFile", "foo/42", "foo", "42");

		guardHttp(formSubmit).click();
		verify200("MultiViewsITWelcomeFile", "foo/42", "foo", "42");

		open("foo/42/bar/");
		verify200("MultiViewsITWelcomeFile", "foo/42/bar/", "foo", "42");

		guardHttp(formSubmit).click();
		verify200("MultiViewsITWelcomeFile", "foo/42/bar/", "foo", "42");
	}

	@Test
	public void testOtherPage() {
		open("MultiViewsITOtherPage");
		verify200("MultiViewsITOtherPage", "MultiViewsITOtherPage", "", "");

		guardHttp(formSubmit).click();
		verify200("MultiViewsITOtherPage", "MultiViewsITOtherPage", "", "");

		open("MultiViewsITOtherPage.xhtml");
		verify200("MultiViewsITOtherPage", "MultiViewsITOtherPage", "", "");

		guardHttp(formSubmit).click();
		verify200("MultiViewsITOtherPage", "MultiViewsITOtherPage", "", "");

		open("MultiViewsITOtherPage/foo/42");
		verify200("MultiViewsITOtherPage", "MultiViewsITOtherPage/foo/42", "foo", "42");

		guardHttp(formSubmit).click();
		verify200("MultiViewsITOtherPage", "MultiViewsITOtherPage/foo/42", "foo", "42");

		open("MultiViewsITOtherPage/foo/42/bar/");
		verify200("MultiViewsITOtherPage", "MultiViewsITOtherPage/foo/42/bar/", "foo", "42");

		guardHttp(formSubmit).click();
		verify200("MultiViewsITOtherPage", "MultiViewsITOtherPage/foo/42/bar/", "foo", "42");

		guardHttp(link).click();
		verify200("MultiViewsITOtherPage", "MultiViewsITOtherPage/pathParam/471", "pathParam", "471");
	}

	@Test
	public void testNonExistingPage() {
		if (isLiberty()) {
			// Unfortunately, OpenLiberty doesn't seem to deal well with path parameters in a MultiViews welcome file.
			// TODO: investigate and fix?
			return;
		}

		open("MultiViewsITNonExistingPage");
		verify200("MultiViewsITWelcomeFile", "MultiViewsITNonExistingPage", "MultiViewsITNonExistingPage", ""); // So it should just have been reinterpreted as welcome file with path parameters.

		open("MultiViewsITNonExistingPage/");
		verify200("MultiViewsITWelcomeFile", "MultiViewsITNonExistingPage/", "MultiViewsITNonExistingPage", "");

		if (!isMyFacesUsed()) { // MyFaces throws NPE on this and thus incorrectly returns 500 instead of 404.
			open("MultiViewsITNonExistingPage.xhtml");
			verify404("MultiViewsITNonExistingPage.xhtml");
		}
	}

	@Test
	public void testExcludedFolder() {
		open("excludedfolder/MultiViewsITOtherPageInExcludedFolder.xhtml");
		verify200("MultiViewsITOtherPageInExcludedFolder", "excludedfolder/MultiViewsITOtherPageInExcludedFolder.xhtml", "", "");

		if (isLiberty()) {
			// Unfortunately, OpenLiberty doesn't seem to deal well with returning 404 to multiviews excluded folder. It returns 200 all time.
			// TODO: investigate and fix?
			return;
		}

		open("excludedfolder/MultiViewsITOtherPageInExcludedFolder");
		verify404("excludedfolder/MultiViewsITOtherPageInExcludedFolder");

		open("excludedfolder/MultiViewsITOtherPageInExcludedFolder/");
		verify404("excludedfolder/MultiViewsITOtherPageInExcludedFolder/");

		open("excludedfolder/MultiViewsITOtherPageInExcludedFolder/foo/42");
		verify404("excludedfolder/MultiViewsITOtherPageInExcludedFolder/foo/42");

		open("excludedfolder/MultiViewsITOtherPageInExcludedFolder.xhtml/foo/42");
		verify404("excludedfolder/MultiViewsITOtherPageInExcludedFolder.xhtml/foo/42");

		open("excludedfolder/foo/42");
		verify404("excludedfolder/foo/42");

		open("excludedfolder/");
		verify404("excludedfolder/");
	}

	private void verify200(String title, String path, String firstPathParam, String secondPathParam) {
		assertEquals(title, browser.getTitle());
		assertEquals(baseURL + path, stripJsessionid(browser.getCurrentUrl()));
		assertEquals(firstPathParam, firstPathParamAsString.getText());
		assertEquals(secondPathParam, secondPathParamAsInteger.getText());
		assertEquals("/MultiViewsIT/" + path, stripJsessionid(form.getAttribute("action")));
		assertEquals(baseURL + "MultiViewsITOtherPage/pathParam/471", stripJsessionid(link.getAttribute("href")));
	}

	private void verify404(String path) {
		assertEquals("404", browser.getTitle());
		assertEquals(baseURL + path, stripJsessionid(browser.getCurrentUrl()));
	}
}