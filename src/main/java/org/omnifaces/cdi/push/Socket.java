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
package org.omnifaces.cdi.push;

import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.parseBoolean;
import static java.util.Collections.unmodifiableList;
import static javax.faces.component.behavior.ClientBehaviorContext.createClientBehaviorContext;
import static org.omnifaces.cdi.push.SocketChannelManager.ESTIMATED_TOTAL_CHANNELS;
import static org.omnifaces.util.Beans.getReference;
import static org.omnifaces.util.Components.getClosestParent;
import static org.omnifaces.util.FacesLocal.getApplicationAttribute;
import static org.omnifaces.util.FacesLocal.getRequestContextPath;
import static org.omnifaces.util.FacesLocal.getRequestParameter;
import static org.omnifaces.util.FacesLocal.getViewAttribute;
import static org.omnifaces.util.FacesLocal.setViewAttribute;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.el.ValueExpression;
import javax.enterprise.event.Observes;
import javax.faces.FacesException;
import javax.faces.application.ResourceDependency;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIForm;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ComponentSystemEvent;
import javax.servlet.ServletContext;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;
import org.omnifaces.cdi.push.SocketEvent.Closed;
import org.omnifaces.cdi.push.SocketEvent.Opened;
import org.omnifaces.component.script.OnloadScript;
import org.omnifaces.util.Beans;
import org.omnifaces.util.Callback;
import org.omnifaces.util.Json;
import org.omnifaces.util.State;

