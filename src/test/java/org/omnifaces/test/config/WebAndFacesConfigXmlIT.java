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
package org.omnifaces.test.config;

import static java.lang.Boolean.parseBoolean;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omnifaces.test.OmniFacesIT.FacesConfig.withSupportedLocales;
import static org.omnifaces.test.OmniFacesIT.WebXml.withErrorPage;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class WebAndFacesConfigXmlIT extends OmniFacesIT {

    @FindBy(id = "webXmlInjected")
    private WebElement webXmlInjected;

    @FindBy(id = "facesConfigXmlInjected")
    private WebElement facesConfigXmlInjected;

    @FindBy(id = "errorPageLocations")
    private WebElement errorPageLocations;

    @FindBy(id = "supportedLocales")
    private WebElement supportedLocales;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return buildWebArchive(WebAndFacesConfigXmlIT.class)
            .withFacesConfig(withSupportedLocales)
            .withWebXml(withErrorPage)
            .withPrimeFaces()
            .createDeployment();
    }

    @Test
    void test() {
        assertAll(
            () -> assertTrue(parseBoolean(webXmlInjected.getText())),
            () -> assertTrue(parseBoolean(facesConfigXmlInjected.getText())),
            () -> assertEquals("{null=/WEB-INF/500.xhtml}", errorPageLocations.getText()),
            () -> assertEquals("[nl, es, en, pt]", supportedLocales.getText())
        );
    }

}
