///
/// Copyright OmniFaces
///
/// Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
/// the License. You may obtain a copy of the License at
///
///     https://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
/// an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
/// specific language governing permissions and limitations under the License.
///

import { Util } from "./Util";

/**
 * Manage web socket push channels. This script is used by <code>&lt;o:socket&gt;</code>.
 * 
 * @author Bauke Scholtz
 * @see org.omnifaces.cdi.push.Socket
 * @since 2.3
 */
export module Push {

	// "Constant" fields ----------------------------------------------------------------------------------------------

	const URL_PROTOCOL = window.location.protocol.replace("http", "ws") + "//";
	const URI_PREFIX = "/omnifaces.push";
	const RECONNECT_INTERVAL = 500;
	const MAX_RECONNECT_ATTEMPTS = 25;
	const REASON_EXPIRED = "Expired";
	const REASON_UNKNOWN_CHANNEL = "Unknown channel";

	// Private static fields ------------------------------------------------------------------------------------------

	const sockets: Record<string, Socket> = {};

	// Private static classes -----------------------------------------------------------------------------------------

	class Socket {

		// Private fields ---------------------------------------------------------------------------------------------

		readonly url: string;
		readonly channel: string;
		readonly onopen: Function;
		readonly onmessage: Function;
		readonly onerror: Function;
		readonly onclose: Function;
		readonly behaviors: Record<string, Function[]>;

		socket: WebSocket;
		reconnectAttempts: number;

		// Constructor ------------------------------------------------------------------------------------------------

		/**
		 * Creates a reconnecting web socket. When the web socket successfully connects on first attempt, then it will
		 * automatically reconnect on timeout with cumulative intervals of 500ms with a maximum of 25 attempts (~3 minutes).
		 * The <code>onclose</code> function will be called with the error code of the last attempt.
		 * @constructor
		 * @param url The URL of the web socket 
		 * @param channel The name of the web socket channel.
		 * @param onopen The function to be invoked when the web socket is opened.
		 * @param onmessage The function to be invoked when a message is received.
		 * @param onerror The funtypction to be invoked when a connection error has occurred and the web socket will attempt to reconnect.
		 * @param onclose The function to be invoked when the web socket is closed and will not anymore attempt to reconnect.
		 * @param behaviors Client behavior functions to be invoked when specific message is received.
		 */
		constructor(url: string, channel: string, onopen: Function, onmessage: Function, onerror: Function, onclose: Function, behaviors: Record<string, Function[]>) {
			this.url = url;
			this.channel = channel;
			this.onopen = onopen;
			this.onmessage = onmessage;
			this.onerror = onerror;
			this.onclose = onclose;
			this.behaviors = behaviors;
		}

		// Public functions -------------------------------------------------------------------------------------------

		/**
		 * Opens the reconnecting web socket.
		 */
		open() {
			const self = this;

			if (this.socket && this.socket.readyState == 1) {
				return;
			}

			this.socket = new WebSocket(this.url);

			this.socket.onopen = function() {
				if (self.reconnectAttempts == null) {
					self.onopen(self.channel);
				}

				self.reconnectAttempts = 0;
			}

			this.socket.onmessage = function(event: MessageEvent) {
				const message = JSON.parse(event.data);
				self.onmessage(message, self.channel, event);
				const functions = self.behaviors[message];

				if (functions && functions.length) {
					for (let behavior of functions) {
						behavior();
					}
				}
			}

			this.socket.onclose = function(event: CloseEvent) {
				if (!self.socket
					|| (event.code == 1000 && event.reason == REASON_EXPIRED)
					|| (event.code == 1008 || (event.code == 1005 && event.reason == REASON_UNKNOWN_CHANNEL)) // Older IE versions incorrectly return 1005 instead of 1008, hence the extra check on the message.
					|| (self.reconnectAttempts == null)
					|| (self.reconnectAttempts >= MAX_RECONNECT_ATTEMPTS))
				{
					self.onclose(event.code, self.channel, event);
				}
				else {
					self.onerror(event.code, self.channel, event);
					setTimeout(self.open.bind(self), RECONNECT_INTERVAL * self.reconnectAttempts++);
				}
			}
		}

		/**
		 * Closes the reconnecting web socket.
		 */
		close() {
			if (this.socket) {
				const s = this.socket;
				this.socket = null;
				this.reconnectAttempts = null;
				s.close();
			}
		}
	}

	// Public static functions ----------------------------------------------------------------------------------------

