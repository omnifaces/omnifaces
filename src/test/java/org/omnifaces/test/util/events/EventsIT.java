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
package org.omnifaces.test.util.events;

import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.omnifaces.test.util.events.EventsITBean.CALLBACK_COUNT;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Phase listeners subscribed to the current request must be invoked in the order in which they were subscribed, so that
 * e.g. a script resource added by one of them is rendered before an inline script added by another one which needs it.
 */
public class EventsIT extends OmniFacesIT {

    @FindBy(id="invocations")
    private WebElement invocations;

    @Deployment(testable=false)
    public static WebArchive createDeployment() {
        return createWebArchive(EventsIT.class);
    }

    @Test
    public void subscribeToRequestBeforePhaseMustInvokeInSubscriptionOrder() {
        String expected = range(0, CALLBACK_COUNT).mapToObj(String::valueOf).collect(joining(", ", "[", "]"));
        assertEquals(expected, invocations.getText());
    }

}
