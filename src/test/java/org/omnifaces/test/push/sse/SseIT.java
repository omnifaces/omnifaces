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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class SseIT extends OmniFacesIT {

    @FindBy(id="messages")
    private WebElement messages;

    @FindBy(id="clientOpenedMessages")
    private WebElement clientOpenedMessages;

    @FindBy(id="applicationScopedEventMessage")
    private WebElement applicationScopedEventMessage;

    @FindBy(id="push:applicationScopedEvent")
    private WebElement pushApplicationScopedEvent;

    @FindBy(id="sessionScopedUserTargetedMessage")
    private WebElement sessionScopedUserTargetedMessage;

    @FindBy(id="push:sessionScopedUserTargeted")
    private WebElement pushSessionScopedUserTargeted;

    @FindBy(id="viewScopedAjaxAwareMessage")
    private WebElement viewScopedAjaxAwareMessage;

    @FindBy(id="push:viewScopedAjaxAware")
    private WebElement pushViewScopedAjaxAware;

    @FindBy(id="clientClosedMessages")
    private WebElement clientClosedMessages;

    @FindBy(id="closeAllSse")
    private WebElement closeAllSse;

    @Deployment(testable=false)
    public static WebArchive createDeployment() {
        return createWebArchive(SseIT.class);
    }

    @Test
    void test() {
        testOnopen();

        assertEquals(pushApplicationScopedEvent(), "1," + applicationScopedEventMessage.getText());
        assertEquals(pushSessionScopedUserTargeted(), "1," + sessionScopedUserTargetedMessage.getText());
        assertEquals(pushViewScopedAjaxAware(), "1," + viewScopedAjaxAwareMessage.getText());

        assertEquals(pushApplicationScopedEvent(), "1," + applicationScopedEventMessage.getText());
        assertEquals(pushSessionScopedUserTargeted(), "1," + sessionScopedUserTargetedMessage.getText());
        assertEquals(pushViewScopedAjaxAware(), "1," + viewScopedAjaxAwareMessage.getText());

        // Multi-tab testing is not possible with SSE over HTTP/1.1 because browsers limit concurrent connections to
        // 6 per origin. With 3 SSE channels on tab 1 and 3 on tab 2, all 6 slots are taken and AJAX requests get
        // queued indefinitely. This is a well-known SSE limitation that doesn't apply to WebSocket (which upgrades
        // the protocol). Multi-tab scoping is already tested by SocketIT. With HTTP/2 this limit doesn't apply
        // because SSE streams are multiplexed over a single TCP connection.

        testOnclose();
    }

    private void testOnopen() {
        waitUntilTextContains(clientOpenedMessages, "|applicationScopedEvent|");
        waitUntilTextContains(clientOpenedMessages, "|sessionScopedUserTargeted|");
        waitUntilTextContains(clientOpenedMessages, "|viewScopedAjaxAware|");
    }

    private void testOnclose() {
        closeAllSse.click();
        waitUntilTextContains(clientClosedMessages, "|sessionScopedUserTargeted|");
        waitUntilTextContains(clientClosedMessages, "|viewScopedAjaxAware|");
    }

    private String pushApplicationScopedEvent() {
        guardAjax(pushApplicationScopedEvent::click);
        String message = messages.getText();
        waitUntilTextContent(applicationScopedEventMessage);
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