	/**
	 * Initialize a web socket on the given channel. When connected, it will stay open and reconnect as long as channel
	 * is valid and <code>OmniFaces.Push.close()</code> hasn't explicitly been called on the same channel.
	 * @param host The host of the web socket in either the format <code>example.com:8080/context</code>, or
	 * <code>:8080/context</code>, or <code>/context</code>.
	 * If the value is falsey, then it will default to <code>window.location.host</code>.
	 * If the value starts with <code>:</code>, then <code>window.location.hostname</code> will be prepended.
	 * If the value starts with <code>/</code>, then <code>window.location.host</code> will be prepended.
	 * @param uri The uri of the web socket representing the channel name and identifier, separated by a 
	 * question mark. All open websockets on the same uri will receive the same push notification from the server.
	 * @param onopen The JavaScript event handler function that is invoked when the web socket is opened.
	 * The function will be invoked with one argument: the channel name.
	 * @param onmessage The JavaScript event handler function that is invoked when a message is received from
	 * the server. The function will be invoked with three arguments: the push message, the channel name and the raw
	 * <code>MessageEvent</code> itself.
	 * @param onerror The JavaScript event handler function that is invoked when a connection error has
	 * occurred and the web socket will attempt to reconnect. The function will be invoked with three arguments: the
	 * error reason code, the channel name and the raw <code>CloseEvent</code> itself. Note that this will not be
	 * invoked on final close of the web socket, even when the final close is caused by an error. See also
	 * <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-7.4.1">RFC 6455 section 7.4.1</a> and {@link CloseCodes} API
	 * for an elaborate list of all close codes.
	 * @param onclose The function to be invoked when the web socket is closed and will not anymore attempt
	 * to reconnect. The function will be invoked with three arguments: the close reason code, the channel name
	 * and the raw <code>CloseEvent</code> itself. Note that this will also be invoked when the close is caused by an
	 * error and that you can inspect the close reason code if an actual connection error occurred and which one (i.e.
	 * when the code is not 1000 or 1008). See also <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-7.4.1">RFC 6455
	 * section 7.4.1</a> and {@link CloseCodes} API for an elaborate list of all close codes.
	 * @param behaviors Client behavior functions to be invoked when specific message is received.
	 * @param autoconnect Whether or not to immediately open the socket. Defaults to <code>false</code>.
	 */
	export function init(host: string, uri: string, onopen: Function, onmessage: Function, onerror: Function, onclose: Function, behaviors: Record<string, Function[]>, autoconnect: boolean) {
		onclose = Util.resolveFunction(onclose);
		const channel = uri.split(/\?/)[0];

		if (!window.WebSocket) { // IE6-9.
			onclose(-1, channel);
			return;
		}

		if (!sockets[channel]) {
			sockets[channel] = new Socket(getBaseURL(host) + uri, channel, Util.resolveFunction(onopen), Util.resolveFunction(onmessage), Util.resolveFunction(onerror), onclose, behaviors);
		}

		if (autoconnect) {
			open(channel);
		}
	}

	/**
	 * Open the web socket on the given channel.
	 * @param channel The name of the web socket channel.
	 * @throws {Error} When channel is unknown.
	 */
	export function open(channel: string) {
		getSocket(channel).open();
	}

	/**
	 * Close the web socket on the given channel.
	 * @param channel The name of the web socket channel.
	 * @throws {Error} When channel is unknown.
	 */
	export function close(channel: string) {
		getSocket(channel).close();
	}

	// Private static functions ---------------------------------------------------------------------------------------

	/**
	 * Get base URL from given host.
	 * @param host The host of the web socket in either the format 
	 * <code>example.com:8080/context</code>, or <code>:8080/context</code>, or <code>/context</code>.
	 * If the value is falsey, then it will default to <code>window.location.host</code>.
	 * If the value starts with <code>:</code>, then <code>window.location.hostname</code> will be prepended.
	 * If the value starts with <code>/</code>, then <code>window.location.host</code> will be prepended.
     * @return Base URL
	 */
	function getBaseURL(host: string): string {
		host = host || "";
		const base = (!host || host.indexOf("/") == 0) ? window.location.host
				: (host.indexOf(":") == 0) ? window.location.hostname
				: "";
		return URL_PROTOCOL + base + host + URI_PREFIX + "/";
	}

	/**
	 * Get socket associated with given channel.
	 * @param channel The name of the web socket channel.
	 * @return Socket associated with given channel.
	 * @throws {Error} When channel is unknown. You may need to initialize it first via <code>init()</code> function.
	 */
	function getSocket(channel: string): Socket {
		const socket = sockets[channel];

		if (socket) {
			return socket;
		}
		else {
			throw new Error("Unknown channel: " + channel);
		}
	}

}