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
package org.omnifaces.component.input;

import static java.lang.String.format;
import static org.omnifaces.util.Beans.fireEvent;
import static org.omnifaces.util.ComponentsLocal.convertToString;
import static org.omnifaces.util.FacesLocal.getHashParameters;
import static org.omnifaces.util.FacesLocal.getHashQueryString;
import static org.omnifaces.util.FacesLocal.getRequestParameter;
import static org.omnifaces.util.Servlets.toParameterMap;
import static org.omnifaces.util.Utils.coalesce;

import java.util.Objects;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.context.FacesContext;

import org.omnifaces.event.HashChangeEvent;
import org.omnifaces.util.Faces;

/**
 * <p>
 * The <code>&lt;o:hashParam&gt;</code> is a component that extends the standard <code>&lt;f:viewParam&gt;</code>
 * with support for setting hash query parameter values in bean and automatically reflecting updated model values in
 * hash query string.
 * <p>
 * The "hash query string" is the part in URL after the <code>#</code> which could be formatted in the same format
 * as a regular request query string (the part in URL after the <code>?</code>). An example:
 * <pre>
 * https://example.com/page.xhtml#foo=baz&amp;bar=kaz
 * </pre>
 * <p>
 * This specific part of the URL (also called hash fragment identifier) is by default not sent to the server. This
 * component will on page load and on every <code>window.onhashchange</code> event send it anyway so that the Faces model
 * gets updated, and on every Faces ajax request update the hash query string in client side when the Faces model value has
 * changed.
 *
 * <h2>Usage</h2>
 * <p>
 * It's very similar to the <code>&lt;o:viewParam&gt;</code>.
 * <pre>
 * &lt;f:metadata&gt;
 *     &lt;o:hashParam name="foo" value="#{bean.foo}" /&gt;
 *     &lt;o:hashParam name="bar" value="#{bean.bar}" /&gt;
 * &lt;/f:metadata&gt;
 * </pre>
 * <p>
 * You can use the <code>render</code> attribute to declare which components should be updated when a hash parameter
 * value is present.
 * <pre>
 * &lt;f:metadata&gt;
 *     &lt;o:hashParam name="foo" value="#{bean.foo}" render="fooResult" /&gt;
 *     &lt;o:hashParam name="bar" value="#{bean.bar}" /&gt;
 * &lt;/f:metadata&gt;
 * ...
 * &lt;h:body&gt;
 *     ...
 *     &lt;h:panelGroup id="fooResult"&gt;
 *         ...
 *     &lt;/h:panelGroup&gt;
 *     ...
 * &lt;/h:body&gt;
 * </pre>
 * <p>
 * In case you need to invoke a bean method before rendering, e.g. to preload the rendered contents based on new hash
 * param values, then you can observe the {@link HashChangeEvent}. See the "Events" section for an usage example.
 * <p>
 * You can use the <code>default</code> attribute to declare a non-null value which should be interpreted as the default
 * value. In other words, when the current model value matches the default value, then the hash parameter will be
 * removed.
 * <pre>
 * &lt;f:metadata&gt;
 *     &lt;o:hashParam name="foo" value="#{bean.foo}" /&gt;
 *     &lt;o:hashParam name="bar" value="#{bean.bar}" default="kaz" /&gt;
 * &lt;/f:metadata&gt;
 * </pre>
 * <p>
 * When <code>#{bean.foo}</code> is <code>"baz"</code> and <code>#{bean.bar}</code> is <code>"kaz"</code> or empty,
 * then the reflected hash query string will become <code>https://example.com/page.xhtml#foo=baz</code>.
 * If <code>#{bean.bar}</code> is any other value, then it will appear in the hash query string.
 * <p>
 * Note that as it extends from the standard <code>&lt;f:viewParam&gt;</code>, its built-in conversion and validation
 * functionality is also supported on this component.
 *
 * <h2>Events</h2>
 * <p>
 * When the hash query string is changed by the client side, e.g. by following a <code>#foo=baz&amp;bar=kaz</code> link,
 * or by manually manipulating the URL, then a CDI {@link HashChangeEvent} will be fired which can be observed in any
 * CDI managed bean as below:
 * <pre>
 * public void onHashChange(&#64;Observes HashChangeEvent event) {
 *     String oldHashString = event.getOldValue();
 *     String newHashString = event.getNewValue();
 *     // ...
 * }
 * </pre>
 * <p>
 * This is useful in case you want to preload the model for whatever is rendered by
 * <code>&lt;o:hashParam render&gt;</code>.
 *
 * @author Bauke Scholtz
 * @since 3.2
 * @see OnloadParam
 * @see HashChangeEvent
 * @see Faces#getHashParameters()
 * @see Faces#getHashParameterMap()
 * @see Faces#getHashQueryString()
 */
