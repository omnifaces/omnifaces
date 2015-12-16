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
package org.omnifaces.cdi.push;

import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.parseBoolean;
import static org.omnifaces.util.Events.subscribeToViewEvent;
import static org.omnifaces.util.Facelets.getObject;
import static org.omnifaces.util.Facelets.getValueExpression;
import static org.omnifaces.util.FacesLocal.getServletContext;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.el.ValueExpression;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
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
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import org.omnifaces.cdi.PushContext;
import org.omnifaces.util.Callback;
import org.omnifaces.util.Json;

/**
 * <p>
 * Opens an one-way (server to client) web socket based push connection in client side which can be reached from
 * server side via {@link PushContext} interface injected in any CDI/container managed artifact.
 *
 *
 * <h3>Usage (client)</h3>
 * <p>
 * Declare <code>&lt;o:socket&gt;</code> in the view with at least a <code>channel</code> name and an
 * <code>onmessage</code> JavaScript listener function.
 * <p>
 * Here's an example which refers an existing JS listener function.
 * <pre>
 * &lt;o:socket channel="global" onmessage="socketListener" /&gt;
 * </pre>
 * <pre>
 * function socketListener(message) {
 *     console.log(message);
 * }
 * </pre>
 * <p>
 * Here's an example which declares an inline JS listener function.
 * <pre>
 * &lt;o:socket channel="global" onmessage="function(message) { console.log(message); }" /&gt;
 * </pre>
 * <p>
 * The <code>onmessage</code> JS listener function will be invoked with three arguments:
 * <ul>
 * <li><code>message</code>: the push message as JSON object.</li>
 * <li><code>channel</code>: the channel name, useful in case you intend to have a global listener, or want to manually
 * control the close.</li>
 * <li><code>event</code>: the raw <a href="https://developer.mozilla.org/en-US/docs/Web/API/MessageEvent"><code>
 * MessageEvent</code></a> instance, useful in case you intend to inspect it.</li>
 * </ul>
 * <p>
 * The optional <code>onclose</code> JS listener function can be used to listen on (ab)normal close of a web socket.
 * <pre>
 * &lt;o:socket ... onclose="socketCloseListener" /&gt;
 * </pre>
 * <pre>
 * function socketCloseListener(code) {
 *     if (code == -1) {
 *         // Web sockets not supported by client.
 *     } else if (code != 1000) {
 *         // Abnormal close reason.
 *     }
 * }
 * </pre>
 * <p>
 * The <code>onclose</code> JS listener function will be invoked with three arguments:
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
 * In case your server is configured to run WS container on a different TCP port than the HTTP container, then you can
 * use the <code>port</code> attribute to explicitly specify the port.
 * <pre>
 * &lt;o:socket ... port="8000" /&gt;
 * </pre>
 *
 *
 * <h3>Usage (server)</h3>
 * <p>
 * In the WAR side (not in EAR/EJB side!), you can inject {@link PushContext} in any CDI/container managed artifact
 * such as <code>@Named</code>, <code>@WebServlet</code>, etc wherever you'd like to send a push message and then invoke
 * {@link PushContext#send(String, Object)} with the channel name and any Java object representing the push message.
 * <pre>
 * &#64;Inject
 * private PushContext pushContext;
 *
 * public void sendMessage(Object message) {
 *     pushContext.send("global", message);
 * }
 * </pre>
 * <p>
 * The message object will be encoded as JSON and be delivered as <code>message</code> argument of the
 * <code>onmessage</code> JavaScript listener function associated with the <code>channel</code> name. It can be a
 * plain vanilla <code>String</code>, but it can also be a collection, map and even a javabean. For supported argument
 * types, see also {@link Json#encode(Object)}.
 * <p>
 * The push is one-way, from server to client. In case you intend to send some data from client to server, just continue
 * using Ajax the usual way, if necessary with <code>&lt;o:commandScript&gt;</code> or perhaps
 * <code>&lt;p:remoteCommand&gt;</code> or similar. This has among others the advantage of maintaining the JSF view
 * state.
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
 * You can also explicitly set it to <code>false</code> and manually open the push connection from client side by
 * invoking <code>OmniFaces.Push.open(channel)</code>, passing the channel name.
 * <pre>
 * &lt;o:socket channel="foo" ... connected="false" /&gt;
 * </pre>
 * <pre>
 * function someFunction() {
 *     // ...
 *     OmniFaces.Push.open("foo");
 * }
 * </pre>
 * <p>
 * The web socket is by default open as long as the document is open. It will be implicitly closed once the document is
 * unloaded (e.g. navigating away, close of browser window/tab, etc). In case you intend to have an one-time push,
 * usually because you only wanted to present the result of an one-time asynchronous action, you can optionally
 * explicitly close the push connection from client side by invoking <code>OmniFaces.Push.close(channel)</code>, passing
 * the channel name. For example, in the <code>onmessage</code> JS listener function as below:
 * <pre>
 * function socketListener(message, channel) {
 *     // ...
 *     OmniFaces.Push.close(channel);
 * }
 * </pre>
 * <p>
 * Noted should be that both ways should not be mixed. Choose either the server side way of an EL expression in
 * <code>connected</code> attribute, or the client side way of manually invoking <code>OmniFaces.Push</code> functions.
 * Mixing them may end up in undefined behavior.
 *
 *
 * <h3>Channel name design hints</h3>
 * <p>
 * With the channel name you can less or more specify the desired push scope. With a static channel name, basically
 * anyone having a push socket open on the same channel will receive the same push message. This is OK for global push
 * messages, but this may not be OK for push messages with sensitive information restricted to specific user(s).
 * <p>
 * To send a push message to a specific user session, append the session ID to channel ID.
 * <pre>
 * &lt;o:socket channel="foo_#{session.id}" ... /&gt;
 * </pre>
 * <pre>
 * public void sendMessage(Object message) {
 *     pushContext.send("foo_" + Faces.getSessionId(), message);
 * }
 * </pre>
 * <p>
 * If you intend to send only to a specific page within the specific user session, make sure that the channel name
 * prefix is specific to the particular page.
 * <pre>
 * &lt;o:socket channel="pagename_#{session.id}" ... /&gt;
 * </pre>
 * <pre>
 * public void sendMessage(Object message) {
 *     pushContext.send("pagename_" + Faces.getSessionId(), message);
 * }
 * </pre>
 * <p>
 * Noted should be that the channel name may only contain alphanumeric characters, hyphens, underscores and periods.
 * <p>
 * Whatever you do, make sure that the user-restricted channel name has an unguessable value, such as the session ID.
 * Otherwise people can easily manually open a web socket listening on a guessed channel name. For example, in case you
 * intend to push a message to users of only a specific role, then encrypt that role name or map it to an UUID and use
 * it in place of the session ID in above examples.
 *
 *
 * <h3>EJB design hints</h3>
 * <p>
 * In case you'd like to trigger a push from EAR/EJB side, you could make use of CDI events. First create a custom bean
 * class representing the push event something like <code>PushEvent</code> taking whatever you'd like to pass as push
 * message.
 * <pre>
 * public class PushEvent {
 *
 *     private String message;
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
 * Then use {@link BeanManager#fireEvent(Object, java.lang.annotation.Annotation...)} to fire the CDI event.
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
 * &#64;Inject
 * private PushContext pushContext;
 *
 * public void onPushEvent(@Observes PushEvent event) {
 *     pushContext.send("someChannel", event.getMessage());
 * }
 * </pre>
 * <p>
 * Note that a request scoped bean wouldn't be the same one as from the originating page for the simple reason that
 * there's no means of a HTTP request anywhere at that moment. For exactly this reason a view and session scoped bean
 * would not work at all (as they require respectively the JSF view state and HTTP session which can only be identified
 * by a HTTP request). Also, the {@link FacesContext} will be unavailable in the method.
 * <p>
 * The alternative would be to make use of callbacks. Let the business service method take a callback instance as
 * argument, e.g {@link Runnable}.
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
 * &#64;Inject
 * private PushContext pushContext;
 *
 * public void submit() {
 *     someService.someAsyncServiceMethod(entity, new Runnable() {
 *         public void run() {
 *             pushContext.send("someChannel", entity.getSomeProperty());
 *         }
 *     });
 * }
 * </pre>
 * <p>
 * This would be the only way in case you intend to pass something from {@link FacesContext} or the initial
 * request/view/session scope along as (<code>final</code>) argument.
 * <p>
 * Note that OmniFaces {@link Callback} interfaces are insuitable as you're not supposed to use WAR (front end)
 * frameworks and libraries like JSF and OmniFaces in EAR/EJB (back end) side!
 * <p>
 * In case you're already on Java 8, of course make use of the <code>Consumer</code> functional interface.
 * <pre>
 * &#64;Asynchronous
 * public void someAsyncServiceMethod(Entity entity, Consumer&lt;Object&gt; callback) {
 *     // ... (some long process)
 *     callback.accept(entity.getSomeProperty());
 * }
 * </pre>
 * <pre>
 * public void submit() {
 *     someService.someAsyncServiceMethod(entity, someProperty -&gt; pushContext.send("someChannel", someProperty);
 * }
 * </pre>
 *
 *
 * <h3>UI update design hints</h3>
 * <p>
 * In case you'd like to perform complex UI updates, which would be a piece of cake with JSF ajax, then easiest would
 * be to combine <code>&lt;o:socket&gt;</code> with <code>&lt;o:commandScript&gt;</code> or perhaps
 * <code>&lt;p:remoteCommand&gt;</code> or similar which simply invokes a bean action and ajax-updates the UI once a
 * push message arrives. The combination can look like below:
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
 * <h3>Configuration</h3>
 * <p>
 * The web socket endpoint associated with <code>&lt;o:socket&gt;</code> is only lazily initialized. So as long as
 * you don't use <code>&lt;o:socket&gt;</code> anywhere in the application, then the web socket endpoint is not open.
 * However, the lazy initialization fails in case Tyrus is used as web socket implementation (GlassFish/Payara). It
 * only supports programmatic initialization during webapp's startup, otherwise it will throw the below exception:
 * <pre>
 * java.lang.IllegalStateException: Not in 'deploy' scope.
 *     at org.glassfish.tyrus.server.TyrusServerContainer.addEndpoint
 *     at org.omnifaces.cdi.push.Socket.registerEndpointIfNecessary
 *     at org.omnifaces.cdi.push.Socket.apply
 * </pre>
 * <p>
 * If you're facing this exception, then you need to add the below context parameter to your webapp's
 * <code>web.xml</code> to explicitly register the web socket endpoint during webapp's startup.
 * <pre>
 * &lt;context-param&gt;
 *     &lt;param-name&gt;org.omnifaces.SOCKET_ENDPOINT_ALWAYS_ENABLED&lt;/param-name&gt;
 *     &lt;param-value&gt;true&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * </pre>
 * <p>
 * This is not necessary if you're not facing an exception like above, but may be useful if you always want to keep it
 * open as it's not automatically reopened after a server restart until a page with <code>&lt;o:socket&gt;</code> has
 * been requested.
 *
 * @author Bauke Scholtz
 * @see SocketEventListener
 * @see SocketEndpoint
 * @see SocketChannelFilter
 * @see SocketPushContext
 * @see PushContext
 * @since 2.3
 */
