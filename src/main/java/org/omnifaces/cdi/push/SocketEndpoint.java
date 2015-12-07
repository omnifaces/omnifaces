/*
 * Copyright 2015 OmniFaces.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.cdi.push;

import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.omnifaces.config.BeanManager;

/**
 * <p>
 * The web socket server endpoint of <code>&lt;o:socket&gt;</code>.
 *
 * @author Bauke Scholtz
 * @see Socket
 * @since 2.3
 */
@ServerEndpoint(value = SocketEndpoint.URI_TEMPLATE)
public class SocketEndpoint {

	public static final String URI_TEMPLATE = PushContext.URI_PREFIX + "/{channel}";

	/**
	 * Add given web socket session to the push context associated with given channel.
	 * @param session The opened web socket session.
	 * @param channel The push channel name.
	 */
	@OnOpen
	public void open(Session session, @PathParam("channel") String channel) {
		BeanManager.INSTANCE.getReference(SocketPushContext.class).add(session, channel); // @Inject in @ServerEndpoint doesn't work in Tomcat+Weld.
	}

	/**
	 * Remove given web socket session from the push context.
	 * @param session The closed web socket session.
	 */
	@OnClose
	public void close(Session session) {
		BeanManager.INSTANCE.getReference(SocketPushContext.class).remove(session); // @Inject in @ServerEndpoint doesn't work in Tomcat+Weld.
	}

}