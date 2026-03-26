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

import static java.lang.Boolean.TRUE;
import static org.omnifaces.cdi.PushContext.SSE_URI_PREFIX;
import static org.omnifaces.cdi.push.PushChannelManager.ESTIMATED_TOTAL_CHANNELS;
import static org.omnifaces.config.OmniFaces.OMNIFACES_LIBRARY_NAME;
import static org.omnifaces.config.OmniFaces.OMNIFACES_SCRIPT_NAME;
import static org.omnifaces.util.FacesLocal.getRequestContextPath;
import static org.omnifaces.util.FacesLocal.getViewAttribute;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;

import jakarta.enterprise.event.Observes;
import jakarta.faces.FacesException;
import jakarta.faces.application.ResourceDependency;
import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.ServletContext;

import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;
import org.omnifaces.cdi.push.SseEvent.Closed;
import org.omnifaces.cdi.push.SseEvent.Opened;
import org.omnifaces.cdi.push.SseEvent.Switched;
import org.omnifaces.config.OmniFaces;
import org.omnifaces.util.Beans;
import org.omnifaces.util.Json;

/**
 * <p>
 * The <code>&lt;o:sse&gt;</code> is an {@link UIComponent} which opens an one-way (server to client) SSE (Server-Sent
 * Events) based push connection in client side which can be reached from server side via {@link PushContext} interface
 * injected in any CDI/container managed artifact via <code>&#64;</code>{@link Push}<code>(type=SSE)</code> annotation.
 *
 * <h2 id="prerequisites"><a href="#prerequisites">Prerequisites</a></h2>
 * <p>
 * All servlet filters mapped on {@code /*} must have {@code @WebFilter(asyncSupported=true)} or
 * {@code web.xml <async-supported>true</async-supported>} for SSE to work. In case that's not possible for your filter
 * (e.g because it relies on some request/thread-specific state after invoking {@code chain.doFilter()} such as DB
 * connection, locked/shared resource, {@code ThreadLocal}, etc), then you'll need to map it on a more specific
 * URL-pattern excluding {@value PushContext#SSE_URI_PREFIX} and create yet another filter on <code>/*</code> which
 * forwards to that filter when the request path does not match {@value PushContext#SSE_URI_PREFIX}.
 * <p>
 * The server (and proxy, if any) must also support HTTP/2 and have it enabled. Otherwise it will fall back to HTTP/1.1
 * which limits concurrent connections per origin (e.g. Chrome limits to 6). In such case, multiple SSE connections
 * across multiple tabs may hit this limit and queue further HTTP requests (including Ajax requests!), which has the
 * consequence that {@code <f:ajax>} will simply stop working. With HTTP/2, SSE streams are multiplexed over a single
 * TCP connection.
 *
 * <h2 id="sse-vs-websocket"><a href="#sse-vs-websocket">SSE vs WebSocket</a></h2>
 * <p>
 * Both <code>&lt;o:sse&gt;</code> and <code>&lt;o:socket&gt;</code> provide one-way (server to client) push.
 * The table below summarizes the key differences to help you choose the right transport.
 *
 * <table>
 * <caption>SSE (<code>&lt;o:sse&gt;</code>) vs WebSocket (<code>&lt;o:socket&gt;</code>)</caption>
 * <thead>
 * <tr><th></th><th>SSE (<code>&lt;o:sse&gt;</code>)</th><th>WebSocket (<code>&lt;o:socket&gt;</code>)</th></tr>
 * </thead>
 * <tbody>
 * <tr><td><b>Transport</b></td><td>Plain HTTP</td><td>WebSocket protocol upgrade</td></tr>
 * <tr><td><b>Dependencies</b></td><td>None (async servlet)</td><td>Requires <code>jakarta.websocket</code></td></tr>
 * <tr><td><b>Proxy/CDN/firewall</b></td><td>Works through HTTP infrastructure</td><td>May be blocked by proxies or corporate firewalls</td></tr>
 * <tr><td><b>Reconnect</b></td><td>Built-in (<code>EventSource</code> API)</td><td>Manual (with cumulative backoff)</td></tr>
 * <tr><td><b>HTTP/2 multiplexing</b></td><td>Yes (shares TCP connection)</td><td>No (separate TCP connection)</td></tr>
 * <tr><td><b>Connection limit</b></td><td>Max 6 per origin without HTTP/2</td><td>Not affected (protocol upgrade)</td></tr>
 * <tr><td><b>CDI events</b></td><td>{@link SseEvent} on open/close/switch</td><td>{@link SocketEvent} on open/close/switch</td></tr>
 * <tr><td><b>Dynamic connect/disconnect</b></td><td>Not available</td><td><code>connected</code> attribute</td></tr>
 * </tbody>
 * </table>
 * <p>
 * <b>When to prefer SSE:</b> for typical unidirectional push use cases (notifications, live updates, dashboards) where
 * HTTP infrastructure compatibility and simplicity are important. SSE is the simpler and more robust choice for most
 * server-to-client push scenarios.
 * <p>
 * <b>When to prefer WebSocket:</b> when you need dynamic connect/disconnect via the {@code connected} attribute, or when
 * your server does not support HTTP/2.
 *
 *
 * <h2 id="configuration"><a href="#configuration">Configuration</a></h2>
 * <p>
 * No explicit configuration is needed. The SSE endpoint is automatically registered during application startup when at
 * least one <code>&#64;</code>{@link Push}<code>(type=SSE)</code> qualified injection point is detected.
 *
 *
 * <h2 id="usage-client"><a href="#usage-client">Usage (client)</a></h2>
 * <p>
 * Declare <strong><code>&lt;o:sse&gt;</code></strong> tag in the Faces view with at least a
 * <strong><code>channel</code></strong> name and an <strong><code>onmessage</code></strong> JavaScript listener
 * function. The channel name may not be an EL expression and it may only contain alphanumeric characters, hyphens,
 * underscores and periods.
 * <p>
 * Here's an example which refers an existing JavaScript listener function (do not include the parentheses!).
 * <pre>
 * &lt;o:sse channel="someChannel" onmessage="sseListener" /&gt;
 * </pre>
 * <pre>
 * function sseListener(message, channel, event) {
 *     console.log(message);
 * }
 * </pre>
 * <p>
 * Here's an example which declares an inline JavaScript listener function.
 * <pre>
 * &lt;o:sse channel="someChannel" onmessage="function(message) { console.log(message); }" /&gt;
 * </pre>
 * <p>
 * The <code>onmessage</code> JavaScript listener function will be invoked with three arguments:
 * <ul>
 * <li><code>message</code>: the push message as JSON object.</li>
 * <li><code>channel</code>: the channel name, useful in case you intend to have a global listener.</li>
 * <li><code>event</code>: the raw <a href="https://developer.mozilla.org/en-US/docs/Web/API/MessageEvent"><code>
 * MessageEvent</code></a> instance, useful in case you intend to inspect it.</li>
 * </ul>
 * <p>
 * The SSE connection is automatically established when the page loads and will auto-reconnect when the connection is
 * lost due to e.g. a network error or server restart. This is a built-in feature of the browser's
 * <a href="https://developer.mozilla.org/en-US/docs/Web/API/EventSource"><code>EventSource</code></a> API. The SSE
 * connection will be implicitly closed once the document is unloaded (e.g. navigating away, close of browser window/tab, etc).
 * <p>
 * In order to successfully reconnect after a server restart, or when switching to a new server node, you need to ensure
 * that session persistence is enabled on the server.
 *
 *
 * <h2 id="usage-server"><a href="#usage-server">Usage (server)</a></h2>
 * <p>
 * In WAR side, you can inject <strong>{@link PushContext}</strong> via <strong><code>&#64;</code>{@link Push}<code>(type=SSE)</code></strong>
 * annotation on the given channel name in any CDI/container managed artifact such as <code>&#64;Named</code>,
 * <code>&#64;WebServlet</code>, etc wherever you'd like to send a push message and then invoke
 * <strong>{@link PushContext#send(Object)}</strong> with any Java object representing the push message.
 * <pre>
 * &#64;Inject &#64;Push(type=SSE)
 * private PushContext someChannel;
 *
 * public void sendMessage(Object message) {
 *     someChannel.send(message);
 * }
 * </pre>
 * <p>
 * By default the name of the channel is taken from the name of the variable into which injection takes place. The
 * channel name can be optionally specified via the <code>channel</code> attribute. The example below injects the push
 * context for channel name <code>foo</code> into a variable named <code>bar</code>.
 * <pre>
 * &#64;Inject &#64;Push(type=SSE, channel="foo")
 * private PushContext bar;
 * </pre>
 * <p>
 * The message object will be encoded as JSON and be delivered as <code>message</code> argument of the
 * <code>onmessage</code> JavaScript listener function associated with the <code>channel</code> name. It can be a
 * plain vanilla <code>String</code>, but it can also be a collection, map and even a javabean. For supported argument
 * types, see also {@link Json#encode(Object)}.
 *
 *
 * <h2 id="scopes-and-users"><a href="#scopes-and-users">Scopes and users</a></h2>
 * <p>
 * By default the SSE channel is <code>application</code> scoped, i.e. any view/session throughout the web application
 * having the same SSE channel open will receive the same push message. The push message can be sent by all users
 * and the application itself. This is useful for application-wide feedback triggered by site itself such as real time
 * updates of a certain page (e.g. site-wide statistics, top100 lists, stock updates, etc).
 * <p>
 * The optional <strong><code>scope</code></strong> attribute can be set to <code>session</code> to restrict the push
 * messages to all views in the current user session only. The push message can only be sent by the user itself and not
 * by the application. This is useful for session-wide feedback triggered by user itself (e.g. as result of asynchronous
 * tasks triggered by user specific action).
 * <pre>
 * &lt;o:sse channel="someChannel" scope="session" ... /&gt;
 * </pre>
 * <p>
 * The <code>scope</code> attribute can also be set to <code>view</code> to restrict the push messages to the current
 * view only. The push message will not show up in other views in the same session even if it's the same URL. The push
 * message can only be sent by the user itself and not by the application. This is useful for view-wide feedback
 * triggered by user itself (e.g. progress bar tied to a user specific action on current view).
 * <pre>
 * &lt;o:sse channel="someChannel" scope="view" ... /&gt;
 * </pre>
 * <p>
 * The <code>scope</code> attribute may not be an EL expression and allowed values are <code>application</code>,
 * <code>session</code> and <code>view</code>, case insensitive.
 * <p>
 * Additionally, the optional <strong><code>user</code></strong> attribute can be set to the unique identifier of the
 * logged-in user, usually the login name or the user ID. This way the push message can be targeted to a specific user
 * and can also be sent by other users and the application itself. The value of the <code>user</code> attribute must at
 * least implement {@link Serializable} and have a low memory footprint, so putting entire user entity is not
 * recommended.
 * <p>
 * E.g. when you're using container managed authentication or a related framework/library:
 * <pre>
 * &lt;o:sse channel="someChannel" user="#{request.remoteUser}" ... /&gt;
 * </pre>
 * <p>
 * Or when you have a custom user entity around in EL as <code>#{someLoggedInUser}</code> which has an <code>id</code>
 * property representing its identifier:
 * <pre>
 * &lt;o:sse channel="someChannel" user="#{someLoggedInUser.id}" ... /&gt;
 * </pre>
 * When the <code>user</code> attribute is specified, then the <code>scope</code> defaults to <code>session</code> and
 * cannot be set to <code>application</code>. It can be set to <code>view</code>, but this is kind of unusual and should
 * only be used if the logged-in user represented by <code>user</code> has a shorter lifetime than the HTTP session
 * (e.g. when your application allows changing a logged-in user during same HTTP session without invaliding it &mdash;
 * which is in turn poor security practice). If in such case a session scoped channel is reused, undefined behavior may
 * occur when user-targeted push message is sent. It may target previously logged-in user only. This can be solved by
 * setting the scope to <code>view</code>, but better is to fix the logout to invalidate the HTTP session altogether.
 * <p>
 * When the <code>user</code> attribute is an EL expression and it changes during an ajax request, then the SSE
 * user will be actually switched, even though you did not cover the <code>&lt;o:sse&gt;</code> component in any ajax
 * render/update. So make sure the value is tied to at least a view scoped property in case you intend to control it
 * during the view scope.
 * <p>
 * In the server side, the push message can be targeted to the user specified in the <code>user</code> attribute via
 * <strong>{@link PushContext#send(Object, Serializable)}</strong>. The push message can be sent by all users and the
 * application itself. This is useful for user-specific feedback triggered by other users (e.g. chat, admin messages,
 * etc) or by application's background tasks (e.g. notifications, event listeners, etc).
 * <pre>
 * &#64;Inject &#64;Push(type=SSE)
 * private PushContext someChannel;
 *
 * public void sendMessage(Object message, User recipientUser) {
 *     Long recipientUserId = recipientUser.getId();
 *     someChannel.send(message, recipientUserId);
 * }
 * </pre>
 * <p>
 * Multiple users can be targeted by passing a {@link Collection} holding user identifiers to
 * <strong>{@link PushContext#send(Object, Collection)}</strong>.
 * <pre>
 * public void sendMessage(Object message, Group recipientGroup) {
 *     Collection&lt;Long&gt; recipientUserIds = recipientGroup.getUserIds();
 *     someChannel.send(message, recipientUserIds);
 * }
 * </pre>
 *
 *
 * <h2 id="channels"><a href="#channels">Channel design hints</a></h2>
 * <p>
 * You can declare multiple push channels on different scopes with or without user target throughout the application.
 * Be however aware that the same channel name can easily be reused across multiple views, even if it's view scoped.
 * It's more efficient if you use as few different channel names as possible and tie the channel name to a specific
 * push channel scope/user combination, not to a specific Faces view. In case you intend to have multiple view scoped
 * channels for different purposes, best is to use only one view scoped channel and have a global JavaScript listener
 * which can distinguish its task based on the delivered message. E.g. by sending the message in server as below:
 * <pre>
 * Map&lt;String, Object&gt; message = new HashMap&lt;&gt;();
 * message.put("functionName", "someFunction");
 * message.put("functionData", functionData); // Can be Map or Bean.
 * someChannel.send(message);
 * </pre>
 * <p>
 * Which is processed in the <code>onmessage</code> JavaScript listener function as below:
 * <pre>
 * function someSseListener(message) {
 *     window[message.functionName](message.functionData);
 * }
 *
 * function someFunction(data) {
 *     // ...
 * }
 *
 * function otherFunction(data) {
 *     // ...
 * }
 *
 * // ...
 * </pre>
 *
 *
 * <h2 id="events-client"><a href="#events-client">Events (client)</a></h2>
 * <p>
 * The optional <strong><code>onopen</code></strong> JavaScript listener function can be used to listen on open of an
 * SSE connection in client side. It will be invoked with one argument:
 * <ul>
 * <li><code>channel</code>: the channel name, useful in case you intend to have a global listener.</li>
 * </ul>
 * <pre>
 * &lt;o:sse ... onopen="sseOpenListener" /&gt;
 * </pre>
 * <pre>
 * function sseOpenListener(channel) {
 *     // ...
 * }
 * </pre>
 * <p>
 * The optional <strong><code>onerror</code></strong> JavaScript listener function can be used to listen on a
 * connection error whereby the browser will automatically attempt to reconnect. This will be invoked when a transient
 * connection error occurs (e.g. temporary network interruption) while the browser's <code>EventSource</code> is still
 * attempting to reconnect. This will <em>not</em> be invoked when the server has explicitly closed the connection
 * (e.g. unknown channel, or expired session or view), or when the <code>EventSource</code> API is not supported.
 * Instead, the <code>onclose</code> will be invoked.
 * <pre>
 * &lt;o:sse ... onerror="sseErrorListener" /&gt;
 * </pre>
 * <pre>
 * function sseErrorListener(code, channel, event) {
 *     if (code == 500) {
 *         // Connection error. The browser will automatically attempt to reconnect.
 *     }
 * }
 * </pre>
 * <p>
 * The <code>onerror</code> JavaScript listener function will be invoked with three arguments:
 * <ul>
 * <li><code>code</code>: synthetic HTTP-based close code as integer. Currently this is always <code>500</code>
 * (connection error, browser will auto-reconnect).</li>
 * <li><code>channel</code>: the channel name, useful in case you intend to have a global listener.</li>
 * <li><code>event</code>: the raw <a href="https://developer.mozilla.org/en-US/docs/Web/API/EventSource/error_event">
 * <code>Event</code></a> instance from the <code>EventSource</code> API. Note that unlike WebSocket's
 * <code>CloseEvent</code>, the SSE error event does not carry a close code or reason.</li>
 * </ul>
 * <p>
 * The optional <strong><code>onclose</code></strong> JavaScript listener function can be used to listen on the final
 * close of an SSE connection. This will be invoked when the <code>EventSource</code> API is not supported by the
 * client, or when the server has explicitly closed the connection (e.g. on session or view expiry, or unknown channel), or when the client
 * has explicitly closed the connection via <code>OmniFaces.Push.close(channel)</code>. This will <em>not</em> be
 * invoked when the browser can make an auto-reconnect attempt. Instead, the <code>onerror</code> will be invoked.
 * <pre>
 * &lt;o:sse ... onclose="sseCloseListener" /&gt;
 * </pre>
 * <pre>
 * function sseCloseListener(code, channel, event) {
 *     if (code == -1) {
 *         // EventSource API not supported by client.
 *     } else if (code == 200) {
 *         // Server explicitly closed the connection (e.g. expired session or view).
 *     } else if (code == 204) {
 *         // Client explicitly closed the connection via OmniFaces.Push.close().
 *     } else if (code == 404) {
 *         // Unknown channel (e.g. randomly guessed or spoofed by endusers or manually reconnected after session expiry).
 *     } else {
 *         // Abnormal close reason (as result of an error).
 *     }
 * }
 * </pre>
 * <p>
 * The <code>onclose</code> JavaScript listener function will be invoked with three arguments:
 * <ul>
 * <li><code>code</code>: synthetic HTTP-based close code as integer. If this is <code>-1</code>, then the
 * <code>EventSource</code> API is simply not
 * <a href="https://caniuse.com/eventsource">supported</a> by the client. If this is <code>200</code>, then the server
 * explicitly closed the connection (e.g. due to an expired session or view). If this is <code>204</code>, then the
 * client explicitly closed the connection via <code>OmniFaces.Push.close(channel)</code>. If this is <code>404</code>,
 * then the channel is unknown (e.g. randomly guessed or spoofed by endusers or manually reconnected after the session
 * is expired).</li>
 * <li><code>channel</code>: the channel name, useful in case you intend to have a global listener.</li>
 * <li><code>event</code>: the raw <a href="https://developer.mozilla.org/en-US/docs/Web/API/EventSource/error_event">
 * <code>Event</code></a> instance, if available. This is present when the server closed the connection (codes
 * <code>200</code> and <code>404</code>), but absent when the <code>EventSource</code> API is not supported (code
 * <code>-1</code>) or the client explicitly closed the connection (code <code>204</code>).</li>
 * </ul>
 *
 *
 * <h2 id="events-server"><a href="#events-server">Events (server)</a></h2>
 * <p>
 * When an SSE connection has been opened, a new CDI <strong>{@link SseEvent}</strong> will be fired with
 * <strong><code>&#64;</code>{@link Opened}</strong> qualifier. When the <code>user</code> attribute of the
 * <code>&lt;o:sse&gt;</code> changes, a new CDI <strong>{@link SseEvent}</strong> will be fired with
 * <strong><code>&#64;</code>{@link Switched}</strong> qualifier. When an SSE connection has been closed, a new CDI
 * {@link SseEvent} will be fired with <strong><code>&#64;</code>{@link Closed}</strong> qualifier. They can only be
 * observed and collected in an application scoped CDI bean as below. Observing in a request/view/session scoped CDI
 * bean is not possible as there's no means of a HTTP request anywhere at that moment.
 * <pre>
 * &#64;ApplicationScoped
 * public class SseObserver {
 *
 *     public void onOpen(&#64;Observes &#64;Opened SseEvent event) {
 *         String channel = event.getChannel(); // Returns &lt;o:sse channel&gt;.
 *         Long userId = event.getUser(); // Returns &lt;o:sse user&gt;, if any.
 *         // Do your thing with it. E.g. collecting them in a concurrent/synchronized collection.
 *         // Do note that a single person can open multiple SSE connections on same channel/user.
 *     }
 *
 *     public void onSwitch(&#64;Observes &#64;Switched SseEvent event) {
 *         String channel = event.getChannel(); // Returns &lt;o:sse channel&gt;.
 *         Long currentUserId = event.getUser(); // Returns current &lt;o:sse user&gt;, if any.
 *         Long previousUserId = event.getPreviousUser(); // Returns previous &lt;o:sse user&gt;, if any.
 *         // Do your thing with it. E.g. updating in a concurrent/synchronized collection.
 *     }
 *
 *     public void onClose(&#64;Observes &#64;Closed SseEvent event) {
 *         String channel = event.getChannel(); // Returns &lt;o:sse channel&gt;.
 *         Long userId = event.getUser(); // Returns &lt;o:sse user&gt;, if any.
 *         // Do your thing with it. E.g. removing them from collection.
 *     }
 *
 * }
 * </pre>
 * <p>
 * You could take the opportunity to send another push message to an application scoped channel, e.g. "User X has been
 * logged in" (or out) when a session scoped channel is opened (or closed).
 *
 *
 * <h2 id="security"><a href="#security">Security considerations</a></h2>
 * <p>
 * If the SSE channel is declared in a page which is only restricted to logged-in users with a specific role, then you may
 * want to add the URL of the SSE connection to the set of restricted URLs.
 * <p>
 * The SSE request URL is composed of the URI prefix <strong><code>/omnifaces.sse/</code></strong>, followed
 * by channel name. So, in case of for example container managed security which has already restricted an example page
 * <code>/user/foo.xhtml</code> to logged-in users with the example role <code>USER</code> on the example URL pattern
 * <code>/user/*</code> in <code>web.xml</code> like below,
 * <pre>
 * &lt;security-constraint&gt;
 *     &lt;web-resource-collection&gt;
 *         &lt;web-resource-name&gt;Restrict access to role USER.&lt;/web-resource-name&gt;
 *         &lt;url-pattern&gt;/user/*&lt;/url-pattern&gt;
 *     &lt;/web-resource-collection&gt;
 *     &lt;auth-constraint&gt;
 *         &lt;role-name&gt;USER&lt;/role-name&gt;
 *     &lt;/auth-constraint&gt;
 * &lt;/security-constraint&gt;
 * </pre>
 * <p>
 * .. and the page <code>/user/foo.xhtml</code> in turn contains a <code>&lt;o:sse channel="foo"&gt;</code>, then you
 * need to add a restriction on SSE connection request URL pattern of <code>/omnifaces.sse/foo</code> like below.
 * <pre>
 * &lt;security-constraint&gt;
 *     &lt;web-resource-collection&gt;
 *         &lt;web-resource-name&gt;Restrict access to role USER.&lt;/web-resource-name&gt;
 *         &lt;url-pattern&gt;/user/*&lt;/url-pattern&gt;
 *         &lt;url-pattern&gt;/omnifaces.sse/foo&lt;/url-pattern&gt;
 *     &lt;/web-resource-collection&gt;
 *     &lt;auth-constraint&gt;
 *         &lt;role-name&gt;USER&lt;/role-name&gt;
 *     &lt;/auth-constraint&gt;
 * &lt;/security-constraint&gt;
 * </pre>
 * <p>
 * As extra security, particularly for those public channels which can't be restricted by security constraints, the
 * <code>&lt;o:sse&gt;</code> will register all so far declared channels in the current HTTP session, and any
 * incoming SSE connection request will be checked whether it matches a registered channel in the current HTTP session.
 * In case the channel is unknown (e.g. randomly guessed or spoofed by endusers or manually reconnected after the
 * session is expired), then the SSE connection will immediately be completed. Also, when the HTTP session gets
 * destroyed, all session and view scoped channels which are still open will explicitly be closed from server side.
 * Only application scoped channels remain open and are still reachable from server end even when the session or view
 * associated with the page in client side is expired.
 *
 *
 * <h2 id="business-service"><a href="#business-service">Business service design hints</a></h2>
 * <p>
 * In case you'd like to trigger a push from business service side to an application scoped push channel, then you could make use
 * of CDI events. First create a custom bean class representing the push event something like <code>PushEvent</code>
 * below taking whatever you'd like to pass as push message.
 * <pre>
 * public final class PushEvent {
 *
 *     private final String message;
 *
 *     public PushEvent(String message) {
 *         this.message = message;
 *     }
 *
 *     public String getMessage() {
 *         return message;
 *     }
 * }
 * </pre>
 * <p>
 * Then use {@link jakarta.enterprise.inject.spi.BeanManager#getEvent()} to fire the CDI event.
 * <pre>
 * &#64;Inject
 * private BeanManager beanManager;
 *
 * public void onSomeEntityChange(Entity entity) {
 *     beanManager.getEvent().select().fire(new PushEvent(entity.getSomeProperty()));
 * }
 * </pre>
 * <p>
 * Note that OmniFaces own {@link Beans#fireEvent(Object, java.lang.annotation.Annotation...)} utility method is
 * insuitable as it is not allowed to use WAR (front end) frameworks and libraries like Faces and OmniFaces in business
 * service (back end) side.
 * <p>
 * Finally just <code>&#64;</code>{@link Observes} it in some request or application scoped CDI managed bean in WAR and
 * delegate to {@link PushContext} as below.
 * <pre>
 * &#64;Inject &#64;Push(type=SSE)
 * private PushContext someChannel;
 *
 * public void onPushEvent(@Observes PushEvent event) {
 *     someChannel.send(event.getMessage());
 * }
 * </pre>
 * <p>
 * Note that a request scoped bean wouldn't be the same one as from the originating page for the simple reason that
 * there's no means of a HTTP request anywhere at that moment. For exactly this reason a view and session scoped bean
 * would not work (as they require respectively the Faces view state and HTTP session which can only be identified by a
 * HTTP request). A view and session scoped push channel would also not work, so the push channel really needs to be
 * application scoped. The {@link FacesContext} will also be unavailable in the above event listener method.
 * <p>
 * In case the trigger in business service side is an asynchronous service method which is in turn initiated in WAR side, then
 * you could make use of callbacks from WAR side. Let the business service method take a callback instance as argument,
 * e.g. the <code>java.util.function.Consumer</code> functional interface.
 * <pre>
 * &#64;Asynchronous
 * public void someAsyncServiceMethod(Entity entity, Consumer&lt;Object&gt; callback) {
 *     // ... (some long process)
 *     callback.accept(entity.getSomeProperty());
 * }
 * </pre>
 * <p>
 * And invoke the asynchronous service method in WAR as below.
 * <pre>
 * &#64;Inject
 * private SomeService someService;
 *
 * &#64;Inject &#64;Push(type=SSE)
 * private PushContext someChannel;
 *
 * public void someAction() {
 *     someService.someAsyncServiceMethod(entity, message -&gt; someChannel.send(message));
 * }
 * </pre>
 * <p>
 * This would be the only way in case you intend to asynchronously send a message to a view or session scoped push
 * channel, and/or want to pass something from {@link FacesContext} or the initial request/view/session scope along as
 * (<code>final</code>) argument.
 *
 *
 * <h2 id="cluster"><a href="#cluster">Cluster design hints</a></h2>
 * <p>
 * In case your web application is deployed to a server cluster with multiple nodes, and the push event could be
 * triggered in a different node than where the client is connected to, then it won't reach the client. One solution is
 * to activate and configure a JMS topic in the server configuration, trigger the push event via JMS instead of CDI,
 * and use a JMS listener (a message driven bean, MDB) to delegate the push event to CDI.</p>
 * <p>
 * Below is an example extending on the above given business service example.
 * <pre>
 * &#64;ApplicationScoped
 * public class PushManager {
 *
 *     &#64;Resource(lookup = "java:/jms/topic/push")
 *     private Topic jmsTopic;
 *
 *     &#64;Inject
 *     private JMSContext jmsContext;
 *
 *     public void fireEvent(PushEvent pushEvent) {
 *         try {
 *             Message jmsMessage = jmsContext.createMessage();
 *             jmsMessage.setStringProperty("message", pushEvent.getMessage());
 *             jmsContext.createProducer().send(jmsTopic, jmsMessage);
 *         }
 *         catch (Exception e) {
 *             // Handle.
 *         }
 *     }
 * }
 * </pre>
 * <pre>
 * &#64;MessageDriven(activationConfig = {
 *     &#64;ActivationConfigProperty(propertyName = "destination", propertyValue = "java:/jms/topic/push")
 * })
 * public class PushListener implements MessageListener {
 *
 *     &#64;Inject
 *     private BeanManager beanManager;
 *
 *     &#64;Override
 *     public void onMessage(Message jmsMessage) {
 *         try {
 *             String message = jmsMessage.getStringProperty("message");
 *             beanManager.fireEvent(new PushEvent(message));
 *         }
 *         catch (Exception e) {
 *             // Handle.
 *         }
 *     }
 * }
 * </pre>
 * <p>
 * Then, in your business service, instead of using <code>BeanManager#fireEvent()</code> to fire the CDI event,
 * <pre>
 * &#64;Inject
 * private BeanManager beanManager;
 *
 * public void onSomeEntityChange(Entity entity) {
 *     beanManager.fireEvent(new PushEvent(entity.getSomeProperty()));
 * }
 * </pre>
 * <p>
 * use the newly created <code>PushManager#fireEvent()</code> to fire the JMS event from one server node of the cluster,
 * which in turn will fire the CDI event in all server nodes of the cluster.
 * <pre>
 * &#64;Inject
 * private PushManager pushManager;
 *
 * public void onSomeEntityChange(Entity entity) {
 *     pushManager.fireEvent(new PushEvent(entity.getSomeProperty()));
 * }
 * </pre>
 *
 *
 * <h2 id="ui"><a href="#ui">UI update design hints</a></h2>
 * <p>
 * In case you'd like to perform complex UI updates, then easiest would be to put <code>&lt;f:ajax&gt;</code> inside
 * <code>&lt;o:sse&gt;</code>. Here's an example:
 * <pre>
 * &lt;h:panelGroup id="foo"&gt;
 *     ... (some complex UI here) ...
 * &lt;/h:panelGroup&gt;
 *
 * &lt;h:form&gt;
 *     &lt;o:sse channel="someChannel" scope="view"&gt;
 *         &lt;f:ajax event="someEvent" listener="#{bean.pushed}" render=":foo" /&gt;
 *     &lt;/o:sse&gt;
 * &lt;/h:form&gt;
 * </pre>
 * <p>
 * Here, the push message simply represents the ajax event name. You can use any custom event name.
 * <pre>
 * someChannel.send("someEvent");
 * </pre>
 * <p>
 * An alternative is to combine <code>&lt;o:sse&gt;</code> with <code>&lt;h:commandScript&gt;</code>. E.g.
 * <pre>
 * &lt;h:panelGroup id="foo"&gt;
 *     ... (some complex UI here) ...
 * &lt;/h:panelGroup&gt;
 *
 * &lt;o:sse channel="someChannel" scope="view" onmessage="someCommandScript" /&gt;
 * &lt;h:form&gt;
 *     &lt;h:commandScript name="someCommandScript" action="#{bean.pushed}" render=":foo" /&gt;
 * &lt;/h:form&gt;
 * </pre>
 * <p>
 * If you pass a <code>Map&lt;String,V&gt;</code> or a JavaBean as push message object, then all entries/properties will
 * transparently be available as request parameters in the command script method <code>#{bean.pushed}</code>.
 *
 *
 * @author Bauke Scholtz
 * @see SseEndpoint
 * @see SseChannelManager
 * @see SseUserManager
 * @see SseSessionManager
 * @see SseEvent
 * @see Push
 * @see PushContext
 * @see PushExtension
 * @see SsePushContext
 * @since 5.2
 */
