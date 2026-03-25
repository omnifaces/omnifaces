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

import static java.util.Objects.requireNonNull;
import static org.omnifaces.config.OmniFaces.OMNIFACES_LIBRARY_NAME;
import static org.omnifaces.config.OmniFaces.OMNIFACES_SCRIPT_NAME;
import static org.omnifaces.util.FacesLocal.getRequestContextPath;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;

import jakarta.faces.application.ResourceDependency;
import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;

import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;
import org.omnifaces.config.OmniFaces;
import org.omnifaces.resourcehandler.PWAResourceHandler;
import org.omnifaces.util.Json;

/**
 * <p>
 * The <code>&lt;o:notification&gt;</code> is a {@link UIComponent} which integrates the browser
 * <a href="https://notifications.spec.whatwg.org/">Web Notifications API</a> with SSE (Server-Sent Events) based push.
 * It opens a one-way (server to client) SSE connection and shows incoming push messages as browser notifications.
 *
 * <h2 id="prerequisites"><a href="#prerequisites">Prerequisites</a></h2>
 * <p>
 * This component requires the {@link PWAResourceHandler} to be activated by adding the following line to the
 * <code>&lt;h:head&gt;</code> because it provides the service worker mandatory for cross-platform notification support
 * via {@code ServiceWorkerRegistration.showNotification()}:
 * <pre>
 * &lt;link rel="manifest" href="#{resource['omnifaces:manifest.webmanifest']}" /&gt;
 * </pre>
 * <p>
 * The browser notification permission must be requested via a user gesture before any notification can be shown:
 * <pre>
 * &lt;button type="button" onclick="OmniFaces.Notification.requestPermission()"&gt;Enable Notifications&lt;/button&gt;
 * </pre>
 * <p>
 * The CDI backing bean must have at least one <code>&#64;</code>{@link Push}<code>(type=NOTIFICATION)</code> qualified
 * injection point using the channel name matching the <code>channel</code> attribute of this component, so that the
 * {@link SseEndpoint} will be auto-activated.
 *
 * <h2 id="basic-usage"><a href="#basic-usage">Basic Usage</a></h2>
 * <p>
 * Declare the <code>&lt;o:notification&gt;</code> tag in the Faces view with a <strong><code>channel</code></strong>
 * name.
 * <pre>
 * &lt;o:notification channel="notifications" /&gt;
 * </pre>
 * <p>
 * In the server side, inject the push context and send notification messages created via
 * {@link Notification#createNotificationMessage(String, String)}.
 * <pre>
 * &#64;Inject &#64;Push(type=NOTIFICATION)
 * private PushContext notifications;
 *
 * public void sendNotification() {
 *     notifications.send(Notification.createNotificationMessage("Order shipped", "Your order #1234 has been shipped."));
 * }
 * </pre>
 * <p>
 * You can also add a URL so that clicking the notification will navigate to it:
 * <pre>
 * notifications.send(Notification.createNotificationMessage("Order shipped", "Your order has been shipped.", "https://example.com/orders/1234"));
 * </pre>
 * <p>
 * Relative URLs are also accepted:
 * <pre>
 * notifications.send(Notification.createNotificationMessage("Order shipped", "Your order has been shipped.", "/orders/1234"));
 * </pre>
 *
 * <h2 id="user-targeted"><a href="#user-targeted">User-targeted Notifications</a></h2>
 * <p>
 * The optional <strong><code>user</code></strong> attribute can be set to the unique identifier of the logged-in user,
 * so that notifications can be targeted to a specific user.
 * <pre>
 * &lt;o:notification channel="notifications" user="#{request.remoteUser}" /&gt;
 * </pre>
 * <p>
 * The notification can then be sent to a specific user:
 * <pre>
 * notifications.send(Notification.createNotificationMessage("New message", "You have a new message."), recipientUserId);
 * </pre>
 *
 * <h2 id="display-behavior"><a href="#display-behavior">Display behavior</a></h2>
 * <p>
 * By default, each new notification replaces the previous one from the same component. To let notifications stack
 * independently instead, set the <strong><code>stacked</code></strong> attribute to <code>true</code>:
 * <pre>
 * &lt;o:notification channel="notifications" stacked="true" /&gt;
 * </pre>
 * <p>
 * The <strong><code>requireInteraction</code></strong> attribute controls whether the notification remains visible
 * until the user explicitly interacts with it (click or dismiss), rather than auto-closing after a timeout. This is
 * useful for important alerts that should not be missed:
 * <pre>
 * &lt;o:notification channel="alerts" requireInteraction="true" /&gt;
 * </pre>
 * <p>
 * The <strong><code>silent</code></strong> attribute suppresses the device's notification sound and vibration:
 * <pre>
 * &lt;o:notification channel="updates" silent="true" /&gt;
 * </pre>
 *
 * <h2 id="event-handlers"><a href="#event-handlers">Event Handlers</a></h2>
 * <p>
 * The <code>&lt;o:notification&gt;</code> supports click and close event handlers.
 * <pre>
 * &lt;o:notification channel="notifications" onclick="handleClick" onclose="handleClose" /&gt;
 * </pre>
 * <p>
 * The event's <code>detail</code> object contains <code>data</code> and <code>tag</code>. The <code>data</code> object
 * contains the <code>channel</code> name and the optional <code>url</code>. The <code>tag</code> is set to the
 * component's client ID (when not {@link #setStacked(boolean) stacked}) and is used by the browser for notification
 * deduplication: a new notification with the same tag replaces the previous one instead of stacking.
 * <pre>
 * function handleClick(event) {
 *     console.log("Channel: " + event.detail.data.channel);
 *     console.log("URL (if any): " + event.detail.data.url);
 *     console.log("Tag (if not stacked): " + event.detail.tag);
 * }
 * function handleClose(event) {
 *     console.log("Channel: " + event.detail.data.channel);
 *     console.log("Tag (if not stacked): " + event.detail.tag);
 * }
 * </pre>
 * <p>
 * When you need to invoke a server-side action on notification click, use <code>&lt;h:commandScript&gt;</code>
 * to bridge the client-side event to the server. For example, to mark a notification as read when the user clicks it:
 * <pre>
 * &lt;o:notification channel="notifications" onclick="handleNotificationClick" /&gt;
 * &lt;h:form&gt;
 *     &lt;h:commandScript name="markAsRead" action="#{notificationBean.markAsRead}" /&gt;
 * &lt;/h:form&gt;
 * </pre>
 * <p>
 * With this handler:
 * <pre>
 * function handleNotificationClick(event) {
 *     markAsRead({ "notification.url": event.detail.data?.url });
 * }
 * </pre>
 * <p>
 * And this bean:
 * <pre>
 * public void markAsRead() {
 *     var url = Faces.getRequestParameter("notification.url");
 *     // ...
 * }
 * </pre>
 *
 * <h2 id="javascript-api"><a href="#javascript-api">JavaScript API</a></h2>
 * <p>
 * The current permission can be checked via <code>OmniFaces.Notification.getPermission()</code>, which returns
 * <code>"granted"</code>, <code>"denied"</code>, <code>"default"</code>, or <code>"unsupported"</code>.
 * <pre>
 * const notificationPermission = OmniFaces.Notification.getPermission();
 * </pre>
 * <p>
 * You can use <code>OmniFaces.Notification.show</code> to programmatically show notifications using JavaScript.
 * The first argument is the channel name, the second the title, the optional third the body, and the optional fourth a
 * URL:
 * <pre>
 * OmniFaces.Notification.show("notifications", "Please wait ...");
 * OmniFaces.Notification.show("notifications", "Hello!", "This is a notification.");
 * OmniFaces.Notification.show("notifications", "Click me!", "Opens a link.", "https://omnifaces.org");
 * </pre>
 *
 * <h2 id="platform-limitations"><a href="#platform-limitations">Platform limitations</a></h2>
 * <p>
 * The <code>icon</code> attribute is passed to the Notifications API, but custom notification icons are not
 * guaranteed to work across all platforms. As of March 2026, macOS and Linux with GNOME always display the
 * browser's own application icon instead, due to OS/desktop-environment-level policies. Custom icons do work on
 * Windows and Android. Use PNG format at 192x192 or larger for best results.
 *
 * @author Bauke Scholtz
 * @see PWAResourceHandler
 * @see Sse
 * @see SseEndpoint
 * @see Push
 * @see PushContext
 * @since 5.2
 */
