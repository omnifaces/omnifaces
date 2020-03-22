/*
 * Copyright 2020 OmniFaces
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

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static javax.faces.application.ResourceHandler.JSF_SCRIPT_LIBRARY_NAME;
import static javax.faces.application.ResourceHandler.JSF_SCRIPT_RESOURCE_NAME;
import static javax.faces.application.StateManager.IS_BUILDING_INITIAL_STATE;
import static javax.faces.event.PhaseId.RENDER_RESPONSE;
import static org.omnifaces.config.OmniFaces.OMNIFACES_EVENT_PARAM_NAME;
import static org.omnifaces.config.OmniFaces.OMNIFACES_LIBRARY_NAME;
import static org.omnifaces.config.OmniFaces.OMNIFACES_SCRIPT_NAME;
import static org.omnifaces.util.Ajax.oncomplete;
import static org.omnifaces.util.Ajax.update;
import static org.omnifaces.util.Beans.fireEvent;
import static org.omnifaces.util.Components.addScriptResourceToHead;
import static org.omnifaces.util.Components.addScriptToBody;
import static org.omnifaces.util.Events.subscribeToRequestBeforePhase;
import static org.omnifaces.util.Faces.getHashQueryString;
import static org.omnifaces.util.Faces.getRequestMap;
import static org.omnifaces.util.Faces.isAjaxRequestWithPartialRendering;
import static org.omnifaces.util.Faces.isPostback;
import static org.omnifaces.util.Faces.renderResponse;
import static org.omnifaces.util.FacesLocal.getHashParameters;
import static org.omnifaces.util.FacesLocal.getRequestParameter;
import static org.omnifaces.util.Servlets.toParameterMap;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.faces.component.FacesComponent;
import javax.faces.component.UIForm;
import javax.faces.component.UIViewParameter;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.omnifaces.event.HashChangeEvent;
import org.omnifaces.util.Ajax;
import org.omnifaces.util.Faces;
import org.omnifaces.util.State;

/**
 * <p>
 * The <code>&lt;o:hashParam&gt;</code> is a component that extends the standard <code>&lt;f:viewParam&gt;</code>
 * with support for setting hash query parameter values in bean and automatically reflecting updated model values in
 * hash query string.
 * <p>
 * The "hash query string" is the part in URL after the <code>#</code> which could be formatted in the same format
 * as a regular request query string (the part in URL after the <code>?</code>). An example:
 * <pre>
 * http://example.com/page.xhtml#foo=baz&amp;bar=kaz
 * </pre>
 * <p>
 * This specific part of the URL (also called hash fragment identifier) is by default not sent to the server. This
 * component will on page load and on every <code>window.onhashchange</code> event send it anyway so that the JSF model
 * gets updated, and on every JSF ajax request update the hash query string in client side when the JSF model value has
 * changed.
 *
 * <h3>Usage</h3>
 * <p>
 * It's very similar to the <code>&lt;o:viewParam&gt;</code>.
 * <pre>
 * &lt;f:metadata&gt;
 *     &lt;o:hashParam name="foo" value="#{bean.foo}" /&gt;
 *     &lt;o:hashParam name="bar" value="#{bean.bar}" /&gt;
 * &lt;/f:metadata&gt;
 * </pre>
 * <p>
 * This only requires that the JSF page has at least one {@link UIForm} component, such as <code>&lt;h:form&gt;</code>
 * or <code>&lt;o:form&gt;</code>, otherwise the <code>&lt;o:hashParam&gt;</code> won't be able to fire the ajax
 * request which sets the with hash query parameter values in bean. In such case an error will be printed to JS console
 * when the project stage is <code>Development</code>.
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
 * then the reflected hash query string will become <code>http://example.com/page.xhtml#foo=baz</code>.
 * If <code>#{bean.bar}</code> is any other value, then it will appear in the hash query string.
 *
 * <h3>Events</h3>
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
 * <code>&lt;o:hashParam rendered&gt;</code>.
 *
 * @author Bauke Scholtz
 * @since 3.2
 * @see Faces#getHashParameters()
 * @see Faces#getHashParameterMap()
 * @see Faces#getHashQueryString()
 * @see HashChangeEvent
 */
@FacesComponent(HashParam.COMPONENT_TYPE)
public class HashParam extends UIViewParameter {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The component type, which is {@value org.omnifaces.component.input.HashParam#COMPONENT_TYPE}. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.input.HashParam";

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String SCRIPT_INIT = "OmniFaces.HashParam.init('%s')";
	private static final String SCRIPT_UPDATE = "OmniFaces.HashParam.update('%s', '%s')";

