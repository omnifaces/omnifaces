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
package org.omnifaces.test.cdi.viewscoped.unrendered;

import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omnifaces.cdi.viewscope.ViewScopeManager.DEFAULT_MAX_ACTIVE_VIEW_SCOPES;
import static org.omnifaces.test.OmniFacesIT.FacesConfig.withFullAjaxExceptionHandler;
import static org.omnifaces.test.OmniFacesIT.WebXml.withErrorPage;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * A request which creates a view scoped bean, but completes without rendering its view, never sends the unload script to the client, so no unload request can
 * ever destroy that bean. Its beans must therefore be destroyed at the end of that request, and it must not take a place in the active view scopes of
 * OmniFaces, nor create a view scope in the Faces implementation. A view scoped bean of a view which is rendered must stay.
 */
public class ViewScopedUnrenderedIT extends OmniFacesIT {

    private static final long AWAIT_TIMEOUT_IN_MILLIS = 5000;
    private static final long AWAIT_INTERVAL_IN_MILLIS = 50;

    /** The hidden form fields carry the view state and the implementation specific marker of the submitted form. */
    private static final String SCRIPT_SERIALIZE_HIDDEN_FORM_FIELDS = "return Array.prototype.map.call(document.querySelectorAll('#form input[type=hidden]'),"
        + " function(input) { return encodeURIComponent(input.name) + '=' + encodeURIComponent(input.value); }).join('&');";

    @FindBy(id = "form:bean")
    private WebElement bean;

    @FindBy(id = "form:count")
    private WebElement count;

    @FindBy(id = "form:submit")
    private WebElement submit;

    @FindBy(id = "form:nonAjaxSubmit")
    private WebElement nonAjaxSubmit;

    @FindBy(id = "afterForm")
    private WebElement afterForm;

    @FindBy(id = "form:increment")
    private WebElement increment;

    @FindBy(id = "form:navigate")
    private WebElement navigate;

    @FindBy(id = "form:fail")
    private WebElement fail;

    @FindBy(id = "form:failRender")
    private WebElement failRender;

    @FindBy(id = "form:show")
    private WebElement show;

