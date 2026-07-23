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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;

/**
 * Verifies that a channel accepts no more concurrent web socket sessions than the configured maximum (here 1 via
 * <code>org.omnifaces.SOCKET_MAX_SESSIONS_PER_CHANNEL</code>). The cap is enforced at <code>onOpen</code>, so the second connection is still accepted at
 * handshake (101) but is then immediately closed by the server, whereas the first stays open.
 */
public class SocketMaxSessionsIT extends OmniFacesIT {

    private static final int SWITCHING_PROTOCOLS = 101;
    private static final int CLOSE_TIMEOUT = 2000;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return buildWebArchive(SocketMaxSessionsIT.class)
            .withWebXml(WebXml.withSocketMaxSessions)
            .createDeployment();
    }

    @Test
    public void test() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(baseURL + "SocketMaxSessionsIT.xhtml").openConnection();
        connection.setRequestProperty("Accept-Encoding", "identity");
        String channelId = SocketTestUtil.getChannelId(SocketTestUtil.getResponseBody(connection), "capped");

        Socket first = handshake(channelId);

        try {
            // Confirms the first connection is within the cap and stays open (which also lets its onOpen complete
            // before the second connects).
            assertFalse(isClosedByServer(first), "First connection within cap stays open");

            Socket second = handshake(channelId);

            try {
                // The second connection is over the cap and is therefore closed by the server right after onOpen.
                assertTrue(isClosedByServer(second), "Second connection over cap is closed by server");
            }
            finally {
                second.close();
            }
        }
        finally {
            first.close();
        }
    }

    private Socket handshake(String channelId) throws IOException {
        Socket socket = SocketTestUtil.sendHandshake(baseURL, channelId, null);
        assertEquals(SWITCHING_PROTOCOLS, SocketTestUtil.readStatusCode(socket.getInputStream()), "Handshake accepted");
        return socket;
    }

    private static boolean isClosedByServer(Socket socket) throws IOException {
        socket.setSoTimeout(CLOSE_TIMEOUT);

        try {
            socket.getInputStream().read(); // A web socket close frame byte, or -1 on EOF; either way the server is closing.
            return true;
        }
        catch (SocketTimeoutException openConnection) {
            return false; // No data within the timeout, so the connection is being held open.
        }
        catch (SocketException closedConnection) {
            return true; // A connection reset also indicates the server closed it.
        }
    }

}
