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
package org.omnifaces.test.resourcehandler.combinedresourcehandler;

import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.jboss.arquillian.graphene.Graphene.guardHttp;
import static org.junit.Assert.assertEquals;
import static org.omnifaces.test.OmniFacesIT.FacesConfig.withCombinedResourceHandler;
import static org.omnifaces.util.Utils.serializeURLSafe;

import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class CombinedResourceHandlerIT extends OmniFacesIT {

	private static final String HEAD_COMBINED_RESOURCE_NAME = serializeURLSafe("omnifaces:omnifaces.js|javax.faces:jsf.js|headWithTarget.js|bodyWithTargetHead.js");
	private static final String DEFERRED_COMBINED_RESOURCE_NAME = serializeURLSafe("deferredInHead.js|deferredInBody.js");

	@FindBy(css="script[crossorigin=anonymous]")
	private List<WebElement> combinedResources;

	@FindBy(id="bodyWithTargetBody")
	private WebElement bodyWithTargetBody;

	@FindBy(id="headWithoutTarget")
	private WebElement headWithoutTarget;

	@FindBy(id="headWithTarget")
	private WebElement headWithTarget;

	@FindBy(id="bodyWithTargetHead")
	private WebElement bodyWithTargetHead;

	@FindBy(id="bodyWithoutTarget")
	private WebElement bodyWithoutTarget;

	@FindBy(id="deferredInHead")
	private WebElement deferredInHead;

	@FindBy(id="deferredInBody")
	private WebElement deferredInBody;

	@FindBy(id="nonAjax:submit")
	private WebElement nonAjaxSubmit;

	@FindBy(id="nonAjax:rebuild")
	private WebElement nonAjaxRebuild;

	@FindBy(id="ajax:submit")
	private WebElement ajaxSubmit;

	@FindBy(id="ajax:rebuild")
	private WebElement ajaxRebuild;

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return buildWebArchive(CombinedResourceHandlerIT.class)
			.withFacesConfig(withCombinedResourceHandler)
			.createDeployment();
	}

	@Test
	public void nonAjax() {
		verifyElements();
		guardHttp(nonAjaxSubmit).click();
		verifyElements();
		guardHttp(nonAjaxRebuild).click();
		verifyElements();
	}

	@Test
	public void ajax() {
		verifyElements();
		guardAjax(ajaxSubmit).click();
		verifyElements();
		guardAjax(ajaxRebuild).click();
		verifyElements();
	}

	@Test
	public void mixed() {
		verifyElements();
		guardHttp(nonAjaxSubmit).click();
		verifyElements();
		guardAjax(ajaxSubmit).click();
		verifyElements();
		guardHttp(nonAjaxRebuild).click();
		verifyElements();
		guardAjax(ajaxSubmit).click();
		verifyElements();
		guardHttp(nonAjaxSubmit).click();
		verifyElements();
		guardAjax(ajaxRebuild).click();
		verifyElements();
		guardHttp(nonAjaxSubmit).click();
		verifyElements();
		guardAjax(ajaxSubmit).click();
		verifyElements();
	}

	private void verifyElements() {
		assertEquals(2, combinedResources.size());
		assertEquals(HEAD_COMBINED_RESOURCE_NAME, combinedResources.get(0).getAttribute("src").split("(.*/javax.faces.resource/)|(\\.js\\.xhtml.*)")[1]);
		assertEquals(DEFERRED_COMBINED_RESOURCE_NAME, combinedResources.get(1).getAttribute("src").split("(.*/javax.faces.resource/)|(\\.js\\.xhtml.*)")[1]);
		assertEquals("1,bodyWithTargetBody", bodyWithTargetBody.getText());
		assertEquals("2,headWithoutTarget", headWithoutTarget.getText());
		assertEquals("3,headWithTarget", headWithTarget.getText());
		assertEquals("4,bodyWithTargetHead", bodyWithTargetHead.getText());
		assertEquals("5,bodyWithoutTarget", bodyWithoutTarget.getText());
		assertEquals("6,deferredInHead", deferredInHead.getText());
		assertEquals("7,deferredInBody", deferredInBody.getText());
	}

}