    @FindBy(tagName = "body")
    private WebElement body;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return buildWebArchive(ViewScopedUnrenderedIT.class)
            .withFacesConfig(withFullAjaxExceptionHandler)
            .withWebXml(withErrorPage)
            .createDeployment();
    }

    @Test
    public void unrenderedViewsMustNotEvictOpenView() throws IOException {
        String beanId = bean.getText();
        Map<String, Integer> before = getCounts();

        for (int i = 0; i <= DEFAULT_MAX_ACTIVE_VIEW_SCOPES; i++) {
            assertEquals(HTTP_MOVED_TEMP, get("ViewScopedUnrenderedITRedirect.xhtml").getResponseCode(), "redirect response status");
        }

        Map<String, Integer> after = awaitDelta(before, "destroyed.viewAction", DEFAULT_MAX_ACTIVE_VIEW_SCOPES + 1);
        assertDelta(before, after, "activeViewScopes", 0);
        assertNoIncrease(before, after, "viewMaps");
        assertDelta(before, after, "created.viewAction", DEFAULT_MAX_ACTIVE_VIEW_SCOPES + 1);
        assertDelta(before, after, "destroyedWithoutFacesContext.viewAction", 0);

        guardAjax(submit::click);
        assertEquals(beanId, bean.getText(), "bean of open view");
    }

    @Test
    public void preDestroyOfUnrenderedViewMustReachSiblingWithoutCreatingNewOne() throws IOException {
        Map<String, Integer> before = getCounts();
        assertEquals(HTTP_MOVED_TEMP, get("ViewScopedUnrenderedITRedirect.xhtml").getResponseCode(), "redirect response status");
        Map<String, Integer> after = awaitDelta(before, "destroyed.sibling", 1);
        assertDelta(before, after, "created.sibling", 1);
        assertDelta(before, after, "destroyedWithoutFacesContext.sibling", 0);
    }

    @Test
    public void redirectDuringPreRenderViewMustDestroyBean() throws IOException {
        Map<String, Integer> before = getCounts();
        assertEquals(HTTP_MOVED_TEMP, get("ViewScopedUnrenderedITPreRenderViewRedirect.xhtml").getResponseCode(), "redirect response status");
        Map<String, Integer> after = awaitDelta(before, "destroyed.preRenderView", 1);
        assertDelta(before, after, "activeViewScopes", 0);
        assertNoIncrease(before, after, "viewMaps");
        assertDelta(before, after, "created.preRenderView", 1);
        assertDelta(before, after, "destroyedWithoutFacesContext.preRenderView", 0);
    }

    @Test
    public void redirectDuringBuildMustDestroyBean() throws IOException {
        Map<String, Integer> before = getCounts();
        assertEquals(HTTP_MOVED_TEMP, get("ViewScopedUnrenderedITBuildRedirect.xhtml").getResponseCode(), "redirect response status");
        Map<String, Integer> after = awaitDelta(before, "destroyed.build", 1);
        assertDelta(before, after, "activeViewScopes", 0);
        assertNoIncrease(before, after, "viewMaps");
        assertDelta(before, after, "created.build", 1);
        assertDelta(before, after, "destroyedWithoutFacesContext.build", 0);
    }

    @Test
    public void renderedViewMustKeepBean() throws IOException {
        Map<String, Integer> before = getCounts();
        assertEquals(HTTP_OK, get("ViewScopedUnrenderedIT.xhtml").getResponseCode(), "response status");
        Map<String, Integer> after = getCounts();
        assertDelta(before, after, "activeViewScopes", 1);
        assertViewMapCreated(before, after);
        assertDelta(before, after, "created.main", 1);
        assertDelta(before, after, "destroyed.main", 0);
    }

    @Test
    public void ajaxPostbackMustKeepBean() throws IOException {
        String beanId = bean.getText();
        Map<String, Integer> before = getCounts();
        guardAjax(submit::click);
        Map<String, Integer> after = getCounts();
        assertEquals(beanId, bean.getText(), "bean of open view");
        assertDelta(before, after, "activeViewScopes", 0);
        assertNoIncrease(before, after, "viewMaps");
        assertDelta(before, after, "created.main", 0);
        assertDelta(before, after, "destroyed.main", 0);
    }

    @Test
    public void downloadAfterRenderedPostbackMustKeepBean() throws IOException {
        String beanId = bean.getText();
        guardAjax(increment::click);
        assertEquals("1", count.getText(), "count before download");
        Map<String, Integer> before = getCounts();

        HttpURLConnection download = post(browser.getCurrentUrl(), executeScript(SCRIPT_SERIALIZE_HIDDEN_FORM_FIELDS) + "&form%3Adownload=download");
        assertEquals(HTTP_OK, download.getResponseCode(), "download response status");
        assertTrue(String.valueOf(download.getHeaderField("Content-Disposition")).contains("download.txt"), "download response");

        guardAjax(increment::click);
        assertEquals(beanId, bean.getText(), "bean after download");
        assertEquals("2", count.getText(), "count after download");
        Map<String, Integer> after = getCounts();
        assertDelta(before, after, "activeViewScopes", 0);
        assertNoIncrease(before, after, "viewMaps");
        assertDelta(before, after, "created.main", 0);
        assertDelta(before, after, "destroyed.main", 0);
    }

    @Test
    public void downloadDuringPostbackMustDestroyNewBeanAndKeepViewUsable() throws IOException {
        openPostbackPage();
        Map<String, Integer> before = getCounts();
        HttpURLConnection download = post(browser.getCurrentUrl(), executeScript(SCRIPT_SERIALIZE_HIDDEN_FORM_FIELDS) + "&form%3Adownload=download");
        assertEquals(HTTP_OK, download.getResponseCode(), "download response status");
        assertTrue(String.valueOf(download.getHeaderField("Content-Disposition")).contains("download.txt"), "download response");
        Map<String, Integer> afterDownload = awaitDelta(before, "destroyed.postback", 1);
        assertDelta(before, afterDownload, "activeViewScopes", 0);
        assertNoIncrease(before, afterDownload, "viewMaps");
        assertDelta(before, afterDownload, "created.postback", 1);
        assertDelta(before, afterDownload, "destroyedWithoutFacesContext.postback", 0);

        guardAjax(show::click);
        assertFalse(bean.getText().isEmpty(), "bean rendered after download");
        Map<String, Integer> afterShow = getCounts();
        assertDelta(before, afterShow, "activeViewScopes", 1);
        assertViewMapCreated(before, afterShow);
        assertDelta(before, afterShow, "created.postback", 2);
        assertDelta(before, afterShow, "destroyed.postback", 1);
    }

    @Test
    public void ajaxExceptionDuringPostbackMustDestroyNewBean() throws IOException {
        openPostbackPage();
        Map<String, Integer> before = getCounts();
        guardAjax(fail::click);
        assertTrue(body.getText().contains("Expected exception."), "error page is rendered: " + body.getText());
        Map<String, Integer> after = awaitDelta(before, "destroyed.postback", 1);
        assertDelta(before, after, "activeViewScopes", 1);
        assertViewMapCreated(before, after);
        assertDelta(before, after, "created.postback", 1);
        assertDelta(before, after, "destroyed.postback", 1);
        assertDelta(before, after, "created.errorPage", 1);
        assertDelta(before, after, "destroyed.errorPage", 0);
    }

    @Test
    public void ajaxExceptionDuringRenderMustDestroyNewBean() throws IOException {
        openPostbackPage();
        Map<String, Integer> before = getCounts();
        guardAjax(failRender::click);
        assertTrue(body.getText().contains("Expected exception."), "error page is rendered: " + body.getText());
        Map<String, Integer> after = awaitDelta(before, "destroyed.postback", 1);
        assertDelta(before, after, "activeViewScopes", 1);
        assertDelta(before, after, "created.postback", 1);
        assertDelta(before, after, "destroyed.postback", 1);
        assertDelta(before, after, "created.errorPage", 1);
        assertDelta(before, after, "destroyed.errorPage", 0);
    }

    @Test
    public void forwardNavigationDuringPostbackMustDestroyNewBeanOfLeftView() throws IOException {
        openPostbackPage();
        Map<String, Integer> before = getCounts();
        guardHttp(navigate::click);
        assertFalse(bean.getText().isEmpty(), "navigated view is rendered");
        assertEquals(1, browser.getPageSource().split("OmniFaces\\.Unload\\.init\\(", -1).length - 1, "unload scripts of navigated view");
        Map<String, Integer> after = awaitDelta(before, "destroyed.postback", 1);
        assertDelta(before, after, "activeViewScopes", 1);
        assertViewMapCreated(before, after);
        assertDelta(before, after, "created.postback", 1);
        assertDelta(before, after, "destroyedWithoutFacesContext.postback", 0);
        assertDelta(before, after, "created.main", 1);
        assertDelta(before, after, "destroyed.main", 0);
    }

    /**
     * The navigated view must initialize the unload of its own bean only, and not of the bean of the view which was navigated away from.
     */
    @Test
    public void ajaxNavigationDuringPostbackMustRenderUnloadScriptOfNavigatedViewOnly() throws IOException {
        openPostbackPage();
        Map<String, Integer> before = getCounts();
        HttpURLConnection navigation = post(
            browser.getCurrentUrl(), executeScript(SCRIPT_SERIALIZE_HIDDEN_FORM_FIELDS)
                + "&form%3AnavigateAjax=navigateAjax&jakarta.faces.source=form%3AnavigateAjax&jakarta.faces.partial.ajax=true"
                + "&jakarta.faces.partial.execute=form%3AnavigateAjax&jakarta.faces.behavior.event=action"
        );
        String partialResponse = readResponseBody(navigation);
        assertEquals(1, partialResponse.split("OmniFaces\\.Unload\\.init\\(", -1).length - 1, () -> "unload scripts of navigated view: " + partialResponse);
        Map<String, Integer> after = awaitDelta(before, "destroyed.postback", 1);
        assertDelta(before, after, "created.postback", 1);
        assertDelta(before, after, "destroyedWithoutFacesContext.postback", 0);
        assertDelta(before, after, "created.main", 1);
        assertDelta(before, after, "destroyed.main", 0);
    }

    /**
     * A view scoped bean which is first referenced by a JSTL tag must stay during postbacks. Such a bean is created while the view is being built, so also
     * again while the view is restored.
     */
    @Test
    public void beanCreatedDuringBuildOfRenderedViewMustStay() throws IOException {
        Map<String, Integer> before = getCounts();
        openWithoutUnload("ViewScopedUnrenderedITJstl.xhtml");
        String beanId = bean.getText();
        Map<String, Integer> afterOpen = getCounts();
        assertDelta(before, afterOpen, "activeViewScopes", 1);
        assertDelta(before, afterOpen, "created.jstl", 1);

        guardAjax(submit::click);
        assertEquals(beanId, bean.getText(), "bean after ajax postback");
        guardHttp(nonAjaxSubmit::click);
        assertEquals(beanId, bean.getText(), "bean after non-ajax postback");
        guardAjax(submit::click);
        assertEquals(beanId, bean.getText(), "bean after ajax postback following non-ajax postback");

        Map<String, Integer> after = getCounts();
        assertDelta(before, after, "activeViewScopes", 1);
        assertDelta(before, after, "created.jstl", 1);
        assertDelta(before, after, "destroyed.jstl", 0);
    }

    /**
     * A view scoped bean which is first referenced after the last form of its view must stay during postbacks.
     */
    @Test
    public void beanCreatedAfterFormOfRenderedViewMustStay() throws IOException {
        Map<String, Integer> before = getCounts();
        openWithoutUnload("ViewScopedUnrenderedITAfterForm.xhtml");
        String beanId = afterForm.getText();
        Map<String, Integer> afterOpen = getCounts();
        assertDelta(before, afterOpen, "activeViewScopes", 1);
        assertDelta(before, afterOpen, "created.afterForm", 1);

        guardAjax(submit::click);
        assertEquals(beanId, afterForm.getText(), "bean after ajax postback");
        guardHttp(nonAjaxSubmit::click);
        assertEquals(beanId, afterForm.getText(), "bean after non-ajax postback");

        Map<String, Integer> after = getCounts();
        assertDelta(before, after, "activeViewScopes", 1);
        assertDelta(before, after, "created.afterForm", 1);
        assertDelta(before, after, "destroyed.afterForm", 0);
    }

    /**
     * A view whose rendering fails after its form has written the view state never reaches the client along with its unload script.
     */
    @Test
    public void exceptionDuringRenderAfterFormMustDestroyBean() throws IOException {
        Map<String, Integer> before = getCounts();
        openWithoutUnload("ViewScopedUnrenderedITRenderFailure.xhtml");
        assertTrue(body.getText().contains("Expected exception."), "error page is rendered: " + body.getText());
        Map<String, Integer> after = awaitDelta(before, "destroyed.postback", 1);
        assertDelta(before, after, "activeViewScopes", 1);
        assertDelta(before, after, "created.postback", 1);
        assertDelta(before, after, "destroyedWithoutFacesContext.postback", 0);
        assertDelta(before, after, "created.errorPage", 1);
        assertDelta(before, after, "destroyed.errorPage", 0);
    }

    /**
     * A view without <code>&lt;h:body&gt;</code> can't render the unload script.
     */
    @Test
    public void viewWithoutBodyMustDestroyBean() throws IOException {
        Map<String, Integer> before = getCounts();
        openWithoutUnload("ViewScopedUnrenderedITWithoutBody.xhtml");
        Map<String, Integer> after = awaitDelta(before, "destroyed.main", 1);
        assertDelta(before, after, "activeViewScopes", 0);
        assertDelta(before, after, "created.main", 1);
        assertDelta(before, after, "destroyedWithoutFacesContext.main", 0);
    }

    private void openPostbackPage() {
        openWithoutUnload("ViewScopedUnrenderedITPostback.xhtml");
    }

    private void openWithoutUnload(String pageName) {
        executeScript("OmniFaces.Unload.disable()"); // So that the counts are not affected by an unload request of the page opened by init().
        open(pageName);
    }

    /**
     * A bean storage which is destroyed at the end of a request may be destroyed only after the client has received the response, so the counts are polled.
     */
    private Map<String, Integer> awaitDelta(Map<String, Integer> before, String name, int expectedDelta) throws IOException {
        long timeout = System.currentTimeMillis() + AWAIT_TIMEOUT_IN_MILLIS;
        Map<String, Integer> after = getCounts();

        while (after.get(name) - before.get(name) != expectedDelta && System.currentTimeMillis() < timeout) {
            sleep(AWAIT_INTERVAL_IN_MILLIS);
            after = getCounts();
        }

        assertDelta(before, after, name, expectedDelta);
        return after;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static void assertViewMapCreated(Map<String, Integer> before, Map<String, Integer> after) {
        if (!isLibertyUsed()) { // The probe servlet can't count the view scopes of MyFaces on Liberty.
            assertDelta(before, after, "viewMaps", 1);
        }
    }

    /**
     * Asserts that the count did not grow. A decrease is accepted, because the Faces implementation may destroy view scopes of earlier views during the
     * request.
     */
    private static void assertNoIncrease(Map<String, Integer> before, Map<String, Integer> after, String name) {
        assertTrue(after.get(name) <= before.get(name), () -> name + " before " + before + " after " + after);
    }

    private static void assertDelta(Map<String, Integer> before, Map<String, Integer> after, String name, int expectedDelta) {
        assertEquals(expectedDelta, after.get(name) - before.get(name), () -> name + " before " + before + " after " + after);
    }

    private Map<String, Integer> getCounts() throws IOException {
        Map<String, Integer> counts = new HashMap<>();

        for (String line : readResponseBody(openConnection("probe", getCookies())).split("\n")) {
            String[] nameValue = line.split("=", 2);
            counts.put(nameValue[0], Integer.valueOf(nameValue[1]));
        }

        assertTrue(counts.get("viewMaps") >= 0, () -> "view scopes of Faces implementation are available: " + counts);
        return counts;
    }

    private HttpURLConnection get(String path) throws IOException {
        HttpURLConnection connection = openConnection(path, getCookies());
        connection.setInstanceFollowRedirects(false);
        return connection;
    }

    private HttpURLConnection post(String url, String body) throws IOException {
        HttpURLConnection connection = openConnection(stripJsessionid(url), getCookies());
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setDoOutput(true);

        try (OutputStream output = connection.getOutputStream()) {
            output.write(body.getBytes(UTF_8));
        }

        return connection;
    }

}
