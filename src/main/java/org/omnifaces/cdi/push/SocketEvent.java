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

import java.io.Serializable;
import java.util.Objects;

import javax.enterprise.event.Observes;
import javax.websocket.CloseReason;

import org.omnifaces.cdi.push.event.Closed;
import org.omnifaces.cdi.push.event.Opened;

/**
 * <p>
 * The socket event. This will be created by {@link SocketSessionManager} when a new socket has been {@link Opened} or
 * {@link Closed}. An application scoped CDI bean can {@link Observes} them.
 * <p>
 * For detailed usage instructions, see {@link Socket} javadoc.
 *
 * @author Bauke Scholtz
 * @see Socket
 * @see Opened
 * @see Closed
 * @since 2.3
 */
public final class SocketEvent implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String channel;
	private final Serializable user;
	private final CloseReason reason;

	SocketEvent(String channel, Serializable user, CloseReason reason) {
		this.channel = channel;
		this.user = user;
		this.reason = reason;
	}

	/**
	 * Returns the <code>&lt;o:socket channel&gt;</code>.
	 * @return The web socket channel name.
	 */
	public String getChannel() {
		return channel;
	}

	/**
	 * Returns the <code>&lt;o:socket user&gt;</code>, if any.
	 * @param <S> The generic type of the user identifier.
	 * @return The web socket user identifier, if any.
	 * @throws ClassCastException When <code>S</code> is of wrong type.
	 */
	@SuppressWarnings("unchecked")
	public <S extends Serializable> S getUser() {
		return (S) user;
	}

	/**
	 * Returns the close reason.
	 * If this returns <code>null</code>, then it was {@link Opened}.
	 * If this returns non-<code>null</code>, then it was {@link Closed}.
	 * @return The close reason.
	 */
	public CloseReason getCloseReason() {
		return reason;
	}

	@Override
	public int hashCode() {
		return super.hashCode() + Objects.hash(channel, user, reason);
	}

	@Override
	public boolean equals(Object other) {
		return other != null && getClass() == other.getClass()
			&& Objects.equals(channel, ((SocketEvent) other).channel)
			&& Objects.equals(user, ((SocketEvent) other).user)
			&& Objects.equals(reason, ((SocketEvent) other).reason);
	}

	@Override
	public String toString() {
		return String.format("SocketEvent[channel=%s, user=%s, closeReason=%s]", channel, user, reason);
	}

}