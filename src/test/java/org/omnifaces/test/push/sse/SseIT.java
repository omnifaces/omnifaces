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
package org.omnifaces.test.push.sse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

@DisabledIfSystemProperty(
    named = "profile.id", matches = "quarkus-.*", disabledReason = "quarkus-omnifaces needs to be upgraded to support PushContextProducer and SseChannelManager.ViewScope"
)
public class SseIT extends OmniFacesIT {

    @FindBy(id = "messages")
    private WebElement messages;

    @FindBy(id = "newtab")
    private WebElement newtab;

    @FindBy(id = "clientOpenedMessages")
    private WebElement clientOpenedMessages;

    @FindBy(id = "applicationScopedServerEventMessage")
    private WebElement applicationScopedServerEventMessage;

    @FindBy(id = "push:applicationScopedServerEvent")
    private WebElement pushApplicationScopedServerEvent;

    @FindBy(id = "sessionScopedUserTargetedMessage")
    private WebElement sessionScopedUserTargetedMessage;

    @FindBy(id = "push:sessionScopedUserTargeted")
    private WebElement pushSessionScopedUserTargeted;

    @FindBy(id = "viewScopedAjaxAwareMessage")
    private WebElement viewScopedAjaxAwareMessage;

    @FindBy(id = "push:viewScopedAjaxAware")
    private WebElement pushViewScopedAjaxAware;

    @FindBy(id = "clientClosedMessages")
    private WebElement clientClosedMessages;

    @FindBy(id = "closeAllSse")
    private WebElement closeAllSse;

    @FindBy(id = "poll:pollOpenedChannels")
    private WebElement pollOpenedChannels;

    @FindBy(id = "poll:pollClosedChannels")
    private WebElement pollClosedChannels;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return createWebArchive(SseIT.class);
    }

    @Test
    void test() {
        testOnopen(false);

        assertEquals(pushApplicationScopedServerEvent(), "1," + applicationScopedServerEventMessage.getText());
        assertEquals(pushSessionScopedUserTargeted(), "1," + sessionScopedUserTargetedMessage.getText());
        assertEquals(pushViewScopedAjaxAware(), "1," + viewScopedAjaxAwareMessage.getText());

        assertEquals(pushApplicationScopedServerEvent(), "1," + applicationScopedServerEventMessage.getText());
        assertEquals(pushSessionScopedUserTargeted(), "1," + sessionScopedUserTargetedMessage.getText());
        assertEquals(pushViewScopedAjaxAware(), "1," + viewScopedAjaxAwareMessage.getText());

        String firstTab = browser.getWindowHandle();
        openNewTab(newtab);
        testOnopen(true); // NOTE: application scoped push is disabled in new tab because currently used Chrome browser limits concurrent HTTP 1.1 connections
                          // to 6 (so we cannot use 3 SSE channels in second tab and expect ajax to continue working).

        assertEquals(pushSessionScopedUserTargeted(), "2," + sessionScopedUserTargetedMessage.getText());
        assertEquals(pushViewScopedAjaxAware(), "1," + viewScopedAjaxAwareMessage.getText());

        assertEquals(pushSessionScopedUserTargeted(), "2," + sessionScopedUserTargetedMessage.getText());
        assertEquals(pushViewScopedAjaxAware(), "1," + viewScopedAjaxAwareMessage.getText());

        // Unfortunately Selenium doesn't (seem to?) support starting a new HTTP session within the same IT, so
        // application, session and user sockets can't be tested more extensively. If possible somehow, it's expected
        // that numbers should equal respectively 3, 2, 1 on first session and 3, 1, 1 on second session.

        testOnclose(firstTab);
    }

    private void testOnopen(boolean newtab) {
        if (!newtab) {
            waitUntilTextContains(clientOpenedMessages, "|applicationScopedServerEvent|");
        }

        waitUntilTextContains(clientOpenedMessages, "|sessionScopedUserTargeted|");
        waitUntilTextContains(clientOpenedMessages, "|viewScopedAjaxAware|");

        pollUntilOpenedEventsReceived();

        var openedChannels = browser.findElement(By.id("serverOpenedMessages")).getText();

        if (!newtab) {
            assertTrue(openedChannels.contains("applicationScopedServerEvent"));
        }

        assertTrue(openedChannels.contains("sessionScopedUserTargeted"));
        assertTrue(openedChannels.contains("viewScopedAjaxAware"));
    }

    private void testOnclose(String tabToSwitch) {
        closeAllSse.click();
        waitUntilTextContains(clientClosedMessages, "|sessionScopedUserTargeted|");
        waitUntilTextContains(clientClosedMessages, "|viewScopedAjaxAware|");

        pollUntilClosedEventsReceived();

        var closedChannels = browser.findElement(By.id("serverClosedMessages")).getText();
        assertTrue(closedChannels.contains("sessionScopedUserTargeted"));
        assertTrue(closedChannels.contains("viewScopedAjaxAware"));

        closeCurrentTabAndSwitchTo(tabToSwitch);
    }

    private void pollUntilOpenedEventsReceived() {
        waitUntilTextContent("serverOpenedMessages", pollOpenedChannels::click);
    }

    private void pollUntilClosedEventsReceived() {
        waitUntilTextContent("serverClosedMessages", pollClosedChannels::click);
    }

    private String pushApplicationScopedServerEvent() {
        guardAjax(pushApplicationScopedServerEvent::click);
        String message = messages.getText();
        waitUntilTextContent(applicationScopedServerEventMessage);
        return message;
    }

    private String pushSessionScopedUserTargeted() {
        guardAjax(pushSessionScopedUserTargeted::click);
        String message = messages.getText();
        waitUntilTextContent(sessionScopedUserTargetedMessage);
        return message;
    }

    private String pushViewScopedAjaxAware() {
        guardAjax(pushViewScopedAjaxAware::click);
        String message = messages.getText();
        waitUntilTextContent(viewScopedAjaxAwareMessage);
        return message;
    }

}
