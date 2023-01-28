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
package org.omnifaces.test.cdi.eager;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Long.parseLong;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class EagerIT extends OmniFacesIT {

	@FindBy(id="lazyApplicationScopedBean")
	private WebElement lazyApplicationScopedBean;

	@FindBy(id="eagerApplicationScopedBean")
	private WebElement eagerApplicationScopedBean;

	@FindBy(id="lazySessionScopedBean")
	private WebElement lazySessionScopedBean;

	@FindBy(id="eagerSessionScopedBean")
	private WebElement eagerSessionScopedBean;

	@FindBy(id="lazyViewScopedBean")
	private WebElement lazyViewScopedBean;

	@FindBy(id="eagerViewScopedBean")
	private WebElement eagerViewScopedBean;

	@FindBy(id="lazyRequestScopedBean")
	private WebElement lazyRequestScopedBean;

	@FindBy(id="eagerRequestScopedBean")
	private WebElement eagerRequestScopedBean;

	@FindBy(id="ejbInjectedInStartupBean")
	private WebElement ejbInjectedInStartupBean;

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return createWebArchive(EagerIT.class);
	}

	@Test
	void test() {
		assertTrue(parseLong(lazyApplicationScopedBean.getText()) > parseLong(eagerApplicationScopedBean.getText()));
		assertTrue(parseLong(lazySessionScopedBean.getText()) > parseLong(eagerSessionScopedBean.getText()));
		assertTrue(parseLong(lazyViewScopedBean.getText()) > parseLong(eagerViewScopedBean.getText()));
		assertTrue(parseLong(lazyRequestScopedBean.getText()) > parseLong(eagerRequestScopedBean.getText()));
		assertTrue(parseBoolean(ejbInjectedInStartupBean.getText()));
	}

}