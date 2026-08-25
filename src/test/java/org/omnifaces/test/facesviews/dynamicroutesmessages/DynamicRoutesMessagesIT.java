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
package org.omnifaces.test.facesviews.dynamicroutesmessages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.omnifaces.test.OmniFacesIT.WebXml.withFacesViewsAndDevelopmentStage;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * An outcome which resolves to a dynamic route must leave no faces message behind, in Development stage as well, where a Faces implementation warns about every
 * outcome for which it finds no navigation case of its own.
 */
public class DynamicRoutesMessagesIT extends OmniFacesIT {

    @FindBy(id = "messages")
    private WebElement messages;

    @FindBy(id = "form:forward")
    private WebElement forwardSubmit;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return buildWebArchive(DynamicRoutesMessagesIT.class)
            .withWebXml(withFacesViewsAndDevelopmentStage)
            .createDeployment();
    }

    @Test
    void testRenderedOutcomeTargetsLeaveNoMessage() {
        open("DynamicRoutesMessagesIT.xhtml");
        assertEquals(contextPath + "/organizations/977/members", hrefOf("link"));
        assertEquals(contextPath + "/organizations/977/members", hrefOf("passthroughLink"));
        assertEquals(contextPath + "/organizations/977/members", hrefOf("templateLink"));
        assertEquals("", messages.getText());
    }

    @Test
    void testNavigationToAConcreteOutcomeLeavesNoMessage() {
        open("DynamicRoutesMessagesIT.xhtml");
        guardHttp(forwardSubmit::click);
        assertEquals("members", browser.getTitle());
        assertEquals("", messages.getText());
    }

    private String hrefOf(String id) {
        return stripHostAndJsessionid(browser.findElement(By.id(id)).getAttribute("href"));
    }

}
