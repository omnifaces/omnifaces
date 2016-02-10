var OmniFaces=OmniFaces||{};
OmniFaces.Util={addOnloadListener:function(n){if("complete"===document.readyState)setTimeout(n);else if(window.addEventListener)window.addEventListener("load",n,!1);else if(window.attachEvent)window.attachEvent("onload",n);else if("function"==typeof window.onload){var o=window.onload;window.onload=function(){o(),n()}}else window.onload=n},resolveFunction:function(n){return"function"!=typeof n&&(n=window[n]||function(){}),n}};
OmniFaces.Highlight=function(e){function n(){for(var n=e.getElementsByTagName("LABEL"),t={},a=0;a<n.length;a++){var r=n[a],l=r.htmlFor;l&&(t[l]=r)}return t}function t(n){var t=e.getElementById(n);if(!t){var a=e.getElementsByName(n);a&&a.length&&(t=a[0])}return t}var a={};return a.apply=function(e,a,r){for(var l=n(),i=0;i<e.length;i++){var m=t(e[i]);if(m){m.className+=" "+a;var c=l[m.id];c&&(c.className+=" "+a),r&&(m.focus(),r=!1)}}},a}(document);
OmniFaces.DeferredScript=function(e,n){function r(e){if(!(0>e||e>=t.length)){var o=t[e],c=n.createElement("script"),a=n.head||n.documentElement;c.async=!0,c.src=o.url,c.onerror=function(){o.error()},c.onload=c.onreadystatechange=function(n,t){(t||!c.readyState||/loaded|complete/.test(c.readyState))&&(c.onload=c.onreadystatechange=null,t?c.onerror():o.success(),c=null,r(e+1))},o.begin(),a.insertBefore(c,null)}}var t=[],o={};return o.add=function(n,o,c,a){t.push({url:n,begin:e.resolveFunction(o),success:e.resolveFunction(c),error:e.resolveFunction(a)}),1==t.length&&e.addOnloadListener(function(){r(0)})},o}(OmniFaces.Util,document);
OmniFaces.Unload=function(n,t){function e(){for(var n=0;n<t.forms.length;n++){var e=t.forms[n][i];if(e)return e.value}return null}function a(n,t,e){n.addEventListener?n.addEventListener(t,e,!1):n.attachEvent&&n.attachEvent("on"+t,e)}var i="javax.faces.ViewState",o=!1,r={};return r.init=function(u){if(n.XMLHttpRequest){var c=e();c&&(a(n,"beforeunload",function(){if(o)return void(o=!1);try{var t=new XMLHttpRequest;t.open("POST",n.location.href.split(/[?#;]/)[0],!1),t.setRequestHeader("Content-type","application/x-www-form-urlencoded"),t.send("omnifaces.event=unload&id="+u+"&"+i+"="+c)}catch(e){}}),a(t,"submit",function(){r.disable()}))}},r.disable=function(){o=!0},r}(window,document);

OmniFaces.Push = (function(Util, window) {

	// "Constant" fields ----------------------------------------------------------------------------------------------

	var URL_PROTOCOL = window.location.protocol.replace("http", "ws") + "//";
	var URI_PREFIX = "/omnifaces.push";
	var RECONNECT_INTERVAL = 500;
	var MAX_RECONNECT_ATTEMPTS = 25;

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
	 * @param {function} onmessage The function to be invoked when a message is received.
	 * @param {function} onclose The function to be invoked when the web socket is closed.
	 */
	function Socket(url, channel, onmessage, onclose) {

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
				reconnectAttempts = 0;
			}

			socket.onmessage = function(event) {
				onmessage(JSON.parse(event.data), channel, event);
			}

			socket.onclose = function(event) {
				if (!socket || event.code == 1008 || reconnectAttempts == null || reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
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
	self.init = function(host, uri, onmessage, onclose, autoconnect) {
		onclose = Util.resolveFunction(onclose);
		var channel = uri.split(/\?/)[0];

		if (!window.WebSocket) { // IE6-9.
			onclose(-1, channel);
			return;
		}

		if (!sockets[channel]) {
			sockets[channel] = new Socket(getBaseURL(host) + uri, channel, Util.resolveFunction(onmessage), onclose);
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
		var base = (!host || host.indexOf("/") == 0) ? window.location.host + host
				: (host.indexOf(":") == 0) ? window.location.hostname + host
				: host;
		return URL_PROTOCOL + base + URI_PREFIX + "/";
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