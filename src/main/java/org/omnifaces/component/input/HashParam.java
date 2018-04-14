/*
 * Copyright 2018 OmniFaces
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
package org.omnifaces.component.input;

import static java.lang.String.format;
import static javax.faces.application.ResourceHandler.JSF_SCRIPT_LIBRARY_NAME;
import static javax.faces.application.ResourceHandler.JSF_SCRIPT_RESOURCE_NAME;
import static javax.faces.component.UIViewRoot.METADATA_FACET_NAME;
import static javax.faces.event.PhaseId.RENDER_RESPONSE;
import static org.omnifaces.config.OmniFaces.OMNIFACES_EVENT_PARAM_NAME;
import static org.omnifaces.config.OmniFaces.OMNIFACES_LIBRARY_NAME;
import static org.omnifaces.config.OmniFaces.OMNIFACES_SCRIPT_NAME;
import static org.omnifaces.util.Ajax.oncomplete;
import static org.omnifaces.util.Ajax.update;
import static org.omnifaces.util.Components.addScriptResourceToHead;
import static org.omnifaces.util.Components.addScriptToBody;
import static org.omnifaces.util.Components.findComponentsInChildren;
import static org.omnifaces.util.Events.subscribeToRequestBeforePhase;
import static org.omnifaces.util.Faces.getRequestMap;
import static org.omnifaces.util.Faces.getViewRoot;
import static org.omnifaces.util.Faces.isAjaxRequestWithPartialRendering;
import static org.omnifaces.util.Faces.isPostback;
import static org.omnifaces.util.Faces.renderResponse;
import static org.omnifaces.util.FacesLocal.getRequestParameter;
import static org.omnifaces.util.Servlets.toParameterMap;
import static org.omnifaces.util.Utils.encodeURL;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.faces.component.FacesComponent;
import javax.faces.component.UIViewParameter;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.omnifaces.util.State;

/**
 * <p>
 * The <code>&lt;o:hashParam&gt;</code> is a component that extends the standard <code>&lt;f:viewParam&gt;</code>
 * with support for setting hash parameter values in bean and reflecting changed bean values in hash parameter.
 *
 * <h3>Usage</h3>
 * <p>
 * In <code>&lt;f:metadata&gt;</code>:
 * <pre>
 * &lt;o:hashParam name="foo" value="#{bean.foo}" /&gt;
 * </pre>
 *
 * TODO: elaborate javadoc
 *
 * @author Bauke Scholtz
 * @since 3.2
 */
@FacesComponent(HashParam.COMPONENT_TYPE)
public class HashParam extends UIViewParameter {

	// Public constants -----------------------------------------------------------------------------------------------

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

	public HashParam() {
		subscribeToRequestBeforePhase(RENDER_RESPONSE, this::registerScriptsIfNecessary); // PreRenderViewEvent didn't work flawlessly with MyFaces? TODO: investigate
	}

	private void registerScriptsIfNecessary() {
		// @ResourceDependency bugs in Mojarra with NPE on createMetadataView because UIViewRoot is null.
		addScriptResourceToHead(JSF_SCRIPT_LIBRARY_NAME, JSF_SCRIPT_RESOURCE_NAME); // Required for jsf.ajax.request.
		addScriptResourceToHead(OMNIFACES_LIBRARY_NAME, OMNIFACES_SCRIPT_NAME); // Specifically hashparam.js.

		if (!isPostback()) {
			if (getRequestMap().put(getClass().getName(), Boolean.TRUE) == null) {
				addScriptToBody(format(SCRIPT_INIT, getClientId())); // Just init only once for first encountered HashParam.
			}
		}
		else if (isAjaxRequestWithPartialRendering()) {
			oncomplete(format(SCRIPT_UPDATE, getName(), getRenderedValue()));
		}
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * If this is invoked during a hashparam postback, then decode as if immediate=true to save lifecycle overhead.
	 */
	@Override
	public void processDecodes(FacesContext context) {
		if (isPostback() && isHashParamRequest(context) && getClientId(context).equals(getRequestParameter(context, "clientId"))) {
			String hashString = getRequestParameter(context, "hash");
			Map<String, List<String>> hashParams = toParameterMap(hashString);

			for (HashParam hashParam : getHashParams()) {
				List<String> values = hashParams.get(hashParam.getName());

				if (values != null) {
					decode(context, values.get(0));
				}
			}

			renderResponse();
		}
	}

	private void decode(FacesContext context, String value) {
		setSubmittedValue(value);
		validate(context);

		if (!context.isValidationFailed()) {
			super.updateModel(context);
			String render = getRender();

			if (render != null) {
				update(render.split("\\s+"));
			}
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

	@SuppressWarnings("unchecked")
	protected String getRenderedValue() {
		Object value = getValue();
		Converter<Object> converter = getConverter();

		if (Objects.equals(value, getDefault())) {
			value = null;
		}
		else if (converter != null) {
			value = converter.getAsString(getFacesContext(), this, value);
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
	 * @param context The involved faces context.
	 * @return <code>true</code> if the current request is triggered by a hash param request.
	 */
	public static boolean isHashParamRequest(FacesContext context) {
		return "setHashParamValues".equals(getRequestParameter(context, OMNIFACES_EVENT_PARAM_NAME));
	}

	/**
	 * Returns the current hash string based on all HashParam instances in metadata.
	 * @return The current hash string based on all HashParam instances in metadata.
	 */
	public static String getHashString() {
		StringBuilder hashString = new StringBuilder();

		for (HashParam param : getHashParams()) {
			if (isEmpty(param.getName())) {
				continue;
			}

			String value = param.getRenderedValue();

			if (value != null && !value.isEmpty()) {
				if (hashString.length() > 0) {
					hashString.append("&");
				}

				hashString.append(encodeURL(param.getName())).append("=").append(encodeURL(value.toString()));
			}
		}

		return hashString.toString();
	}

	private static List<HashParam> getHashParams() {
		return findComponentsInChildren(getViewRoot().getFacet(METADATA_FACET_NAME), HashParam.class); // TODO: filter rendered=false?
	}

}
