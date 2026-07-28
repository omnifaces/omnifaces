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
 * Verifies that a channel accepts no more concurrent SSE connections than the configured maximum (here 1 via
 * <code>org.omnifaces.SSE_MAX_SESSIONS_PER_CHANNEL</code>). The cap is enforced right after the async context is started, so the second connection still gets
 * the HTTP 200 event stream head but is then immediately closed by the server, whereas the first stays open.
 * <p>
 * The capped channel is declared in <code>SseMaxSessionsChannelIT.xhtml</code> instead of the page that this test opens, because <code>o:sse</code> has no
 * <code>connected</code> attribute and the browser would otherwise occupy the single allowed connection.
 */
@DisabledIfSystemProperty(
    named = "profile.id", matches = "quarkus-.*", disabledReason = "quarkus-omnifaces needs to be upgraded to support PushContextProducer and SseChannelManager.ViewScope"
)
public class SseMaxSessionsIT extends OmniFacesIT {

    private static final int CLOSED_BY_SERVER = 200;
    private static final int STILL_OPEN = -1;
    private static final int READ_TIMEOUT = 2000;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return buildWebArchive(SseMaxSessionsIT.class)
            .withWebXml(WebXml.withSseMaxSessions)
            .createDeployment();
    }

    @Test
    public void test() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(baseURL + "SseMaxSessionsChannelIT.xhtml").openConnection();
        connection.setRequestProperty("Accept-Encoding", "identity");
        String channelId = PushTestUtil.getChannelId(PushTestUtil.getResponseBody(connection), "capped");

        try (Socket first = SseTestUtil.connect(baseURL, channelId, null, READ_TIMEOUT)) {
            // Confirms the first connection is within the cap and stays open (which also lets it get registered before the second connects).
            assertEquals(STILL_OPEN, SseTestUtil.readCloseCode(first), "First connection within cap stays open");

            try (Socket second = SseTestUtil.connect(baseURL, channelId, null, READ_TIMEOUT)) {
                assertEquals(CLOSED_BY_SERVER, SseTestUtil.readCloseCode(second), "Second connection over cap is closed by server");
            }
        }
    }

}
