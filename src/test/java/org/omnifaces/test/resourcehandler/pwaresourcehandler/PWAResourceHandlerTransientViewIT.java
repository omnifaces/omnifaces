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
package org.omnifaces.test.resourcehandler.pwaresourcehandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.cdi.ViewScoped;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * The transient view must be the first view which the browser visits, hence this separate test class with its own browser instance. Navigating away from a
 * stateful view fires the {@link ViewScoped} unload beacon, and answering that makes the state manager read the server side state, which on MyFaces creates a
 * session.
 */
public class PWAResourceHandlerTransientViewIT extends OmniFacesIT {

    @FindBy(id = "transient")
    private WebElement transientView;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return createWebArchive(PWAResourceHandlerTransientViewIT.class);
    }

    /**
     * Referencing the manifest must not create an HTTP session. A stateless application declares its views transient precisely so that no per-user server state
     * exists, and a session created behind its back reintroduces sticky routing and replication for every request thereafter.
     */
    @Test
    void verifyNoSessionIsCreatedOnTransientView() {
        assertEquals("true", transientView.getText(), "The view is transient");
        assertNull(browser.manage().getCookieNamed("JSESSIONID"), "No session is created");
    }

}
