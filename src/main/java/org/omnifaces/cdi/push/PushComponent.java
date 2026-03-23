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

import static jakarta.faces.component.behavior.ClientBehaviorContext.BEHAVIOR_EVENT_PARAM_NAME;
import static jakarta.faces.component.behavior.ClientBehaviorContext.BEHAVIOR_SOURCE_PARAM_NAME;
import static jakarta.faces.component.behavior.ClientBehaviorContext.createClientBehaviorContext;
import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;
import static org.omnifaces.util.FacesLocal.getRequestParameter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import jakarta.el.ValueExpression;
import jakarta.faces.component.behavior.ClientBehaviorHolder;
import jakarta.faces.context.FacesContext;

import org.omnifaces.component.script.ScriptFamily;
import org.omnifaces.util.State;
import org.omnifaces.vdl.FacesAttribute;

/**
 * <p>
 * Base class for push components ({@link Socket} and {@link Sse}) that share common attributes, validation, client
 * behavior handling and property accessors for channel, scope, user, onopen, onmessage, onerror and onclose.
 *
 * @author Bauke Scholtz
 * @see Socket
 * @see Sse
 * @since 5.2
 */
abstract class PushComponent extends ScriptFamily implements ClientBehaviorHolder {

    // Constants ------------------------------------------------------------------------------------------------------

    static final Pattern PATTERN_CHANNEL = Pattern.compile("[\\w.-]+");

