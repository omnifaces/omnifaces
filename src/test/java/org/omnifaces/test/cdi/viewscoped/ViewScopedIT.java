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
package org.omnifaces.test.cdi.viewscoped;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.omnifaces.test.OmniFacesIT.WebXml.withThreeViewsInSession;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * The unload is fired during <code>pagehide</code>, which takes place after the browser has already issued the request of the page being navigated to. Whether
 * the unload request is processed before or after that request depends on whether the container serializes concurrent requests on the same session. The request
 * during which an <code>unload</code> or <code>destroy</code> message is observed is therefore not fixed. The assertions on an individual response are hence
 * limited to the event of the request itself, which is always the last one, and the unloads and destroys are asserted as totals at the end of the test.
 */
@TestMethodOrder(OrderAnnotation.class)
public class ViewScopedIT extends OmniFacesIT {

    @FindBy(id = "bean")
    private WebElement bean;

    @FindBy(id = "messages")
    private WebElement messages;

    @FindBy(id = "unload")
    private WebElement unload;

    @FindBy(id = "newtab")
    private WebElement newtab;

    @FindBy(id = "non-ajax:submit")
    private WebElement nonAjaxSubmit;

    @FindBy(id = "non-ajax:navigate")
    private WebElement nonAjaxNavigate;

    @FindBy(id = "ajax:submit")
    private WebElement ajaxSubmit;

    @FindBy(id = "ajax:navigate")
    private WebElement ajaxNavigate;

    @FindBy(id = "form:conditionallyRenderViewScopedIT")
    private WebElement conditionallyRenderViewScopedIT;

    private String previousBean;

