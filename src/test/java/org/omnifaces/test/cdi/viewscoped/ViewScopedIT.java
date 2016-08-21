/*
 * Copyright 2016 OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnifaces.test.cdi.viewscoped;

import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.jboss.arquillian.graphene.Graphene.guardHttp;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.omnifaces.test.OmniFacesIT.ArchiveBuilder.createWebArchive;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

@RunWith(Arquillian.class)
public class ViewScopedIT extends OmniFacesIT {

	@FindBy(id="bean")
	private WebElement bean;

	@FindBy(id="messages")
	private WebElement messages;

	@FindBy(id="unload")
	private WebElement unload;

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
		return createWebArchive(ViewScopedIT.class);
	}

	@Test @InSequence(1)
	public void nonAjax() {
		assertEquals("init", messages.getText());
		String previousBean = bean.getText();

		// Unload.
		guardHttp(unload).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("unload init", messages.getText());


		// Submit then unload.
		guardHttp(nonAjaxSubmit).click();
		assertEquals(previousBean, previousBean = bean.getText());
		assertEquals("submit", messages.getText());

		guardHttp(unload).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("unload init", messages.getText());


		// Navigate then unload.
		guardHttp(nonAjaxNavigate).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("navigate destroy init", messages.getText());

		guardHttp(unload).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("unload init", messages.getText());


		// Submit then navigate then unload.
		guardHttp(nonAjaxSubmit).click();
		assertEquals(previousBean, previousBean = bean.getText());
		assertEquals("submit", messages.getText());

		guardHttp(nonAjaxNavigate).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("navigate destroy init", messages.getText());

		guardHttp(unload).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals(messages.getText(), "unload init");


		// Navigate then submit then unload.
		guardHttp(nonAjaxNavigate).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("navigate destroy init", messages.getText());

		guardHttp(nonAjaxSubmit).click();
		assertEquals(previousBean, previousBean = bean.getText());
		assertEquals("submit", messages.getText());

		guardHttp(unload).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals(messages.getText(), "unload init");
	}

	@Test @InSequence(2)
	public void ajax() {

		// Unloaded bean is from previous test.
		assertEquals("unload init", messages.getText());
		String previousBean = bean.getText();


		// Submit then unload.
		guardAjax(ajaxSubmit).click();
		assertEquals(previousBean, previousBean = bean.getText());
		assertEquals("submit", messages.getText());

		guardHttp(unload).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("unload init", messages.getText());


		// Navigate then unload.
		guardAjax(ajaxNavigate).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("navigate destroy init", messages.getText());

		guardHttp(unload).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("unload init", messages.getText());


		// Submit then navigate then unload.
		guardAjax(ajaxSubmit).click();
		assertEquals(previousBean, previousBean = bean.getText());
		assertEquals("submit", messages.getText());

		guardAjax(ajaxNavigate).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("navigate destroy init", messages.getText());

		guardHttp(unload).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("unload init", messages.getText());


		// Navigate then submit then unload.
		guardAjax(ajaxNavigate).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("navigate destroy init", messages.getText());

		guardAjax(ajaxSubmit).click();
		assertEquals(previousBean, previousBean = bean.getText());
		assertEquals("submit", messages.getText());

		guardHttp(unload).click();
		assertNotEquals(previousBean, previousBean = bean.getText());
		assertEquals("unload init", messages.getText());
	}

}