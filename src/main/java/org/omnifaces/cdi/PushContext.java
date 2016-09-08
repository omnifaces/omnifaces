/*
 * Copyright 2016 OmniFaces
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
package org.omnifaces.cdi;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.websocket.RemoteEndpoint.Async;

import org.omnifaces.cdi.push.Socket;
import org.omnifaces.util.Json;

/**
 * <p>
 * CDI interface to send a message object to the push socket channel as identified by <code>&#64;</code>{@link Push}.
 * This can be injected via <code>&#64;</code>{@link Push} in any container managed artifact in WAR (not in EAR/EJB!).
 * <pre>
 * &#64;Inject &#64;Push
 * private PushContext channelName;
 * </pre>
 * <p>
 * For detailed usage instructions, see {@link Socket} javadoc.
 *
 * @author Bauke Scholtz
 * @see Socket
 * @since 2.3
 */
public interface PushContext extends Serializable {

	// Constants ------------------------------------------------------------------------------------------------------

	/** The context-relative web socket URI prefix where the endpoint should listen on. */
	String URI_PREFIX = "/omnifaces.push";

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Send given message object to the push socket channel as identified by <code>&#64;</code>{@link Push}.
	 * The message object will be encoded as JSON and be available as first argument of the JavaScript listener function
	 * declared in <code>&lt;o:socket onmessage&gt;</code>.
	 * @param message The push message object.
	 * @return The results of the send operation. If it returns an empty set, then there was no open web socket session
	 * associated with given socket channel. The returned futures will return <code>null</code> on {@link Future#get()}
	 * if the message was successfully delivered and otherwise throw {@link ExecutionException}.
	 * @throws IllegalArgumentException If given message object cannot be encoded as JSON.
	 * @see Json#encode(Object)
	 * @see Async#sendText(String)
	 */
	Set<Future<Void>> send(Object message);

	/**
	 * Send given message object to the push socket channel as identified by <code>&#64;</code>{@link Push}, targeted
	 * to the given user as identified by <code>&lt;o:socket user&gt;</code>.
	 * The message object will be encoded as JSON and be available as first argument of the JavaScript listener function
	 * declared in <code>&lt;o:socket onmessage&gt;</code>.
	 * @param <S> The generic type of the user identifier.
	 * @param message The push message object.
	 * @param user The user to which the push message object must be delivered to.
	 * @return The results of the send operation. If it returns an empty set, then there was no open web socket session
	 * associated with given socket channel and user. The returned futures will return <code>null</code> on
	 * {@link Future#get()} if the message was successfully delivered and otherwise throw {@link ExecutionException}.
	 * @throws IllegalArgumentException If given message object cannot be encoded as JSON.
	 * @see Json#encode(Object)
	 * @see Async#sendText(String)
	 */
	<S extends Serializable> Set<Future<Void>> send(Object message, S user);

	/**
	 * Send given message object to the push socket channel as identified by <code>&#64;</code>{@link Push}, targeted
	 * to the given users as identified by <code>&lt;o:socket user&gt;</code>.
	 * The message object will be encoded as JSON and be available as first argument of the JavaScript listener function
	 * declared in <code>&lt;o:socket onmessage&gt;</code>.
	 * @param <S> The generic type of the user identifier.
	 * @param message The push message object.
	 * @param users The users to which the push message object must be delivered to.
	 * @return The results of the send operation grouped by user. If it contains an empty set, then there was no open
	 * web socket session associated with given socket channel and user. The returned futures will return
	 * <code>null</code> on {@link Future#get()} if the message was successfully delivered and otherwise throw
	 * {@link ExecutionException}.
	 * @throws IllegalArgumentException If given message object cannot be encoded as JSON.
	 * @see Json#encode(Object)
	 * @see Async#sendText(String)
	 */
	<S extends Serializable> Map<S, Set<Future<Void>>> send(Object message, Collection<S> users);

}