    private final List<String> observedMessages = new ArrayList<>();

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return buildWebArchive(ViewScopedIT.class)
            .withWebXml(withThreeViewsInSession)
            .createDeployment();
    }

    @BeforeEach
    void resetBrowser() {
        // Make sure browser is crisp clean before starting each test.
        teardown();
        setup();
        observedMessages.clear(); // Arquillian reuses the test instance across test methods.
    }

    @Test
    void nonAjax() {
        init();
        observeMessages("init");
        previousBean = bean.getText();

        // Unload.
        guardHttp(unload::click);
        assertBeanRecreated();
        observeMessages("init");

        // Submit then unload.
        guardHttp(nonAjaxSubmit::click);
        assertBeanRetained();
        observeMessages("submit");

        guardHttp(unload::click);
        assertBeanRecreated();
        observeMessages("init");

        // Navigate then unload.
        guardHttp(nonAjaxNavigate::click);
        assertBeanRecreated();
        observeMessages("init");

        guardHttp(unload::click);
        assertBeanRecreated();
        observeMessages("init");

        // Submit then navigate then unload.
        guardHttp(nonAjaxSubmit::click);
        assertBeanRetained();
        observeMessages("submit");

        guardHttp(nonAjaxNavigate::click);
        assertBeanRecreated();
        observeMessages("init");

        guardHttp(unload::click);
        assertBeanRecreated();
        observeMessages("init");

        // Navigate then submit then unload.
        guardHttp(nonAjaxNavigate::click);
        assertBeanRecreated();
        observeMessages("init");

        guardHttp(nonAjaxSubmit::click);
        assertBeanRetained();
        observeMessages("submit");

        guardHttp(unload::click);
        assertBeanRecreated();
        observeMessages("init");

        // Submit once more so that the unload of the last unload is observed as well.
        guardHttp(nonAjaxSubmit::click);
        assertBeanRetained();
        observeMessages("submit");

        assertObservedEvents(9, 4, 3, 5, 3);
    }

    @Test
    void ajax() {
        init();
        observeMessages("init");
        previousBean = bean.getText();

        // Submit then unload.
        guardAjax(ajaxSubmit::click);
        assertBeanRetained();
        observeMessages("submit");

        guardHttp(unload::click);
        assertBeanRecreated();
        observeMessages("init");

        // Navigate then unload.
        guardAjax(ajaxNavigate::click);
        assertBeanRecreated();
        observeMessages("init");

        guardHttp(unload::click);
        assertBeanRecreated();
        observeMessages("init");

        // Submit then navigate then unload.
        guardAjax(ajaxSubmit::click);
        assertBeanRetained();
        observeMessages("submit");

        guardAjax(ajaxNavigate::click);
        assertBeanRecreated();
        observeMessages("init");

        guardHttp(unload::click);
        assertBeanRecreated();
        observeMessages("init");

        // Navigate then submit then unload.
        guardAjax(ajaxNavigate::click);
        assertBeanRecreated();
        observeMessages("init");

        guardAjax(ajaxSubmit::click);
        assertBeanRetained();
        observeMessages("submit");

        guardHttp(unload::click);
        assertBeanRecreated();
        observeMessages("init");

        // Submit once more so that the unload of the last unload is observed as well.
        guardAjax(ajaxSubmit::click);
        assertBeanRetained();
        observeMessages("submit");

        assertObservedEvents(8, 4, 3, 4, 3);
    }

    /**
     * The unload is hooked on <code>pagehide</code>, which is fired only once the navigation is actually committed, so a <code>beforeunload</code> event on its
     * own must keep the view scoped bean alive. This is what allows an enduser to cancel a custom confirmation and stay in the same page. A custom handler may
     * also be assigned to the <code>window.onbeforeunload</code> property without suppressing the unload.
     */
    @Test
    void beforeUnload() {
        init();
        observeMessages("init");
        previousBean = bean.getText();

        executeScript("window.dispatchEvent(new Event('beforeunload'))");
        guardAjax(ajaxSubmit::click);
        assertBeanRetained();
        observeMessages("submit");

        executeScript("window.onbeforeunload = function() {}");
        guardHttp(unload::click);
        assertBeanRecreated();
        observeMessages("init");

        // Submit once more so that the unload of the unload is observed as well.
        guardAjax(ajaxSubmit::click);
        assertBeanRetained();
        observeMessages("submit");

        assertObservedEvents(2, 2, 0, 1, 0);
    }

    @Test
    void conditionallyRenderViewScopedIT() {
        open("ViewScopedITWithConditionalRendering.xhtml");
        assertEquals("", getMessagesText());

        // Trigger view scoped creation during conditional render.
        guardAjax(conditionallyRenderViewScopedIT::click);
        previousBean = bean.getText();
        observeMessages("init");

        // Submit then unload.
        guardAjax(ajaxSubmit::click);
        assertBeanRetained();
        observeMessages("submit");

        guardHttp(unload::click);
        assertBeanRecreated();
        observeMessages("init");

        // Submit once more so that the unload of the unload is observed as well.
        guardAjax(ajaxSubmit::click);
        assertBeanRetained();
        observeMessages("submit");

        assertObservedEvents(2, 2, 0, 1, 0);
    }

    @Test
    void destroyViewStateWithXhtmlMapping() {
        destroyViewState("ViewScopedIT.xhtml");
    }

    @Test
    void destroyViewStateWithJsfMapping() {
        destroyViewState("ViewScopedIT.jsf");
    }

    @Test
    void destroyViewStateWithViewAction() {
        destroyViewState("ViewScopedITWithViewAction.xhtml");
    }

    private void destroyViewState(String pageName) {
        open(pageName);
        assertEquals("init", getMessagesText());
        var firstBean = bean.getText();
        var firstTab = browser.getWindowHandle();

        // Open three new tabs and close them immediately.
        openNewTab(newtab);
        assertEquals("init", getMessagesText());
        assertNotEquals(firstBean, bean.getText());
        closeCurrentTabAndSwitchTo(firstTab);

        openNewTab(newtab);
        assertEquals("unload init", getMessagesText()); // Unload was from previous tab.
        assertNotEquals(firstBean, bean.getText());
        closeCurrentTabAndSwitchTo(firstTab);

        openNewTab(newtab);
        assertEquals("unload init", getMessagesText()); // Unload was from previous tab.
        assertNotEquals(firstBean, bean.getText());
        closeCurrentTabAndSwitchTo(firstTab);

        // Submit form in first tab. As Faces is instructed to store only 3 views in session,
        // and the @ViewScoped unload in three previously opened tabs should also physically
        // destroy the view state, the submit in first tab should not throw ViewExpiredException.
        guardAjax(ajaxSubmit::click);
        assertEquals(firstBean, bean.getText());
        assertEquals("unload submit", getMessagesText()); // Unload was from previous tab.
    }

    private String getMessagesText() {
        return messages.getText().replaceAll("\\s+", " ");
    }

    private void observeMessages(String expectedLastEvent) {
        var messagesText = getMessagesText();
        observedMessages.add(messagesText);
        var events = messagesText.split(" ");
        assertEquals(expectedLastEvent, events[events.length - 1]);
    }

    private void assertObservedEvents(int init, int submit, int navigate, int unload, int destroy) {
        assertEquals(init, countObservedEvents("init"), () -> "init in " + observedMessages);
        assertEquals(submit, countObservedEvents("submit"), () -> "submit in " + observedMessages);
        assertEquals(navigate, countObservedEvents("navigate"), () -> "navigate in " + observedMessages);
        assertEquals(unload, countObservedEvents("unload"), () -> "unload in " + observedMessages);
        assertEquals(destroy, countObservedEvents("destroy"), () -> "destroy in " + observedMessages);
    }

    private long countObservedEvents(String event) {
        return observedMessages.stream().flatMap(messagesText -> Stream.of(messagesText.split(" "))).filter(event::equals).count();
    }

    private void assertBeanRetained() {
        assertEquals(previousBean, previousBean = bean.getText());
    }

    private void assertBeanRecreated() {
        assertNotEquals(previousBean, previousBean = bean.getText());
    }

}