/**
 * <p>
 * The <code>&lt;o:socket&gt;</code> tag opens an one-way (server to client) web socket based push connection in client
 * side which can be reached from server side via {@link PushContext} interface injected in any CDI/container managed
 * artifact via <code>&#64;</code>{@link Push} annotation.
 *
 *
 * <h3 id="configuration"><a href="#configuration">Configuration</a></h3>
 * <p>
 * First enable the web socket endpoint by below boolean context parameter in <code>web.xml</code>:
 * <pre>
 * &lt;context-param&gt;
 *     &lt;param-name&gt;org.omnifaces.SOCKET_ENDPOINT_ENABLED&lt;/param-name&gt;
 *     &lt;param-value&gt;true&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * </pre>
 * <p>
 * It will install the {@link SocketEndpoint}. Lazy initialization of the endpoint via taghandler is unfortunately not
 * possible across all containers (yet).
 * See also <a href="https://java.net/jira/browse/WEBSOCKET_SPEC-211">WS spec issue 211</a>.
 *
 *
 * <h3 id="usage-client"><a href="#usage-client">Usage (client)</a></h3>
 * <p>
 * Declare <strong><code>&lt;o:socket&gt;</code></strong> tag in the JSF view with at least a
 * <strong><code>channel</code></strong> name and an <strong><code>onmessage</code></strong> JavaScript listener
 * function. The channel name may not be an EL expression and it may only contain alphanumeric characters, hyphens,
 * underscores and periods.
 * <p>
 * Here's an example which refers an existing JavaScript listener function (do not include the parentheses!).
 * <pre>
 * &lt;o:socket channel="someChannel" onmessage="socketListener" /&gt;
 * </pre>
 * <pre>
 * function socketListener(message, channel, event) {
 *     console.log(message);
 * }
 * </pre>
 * <p>
 * Here's an example which declares an inline JavaScript listener function.
 * <pre>
 * &lt;o:socket channel="someChannel" onmessage="function(message) { console.log(message); }" /&gt;
 * </pre>
 * <p>
 * The <code>onmessage</code> JavaScript listener function will be invoked with three arguments:
 * <ul>
 * <li><code>message</code>: the push message as JSON object.</li>
 * <li><code>channel</code>: the channel name, useful in case you intend to have a global listener, or want to manually
 * control the close.</li>
 * <li><code>event</code>: the raw <a href="https://developer.mozilla.org/en-US/docs/Web/API/MessageEvent"><code>
 * MessageEvent</code></a> instance, useful in case you intend to inspect it.</li>
 * </ul>
 * <p>
 * In case your server is configured to run WS container on a different TCP port than the HTTP container, then you can
 * use the optional <strong><code>port</code></strong> attribute to explicitly specify the port.
 * <pre>
 * &lt;o:socket port="8000" ... /&gt;
 * </pre>
 * <p>
 * When successfully connected, the web socket is by default open as long as the document is open, and it will
 * auto-reconnect at increasing intervals when the connection is closed/aborted as result of e.g. a network error or
 * server restart. It will not auto-reconnect when the very first connection attempt already fails. The web socket will
 * be implicitly closed once the document is unloaded (e.g. navigating away, close of browser window/tab, etc).
 *
 *
 * <h3 id="usage-server"><a href="#usage-server">Usage (server)</a></h3>
 * <p>
 * In WAR side, you can inject <strong>{@link PushContext}</strong> via <strong><code>&#64;</code>{@link Push}</strong>
 * annotation on the given channel name in any CDI/container managed artifact such as <code>@Named</code>,
 * <code>@WebServlet</code>, etc wherever you'd like to send a push message and then invoke
 * <strong>{@link PushContext#send(Object)}</strong> with any Java object representing the push message.
 * <pre>
 * &#64;Inject &#64;Push
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
 * &#64;Inject &#64;Push(channel="foo")
 * private PushContext bar;
 * </pre>
 * <p>
 * The message object will be encoded as JSON and be delivered as <code>message</code> argument of the
 * <code>onmessage</code> JavaScript listener function associated with the <code>channel</code> name. It can be a
 * plain vanilla <code>String</code>, but it can also be a collection, map and even a javabean. For supported argument
 * types, see also {@link Json#encode(Object)}.
 * <p>
 * Although web sockets support two-way communication, the <code>&lt;o:socket&gt;</code> push is designed for one-way
 * communication, from server to client. In case you intend to send some data from client to server, just continue
 * using JSF ajax the usual way, if necessary from JavaScript on with <code>&lt;o:commandScript&gt;</code> or perhaps
 * <code>&lt;p:remoteCommand&gt;</code> or similar. This has among others the advantage of maintaining the JSF view
 * state, the HTTP session and, importantingly, all security constraints on business service methods. Namely, those
 * security constraints are not available during an incoming web socket message per se. See also a.o.
 * <a href="https://java.net/jira/browse/WEBSOCKET_SPEC-238">WS spec issue 238</a>.
 *
 *
 * <h3 id="scopes-and-users"><a href="#scopes-and-users">Scopes and users</a></h3>
 * <p>
 * By default the web socket is <code>application</code> scoped, i.e. any view/session throughout the web application
 * having the same web socket channel open will receive the same push message. The push message can be sent by all users
 * and the application itself. This is useful for application-wide feedback triggered by site itself such as real time
 * updates of a certain page (e.g. site-wide statistics, top100 lists, stock updates, etc).
 * <p>
 * The optional <strong><code>scope</code></strong> attribute can be set to <code>session</code> to restrict the push
 * messages to all views in the current user session only. The push message can only be sent by the user itself and not
 * by the application. This is useful for session-wide feedback triggered by user itself (e.g. as result of asynchronous
 * tasks triggered by user specific action).
 * <pre>
 * &lt;o:socket channel="someChannel" scope="session" ... /&gt;
 * </pre>
 * <p>
 * The <code>scope</code> attribute can also be set to <code>view</code> to restrict the push messages to the current
 * view only. The push message will not show up in other views in the same session even if it's the same URL. The push
 * message can only be sent by the user itself and not by the application. This is useful for view-wide feedback
 * triggered by user itself (e.g. progress bar tied to a user specific action on current view).
 * <pre>
 * &lt;o:socket channel="someChannel" scope="view" ... /&gt;
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
 * &lt;o:socket channel="someChannel" user="#{request.remoteUser}" ... /&gt;
 * </pre>
 * <p>
 * Or when you have a custom user entity around in EL as <code>#{someLoggedInUser}</code> which has an <code>id</code>
 * property representing its identifier:
 * <pre>
 * &lt;o:socket channel="someChannel" user="#{someLoggedInUser.id}" ... /&gt;
 * </pre>
 * <p>
 * When the <code>user</code> attribute is specified, then the <code>scope</code> defaults to <code>session</code> and
 * cannot be set to <code>application</code>. It can be set to <code>view</code>, but this is kind of unusual and should
 * only be used if the logged-in user represented by <code>user</code> has a shorter lifetime than the HTTP session
 * (e.g. when your application allows changing a logged-in user during same HTTP session without invaliding it &mdash;
 * which is in turn poor security practice). If in such case a session scoped socket is reused, undefined behavior may
 * occur when user-targeted push message is sent. It may target previously logged-in user only. This can be solved by
 * setting the scope to <code>view</code>, but better is to fix the logout to invalidate the HTTP session altogether.
 * <p>
 * In the server side, the push message can be targeted to the user specified in the <code>user</code> attribute via
 * <strong>{@link PushContext#send(Object, Serializable)}</strong>. The push message can be sent by all users and the
 * application itself. This is useful for user-specific feedback triggered by other users (e.g. chat, admin messages,
 * etc) or by application's background tasks (e.g. notifications, event listeners, etc).
 * <pre>
 * &#64;Inject &#64;Push
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
 * <h3 id="channels"><a href="#channels">Channel design hints</a></h3>
 * <p>
 * You can declare multiple push channels on different scopes with or without user target throughout the application.
 * Be however aware that the same channel name can easily be reused across multiple views, even if it's view scoped.
 * It's more efficient if you use as few different channel names as possible and tie the channel name to a specific
 * push socket scope/user combination, not to a specific JSF view. In case you intend to have multiple view scoped
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
 * function someSocketListener(message) {
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
 * <h3 id="connecting"><a href="#connecting">Conditionally connecting</a></h3>
 * <p>
 * You can use the optional <strong><code>connected</code></strong> attribute to control whether to auto-connect the web
 * socket or not.
 * <pre>
 * &lt;o:socket ... connected="#{bean.pushable}" /&gt;
 * </pre>
 * <p>
 * It defaults to <code>true</code> and it's under the covers interpreted as a JavaScript instruction whether to open or
 * close the web socket push connection. If the value is an EL expression and it becomes <code>false</code> during an
 * ajax request, then the push connection will explicitly be closed during oncomplete of that ajax request, even though
 * you did not cover the <code>&lt;o:socket&gt;</code> tag in ajax render/update. So make sure it's tied to at least a
 * view scoped property in case you intend to control it during the view scope.
 * <p>
 * You can also explicitly set it to <code>false</code> and manually open the push connection in client side by
 * invoking <strong><code>OmniFaces.Push.open(channel)</code></strong>, passing the channel name, for example in an
 * onclick listener function of a command button which initiates a long running asynchronous task in server side. This
 * is particularly useful on view scoped sockets which doesn't necessarily need to immediately open on page load.
 * <pre>
 * &lt;h:commandButton ... onclick="OmniFaces.Push.open('foo')"&gt;
 *     &lt;f:ajax ... /&gt;
 * &lt;/h:commandButton&gt;
 * &lt;o:socket channel="foo" scope="view" ... connected="false" /&gt;
 * </pre>
 * <p>
 * In case you intend to have an one-time push and don't expect more messages, usually because you only wanted to
 * present the result of an one-time asynchronous action in a manually opened view scoped push socket as in above
 * example, you can optionally explicitly close the push connection from client side by invoking
 * <strong><code>OmniFaces.Push.close(channel)</code></strong>, passing the channel name. For example, in the
 * <code>onmessage</code> JavaScript listener function as below:
 * <pre>
 * function someSocketListener(message, channel) {
 *     // ...
 *     OmniFaces.Push.close(channel);
 * }
 * </pre>
 * <p>
 * Noted should be that both ways should not be mixed. Choose either the server side way of an EL expression in
 * <code>connected</code> attribute, or the client side way of explicitly setting <code>connected="false"</code> and
 * manually invoking <code>OmniFaces.Push</code> functions. Mixing them ends up in undefined behavior because the
 * associated JSF view state in the server side can't be notified if a socket is manually opened in client side.
 *
 *
 * <h3 id="events-client"><a href="#events-client">Events (client)</a></h3>
 * <p>
 * The optional <strong><code>onopen</code></strong> JavaScript listener function can be used to listen on open of a web
 * socket in client side. This will be invoked on the very first connection attempt, regardless of whether it will be
 * successful or not. This will not be invoked when the web socket auto-reconnects a broken connection after the first
 * successful connection.
 * <pre>
 * &lt;o:socket ... onopen="socketOpenListener" /&gt;
 * </pre>
 * <pre>
 * function socketOpenListener(channel) {
 *     // ...
 * }
 * </pre>
 * <p>
 * The <code>onopen</code> JavaScript listener function will be invoked with one argument:
 * <ul>
 * <li><code>channel</code>: the channel name, useful in case you intend to have a global listener.</li>
 * </ul>
 * <p>
 * The optional <strong><code>onclose</code></strong> JavaScript listener function can be used to listen on (ab)normal
 * close of a web socket. This will be invoked when the very first connection attempt fails, or the server has returned
 * close reason code <code>1000</code> (normal closure) or <code>1008</code> (policy violated), or the maximum reconnect
 * attempts has exceeded. This will not be invoked when the web socket can make an auto-reconnect attempt on a broken
 * connection after the first successful connection.
 * <pre>
 * &lt;o:socket ... onclose="socketCloseListener" /&gt;
 * </pre>
 * <pre>
 * function socketCloseListener(code, channel, event) {
 *     if (code == -1) {
 *         // Web sockets not supported by client.
 *     } else if (code == 1000) {
 *         // Normal close (as result of expired session or view).
 *     } else {
 *         // Abnormal close reason (as result of an error).
 *     }
 * }
 * </pre>
 * <p>
 * The <code>onclose</code> JavaScript listener function will be invoked with three arguments:
 * <ul>
 * <li><code>code</code>: the close reason code as integer. If this is <code>-1</code>, then the web socket
 * is simply not <a href="http://caniuse.com/websockets">supported</a> by the client. If this is <code>1000</code>,
 * then it was normally closed. Else if this is not <code>1000</code>, then there may be an error. See also
 * <a href="http://tools.ietf.org/html/rfc6455#section-7.4.1">RFC 6455 section 7.4.1</a> and {@link CloseCodes} API for
 * an elaborate list of all close codes.</li>
 * <li><code>channel</code>: the channel name, useful in case you intend to have a global listener.</li>
 * <li><code>event</code>: the raw <a href="https://developer.mozilla.org/en-US/docs/Web/API/CloseEvent"><code>
 * CloseEvent</code></a> instance, useful in case you intend to inspect it.</li>
 * </ul>
 * <p>
 * When a session or view scoped socket is automatically closed with close reason code <code>1000</code> by the server
 * (and thus not manually by the client via <code>OmniFaces.Push.close(channel)</code>), then it means that the session
 * or view has expired. In case of a session scoped socket you could take the opportunity to let JavaScript show a
 * "Session expired" message and/or immediately redirect to the login page via <code>window.location</code>. In case of
 * a view scoped socket the handling depends on the reason of the view expiration. A view can be expired when the
 * associated session has expired, but it can also be expired as result of (accidental) navigation or rebuild, or when
 * the JSF "views per session" configuration setting is set relatively low and the client has many views (windows/tabs)
 * open in the same session. You might take the opportunity to warn the client and/or let JavaScript reload the page as
 * submitting any form in it would throw <code>ViewExpiredException</code> anyway.
 *
 *
 * <h3 id="events-server"><a href="#events-server">Events (server)</a></h3>
 * <p>
 * When a web socket has been opened, a new CDI <strong>{@link SocketEvent}</strong> will be fired with
 * <strong><code>&#64;</code>{@link Opened}</strong> qualifier. When a web socket has been closed, a new CDI
 * {@link SocketEvent} will be fired with <strong><code>&#64;</code>{@link Closed}</strong> qualifier. They can only be
 * observed and collected in an application scoped CDI bean as below. Observing in a request/view/session scoped CDI
 * bean is not possible as there's no means of a HTTP request anywhere at that moment.
 * <pre>
 * &#64;ApplicationScoped
 * public class SocketObserver {
 *
 *     public void onOpen(&#64;Observes &#64;Opened SocketEvent event) {
 *         String channel = event.getChannel(); // Returns &lt;o:socket channel&gt;.
 *         Long userId = event.getUser(); // Returns &lt;o:socket user&gt;, if any.
 *         // Do your thing with it. E.g. collecting them in a concurrent/synchronized collection.
 *         // Do note that a single person can open multiple sockets on same channel/user.
 *     }
 *
 *     public void onClose(&#64;Observes &#64;Closed SocketEvent event) {
 *         String channel = event.getChannel(); // Returns &lt;o:socket channel&gt;.
 *         Long userId = event.getUser(); // Returns &lt;o:socket user&gt;, if any.
 *         CloseCode code = event.getCloseCode(); // Returns close reason code.
 *         // Do your thing with it. E.g. removing them from collection.
 *     }
 *
 * }
 * </pre>
 * <p>
 * You could take the opportunity to send another push message to an application scoped socket, e.g. "User X has been
 * logged in" (or out) when a session scoped socket is opened (or closed).
 *
 *
 * <h3 id="security"><a href="#security">Security considerations</a></h3>
 * <p>
 * If the socket is declared in a page which is only restricted to logged-in users with a specific role, then you may
 * want to add the URL of the push handshake request URL to the set of restricted URLs.
 * <p>
 * The push handshake request URL is composed of the URI prefix <strong><code>/omnifaces.push/</code></strong>, followed
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
 * .. and the page <code>/user/foo.xhtml</code> in turn contains a <code>&lt;o:socket channel="foo"&gt;</code>, then you
 * need to add a restriction on push handshake request URL pattern of <code>/omnifaces.push/foo</code> like below.
 * <pre>
 * &lt;security-constraint&gt;
 *     &lt;web-resource-collection&gt;
 *         &lt;web-resource-name&gt;Restrict access to role USER.&lt;/web-resource-name&gt;
 *         &lt;url-pattern&gt;/user/*&lt;/url-pattern&gt;
 *         &lt;url-pattern&gt;/omnifaces.push/foo&lt;/url-pattern&gt;
 *     &lt;/web-resource-collection&gt;
 *     &lt;auth-constraint&gt;
 *         &lt;role-name&gt;USER&lt;/role-name&gt;
 *     &lt;/auth-constraint&gt;
 * &lt;/security-constraint&gt;
 * </pre>
 * <p>
 * As extra security, particularly for those public channels which can't be restricted by security constraints, the
 * <code>&lt;o:socket&gt;</code> will register all so far declared channels in the current HTTP session, and any
 * incoming web socket open request will be checked whether they match the so far registered channels in the current
 * HTTP session. In case the channel is unknown (e.g. randomly guessed or spoofed by endusers or manually reconnected
 * after the session is expired), then the web socket will immediately be closed with close reason code
 * <code>1008</code> ({@link CloseCodes#VIOLATED_POLICY}). Also, when the HTTP session gets destroyed, all session and
 * view scoped channels which are still open will explicitly be closed from server side with close reason code
 * <code>1000</code> ({@link CloseCodes#NORMAL_CLOSURE}). Only application scoped sockets remain open and are still
 * reachable from server end even when the session or view associated with the page in client side is expired.
 *
 *
 * <h3 id="ejb"><a href="#ejb">EJB design hints</a></h3>
 * <p>
 * In case you'd like to trigger a push from EAR/EJB side to an application scoped push socket, then you could make use
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
 * Then use {@link javax.enterprise.inject.spi.BeanManager#fireEvent(Object, java.lang.annotation.Annotation...)} to
 * fire the CDI event.
 * <pre>
 * &#64;Inject
 * private BeanManager beanManager;
 *
 * public void onSomeEntityChange(Entity entity) {
 *     beanManager.fireEvent(new PushEvent(entity.getSomeProperty()));
 * }
 * </pre>
 * <p>
 * Note that OmniFaces own {@link Beans#fireEvent(Object, java.lang.annotation.Annotation...)} utility method is
 * insuitable as it is not allowed to use WAR (front end) frameworks and libraries like JSF and OmniFaces in EAR/EJB
 * (back end) side.
 * <p>
 * Finally just <code>&#64;</code>{@link Observes} it in some request or application scoped CDI managed bean in WAR and
 * delegate to {@link PushContext} as below.
 * <pre>
 * &#64;Inject &#64;Push
 * private PushContext someChannel;
 *
 * public void onPushEvent(@Observes PushEvent event) {
 *     someChannel.send(event.getMessage());
 * }
 * </pre>
 * <p>
 * Note that a request scoped bean wouldn't be the same one as from the originating page for the simple reason that
 * there's no means of a HTTP request anywhere at that moment. For exactly this reason a view and session scoped bean
 * would not work (as they require respectively the JSF view state and HTTP session which can only be identified by a
 * HTTP request). A view and session scoped push socket would also not work, so the push socket really needs to be
 * application scoped. The {@link FacesContext} will also be unavailable in the above event listener method.
 * <p>
 * In case the trigger in EAR/EJB side is an asynchronous service method which is in turn initiated in WAR side, then
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
 * &#64;Inject &#64;Push
 * private PushContext someChannel;
 *
 * public void someAction() {
 *     someService.someAsyncServiceMethod(entity, message -&gt; someChannel.send(message));
 * }
 * </pre>
 * <p>
 * This would be the only way in case you intend to asynchronously send a message to a view or session scoped push
 * socket, and/or want to pass something from {@link FacesContext} or the initial request/view/session scope along as
 * (<code>final</code>) argument.
 * <p>
 * In case you're not on Java 8 yet, then you can make use of {@link Runnable} as callback instance instead of the
 * above <code>Consumer</code> functional interface example.
 * <pre>
 * &#64;Asynchronous
 * public void someAsyncServiceMethod(Entity entity, Runnable callback) {
 *     // ... (some long process)
 *     entity.setSomeProperty(someProperty);
 *     callback.run();
 * }
 * </pre>
 * <p>
 * Which is invoked in WAR as below.
 * <pre>
 * public void someAction() {
 *     someService.someAsyncServiceMethod(entity, new Runnable() {
 *         public void run() {
 *             someChannel.send(entity.getSomeProperty());
 *         }
 *     });
 * }
 * </pre>
 * <p>
 * Note that OmniFaces own {@link Callback} interfaces are insuitable as it is not allowed to use WAR (front end)
 * frameworks and libraries like JSF and OmniFaces in EAR/EJB (back end) side.
 *
 *
 * <h3 id="ui"><a href="#ui">UI update design hints</a></h3>
 * <p>
 * In case you'd like to perform complex UI updates, then easiest would be to put <code>&lt;f:ajax&gt;</code> inside
 * <code>&lt;o:socket&gt;</code>. The support was added in OmniFaces 2.6. Here's an example:
 * <pre>
 * &lt;h:panelGroup id="foo"&gt;
 *     ... (some complex UI here) ...
 * &lt;/h:panelGroup&gt;
 *
 * &lt;h:form&gt;
 *     &lt;o:socket channel="someChannel" scope="view"&gt;
 *         &lt;f:ajax event="someEvent" listener="#{bean.pushed}" render=":foo" /&gt;
 *     &lt;/o:socket&gt;
 * &lt;/h:form&gt;
 * </pre>
 * <p>
 * Here, the push message simply represents the ajax event name. You can use any custom event name.
 * <pre>
 * someChannel.send("someEvent");
 * </pre>
 * <p>
 * An alternative is to combine <code>&lt;o:socket&gt;</code> with <code>&lt;o:commandScript&gt;</code>. E.g.
 * <pre>
 * &lt;h:panelGroup id="foo"&gt;
 *     ... (some complex UI here) ...
 * &lt;/h:panelGroup&gt;
 *
 * &lt;o:socket channel="someChannel" scope="view" onmessage="someCommandScript" /&gt;
 * &lt;h:form&gt;
 *     &lt;o:commandScript name="someCommandScript" action="#{bean.pushed}" render=":foo" /&gt;
 * &lt;/h:form&gt;
 * </pre>
 * <p>
 * If you pass a <code>Map&lt;String,V&gt;</code> or a JavaBean as push message object, then all entries/properties will
 * transparently be available as request parameters in the command script method <code>#{bean.pushed}</code>.
 *
 *
 * @author Bauke Scholtz
 * @see SocketEndpoint
 * @see SocketChannelManager
 * @see SocketUserManager
 * @see SocketSessionManager
 * @see SocketEvent
 * @see Push
 * @see PushContext
 * @see SocketPushContext
 * @see SocketPushContextProducer
 * @since 2.3
 */
