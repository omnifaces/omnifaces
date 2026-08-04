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
 * Manage push channels via Web Socket or SSE (Server-Sent Events).
 * This script is used by <code>&lt;o:socket&gt;</code> and <code>&lt;o:sse&gt;</code>.
 *
 * @author Bauke Scholtz
 * @see org.omnifaces.cdi.push.Socket
 * @see org.omnifaces.cdi.push.Sse
 * @since 2.3
 */
export namespace Push {

    // "Constant" fields ----------------------------------------------------------------------------------------------

    const WS_PROTOCOL = window.location.protocol.replace("http", "ws") + "//";
    const WS_URI_PREFIX = "/omnifaces.socket";
    const SSE_URI_PREFIX = "/omnifaces.sse";
    const RECONNECT_INTERVAL = 500;
    const MAX_RECONNECT_ATTEMPTS = 25;
    const REASON_EXPIRED = "Expired";
    const REASON_UNKNOWN_CHANNEL = "Unknown channel";

    // Private static fields ------------------------------------------------------------------------------------------

    const connections: Record<string, Connection> = {};

    // Private interfaces ---------------------------------------------------------------------------------------------

    interface Connection {
        open(): void;
        close(): void;
    }

    // Private static classes -----------------------------------------------------------------------------------------

    class SocketConnection implements Connection {

        readonly url: string;
        readonly channel: string;
        readonly onopen: Function;
        readonly onmessage: Function;
        readonly onerror: Function;
        readonly onclose: Function;
        readonly behaviors: Record<string, Function[]>;

        socket: WebSocket | null = null;
        reconnectAttempts: number | null = null;

        // Constructor ------------------------------------------------------------------------------------------------

