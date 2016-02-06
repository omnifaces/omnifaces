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
import org.omnifaces.util.Callback;
import org.omnifaces.util.Json;

/**
 * <p>
 * Opens an one-way (server to client) web socket based push connection in client side which can be reached from
 * server side via {@link PushContext} interface injected in any CDI/container managed artifact via {@link Push}.
 *
 *
 * <h3>Configuration</h3>
 * <p>
 * First enable the web socket endpoint by below boolean context parameter in <code>web.xml</code>:
 * <pre>
 * &lt;context-param&gt;
 *     &lt;param-name&gt;org.omnifaces.ENABLE_SOCKET_ENDPOINT&lt;/param-name&gt;
 *     &lt;param-value&gt;true&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * </pre>
 * <p>
 * It will install the {@link SocketEndpoint}. Lazy initialization of the endpoint is unfortunately not possible across
 * all containers (yet).
 * See also <a href="https://java.net/jira/browse/WEBSOCKET_SPEC-211">WS spec issue 211</a>.
 *
 *
 * <h3>Usage (client)</h3>
 * <p>
 * Declare <code>&lt;o:socket&gt;</code> tag in the view with at least a <code>channel</code> name and an
 * <code>onmessage</code> JavaScript listener function. The channel name may only contain alphanumeric characters,
 * hyphens, underscores and periods.
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
 * The optional <code>onclose</code> JavaScript listener function can be used to listen on (ab)normal close of a web
 * socket.
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
 * <p>By default the web socket is application scoped, i.e. any view/session throughout the web application having the
 * same web socket channel open will receive the same push message. The optional <code>scope</code> attribute can be set
 * to <code>session</code> to restrict the push messages to all views in the current user session only.
 * <pre>
 * &lt;o:socket channel="someChannel" scope="session" ... /&gt;
 * </pre>
 * <p>
 * In case your server is configured to run WS container on a different TCP port than the HTTP container, then you can
 * use the <code>port</code> attribute to explicitly specify the port.
 * <pre>
 * &lt;o:socket ... port="8000" /&gt;
 * </pre>
 *
 *
 * <h3>Usage (server)</h3>
 * <p>
 * In the WAR side (not in EAR/EJB side!), you can inject {@link PushContext} via {@link Push} on the given channel name
 * in any CDI/container managed artifact such as <code>@Named</code>, <code>@WebServlet</code>, etc wherever you'd like
 * to send a push message and then invoke {@link PushContext#send(Object)} with any Java object representing the push
 * message.
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
 * state, the HTTP session and, importantingly, all security constraints on business service methods. Namely, those are
 * not available during an incoming web socket message per se. See also a.o.
 * <a href="https://java.net/jira/browse/WEBSOCKET_SPEC-238">WS spec issue 238</a>.
 *
 *
 * <h3>Conditionally connecting socket</h3>
 * <p>
 * You can use the <code>connected</code> attribute for that.
 * <pre>
 * &lt;o:socket ... connected="#{bean.pushable}" /&gt;
 * </pre>
 * <p>
 * It defaults to <code>true</code> and it's interpreted as a JavaScript instruction whether to open or close the web
 * socket push connection. If the value is an EL expression and it becomes <code>false</code> during an ajax request,
 * then the push connection will explicitly be closed during oncomplete of that ajax request, even though you did not
 * cover the <code>&lt;o:socket&gt;</code> tag in ajax render/update. So make sure it's tied to at least a view scoped
 * property in case you intend to control it during the view scope.
 * <p>
 * You can also explicitly set it to <code>false</code> and manually open the push connection in client side by
 * invoking <code>OmniFaces.Push.open(channel)</code>, passing the channel name, for example in an onclick listener
 * function of a command button which initiates a long running asynchronous task in server side.
 * <pre>
 * &lt;o:socket channel="foo" ... connected="false" /&gt;
 * </pre>
 * <pre>
 * function someOnclickListener() {
 *     // ...
 *     OmniFaces.Push.open("foo");
 * }
 * </pre>
 * <p>
 * The web socket is by default open as long as the document is open. It will be implicitly closed once the document is
 * unloaded (e.g. navigating away, close of browser window/tab, etc). In case you intend to have an one-time push,
 * usually because you only wanted to present the result of an one-time asynchronous action in a manually opened push
 * socket as in above example, you can optionally explicitly close the push connection from client side by invoking
 * <code>OmniFaces.Push.close(channel)</code>, passing the channel name. For example, in the <code>onmessage</code>
 * JavaScript listener function as below:
 * <pre>
 * function someSocketListener(message, channel) {
 *     // ...
 *     OmniFaces.Push.close(channel);
 * }
 * </pre>
 * <p>
 * Noted should be that both ways should not be mixed. Choose either the server side way of an EL expression in
 * <code>connected</code> attribute, or the client side way of explicitly setting <code>connected="false"</code> and
 * manually invoking <code>OmniFaces.Push</code> functions. Mixing them ends up in undefined behavior.
 *
 *
 * <h3>Security considerations</h3>
 * <p>
 * If the socket is declared in a page which is only restricted to logged-in users with a specific role, then you may
 * need to add the URL of the push handshake request URL to the set of restricted URLs. Otherwise anyone having manually
 * opened a push socket on the same channel will receive the same push message. This may be OK for global push messages,
 * but this may not be OK for push messages with sensitive information restricted to specific user(s).
 * <p>
 * The push handshake request URL is composed of the URI prefix <code>/omnifaces.push/</code>, followed by channel name.
 * So, in case of for example container managed security which has already restricted the page <code>/user/foo.xhtml</code>
 * itself to logged-in users with the example role <code>USER</code> on the example URL pattern <code>/user/*</code> in
 * <code>web.xml</code> like below,
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
 * .. and the page <code>/user/foo.xhtml</code> contains a <code>&lt;o:socket channel="foo"&gt;</code>, then you need to
 * add a restriction on push handshake request URL pattern of <code>/omnifaces.push/foo</code> like below.
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
 * As extra security, the <code>&lt;o:socket&gt;</code> will register all so far declared channels in the current HTTP
 * session and any incoming web socket open request will be checked whether they match the so far registered channels in
 * the current HTTP session. In case the channel is unknown (e.g. randomly guessed or spoofed by endusers), then the web
 * socket will immediately be closed with close code {@link CloseCodes#VIOLATED_POLICY} (error code <code>1008</code>).
 *
 *
 * <h3>EJB design hints</h3>
 * <p>
 * In case you'd like to trigger a push from EAR/EJB side to an application scoped push socket, you could make use of
 * CDI events. First create a custom bean class representing the push event something like <code>PushEvent</code> taking
 * whatever you'd like to pass as push message.
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
 * would not work at all (as they require respectively the JSF view state and HTTP session which can only be identified
 * by a HTTP request). A session scoped push socket would also not work at all (so the push socket really needs to be
 * application scoped). The {@link FacesContext} will also be unavailable in the method.
 * <p>
 * In case the trigger in EAR/EJB side is in turn initiated in WAR side, then you could make use of callbacks from WAR
 * side. Let the business service method take a callback instance as argument, e.g. {@link Runnable}.
 * <pre>
 * &#64;Asynchronous
 * public void someAsyncServiceMethod(Entity entity, Runnable callback) {
 *     // ... (some long process)
 *     entity.setSomeProperty(someProperty);
 *     callback.run();
 * }
 * </pre>
 * <p>
 * And invoke the service method as below.
 * <pre>
 * &#64;Inject
 * private SomeService someService;
 *
 * &#64;Inject &#64;Push
 * private PushContext someChannel;
 *
 * public void submit() {
 *     someService.someAsyncServiceMethod(entity, new Runnable() {
 *         public void run() {
 *             someChannel.send(entity.getSomeProperty());
 *         }
 *     });
 * }
 * </pre>
 * <p>
 * This would be the only way in case you intend to asynchronously send a message to a session scoped push socket,
 * and/or want to pass something from {@link FacesContext} or the initial request/view/session scope along as
 * (<code>final</code>) argument.
 * <p>
 * Note that OmniFaces own {@link Callback} interfaces are insuitable as you're not supposed to use WAR (front end)
 * frameworks and libraries like JSF and OmniFaces in EAR/EJB (back end) side.
 * <p>
 * In case you're already on Java 8, of course make use of the <code>Consumer</code> functional interface instead of
 * the above {@link Runnable} example.
 * <pre>
 * &#64;Asynchronous
 * public void someAsyncServiceMethod(Entity entity, Consumer&lt;Object&gt; callback) {
 *     // ... (some long process)
 *     callback.accept(entity.getSomeProperty());
 * }
 * </pre>
 * <pre>
 * public void submit() {
 *     someService.someAsyncServiceMethod(entity, message -&gt; someChannel.send(message);
 * }
 * </pre>
 *
 *
 * <h3>UI update design hints</h3>
 * <p>
 * In case you'd like to perform complex UI updates, which would be a piece of cake with JSF ajax, then easiest would
 * be to combine <code>&lt;o:socket&gt;</code> with <code>&lt;o:commandScript&gt;</code> which simply invokes a bean
 * action and ajax-updates the UI once a push message arrives. The combination can look like below:
 * <pre>
 * &lt;h:panelGroup id="foo"&gt;
 *     ... (some complex UI here) ...
 * &lt;/h:panelGroup&gt;
 *
 * &lt;o:socket channel="someChannel" onmessage="someCommandScript" /&gt;
 * &lt;o:commandScript name="someCommandScript" action="#{bean.pushed}" render="foo" /&gt;
 * </pre>
 * <p>
 * If you pass a <code>Map&lt;String,V&gt;</code> or a JavaBean as push message object, then all entries/properties will
 * transparently be available as request parameters in the command script method <code>#{bean.pushed}</code>.
 *
 *
 * @author Bauke Scholtz
 * @see SocketEndpoint
 * @see SocketChannelManager
 * @see SocketEventListener
 * @see SocketManager
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
		"o:socket 'channel' attribute '%s' does not represent a valid channel name."
			+ " It may only contain alphanumeric characters, hyphens, underscores, periods.";
	private static final String ERROR_INVALID_SCOPE =
		"o:socket 'scope' attribute '%s' does not represent a valid scope."
			+ " Allowed values are 'application' and 'session', case insensitive. The default is 'application'.";
	private static final String ERROR_DUPLICATE_CHANNEL =
		"o:socket channel '%s' is already registered on a different scope. Choose an unique channel name for a"
			+ " different channel (or shutdown all browsers and restart the server if you were just testing).";

	// Properties -----------------------------------------------------------------------------------------------------

	private TagAttribute port;
	private TagAttribute channel;
	private TagAttribute scope;
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
		onmessage = getRequiredAttribute("onmessage");
		onclose = getAttribute("onclose");
		connected = getAttribute("connected");
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * First check if the web socket endpoint is enabled in <code>web.xml</code> and the channel name and scope is
	 * valid, then subcribe the {@link SocketEventListener}.
	 * @throws IllegalStateException When the web socket endpoint is not enabled in <code>web.xml</code>.
	 * @throws IllegalArgumentException When the channel name or scope is invalid.
	 * The channel name may only contain alphanumeric characters, hyphens, underscores and periods.
	 * The allowed channel scope values are "application" and "session", case insensitive.
	 * The channel name must be uniquely tied to the channel scope.
	 */
	@Override
	public void apply(FaceletContext context, UIComponent parent) throws IOException {
		if (!ComponentHandler.isNew(parent)) {
			return;
		}

		if (!TRUE.equals(getApplicationAttribute(context.getFacesContext(), Socket.class.getName()))) {
			throw new IllegalStateException(ERROR_ENDPOINT_NOT_ENABLED);
		}

		String channelName = channel.getValue(context);

		if (!PATTERN_CHANNEL.matcher(channelName).matches()) {
			throw new IllegalArgumentException(String.format(ERROR_INVALID_CHANNEL, channelName));
		}

		SocketChannelManager channelManager = getReference(SocketChannelManager.class);
		String scopeName = getString(context, scope);
		String channelId;

		try {
			channelId = channelManager.register(channelName, scopeName);
		}
		catch (IllegalArgumentException ignore) {
			throw new IllegalArgumentException(String.format(ERROR_INVALID_SCOPE, scopeName));
		}

		if (channelId == null) {
			throw new IllegalArgumentException(String.format(ERROR_DUPLICATE_CHANNEL, channelName));
		}

		Integer portNumber = getObject(context, port, Integer.class);
		String onmessageFunction = onmessage.getValue(context);
		String oncloseFunction = getString(context, onclose);
		String functions = onmessageFunction + "," + oncloseFunction;
		ValueExpression connectedExpression = getValueExpression(context, connected, Boolean.class);

		SystemEventListener listener = new SocketEventListener(portNumber, channelName, channelId, functions, connectedExpression);
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