@FacesComponent(value = Notification.COMPONENT_TYPE, namespace = OmniFaces.OMNIFACES_NAMESPACE)
@ResourceDependency(library = OMNIFACES_LIBRARY_NAME, name = OMNIFACES_SCRIPT_NAME, target = "head")
public class Notification extends ChannelComponent {

    // Public constants -----------------------------------------------------------------------------------------------

    /** The component type, which is {@value org.omnifaces.cdi.push.Notification#COMPONENT_TYPE}. */
    public static final String COMPONENT_TYPE = "org.omnifaces.cdi.push.Notification";

    // Public inner types ---------------------------------------------------------------------------------------------

    /**
     * Represents a notification message for use with {@code PushContext.send()}.
     * @param title The notification title.
     * @param body Optional notification body text. May be {@code null}.
     * @param url Optional URL to open when the notification is clicked. May be {@code null}.
     */
    public record Message(String title, String body, String url) {
        /** Validates that the title is not null. */
        public Message {
            requireNonNull(title, "title");
        }
    }

    // Public static methods ------------------------------------------------------------------------------------------

    /**
     * Creates a notification message with only a title for use with {@code PushContext.send()}.
     * @param title The notification title.
     * @return A new {@link Message} instance.
     */
    public static Message createNotificationMessage(String title) {
        return new Message(title, null, null);
    }

