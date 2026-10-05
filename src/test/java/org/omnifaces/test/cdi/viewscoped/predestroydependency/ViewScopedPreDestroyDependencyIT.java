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
package org.omnifaces.test.cdi.viewscoped.predestroydependency;

import static java.util.Arrays.asList;
import static org.jboss.arquillian.graphene.Graphene.guardHttp;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * The {@code @PreDestroy} of a view scoped bean must resolve the same instance of another view scoped bean of the same
 * view, and must not create a new one, when the view is destroyed by an unload or by a navigation.
 */
public class ViewScopedPreDestroyDependencyIT extends OmniFacesIT {

	@FindBy(id="dependency")
	private WebElement dependency;

	@FindBy(id="created")
	private WebElement created;

	@FindBy(id="seen")
	private WebElement seen;

	@FindBy(id="unload")
	private WebElement unload;

	@FindBy(id="form:navigate")
	private WebElement navigate;

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return createWebArchive(ViewScopedPreDestroyDependencyIT.class);
	}

	@Test
	public void unload() {
		assertDestroyedWithSameDependency(unload);
	}

	@Test
	public void navigate() {
		assertDestroyedWithSameDependency(navigate);
	}

	private void assertDestroyedWithSameDependency(WebElement destroyer) {
		String previousDependency = dependency.getText();
		int previousCreated = Integer.parseInt(created.getText());

		guardHttp(destroyer).click();

		assertAll(
			() -> assertNotEquals(previousDependency, dependency.getText(), "new view has new dependency"),
			() -> assertTrue(asList(seen.getText().split(" ")).contains(previousDependency), () -> "dependency " + previousDependency + " seen on destroy in " + seen.getText()),
			() -> assertEquals(previousCreated + 1, Integer.parseInt(created.getText()), "only the dependency of the new view is created")
		);
	}

}
