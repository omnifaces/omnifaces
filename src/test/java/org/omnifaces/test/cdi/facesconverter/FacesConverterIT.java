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
package org.omnifaces.test.cdi.facesconverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class FacesConverterIT extends OmniFacesIT {

	@FindBy(id="facesConverterITConverter")
	private WebElement facesConverterITConverter;

	@FindBy(id="facesConverterITManagedConverter")
	private WebElement facesConverterITManagedConverter;

	@FindBy(id="facesConverterITConverterResourceDependency")
	private WebElement facesConverterITConverterResourceDependency;

	@FindBy(id="facesConverterITManagedConverterResourceDependency")
	private WebElement facesConverterITManagedConverterResourceDependency;

	@FindBy(id="facesConverterITExtendedConverter")
	private WebElement facesConverterITExtendedConverter;

	@FindBy(id="facesConverterITExtendedManagedConverter")
	private WebElement facesConverterITExtendedManagedConverter;

	@FindBy(id="facesConverterITExtendedConverterResourceDependency")
	private WebElement facesConverterITExtendedConverterResourceDependency;

	@FindBy(id="facesConverterITExtendedManagedConverterResourceDependency")
	private WebElement facesConverterITExtendedManagedConverterResourceDependency;

	@FindBy(id="loaded")
	private WebElement loaded;

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return createWebArchive(FacesConverterIT.class);
	}

	@Test
	void test() {
		waitUntilTextContent(loaded); // Wait until "load" event is finished because of resource dependencies.

		assertTrue(facesConverterITConverter.getText().startsWith("FacesConverterITSomeEJB"), "EJB is injected in facesConverterITConverter");
		assertEquals("facesConverterITConverterResourceDependency" ,facesConverterITConverterResourceDependency.getText(), "ResourceDependency is injected in facesConverterITConverter");
		assertTrue(facesConverterITManagedConverter.getText().startsWith("FacesConverterITSomeEJB"), "EJB is injected in facesConverterITManagedConverter");

		if (!isMojarraUsed()) { // Mojarra bugs on this (fixed in 2.3.16, so this check can be removed once all IT envs run 2.3.16 or newer).
			assertEquals("facesConverterITManagedConverterResourceDependency", facesConverterITManagedConverterResourceDependency.getText(), "ResourceDependency is injected in facesConverterITManagedConverter");
		}

		assertTrue(facesConverterITExtendedConverter.getText().startsWith("FacesConverterITSomeEJB"), "EJB is injected in facesConverterITExtendedConverter");
		assertEquals("facesConverterITExtendedConverterResourceDependency", facesConverterITExtendedConverterResourceDependency.getText(), "ResourceDependency is injected in facesConverterITExtendedConverter");
		assertTrue(facesConverterITExtendedManagedConverter.getText().startsWith("FacesConverterITSomeEJB"), "EJB is injected in facesConverterITExtendedManagedConverter");

		if (!isMojarraUsed()) { // Mojarra bugs on this (fixed in 2.3.16, so this check can be removed once all IT envs run 2.3.16 or newer).
			assertEquals("facesConverterITExtendedManagedConverterResourceDependency", facesConverterITExtendedManagedConverterResourceDependency.getText(), "ResourceDependency is injected in facesConverterITExtendedManagedConverter");
		}

		init(); // This basically refreshes the page.
		waitUntilTextContent(loaded);

		assertTrue(facesConverterITConverter.getText().startsWith("FacesConverterITSomeEJB"), "EJB is still injected in facesConverterITConverter after page refresh");
		assertEquals("facesConverterITConverterResourceDependency", facesConverterITConverterResourceDependency.getText(), "ResourceDependency is still injected in facesConverterITConverter after page refresh");
		assertTrue(facesConverterITManagedConverter.getText().startsWith("FacesConverterITSomeEJB"), "EJB is still injected in facesConverterITManagedConverter after page refresh");

		if (!isMojarraUsed()) { // Mojarra bugs on this (fixed in 2.3.16, so this check can be removed once all IT envs run 2.3.16 or newer).
			assertEquals("facesConverterITManagedConverterResourceDependency", facesConverterITManagedConverterResourceDependency.getText(), "ResourceDependency is still injected in facesConverterITManagedConverter after page refresh");
		}

		assertTrue(facesConverterITExtendedConverter.getText().startsWith("FacesConverterITSomeEJB"), "EJB is still injected in facesConverterITExtendedConverter after page refresh");
		assertEquals("facesConverterITExtendedConverterResourceDependency", facesConverterITExtendedConverterResourceDependency.getText(), "ResourceDependency is still injected in facesConverterITExtendedConverter after page refresh");
		assertTrue(facesConverterITExtendedManagedConverter.getText().startsWith("FacesConverterITSomeEJB"), "EJB is still injected in facesConverterITExtendedManagedConverter after page refresh");

		if (!isMojarraUsed()) { // Mojarra bugs on this (fixed in 2.3.16, so this check can be removed once all IT envs run 2.3.16 or newer).
			assertEquals("facesConverterITExtendedManagedConverterResourceDependency", facesConverterITExtendedManagedConverterResourceDependency.getText(), "ResourceDependency is still injected in facesConverterITExtendedManagedConverter after page refresh");
		}
	}

}