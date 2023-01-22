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
package org.omnifaces.test.cdi.viewscoped;

import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.jboss.arquillian.graphene.Graphene.guardHttp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.omnifaces.test.OmniFacesIT.WebXml.withThreeViewsInSession;

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
public class ViewScopedIT extends OmniFacesIT {

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

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return buildWebArchive(ViewScopedIT.class)
			.withWebXml(withThreeViewsInSession)
			.createDeployment();
	}

	@Test @Order(1)
	public void nonAjax() {
		assertEquals("init", getMessagesText());
		String previousBean = bean.getText();

		// Unload.
		guardHttp(unload).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("unload init", getMessagesText());


		// Submit then unload.
		guardHttp(nonAjaxSubmit).click();
		assertEquals(previousBean, previousBean = bean.getText());
		assertEquals("submit", getMessagesText());

		guardHttp(unload).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("unload init", getMessagesText());


		// Navigate then unload.
		guardHttp(nonAjaxNavigate).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("navigate destroy init", getMessagesText());

		guardHttp(unload).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("unload init", getMessagesText());


		// Submit then navigate then unload.
		guardHttp(nonAjaxSubmit).click();
		assertEquals(previousBean, previousBean = bean.getText());
		assertEquals("submit", getMessagesText());

		guardHttp(nonAjaxNavigate).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("navigate destroy init", getMessagesText());

		guardHttp(unload).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("unload init", getMessagesText());


		// Navigate then submit then unload.
		guardHttp(nonAjaxNavigate).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("navigate destroy init", getMessagesText());

		guardHttp(nonAjaxSubmit).click();
		assertEquals(previousBean, previousBean = bean.getText());
		assertEquals("submit", getMessagesText());

		guardHttp(unload).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("unload init", getMessagesText());
	}

	@Test @Order(2)
	public void ajax() {

		// Unloaded bean is from previous test.
		assertEquals("unload init", getMessagesText());
		String previousBean = bean.getText();


		// Submit then unload.
		guardAjax(ajaxSubmit).click();
		assertEquals(previousBean, previousBean = bean.getText());
		assertEquals("submit", getMessagesText());

		guardHttp(unload).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("unload init", getMessagesText());


		// Navigate then unload.
		guardAjax(ajaxNavigate).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("navigate destroy init", getMessagesText());

		guardHttp(unload).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("unload init", getMessagesText());


		// Submit then navigate then unload.
		guardAjax(ajaxSubmit).click();
		assertEquals(previousBean, previousBean = bean.getText());
		assertEquals("submit", getMessagesText());

		guardAjax(ajaxNavigate).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("navigate destroy init", getMessagesText());

		guardHttp(unload).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("unload init", getMessagesText());


		// Navigate then submit then unload.
		guardAjax(ajaxNavigate).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("navigate destroy init", getMessagesText());

		guardAjax(ajaxSubmit).click();
		assertEquals(previousBean, previousBean = bean.getText());
		assertEquals("submit", getMessagesText());

		guardHttp(unload).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("unload init", getMessagesText());
	}

	@Test @Order(3)
	public void destroyViewState() {

		// Unloaded bean is from previous test.
		assertEquals("unload init", getMessagesText());
		String firstBean = bean.getText();
		String firstTab = browser.getWindowHandle();

		// Open three new tabs and close them immediately.
		openNewTab(newtab);
		assertEquals("init", getMessagesText());
		assertNotEquals(firstBean, bean.getText());
		closeCurrentTabAndSwitchTo(firstTab);

		openNewTab(newtab);
		assertEquals("unload init", getMessagesText()); // Unload was from previous tab.
		assertNotEquals(firstBean, bean.getText());
		closeCurrentTabAndSwitchTo(firstTab);

		openNewTab(newtab);
		assertEquals("unload init", getMessagesText()); // Unload was from previous tab.
		assertNotEquals(firstBean, bean.getText());
		closeCurrentTabAndSwitchTo(firstTab);

		// Submit form in first tab. As JSF is instructed to store only 3 views in session,
		// and the @ViewScoped unload in three previously opened tabs should also physically
		// destroy the view state, the submit in first tab should not throw ViewExpiredException.
		guardAjax(ajaxSubmit).click();
		assertEquals(firstBean, bean.getText());
		assertEquals("unload submit", getMessagesText()); // Unload was from previous tab.
	}

	private String getMessagesText() {
		return messages.getText().replaceAll("\\s+", " ");
	}

}