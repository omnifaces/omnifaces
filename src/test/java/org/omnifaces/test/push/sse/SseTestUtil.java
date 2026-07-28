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

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.omnifaces.cdi.PushContext;
import org.omnifaces.test.push.PushTestUtil;

/**
 * Test helpers to connect to the SSE endpoint as a raw HTTP request, so that the connecting HTTP session (i.e. the presence or absence of the
 * <code>JSESSIONID</code> cookie) and the connection lifecycle are fully controllable, which a browser driven test cannot do.
 */
final class SseTestUtil {

    /** The SSE endpoint always responds with HTTP 200 as the SSE protocol has no other way to convey the reason to the <code>EventSource</code> client. */
    private static final int OK = 200;

    private static final Pattern SSE_CLOSE_EVENT = Pattern.compile("event: close\ndata: (\\d+)\n\n");

    private SseTestUtil() {
        throw new AssertionError();
    }

    /**
     * Opens a socket to the SSE endpoint of the given channel ID, optionally presenting the given session cookie, consumes the response head and returns the
     * still-open socket. The caller is responsible for closing it.
     */
    static Socket connect(URL baseURL, String channelId, String sessionCookie, int readTimeout) throws IOException {
        Socket socket = PushTestUtil.sendRequest(baseURL, PushContext.SSE_URI_PREFIX, channelId, sessionCookie, "Accept: text/event-stream");
        socket.setSoTimeout(readTimeout);
        int statusCode = PushTestUtil.readStatusCode(socket.getInputStream());

        if (statusCode != OK) {
            socket.close();
            throw new IllegalStateException("SSE endpoint responded with HTTP " + statusCode + " instead of " + OK);
        }

        return socket;
    }

    /**
     * Reads the event stream of the given socket until the server sent a close event and returns its code, or -1 when the read timeout expired without one,
     * i.e. the connection is being held open.
     */
    static int readCloseCode(Socket socket) throws IOException {
        InputStream input = socket.getInputStream();
        StringBuilder body = new StringBuilder();

        try {
            for (int b; (b = input.read()) != -1;) {
                body.append((char) b);

                if (b == '\n') {
                    Matcher matcher = SSE_CLOSE_EVENT.matcher(body);

                    if (matcher.find()) {
                        return Integer.parseInt(matcher.group(1));
                    }
                }
            }
        }
        catch (SocketTimeoutException openConnection) {
            // No close event within the timeout, so the connection is being held open.
        }

        return -1;
    }

}