	private enum PropertyKeys {
		DEFAULT, RENDER;
		@Override public String toString() { return name().toLowerCase(); }
	}

	// Variables ------------------------------------------------------------------------------------------------------

	private final State state = new State(getStateHelper());

	// Init -----------------------------------------------------------------------------------------------------------

	/**
	 * The constructor instructs JSF to register all scripts during the render response phase if necessary.
	 */
	public HashParam() {
		subscribeToRequestBeforePhase(RENDER_RESPONSE, this::registerScriptsIfNecessary);
	}

	private void registerScriptsIfNecessary() {
		if (TRUE.equals(getFacesContext().getAttributes().get(IS_BUILDING_INITIAL_STATE))) {
			// @ResourceDependency bugs in Mojarra with NPE on createMetadataView because UIViewRoot is null (this component is part of f:metadata).
			addScriptResourceToHead(JSF_SCRIPT_LIBRARY_NAME, JSF_SCRIPT_RESOURCE_NAME); // Required for jsf.ajax.request.
			addScriptResourceToHead(OMNIFACES_LIBRARY_NAME, OMNIFACES_SCRIPT_NAME); // Specifically hashparam.js.
		}

		if (!isPostback()) {
			if (getRequestMap().put(getClass().getName(), Boolean.TRUE) == null) {
				addScriptToBody(format(SCRIPT_INIT, getClientId())); // Just init only once for first encountered HashParam.
			}
		}
		else {
			if (isAjaxRequestWithPartialRendering() && !isHashParamRequest(getFacesContext())) {
				oncomplete(format(SCRIPT_UPDATE, getName(), getRenderedValue(getFacesContext()))); // Update hash string based on JSF model if necessary.
			}

			setValid(true); // Don't leave HashParam in an invalid state for next postback as it would block processing of regular forms.
		}
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * If this is invoked during a hashparam postback, then decode as if immediate=true to save lifecycle overhead.
	 */
	@Override
	public void processDecodes(FacesContext context) {
		if (isHashParamRequest(context) && Ajax.getContext().getExecuteIds().contains(getClientId(context))) {
			String oldHashQueryString = getHashQueryString();
			Map<String, List<String>> hashParams = toParameterMap(getRequestParameter(context, "hash"));

			for (HashParam hashParam : getHashParameters(context)) {
				List<String> values = hashParams.get(hashParam.getName());
				hashParam.decodeImmediately(context, values != null ? values.get(0) : "");
			}

			String newHashQueryString = getHashQueryString();

			if (!Objects.equals(oldHashQueryString, newHashQueryString)) {
				fireEvent(new HashChangeEvent(context, oldHashQueryString, newHashQueryString));
			}

			renderResponse();
		}
	}

	private void decodeImmediately(FacesContext context, String submittedValue) {
		setSubmittedValue(submittedValue);
		validate(context);

		if (isValid()) {
			super.updateModel(context);
		}

		String render = getRender();

		if (render != null) {
			update(render.split("\\s+"));
		}
	}

	/**
	 * This override which does effectively nothing prevents JSF from performing validation during non-hashparam postbacks.
	 */
	@Override
	public void processValidators(FacesContext context) {
		// NOOP.
	}

	/**
	 * This override which does effectively nothing prevents JSF from performing update during non-hashparam postbacks.
	 */
	@Override
	public void updateModel(FacesContext context) {
		// NOOP.
	}

	/**
	 * Convert the value to string using any converter and ensure that an empty string is returned when the component
	 * is invalid or the resulting string is null or represents the default value.
	 * @return The rendered value.
	 */
	@SuppressWarnings("unchecked")
	public String getRenderedValue(FacesContext context) {
		if (!isValid()) {
			return "";
		}

		Object value = getValue();
		Converter<Object> converter = getConverter();

		if (Objects.equals(value, getDefault())) {
			value = null;
		}
		else if (converter != null) {
			value = converter.getAsString(context, this, value);
		}

		return (value == null) ? "" : value.toString();
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

	/**
	 * Returns a space separated string of client IDs to update on ajax response.
	 * @return A space separated string of client IDs to update on ajax response.
	 */
	public String getRender() {
		return state.get(PropertyKeys.RENDER, "@none");
	}

	/**
	 * Sets a space separated string of client IDs to update on ajax response.
	 * @param render A space separated string of client IDs to update on ajax response.
	 */
	public void setRender(String render) {
		state.put(PropertyKeys.RENDER, render);
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
		return context.isPostback() && "setHashParamValues".equals(getRequestParameter(context, OMNIFACES_EVENT_PARAM_NAME));
	}

}
