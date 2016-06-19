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
package org.omnifaces.component.util;

import static java.lang.String.format;
import static org.omnifaces.component.util.ResolveComponent.PropertyKeys.name;
import static org.omnifaces.component.util.ResolveComponent.PropertyKeys.scope;
import static org.omnifaces.util.Components.findComponentRelatively;
import static org.omnifaces.util.Events.subscribeToViewEvent;
import static org.omnifaces.util.Faces.isPostback;
import static org.omnifaces.util.Faces.setRequestAttribute;
import static org.omnifaces.util.Utils.isEmpty;

import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponent;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.PostRestoreStateEvent;
import javax.faces.event.PreRenderViewEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;
import javax.faces.view.facelets.FaceletContext;

import org.omnifaces.el.ReadOnlyValueExpression;
import org.omnifaces.taghandler.ComponentExtraHandler;
import org.omnifaces.util.Callback.SerializableReturning;
import org.omnifaces.util.Components;
import org.omnifaces.util.State;

/**
 * <p>
 * The <code>&lt;o:resolveComponent&gt;</code> component is a utility component via which a component can be looked up
 * by its ID and a reference to it put in either the "facelet scope" (default) or the request scope.
 *
 * @since 2.0
 * @author Arjan Tijms
 * @see FaceletContextConsumer
 * @see ComponentExtraHandler
 * @see UtilFamily
 */
@FacesComponent(ResolveComponent.COMPONENT_TYPE)
public class ResolveComponent extends UtilFamily implements FaceletContextConsumer, SystemEventListener {

	public static final String COMPONENT_TYPE = "org.omnifaces.component.util.ResolveComponent";

	private static final String ERROR_COMPONENT_NOT_FOUND =
		"A component with ID '%s' as specified by the 'for' attribute of the ResolveComponent with Id '%s' could not be found.";

	private static final String ERROR_ILLEGAL_SCOPE =
		"o:resolveComponent 'scope' attribute only supports 'facelet' (default) or 'request'. Encountered an invalid value of '%s'.";

	public static final String DEFAULT_SCOPE = "facelet";

	private ReadOnlyValueExpression readOnlyValueExpression;

	private final State state = new State(getStateHelper());

	enum PropertyKeys {
		name, scope, /* for */
	}

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public void setFaceletContext(FaceletContext faceletContext) {
		if (getScope().equals("facelet")) {

			readOnlyValueExpression = new ReadOnlyValueExpression(UIComponent.class);

			faceletContext.getVariableMapper().setVariable(getName(), readOnlyValueExpression);
		}
	}

	public ResolveComponent() {
		if (!isPostback()) { // For an initial (GET) request, there's no restore
								// state event and we use pre-render view
			subscribeToViewEvent(PreRenderViewEvent.class, this);
		}
	}

	@Override
	public boolean isListenerForSource(Object source) {
		return true;
	}

	@Override
	public void processEvent(SystemEvent event) throws AbortProcessingException {
		doProcess();
	}

	@Override
	public void processEvent(ComponentSystemEvent event) throws AbortProcessingException {
		if (event instanceof PostRestoreStateEvent) { // For a postback we use the post-restore state event.
			doProcess();
		}
	}

	public void doProcess() {
		String forValue = getFor();

		if (!isEmpty(forValue)) {
			UIComponent component = findComponentRelatively(this, forValue);

			if (component == null) {
				component = findComponent(forValue);
			}

			if (component == null) {
				throw new IllegalArgumentException(format(ERROR_COMPONENT_NOT_FOUND, forValue, getId()));
			}

			String scope = getScope();

			switch (scope) { // TODO: refactor "scope" to a reusable enum, together with those of a.o. Cache.
				case "facelet":
					// Component will be resolved again dynamically when the value expression is evaluated.
					if (readOnlyValueExpression != null) {
						readOnlyValueExpression.setCallbackReturning(new ComponentClientIdResolver(component.getClientId()));
					}
					break;

				case "request":
					setRequestAttribute(getName(), component);
					break;

				default:
					throw new IllegalArgumentException(format(ERROR_ILLEGAL_SCOPE, scope));
			}
		}
	}

	public static class ComponentClientIdResolver implements SerializableReturning<Object> {

		private static final long serialVersionUID = 1L;

		private final String foundComponentId;
		private transient UIComponent foundComponent;

		public ComponentClientIdResolver(String foundComponentId) {
			this.foundComponentId = foundComponentId;
		}

		@Override
		public Object invoke() {
			if (foundComponent == null) {
				foundComponent = Components.findComponent(foundComponentId);
			}
			return foundComponent;
		}
	}

	// Attribute getters/setters --------------------------------------------------------------------------------------

	public String getName() {
		return state.get(name);
	}

	public void setName(String nameValue) {
		state.put(name, nameValue);
	}

	public String getFor() {
		return state.get("for");
	}

	public void setFor(String nameValue) {
		state.put("for", nameValue);
	}

	public String getScope() {
		return state.get(scope, DEFAULT_SCOPE);
	}

	public void setScope(String scopeValue) {
		state.put(scope, scopeValue);
	}

}