@FacesComponent(value = Sse.COMPONENT_TYPE, namespace = OmniFaces.OMNIFACES_NAMESPACE, tagName = "sse")
@ResourceDependency(library=OMNIFACES_LIBRARY_NAME, name=OMNIFACES_SCRIPT_NAME, target="head")
public class Sse extends PushComponent {

    // Public constants -----------------------------------------------------------------------------------------------

    /** The component type, which is {@value org.omnifaces.cdi.push.Sse#COMPONENT_TYPE}. */
    public static final String COMPONENT_TYPE = "org.omnifaces.cdi.push.SseComponent";

    // Private constants ----------------------------------------------------------------------------------------------

    private static final String ERROR_SERVLET_NOT_REGISTERED =
        "SSE endpoint is not registered."
            + " Make sure there is at least one CDI managed bean with @Push(type=SSE) or @Push(type=NOTIFICATION) qualified injection point.";

    static final String SCRIPT_INIT = "OmniFaces.Util.addOnloadListener(function(){OmniFaces.Push.init(true,'%s','%s',%s,%s);});";

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * First check if the SSE endpoint is registered and the channel name and scope is valid, then register it in
     * {@link SseChannelManager} and get the channel ID, then render the <code>init()</code> script. This script will
     * instruct the client to open an SSE connection to {@link SseEndpoint}.
     * @throws IllegalStateException When the SSE endpoint is not registered.
     * @throws IllegalArgumentException When the channel name, scope or user is invalid.
     * The channel name may only contain alphanumeric characters, hyphens, underscores and periods.
     * The allowed channel scope values are "application", "session" and "view", case insensitive.
     * The channel name must be uniquely tied to the channel scope.
     * The user, if any, must implement <code>Serializable</code>.
     */
    @Override
    public void encodeChildren(FacesContext context) throws IOException {
        checkEndpointRegistered(context);

        var contextPath = getRequestContextPath(context);
        var channelId = registerChannel(context, this, getScope());
        var functions = getOnopen() + "," + getOnmessage() + "," + getOnerror() + "," + getOnclose();
        var behaviors = getBehaviorScripts();

        var script = SCRIPT_INIT.formatted(contextPath, channelId, functions, behaviors);
        context.getResponseWriter().write(script);
    }

