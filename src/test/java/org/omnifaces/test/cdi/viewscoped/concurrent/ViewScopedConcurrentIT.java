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
package org.omnifaces.test.cdi.viewscoped.concurrent;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omnifaces.test.OmniFacesIT.WebXml.withThreeActiveViewScopes;
import static org.omnifaces.test.cdi.viewscoped.concurrent.ViewScopedConcurrentITLatch.CONCURRENT_REQUESTS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;

/**
 * Concurrent GET requests within the same session must each render their own view scoped bean, even when there are more of them in flight than the configured
 * maximum active view scopes of 3.
 */
public class ViewScopedConcurrentIT extends OmniFacesIT {

    private static final String PAGE = "ViewScopedConcurrentIT.xhtml";
    private static final String PARAM_VALUE = "foo";
    private static final String EXPECTED_OUTPUT = "<span id=\"param\">" + PARAM_VALUE + "</span>";
    private static final int TIMEOUT_IN_MILLIS = 60000;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return buildWebArchive(ViewScopedConcurrentIT.class)
            .withWebXml(withThreeActiveViewScopes)
            .createDeployment();
    }

    @Override
    public void init() {
        // Deliberately not opening the page in the browser; this test drives raw concurrent HTTP requests instead.
    }

    @Test
    public void concurrentRequestsMustNotEvictEachOthersViewScope() throws Exception {
        var jsessionidCookie = createSession();
        var executor = newFixedThreadPool(CONCURRENT_REQUESTS);
        List<Future<String>> responses = new ArrayList<>();

        try {
            for (var i = 0; i < CONCURRENT_REQUESTS; i++) {
                responses.add(executor.submit(() -> getResponseBody(PAGE + "?param=" + PARAM_VALUE, jsessionidCookie)));
            }

            for (var response : responses) {
                assertTrue(response.get().contains(EXPECTED_OUTPUT), "View scoped bean of concurrent request must still hold the view param");
            }
        }
        finally {
            executor.shutdownNow();
        }
    }

    /**
     * Fires the very first request, which is the one creating the session, and returns its JSESSIONID cookie. It deliberately doesn't pass the view param, as
     * the view scoped bean is instructed to only await the other requests when the view param is present.
     */
    private String createSession() throws IOException {
        var connection = openConnection(PAGE, null);
        readResponseBody(connection);
        var cookies = connection.getHeaderFields().get("Set-Cookie");

        if (cookies != null) {
            for (var cookie : cookies) {
                if (cookie.startsWith("JSESSIONID=")) {
                    return cookie.split(";", 2)[0];
                }
            }
        }

        throw new IllegalStateException("Cannot find JSESSIONID cookie among " + cookies);
    }

    private String getResponseBody(String path, String jsessionidCookie) throws IOException {
        return readResponseBody(openConnection(path, jsessionidCookie));
    }

    private HttpURLConnection openConnection(String path, String jsessionidCookie) throws IOException {
        var connection = (HttpURLConnection) new URL(baseURL + path).openConnection();
        connection.setConnectTimeout(TIMEOUT_IN_MILLIS);
        connection.setReadTimeout(TIMEOUT_IN_MILLIS);
        connection.setRequestProperty("Connection", "close"); // Ensure that every request gets its own socket.

        if (jsessionidCookie != null) {
            connection.setRequestProperty("Cookie", jsessionidCookie);
        }

        return connection;
    }

    private static String readResponseBody(HttpURLConnection connection) throws IOException {
        try (var reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), UTF_8))) {
            return reader.lines().collect(joining("\n"));
        }
    }

}