@FacesComponent(HashParam.COMPONENT_TYPE)
public class HashParam extends OnloadParam {

    // Public constants -----------------------------------------------------------------------------------------------

    /** The component type, which is {@value org.omnifaces.component.input.HashParam#COMPONENT_TYPE}. */
    public static final String COMPONENT_TYPE = "org.omnifaces.component.input.HashParam";

    /** The omnifaces event value, which is {@value org.omnifaces.component.input.HashParam#EVENT_VALUE}. */
    public static final String EVENT_VALUE = "setHashParamValues";

    // Private constants ----------------------------------------------------------------------------------------------

    private static final String SCRIPT_INIT = "OmniFaces.HashParam.init('%s')";
    private static final String SCRIPT_UPDATE = "OmniFaces.HashParam.update('%s', '%s')";

    private enum PropertyKeys {
        DEFAULT;
        @Override public String toString() { return name().toLowerCase(); }
    }

    // Init -----------------------------------------------------------------------------------------------------------

    @Override
    protected String getInitScript(FacesContext context) {
        return format(SCRIPT_INIT, getClientId(context));
    }


    @Override
    protected String getUpdateScript(FacesContext context) {
        return format(SCRIPT_UPDATE, getName(), getRenderedValue(context));
    }

    // Actions --------------------------------------------------------------------------------------------------------

    @Override
    protected String getEventValue(FacesContext context) {
        return EVENT_VALUE;
    }

    @Override
    protected void decodeAll(FacesContext context) {
        var oldHashQueryString = getHashQueryString(context);
        var hashParams = toParameterMap(getRequestParameter(context, "hash"));

        for (HashParam hashParam : getHashParameters(context)) {
            var values = hashParams.get(hashParam.getName());
            hashParam.decodeImmediately(context, values != null ? values.get(0) : "");
        }

        var newHashQueryString = getHashQueryString(context);

        if (!Objects.equals(oldHashQueryString, newHashQueryString)) {
            fireEvent(new HashChangeEvent(context, oldHashQueryString, newHashQueryString));
        }
    }

    /**
     * Convert the value to string using any converter and ensure that an empty string is returned when the component
     * is invalid or the resulting string is null or represents the default value.
     * @param context The involved faces context.
     * @return The rendered value.
     */
    public String getRenderedValue(FacesContext context) {
        if (!isValid()) {
            return "";
        }

        var value = getValue();

        if (Objects.equals(value, getDefault())) {
            value = null;
        }

        return coalesce(convertToString(context, this, value), "");
    }

    // Attribute getters/setters --------------------------------------------------------------------------------------

    /**
     * Returns the default value in case the actual hash parameter is <code>null</code> or empty.
     * @return The default value in case the actual hash parameter is <code>null</code> or empty.
     */
    public String getDefault() {
        return state.get(PropertyKeys.DEFAULT);
    }

    /**
     * Sets the default value in case the actual hash parameter is <code>null</code> or empty.
     * @param defaultValue The default value in case the actual hash parameter is <code>null</code> or empty.
     */
    public void setDefault(String defaultValue) {
        state.put(PropertyKeys.DEFAULT, defaultValue);
    }

    // Helpers --------------------------------------------------------------------------------------------------------

    /**
     * Returns <code>true</code> if the current request is triggered by a hash param request.
     * I.e. if it is initiated by <code>OmniFaces.HashParam.setHashParamValues()</code> script which runs on page load
     * when the <code>window.location.hash</code> is present, and on every <code>window.onhashchange</code> event.
     * @param context The involved faces context.
     * @return <code>true</code> if the current request is triggered by a hash param request.
     */
    public static boolean isHashParamRequest(FacesContext context) {
        return isOnloadParamRequest(context, EVENT_VALUE);
    }

}