    // Helpers --------------------------------------------------------------------------------------------------------

    /**
     * Check if the SSE endpoint is registered.
     * @param context The involved faces context.
     * @throws IllegalStateException When the SSE endpoint is not registered.
     */
    static void checkEndpointRegistered(FacesContext context) {
        if (!context.getExternalContext().getApplicationMap().containsKey(Sse.class.getName())) {
            throw new IllegalStateException(ERROR_SERVLET_NOT_REGISTERED);
        }
    }

    /**
     * Validate the channel, handle user switching, and register the channel in {@link SseChannelManager}.
     * @param context The involved faces context.
     * @param component The channel component.
     * @param scope The channel scope.
     * @return The registered channel identifier for use in the SSE connection URI.
     * @throws IllegalArgumentException When the channel name is invalid.
     */
    static String registerChannel(FacesContext context, ChannelComponent component, String scope) {
        var channel = component.getChannel();
        component.validateChannel(context, channel);

        var registeredUsers = getViewAttribute(context, Sse.class.getName(), () -> new HashMap<>(ESTIMATED_TOTAL_CHANNELS, 1));
        var previousUser = (Serializable) registeredUsers.put(channel, component.getUser());

        if (previousUser != null && !previousUser.equals(component.getUser())) {
            SseChannelManager.getInstance().switchUser(channel, scope, previousUser, component.getUser());
        }

        return SseChannelManager.getInstance().register(channel, scope, component.getUser());
    }

    /**
     * Register SSE endpoint if necessary, i.e. when {@link PushExtension} has detected at least one
     * <code>&#64;</code>{@link Push}<code>(type=SSE)</code> or <code>&#64;</code>{@link Push}<code>(type=NOTIFICATION)</code>
     * qualified injection point.
     * @param context The involved servlet context.
     */
    public static void registerEndpointIfNecessary(ServletContext context) {
        if (TRUE.equals(context.getAttribute(Sse.class.getName())) || !PushExtension.isSseActivated()) {
            return;
        }

        try {
            var registration = context.addServlet(SseEndpoint.class.getName(), SseEndpoint.class);
            registration.setAsyncSupported(true);
            registration.addMapping(SSE_URI_PREFIX + "/*");
            context.setAttribute(Sse.class.getName(), Boolean.TRUE);
        }
        catch (Exception e) {
            throw new FacesException(e);
        }
    }

}
