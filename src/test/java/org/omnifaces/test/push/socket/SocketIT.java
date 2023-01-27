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
package org.omnifaces.test.push.socket;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.jboss.arquillian.graphene.Graphene.guardNoRequest;
import static org.jboss.arquillian.graphene.Graphene.waitGui;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omnifaces.test.OmniFacesIT.WebXml.withSocket;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

@DisabledIfSystemProperty(named = "arquillian.browser", matches = "phantomjs", disabledReason = "Web sockets don't seem to work in Selenium+PhantomJS")
public class SocketIT extends OmniFacesIT {

	@FindBy(id="messages")
	private WebElement messages;

	@FindBy(id="newtab")
	private WebElement newtab;

	@FindBy(id="clientOpenedMessages")
	private WebElement clientOpenedMessages;

	@FindBy(id="applicationScopedServerEventMessage")
	private WebElement applicationScopedServerEventMessage;

	@FindBy(id="push:applicationScopedServerEvent")
	private WebElement pushApplicationScopedServerEvent;

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

	@FindBy(id="closeAllSockets")
	private WebElement closeAllSockets;

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return buildWebArchive(SocketIT.class)
				.withWebXml(withSocket)
				.createDeployment();
	}

	@Test
	public void test() throws Exception {
		testOnopen();

		assertEquals(pushApplicationScopedServerEvent(), "1," + applicationScopedServerEventMessage.getText());
		assertEquals(pushSessionScopedUserTargeted(), "1," + sessionScopedUserTargetedMessage.getText());
		assertEquals(pushViewScopedAjaxAware(), "1," + viewScopedAjaxAwareMessage.getText());

		String firstTab = browser.getWindowHandle();
		openNewTab(newtab);
		testOnopen();

		assertEquals(pushApplicationScopedServerEvent(), "2," + applicationScopedServerEventMessage.getText());
		assertEquals(pushSessionScopedUserTargeted(), "2," + sessionScopedUserTargetedMessage.getText());
		assertEquals(pushViewScopedAjaxAware(), "1," + viewScopedAjaxAwareMessage.getText());

		// Unfortunately Selenium doesn't (seem to?) support starting a new HTTP session within the same IT, so
		// application, session and user sockets can't be tested more extensively. If possible somehow, it's expected
		// that numbers should equal respectively 3, 2, 1 on first session and 3, 1, 1 on second session.

		testOnclose(firstTab);
	}

	private void testOnopen() {
		assertTrue(clientOpenedMessages.getText().contains("|applicationScopedServerEvent|"), "applicationScopedServerEvent socket is opened");
		assertTrue(clientOpenedMessages.getText().contains("|sessionScopedUserTargeted|"), "sessionScopedUserTargeted socket is opened");
		assertTrue(clientOpenedMessages.getText().contains("|viewScopedAjaxAware|"), "viewScopedAjaxAware socket is opened");
		waitGui(browser).withTimeout(3, SECONDS).until().element(applicationScopedServerEventMessage).text().contains("|opened:sessionScopedUserTargeted|"); // These are async.
		waitGui(browser).withTimeout(3, SECONDS).until().element(applicationScopedServerEventMessage).text().contains("|opened:viewScopedAjaxAware|");
	}

	private void testOnclose(String tabToSwitch) {
		guardNoRequest(closeAllSockets).click();
		waitGui(browser).withTimeout(3, SECONDS).until().element(clientClosedMessages).text().contains("|sessionScopedUserTargeted|"); // These are async.
		waitGui(browser).withTimeout(3, SECONDS).until().element(clientClosedMessages).text().contains("|viewScopedAjaxAware|");
		closeCurrentTabAndSwitchTo(tabToSwitch);
	}

	private String pushApplicationScopedServerEvent() {
		guardAjax(pushApplicationScopedServerEvent).click();
		String message = messages.getText();
		waitUntilTextContent(applicationScopedServerEventMessage);
		return message;
	}

	private String pushSessionScopedUserTargeted() {
		guardAjax(pushSessionScopedUserTargeted).click();
		String message = messages.getText();
		waitUntilTextContent(sessionScopedUserTargetedMessage);
		return message;
	}

	private String pushViewScopedAjaxAware() {
		guardAjax(pushViewScopedAjaxAware).click();
		String message = messages.getText();
		waitUntilTextContent(viewScopedAjaxAwareMessage);
		return message;
	}

}