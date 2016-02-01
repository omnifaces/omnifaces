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

import static org.omnifaces.util.Utils.isEmpty;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.context.SessionScoped;

/**
 * <p>
 * This manages all web socket scope identifiers explicitly registered by <code>&lt;o:socket&gt;</code>.
 * Any incoming web socket handshake request is here validated via {@link SocketChannelFilter}.
 *
 * @author Bauke Scholtz
 * @see Socket
 * @since 2.3
 */
@SessionScoped
public class SocketScopeManager implements Serializable {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;

	private enum Scope {
		APPLICATION, SESSION;

		public static Scope of(String value) {
			if (value == null) {
				return APPLICATION;
			}

			for (Scope scope : values()) {
				if (scope.name().equalsIgnoreCase(value)) {
					return scope;
				}
			}

			throw new IllegalArgumentException();
		}
	}

	// Properties -----------------------------------------------------------------------------------------------------

	private static ConcurrentMap<String, String> applicationScopeIds = new ConcurrentHashMap<>();
	private ConcurrentMap<String, String> sessionScopeIds = new ConcurrentHashMap<>();

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Register given channel on given scope and returns the web socket scope identifier if channel does not already
	 * exist on a different scope, else <code>null</code>.
	 * @param channel The web socket channel.
	 * @param scope The web socket scope. Supported values are <code>application</code> and <code>session</code>, case
	 * insensitive. If <code>null</code>, the default is <code>application</code>.
	 * @return The web socket scope identifier if channel does not already exist on a different scope, else
	 * <code>null</code>.
	 * @throws IllegalArgumentException When the scope is invalid.
	 */
	public String register(String channel, String scope) {
		switch (Scope.of(scope)) {
			case APPLICATION: return register(channel, applicationScopeIds, sessionScopeIds);
			case SESSION: return register(channel, sessionScopeIds, applicationScopeIds);
			default: throw new UnsupportedOperationException();
		}
	}

	private static String register(String channel, ConcurrentMap<String, String> targetScope, Map<String, String> otherScope) {
		if (otherScope.containsKey(channel)) {
			return null;
		}

		if (!targetScope.containsKey(channel)) {
			targetScope.putIfAbsent(channel, UUID.randomUUID().toString());
		}

		return targetScope.get(channel);
	}

	/**
	 * Returns <code>true</code> if the given channel and scope identifier combination is already registered.
	 * @param channel The web socket channel.
	 * @param scopeId The web socket scope identifier.
	 * @return <code>true</code> if the given channel and scope identifier combination is already registered.
	 */
	public boolean isRegistered(String channel, String scopeId) {
		return !isEmpty(scopeId) && (scopeId.equals(applicationScopeIds.get(channel)) || scopeId.equals(sessionScopeIds.get(channel)));
	}

	/**
	 * For internal usage only. This makes it possible to remember session scope IDs during injection time of
	 * {@link SocketPushContext} (the session scope is not necessarily active during push send time).
	 */
	Map<String, String> getSessionScopeIds() {
		return sessionScopeIds;
	}

	/**
	 * For internal usage only. This makes it possible to resolve the session scope ID during push send time in
	 * {@link SocketPushContext}.
	 */
	static String getScopeId(String channel, Map<String, String> sessionScopeIds) {
		String scopeId = sessionScopeIds.get(channel);
		return (scopeId != null) ? scopeId : applicationScopeIds.get(channel);
	}

	// Serialization --------------------------------------------------------------------------------------------------

	private void writeObject(ObjectOutputStream output) throws IOException {
		output.defaultWriteObject();
		output.writeObject(applicationScopeIds); // Just in case server restarts with session persistence.
	}

	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
		input.defaultReadObject();
		applicationScopeIds.putAll((Map<String, String>) input.readObject());
	}

}