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
package org.omnifaces.test.cdi.viewscoped.truncatedunload;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * An unload request sent via keepalive fetch or synchronous XHR carries its event, view scope ID and view state in
 * request headers, next to the same values in the request body, as long as the view state is short enough for a request
 * header. The client script is verified against stubbed fetch and XHR. The server receives the headers even when the
 * client aborts while the body is still in transit, and must then unload the view from the headers alone, without
 * touching the request body. The abort is simulated via a raw socket.
 */
public class ViewScopedTruncatedUnloadIT extends OmniFacesIT {

    private static final String HEADER_EVENT = "OmniFaces-Event";
    private static final String HEADER_VIEW_SCOPE = "OmniFaces-View-Scope";
    private static final String HEADER_VIEW_STATE = "OmniFaces-View-State";

    private static final int RESPONSE_TIMEOUT_MILLIS = (int) TimeUnit.SECONDS.toMillis(10);
    private static final int TOO_LONG_VIEW_STATE_LENGTH = 1025;

    private static final String SCRIPT_STUB_KEEPALIVE_FETCH = "window.Request = function() {};"
        + " Request.prototype.keepalive = false;"
        + " window.unloadFetch = null;"
        + " window.fetch = function(url, init) { window.unloadFetch = init; return { 'catch': function() {} }; };";

    private static final String SCRIPT_STUB_XHR = "window.Request = undefined;"
        + " Object.defineProperty(navigator, 'sendBeacon', { value: undefined });"
        + " window.unloadXhr = null;"
        + " window.XMLHttpRequest = function() { this.headers = {}; };"
        + " XMLHttpRequest.prototype.open = function(method) { this.method = method; };"
        + " XMLHttpRequest.prototype.setRequestHeader = function(name, value) { this.headers[name] = value; };"
        + " XMLHttpRequest.prototype.send = function() { window.unloadXhr = this; };";

    private static final String SCRIPT_FIRE_UNLOAD = "['beforeunload', 'pagehide', 'unload'].forEach(function(type) {"
        + " var event = document.createEvent('Event'); event.initEvent(type, true, true); window.dispatchEvent(event); });";

    @FindBy(id="bean")
    private WebElement bean;

    @FindBy(id="destroyed")
    private WebElement destroyed;

    @FindBy(id="form")
    private WebElement form;

    @FindBy(css="#form > [name='jakarta.faces.ViewState']")
    private WebElement viewState;

    @Deployment(testable=false)
    public static WebArchive createDeployment() {
        return createWebArchive(ViewScopedTruncatedUnloadIT.class);
    }

    @Test
    public void unloadWithCompleteBody() throws IOException {
        assertUnloaded(false, 1.0);
    }

    @Test
    public void unloadWithHeadersAndTruncatedBody() throws IOException {
        assertUnloaded(true, 0.5);
    }

    @Test
    public void unloadWithHeadersAndWithoutBody() throws IOException {
        assertUnloaded(true, 0.0);
    }

    @Test
    public void unloadViaKeepaliveFetchWithHeaders() {
        executeScript(SCRIPT_STUB_KEEPALIVE_FETCH);
        assertUnloadRequestWithHeaders("window.unloadFetch");
        assertEquals(true, executeScript("return window.unloadFetch.keepalive"));
    }

    @Test
    public void unloadViaSynchronousXhrWithHeaders() {
        executeScript(SCRIPT_STUB_XHR);
        assertUnloadRequestWithHeaders("window.unloadXhr");
    }

    /**
     * A view state too long for a request header, such as with client side state saving, must not be sent as request
     * header, as it may exceed the header size limit of the server and then cause the whole unload request to fail.
     */
    @Test
    public void unloadWithoutKeepaliveFetchWhenViewStateTooLongForHeader() {
        executeScript("document.querySelector(\"#form > [name='jakarta.faces.ViewState']\").value = new Array(" + (TOO_LONG_VIEW_STATE_LENGTH + 1) + ").join('x')");
        executeScript(SCRIPT_STUB_KEEPALIVE_FETCH);
        executeScript(SCRIPT_FIRE_UNLOAD);

        assertNull(executeScript("return window.unloadFetch"));
    }

