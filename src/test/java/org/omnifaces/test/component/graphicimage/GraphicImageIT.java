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
package org.omnifaces.test.component.graphicimage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class GraphicImageIT extends OmniFacesIT {

    @FindBy(id = "image")
    private WebElement image;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return createWebArchive(GraphicImageIT.class);
    }

    @Test
    void testLazyImage() {
        open("GraphicImageIT.xhtml");
        assertEquals("true", image.getAttribute("data-lazy"));
        assertEquals(contextPath + "/jakarta.faces.resource/logo.png.xhtml", stripHostAndJsessionid(image.getAttribute("data-src")));
        assertEquals("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg'/%3E", image.getAttribute("src"));

        scrollIntoView(image);
        assertEquals(null, image.getAttribute("data-lazy"));
        assertEquals(null, image.getAttribute("data-src"));
        assertEquals(contextPath + "/jakarta.faces.resource/logo.png.xhtml", stripHostAndJsessionid(image.getAttribute("src")));
    }

}
