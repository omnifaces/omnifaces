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
package org.omnifaces.test.taghandler.massattribute;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class MassAttributeIT extends OmniFacesIT {

    @FindBy(id="form:outside")
    private WebElement outside;

    @FindBy(id="form:unconditional")
    private WebElement unconditional;

    @FindBy(id="form:direct")
    private WebElement direct;

    @FindBy(id="form:nested")
    private WebElement nested;

    @FindBy(id="form:show")
    private WebElement show;

    @Deployment(testable=false)
    public static WebArchive createDeployment() {
        return buildWebArchive(MassAttributeIT.class)
            .createDeployment();
    }

    /**
     * The attribute is set on the components created by the tag body, and on no component outside it.
     */
    @Test
    void testBuildWhichCreatesTheContainer() {
        assertTrue(outside.isEnabled());
        assertFalse(unconditional.isEnabled());
    }

    /**
     * A component created by the refresh which precedes render response gets the attribute as well, whether the tag
     * body creates it directly below the container or below a component which already exists.
     */
    @Test
    void testBuildWhichRefreshesTheContainer() {
        guardAjax(show::click);
        assertTrue(outside.isEnabled());
        assertFalse(unconditional.isEnabled());
        assertFalse(direct.isEnabled());
        assertFalse(nested.isEnabled());
    }

}