        /**
         * Creates a reconnecting web socket connection. When the web socket successfully connects on first attempt,
         * then it will automatically reconnect on timeout with cumulative intervals of 500ms with a maximum of 25
         * attempts (~3 minutes). The <code>onclose</code> function will be called with the error code of the last
         * attempt.
         * @constructor
         * @param url The URL of the web socket.
         * @param channel The name of the push channel.
         * @param onopen The function to be invoked when the web socket is opened.
         * @param onmessage The function to be invoked when a message is received.
         * @param onerror The function to be invoked when a connection error has occurred and the web socket will
         * attempt to reconnect.
         * @param onclose The function to be invoked when the web socket is closed and will not anymore attempt to
         * reconnect.
         * @param behaviors Client behavior functions to be invoked when specific message is received.
         */
        constructor(
            url: string,
            channel: string,
            onopen: Function,
            onmessage: Function,
            onerror: Function,
            onclose: Function,
            behaviors: Record<string, Function[]>
        ) {
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

            this.socket.onopen = () => {
                if (self.reconnectAttempts == null) {
                    self.onopen(self.channel);
                }

                self.reconnectAttempts = 0;
            };

            this.socket.onmessage = (event: MessageEvent) => {
                const message = JSON.parse(event.data);
                self.onmessage(message, self.channel, event);
                self.behaviors[message]?.forEach(behavior => behavior());
            };

            this.socket.onclose = (event: CloseEvent) => {
                if (!self.socket
                    || (event.code == 1000 && event.reason == REASON_EXPIRED)
                    || (event.code == 1008 && event.reason == REASON_UNKNOWN_CHANNEL)
                    || (self.reconnectAttempts == null)
                    || (self.reconnectAttempts >= MAX_RECONNECT_ATTEMPTS)
                ) {
                    self.onclose(event.code, self.channel, event);
                }
                else {
                    self.onerror(event.code, self.channel, event);
                    setTimeout(self.open.bind(self), RECONNECT_INTERVAL * self.reconnectAttempts++);
                }
            };
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

    class SseConnection implements Connection {

        readonly url: string;
        readonly channel: string;
        readonly onopen: Function;
        readonly onmessage: Function;
        readonly onerror: Function;
        readonly onclose: Function;
        readonly behaviors: Record<string, Function[]>;

        eventSource: EventSource | null = null;

        // Constructor ------------------------------------------------------------------------------------------------

        /**
         * Creates an SSE connection. The browser's <code>EventSource</code> API handles reconnect natively.
         * <p>
         * The <code>onerror</code> function will be invoked with three arguments:
         * <ul>
         * <li><code>code</code>: synthetic HTTP-based close code as integer. <code>500</code> means a connection
         * error occurred and the browser will automatically attempt to reconnect.</li>
         * <li><code>channel</code>: the channel name.</li>
         * <li><code>event</code>: the raw <code>Event</code> instance from the <code>EventSource</code> API.</li>
         * </ul>
         * <p>
         * The <code>onclose</code> function will be invoked with three arguments:
         * <ul>
         * <li><code>code</code>: synthetic HTTP-based close code as integer. <code>-1</code> means the
         * <code>EventSource</code> API is not supported by the client. <code>200</code> means the server explicitly
         * closed the connection (e.g. on session or view expiry). <code>204</code> means the client explicitly closed
         * the connection via <code>OmniFaces.Push.close()</code>. <code>404</code> means the channel is unknown.</li>
         * <li><code>channel</code>: the channel name.</li>
         * <li><code>event</code>: the raw <code>Event</code> instance, if available. Present for server close
         * (<code>200</code> and <code>404</code>), absent for unsupported (<code>-1</code>) and client close
         * (<code>204</code>).</li>
         * </ul>
         * @constructor
         * @param url The URL of the SSE endpoint.
         * @param channel The name of the push channel.
         * @param onopen The function to be invoked when the SSE connection is opened.
         * @param onmessage The function to be invoked when a message is received.
         * @param onerror The function to be invoked when a connection error has occurred.
         * @param onclose The function to be invoked when the SSE connection is closed.
         * @param behaviors Client behavior functions to be invoked when specific message is received.
         */
        constructor(
            url: string,
            channel: string,
            onopen: Function,
            onmessage: Function,
            onerror: Function,
            onclose: Function,
            behaviors: Record<string, Function[]>
        ) {
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
         * Opens the SSE connection.
         */
        open() {
            if (this.eventSource) {
                return;
            }

            this.eventSource = new EventSource(this.url);

            this.eventSource.onopen = () => {
                this.onopen(this.channel);
            };

            this.eventSource.onmessage = (event: MessageEvent) => {
                const message = JSON.parse(event.data);
                this.onmessage(message, this.channel, event);
                this.behaviors[message]?.forEach(behavior => behavior());
            };

            this.eventSource.addEventListener("close", (event: MessageEvent) => {
                const code = parseInt(event.data);
                this.eventSource?.close();
                this.eventSource = null;
                this.onclose(code, this.channel, event);
            });

            this.eventSource.onerror = (event: Event) => {
                if (this.eventSource?.readyState == EventSource.CLOSED) {
                    this.eventSource = null;
                    this.onclose(200, this.channel, event);
                }
                else {
                    this.onerror(500, this.channel, event);
                }
            };
        }

        /**
         * Closes the SSE connection.
         */
        close() {
            if (this.eventSource) {
                const es = this.eventSource;
                this.eventSource = null;
                es.close();
                this.onclose(204, this.channel);
            }
        }
    }

    // Public static functions ----------------------------------------------------------------------------------------

    /**
     * Initialize a push channel using either Web Socket or SSE transport. When connected, it will stay open and
     * reconnect as long as channel is valid and <code>OmniFaces.Push.close()</code> hasn't explicitly been called
     * on the same channel.
     * @param sse Whether to use SSE instead of Web Socket.
     * @param host The host of the push connection. For Web Socket this is in either the format
     * <code>example.com:8080/context</code>, or <code>:8080/context</code>, or <code>/context</code>.
     * For SSE this is the context path (e.g. <code>/context</code>).
     * @param uri The uri representing the channel name and identifier, separated by a question mark.
     * All open connections on the same uri will receive the same push notification from the server.
     * @param onopen The JavaScript event handler function that is invoked when the connection is opened.
     * @param onmessage The JavaScript event handler function that is invoked when a message is received from
     * the server.
     * @param onerror The JavaScript event handler function that is invoked when a connection error has occurred.
     * @param onclose The function to be invoked when the connection is closed.
     * @param behaviors Client behavior functions to be invoked when specific message is received.
     * @param autoconnect Whether or not to immediately open the connection. Defaults to <code>true</code>.
     */
    export function init(
        sse: boolean,
        host: string,
        uri: string,
        onopen: Function,
        onmessage: Function,
        onerror: Function,
        onclose: Function,
        behaviors: Record<string, Function[]>,
        autoconnect?: boolean
    ) {
        onclose = Util.resolveFunction(onclose);
        const channel = uri.split(/\?/)[0];

        if (sse ? !window.EventSource : !window.WebSocket) {
            onclose(-1, channel);
            return;
        }

        if (!connections[channel]) {
            onopen = Util.resolveFunction(onopen);
            onmessage = Util.resolveFunction(onmessage);
            onerror = Util.resolveFunction(onerror);

            if (sse) {
                connections[channel] = new SseConnection(getSseURL(host, uri), channel, onopen, onmessage, onerror, onclose, behaviors);
            }
            else {
                connections[channel] = new SocketConnection(getSocketURL(host, uri), channel, onopen, onmessage, onerror, onclose, behaviors);
            }
        }

        if (autoconnect !== false) {
            open(channel);
        }
    }

    /**
     * Open the push connection on the given channel.
     * @param channel The name of the push channel.
     * @throws {Error} When channel is unknown.
     */
    export function open(channel: string) {
        getConnection(channel).open();
    }

    /**
     * Close the push connection on the given channel.
     * @param channel The name of the push channel.
     * @throws {Error} When channel is unknown.
     */
    export function close(channel: string) {
        getConnection(channel).close();
    }

    // Private static functions ---------------------------------------------------------------------------------------

    /**
     * Get SSE URL from given host.
     * @param host The host of the SSE endpoint in the format <code>/context</code>.
     * @param uri The uri representing the channel name and identifier, separated by a question mark.
     * @return SSE URL
     */
    function getSseURL(host: string, uri: string): string {
        return host + SSE_URI_PREFIX + "/" + uri;
    }

    /**
     * Get web socket URL from given host.
     * @param host The host of the web socket endpoint in either the format
     * <code>example.com:8080/context</code>, or <code>:8080/context</code>, or <code>/context</code>.
     * If the value is falsey, then it will default to <code>window.location.host</code>.
     * If the value starts with <code>:</code>, then <code>window.location.hostname</code> will be prepended.
     * If the value starts with <code>/</code>, then <code>window.location.host</code> will be prepended.
     * @param uri The uri representing the channel name and identifier, separated by a question mark.
     * @return Web socket URL
     */
    function getSocketURL(host: string, uri: string): string {
        host = host ?? "";
        const base = (!host || host.startsWith("/")) ? window.location.host
            : (host.startsWith(":")) ? window.location.hostname
                : "";
        return `${WS_PROTOCOL}${base}${host}${WS_URI_PREFIX}/${uri}`;
    }

    /**
     * Get connection associated with given channel.
     * @param channel The name of the channel.
     * @return Connection associated with given channel.
     * @throws {Error} When channel is unknown. You may need to initialize it first via <code>init()</code> function.
     */
    function getConnection(channel: string): Connection {
        const connection = connections[channel];

        if (connection) {
            return connection;
        }
        else {
            throw new Error(`Unknown channel: ${channel}`);
        }
    }

}
