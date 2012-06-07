/*
 * Copyright 2012 OmniFaces.
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
package org.omnifaces.eventlistener;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.component.UIInput;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PostValidateEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;

import org.omnifaces.event.PostInvokeActionEvent;
import org.omnifaces.event.PreInvokeActionEvent;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Utils;

/**
 * This phase listener takes care that the {@link PreInvokeActionEvent} and {@link PostInvokeActionEvent} events are
 * properly been published.
 *
 * @author Bauke Scholtz
 */
public class InvokeActionEventListener extends DefaultPhaseListener implements SystemEventListener {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = -7324254442944700095L;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * This constructor instructs the {@link DefaultPhaseListener} to hook on {@link PhaseId#INVOKE_APPLICATION} and
	 * subscribes this instance as a {@link SystemEventListener} to the {@link PostValidateEvent} event. This allows
	 * collecting the components eligible for {@link PreInvokeActionEvent} or {@link PostInvokeActionEvent} inside the
	 * {@link #processEvent(SystemEvent)} method.
	 */
	public InvokeActionEventListener() {
		super(PhaseId.INVOKE_APPLICATION);
		Faces.getApplication().subscribeToEvent(PostValidateEvent.class, this);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns <code>true</code> only when the given source is an instance of {@link UIViewRoot}, {@link UIForm},
	 * {@link UIInput} or {@link UICommand}.
	 */
	@Override
	public boolean isListenerForSource(Object source) {
		return (source instanceof UIViewRoot
			|| source instanceof UIForm
			|| source instanceof UIInput
			|| source instanceof UICommand);
	}

	/**
	 * If the validation has not failed for the current faces context, then check if the {@link UIComponent} which
	 * passed the {@link #isListenerForSource(Object)} check has any listeners for the {@link PreInvokeActionEvent}
	 * and/or {@link PostInvokeActionEvent} events and then add them to a set in the current faces context.
	 */
	@Override
	public void processEvent(SystemEvent event) throws AbortProcessingException {
		FacesContext context = FacesContext.getCurrentInstance();

		if (!context.isValidationFailed()) {
			UIComponent component = (UIComponent) event.getSource();
			checkAndAddComponentWithListeners(context, component, PreInvokeActionEvent.class);
			checkAndAddComponentWithListeners(context, component, PostInvokeActionEvent.class);
		}
	}

	/**
	 * Publish the {@link PreInvokeActionEvent} event on the components which are been collected in
	 * {@link #processEvent(SystemEvent)}.
	 */
	@Override
	public void beforePhase(PhaseEvent event) {
		publishEvent(event.getFacesContext(), PreInvokeActionEvent.class);
	}

	/**
	 * Publish the {@link PostInvokeActionEvent} event on the components which are been collected in
	 * {@link #processEvent(SystemEvent)}.
	 */
	@Override
	public void afterPhase(PhaseEvent event) {
		publishEvent(event.getFacesContext(), PostInvokeActionEvent.class);
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * If {@link UIComponent#getListenersForEventClass(Class)} returns a non-<code>null</code> and non-empty collection,
	 * then add the component to the set of components associated with the given event type.
	 * @param context The involved faces context.
	 * @param component The component to be checked.
	 * @param type The event type.
	 */
	@SuppressWarnings("unchecked") // For the cast on Set<UIComponent>.
	private static <T extends SystemEvent> void checkAndAddComponentWithListeners(
		FacesContext context, UIComponent component, Class<T> type)
	{
		if (!Utils.isEmpty(component.getListenersForEventClass(type))) {

			Set<UIComponent> components = (Set<UIComponent>) context.getAttributes().get(type);

			if (components == null) {
				components = new LinkedHashSet<UIComponent>();
				context.getAttributes().put(type, components);
			}

			components.add(component);
		}
	}

	/**
	 * Obtain the set of components associated with the given event type and publish the event on each of them.
	 * @param context The involved faces context.
	 * @param type The event type.
	 */
	@SuppressWarnings("unchecked") // For the cast on Set<UIComponent>.
	private static <T extends SystemEvent> void publishEvent(FacesContext context, Class<T> type) {
		if (context.getPartialViewContext().isAjaxRequest()) {
			// Event listeners on UIViewRoot should always be executed. However, PostValidateEvent of UIViewRoot isn't
			// published during a non-@all ajax request, so it might not have been processed by processEvent() at all.
			// So, just to be sure, we need to force a check for listeners on UIViewRoot.
			checkAndAddComponentWithListeners(context, context.getViewRoot(), type);
		}

		Set<UIComponent> components = (Set<UIComponent>) context.getAttributes().get(type);

		if (components != null) {
			for (UIComponent component : components) {
				context.getApplication().publishEvent(context, type, component);
			}
		}
	}

}