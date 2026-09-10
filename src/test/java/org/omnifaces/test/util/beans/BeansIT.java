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
package org.omnifaces.test.util.beans;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * The bean manager is obtained only once per web application, which is identified by the context class loader of the request thread. This relies on that class
 * loader being the same object for every request of the same web application, no matter which thread of the pool serves it.
 */
public class BeansIT extends OmniFacesIT {

    @FindBy(id="contextClassLoaderStable")
    private WebElement contextClassLoaderStable;

    @FindBy(id="managerRemembered")
    private WebElement managerRemembered;

    @Deployment(testable=false)
    public static WebArchive createDeployment() {
        return createWebArchive(BeansIT.class);
    }

    @Test
    public void test() {
        for (int request = 1; request <= 4; request++) {
            assertEquals("true", contextClassLoaderStable.getText(), "context class loader is the same instance during request " + request);
            assertEquals("true", managerRemembered.getText(), "bean manager is the same instance during request " + request);
            refresh();
        }
    }

}