@FacesComponent(Socket.COMPONENT_TYPE)
@ResourceDependency(library="omnifaces", name="omnifaces.js", target="head")
public class Socket extends OnloadScript implements ClientBehaviorHolder {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The standard component type. */
	public static final String COMPONENT_TYPE = "org.omnifaces.cdi.push.Socket";

	/** The boolean context parameter name to register web socket endpoint during startup. */
	public static final String PARAM_SOCKET_ENDPOINT_ENABLED = "org.omnifaces.SOCKET_ENDPOINT_ENABLED";

	/** Naming convention was wrong. Use {@link #PARAM_SOCKET_ENDPOINT_ENABLED} instead. */
	@Deprecated // TODO: remove in 3.0.
	public static final String PARAM_ENABLE_SOCKET_ENDPOINT = "org.omnifaces.ENABLE_SOCKET_ENDPOINT";

	// Private constants ----------------------------------------------------------------------------------------------

	private static final Pattern PATTERN_CHANNEL = Pattern.compile("[\\w.-]+");

	private static final String ERROR_EXPRESSION_DISALLOWED =
		"o:socket 'channel' and 'scope' attributes may not contain an EL expression.";
	private static final String ERROR_INVALID_USER =
		"o:socket 'user' attribute '%s' does not represent a valid user identifier. It must implement Serializable and"
			+ " preferably have low memory footprint. Suggestion: use #{request.remoteUser} or #{someLoggedInUser.id}.";
	private static final String ERROR_INVALID_CHANNEL =
		"o:socket 'channel' attribute '%s' does not represent a valid channel name."
			+ " It is required and it may only contain alphanumeric characters, hyphens, underscores and periods.";
	private static final String ERROR_ENDPOINT_NOT_ENABLED =
		"o:socket endpoint is not enabled."
			+ " You need to set web.xml context param '" + PARAM_SOCKET_ENDPOINT_ENABLED + "' with value 'true'.";

