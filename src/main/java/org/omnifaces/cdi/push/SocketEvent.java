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
package org.omnifaces.cdi.push;

import static java.lang.String.format;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.Serializable;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Objects;

import javax.enterprise.event.Observes;
import javax.inject.Qualifier;
import javax.websocket.CloseReason.CloseCode;

/**
 * <p>
 * This web socket event will be fired by {@link SocketSessionManager} when a new socket has been
 * <code>&#64;</code>{@link Opened} or <code>&#64;</code>{@link Closed}. An application scoped CDI bean can
 * <code>&#64;</code>{@link Observes} them.
 * <p>
 * For detailed usage instructions, see {@link Socket} javadoc.
 *
 * @author Bauke Scholtz
 * @see Socket
 * @since 2.3
 */
public final class SocketEvent implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String channel;
	private final Serializable user;
	private final CloseCode code;

	SocketEvent(String channel, Serializable user, CloseCode code) {
		this.channel = channel;
		this.user = user;
		this.code = code;
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
	 * Returns the close code.
	 * If this returns <code>null</code>, then it was {@link Opened}.
	 * If this returns non-<code>null</code>, then it was {@link Closed}.
	 * @return The close code.
	 */
	public CloseCode getCloseCode() {
		return code;
	}

	@Override
	public int hashCode() {
		return super.hashCode() + Objects.hash(channel, user, code);
	}

	@Override
	public boolean equals(Object object) {
		if (object == null || getClass() != object.getClass()) {
			return false;
		}

		SocketEvent other = (SocketEvent) object;

		return Objects.equals(channel, other.channel)
			&& Objects.equals(user, other.user)
			&& Objects.equals(code, other.code);
	}

	@Override
	public String toString() {
		return format("SocketEvent[channel=%s, user=%s, closeCode=%s]", channel, user, code);
	}

	/**
	 * <p>
	 * Indicates that a socket has opened.
	 * <p>
	 * For detailed usage instructions, see {@link Socket} javadoc.
	 *
	 * @author Bauke Scholtz
	 * @see Socket
	 * @since 2.3
	 */
	@Qualifier
	@Target(PARAMETER)
	@Retention(RUNTIME)
	@Documented
	public @interface Opened {
		//
	}

	/**
	 * <p>
	 * Indicates that a socket has closed.
	 * <p>
	 * For detailed usage instructions, see {@link Socket} javadoc.
	 *
	 * @author Bauke Scholtz
	 * @see Socket
	 * @since 2.3
	 */
	@Qualifier
	@Target(PARAMETER)
	@Retention(RUNTIME)
	@Documented
	public @interface Closed {
		//
	}

}