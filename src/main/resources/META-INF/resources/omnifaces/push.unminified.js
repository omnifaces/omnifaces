/*
 * Copyright 2016 OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Manage web socket push channels. This script is used by <code>&lt;o:socket&gt;</code>.
 * 
 * @author Bauke Scholtz
 * @see org.omnifaces.cdi.push.Socket
 * @since 2.3
 */
OmniFaces.Push = (function(Util, window) {

	// "Constant" fields ----------------------------------------------------------------------------------------------

	var URL_PROTOCOL = window.location.protocol.replace("http", "ws") + "//";
	var URI_PREFIX = "/omnifaces.push";
	var RECONNECT_INTERVAL = 500;
	var MAX_RECONNECT_ATTEMPTS = 25;
	var REASON_EXPIRED = "Expired";

	// Private static fields ------------------------------------------------------------------------------------------

	var sockets = {};
	var self = {};

	// Private constructor functions ----------------------------------------------------------------------------------

	/**
	 * Creates a reconnecting web socket. When the web socket successfully connects on first attempt, then it will
	 * automatically reconnect on timeout with cumulative intervals of 500ms with a maximum of 25 attempts (~3 minutes).
	 * The <code>onclose</code> function will be called with the error code of the last attempt.
	 * @constructor
	 * @param {string} url The URL of the web socket 
	 * @param {string} channel The name of the web socket channel.
	 * @param {function} onopen The function to be invoked when the web socket is opened.
	 * @param {function} onmessage The function to be invoked when a message is received.
	 * @param {function} onclose The function to be invoked when the web socket is closed.
	 */
	function Socket(url, channel, onopen, onmessage, onclose) {

		// Private fields -----------------------------------------------------------------------------------------

		var socket;
		var reconnectAttempts;
		var self = this;

		// Public functions ---------------------------------------------------------------------------------------

		/**
		 * Opens the reconnecting web socket.
		 */
		self.open = function() {
			if (socket && socket.readyState == 1) {
				return;
			}

			socket = new WebSocket(url);

			socket.onopen = function(event) {
				if (reconnectAttempts == null) {
					onopen(channel);
				}

				reconnectAttempts = 0;
			}

			socket.onmessage = function(event) {
				onmessage(JSON.parse(event.data), channel, event);
			}

			socket.onclose = function(event) {
				if (!socket
					|| (event.code == 1000 && event.reason == REASON_EXPIRED) 
					|| (event.code == 1008) 
					|| (reconnectAttempts == null) 
					|| (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS))
				{
					onclose(event.code, channel, event);
				}
				else {
					setTimeout(self.open, RECONNECT_INTERVAL * reconnectAttempts++);
				}
			}
		}

		/**
		 * Closes the reconnecting web socket.
		 */
		self.close = function() {
			if (socket) {
				var s = socket;
				socket = null;
				s.close();
			}
		}

	}

	// Public static functions ----------------------------------------------------------------------------------------

	/**
	 * Initialize a web socket on the given channel. When connected, it will stay open and reconnect as long as channel
	 * is valid and <code>OmniFaces.Push.close()</code> hasn't explicitly been called on the same channel.
	 * @param {string} host The host of the web socket in either the format <code>example.com:8080/context</code>, or
	 * <code>:8080/context</code>, or <code>/context</code>.
	 * If the value is falsey, then it will default to <code>window.location.host</code>.
	 * If the value starts with <code>:</code>, then <code>window.location.hostname</code> will be prepended.
	 * If the value starts with <code>/</code>, then <code>window.location.host</code> will be prepended.
	 * @param {string} uri The uri of the web socket representing the channel name and identifier, separated by a 
	 * question mark. All open websockets on the same uri will receive the same push notification from the server.
	 * @param {function} onopen The JavaScript event handler function that is invoked when the web socket is opened.
	 * The function will be invoked with one argument: the channel name.
	 * @param {function} onmessage The JavaScript event handler function that is invoked when a message is received from
	 * the server. The function will be invoked with three arguments: the push message, the channel name and the raw
	 * <code>MessageEvent</code> itself.
	 * @param {function} onclose The JavaScript event handler function that is invoked when the web socket is closed.
	 * The function will be invoked with three arguments: the close reason code, the channel name and the raw
	 * <code>CloseEvent</code> itself. Note that this will also be invoked on errors and that you can inspect the
	 * close reason code if an error occurred and which one (i.e. when the code is not 1000). See also
	 * <a href="http://tools.ietf.org/html/rfc6455#section-7.4.1">RFC 6455 section 7.4.1</a> and
	 * <a href="http://docs.oracle.com/javaee/7/api/javax/websocket/CloseReason.CloseCodes.html">CloseCodes</a> API
	 * for an elaborate list.
	 * @param {boolean} autoconnect Whether or not to immediately open the socket. Defaults to <code>false</code>.
	 */
	self.init = function(host, uri, onopen, onmessage, onclose, autoconnect) {
		onclose = Util.resolveFunction(onclose);
		var channel = uri.split(/\?/)[0];

		if (!window.WebSocket) { // IE6-9.
			onclose(-1, channel);
			return;
		}

		if (!sockets[channel]) {
			sockets[channel] = new Socket(getBaseURL(host) + uri, channel, Util.resolveFunction(onopen), Util.resolveFunction(onmessage), onclose);
		}

		if (autoconnect) {
			self.open(channel);
		}
	}

	/**
	 * Open the web socket on the given channel.
	 * @param {string} channel The name of the web socket channel.
	 * @throws {Error} When channel is unknown.
	 */
	self.open = function(channel) {
		getSocket(channel).open();
	}

	/**
	 * Close the web socket on the given channel.
	 * @param {string} channel The name of the web socket channel.
	 * @throws {Error} When channel is unknown.
	 */
	self.close = function(channel) {
		getSocket(channel).close();
	}

	// Private static functions ---------------------------------------------------------------------------------------

	/**
	 * Get base URL from given host.
	 * @param {string} host The host of the web socket in either the format 
	 * <code>example.com:8080/context</code>, or <code>:8080/context</code>, or <code>/context</code>.
	 * If the value is falsey, then it will default to <code>window.location.host</code>.
	 * If the value starts with <code>:</code>, then <code>window.location.hostname</code> will be prepended.
	 * If the value starts with <code>/</code>, then <code>window.location.host</code> will be prepended.
	 */
	function getBaseURL(host) {
		host = host || "";
		var base = (!host || host.indexOf("/") == 0) ? window.location.host
				: (host.indexOf(":") == 0) ? window.location.hostname
				: "";
		return URL_PROTOCOL + base + host + URI_PREFIX + "/";
	}

	/**
	 * Get socket associated with given channel.
	 * @param {string} channel The name of the web socket channel.
	 * @return {Socket} Socket associated with given channel.
	 * @throws {Error} When channel is unknown. You may need to initialize it first via <code>init()</code> function.
	 */
	function getSocket(channel) {
		var socket = sockets[channel];

		if (socket) {
			return socket;
		}
		else {
			throw new Error("Unknown channel: " + channel);
		}
	}

	// Expose self to public ------------------------------------------------------------------------------------------

	return self;

})(OmniFaces.Util, window);