	private static final String SCRIPT_INIT = "OmniFaces.Push.init('%s','%s',%s,%s,%s);";
	private static final String SCRIPT_OPEN = "OmniFaces.Push.open('%s');";
	private static final String SCRIPT_CLOSE = "OmniFaces.Push.close('%s');";

	private static final Collection<String> CONTAINS_EVERYTHING = unmodifiableList(new ArrayList<String>() {
		private static final long serialVersionUID = 1L;

		@Override
		public boolean contains(Object object) {
			return true;
		}
	});

	private enum PropertyKeys {
		// Cannot be uppercased. They have to exactly match the attribute names.
		port, channel, scope, user, onopen, onmessage, onclose, connected;
	}

	// Variables ------------------------------------------------------------------------------------------------------

	private final State state = new State(getStateHelper());

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * An override which checks if this isn't been invoked on <code>channel</code> or <code>scope</code> attribute, and
	 * if the <code>user</code> attribute is <code>Serializable</code>.
	 * Finally it delegates to the super method.
	 * @throws IllegalArgumentException When this value expression is been set on <code>channel</code> or
	 * <code>scope</code> attribute, or when the <code>user</code> attribute is not <code>Serializable</code>.
	 */
	@Override
	public void setValueExpression(String name, ValueExpression binding) {
		if (PropertyKeys.channel.toString().equals(name) || PropertyKeys.scope.toString().equals(name)) {
			throw new IllegalArgumentException(ERROR_EXPRESSION_DISALLOWED);
		}

		Object user = binding.getValue(getFacesContext().getELContext());

		if (user != null && !(user instanceof Serializable)) {
			throw new IllegalArgumentException(String.format(ERROR_INVALID_USER, user));
		}

		super.setValueExpression(name, binding);
	}

