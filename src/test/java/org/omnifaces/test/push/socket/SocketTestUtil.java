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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test helpers to perform the web socket handshake as a raw HTTP upgrade, so that the connecting HTTP session (i.e. the
 * presence or absence of the <code>JSESSIONID</code> cookie) and the connection lifecycle are fully controllable, which
 * a browser driven test cannot do.
 */
final class SocketTestUtil {

    private static final String WEBSOCKET_HANDSHAKE_KEY = "dGhlIHNhbXBsZSBub25jZQ=="; // Arbitrary; the server does not validate its value.

    private SocketTestUtil() {
        throw new AssertionError();
    }

    static String getResponseBody(HttpURLConnection connection) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[10240];

        try (InputStream input = connection.getInputStream()) {
            for (int length; (length = input.read(buffer)) != -1;) {
                output.write(buffer, 0, length);
            }
        }

        return new String(output.toByteArray(), "UTF-8");
    }

    static String getSessionCookie(HttpURLConnection connection) {
        List<String> setCookies = connection.getHeaderFields().get("Set-Cookie");

        if (setCookies != null) {
            for (String setCookie : setCookies) {
                if (setCookie.startsWith("JSESSIONID=")) {
                    return setCookie.split(";", 2)[0];
                }
            }
        }

        throw new IllegalStateException("Response did not set a JSESSIONID cookie");
    }

    static String getChannelId(String page, String channel) {
        Matcher matcher = Pattern.compile("'(" + Pattern.quote(channel) + "\\?[^']+)'").matcher(page);

        if (!matcher.find()) {
            throw new IllegalStateException("Cannot find channel ID of '" + channel + "' in page: " + page);
        }

        return matcher.group(1);
    }

    /**
     * Opens a socket to the web socket endpoint of the given channel ID, sends the upgrade handshake request, optionally
     * presenting the given session cookie, and returns the still-open socket. The caller is responsible for closing it.
     */
    static Socket sendHandshake(URL baseURL, String channelId, String sessionCookie) throws IOException {
        int port = baseURL.getPort() != -1 ? baseURL.getPort() : baseURL.getDefaultPort();
        String path = baseURL.getPath().replaceFirst("/$", "") + "/omnifaces.push/" + channelId;
        Socket socket = new Socket(baseURL.getHost(), port);
        StringBuilder request = new StringBuilder();
        request.append("GET ").append(path).append(" HTTP/1.1\r\n");
        request.append("Host: ").append(baseURL.getHost()).append(':').append(port).append("\r\n");
        request.append("Upgrade: websocket\r\n");
        request.append("Connection: Upgrade\r\n");
        request.append("Sec-WebSocket-Key: ").append(WEBSOCKET_HANDSHAKE_KEY).append("\r\n");
        request.append("Sec-WebSocket-Version: 13\r\n");

        if (sessionCookie != null) {
            request.append("Cookie: ").append(sessionCookie).append("\r\n");
        }

        request.append("\r\n");
        OutputStream output = socket.getOutputStream();
        output.write(request.toString().getBytes("UTF-8"));
        output.flush();
        return socket;
    }

    /**
     * Reads and consumes the HTTP response head from the given input stream (leaving it positioned at any web socket
     * frame that follows) and returns its status code, i.e. 101 when the handshake is accepted, or -1 when the server
     * did not return a status line.
     */
    static int readStatusCode(InputStream input) throws IOException {
        StringBuilder head = new StringBuilder();

        for (int b; (b = input.read()) != -1;) {
            head.append((char) b);

            if (head.length() >= 4 && "\r\n\r\n".equals(head.substring(head.length() - 4))) {
                break;
            }
        }

        String[] statusLine = head.toString().split("\r\n", 2)[0].split(" ");
        return statusLine.length >= 2 ? Integer.parseInt(statusLine[1]) : -1;
    }

}
