/*
 * Copyright 2020 OmniFaces
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

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return createWebArchive(FacesConverterIT.class);
	}

	@Test
	public void test() {
		assertTrue("EJB is injected in facesConverterITConverter", facesConverterITConverter.getText().startsWith("FacesConverterITSomeEJB"));

		if (!isTomee()) { // MyFaces bugs on this.
			assertTrue("EJB is injected in facesConverterITManagedConverter", facesConverterITManagedConverter.getText().startsWith("FacesConverterITSomeEJB"));
		}
	}

}