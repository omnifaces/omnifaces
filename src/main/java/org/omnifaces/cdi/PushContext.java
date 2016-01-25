/*
 * Copyright 2015 OmniFaces.
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

import org.omnifaces.cdi.push.Socket;
import org.omnifaces.util.Json;

/**
 * <p>
 * CDI interface to send a message object to the push socket channel as identified by {@link Push}.
 * This can be injected via {@link Push} in any container managed artifact in WAR (not in EAR/EJB!).
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
	public static final String URI_PREFIX = "/omnifaces.push";

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Send given message object to the push socket channel as identified by {@link Push}. This will be encoded as JSON
	 * and be available as first argument of the JavaScript listener function declared in
	 * <code>&lt;o:socket onmessage&gt;</code>.
	 * @param message The push message object.
	 * @throws IllegalArgumentException If given message object cannot be encoded as JSON.
	 * @see Json#encode(Object)
	 */
	public void send(Object message);

}