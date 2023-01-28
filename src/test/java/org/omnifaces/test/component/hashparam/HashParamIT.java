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
package org.omnifaces.test.component.hashparam;

import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class HashParamIT extends OmniFacesIT {

	@FindBy(id="pageLoadTimestamp")
	private WebElement pageLoadTimestamp;

	@FindBy(id="hashLoadTimestamp")
	private WebElement hashLoadTimestamp;

	@FindBy(id="form:foo")
	private WebElement foo;

	@FindBy(id="form:bar")
	private WebElement bar;

	@FindBy(id="form:submit")
	private WebElement submit;

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return createWebArchive(HashParamIT.class);
	}

	@Test
	void testHashParam() {
		openWithHashString("foo=baz&bar=kaz");
		waitUntilTextContent(this.hashLoadTimestamp); // TODO: should not have been necessary.

		long pageLoadTimestamp = Long.valueOf(this.pageLoadTimestamp.getText());
		long hashLoadTimestamp = Long.valueOf(this.hashLoadTimestamp.getText());
		assertTrue(hashLoadTimestamp > pageLoadTimestamp, "Hash param is set later");

		foo.sendKeys("");
		bar.sendKeys("bar");
		guardAjax(submit).click();
		assertTrue(browser.getCurrentUrl().endsWith("#bar=bar"), browser.getCurrentUrl() + " ends with #bar=bar (and thus not #&bar=bar)");

		foo.clear();
		bar.clear();
		foo.sendKeys("foo");
		bar.sendKeys("");
		guardAjax(submit).click();
		assertTrue(browser.getCurrentUrl().endsWith("#foo=foo"), browser.getCurrentUrl() + " ends with #foo=foo");

		foo.clear();
		bar.clear();
		foo.sendKeys("foo");
		bar.sendKeys("bar");
		guardAjax(submit).click();
		assertTrue(browser.getCurrentUrl().endsWith("#foo=foo&bar=bar"), browser.getCurrentUrl() + " ends with #foo=foo&bar=bar");

		foo.clear();
		bar.clear();
		foo.sendKeys("def");
		bar.sendKeys("def");
		guardAjax(submit).click();
		assertTrue(browser.getCurrentUrl().endsWith("#foo=def"), browser.getCurrentUrl() + " ends with #foo=def (and thus not #foo=def&bar=def)");

		foo.clear();
		bar.clear();
		foo.sendKeys("");
		bar.sendKeys("");
		guardAjax(submit).click();
		assertTrue(!browser.getCurrentUrl().contains("#"), browser.getCurrentUrl() + " ends with no hash param");
	}

}