    /**
     * Creates a notification message for use with {@code PushContext.send()}.
     * @param title The notification title.
     * @param body The notification body text.
     * @return A new {@link Message} instance.
     */
    public static Message createNotificationMessage(String title, String body) {
        return new Message(title, body, null);
    }

    /**
     * Creates a notification message with a navigation URL for use with {@code PushContext.send()}.
     * Clicking the notification will navigate to the specified URL.
     * @param title The notification title.
     * @param body The notification body text.
     * @param url The URL to navigate to when the notification is clicked. Both absolute and relative URLs are accepted.
     * @return A new {@link Message} instance.
     * @throws IllegalArgumentException When the URL syntax is invalid.
     */
    public static Message createNotificationMessage(String title, String body, String url) {
        URI.create(url);
        return new Message(title, body, url);
    }

    // Private constants ----------------------------------------------------------------------------------------------

    private static final String SCOPE_SESSION = "session";

    private static final String ERROR_MANIFEST_RESOURCE_NOT_REFERENCED =
        "o:notification requires the web app manifest resource to be referenced in the view."
            + " Add <link rel=\"manifest\" href=\"#{resource['omnifaces:manifest.webmanifest']}\" /> to <h:head>."
            + " It provides the service worker needed for cross-platform notification support.";

    private static final String SCRIPT_INIT = "OmniFaces.Notification.init('%s',%s);";
    private static final String SSE_FUNCTIONS = "null,OmniFaces.Notification.handle,null,null";
    private static final String SSE_BEHAVIORS = "{}";

    private enum PropertyKeys {
        // Cannot be uppercased. They have to exactly match the attribute names.
        stacked, requireInteraction, silent, icon,
        onclick, onclose, onerror, onpermissionchange, onunsupported;
    }

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * First check if the SSE endpoint is registered and the web app manifest resource is referenced, then validate and
     * register the channel in {@link SseChannelManager}, then render the <code>Notification.init()</code> and
     * <code>Push.init()</code> scripts.
     * @throws IllegalStateException When the SSE endpoint is not registered or the web app manifest resource is not
     * referenced.
     * @throws IllegalArgumentException When the channel name is invalid.
     */
    @Override
    public void encodeChildren(FacesContext context) throws IOException {
        Sse.checkEndpointRegistered(context);

        if (!PWAResourceHandler.isActive(context)) {
            throw new IllegalStateException(ERROR_MANIFEST_RESOURCE_NOT_REFERENCED);
        }

        var contextPath = getRequestContextPath(context);
        var channelId = Sse.registerChannel(context, this, SCOPE_SESSION);
        var writer = context.getResponseWriter();

        writer.write(Sse.SCRIPT_INIT.formatted(contextPath, channelId, SSE_FUNCTIONS, SSE_BEHAVIORS));
        writer.write(SCRIPT_INIT.formatted(getChannel(), buildInitOptionsJson(getClientId(context))));
    }

    // Attribute getters/setters --------------------------------------------------------------------------------------

    /**
     * Returns whether notifications should be stacked instead of replaced.
     * @return Whether notifications are stacked.
     */
    public boolean isStacked() {
        return state.get(PropertyKeys.stacked, false);
    }

    /**
     * Sets whether notifications should be stacked instead of replaced. Defaults to <code>false</code>, meaning each
     * new notification replaces the previous one (deduplication by component client ID as tag). When set to
     * <code>true</code>, no tag is set and each notification is displayed independently.
     * @param stacked Whether notifications should be stacked.
     */
    public void setStacked(boolean stacked) {
        state.put(PropertyKeys.stacked, stacked);
    }

    /**
     * Returns whether the notification requires interaction to be dismissed.
     * @return Whether the notification requires interaction.
     */
    public boolean isRequireInteraction() {
        return state.get(PropertyKeys.requireInteraction, false);
    }

