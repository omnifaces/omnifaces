/*
 * Copyright 2016 OmniFaces.
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

import static org.omnifaces.cdi.push.SocketChannelManager.getChannelId;
import static org.omnifaces.util.Beans.isActive;

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
public class SocketPushContext implements PushContext {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;

	// Variables ------------------------------------------------------------------------------------------------------

	private String channel;
	private Map<String, String> sessionScopeIds;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Creates a socket push context whereby the mutable map of session scope channel identifiers is referenced, so it's
	 * still available when another thread invokes {@link #send(Object)} during which the session scope is not
	 * necessarily active anymore.
	 */
	SocketPushContext(String channel, SocketChannelManager manager) {
		this.channel = channel;
		sessionScopeIds = isActive(SessionScoped.class) ? manager.getSessionScopeIds() : Collections.<String, String>emptyMap();
	}

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public void send(Object message) {
		SocketManager.getInstance().send(getChannelId(channel, sessionScopeIds), message);
	}

}