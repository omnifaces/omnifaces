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
package org.omnifaces.test.component.lazypanel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LazyPanelIT extends OmniFacesIT {

    @FindBy(id = "form:lazy")
    private WebElement lazy;

    @FindBy(id = "form:lazyNoArgs")
    private WebElement lazyNoArgs;

    @FindBy(id = "form:lazyNoListener")
    private WebElement lazyNoListener;

    @FindBy(id = "form:lazyParams")
    private WebElement lazyParams;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return createWebArchive(LazyPanelIT.class);
    }

    @Test
    void testLazyPanel() {
        open("LazyPanelIT.xhtml");
        assertTrue(isDisplayed("placeholder"), "placeholder is shown initially");
        assertFalse(isPresent("loaded"), "loaded content is not yet rendered");

        scrollIntoView(lazy);
        waitUntilTextContains("form:lazy", "loaded content");
        assertTrue(isPresent("loaded"), "loaded content is now rendered");
        assertFalse(isPresent("placeholder"), "placeholder is replaced");
        assertEquals("1", browser.findElement(By.id("triggerCount")).getText(), "onload fired exactly once");
        assertEquals("form:lazy", browser.findElement(By.id("triggeredClientId")).getText(), "event carries component client id");
    }

    @Test
    void testLazyPanelNoArgs() {
        open("LazyPanelNoArgsIT.xhtml");
        assertTrue(isDisplayed("placeholder"), "placeholder is shown initially");
        assertFalse(isPresent("loaded"), "loaded content is not yet rendered");

        scrollIntoView(lazyNoArgs);
        waitUntilTextContains("form:lazyNoArgs", "loaded content");
        assertTrue(isPresent("loaded"), "loaded content is now rendered");
        assertFalse(isPresent("placeholder"), "placeholder is replaced");
        assertEquals("1", browser.findElement(By.id("triggerCount")).getText(), "onload fired exactly once");
        assertEquals("N/A", browser.findElement(By.id("triggeredClientId")).getText(), "event was not available");
    }

    @Test
    void testLazyPanelNoListener() {
        open("LazyPanelNoListenerIT.xhtml");
        assertTrue(isDisplayed("placeholder"), "placeholder is shown initially");
        assertFalse(isPresent("loaded"), "loaded content is not yet rendered");

        scrollIntoView(lazyNoListener);
        waitUntilTextContains("form:lazyNoListener", "loaded content");
        assertTrue(isPresent("loaded"), "loaded content is now rendered");
        assertFalse(isPresent("placeholder"), "placeholder is replaced");
        assertEquals("0", browser.findElement(By.id("triggerCount")).getText(), "listener not fired");
        assertEquals("", browser.findElement(By.id("triggeredClientId")).getText(), "listener not fired");
    }

    @Test
    void testLazyPanelParams() {
        open("LazyPanelParamsIT.xhtml");
        assertTrue(isDisplayed("placeholder"), "placeholder is shown initially");
        assertFalse(isPresent("loaded"), "loaded content is not yet rendered");

        scrollIntoView(lazyParams);
        waitUntilTextContains("form:lazyParams", "loaded content");
        assertTrue(isPresent("loaded"), "loaded content is now rendered");
        assertEquals("1", browser.findElement(By.id("triggerCount")).getText(), "onload fired exactly once");
        assertEquals("42", browser.findElement(By.id("productId")).getText(), "f:param productId passed through");
        assertEquals("active", browser.findElement(By.id("filter")).getText(), "f:param filter passed through");
        assertEquals("", browser.findElement(By.id("disabledParam")).getText(), "disabled f:param is skipped");
        assertEquals("", browser.findElement(By.id("emptyParam")).getText(), "empty f:param value is skipped");
    }

    private boolean isPresent(String id) {
        return !browser.findElements(By.id(id)).isEmpty();
    }

    private boolean isDisplayed(String id) {
        var elements = browser.findElements(By.id(id));
        return !elements.isEmpty() && elements.get(0).isDisplayed();
    }

}
