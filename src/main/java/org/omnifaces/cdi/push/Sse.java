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
import static java.lang.String.format;
import static org.omnifaces.cdi.PushContext.SSE_URI_PREFIX;
import static org.omnifaces.config.OmniFaces.OMNIFACES_LIBRARY_NAME;
import static org.omnifaces.config.OmniFaces.OMNIFACES_SCRIPT_NAME;
import static org.omnifaces.util.FacesLocal.getRequestContextPath;

import java.io.IOException;
import java.io.Serializable;

import jakarta.faces.FacesException;
import jakarta.faces.application.ResourceDependency;
import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.ServletContext;

import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;
import org.omnifaces.config.OmniFaces;

/**
 * <p>
 * The <code>&lt;o:sse&gt;</code> is an {@link UIComponent} which opens an one-way (server to client) SSE (Server-Sent
 * Events) based push connection in client side which can be reached from server side via {@link PushContext} interface
 * injected in any CDI/container managed artifact via <code>&#64;</code>{@link Push}<code>(sse=true)</code> annotation.
 *
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
 * <tr><td><b>CDI events</b></td><td>Not available</td><td>{@link SocketEvent} on open/close/switch</td></tr>
 * <tr><td><b>Dynamic connect/disconnect</b></td><td>Not available</td><td><code>connected</code> attribute</td></tr>
 * <tr><td><b>Configuration</b></td><td>None (auto-registered)</td><td>Auto-registered or context param</td></tr>
 * </tbody>
 * </table>
 * <p>
 * <b>When to prefer SSE:</b> for typical unidirectional push use cases (notifications, live updates, dashboards) where
 * HTTP infrastructure compatibility and simplicity are important. SSE is the simpler and more robust choice for most
 * server-to-client push scenarios.
 * <p>
 * <b>When to prefer WebSocket:</b> when you need CDI lifecycle events ({@code @Opened}/{@code @Closed}), dynamic
 * connect/disconnect via the {@code connected} attribute, or binary message support.
 * <p>
 * <b>Note:</b> browsers limit concurrent HTTP/1.1 connections to 6 per origin. When the server does not support
 * HTTP/2, multiple SSE channels across multiple tabs may exhaust this limit and queue further HTTP requests. This can
 * be resolved by configuring the server to use HTTP/2, where all SSE streams are multiplexed over a single TCP
 * connection.
 *
 *
 * <h2 id="configuration"><a href="#configuration">Configuration</a></h2>
 * <p>
 * No explicit configuration is needed. The SSE servlet is automatically registered during application startup when at
 * least one <code>&#64;</code>{@link Push}<code>(sse=true)</code> qualified injection point is detected.
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
 * <a href="https://developer.mozilla.org/en-US/docs/Web/API/EventSource"><code>EventSource</code></a> API.
 *
 *
 * <h2 id="usage-server"><a href="#usage-server">Usage (server)</a></h2>
 * <p>
 * In WAR side, you can inject <strong>{@link PushContext}</strong> via
 * <strong><code>&#64;</code>{@link Push}<code>(sse=true)</code></strong> annotation on the given channel name in any
 * CDI/container managed artifact such as <code>&#64;Named</code>, <code>&#64;WebServlet</code>, etc wherever you'd
 * like to send a push message and then invoke <strong>{@link PushContext#send(Object)}</strong> with any Java object
 * representing the push message.
 * <pre>
 * &#64;Inject &#64;Push(sse=true)
 * private PushContext someChannel;
 *
 * public void sendMessage(Object message) {
 *     someChannel.send(message);
 * }
 * </pre>
 * <p>
 * By default the name of the channel is taken from the name of the variable into which injection takes place. The
 * channel name can be optionally specified via the <code>channel</code> attribute. The example below injects the SSE
 * push context for channel name <code>foo</code> into a variable named <code>bar</code>.
 * <pre>
 * &#64;Inject &#64;Push(channel="foo", sse=true)
 * private PushContext bar;
 * </pre>
 * <p>
 * The message object will be encoded as JSON and be delivered as <code>message</code> argument of the
 * <code>onmessage</code> JavaScript listener function associated with the <code>channel</code> name. It can be a
 * plain vanilla <code>String</code>, but it can also be a collection, map and even a javabean. For supported argument
 * types, see also {@link org.omnifaces.util.Json#encode(Object)}.
 *
 *
 * <h2 id="scopes-and-users"><a href="#scopes-and-users">Scopes and users</a></h2>
 * <p>
 * By default the SSE channel is <code>application</code> scoped, i.e. any view/session throughout the web application
 * having the same SSE channel open will receive the same push message. This is useful for application-wide real time
 * updates such as site-wide statistics, top100 lists, stock updates, etc.
 * <p>
 * The optional <strong><code>scope</code></strong> attribute can be set to <code>session</code> to restrict the push
 * messages to all views in the current user session only. Example:
 * <pre>
 * &lt;o:sse channel="someChannel" scope="session" ... /&gt;
 * </pre>
 * <p>
 * The <code>scope</code> attribute can also be set to <code>view</code> to restrict the push messages to the current
 * view only. This is the narrowest scope. Example:
 * <pre>
 * &lt;o:sse channel="someChannel" scope="view" ... /&gt;
 * </pre>
 * <p>
 * Additionally, the optional <strong><code>user</code></strong> attribute can be set to the unique identifier of the
 * logged-in user, usually the login name or the user ID. This way the push message can be targeted to a specific user
 * and the push message will only be delivered to the (multiple) views/sessions opened by the specific user. Example:
 * <pre>
 * &lt;o:sse channel="someChannel" user="#{request.remoteUser}" ... /&gt;
 * </pre>
 * <p>
 * In the server side, the push message can be targeted to the user specified in the <code>user</code> attribute via
 * <strong>{@link PushContext#send(Object, Serializable)}</strong>. Example:
 * <pre>
 * someChannel.send(message, recipientUserId);
 * </pre>
 * <p>
 * Multiple users can be targeted by passing a {@link java.util.Collection} holding user identifiers to
 * <strong>{@link PushContext#send(Object, java.util.Collection)}</strong>.
 * <pre>
 * someChannel.send(message, recipientUserIds);
 * </pre>
 *
 *
 * <h2 id="events-client"><a href="#events-client">Events (client)</a></h2>
 * <p>
 * The optional <strong><code>onopen</code></strong> JavaScript listener function can be used to listen on open of an
 * SSE connection in client side.
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
 * connection error whereby the SSE connection will attempt to reconnect.
 * <pre>
 * &lt;o:sse ... onerror="sseErrorListener" /&gt;
 * </pre>
 * <pre>
 * function sseErrorListener(channel, event) {
 *     // ...
 * }
 * </pre>
 * <p>
 * The optional <strong><code>onclose</code></strong> JavaScript listener function can be used to listen on the final
 * close of an SSE connection. This is invoked when the server explicitly completes the connection (e.g. on session or
 * view expiry).
 * <pre>
 * &lt;o:sse ... onclose="sseCloseListener" /&gt;
 * </pre>
 * <pre>
 * function sseCloseListener(channel) {
 *     // ...
 * }
 * </pre>
 *
 *
 * <h2 id="ui"><a href="#ui">UI update design hints</a></h2>
 * <p>
 * You can use <code>&lt;f:ajax&gt;</code> inside <code>&lt;o:sse&gt;</code> for complex UI updates. Here's an example:
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
 *
 *
 * <h2 id="security"><a href="#security">Security considerations</a></h2>
 * <p>
 * If the SSE channel is declared in a page which is only restricted to logged-in users with a specific role, then you
 * may want to add the URL of the SSE request URL to the set of restricted URLs.
 * <p>
 * The SSE request URL is composed of the URI prefix <strong><code>/omnifaces.sse/</code></strong>, followed by the
 * channel name. So, in case of for example container managed security which has already restricted an example page
 * <code>/user/foo.xhtml</code> to logged-in users with the example role <code>USER</code> on the example URL pattern
 * <code>/user/*</code> in <code>web.xml</code>, and the page contains a <code>&lt;o:sse channel="foo"&gt;</code>,
 * then you need to add a restriction on SSE request URL pattern of <code>/omnifaces.sse/foo/*</code>.
 * <pre>
 * &lt;security-constraint&gt;
 *     &lt;web-resource-collection&gt;
 *         &lt;web-resource-name&gt;Restrict access to role USER.&lt;/web-resource-name&gt;
 *         &lt;url-pattern&gt;/user/*&lt;/url-pattern&gt;
 *         &lt;url-pattern&gt;/omnifaces.sse/foo/*&lt;/url-pattern&gt;
 *     &lt;/web-resource-collection&gt;
 *     &lt;auth-constraint&gt;
 *         &lt;role-name&gt;USER&lt;/role-name&gt;
 *     &lt;/auth-constraint&gt;
 * &lt;/security-constraint&gt;
 * </pre>
 * <p>
 * As extra security, the <code>&lt;o:sse&gt;</code> will register all so far declared channels in the current HTTP
 * session, and any incoming SSE connection request will be checked whether they match the so far registered channels in
 * the current HTTP session. In case the channel is unknown (e.g. randomly guessed or spoofed by endusers or manually
 * reconnected after the session is expired), then the connection will immediately be closed.
 * <p>
 * <b>Important:</b> All servlet filters mapped on {@code /*} must have {@code asyncSupported=true} for SSE to work.
 * OmniFaces sets this on its own programmatically registered {@code /*} filters. Third-party {@code /*} mapped
 * filters are a deployment concern to configure accordingly.
 *
 *
 * @author Bauke Scholtz
 * @see SseServlet
 * @see SseChannelManager
 * @see SseUserManager
 * @see SseSessionManager
 * @see Push
 * @see PushContext
 * @see SsePushContext
 * @see PushExtension
 * @since 5.2
 */
