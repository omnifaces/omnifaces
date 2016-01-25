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

import static java.util.Collections.synchronizedSet;
import static org.omnifaces.cdi.push.Socket.Scope.APPLICATION;
import static org.omnifaces.util.Utils.isEmpty;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.context.SessionScoped;

import org.omnifaces.cdi.push.Socket.Scope;

/**
 * <p>
 * The web socket scope of <code>&lt;o:socket&gt;</code>.
 *
 * @author Bauke Scholtz
 * @see Socket
 * @since 2.3
 */
@SessionScoped
public class SocketScope implements Serializable {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;

	// Properties -----------------------------------------------------------------------------------------------------

	private static Set<String> applicationScopedChannels = synchronizedSet(new HashSet<String>());
	private ConcurrentMap<String, String> sessionScopedChannelIds = new ConcurrentHashMap<>();

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Register given channel on given scope and returns the web socket channel identifier if channel does not already
	 * exist on a different scope, else <code>null</code>.
	 * @param channel The web socket channel.
	 * @param scope The web socket scope.
	 * @return The web socket channel identifier if channel does not already exist on a different scope, else
	 * <code>null</code>.
	 */
	public String register(String channel, Scope scope) {
		if (scope == APPLICATION) {
			if (sessionScopedChannelIds.containsKey(channel)) {
				return null;
			}

			applicationScopedChannels.add(channel);
		}
		else {
			if (applicationScopedChannels.contains(channel)) {
				return null;
			}

			if (!sessionScopedChannelIds.containsKey(channel)) {
				sessionScopedChannelIds.putIfAbsent(channel, UUID.randomUUID().toString());
			}
		}

		return getChannelId(channel, sessionScopedChannelIds.get(channel));
	}

	Map<String, String> getSessionScopedChannelIds() {
		return sessionScopedChannelIds;
	}

	static String getChannelId(String channel, String scopeId) {
		return channel + (isEmpty(scopeId) ? "" : ("?" + scopeId));
	}

	private void writeObject(ObjectOutputStream output) throws IOException {
		output.defaultWriteObject();
		output.writeObject(applicationScopedChannels); // Just in case server restarts with session persistence.
	}

	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
		input.defaultReadObject();
		applicationScopedChannels.addAll((Set<String>) input.readObject());
	}

}