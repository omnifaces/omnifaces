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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.omnifaces.test.OmniFacesIT;
import org.omnifaces.test.push.PushTestUtil;

/**
 * Verifies that a session scoped SSE channel can only be connected to by the HTTP session which registered it, while an application scoped channel remains
 * connectable by any (or no) HTTP session. Performs the connection as a raw HTTP request so that the connecting HTTP session (i.e. the presence or absence of
 * the <code>JSESSIONID</code> cookie) is fully controllable, which a browser driven test cannot do.
 */
@DisabledIfSystemProperty(
    named = "profile.id", matches = "quarkus-.*", disabledReason = "quarkus-omnifaces must first be upgraded to a version which activates @Push injection points at build time"
)
public class SseSessionBindingIT extends OmniFacesIT {

    private static final int NOT_FOUND = 404;
    private static final int READ_TIMEOUT = 3000;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return createWebArchive(SseSessionBindingIT.class);
    }

    @Test
    public void test() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(baseURL + "SseSessionBindingIT.xhtml").openConnection();
        connection.setRequestProperty("Accept-Encoding", "identity");
        String sessionCookie = PushTestUtil.getSessionCookie(connection);
        String page = PushTestUtil.getResponseBody(connection);
        String sessionScopedChannelId = PushTestUtil.getChannelId(page, "sessionScoped");
        String applicationScopedChannelId = PushTestUtil.getChannelId(page, "applicationScoped");

        // A session scoped channel may only be connected to by the owning HTTP session.
        assertNotEquals(NOT_FOUND, connect(sessionScopedChannelId, sessionCookie), "Owning session may connect to session scoped channel");
        assertEquals(NOT_FOUND, connect(sessionScopedChannelId, null), "Foreign session may not connect to session scoped channel");

        // An application scoped channel is by design not bound to any HTTP session.
        assertNotEquals(NOT_FOUND, connect(applicationScopedChannelId, null), "Any session may connect to application scoped channel");
    }

    private int connect(String channelId, String sessionCookie) throws IOException {
        try (Socket socket = SseTestUtil.connect(baseURL, channelId, sessionCookie, READ_TIMEOUT)) {
            return SseTestUtil.readCloseCode(socket);
        }
    }

}
