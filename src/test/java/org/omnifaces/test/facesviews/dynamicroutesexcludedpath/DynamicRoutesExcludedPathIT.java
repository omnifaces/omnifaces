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
package org.omnifaces.test.facesviews.dynamicroutesexcludedpath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.omnifaces.test.OmniFacesIT.WebXml.withMultiViews;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * A path excluded from FacesViews by <code>!/excludedfolder</code> is excluded from dynamic routes as well: neither a dynamic route at the root nor one below
 * the excluded path answers for it.
 */
public class DynamicRoutesExcludedPathIT extends OmniFacesIT {

    @FindBy(id = "viewId")
    private WebElement viewId;

    @FindBy(id = "tenant")
    private WebElement tenant;

    @FindBy(id = "excludedLink")
    private WebElement excludedLink;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return buildWebArchive(DynamicRoutesExcludedPathIT.class)
            .withWebXml(withMultiViews)
            .createDeployment();
    }

    @Test
    void testDynamicRouteAtRootAnswersOutsideExcludedPath() {
        open("foo");
        assertEquals("/[tenant]/MultiViewsITWelcomeFile.xhtml", viewId.getText());
        assertEquals("foo", tenant.getText());
    }

    @Test
    void testServletBelowExcludedPathIsNotForwardedToDynamicRoute() {
        open("excludedfolder/servlet");
        assertEquals(DynamicRoutesExcludedPathITServlet.class.getSimpleName(), browser.getTitle());
    }

    @Test
    void testDynamicRouteBelowExcludedPathIsNotForwarded() {
        open("excludedfolder/42/excludedView");
        assertEquals("404", browser.getTitle());
    }

    @Test
    void testDynamicRouteBelowExcludedPathIsNotResolvedAsOutcome() {
        open("foo");
        assertNull(excludedLink.getDomAttribute("href"));
    }

}
