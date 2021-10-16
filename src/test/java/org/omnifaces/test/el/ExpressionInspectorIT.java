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
package org.omnifaces.test.el;

import static org.junit.Assert.assertEquals;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class ExpressionInspectorIT extends OmniFacesIT {

	@FindBy(id="valueReferenceBase")
	private WebElement valueReferenceBase;

	@FindBy(id="valueReferenceProperty")
	private WebElement valueReferenceProperty;

	@FindBy(id="getterReferenceBase")
	private WebElement getterReferenceBase;

	@FindBy(id="getterReferenceMethodName")
	private WebElement getterReferenceMethodName;

	@FindBy(id="getterReferenceReturnType")
	private WebElement getterReferenceReturnType;

	@FindBy(id="getterReferenceParamTypes")
	private WebElement getterReferenceParamTypes;

	@FindBy(id="getterReferenceActualParameters")
	private WebElement getterReferenceActualParameters;

	@FindBy(id="methodReferenceBase")
	private WebElement methodReferenceBase;

	@FindBy(id="methodReferenceMethodName")
	private WebElement methodReferenceMethodName;

	@FindBy(id="methodReferenceReturnType")
	private WebElement methodReferenceReturnType;

	@FindBy(id="methodReferenceFirstParamType")
	private WebElement methodReferenceFirstParamType;

	@FindBy(id="methodReferenceFirstActualParameter")
	private WebElement methodReferenceFirstActualParameter;

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return createWebArchive(ExpressionInspectorIT.class);
	}

	@Test
	public void test() {
		assertEquals("valueReferenceBase is 'Bar'", isOWBUsed() ? "ExpressionInspectorITBean$Bar$$OwbNormalScopeProxy0" : "Bar", valueReferenceBase.getText());
		assertEquals("valueReferenceProperty is 'selected'", "selected", valueReferenceProperty.getText());
		assertEquals("getterReferenceBase is 'Bar'", isOWBUsed() ? "ExpressionInspectorITBean$Bar$$OwbNormalScopeProxy0" : "Bar", getterReferenceBase.getText());
		assertEquals("getterReferenceMethodName is 'getSelected'", "getSelected", getterReferenceMethodName.getText());
		assertEquals("getterReferenceReturnType is 'Baz'", "Baz", getterReferenceReturnType.getText());
		assertEquals("getterReferenceParamTypes is empty", "", getterReferenceParamTypes.getText());
		assertEquals("getterReferenceActualParameters is empty", "", getterReferenceActualParameters.getText());
		assertEquals("methodReferenceBase is 'Foo'", isOWBUsed() ? "ExpressionInspectorITBean$Foo$$OwbNormalScopeProxy0" : "Foo", methodReferenceBase.getText());
		assertEquals("methodReferenceMethodName is 'create'", "create", methodReferenceMethodName.getText());
		assertEquals("methodReferenceReturnType is 'void'", "void", methodReferenceReturnType.getText());
		assertEquals("methodReferenceFirstParamType is 'Baz'", "Baz", methodReferenceFirstParamType.getText());
		assertEquals("methodReferenceFirstActualParameter is 'Baz'", "Baz", methodReferenceFirstActualParameter.getText());
	}

}

