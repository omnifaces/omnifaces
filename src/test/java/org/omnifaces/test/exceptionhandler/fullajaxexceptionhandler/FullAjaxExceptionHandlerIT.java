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
package org.omnifaces.test.exceptionhandler.fullajaxexceptionhandler;

import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.jboss.arquillian.graphene.Graphene.guardHttp;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.omnifaces.test.OmniFacesIT.FacesConfig.withFullAjaxExceptionHandler;
import static org.omnifaces.test.OmniFacesIT.WebXml.withErrorPage;

import java.util.ArrayList;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.omnifaces.resourcehandler.ResourceIdentifier;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.By;
import org.openqa.selenium.InvalidSelectorException;
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

	@FindBy(id="form1:throwDuringSecondUpdateOfRenderResponse")
	private WebElement throwDuringSecondUpdateOfRenderResponse;

	@FindBy(id="form1:throwDuringTreeVisitingOnRenderResponse")
	private WebElement throwDuringTreeVisitingOnRenderResponse;

	@FindBy(id="form1:throwPrimeFacesDuringInvokeApplication")
	private WebElement throwPrimeFacesDuringInvokeApplication;

	@FindBy(id="form1:throwMixedDuringInvokeApplication")
	private WebElement throwMixedDuringInvokeApplication;

	@FindBy(id="form1:throwPrimeFacesDuringUpdateModelValues")
	private WebElement throwPrimeFacesDuringUpdateModelValues;

	@FindBy(id="form1:throwPrimeFacesDuringRenderResponse")
	private WebElement throwPrimeFacesDuringRenderResponse;

	@FindBy(id="form1:throwPrimeFacesDuringSecondUpdateOfRenderResponse")
	private WebElement throwPrimeFacesDuringSecondUpdateOfRenderResponse;

	@FindBy(id="form1:throwPrimeFacesDuringTreeVisitingOnRenderResponse")
	private WebElement throwPrimeFacesDuringTreeVisitingOnRenderResponse;

	@FindBy(id="form2:throwNonAjaxDuringInvokeApplication")
	private WebElement throwNonAjaxDuringInvokeApplication;

	@FindBy(id="form1:throwNonAjaxDuringUpdateModelValues")
	private WebElement throwNonAjaxDuringUpdateModelValues;

	@FindBy(id="form3:throwNonAjaxDuringRenderResponse")
	private WebElement throwNonAjaxDuringRenderResponse;

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return buildWebArchive(FullAjaxExceptionHandlerIT.class)
			.withFacesConfig(withFullAjaxExceptionHandler)
			.withWebXml(withErrorPage)
			.withPrimeFaces()
			.createDeployment();
	}

	@Test
	public void throwDuringInvokeApplication() {
		assertAllResourcesRendered();
		guardAjax(throwDuringInvokeApplication).click();
		assertTrue(exception.getText().contains("throwDuringInvokeApplication"));
		assertAllResourcesRendered();
	}

	@Test
	public void throwDuringUpdateModelValues() {
		assertAllResourcesRendered();
		guardAjax(throwDuringUpdateModelValues).click();
		assertTrue(exception.getText().contains("throwDuringUpdateModelValues"));
		assertAllResourcesRendered();
	}

	@Test
	public void throwDuringRenderResponse() {
		assertAllResourcesRendered();
		guardAjax(throwDuringRenderResponse).click();
		assertTrue(exception.getText().contains("throwDuringRenderResponse"));
		assertAllResourcesRendered();
	}

	@Test
	public void throwDuringSecondUpdateOfRenderResponse() {
		refresh(); // TODO: fix so that this is not necessary anymore -- PrimeFaces will unnecessarily render duplicate CSS resources in error page because existing ones have JSESSIONID path param appended and new ones not.
		assertAllResourcesRendered();
		guardAjax(throwDuringSecondUpdateOfRenderResponse).click();
		assertTrue(exception.getText().contains("throwDuringRenderResponse"));
		assertAllResourcesRendered();
	}

	@Test
	public void throwDuringTreeVisitingOnRenderResponse() {
		assertAllResourcesRendered();
		guardAjax(throwDuringTreeVisitingOnRenderResponse).click();
		assertTrue(exception.getText().contains("throwDuringRenderResponse"));
		assertAllResourcesRendered();
	}

	@Test
	public void throwPrimeFacesDuringInvokeApplication() {
		assertAllResourcesRendered();
		guardAjax(throwPrimeFacesDuringInvokeApplication).click();
		assertTrue(exception.getText().contains("throwDuringInvokeApplication"));
		assertAllResourcesRendered();
	}

	@Test
	public void throwMixedDuringInvokeApplication() {
		assertAllResourcesRendered();
		guardAjax(throwMixedDuringInvokeApplication).click();
		assertTrue(exception.getText().contains("throwDuringInvokeApplication"));
		assertAllResourcesRendered();
	}

	@Test
	public void throwPrimeFacesDuringUpdateModelValues() {
		assertAllResourcesRendered();
		guardAjax(throwPrimeFacesDuringUpdateModelValues).click();
		assertTrue(exception.getText().contains("throwDuringUpdateModelValues"));
		assertAllResourcesRendered();
	}

	@Test
	public void throwPrimeFacesDuringRenderResponse() {
		assertAllResourcesRendered();
		guardAjax(throwPrimeFacesDuringRenderResponse).click();
		assertTrue(exception.getText().contains("throwDuringRenderResponse"));
		assertAllResourcesRendered();
	}

	@Test
	public void throwPrimeFacesDuringSecondUpdateOfRenderResponse() {
		assertAllResourcesRendered();
		guardAjax(throwPrimeFacesDuringSecondUpdateOfRenderResponse).click();
		assertTrue(exception.getText().contains("throwDuringRenderResponse"));
		assertAllResourcesRendered();
	}

	@Test
	public void throwPrimeFacesDuringTreeVisitingOnRenderResponse() {
		assertAllResourcesRendered();
		guardAjax(throwPrimeFacesDuringTreeVisitingOnRenderResponse).click();
		assertTrue(exception.getText().contains("throwDuringRenderResponse"));
		assertAllResourcesRendered();
	}

	@Test
	public void throwNonAjaxDuringInvokeApplication() {
		assertAllResourcesRendered();
		guardHttp(throwNonAjaxDuringInvokeApplication).click();
		assertTrue(exception.getText().contains("throwDuringInvokeApplication"));
		assertAllResourcesRendered();
	}

	@Test
	public void throwNonAjaxDuringUpdateModelValues() {
		assertAllResourcesRendered();
		guardHttp(throwNonAjaxDuringUpdateModelValues).click();
		assertTrue(exception.getText().contains("throwDuringUpdateModelValues"));
		assertAllResourcesRendered();
	}

	@Test
	public void throwNonAjaxDuringRenderResponse() {
		assertAllResourcesRendered();
		guardHttp(throwNonAjaxDuringRenderResponse).click();
		assertTrue(exception.getText().contains("throwDuringRenderResponse"));
		assertAllResourcesRendered();
	}

	private void assertAllResourcesRendered() {
		assertStylesheetResourceRendered("primefaces-saga", "theme.css");
		assertStylesheetResourceRendered("primefaces", "components.css");
		assertStylesheetResourceRendered(null, "style.css");

		assertScriptResourceRendered("primefaces", "jquery/jquery.js");
		assertScriptResourceRendered("primefaces", "jquery/jquery-plugins.js");
		assertScriptResourceRendered("primefaces", "core.js");
		assertScriptResourceRendered("primefaces", "components.js");
	}

	private void assertStylesheetResourceRendered(String library, String name) {
		List<WebElement> stylesheets = new ArrayList<>();

		try {
			stylesheets.addAll(browser.findElements(By.cssSelector("link[rel=stylesheet][href*='" + name + "']" + (library == null ? "" : ("[href*='ln=" + library + "']")))));
			stylesheets.addAll(browser.findElements(By.xpath("//style[contains(text(),'@import')][contains(text(),'" + name + "')]" + (library == null ? "" : ("[contains(text(),'ln=" + library + "')]")))));
		}
		catch (InvalidSelectorException e) {
			System.out.println(browser.getPageSource());
			fail("Unselectable stylesheet " + new ResourceIdentifier(library, name) + ": " + e);
		}

		if (stylesheets.isEmpty()) {
			fail("Missing stylesheet " + new ResourceIdentifier(library, name));
		}
		else if (stylesheets.size() > 1) {
			fail("Duplicate stylesheet " + new ResourceIdentifier(library, name));
		}
	}

	private void assertScriptResourceRendered(String library, String name) {
		List<WebElement> scripts = browser.findElements(By.cssSelector("script[src*='" + name + "']" + (library == null ? "" : ("[src*='ln=" + library + "']"))));

		if (scripts.isEmpty()) {
			fail("Missing script " + new ResourceIdentifier(library, name));
		}
		else if (scripts.size() > 1) {
			fail("Duplicate script " + new ResourceIdentifier(library, name));
		}
	}

}