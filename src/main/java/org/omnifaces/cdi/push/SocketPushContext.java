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

import static java.util.Arrays.asList;
import static org.omnifaces.cdi.push.SocketChannelManager.EMPTY_SCOPE;
import static org.omnifaces.cdi.push.SocketChannelManager.getChannelId;
import static org.omnifaces.cdi.push.SocketChannelManager.getSessionScopeIds;
import static org.omnifaces.cdi.push.SocketChannelManager.getViewScopeIds;
import static org.omnifaces.util.Beans.isActive;
import static org.omnifaces.util.Faces.hasContext;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

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
	private Map<String, String> viewScopeIds;

	private SocketSessionManager sessionManager;
	private SocketUserManager userManager;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Creates a socket push context whereby the mutable map of session and view scope channel identifiers is
	 * referenced, so it's still available when another thread invokes {@link #send(Object)} during which the session
	 * and view scope is not necessarily active anymore.
	 */
	SocketPushContext(String channel, SocketSessionManager sessionManager, SocketUserManager userManager) {
		this.channel = channel;
		boolean hasSession = isActive(SessionScoped.class);
		sessionScopeIds = hasSession ? getSessionScopeIds() : EMPTY_SCOPE;
		viewScopeIds = hasSession && hasContext() ? getViewScopeIds(false) : EMPTY_SCOPE;
		this.sessionManager = sessionManager;
		this.userManager = userManager;
	}

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public Set<Future<Void>> send(Object message) {
		return SocketSessionManager.getInstance().send(getChannelId(channel, sessionScopeIds, viewScopeIds), message);
	}

	@Override
	public <S extends Serializable> Set<Future<Void>> send(Object message, S user) {
		return send(message, asList(user)).get(user);
	}

	@Override
	public <S extends Serializable> Map<S, Set<Future<Void>>> send(Object message, Collection<S> users) {
		Map<S, Set<Future<Void>>> resultsByUser = new HashMap<>(users.size());

		for (S user : users) {
			Set<String> userChannelIds = userManager.getUserChannelIds(user, channel);
			Set<Future<Void>> results = new HashSet<>(userChannelIds.size());

			for (String channelId : userChannelIds) {
				results.addAll(sessionManager.send(channelId, message));
			}

			resultsByUser.put(user, results);
		}

		return resultsByUser;
	}

}