	/**
	 * Accept all event names.
	 */
	@Override
	public Collection<String> getEventNames() {
		return CONTAINS_EVERYTHING;
	}

	/**
	 * Only move to body when not nested in a form.
	 */
	@Override
	public void processEvent(ComponentSystemEvent event) throws AbortProcessingException {
		if (getClosestParent(this, UIForm.class) == null) {
			super.processEvent(event);
		}
	}

	/**
	 * First check if the web socket endpoint is enabled in <code>web.xml</code> and the channel name and scope is
	 * valid, then register it in {@link SocketChannelManager} and get the channel ID, then render the
	 * <code>init()</code> script, or if it has just switched the <code>connected</code> attribute, then render either
	 * the <code>open()</code> script or the <code>close()</code> script. During an ajax request with partial rendering,
	 * it's added as <code>&lt;eval&gt;</code> by partial response writer, else it's just added as a script component
	 * with <code>target="body"</code>. Those scripts will in turn hit {@link SocketEndpoint}.
	 * @throws IllegalStateException When the web socket endpoint is not enabled in <code>web.xml</code>.
	 * @throws IllegalArgumentException When the channel name, scope or user is invalid.
	 * The channel name may only contain alphanumeric characters, hyphens, underscores and periods.
	 * The allowed channel scope values are "application", "session" and "view", case insensitive.
	 * The channel name must be uniquely tied to the channel scope.
	 * The user, if any, must implement <code>Serializable</code>.
	 */
	@Override
	public void encodeChildren(FacesContext context) throws IOException {
		if (!TRUE.equals(getApplicationAttribute(context, Socket.class.getName()))) {
			throw new IllegalStateException(ERROR_ENDPOINT_NOT_ENABLED);
		}

		String channel = getChannel();

		if (channel == null || !PATTERN_CHANNEL.matcher(channel).matches()) {
			throw new IllegalArgumentException(String.format(ERROR_INVALID_CHANNEL, channel));
		}

		boolean connected = isConnected();
		Boolean switched = hasSwitched(context, channel, connected);
		String script = null;

		if (switched == null) {
			Integer port = getPort();
			String host = (port != null ? ":" + port : "") + getRequestContextPath(context);
			String channelId = getReference(SocketChannelManager.class).register(channel, getScope(), getUser());
			String functions = getOnopen() + "," + getOnmessage() + "," + getOnclose();
			script = String.format(SCRIPT_INIT, host, channelId, functions, getBehaviorScripts(), connected);
		}
		else if (switched) {
			script = String.format(connected ? SCRIPT_OPEN : SCRIPT_CLOSE, channel);
		}

		if (script != null) {
			context.getResponseWriter().write(script);
		}
	}

