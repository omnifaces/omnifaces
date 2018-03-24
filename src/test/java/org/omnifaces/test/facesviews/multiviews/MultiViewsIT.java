/*
 * Copyright 2018 OmniFaces
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

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return buildWebArchive(MultiViewsIT.class)
			.withWebXml(withMultiViews)
			.createDeployment();
	}

	@Test
	public void test() {
		verify200("MultiViewsIT", "", "", "");

		guardHttp(formSubmit).click();
		verify200("MultiViewsIT", "", "", "");

		open("foo/42");
		verify200("MultiViewsIT", "foo/42", "foo", "42");

		guardHttp(formSubmit).click();
		verify200("MultiViewsIT", "foo/42", "foo", "42");

		open("foo/42/bar/");
		verify200("MultiViewsIT", "foo/42/bar/", "foo", "42");

		guardHttp(formSubmit).click();
		verify200("MultiViewsIT", "foo/42/bar/", "foo", "42");

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

		open("MultiViewsITNonExistingPage");
		verify200("MultiViewsIT", "MultiViewsITNonExistingPage", "MultiViewsITNonExistingPage", "");

		open("MultiViewsITNonExistingPage/");
		verify200("MultiViewsIT", "MultiViewsITNonExistingPage/", "MultiViewsITNonExistingPage", "");

		if (!isTomee()) { // MyFaces bugs on this case with NPE in getViewMetadataFacelet() --> resolveURL()
			open("MultiViewsITNonExistingPage.xhtml");
			verify404("MultiViewsITNonExistingPage.xhtml");
		}
	}

	private void verify200(String title, String path, String firstPathParam, String secondPathParam) {
		assertEquals(title, browser.getTitle());
		assertEquals(baseURL + path, stripJsessionid(browser.getCurrentUrl()));
		assertEquals(firstPathParam, firstPathParamAsString.getText());
		assertEquals(secondPathParam, secondPathParamAsInteger.getText());
		assertEquals("/MultiViewsIT/" + path, stripJsessionid(form.getAttribute("action")));
	}

	private void verify404(String path) {
		assertEquals("404", browser.getTitle());
		assertEquals(baseURL + path, stripJsessionid(browser.getCurrentUrl()));
	}
}