    private static final Collection<String> CONTAINS_EVERYTHING = unmodifiableList(new ArrayList<String>() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean contains(Object object) {
            return true;
        }
    });

    private enum PropertyKeys {
        // Cannot be uppercased. They have to exactly match the attribute names.
        channel, scope, user, onopen, onmessage, onerror, onclose;
    }

    // Variables ------------------------------------------------------------------------------------------------------

    final State state = new State(getStateHelper());

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
            throw new IllegalArgumentException(
                getClass().getSimpleName() + " 'channel' and 'scope' attributes may not contain an EL expression.");
        }

        if (PropertyKeys.user.toString().equals(name)) {
            var user = binding.getValue(getFacesContext().getELContext());

            if (user != null && !(user instanceof Serializable)) {
                throw new IllegalArgumentException(format(
                        getClass().getSimpleName() + " 'user' attribute '%s' does not represent a valid user identifier."
                        + " It must implement Serializable and preferably have low memory footprint."
                        + " Suggestion: use #{request.remoteUser} or #{someLoggedInUser.id}.", user));
            }
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
     * Decode client behaviors.
     */
    @Override
    public void decode(FacesContext context) {
        var clientBehaviors = getClientBehaviors();

        if (clientBehaviors.isEmpty() || !getClientId(context).equals(getRequestParameter(context, BEHAVIOR_SOURCE_PARAM_NAME))) {
            return;
        }

        var behaviors = clientBehaviors.get(getRequestParameter(context, BEHAVIOR_EVENT_PARAM_NAME));

        if (behaviors == null) {
            return;
        }

        for (var behavior : behaviors) {
            behavior.decode(context, this);
        }
    }

    /**
     * Build the client behavior scripts map as a JavaScript object literal.
     * @return The client behavior scripts as a JavaScript object literal string.
     */
    String getBehaviorScripts() {
        var clientBehaviorsByEvent = getClientBehaviors();

        if (clientBehaviorsByEvent.isEmpty()) {
            return "{}";
        }

        var clientId = getClientId(getFacesContext());
        var scripts = new StringBuilder("{");

        for (var entry : clientBehaviorsByEvent.entrySet()) {
            var event = entry.getKey();
            var clientBehaviors = entry.getValue();
            scripts.append(scripts.length() > 1 ? "," : "").append(event).append(":[");

            for (var i = 0; i < clientBehaviors.size(); i++) {
                scripts.append(i > 0 ? "," : "").append("function(event){");
                scripts.append(clientBehaviors.get(i).getScript(createClientBehaviorContext(getFacesContext(), this, event, clientId, null)));
                scripts.append("}");
            }

            scripts.append("]");
        }

        return scripts.append("}").toString();
    }

    /**
     * Validate the channel name.
     * @param channel The channel name to validate.
     * @throws IllegalArgumentException When the channel name is invalid.
     */
    void validateChannel(String channel) {
        if (channel == null || !PATTERN_CHANNEL.matcher(channel).matches()) {
            throw new IllegalArgumentException(format(
                getClass().getSimpleName() + " 'channel' attribute '%s' does not represent a valid channel name."
                    + " It is required and it may only contain alphanumeric characters, hyphens, underscores and periods.", channel));
        }
    }

    // Attribute getters/setters --------------------------------------------------------------------------------------

    /**
     * Returns the name of the push channel.
     * @return The name of the push channel.
     */
    public String getChannel() {
        return state.get(PropertyKeys.channel);
    }

    /**
     * Sets the name of the push channel.
     * It may not be an EL expression and it may only contain alphanumeric characters, hyphens, underscores and periods.
     * All open connections on the same channel will receive the same push message from the server.
     * @param channel The name of the push channel.
     */
    @FacesAttribute(required = true)
    public void setChannel(String channel) {
        state.put(PropertyKeys.channel, channel);
    }

    /**
     * Returns the scope of the push channel.
     * @return The scope of the push channel.
     */
    public String getScope() {
        return state.get(PropertyKeys.scope);
    }

    /**
     * Sets the scope of the push channel.
     * It may not be an EL expression and allowed values are <code>application</code>, <code>session</code> and
     * <code>view</code>, case insensitive. When the value is <code>application</code>, then all channels with the same
     * name throughout the application will receive the same push message. When the value is <code>session</code>, then
     * only the channels with the same name in the current user session will receive the same push message. When the
     * value is <code>view</code>, then only the channel in the current view will receive the push message. The default
     * scope is <code>application</code>. When the <code>user</code> attribute is specified, then the default scope is
     * <code>session</code>.
     * @param scope The scope of the push channel.
     */
    public void setScope(String scope) {
        state.put(PropertyKeys.scope, scope);
    }

    /**
     * Returns the user identifier of the push channel.
     * @return The user identifier of the push channel.
     */
    public Serializable getUser() {
        return state.get(PropertyKeys.user);
    }

    /**
     * Sets the user identifier of the push channel, so that user-targeted push messages can be sent.
     * All open connections on the same channel and user will receive the same push message from the server.
     * It must implement <code>Serializable</code> and preferably have low memory footprint.
     * Suggestion: use <code>#{request.remoteUser}</code> or <code>#{someLoggedInUser.id}</code>.
     * @param user The user identifier of the push channel.
     */
    public void setUser(Serializable user) {
        state.put(PropertyKeys.user, user);
    }

    /**
     * Returns the JavaScript event handler function that is invoked when the push connection is opened.
     * @return The JavaScript event handler function that is invoked when the push connection is opened.
     */
    public String getOnopen() {
        return state.get(PropertyKeys.onopen);
    }

    /**
     * Sets the JavaScript event handler function that is invoked when the push connection is opened.
     * The function will be invoked with one argument: the channel name.
     * @param onopen The JavaScript event handler function that is invoked when the push connection is opened.
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
     * The function will be invoked with three arguments: the push message, the channel name and the raw
     * {@code MessageEvent} itself.
     * @param onmessage The JavaScript event handler function that is invoked when a push message is received from the
     * server.
     */
    @FacesAttribute(required = true)
    public void setOnmessage(String onmessage) {
        state.put(PropertyKeys.onmessage, onmessage);
    }

    /**
     * Returns the JavaScript event handler function that is invoked when a connection error has occurred.
     * @return The JavaScript event handler function that is invoked when a connection error has occurred.
     */
    public String getOnerror() {
        return state.get(PropertyKeys.onerror);
    }

    /**
     * Sets the JavaScript event handler function that is invoked when a connection error has occurred.
     * @param onerror The JavaScript event handler function that is invoked when a connection error has occurred.
     */
    public void setOnerror(String onerror) {
        state.put(PropertyKeys.onerror, onerror);
    }

    /**
     * Returns the JavaScript event handler function that is invoked when the push connection is closed.
     * @return The JavaScript event handler function that is invoked when the push connection is closed.
     */
    public String getOnclose() {
        return state.get(PropertyKeys.onclose);
    }

    /**
     * Sets the JavaScript event handler function that is invoked when the push connection is closed.
     * @param onclose The JavaScript event handler function that is invoked when the push connection is closed.
     */
    public void setOnclose(String onclose) {
        state.put(PropertyKeys.onclose, onclose);
    }

}
