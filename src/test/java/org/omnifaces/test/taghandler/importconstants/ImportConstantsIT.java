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
package org.omnifaces.test.taghandler.importconstants;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class ImportConstantsIT extends OmniFacesIT {

	@FindBy(id="parent")
	private WebElement parent;

	@FindBy(id="child")
	private WebElement child;

	@FindBy(id="interface")
	private WebElement iface;

	@FindBy(id="enumValues")
	private WebElement enumValues;

	@FindBy(id="enumInterfaceValues")
	private WebElement enumInterfaceValues;

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return buildWebArchive(ImportConstantsIT.class)
			.createDeployment();
	}

	@Test
	public void testParent() {
		assertEquals("parent", parent.getText());
	}

	@Test
	public void testChild() {
		assertEquals("child", child.getText());
	}

	@Test
	public void testInterface() {
		assertEquals("interface", iface.getText());
	}

	@Test
	public void testEnum() {
		assertEquals("[ONE, TWO, THREE]", enumValues.getText());
	}

	@Test
	public void testEnumImplementingInterface() {
		assertEquals("[ONE, TWO, THREE, interface]", enumInterfaceValues.getText());
	}

}