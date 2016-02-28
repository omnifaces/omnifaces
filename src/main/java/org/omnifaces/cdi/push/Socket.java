/*
 * Copyright 2016 OmniFaces.
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
import static org.omnifaces.util.Beans.getReference;
import static org.omnifaces.util.Events.subscribeToViewEvent;
import static org.omnifaces.util.Facelets.getObject;
import static org.omnifaces.util.Facelets.getString;
import static org.omnifaces.util.Facelets.getValueExpression;
import static org.omnifaces.util.FacesLocal.getApplicationAttribute;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.regex.Pattern;

import javax.el.ValueExpression;
import javax.enterprise.event.Observes;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.PostAddToViewEvent;
import javax.faces.event.PreRenderViewEvent;
import javax.faces.event.SystemEventListener;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;
import javax.servlet.ServletContext;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;
import org.omnifaces.cdi.push.event.Closed;
import org.omnifaces.cdi.push.event.Opened;
import org.omnifaces.util.Beans;
import org.omnifaces.util.Callback;
import org.omnifaces.util.Json;

/**
 * <p>
 * The <code>&lt;o:socket&gt;</code> tag opens an one-way (server to client) web socket based push connection in client
 * side which can be reached from server side via {@link PushContext} interface injected in any CDI/container managed
 * artifact via {@link Push} annotation.
 *
 *
 * <h3 id="configuration"><a href="#configuration">Configuration</a></h3>
 * <p>
 * First enable the web socket endpoint by below boolean context parameter in <code>web.xml</code>:
 * <pre>
 * &lt;context-param&gt;
 *     &lt;param-name&gt;org.omnifaces.ENABLE_SOCKET_ENDPOINT&lt;/param-name&gt;
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
 * use the <code>port</code> attribute to explicitly specify the port.
 * <pre>
 * &lt;o:socket port="8000" ... /&gt;
 * </pre>
 * <p>
 * When successfully connected, the web socket is by default open as long as the document is open, and it will
 * auto-reconnect at increasing intervals when the connection is unintentionally closed/aborted as result of e.g. a
 * timeout or a network error. It will not auto-reconnect when the very first connection attempt already fails. The web
 * socket will be implicitly closed once the document is unloaded (e.g. navigating away, close of browser window/tab,
 * etc).
 *
 *
 * <h3 id="usage-server"><a href="#usage-server">Usage (server)</a></h3>
 * <p>
 * In WAR side, you can inject <strong>{@link PushContext}</strong> via <strong>{@link Push}</strong> annotation on the
 * given channel name in any CDI/container managed artifact such as <code>@Named</code>, <code>@WebServlet</code>, etc
 * wherever you'd like to send a push message and then invoke <strong>{@link PushContext#send(Object)}</strong> with any
 * Java object representing the push message.
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
 * By default the web socket is <strong><code>application</code></strong> scoped, i.e. any view/session throughout the
 * web application having the same web socket channel open will receive the same push message. The push message can be
 * sent by all users and the application itself. This is useful for application-wide feedback triggered by site itself
 * such as real time updates of a certain page (e.g. site-wide statistics, top100 lists, stock updates, etc).
 * <p>
 * The optional <strong><code>scope</code></strong> attribute can be set to <strong><code>session</code></strong> to
 * restrict the push messages to all views in the current user session only. The push message can only be sent by the
 * user itself and not by the application. This is useful for session-wide feedback triggered by user itself (e.g. as
 * result of asynchronous tasks triggered by user specific action).
 * <pre>
 * &lt;o:socket channel="someChannel" scope="session" ... /&gt;
 * </pre>
 * <p>
 * The <code>scope</code> attribute can also be set to <strong><code>view</code></strong> to restrict the push messages
 * to the current view only. The push message will not show up in other views in the same session even if it's the same
 * URL. The push message can only be sent by the user itself and not by the application. This is useful for view-wide
 * feedback triggered by user itself (e.g. progress bar tied to a user specific action on current view).
 * <pre>
 * &lt;o:socket channel="someChannel" scope="view" ... /&gt;
 * </pre>
 * <p>
 * The <code>scope</code> attribute may not be an EL expression and allowed values are <code>application</code>,
 * <code>session</code> and <code>view</code>, case insensitive.
 * <p>
 * Additionally, the optional <strong><code>user</code></strong> attribute can be set to the unique identifier of the
 * logged-in user, usually the login name or the user ID. This way the push message can be targeted to a specific user
 * and can be sent by other users and the application itself. The value of the <code>user</code> attribute must at least
 * implement {@link Serializable} and have a low memory footprint, so putting entire user entity is not recommended.
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
 * <strong>{@link PushContext#send(Object, Serializable)}</strong>. The push message can be sent by the user itself,
 * other users, and even the application itself. This is useful for user-specific feedback triggered by other users
 * (e.g. chat, admin messages, etc) or by application's background tasks (e.g. notifications, event listeners, etc).
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
 * socket in client side. This will not be invoked when the web socket auto-reconnects a broken connection after the
 * first succesful connection.
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
 * close of a web socket. This will not be invoked when the web socket auto-reconnects a broken connection after the
 * first succesful connection. This will only be invoked when the very first connection attempt fails, or the maximum
 * reconnect attempts has exceeded, or the server has returned close reason code <code>1008</code> (policy violated).
 * <pre>
 * &lt;o:socket ... onclose="socketCloseListener" /&gt;
 * </pre>
 * <pre>
 * function socketCloseListener(code, channel, event) {
 *     if (code == -1) {
 *         // Web sockets not supported by client.
 *     } else if (code != 1000) {
 *         // Abnormal close reason.
 *     }
 * }
 * </pre>
 * <p>
 * The <code>onclose</code> JavaScript listener function will be invoked with three arguments:
 * <ul>
 * <li><code>code</code>: the close reason code as integer. If this is <code>-1</code>, then the web socket
 * is simply not supported by the client. If this is <code>1000</code>, then it was normally closed. Else if this is not
 * <code>1000</code>, then there may be an error. See also
 * <a href="http://tools.ietf.org/html/rfc6455#section-7.4.1">RFC 6455 section 7.4.1</a> and {@link CloseCodes} API for
 * an elaborate list of all close codes.</li>
 * <li><code>channel</code>: the channel name, useful in case you intend to have a global listener.</li>
 * <li><code>event</code>: the raw <a href="https://developer.mozilla.org/en-US/docs/Web/API/CloseEvent"><code>
 * CloseEvent</code></a> instance, useful in case you intend to inspect it.</li>
 * </ul>
 * <p>
 * When a session or view scoped socket is automatically closed with close reason code <code>1000</code> by the server
 * (and thus not manually by the client via <code>OmniFaces.Push.close(channel)</code>), then it means that the session
 * or view has expired. In case of a session scoped socket you could take the opportunity to show a "Session expired"
 * message and/or immediately redirect to the login page via <code>window.location</code>. In case of a view scoped
 * socket this is quite rare as it actually means "View expired" while the page is still open, but it could happen if
 * the JSF "views per session" configuration setting is set relatively low. You might take the opportunity to reload
 * the page.
 *
 *
 * <h3 id="events-server"><a href="#events-server">Events (server)</a></h3>
 * <p>
 * When a web socket has been opened, a new CDI <strong>{@link SocketEvent}</strong> will be fired with
 * <strong>{@link Opened}</strong> qualifier. When a web socket has been closed, a new CDI {@link SocketEvent} will be
 * fired with <strong>{@link Closed}</strong> qualifier. They can only be observed and collected in an application
 * scoped CDI bean as below. A request scoped one is useless and a view/session scoped one wouldn't work as there's no
 * means of a HTTP request anywhere at that moment.
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
 *         CloseReason reason = event.getCloseReason(); // Returns close reason.
 *         // Do your thing with it. E.g. removing them from collection.
 *     }
 *
 * }
 * </pre>
 * <p>
 * You could even use it to send another push message to an application scoped socket, e.g. "User X has been logged in"
 * (or out) when a session scoped socket is opened (or closed).
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
 * HTTP session. In case the channel is unknown (e.g. randomly guessed or spoofed by endusers), then the web socket will
 * immediately be closed with close reason code {@link CloseCodes#VIOLATED_POLICY} (<code>1008</code>). Also, when the
 * HTTP session gets destroyed, all session and view scoped channels which are still open will explicitly be closed
 * from server side with close reason code {@link CloseCodes#NORMAL_CLOSURE} (<code>1000</code>). Only application
 * scoped sockets remain open and are still reachable from server end even when the session or view is expired on client
 * side.
 *
 *
 * <h3 id="ejb"><a href="#ejb">EJB design hints</a></h3>
 * <p>
 * In case you'd like to trigger a push from EAR/EJB side to an application scoped push socket, then you could make use
 * of CDI events. First create a custom bean class representing the push event something like <code>PushEvent</code>
 * below taking whatever you'd like to pass as push message.
 * <pre>
 * public class PushEvent {
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
 * insuitable as you're not supposed to use WAR (front end) frameworks and libraries like JSF and OmniFaces in EAR/EJB
 * (back end) side.
 * <p>
 * Finally just {@link Observes} it in some request or application scoped CDI managed bean in WAR and delegate to
 * {@link PushContext} as below.
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
 * application scoped). The {@link FacesContext} will also be unavailable in the above event listener method.
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
 * public void submit() {
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
 * <pre>
 * public void submit() {
 *     someService.someAsyncServiceMethod(entity, new Runnable() {
 *         public void run() {
 *             someChannel.send(entity.getSomeProperty());
 *         }
 *     });
 * }
 * </pre>
 * <p>
 * Note that OmniFaces own {@link Callback} interfaces are insuitable as you're not supposed to use WAR (front end)
 * frameworks and libraries like JSF and OmniFaces in EAR/EJB (back end) side.
 *
 *
 * <h3 id="ui"><a href="#ui">UI update design hints</a></h3>
 * <p>
 * In case you'd like to perform complex UI updates, which would be a piece of cake with JSF ajax, then easiest would
 * be to combine <code>&lt;o:socket&gt;</code> with <code>&lt;o:commandScript&gt;</code> which simply invokes a bean
 * action and ajax-updates the UI once a push message arrives. The combination can look like below:
 * <pre>
 * &lt;h:panelGroup id="foo"&gt;
 *     ... (some complex UI here) ...
 * &lt;/h:panelGroup&gt;
 *
 * &lt;o:socket channel="someChannel" scope="view" onmessage="someCommandScript" /&gt;
 * &lt;o:commandScript name="someCommandScript" action="#{bean.pushed}" render="foo" /&gt;
 * </pre>
 * <p>
 * If you pass a <code>Map&lt;String,V&gt;</code> or a JavaBean as push message object, then all entries/properties will
 * transparently be available as request parameters in the command script method <code>#{bean.pushed}</code>.
 *
 *
 * @author Bauke Scholtz
 * @see SocketEndpoint
 * @see SocketFacesListener
 * @see SocketChannelManager
 * @see SocketSessionManager
 * @see SocketEvent
 * @see Opened
 * @see Closed
 * @see Push
 * @see PushContext
 * @see SocketPushContext
 * @see SocketPushContextProducer
 * @since 2.3
 */