@FacesComponent(value = Sse.COMPONENT_TYPE, namespace = OmniFaces.OMNIFACES_NAMESPACE, tagName = "sse")
@ResourceDependency(library=OMNIFACES_LIBRARY_NAME, name=OMNIFACES_SCRIPT_NAME, target="head")
public class Sse extends PushComponent {

    // Public constants -----------------------------------------------------------------------------------------------

    /** The component type, which is {@value org.omnifaces.cdi.push.Sse#COMPONENT_TYPE}. */
    public static final String COMPONENT_TYPE = "org.omnifaces.cdi.sse.SseComponent";

    // Private constants ----------------------------------------------------------------------------------------------

    private static final String ERROR_SERVLET_NOT_REGISTERED =
        "o:sse servlet is not registered."
            + " Perhaps there is no @Push(sse=true) qualified injection point in any CDI managed bean?";

    private static final String SCRIPT_INIT = "OmniFaces.Util.addOnloadListener(function(){OmniFaces.Push.init(true,'%s','%s',%s,%s);});";

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * First check if the SSE servlet is registered and the channel name and scope is valid, then register it in
     * {@link SseChannelManager} and get the channel ID, then render the <code>init()</code> script. This script will
     * instruct the client to open an SSE connection to {@link SseServlet}.
     * @throws IllegalStateException When the SSE servlet is not registered.
     * @throws IllegalArgumentException When the channel name, scope or user is invalid.
     * The channel name may only contain alphanumeric characters, hyphens, underscores and periods.
     * The allowed channel scope values are "application", "session" and "view", case insensitive.
     * The channel name must be uniquely tied to the channel scope.
     * The user, if any, must implement <code>Serializable</code>.
     */
    @Override
    public void encodeChildren(FacesContext context) throws IOException {
        if (!context.getExternalContext().getApplicationMap().containsKey(Sse.class.getName())) {
            throw new IllegalStateException(ERROR_SERVLET_NOT_REGISTERED);
        }

        var channel = getChannel();
        validateChannel(channel);

        var contextPath = getRequestContextPath(context);
        var channelId = SseChannelManager.getInstance().register(channel, getScope(), getUser());
        var functions = getOnopen() + "," + getOnmessage() + "," + getOnerror() + "," + getOnclose();
        var behaviors = getBehaviorScripts();

        var script = format(SCRIPT_INIT, contextPath, channelId, functions, behaviors);
        context.getResponseWriter().write(script);
    }

    // Helpers --------------------------------------------------------------------------------------------------------

    /**
     * Register SSE servlet if necessary, i.e. when {@link PushExtension} has detected at least one
     * <code>&#64;</code>{@link Push}<code>(sse=true)</code> qualified injection point.
     * @param context The involved servlet context.
     */
    public static void registerServletIfNecessary(ServletContext context) {
        if (TRUE.equals(context.getAttribute(Sse.class.getName())) || !PushExtension.isSseActivated()) {
            return;
        }

        try {
            var registration = context.addServlet(SseServlet.class.getName(), SseServlet.class);
            registration.setAsyncSupported(true);
            registration.addMapping(SSE_URI_PREFIX + "/*");
            context.setAttribute(Sse.class.getName(), Boolean.TRUE);
        }
        catch (Exception e) {
            throw new FacesException(e);
        }
    }

}
