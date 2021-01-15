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
package org.omnifaces.test.cdi.facesconverter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
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
	public void test() {
		waitUntilTextContent(loaded); // Wait until "load" event is finished because of resource dependencies.

		assertTrue("EJB is injected in facesConverterITConverter", facesConverterITConverter.getText().startsWith("FacesConverterITSomeEJB"));
		assertEquals("ResourceDependency is injected in facesConverterITConverter", "facesConverterITConverterResourceDependency" ,facesConverterITConverterResourceDependency.getText());
		assertTrue("EJB is injected in facesConverterITManagedConverter", facesConverterITManagedConverter.getText().startsWith("FacesConverterITSomeEJB"));

		if (!isMojarra()) { // Mojarra bugs on this.
			assertEquals("ResourceDependency is injected in facesConverterITManagedConverter", "facesConverterITManagedConverterResourceDependency", facesConverterITManagedConverterResourceDependency.getText());
		}

		assertTrue("EJB is injected in facesConverterITExtendedConverter", facesConverterITExtendedConverter.getText().startsWith("FacesConverterITSomeEJB"));
		assertTrue("ResourceDependency is injected in facesConverterITExtendedConverter", facesConverterITExtendedConverterResourceDependency.getText().equals("facesConverterITExtendedConverterResourceDependency"));
		assertTrue("EJB is injected in facesConverterITExtendedManagedConverter", facesConverterITExtendedManagedConverter.getText().startsWith("FacesConverterITSomeEJB"));

		if (!isMojarra()) { // Mojarra bugs on this.
			assertEquals("ResourceDependency is injected in facesConverterITExtendedManagedConverter", "facesConverterITExtendedManagedConverterResourceDependency", facesConverterITExtendedManagedConverterResourceDependency.getText());
		}

		init(); // This basically refreshes the page.
		waitUntilTextContent(loaded);

		assertTrue("EJB is still injected in facesConverterITConverter after page refresh", facesConverterITConverter.getText().startsWith("FacesConverterITSomeEJB"));
		assertEquals("ResourceDependency is still injected in facesConverterITConverter after page refresh", "facesConverterITConverterResourceDependency", facesConverterITConverterResourceDependency.getText());
		assertTrue("EJB is still injected in facesConverterITManagedConverter after page refresh", facesConverterITManagedConverter.getText().startsWith("FacesConverterITSomeEJB"));

		if (!isMojarra()) { // Mojarra bugs on this.
			assertEquals("ResourceDependency is still injected in facesConverterITManagedConverter after page refresh", "facesConverterITManagedConverterResourceDependency", facesConverterITManagedConverterResourceDependency.getText());
		}

		assertTrue("EJB is still injected in facesConverterITExtendedConverter after page refresh", facesConverterITExtendedConverter.getText().startsWith("FacesConverterITSomeEJB"));
		assertEquals("ResourceDependency is still injected in facesConverterITExtendedConverter after page refresh", "facesConverterITExtendedConverterResourceDependency", facesConverterITExtendedConverterResourceDependency.getText());
		assertTrue("EJB is still injected in facesConverterITExtendedManagedConverter after page refresh", facesConverterITExtendedManagedConverter.getText().startsWith("FacesConverterITSomeEJB"));

		if (!isMojarra()) { // Mojarra bugs on this.
			assertEquals("ResourceDependency is still injected in facesConverterITExtendedManagedConverter after page refresh", "facesConverterITExtendedManagedConverterResourceDependency", facesConverterITExtendedManagedConverterResourceDependency.getText());
		}
	}

}