    private void assertUnloadRequestWithHeaders(String request) {
        String viewScopeId = getViewScopeId();
        String viewStateValue = viewState.getAttribute("value");
        executeScript(SCRIPT_FIRE_UNLOAD);

        assertAll(
            () -> assertEquals("POST", executeScript("return " + request + ".method")),
            () -> assertEquals("unload", executeScript("return " + request + ".headers['" + HEADER_EVENT + "']")),
            () -> assertEquals(viewScopeId, executeScript("return " + request + ".headers['" + HEADER_VIEW_SCOPE + "']")),
            () -> assertEquals(viewStateValue, executeScript("return " + request + ".headers['" + HEADER_VIEW_STATE + "']"))
        );
    }

    /**
     * The unload of the browser itself is disabled, so that only the simulated unload request can destroy the bean.
     */
    private void assertUnloaded(boolean withHeaders, double sentBodyFraction) throws IOException {
        String beanId = bean.getText();
        executeScript("OmniFaces.Unload.disable()");
        int status = sendUnload(withHeaders, sentBodyFraction);

        refresh();
        assertAll(
            () -> assertEquals(HTTP_NO_CONTENT, status, "unload response status"),
            () -> assertTrue(asList(destroyed.getText().split(" ")).contains(beanId), () -> "bean " + beanId + " destroyed in " + destroyed.getText())
        );
    }

    /**
     * Send the unload request the way the client script does and return the response status, or -1 when the server
     * closed the connection without response.
     */
    private int sendUnload(boolean withHeaders, double sentBodyFraction) throws IOException {
        String viewScopeId = getViewScopeId();
        String viewStateValue = viewState.getAttribute("value");
        URI action = URI.create(form.getAttribute("action"));
        byte[] body = ("omnifaces.event=unload&id=" + viewScopeId + "&jakarta.faces.ViewState=" + URLEncoder.encode(viewStateValue, UTF_8.name())).getBytes(UTF_8);
        String cookies = getCookies();
        int port = baseURL.getPort() != -1 ? baseURL.getPort() : baseURL.getDefaultPort();

        StringBuilder head = new StringBuilder()
            .append("POST ").append(action.getRawPath()).append(action.getRawQuery() != null ? "?" + action.getRawQuery() : "").append(" HTTP/1.1\r\n")
            .append("Host: ").append(baseURL.getHost()).append(":").append(port).append("\r\n")
            .append("Content-Type: application/x-www-form-urlencoded\r\n")
            .append("Content-Length: ").append(body.length).append("\r\n")
            .append("Cookie: ").append(cookies).append("\r\n");

        if (withHeaders) {
            head.append(HEADER_EVENT).append(": unload\r\n")
                .append(HEADER_VIEW_SCOPE).append(": ").append(viewScopeId).append("\r\n")
                .append(HEADER_VIEW_STATE).append(": ").append(viewStateValue).append("\r\n");
        }

        head.append("Connection: close\r\n\r\n");

        try (Socket socket = new Socket(baseURL.getHost(), port)) {
            socket.setSoTimeout(RESPONSE_TIMEOUT_MILLIS);
            OutputStream output = socket.getOutputStream();
            output.write(head.toString().getBytes(ISO_8859_1));
            output.write(body, 0, (int) (body.length * sentBodyFraction));
            output.flush();
            socket.shutdownOutput();

            String statusLine = new BufferedReader(new InputStreamReader(socket.getInputStream(), ISO_8859_1)).readLine();
            return statusLine == null ? -1 : Integer.parseInt(statusLine.split(" ")[1]);
        }
    }

}
