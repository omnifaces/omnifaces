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
package org.omnifaces.test.viewhandler;

import static org.junit.Assert.assertEquals;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.omnifaces.test.OmniFacesIT;

public class CheckNestedFormsIT extends OmniFacesIT {

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return buildWebArchive(CheckNestedFormsIT.class)
			.withWebXml(WebXml.withDevelopmentStage)
			.withPrimeFaces()
			.createDeployment();
	}

	@Test
	public void testProperMarkup() {
		verify200("ProperMarkup.xhtml");
	}

	@Test
	public void testCornercaseMarkup() {
		verify200("CornercaseMarkup.xhtml");
	}

	@Test
	public void testNestedForm() {
		verify500("NestedForm.xhtml");
	}

	@Test
	public void testNestedDialogForm() {
		verify500("NestedDialogForm.xhtml");
	}

	private void verify200(String path) {
		open(path);
		assertEquals(path, browser.getTitle());
	}

	private void verify500(String path) {
		open(path);
		assertEquals("500", browser.getTitle());
	}
}
