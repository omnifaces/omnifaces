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

import static org.omnifaces.util.FacesLocal.getViewAttribute;

import java.io.Serializable;
import java.util.HashMap;
import java.util.regex.Pattern;

import jakarta.el.ValueExpression;
import jakarta.faces.context.FacesContext;

import org.omnifaces.component.script.ScriptFamily;
import org.omnifaces.util.State;
import org.omnifaces.vdl.FacesAttribute;

/**
 * <p>
 * Base class for channel-based components ({@link Socket}, {@link Sse} and {@link Notification}) that share the
 * channel and user attributes with their validation.
 *
 * @author Bauke Scholtz
 * @see Socket
 * @see Sse
 * @see Notification
 * @since 5.2
 */
abstract class ChannelComponent extends ScriptFamily {

    // Constants ------------------------------------------------------------------------------------------------------

    static final Pattern PATTERN_CHANNEL = Pattern.compile("[\\w.-]+");

    private enum PropertyKeys {
        // Cannot be uppercased. They have to exactly match the attribute names.
        channel, user;
    }

    // Variables ------------------------------------------------------------------------------------------------------

    final State state = new State(getStateHelper());

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * An override which checks if this isn't been invoked on <code>channel</code> attribute, and if the
     * <code>user</code> attribute is <code>Serializable</code>. Finally it delegates to the super method.
     * @throws IllegalArgumentException When this value expression is been set on <code>channel</code> attribute,
     * or when the <code>user</code> attribute is not <code>Serializable</code>.
     */
    @Override
    public void setValueExpression(String name, ValueExpression binding) {
        if (PropertyKeys.channel.toString().equals(name)) {
            throw new IllegalArgumentException("%s 'channel' attribute may not contain an EL expression.".formatted(getTagName()));
        }

        if (PropertyKeys.user.toString().equals(name)) {
            var user = binding.getValue(getFacesContext().getELContext());

            if (user != null && !(user instanceof Serializable)) {
                throw new IllegalArgumentException("%s 'user' attribute '%s' does not represent a valid user identifier."
                        + " It must implement Serializable and preferably have low memory footprint."
                        + " Suggestion: use #{request.remoteUser} or #{someLoggedInUser.id}.".formatted(getTagName(), user));
            }
        }

        super.setValueExpression(name, binding);
    }

    /**
     * Validate the channel name and check that it is not already used by a different component type on the current view.
     * @param context The involved faces context.
     * @param channel The channel name to validate.
     * @throws IllegalArgumentException When the channel name is invalid or already used by a different component type.
     */
    void validateChannel(FacesContext context, String channel) {
        if (channel == null || !PATTERN_CHANNEL.matcher(channel).matches()) {
            throw new IllegalArgumentException("%s 'channel' attribute '%s' does not represent a valid channel name."
                    + " It is required and it may only contain alphanumeric characters, hyphens, underscores and periods.".formatted(getTagName(), channel));
        }

        var registeredChannels = getViewAttribute(context, ChannelComponent.class.getName(), HashMap::new);
        var existingChannel = registeredChannels.put(channel, getTagName());

        if (existingChannel != null && !existingChannel.equals(getTagName())) {
            throw new IllegalArgumentException("%s 'channel' attribute '%s' is already used by %s on the same view."
                    + " Channel names must be unique across o:socket, o:sse and o:notification.".formatted(getTagName(), channel, existingChannel));
        }
    }

    String getTagName() {
        return "o:" + getClass().getSimpleName().toLowerCase();
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

}
