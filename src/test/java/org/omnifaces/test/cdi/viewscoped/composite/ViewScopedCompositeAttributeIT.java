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
package org.omnifaces.test.cdi.viewscoped.composite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Reproducer for issue #956. A composite component is bound to an <code>@ViewScoped</code> bean property. On a full page refresh, the OmniFaces deferred view
 * state removal (issue #941, ff94cdc) temporarily swaps in a <code>RemoveViewStateFacesContext</code> via <code>setContext()</code>; MyFaces then caches the
 * request <code>ELContext</code> bound to that temporary wrapper, so for the rest of the request <code>#{cc}</code> resolves against the wrapper's detached
 * attributes and ends up <code>null</code>. As a result <code>#{cc.attrs.value}</code> resolves to <code>null</code> even though the very same
 * <code>#{bean.value}</code> rendered by a sibling <code>&lt;h:outputText&gt;</code> is intact.
 */
public class ViewScopedCompositeAttributeIT extends OmniFacesIT {

    private static final int REFRESH_COUNT = 5;

    @FindBy(id = "form:output")
    private WebElement outputValue;

    @FindBy(id = "form:composite:value")
    private WebElement compositeValue;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return createWebArchive(ViewScopedCompositeAttributeIT.class);
    }

    @Test
    void compositeAttributeSurvivesRefresh() {
        assertCompositeValueIntact("initial render");

        for (var i = 1; i <= REFRESH_COUNT; i++) {
            refresh();
            assertCompositeValueIntact("full page refresh #" + i);
        }
    }

    private void assertCompositeValueIntact(String when) {
        var output = outputValue.getText();
        var composite = compositeValue.getText();
        assertEquals(
            output, composite,
            "Composite component lost its 'value' attribute on " + when
                + ": #{bean.value} resolves to '" + output
                + "' but #{cc.attrs.value} resolves to '" + composite + "'"
        );
    }

}
