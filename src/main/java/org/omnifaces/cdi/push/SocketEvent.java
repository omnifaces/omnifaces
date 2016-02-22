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

import org.omnifaces.cdi.push.event.Closed;
import org.omnifaces.cdi.push.event.Opened;

/**
 * <p>
 * The socket event. This will be created by {@link SocketSessionManager} when a new socket has been {@link Opened} or
 * {@link Closed}. An application scoped CDI bean can {@link Observes} them.
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

	SocketEvent(String channel, Serializable user) {
		this.channel = channel;
		this.user = user;
	}

	public String getChannel() {
		return channel;
	}

	public Serializable getUser() {
		return user;
	}

	@Override
	public int hashCode() {
		return super.hashCode() + Objects.hash(channel, user);
	}

	@Override
	public boolean equals(Object other) {
		return super.equals(other)
			&& Objects.equals(channel, ((SocketEvent) other).channel)
			&& Objects.equals(user, ((SocketEvent) other).user);
	}

	@Override
	public String toString() {
		return String.format("SocketEvent[channel=%s, user=%s]", channel, user);
	}

}