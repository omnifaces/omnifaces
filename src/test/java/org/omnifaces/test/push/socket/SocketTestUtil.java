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

import java.io.IOException;
import java.net.Socket;
import java.net.URL;

import org.omnifaces.cdi.PushContext;
import org.omnifaces.test.push.PushTestUtil;

/**
 * Test helper to perform the web socket handshake as a raw HTTP upgrade, so that the connecting HTTP session (i.e. the presence or absence of the
 * <code>JSESSIONID</code> cookie) and the connection lifecycle are fully controllable, which a browser driven test cannot do.
 */
final class SocketTestUtil {

    private static final String WEBSOCKET_HANDSHAKE_KEY = "dGhlIHNhbXBsZSBub25jZQ=="; // Arbitrary; the server does not validate its value.

    private SocketTestUtil() {
        throw new AssertionError();
    }

    /**
     * Opens a socket to the web socket endpoint of the given channel ID, sends the upgrade handshake request, optionally presenting the given session cookie,
     * and returns the still-open socket. The caller is responsible for closing it.
     */
    static Socket sendHandshake(URL baseURL, String channelId, String sessionCookie) throws IOException {
        return PushTestUtil.sendRequest(
            baseURL, PushContext.SOCKET_URI_PREFIX, channelId, sessionCookie,
            "Upgrade: websocket", "Connection: Upgrade", "Sec-WebSocket-Key: " + WEBSOCKET_HANDSHAKE_KEY, "Sec-WebSocket-Version: 13"
        );
    }

}
