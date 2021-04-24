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
package org.omnifaces.test.exceptionhandler.fullajaxexceptionhandler;

import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.jboss.arquillian.graphene.Graphene.guardHttp;
import static org.junit.Assert.assertTrue;
import static org.omnifaces.test.OmniFacesIT.FacesConfig.withFullAjaxExceptionHandler;
import static org.omnifaces.test.OmniFacesIT.WebXml.withErrorPage;

import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class FullAjaxExceptionHandlerIT extends OmniFacesIT {

	@FindBy(id="exception")
	private WebElement exception;

	@FindBy(id="form1:throwDuringInvokeApplication")
	private WebElement throwDuringInvokeApplication;

	@FindBy(id="form1:throwDuringUpdateModelValues")
	private WebElement throwDuringUpdateModelValues;

	@FindBy(id="form1:throwDuringRenderResponse")
	private WebElement throwDuringRenderResponse;

	@FindBy(id="form2:throwNonAjaxDuringInvokeApplication")
	private WebElement throwNonAjaxDuringInvokeApplication;

	@FindBy(id="form1:throwNonAjaxDuringUpdateModelValues")
	private WebElement throwNonAjaxDuringUpdateModelValues;

	@FindBy(id="form3:throwNonAjaxDuringRenderResponse")
	private WebElement throwNonAjaxDuringRenderResponse;

	@FindBy(css="link[rel=stylesheet][href*='style.css']")
	private List<WebElement> stylesheets;

	@FindBy(xpath="//style[contains(text(),'@import')][contains(text(),'style.css')]")
	private List<WebElement> styleimports;

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return buildWebArchive(FullAjaxExceptionHandlerIT.class)
			.withFacesConfig(withFullAjaxExceptionHandler)
			.withWebXml(withErrorPage)
			.createDeployment();
	}

	@Test
	public void throwDuringInvokeApplication() {
		assertTrue(stylesheets.size() + styleimports.size() == 1);
		guardAjax(throwDuringInvokeApplication).click();
		assertTrue(exception.getText().contains("throwDuringInvokeApplication"));
		assertTrue(stylesheets.size() + styleimports.size() == 1);
	}

	@Test
	public void throwDuringUpdateModelValues() {
		assertTrue(stylesheets.size() + styleimports.size() == 1);
		guardAjax(throwDuringUpdateModelValues).click();
		assertTrue(exception.getText().contains("throwDuringUpdateModelValues"));
		assertTrue(stylesheets.size() + styleimports.size() == 1);
	}

	@Test
	public void throwDuringRenderResponse() {
		assertTrue(stylesheets.size() + styleimports.size() == 1);
		guardAjax(throwDuringRenderResponse).click();
		assertTrue(exception.getText().contains("throwDuringRenderResponse"));
		assertTrue(stylesheets.size() + styleimports.size() == 1);
	}

	@Test
	public void throwNonAjaxDuringInvokeApplication() {
		assertTrue(stylesheets.size() + styleimports.size() == 1);
		guardHttp(throwNonAjaxDuringInvokeApplication).click();
		assertTrue(exception.getText().contains("throwDuringInvokeApplication"));
		assertTrue(stylesheets.size() + styleimports.size() == 1);
	}

	@Test
	public void throwNonAjaxDuringUpdateModelValues() {
		assertTrue(stylesheets.size() + styleimports.size() == 1);
		guardHttp(throwNonAjaxDuringUpdateModelValues).click();
		assertTrue(exception.getText().contains("throwDuringUpdateModelValues"));
		assertTrue(stylesheets.size() + styleimports.size() == 1);
	}

	@Test
	public void throwNonAjaxDuringRenderResponse() {
		assertTrue(stylesheets.size() + styleimports.size() == 1);
		guardHttp(throwNonAjaxDuringRenderResponse).click();
		assertTrue(exception.getText().contains("throwDuringRenderResponse"));
		assertTrue(stylesheets.size() + styleimports.size() == 1);
	}

}