	private String getBehaviorScripts() {
		Map<String, List<ClientBehavior>> clientBehaviorsByEvent = getClientBehaviors();

		if (clientBehaviorsByEvent.isEmpty()) {
			return "{}";
		}

		String clientId = getClientId(getFacesContext());
		StringBuilder scripts = new StringBuilder("{");

		for (Entry<String, List<ClientBehavior>> entry : clientBehaviorsByEvent.entrySet()) {
			String event = entry.getKey();
			List<ClientBehavior> clientBehaviors = entry.getValue();
			scripts.append(scripts.length() > 1 ? "," : "").append(event).append(":[");

			for (int i = 0; i < clientBehaviors.size(); i++) {
				scripts.append(i > 0 ? "," : "").append("function(){");
				scripts.append(clientBehaviors.get(i).getScript(createClientBehaviorContext(getFacesContext(), this, event, clientId, null)));
				scripts.append("}");
			}

			scripts.append("]");
		}

		return scripts.append("}").toString();
	}

	@Override
	public void decode(FacesContext context) {
		Map<String, List<ClientBehavior>> clientBehaviors = getClientBehaviors();

		if (clientBehaviors.isEmpty()) {
			return;
		}

		if (!getClientId(context).equals(getRequestParameter(context, "javax.faces.source"))) {
			return;
		}

		List<ClientBehavior> behaviors = clientBehaviors.get(getRequestParameter(context, "javax.faces.behavior.event"));

		if (behaviors == null) {
			return;
		}

		for (ClientBehavior behavior : behaviors) {
			behavior.decode(context, this);
		}
	}