    /**
     * Sets whether the notification requires interaction to be dismissed. Defaults to <code>false</code>. Maps to the
     * Notification API <code>requireInteraction</code> option.
     * @param requireInteraction Whether the notification requires interaction.
     */
    public void setRequireInteraction(boolean requireInteraction) {
        state.put(PropertyKeys.requireInteraction, requireInteraction);
    }

    /**
     * Returns whether the notification should be silent (no sound/vibration).
     * @return Whether the notification is silent.
     */
    public boolean isSilent() {
        return state.get(PropertyKeys.silent, false);
    }

    /**
     * Sets whether the notification should be silent (no sound/vibration). Defaults to <code>false</code>. Maps to
     * the Notification API <code>silent</code> option.
     * @param silent Whether the notification is silent.
     */
    public void setSilent(boolean silent) {
        state.put(PropertyKeys.silent, silent);
    }

    /**
     * Returns the notification icon URL.
     * @return The notification icon URL.
     */
    public String getIcon() {
        return state.get(PropertyKeys.icon);
    }

    /**
     * Sets the notification icon URL. Maps to the Notification API <code>icon</code> option.
     * @param icon The notification icon URL.
     */
    public void setIcon(String icon) {
        state.put(PropertyKeys.icon, icon);
    }

    /**
     * Returns the JavaScript event handler function that is invoked when a notification is clicked.
     * @return The onclick handler.
     */
    public String getOnclick() {
        return state.get(PropertyKeys.onclick);
    }

    /**
     * Sets the JavaScript event handler function that is invoked when a notification is clicked. The function will be
     * invoked with one argument: the <code>CustomEvent</code> with notification details.
     * @param onclick The onclick handler.
     */
    public void setOnclick(String onclick) {
        state.put(PropertyKeys.onclick, onclick);
    }

    /**
     * Returns the JavaScript event handler function that is invoked when a notification is closed.
     * @return The onclose handler.
     */
    public String getOnclose() {
        return state.get(PropertyKeys.onclose);
    }

    /**
     * Sets the JavaScript event handler function that is invoked when a notification is closed. The function will be
     * invoked with one argument: the <code>CustomEvent</code> with notification details.
     * @param onclose The onclose handler.
     */
    public void setOnclose(String onclose) {
        state.put(PropertyKeys.onclose, onclose);
    }

    /**
     * Returns the JavaScript event handler function that is invoked when a notification error occurs.
     * @return The onerror handler.
     */
    public String getOnerror() {
        return state.get(PropertyKeys.onerror);
    }

    /**
     * Sets the JavaScript event handler function that is invoked when a notification error occurs. The function will
     * be invoked with one argument: the error event.
     * @param onerror The onerror handler.
     */
    public void setOnerror(String onerror) {
        state.put(PropertyKeys.onerror, onerror);
    }

    /**
     * Returns the JavaScript event handler function that is invoked when the notification permission changes.
     * @return The onpermissionchange handler.
     */
    public String getOnpermissionchange() {
        return state.get(PropertyKeys.onpermissionchange);
    }

    /**
     * Sets the JavaScript event handler function that is invoked when the notification permission changes. The
     * function will be invoked with one argument: the permission string (<code>"granted"</code>,
     * <code>"denied"</code>, or <code>"default"</code>).
     * @param onpermissionchange The onpermissionchange handler.
     */
    public void setOnpermissionchange(String onpermissionchange) {
        state.put(PropertyKeys.onpermissionchange, onpermissionchange);
    }

    /**
     * Returns the JavaScript event handler function that is invoked when the Notifications API is not supported.
     * @return The onunsupported handler.
     */
    public String getOnunsupported() {
        return state.get(PropertyKeys.onunsupported);
    }

    /**
     * Sets the JavaScript event handler function that is invoked when the Notifications API is not supported or the
     * service worker is not available. The function will be invoked with no arguments.
     * @param onunsupported The onunsupported handler.
     */
    public void setOnunsupported(String onunsupported) {
        state.put(PropertyKeys.onunsupported, onunsupported);
    }

    // Helpers --------------------------------------------------------------------------------------------------------

    private String buildInitOptionsJson(String clientId) {
        var options = new LinkedHashMap<String, Object>();

        if (!isStacked()) {
            options.put("tag", clientId);
        }

        for (var key : PropertyKeys.values()) {
            if (key == PropertyKeys.stacked) {
                continue;
            }

            var value = state.get(key);

            if (value != null && !Boolean.FALSE.equals(value)) {
                options.put(key.toString(), value);
            }
        }

        return Json.encode(options);
    }

}
