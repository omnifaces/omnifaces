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
package org.omnifaces.test.cdi.viewscoped.viewstate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.omnifaces.test.OmniFacesIT.WebXml.withClientStateSaving;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

@TestMethodOrder(OrderAnnotation.class)
public class ViewScopedViewStateIT extends OmniFacesIT {

    @FindBy(id="bean")
    private WebElement bean;

    @FindBy(id="messages")
    private WebElement messages;

    @FindBy(id="unload")
    private WebElement unload;

    @FindBy(id="newtab")
    private WebElement newtab;

    @FindBy(id="non-ajax:submit")
    private WebElement nonAjaxSubmit;

    @FindBy(id="non-ajax:navigate")
    private WebElement nonAjaxNavigate;

    @FindBy(id="ajax:submit")
    private WebElement ajaxSubmit;

    @FindBy(id="ajax:navigate")
    private WebElement ajaxNavigate;

    @FindBy(css="#non-ajax > [name='jakarta.faces.ViewState']")
    private WebElement nonAjaxViewState;

    @FindBy(css="#ajax > [name='jakarta.faces.ViewState']")
    private WebElement ajaxViewState;

    @Deployment(testable=false)
    public static WebArchive createDeployment() {
        return buildWebArchive(ViewScopedViewStateIT.class)
            .withWebXml(withClientStateSaving)
            .createDeployment();
    }

    @BeforeEach
    void resetBrowser() {
        // Make sure browser is crisp clean before starting each test.
        teardown();
        setup();
    }

    @Test
    void nonAjax() {
        init();
        assertEquals("init", getMessagesText());
        var previousBean = bean.getText();


        // Unload.
        guardHttp(unload::click);
        assertNotEquals(previousBean, previousBean = bean.getText());
        assertEquals("init", getMessagesText());


        // Submit then unload.
        guardHttp(nonAjaxSubmit::click);
        assertEquals(previousBean, previousBean = bean.getText());
        assertEquals("submit", getMessagesText());

        guardHttp(unload::click);
        assertNotEquals(previousBean, previousBean = bean.getText());
        assertEquals("init", getMessagesText());


        // Navigate then unload.
        guardHttp(nonAjaxNavigate::click);
        assertNotEquals(previousBean, previousBean = bean.getText());
        assertEquals("navigate init", getMessagesText());

        guardHttp(unload::click);
        assertNotEquals(previousBean, previousBean = bean.getText());
        assertEquals("init", getMessagesText());


        // Submit then navigate then unload.
        guardHttp(nonAjaxSubmit::click);
        assertEquals(previousBean, previousBean = bean.getText());
        assertEquals("submit", getMessagesText());

        guardHttp(nonAjaxNavigate::click);
        assertNotEquals(previousBean, previousBean = bean.getText());
        assertEquals("navigate init", getMessagesText());

        guardHttp(unload::click);
        assertNotEquals(previousBean, previousBean = bean.getText());
        assertEquals("init", getMessagesText());


        // Navigate then submit then unload.
        guardHttp(nonAjaxNavigate::click);
        assertNotEquals(previousBean, previousBean = bean.getText());
        assertEquals("navigate init", getMessagesText());

        guardHttp(nonAjaxSubmit::click);
        assertEquals(previousBean, previousBean = bean.getText());
        assertEquals("submit", getMessagesText());

        guardHttp(unload::click);
        assertNotEquals(previousBean, previousBean = bean.getText());
        assertEquals("init", getMessagesText());
    }

    @Test
    void ajax() {
        init();
        assertEquals("init", getMessagesText());
        var previousBean = bean.getText();


        // Submit then unload.
        guardAjax(ajaxSubmit::click);
        assertEquals(previousBean, previousBean = bean.getText());
        assertEquals("submit", getMessagesText());

        guardHttp(unload::click);
        assertNotEquals(previousBean, previousBean = bean.getText());
        assertEquals("init", getMessagesText());


        // Navigate then unload.
        guardAjax(ajaxNavigate::click);
        assertNotEquals(previousBean, previousBean = bean.getText());
        assertEquals("navigate init", getMessagesText());

        guardHttp(unload::click);
        assertNotEquals(previousBean, previousBean = bean.getText());
        assertEquals("init", getMessagesText());


        // Submit then navigate then unload.
        guardAjax(ajaxSubmit::click);
        assertEquals(previousBean, previousBean = bean.getText());
        assertEquals("submit", getMessagesText());

        guardAjax(ajaxNavigate::click);
        assertNotEquals(previousBean, previousBean = bean.getText());
        assertEquals("navigate init", getMessagesText());

        guardHttp(unload::click);
        assertNotEquals(previousBean, previousBean = bean.getText());
        assertEquals("init", getMessagesText());


        // Navigate then submit then unload.
        guardAjax(ajaxNavigate::click);
        assertNotEquals(previousBean, previousBean = bean.getText());
        assertEquals("navigate init", getMessagesText());

        guardAjax(ajaxSubmit::click);
        assertEquals(previousBean, previousBean = bean.getText());
        assertEquals("submit", getMessagesText());

        guardHttp(unload::click);
        assertNotEquals(previousBean, previousBean = bean.getText());
        assertEquals("init", getMessagesText());
    }

    @Test
    void copyViewState() {
        init();
        assertEquals("init", getMessagesText());
        var firstBean = bean.getText();
        String firstViewState = ajaxViewState.getAttribute("value");
        var firstTab = browser.getWindowHandle();

        // Open new tab, copy view state from first tab into second tab and re-execute via ajax.
        openNewTab(newtab);
        var secondBean = bean.getText();
        String secondViewState = nonAjaxViewState.getAttribute("value");
        assertEquals("init", getMessagesText());
        assertNotEquals(secondBean, firstBean);
        assertNotEquals(secondViewState, firstViewState);

        executeScript("document.querySelectorAll(\"#ajax > [name='jakarta.faces.ViewState']\")[0].value='" + firstViewState + "'");
        guardAjax(ajaxSubmit::click);
        assertEquals(firstBean, bean.getText());
        assertEquals("submit", getMessagesText());


        // Close second tab, copy view state from second tab into first tab and re-execute via non-ajax.
        closeCurrentTabAndSwitchTo(firstTab);
        executeScript("document.querySelectorAll(\"#non-ajax > [name='jakarta.faces.ViewState']\")[0].value='" + secondViewState + "'");
        guardHttp(nonAjaxSubmit::click);
        assertEquals(secondBean, bean.getText());
        assertEquals("submit", getMessagesText());
    }

    private String getMessagesText() {
        return messages.getText().replaceAll("\\s+", " ");
    }

}