	// Attribute getters/setters --------------------------------------------------------------------------------------

	/**
	 * Returns the port number of the web socket host.
	 * @return The port number of the web socket host.
	 */
	public Integer getPort() {
		return state.get(PropertyKeys.port);
	}

	/**
	 * Sets the port number of the web socket host, in case it is different from the port number in the request URI.
	 * Defaults to the port number of the request URI.
	 * @param port The port number of the web socket host.
	 */
	public void setPort(Integer port) {
		state.put(PropertyKeys.port, port);
	}

	/**
	 * Returns the name of the web socket channel.
	 * @return The name of the web socket channel.
	 */
	public String getChannel() {
		return state.get(PropertyKeys.channel);
	}

	/**
	 * Sets the name of the web socket channel.
	 * It may not be an EL expression and it may only contain alphanumeric characters, hyphens, underscores and periods.
	 * All open websockets on the same channel will receive the same push message from the server.
	 * @param channel The name of the web socket channel.
	 */
	public void setChannel(String channel) {
		state.put(PropertyKeys.channel, channel);
	}

	/**
	 * Returns the scope of the web socket channel.
	 * @return The scope of the web socket channel.
	 */
	public String getScope() {
		return state.get(PropertyKeys.scope);
	}

	/**
	 * Sets the scope of the web socket channel.
	 * It may not be an EL expression and allowed values are <code>application</code>, <code>session</code> and
	 * <code>view</code>, case insensitive. When the value is <code>application</code>, then all channels with the same
	 * name throughout the application will receive the same push message. When the value is <code>session</code>, then
	 * only the channels with the same name in the current user session will receive the same push message. When the
	 * value is <code>view</code>, then only the channel in the current view will receive the push message. The default
	 * scope is <code>application</code>. When the <code>user</code> attribute is specified, then the default scope is
	 * <code>session</code>.
	 * @param scope The scope of the web socket channel.
	 */
	public void setScope(String scope) {
		state.put(PropertyKeys.scope, scope);
	}

	/**
	 * Returns the user identifier of the web socket channel.
	 * @return The user identifier of the web socket channel.
	 */
	public Serializable getUser() {
		return state.get(PropertyKeys.user);
	}

