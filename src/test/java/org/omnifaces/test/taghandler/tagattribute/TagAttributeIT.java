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
package org.omnifaces.test.taghandler.tagattribute;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class TagAttributeIT extends OmniFacesIT {

	@FindBy(css="#first .id")
	private WebElement first;

	@FindBy(css="#second .id")
	private WebElement second;

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return buildWebArchive(TagAttributeIT.class)
			.withWebXml(WebXml.withTaglib)
			.createDeployment();
	}

	@Test
	public void test() {
		assertNotEquals(second.getText(), first.getText(), "second id is not equal to the first id");
	}

}