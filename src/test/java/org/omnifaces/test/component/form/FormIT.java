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
package org.omnifaces.test.component.form;

import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.faces.component.behavior.ClientBehaviorContext;
import jakarta.faces.context.PartialViewContext;
import jakarta.faces.render.ResponseStateManager;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class FormIT extends OmniFacesIT {

	@FindBy(id="formDefault:submit")
	private WebElement formDefaultSubmit;

	@FindBy(id="formDefault:params")
	private WebElement formDefaultParams;

	@FindBy(id="formDisabled:submit")
	private WebElement formDisabledSubmit;

	@FindBy(id="formDisabled:params")
	private WebElement formDisabledParams;

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return createWebArchive(FormIT.class);
	}

	@Test
	void testFormDefault() {
		guardAjax(formDefaultSubmit).click();
		String params = formDefaultParams.getText();
		assertTrue(params.contains("formDefault"), "formDefault");
		assertFalse(params.contains("formDefault:input1"), "formDefault:input1");
		assertTrue(params.contains("formDefault:input2"), "formDefault:input2");
		assertTrue(params.contains("formDefault:input3"), "formDefault:input3");
		assertFalse(params.contains("formDefault:input4"), "formDefault:input4");
		assertTrue(params.contains(ResponseStateManager.VIEW_STATE_PARAM), ResponseStateManager.VIEW_STATE_PARAM);
		assertTrue(params.contains(ClientBehaviorContext.BEHAVIOR_SOURCE_PARAM_NAME), ClientBehaviorContext.BEHAVIOR_SOURCE_PARAM_NAME);
		assertTrue(params.contains(ClientBehaviorContext.BEHAVIOR_EVENT_PARAM_NAME), ClientBehaviorContext.BEHAVIOR_EVENT_PARAM_NAME);
		assertTrue(params.contains(PartialViewContext.PARTIAL_EVENT_PARAM_NAME), PartialViewContext.PARTIAL_EVENT_PARAM_NAME);
		assertTrue(params.contains(PartialViewContext.PARTIAL_EXECUTE_PARAM_NAME), PartialViewContext.PARTIAL_EXECUTE_PARAM_NAME);
		assertTrue(params.contains(PartialViewContext.PARTIAL_RENDER_PARAM_NAME), PartialViewContext.PARTIAL_RENDER_PARAM_NAME);
	}

	@Test
	void testFormDisabledPartialSubmit() {
		guardAjax(formDisabledSubmit).click();
		String params = formDisabledParams.getText();
		assertTrue(params.contains("formDisabled"), "formDisabled");
		assertTrue(params.contains("formDisabled:input1"), "formDisabled:input1");
		assertTrue(params.contains("formDisabled:input2"), "formDisabled:input2");
		assertTrue(params.contains("formDisabled:input3"), "formDisabled:input3");
		assertTrue(params.contains("formDisabled:input4"), "formDisabled:input4");
		assertTrue(params.contains(ResponseStateManager.VIEW_STATE_PARAM), ResponseStateManager.VIEW_STATE_PARAM);
		assertTrue(params.contains(ClientBehaviorContext.BEHAVIOR_SOURCE_PARAM_NAME), ClientBehaviorContext.BEHAVIOR_SOURCE_PARAM_NAME);
		assertTrue(params.contains(ClientBehaviorContext.BEHAVIOR_EVENT_PARAM_NAME), ClientBehaviorContext.BEHAVIOR_EVENT_PARAM_NAME);
		assertTrue(params.contains(PartialViewContext.PARTIAL_EVENT_PARAM_NAME), PartialViewContext.PARTIAL_EVENT_PARAM_NAME);
		assertTrue(params.contains(PartialViewContext.PARTIAL_EXECUTE_PARAM_NAME), PartialViewContext.PARTIAL_EXECUTE_PARAM_NAME);
		assertTrue(params.contains(PartialViewContext.PARTIAL_RENDER_PARAM_NAME), PartialViewContext.PARTIAL_RENDER_PARAM_NAME);
	}

}