public class Socket extends TagHandler {

	// Constants ------------------------------------------------------------------------------------------------------

	/** The boolean context parameter name to register web socket endpoint during startup. */
	public static final String PARAM_ENABLE_SOCKET_ENDPOINT = "org.omnifaces.ENABLE_SOCKET_ENDPOINT";

	private static final Pattern PATTERN_CHANNEL = Pattern.compile("[\\w.-]+");

	private static final String ERROR_ENDPOINT_NOT_ENABLED =
		"o:socket endpoint is not enabled."
			+ " You need to set web.xml context param '" + PARAM_ENABLE_SOCKET_ENDPOINT + "' with value 'true'.";
	private static final String ERROR_INVALID_CHANNEL =
		"o:socket 'channel' attribute '%s' does not represent a valid channel name. It may not be an EL expression and"
			+ " it may only contain alphanumeric characters, hyphens, underscores and periods.";
	private static final String ERROR_INVALID_SCOPE =
		"o:socket 'scope' attribute '%s' does not represent a valid scope. It may not be an EL expression and allowed"
			+ " values are 'application', 'session' and 'view', case insensitive. The default is 'application'. When"
			+ " 'user' attribute is specified, then scope defaults to 'session' and may not be 'application'.";
	private static final String ERROR_INVALID_USER =
		"o:socket 'user' attribute '%s' does not represent a valid user identifier. It must implement Serializable and"
			+ " preferably have low memory footprint. Suggestion: use #{request.remoteUser} or #{someLoggedInUser.id}.";
	private static final String ERROR_DUPLICATE_CHANNEL =
		"o:socket channel '%s' is already registered on a different scope. Choose an unique channel name for a"
			+ " different channel (or shutdown all browsers and restart the server if you were just testing).";

