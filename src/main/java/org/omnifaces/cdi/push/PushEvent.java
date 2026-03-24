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

import java.io.Serializable;
import java.util.Objects;

import jakarta.enterprise.event.Observes;

/**
 * <p>
 * Abstract base for push channel events fired when a connection has been opened, its user switched, or closed.
 * An application scoped CDI bean can <code>&#64;</code>{@link Observes} the concrete subclass events
 * {@link SocketEvent} and {@link SseEvent}.
 *
 * @author Bauke Scholtz
 * @see SocketEvent
 * @see SseEvent
 * @since 5.2
 */
public abstract class PushEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String channel;
    private final Serializable user;
    private final Serializable previousUser;

    /**
     * Creates a push event for the given channel and user.
     * @param channel The channel name.
     * @param user The current user identifier, if any.
     * @param previousUser The previous user identifier, if any.
     */
    protected PushEvent(String channel, Serializable user, Serializable previousUser) {
        this.channel = channel;
        this.user = user;
        this.previousUser = previousUser;
    }

    /**
     * Returns the push channel name.
     * @return The push channel name.
     */
    public String getChannel() {
        return channel;
    }

    /**
     * Returns the current user identifier, if any.
     * @param <S> The generic type of the user identifier.
     * @return The current user identifier, if any.
     * @throws ClassCastException When <code>S</code> is of wrong type.
     */
    @SuppressWarnings("unchecked")
    public <S extends Serializable> S getUser() {
        return (S) user;
    }

    /**
     * Returns the previous user identifier, if any.
     * @param <S> The generic type of the user identifier.
     * @return The previous user identifier, if any.
     * @throws ClassCastException When <code>S</code> is of wrong type.
     */
    @SuppressWarnings("unchecked")
    public <S extends Serializable> S getPreviousUser() {
        return (S) previousUser;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(channel, user);
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        var other = (PushEvent) object;

        return Objects.equals(channel, other.channel)
            && Objects.equals(user, other.user);
    }

    @Override
    public String toString() {
        return format("%s[channel=%s, user=%s]", getClass().getSimpleName(), channel, user);
    }

}