public class Socket extends TagHandler {

	// Constants ------------------------------------------------------------------------------------------------------

	/** The context parameter name to explicitly register web socket endpoint during startup (only required in some containers). */
	public static final String PARAM_SOCKET_ENDPOINT_ALWAYS_ENABLED = "org.omnifaces.SOCKET_ENDPOINT_ALWAYS_ENABLED";

	private static final Pattern PATTERN_CHANNEL_NAME = Pattern.compile("[\\w.-]+");

	private static final String ERROR_ENDPOINT_REGISTRATION =
		"o:socket endpoint cannot be registered at this moment."
			+ " To solve this, add context param '" + PARAM_SOCKET_ENDPOINT_ALWAYS_ENABLED + "' with value 'true'.";
	private static final String ERROR_ILLEGAL_CHANNEL_NAME =
		"o:socket 'channel' attribute '%s' does not represent a valid channel name."
			+ " It may only contain alphanumeric characters, hyphens, underscores and periods.";

	// Properties -----------------------------------------------------------------------------------------------------

	private TagAttribute port;
	private TagAttribute channel;
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
		onmessage = getRequiredAttribute("onmessage");
		onclose = getAttribute("onclose");
		connected = getAttribute("connected");
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Register the push channel and endpoint if necessary and then subcribe the {@link SocketEventListener}.
	 * Note thus that any manually triggered push requests on unregistered channels will cause an exception, and that
	 * the push endpoint is only activated when the <code>&lt;o:socket&gt;</code> is actually used in the application.
	 * @throws IllegalArgumentException When the channel name is invalid.
	 * It may only contain alphanumeric characters, hyphens, underscores and periods.
	 */
	@Override
	public void apply(FaceletContext context, UIComponent parent) throws IOException {
		if (!ComponentHandler.isNew(parent)) {
			return;
		}

		String channelName = channel.getValue(context);

		if (!PATTERN_CHANNEL_NAME.matcher(channelName).matches()) {
			throw new IllegalArgumentException(String.format(ERROR_ILLEGAL_CHANNEL_NAME, channelName));
		}

		FacesContext facesContext = context.getFacesContext();
		registerEndpointIfNecessary(getServletContext(facesContext), false);

		Integer portNumber = getObject(context, port, Integer.class);
		String onmessageFunction = onmessage.getValue(context);
		String oncloseFunction = (onclose != null) ? onclose.getValue(context) : null;
		String functions = onmessageFunction + "," + oncloseFunction;
		ValueExpression connectedExpression = getValueExpression(context, connected, Boolean.class);

		SystemEventListener listener = new SocketEventListener(portNumber, channelName, functions, connectedExpression);
		subscribeToViewEvent(PostAddToViewEvent.class, listener);
		subscribeToViewEvent(PreRenderViewEvent.class, listener);
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Register web socket endpoint if necessary, i.e. when it's not registered yet, or is explicitly enabled via
	 * context param.
	 * @param context The involved servlet context.
	 * @param checkParam Whether to check the context param or not.
	 */
	public static void registerEndpointIfNecessary(ServletContext context, boolean checkParam) {
		Boolean registered = (Boolean) context.getAttribute(Socket.class.getName());

		if (TRUE.equals(registered) || (checkParam && !parseBoolean(context.getInitParameter(PARAM_SOCKET_ENDPOINT_ALWAYS_ENABLED)))) {
			return;
		}

		try {
			ServerContainer serverContainer = (ServerContainer) context.getAttribute(ServerContainer.class.getName());
			ServerEndpointConfig serverEndpointConfig = ServerEndpointConfig.Builder.create(SocketEndpoint.class, SocketEndpoint.URI_TEMPLATE).build();
			serverContainer.addEndpoint(serverEndpointConfig);
			context.setAttribute(Socket.class.getName(), TRUE);
		}
		catch (DeploymentException e) {
			throw new FacesException(ERROR_ENDPOINT_REGISTRATION, e);
		}
	}

}