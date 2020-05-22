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

import static javax.faces.application.ResourceHandler.JSF_SCRIPT_LIBRARY_NAME;
import static javax.faces.application.ResourceHandler.JSF_SCRIPT_RESOURCE_NAME;
import static javax.faces.event.PhaseId.RENDER_RESPONSE;
import static org.omnifaces.config.OmniFaces.OMNIFACES_EVENT_PARAM_NAME;
import static org.omnifaces.config.OmniFaces.OMNIFACES_LIBRARY_NAME;
import static org.omnifaces.config.OmniFaces.OMNIFACES_SCRIPT_NAME;
import static org.omnifaces.util.Ajax.isExecuted;
import static org.omnifaces.util.Ajax.update;
import static org.omnifaces.util.Components.addFormIfNecessary;
import static org.omnifaces.util.Components.addScript;
import static org.omnifaces.util.Components.addScriptResource;
import static org.omnifaces.util.Events.subscribeToRequestBeforePhase;
import static org.omnifaces.util.FacesLocal.getRequestMap;
import static org.omnifaces.util.FacesLocal.getRequestParameter;
import static org.omnifaces.util.FacesLocal.isAjaxRequestWithPartialRendering;

import javax.faces.component.UIViewParameter;
import javax.faces.context.FacesContext;

import org.omnifaces.config.OmniFaces;
import org.omnifaces.util.State;

/**
 * Base class of {@link HashParam} and {@link ScriptParam}.
 *
 * @since 3.6
 */
public abstract class OnloadParam extends UIViewParameter {

	// Private constants ----------------------------------------------------------------------------------------------

	private enum PropertyKeys {
		RENDER;
		@Override public String toString() { return name().toLowerCase(); }
	}

	// Variables ------------------------------------------------------------------------------------------------------

	protected final State state = new State(getStateHelper());

	// Init -----------------------------------------------------------------------------------------------------------

	/**
	 * The constructor instructs JSF to register all scripts during the render response phase if necessary.
	 */
	public OnloadParam() {
		subscribeToRequestBeforePhase(RENDER_RESPONSE, this::registerScriptsIfNecessary);
	}

	private void registerScriptsIfNecessary() {
		FacesContext context = getFacesContext();

		if (!isOnloadParamRequest(context)) {
			addFormIfNecessary(); // Required by jsf.ajax.request.

			// This is supposed to be declared via @ResourceDependency. But this bugs in Mojarra with NPE on
			// ViewMetadata#createMetadataView because UIViewRoot is null at the moment the f:metadata is processed.
			addScriptResource(JSF_SCRIPT_LIBRARY_NAME, JSF_SCRIPT_RESOURCE_NAME); // Required for jsf.ajax.request.
			addScriptResource(OMNIFACES_LIBRARY_NAME, OMNIFACES_SCRIPT_NAME);

			if (!isAjaxRequestWithPartialRendering(context)) {
				if (getRequestMap(context).put(getClass().getName(), Boolean.TRUE) == null) { // Just init only once for first encountered OnloadParam.
					String initScript = getInitScript(context);

					if (initScript != null) {
						addScript(initScript);
					}
				}
			}
			else {
				String updateScript = getUpdateScript(context);

				if (updateScript != null) {
					addScript(updateScript);
				}
			}
		}
	}

	/**
	 * Returns script which should be executed upon initialization of a new view.
	 * @param context The involved faces context.
	 * @return Script which should be executed upon initialization of a new view.
	 */
	protected String getInitScript(FacesContext context) {
		return null;
	}

	/**
	 * Returns script which should be exeucted upon ajax update of the current view.
	 * @param context The involved faces context.
	 * @return Script which should be exeucted upon ajax update of the current view.
	 */
	protected String getUpdateScript(FacesContext context) {
		return null;
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the value of the {@link OmniFaces#OMNIFACES_EVENT_PARAM_NAME} associated with the current component.
	 * @param context The involved faces context.
	 * @return The value of the {@link OmniFaces#OMNIFACES_EVENT_PARAM_NAME} associated with the current component.
	 */
	protected abstract String getEventValue(FacesContext context);

	/**
	 * Returns <code>true</code> if the current request was invoked by the current {@link OnloadParam} component.
	 * @param context The involved faces context.
	 * @return <code>true</code> if the current request was invoked by the current {@link OnloadParam} component.
	 */
	protected boolean isOnloadParamRequest(FacesContext context)
	{
		return isOnloadParamRequest(context, getEventValue(context));
	}

	/**
	 * If this is invoked during an OnloadParam postback, then decode as if immediate=true to save lifecycle overhead.
	 */
	@Override
	public void processDecodes(FacesContext context) {
		if (isOnloadParamRequest(context) && isExecuted(getClientId(context))) {
			decodeAll(context);
			context.renderResponse();
		}
	}

	/**
	 * Decode all relevant {@link OnloadParam} components at once.
	 * @param context The involved faces context.
	 */
	protected abstract void decodeAll(FacesContext context);

	/**
	 * This basically acts as if immediate=true is set to save lifecycle overhead.
	 * @param context The involved faces context.
	 * @param submittedValue The submitted value.
	 */
	protected void decodeImmediately(FacesContext context, String submittedValue) {
		setSubmittedValue(submittedValue);
		validate(context);

		if (isValid()) {
			updateModel(context);
		}
		else {
			setValid(true); // Don't leave OnloadParam in an invalid state for next postback as it would block processing of regular forms.
		}

		String render = getRender();

		if (render != null) {
			update(render.split("\\s+"));
		}
	}

	/**
	 * This override which does effectively nothing prevents JSF from performing validation during non-onloadparam postbacks.
	 */
	@Override
	public void processValidators(FacesContext context) {
		// NOOP.
	}

	/**
	 * This override which does effectively nothing prevents JSF from performing update during non-onloadparam postbacks.
	 */
	@Override
	public void processUpdates(FacesContext context) {
		// NOOP.
	}

	// Attribute getters/setters --------------------------------------------------------------------------------------

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
	 * Returns <code>true</code> if the current request is triggered by an onload param request of the given onload param event.
	 * @param context The involved faces context.
	 * @param onloadEvent The onload param event.
	 * @return <code>true</code> if the current request is triggered by an onload param request of the given onload param event.
	 */
	protected static boolean isOnloadParamRequest(FacesContext context, String onloadEvent)
	{
		return context.isPostback() && onloadEvent.equals(getRequestParameter(context, OMNIFACES_EVENT_PARAM_NAME));
	}

}
