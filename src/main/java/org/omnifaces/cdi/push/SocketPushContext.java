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

import static org.omnifaces.util.Beans.isActive;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import javax.enterprise.context.SessionScoped;

import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;

/**
 * <p>
 * Concrete implementation of {@link PushContext} which is to be injected by {@link Push}.
 * This is produced by {@link SocketPushContextProducer}.
 *
 * @author Bauke Scholtz
 * @see Push
 * @since 2.3
 */
public class SocketPushContext implements PushContext, Serializable {

	private static final long serialVersionUID = 1L;

	private String channel;
	private Map<String, String> sessionScopedChannelIds;

	/**
	 * Creates a socket push context whereby the mutable map of scoped channel IDs is referenced, so it's still
	 * available when another thread invokes {@link #send(Object)} where the scope isn't active.
	 */
	SocketPushContext(String channel, SocketScope scope) {
		this.channel = channel;
		sessionScopedChannelIds = isActive(SessionScoped.class) ? scope.getSessionScopedChannelIds() : Collections.<String, String>emptyMap();
	}

	@Override
	public void send(Object message) {
		String channelId = SocketScope.getChannelId(channel, sessionScopedChannelIds.get(channel));
		SocketManager.getInstance().send(channelId, message);
	}

}