	// Properties -----------------------------------------------------------------------------------------------------

	private TagAttribute port;
	private TagAttribute channel;
	private TagAttribute scope;
	private TagAttribute user;
	private TagAttribute onopen;
	private TagAttribute onmessage;
	private TagAttribute onclose;
	private TagAttribute connected;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * The tag constructor. It will extract and validate the attributes.
	 * @param config The tag config.
	 */
	public Socket(TagConfig config) {
		super(config);
		port = getAttribute("port");
		channel = getRequiredAttribute("channel");
		scope = getAttribute("scope");
		user = getAttribute("user");
		onopen = getAttribute("onopen");
		onmessage = getRequiredAttribute("onmessage");
		onclose = getAttribute("onclose");
		connected = getAttribute("connected");
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * First check if the web socket endpoint is enabled in <code>web.xml</code> and the channel name and scope is
	 * valid, then subcribe the {@link SocketFacesListener}.
	 * @throws IllegalStateException When the web socket endpoint is not enabled in <code>web.xml</code>.
	 * @throws IllegalArgumentException When the channel name, scope or user is invalid.
	 * The channel name may only contain alphanumeric characters, hyphens, underscores and periods.
	 * The allowed channel scope values are "application", "session" and "view", case insensitive.
	 * The channel name must be uniquely tied to the channel scope.
	 * The user, if any, must implement <code>Serializable</code>.
	 */
	@Override
	public void apply(FaceletContext context, UIComponent parent) throws IOException {
		if (!ComponentHandler.isNew(parent)) {
			return;
		}

		if (!TRUE.equals(getApplicationAttribute(context.getFacesContext(), Socket.class.getName()))) {
			throw new IllegalStateException(ERROR_ENDPOINT_NOT_ENABLED);
		}

		String channelName = channel.isLiteral() ? channel.getValue(context) : null;

		if (channelName == null || !PATTERN_CHANNEL.matcher(channelName).matches()) {
			throw new IllegalArgumentException(String.format(ERROR_INVALID_CHANNEL, channelName));
		}

		Object userObject = getObject(context, user);

		if (userObject != null && !(userObject instanceof Serializable)) {
			throw new IllegalArgumentException(String.format(ERROR_INVALID_USER, userObject));
		}

		SocketChannelManager channelManager = getReference(SocketChannelManager.class);
		String scopeName = (scope == null) ? null : scope.isLiteral() ? getString(context, scope) : "";
		String channelId;

		try {
			channelId = channelManager.register(channelName, scopeName, (Serializable) userObject);
		}
		catch (IllegalArgumentException ignore) {
			throw new IllegalArgumentException(String.format(ERROR_INVALID_SCOPE, scopeName));
		}

		if (channelId == null) {
			throw new IllegalArgumentException(String.format(ERROR_DUPLICATE_CHANNEL, channelName));
		}

		Integer portNumber = getObject(context, port, Integer.class);
		String onopenFunction = getString(context, onopen);
		String onmessageFunction = onmessage.getValue(context);
		String oncloseFunction = getString(context, onclose);
		String functions = onopenFunction + "," + onmessageFunction + "," + oncloseFunction;
		ValueExpression connectedExpression = getValueExpression(context, connected, Boolean.class);

		SystemEventListener listener = new SocketFacesListener(portNumber, channelName, channelId, functions, connectedExpression);
		subscribeToViewEvent(PostAddToViewEvent.class, listener);
		subscribeToViewEvent(PreRenderViewEvent.class, listener);
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Register web socket endpoint if necessary, i.e. when it's enabled via context param and not already installed.
	 * @param context The involved servlet context.
	 */
	public static void registerEndpointIfNecessary(ServletContext context) {
		if (TRUE.equals(context.getAttribute(Socket.class.getName())) || !parseBoolean(context.getInitParameter(PARAM_ENABLE_SOCKET_ENDPOINT))) {
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

}