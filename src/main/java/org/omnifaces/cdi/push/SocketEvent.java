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

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.Serializable;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Objects;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import jakarta.websocket.CloseReason.CloseCode;
import jakarta.websocket.CloseReason.CloseCodes;

/**
 * <p>
 * This web socket event will be fired by {@link SocketSessionManager} when a socket has been
 * <code>&#64;</code>{@link Opened}, <code>&#64;</code>{@link Switched} or <code>&#64;</code>{@link Closed}.
 * An application scoped CDI bean can <code>&#64;</code>{@link Observes} them.
 * <p>
 * For detailed usage instructions, see {@link Socket} javadoc.
 *
 * @author Bauke Scholtz
 * @see Socket
 * @since 2.3
 */
public final class SocketEvent extends PushEvent {

    private static final long serialVersionUID = 1L;

    private final Integer closeCode;

    SocketEvent(String channel, Serializable user, Serializable previousUser, CloseCode closeCode) {
        super(channel, user, previousUser);
        this.closeCode = closeCode == null ? null : closeCode.getCode();
    }

    /**
     * Returns the close code.
     * If this returns <code>null</code>, then it was {@link Opened}.
     * If this returns non-<code>null</code>, then it was {@link Closed}.
     * @return The close code.
     */
    public CloseCode getCloseCode() {
        return closeCode == null ? null : CloseCodes.getCloseCode(closeCode);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(closeCode);
    }

    @Override
    public boolean equals(Object object) {
        if (!super.equals(object)) {
            return false;
        }

        var other = (SocketEvent) object;

        return Objects.equals(closeCode, other.closeCode);
    }

    @Override
    public String toString() {
        return "SocketEvent[channel=%s, user=%s, closeCode=%s]".formatted(getChannel(), getUser(), getCloseCode());
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

        /**
         * The literal of {@link Opened}.
         * @since 3.2
         */
        AnnotationLiteral<Opened> LITERAL = new AnnotationLiteral<>() {
            private static final long serialVersionUID = 1L;
        };
    }

    /**
     * <p>
     * Indicates that a socket user was switched.
     * <p>
     * For detailed usage instructions, see {@link Socket} javadoc.
     *
     * @author Bauke Scholtz
     * @see Socket
     * @since 3.2
     */
    @Qualifier
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @Documented
    public @interface Switched {

        /**
         * The literal of {@link Switched}.
         */
        AnnotationLiteral<Switched> LITERAL = new AnnotationLiteral<>() {
            private static final long serialVersionUID = 1L;
        };
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

        /**
         * The literal of {@link Closed}.
         * @since 3.2
         */
        AnnotationLiteral<Closed> LITERAL = new AnnotationLiteral<>() {
            private static final long serialVersionUID = 1L;
        };
    }

}
