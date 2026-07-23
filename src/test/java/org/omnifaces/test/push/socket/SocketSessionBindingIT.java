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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.omnifaces.test.OmniFacesIT;

/**
 * Verifies that a session scoped web socket channel can only be connected to by the HTTP session which registered it,
 * while an application scoped channel remains connectable by any (or no) HTTP session. Performs the web socket handshake
 * as a raw HTTP upgrade so that the connecting HTTP session (i.e. the presence or absence of the <code>JSESSIONID</code>
 * cookie) is fully controllable, which a browser driven test cannot do.
 */
public class SocketSessionBindingIT extends OmniFacesIT {

	private static final int SWITCHING_PROTOCOLS = 101;

	@Deployment(testable=false)
	public static WebArchive createDeployment() {
		return buildWebArchive(SocketSessionBindingIT.class)
				.withWebXml(WebXml.withSocket)
				.createDeployment();
	}

	@Test
	public void test() throws Exception {
		HttpURLConnection connection = (HttpURLConnection) new URL(baseURL + "SocketSessionBindingIT.xhtml").openConnection();
		connection.setRequestProperty("Accept-Encoding", "identity");
		String sessionCookie = SocketTestUtil.getSessionCookie(connection);
		String page = SocketTestUtil.getResponseBody(connection);
		String sessionScopedChannelId = SocketTestUtil.getChannelId(page, "sessionScoped");
		String applicationScopedChannelId = SocketTestUtil.getChannelId(page, "applicationScoped");

		// A session scoped channel may only be connected to by the owning HTTP session.
		assertEquals("Owning session may connect to session scoped channel", SWITCHING_PROTOCOLS, handshake(sessionScopedChannelId, sessionCookie));
		assertNotEquals("Foreign session may not connect to session scoped channel", SWITCHING_PROTOCOLS, handshake(sessionScopedChannelId, null));

		// An application scoped channel is by design not bound to any HTTP session.
		assertEquals("Any session may connect to application scoped channel", SWITCHING_PROTOCOLS, handshake(applicationScopedChannelId, null));
	}

	private int handshake(String channelId, String sessionCookie) throws IOException {
		try (Socket socket = SocketTestUtil.sendHandshake(baseURL, channelId, sessionCookie)) {
			return SocketTestUtil.readStatusCode(socket.getInputStream());
		}
	}

}
