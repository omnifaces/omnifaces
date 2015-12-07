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

import static org.omnifaces.util.Components.addScriptResourceToHead;
import static org.omnifaces.util.Components.addScriptToBody;
import static org.omnifaces.util.Events.subscribeToViewEvent;
import static org.omnifaces.util.Facelets.getValueExpression;
import static org.omnifaces.util.Faces.getRequestContextPath;
import static org.omnifaces.util.Utils.isEmpty;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.el.ValueExpression;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import javax.faces.component.UIComponent;
import javax.faces.event.PostAddToViewEvent;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;
import javax.websocket.CloseReason.CloseCodes;

import org.omnifaces.util.Callback;
import org.omnifaces.util.Json;

/**
 * <p>
 * Creates a web socket based push connection in client side which can be reached via {@link PushContext}.
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
 * The web socket is by default open as long as the document is open. It will be implicitly closed once the document is
 * unloaded (e.g. navigating away, close of browser window/tab, etc). In case you intend to have an one-time push,
 * usually because you only wanted to present the result of an one-time asynchronous action, you can optionally
 * explicitly close the channel from client side by invoking <code>OmniFaces.Push.close(channel)</code>, passing the
 * channel name. For example, in the <code>onmessage</code> JS listener function as below:
 * <pre>
 * function socketListener(message, channel) {
 *     // ...
 *     OmniFaces.Push.close(channel);
 * }
 * </pre>
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
 * <h3>EJB design hints</h3>
 * <p>
 * In case you'd like to trigger a push from EAR/EJB side, make use of CDI events. First create a custom bean class
 * representing the push event something like <code>PushEvent</code> taking whatever you'd like to pass as push message.
 * Then use {@link BeanManager#fireEvent(Object, java.lang.annotation.Annotation...)} to fire the CDI event.
 * <pre>
 * &#64;Inject
 * private BeanManager beanManager;
 *
 * public void onSomeEntityChange(Entity entity) {
 *     beanManager.fireEvent(new PushEvent(entity));
 * }
 * </pre>
 * <p>
 * Finally just {@link Observes} it in some CDI managed bean in WAR and delegate to {@link PushContext} as below.
 * <pre>
 * &#64;Inject
 * private PushContext pushContext;
 *
 * public void onPushEvent(@Observes PushEvent event) {
 *     pushContext.send("someChannel", event.getMessage());
 * }
 * </pre>
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
 *
 *
 * <h3>UI update design hints</h3>
 * <p>
 * In case you'd like to perform complex UI updates, which would be a piece of cake with JSF ajax, then easiest would
 * be to combine <code>&lt;o:socket&gt;</code> with <code>&lt;o:commandScript&gt;</code> or perhaps
 * <code>&lt;p:remoteCommand&gt;</code> or similar which simply invokes a bean action and ajax-updates the UI. The
 * combination may then look like below:
 * <pre>
 * &lt;h:panelGroup id="foo"&gt;
 *     ... (some complex UI here) ...
 * &lt;/h:panelGroup&gt;
 *
 * &lt;o:socket channel="someChannel" onmessage="someCommandScript" /&gt;
 * &lt;o:commandScript name="someCommandScript" action="#{bean.pushed}" render="foo" /&gt;
 * </pre>
 * <p>
 * If you pass a <code>Map&lt;K,V&gt;</code> or a JavaBean as push message object, then all entries/properties will
 * transparently be available as request parameters in the command script method <code>#{bean.pushed}</code>.
 *
 * @author Bauke Scholtz
 * @see SocketEndpoint
 * @see PushContext
 * @see SocketPushContext
 * @since 2.3
 */
public class Socket extends TagHandler {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String SCRIPT = "OmniFaces.Push.open('%s','%s',%s);";
	private static final Pattern PATTERN_CHANNEL_NAME = Pattern.compile("[\\w.-]+");
	private static final Pattern PATTERN_SCRIPT_NAME = Pattern.compile("[$a-z_][$\\w]*", Pattern.CASE_INSENSITIVE);
	private static final String ERROR_ILLEGAL_CHANNEL_NAME =
		"o:socket 'channel' attribute '%s' does not represent a valid channel name."
			+ " It may only contain alphanumeric characters, hyphens, underscores and periods.";

	// Properties -----------------------------------------------------------------------------------------------------

	private TagAttribute channel;
	private TagAttribute onmessage;
	private TagAttribute onclose;
	private TagAttribute enabled;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * The tag constructor. It will extract and validate the attributes.
	 * @param config The tag config.
	 * @throws IllegalArgumentException When the channel name is invalid. It must be URL safe.
	 */
	public Socket(TagConfig config) {
		super(config);
		channel = getRequiredAttribute("channel");
		onmessage = getRequiredAttribute("onmessage");
		onclose = getAttribute("onclose");
		enabled = getAttribute("enabled");
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Register the push script and add initialization script to end of body.
	 */
	@Override
	public void apply(FaceletContext context, UIComponent parent) throws IOException {
		if (!ComponentHandler.isNew(parent)) {
			return;
		}

		final String name = channel.getValue(context);

		if (!PATTERN_CHANNEL_NAME.matcher(name).matches()) {
			throw new IllegalArgumentException(String.format(ERROR_ILLEGAL_CHANNEL_NAME, name));
		}

		String onmessageFunction = quoteIfNecessary(onmessage.getValue(context));
		String oncloseFunction = (onclose != null) ? quoteIfNecessary(onclose.getValue(context)) : null;
		final String functions = onmessageFunction + (oncloseFunction != null ? ("," + oncloseFunction) : "");
		final ValueExpression rendered = getValueExpression(context, enabled, Boolean.class);

		subscribeToViewEvent(PostAddToViewEvent.class, new Callback.SerializableVoid() {
			private static final long serialVersionUID = 1L;

			@Override
			public void invoke() {
				addScriptResourceToHead("omnifaces", "omnifaces.js");
				addScriptToBody(String.format(SCRIPT, getRequestContextPath(), name, functions)).setValueExpression("rendered", rendered);
			}
		});
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	private static String quoteIfNecessary(String script) {
		return isEmpty(script) ? "null" : PATTERN_SCRIPT_NAME.matcher(script).matches() ? ("'" + script + "'") : script;
	}

}