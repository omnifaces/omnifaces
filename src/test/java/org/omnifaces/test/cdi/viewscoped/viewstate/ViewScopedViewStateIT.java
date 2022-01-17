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
package org.omnifaces.test.cdi.viewscoped.viewstate;

import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.jboss.arquillian.graphene.Graphene.guardHttp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.omnifaces.test.OmniFacesIT.WebXml.withClientStateSaving;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

@TestMethodOrder(OrderAnnotation.class)
public class ViewScopedViewStateIT extends OmniFacesIT {

	@FindBy(id="bean")
	private WebElement bean;

	@FindBy(id="messages")
	private WebElement messages;

	@FindBy(id="unload")
	private WebElement unload;

	@FindBy(id="newtab")
	private WebElement newtab;

	@FindBy(id="non-ajax:submit")
	private WebElement nonAjaxSubmit;

	@FindBy(id="non-ajax:navigate")
	private WebElement nonAjaxNavigate;

	@FindBy(id="ajax:submit")
	private WebElement ajaxSubmit;

	@FindBy(id="ajax:navigate")
	private WebElement ajaxNavigate;

	@FindBy(css="#non-ajax > [name='javax.faces.ViewState']")
	private WebElement nonAjaxViewState;

	@FindBy(css="#ajax > [name='javax.faces.ViewState']")
	private WebElement ajaxViewState;

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return buildWebArchive(ViewScopedViewStateIT.class)
			.withWebXml(withClientStateSaving)
			.createDeployment();
	}

	@Test @Order(1)
	public void nonAjax() {
		assertEquals("init", messages.getText());
		String previousBean = bean.getText();


		// Unload.
		guardHttp(unload).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("init", messages.getText());


		// Submit then unload.
		guardHttp(nonAjaxSubmit).click();
		assertEquals(previousBean, previousBean = bean.getText());
		assertEquals("submit", messages.getText());

		guardHttp(unload).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("init", messages.getText());


		// Navigate then unload.
		guardHttp(nonAjaxNavigate).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("navigate init", messages.getText());

		guardHttp(unload).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("init", messages.getText());


		// Submit then navigate then unload.
		guardHttp(nonAjaxSubmit).click();
		assertEquals(previousBean, previousBean = bean.getText());
		assertEquals("submit", messages.getText());

		guardHttp(nonAjaxNavigate).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("navigate init", messages.getText());

		guardHttp(unload).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("init", messages.getText());


		// Navigate then submit then unload.
		guardHttp(nonAjaxNavigate).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("navigate init", messages.getText());

		guardHttp(nonAjaxSubmit).click();
		assertEquals(previousBean, previousBean = bean.getText());
		assertEquals("submit", messages.getText());

		guardHttp(unload).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("init", messages.getText());
	}

	@Test @Order(2)
	public void ajax() {
		assertEquals("init", messages.getText());
		String previousBean = bean.getText();


		// Submit then unload.
		guardAjax(ajaxSubmit).click();
		assertEquals(previousBean, previousBean = bean.getText());
		assertEquals("submit", messages.getText());

		guardHttp(unload).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("init", messages.getText());


		// Navigate then unload.
		guardAjax(ajaxNavigate).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("navigate init", messages.getText());

		guardHttp(unload).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("init", messages.getText());


		// Submit then navigate then unload.
		guardAjax(ajaxSubmit).click();
		assertEquals(previousBean, previousBean = bean.getText());
		assertEquals("submit", messages.getText());

		guardAjax(ajaxNavigate).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("navigate init", messages.getText());

		guardHttp(unload).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("init", messages.getText());


		// Navigate then submit then unload.
		guardAjax(ajaxNavigate).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("navigate init", messages.getText());

		guardAjax(ajaxSubmit).click();
		assertEquals(previousBean, previousBean = bean.getText());
		assertEquals("submit", messages.getText());

		guardHttp(unload).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("init", messages.getText());
	}

	@Test @Order(3)
	public void copyViewState() {
		assertEquals("init", messages.getText());
		String firstBean = bean.getText();
		String firstViewState = ajaxViewState.getAttribute("value");
		String firstTab = browser.getWindowHandle();

		// Open new tab, copy view state from first tab into second tab and re-execute via ajax.
		openNewTab(newtab);
		String secondBean = bean.getText();
		String secondViewState = nonAjaxViewState.getAttribute("value");
		assertEquals("init", messages.getText());
		assertNotEquals(secondBean, firstBean);
		assertNotEquals(secondViewState, firstViewState);

		executeScript("document.querySelectorAll(\"#ajax > [name='javax.faces.ViewState']\")[0].value='" + firstViewState + "'");
		guardAjax(ajaxSubmit).click();
		assertEquals(firstBean, bean.getText());
		assertEquals("submit", messages.getText());


		// Close second tab, copy view state from second tab into first tab and re-execute via non-ajax.
		closeCurrentTabAndSwitchTo(firstTab);
		executeScript("document.querySelectorAll(\"#non-ajax > [name='javax.faces.ViewState']\")[0].value='" + secondViewState + "'");
		guardHttp(nonAjaxSubmit).click();
		assertEquals(secondBean, bean.getText());
		assertEquals("submit", messages.getText());
	}

}