	/**
	 * Sets the user identifier of the web socket channel, so that user-targeted push messages can be sent.
	 * All open websockets on the same channel and user will receive the same push message from the server.
	 * It must implement <code>Serializable</code> and preferably have low memory footprint.
	 * Suggestion: use <code>#{request.remoteUser}</code> or <code>#{someLoggedInUser.id}</code>.
	 * @param user The user identifier of the web socket channel.
	 */
	public void setUser(Serializable user) {
		state.put(PropertyKeys.user, user);
	}

	/**
	 * Returns the JavaScript event handler function that is invoked when the web socket is opened.
	 * @return The JavaScript event handler function that is invoked when the web socket is opened.
	 */
	public String getOnopen() {
		return state.get(PropertyKeys.onopen);
	}

	/**
	 * Sets the JavaScript event handler function that is invoked when the web socket is opened.
	 * The function will be invoked with one argument: the channel name.
	 * @param onopen The JavaScript event handler function that is invoked when the web socket is opened.
	 */
	public void setOnopen(String onopen) {
		state.put(PropertyKeys.onopen, onopen);
	}

	/**
	 * Returns the JavaScript event handler function that is invoked when a push message is received from the server.
	 * @return The JavaScript event handler function that is invoked when a push message is received from the server.
	 */
	public String getOnmessage() {
		return state.get(PropertyKeys.onmessage);
	}

	/**
	 * Sets the JavaScript event handler function that is invoked when a push message is received from the server.
	 * The function will be invoked with three arguments: the push message, the channel name and the raw MessageEvent itself.
	 * @param onmessage The JavaScript event handler function that is invoked when a push message is received from the server.
	 */
	public void setOnmessage(String onmessage) {
		state.put(PropertyKeys.onmessage, onmessage);
	}

	/**
	 * Returns the JavaScript event handler function that is invoked when the web socket is closed.
	 * @return The JavaScript event handler function that is invoked when the web socket is closed.
	 */
	public String getOnclose() {
		return state.get(PropertyKeys.onclose);
	}

	/**
	 * Sets the JavaScript event handler function that is invoked when the web socket is closed.
	 * The function will be invoked with three arguments: the close reason code, the channel name and the raw
	 * <code>CloseEvent</code> itself. Note that this will also be invoked on errors and that you can inspect the close
	 * reason code if an error occurred and which one (i.e. when the code is not 1000). See also
	 * <a href="http://tools.ietf.org/html/rfc6455#section-7.4.1">RFC 6455 section 7.4.1</a> and {@link CloseCodes} API
	 * for an elaborate list of all close codes.
	 * @param onclose The JavaScript event handler function that is invoked when the web socket is closed.
	 */
	public void setOnclose(String onclose) {
		state.put(PropertyKeys.onclose, onclose);
	}

	/**
	 * Returns whether to (auto)connect the web socket or not.
	 * @return Whether to (auto)connect the web socket or not.
	 */
	public boolean isConnected() {
		return state.get(PropertyKeys.connected, TRUE);
	}

	/**
	 * Sets whether to (auto)connect the web socket or not. Defaults to <code>true</code>. It's interpreted as a
	 * JavaScript instruction whether to open or close the web socket push connection. Note that this attribute is
	 * re-evaluated on every ajax request. You can also explicitly set it to <code>false</code> and then manually
	 * control in JavaScript by <code>OmniFaces.Push.open("channelName")</code> and
	 * <code>OmniFaces.Push.close("channelName")</code>.
	 * @param connected Whether to (auto)connect the web socket or not.
	 */
	public void setConnected(boolean connected) {
		state.put(PropertyKeys.connected, connected);
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Register web socket endpoint if necessary, i.e. when it's enabled via context param and not already installed.
	 * @param context The involved servlet context.
	 */
	public static void registerEndpointIfNecessary(ServletContext context) {
		if (TRUE.equals(context.getAttribute(Socket.class.getName()))
			|| !(parseBoolean(context.getInitParameter(PARAM_SOCKET_ENDPOINT_ENABLED))
				|| parseBoolean(context.getInitParameter(PARAM_ENABLE_SOCKET_ENDPOINT)))) {
			return;
		}

		try {
			ServerContainer container = (ServerContainer) context.getAttribute(ServerContainer.class.getName());
			ServerEndpointConfig config = ServerEndpointConfig.Builder.create(SocketEndpoint.class, SocketEndpoint.URI_TEMPLATE).build();
			container.addEndpoint(config);
			context.setAttribute(Socket.class.getName(), TRUE);
		}
		catch (Exception e) {
			throw new FacesException(e);
		}
	}

	/**
	 * Helper to remember which channels are opened on the view and returns <code>null</code> if it is new, or
	 * <code>true</code> or <code>false</code> if it has switched its <code>connected</code> attribute.
	 */
	private static Boolean hasSwitched(FacesContext context, String channel, boolean connected) {
		Map<String, Boolean> channels = getViewAttribute(context, Socket.class.getName());

		if (channels == null) {
			channels = new HashMap<>(ESTIMATED_TOTAL_CHANNELS);
			setViewAttribute(context, Socket.class.getName(), channels);
		}

		Boolean previouslyConnected = channels.put(channel, connected);
		return (previouslyConnected == null) ? null : (previouslyConnected != connected);
	}

}