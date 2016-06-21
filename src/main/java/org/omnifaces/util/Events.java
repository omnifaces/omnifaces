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
package org.omnifaces.util;

import static org.omnifaces.util.Faces.getApplication;
import static org.omnifaces.util.Faces.getCurrentPhaseId;
import static org.omnifaces.util.Faces.getViewRoot;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ComponentSystemEventListener;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;

import org.omnifaces.eventlistener.CallbackPhaseListener;
import org.omnifaces.eventlistener.DefaultPhaseListener;
import org.omnifaces.eventlistener.DefaultViewEventListener;

/**
 * <p>
 * Collection of utility methods for the JSF API with respect to working with system and phase events.
 *
 * <h3>Usage</h3>
 * <p>
 * Some examples:
 * <pre>
 * // Add a callback to the current view which should run during every after phase of the render response on same view.
 * Events.subscribeToViewAfterPhase(PhaseId.RENDER_RESPONSE, new Callback.Void() {
 *    &#64;Override
 *    public void invoke() {
 *        // ...
 *    }
 * });
 * </pre>
 * <pre>
 * // Add a callback to the current request which should run during before phase of the render response on current request.
 * Events.subscribeToRequestBeforePhase(PhaseId.RENDER_RESPONSE, new Callback.Void() {
 *     &#64;Override
 *     public void invoke() {
 *         // ...
 *     }
 * });
 * </pre>
 * <pre>
 * // Add a callback to the current view which should run during the pre render view event.
 * Events.subscribeToViewEvent(PreRenderViewEvent.class, new Callback.SerializableVoid() {
 *     &#64;Override
 *     public void invoke() {
 *         // ...
 *     }
 * });
 * </pre>
 * <p>
 * Note that you can specify any phase ID or system event to your choice.
 *
 * @author Arjan Tijms
 * @author Bauke Scholtz
 * @see CallbackPhaseListener
 */
public final class Events {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String ERROR_UNSUBSCRIBE_TOO_LATE =
		"The render response phase is too late to unsubscribe the view event listener. Do it in an earlier phase.";

	// Constructors ---------------------------------------------------------------------------------------------------

	private Events() {
		// Hide constructor.
	}

	// Application scoped event listeners -----------------------------------------------------------------------------

	/**
	 * Subscribe the given system event listener to the current application that get invoked every time when the given
	 * system event type is published in the current application.
	 * @param type The system event type to be observed.
	 * @param listener The system event listener to be subscribed.
	 * @since 2.0
	 * @see Application#subscribeToEvent(Class, SystemEventListener)
	 */
	public static void subscribeToApplicationEvent(Class<? extends SystemEvent> type, SystemEventListener listener) {
		getApplication().subscribeToEvent(type, listener);
	}

	/**
	 * Subscribe the given callback to the current application that get invoked every time when the given
	 * system event type is published in the current application.
	 * @param type The system event type to be observed.
	 * @param callback The callback to be invoked.
	 * @since 2.0
	 * @see #subscribeToApplicationEvent(Class, SystemEventListener)
	 */
	public static void subscribeToApplicationEvent(Class<? extends SystemEvent> type, Callback.SerializableVoid callback) {
		subscribeToApplicationEvent(type, createSystemEventListener(Events.<SystemEvent>wrap(callback)));
	}

	/**
	 * Subscribe the given callback to the current application that get invoked every time when the given
	 * system event type is published in the current application.
	 * @param type The system event type to be observed.
	 * @param callback The callback to be invoked.
	 * @since 2.0
	 * @see #subscribeToApplicationEvent(Class, SystemEventListener)
	 */
	public static void subscribeToApplicationEvent(Class<? extends SystemEvent> type, Callback.SerializableWithArgument<SystemEvent> callback) {
		subscribeToApplicationEvent(type, createSystemEventListener(callback));
	}

	// View scoped event listeners ------------------------------------------------------------------------------------

	/**
	 * Subscribe the given system event listener to the current view that get invoked every time when the given
	 * system event type is published on the current view.
	 * @param type The system event type to be observed.
	 * @param listener The system event listener to be subscribed.
	 * @since 1.2
	 * @see UIViewRoot#subscribeToViewEvent(Class, SystemEventListener)
	 */
	public static void subscribeToViewEvent(Class<? extends SystemEvent> type, SystemEventListener listener) {
		getViewRoot().subscribeToViewEvent(type, listener);
	}

	/**
	 * Subscribe the given callback to the current view that get invoked every time when the given
	 * system event type is published on the current view.
	 * @param type The system event type to be observed.
	 * @param callback The callback to be invoked.
	 * @since 1.2
	 * @see #subscribeToViewEvent(Class, SystemEventListener)
	 */
	public static void subscribeToViewEvent(Class<? extends SystemEvent> type, Callback.SerializableVoid callback) {
		subscribeToViewEvent(type, createSystemEventListener(Events.<SystemEvent>wrap(callback)));
	}

	/**
	 * Subscribe the given callback to the current view that get invoked every time when the given
	 * system event type is published on the current view.
	 * @param type The system event type to be observed.
	 * @param callback The callback to be invoked.
	 * @since 2.0
	 * @see #subscribeToViewEvent(Class, SystemEventListener)
	 */
	public static void subscribeToViewEvent(Class<? extends SystemEvent> type, Callback.SerializableWithArgument<SystemEvent> callback) {
		subscribeToViewEvent(type, createSystemEventListener(callback));
	}

	// View scoped phase listeners ------------------------------------------------------------------------------------

	/**
	 * Adds the given phase listener to the current view.
	 * The difference with {@link #addRequestPhaseListener(PhaseListener)} is that the given phase listener is invoked
	 * during every (postback) request on the same view instead of only during the current request.
	 * @param listener The phase listener to be added to the current view.
	 * @since 2.0
	 * @see UIViewRoot#addPhaseListener(PhaseListener)
	 */
	public static void addViewPhaseListener(PhaseListener listener) {
		getViewRoot().addPhaseListener(listener);
	}

	/**
	 * Subscribe the given callback instance to the current view that get invoked every time before given phase ID.
	 * @param phaseId The phase ID to be observed.
	 * @param callback The callback to be invoked.
	 * @since 2.0
	 * @see #addViewPhaseListener(PhaseListener)
	 */
	public static void subscribeToViewBeforePhase(PhaseId phaseId, Callback.Void callback) {
		addViewPhaseListener(createBeforePhaseListener(phaseId, Events.<PhaseEvent>wrap(callback)));
	}

	/**
	 * Subscribe the given callback instance to the current view that get invoked every time before given phase ID.
	 * @param phaseId The phase ID to be observed.
	 * @param callback The callback to be invoked.
	 * @since 2.0
	 * @see #addViewPhaseListener(PhaseListener)
	 */
	public static void subscribeToViewBeforePhase(PhaseId phaseId, Callback.WithArgument<PhaseEvent> callback) {
		addViewPhaseListener(createBeforePhaseListener(phaseId, callback));
	}

	/**
	 * Subscribe the given callback instance to the current view that get invoked every time after given phase ID.
	 * @param phaseId The phase ID to be observed.
	 * @param callback The callback to be invoked.
	 * @since 2.0
	 * @see #addViewPhaseListener(PhaseListener)
	 */
	public static void subscribeToViewAfterPhase(PhaseId phaseId, Callback.Void callback) {
		addViewPhaseListener(createAfterPhaseListener(phaseId, Events.<PhaseEvent>wrap(callback)));
	}

	/**
	 * Subscribe the given callback instance to the current view that get invoked every time after given phase ID.
	 * @param phaseId The phase ID to be observed.
	 * @param callback The callback to be invoked.
	 * @since 2.0
	 * @see #addViewPhaseListener(PhaseListener)
	 */
	public static void subscribeToViewAfterPhase(PhaseId phaseId, Callback.WithArgument<PhaseEvent> callback) {
		addViewPhaseListener(createAfterPhaseListener(phaseId, callback));
	}

	// Request scoped component event listeners -----------------------------------------------------------------------

	/**
	 * Subscribe the given callback instance to the given component that get invoked only in the current request when
	 * the given component system event type is published on the given component. The difference with
	 * {@link UIComponent#subscribeToEvent(Class, ComponentSystemEventListener)} is that this listener is request
	 * scoped instead of view scoped as component system event listeners are by default saved in JSF state and thus
	 * inherently view scoped.
	 * @param component The component to subscribe the given callback instance to.
	 * @param type The system event type to be observed.
	 * @param callback The callback to be invoked.
	 * @since 2.1
	 * @see UIComponent#subscribeToEvent(Class, ComponentSystemEventListener)
	 * @see #unsubscribeFromComponentEvent(UIComponent, Class, ComponentSystemEventListener)
	 */
	public static void subscribeToRequestComponentEvent(final UIComponent component,
		final Class<? extends ComponentSystemEvent> type, final Callback.WithArgument<ComponentSystemEvent> callback)
	{
		component.subscribeToEvent(type, new ComponentSystemEventListener() {

			@Override
			public void processEvent(ComponentSystemEvent event) throws AbortProcessingException {
				unsubscribeFromComponentEvent(component, type, this); // Prevent it from being saved in JSF state.
				callback.invoke(event);
			}
		});
	}

	// Request scoped phase listeners ---------------------------------------------------------------------------------

	/**
	 * Adds the given phase listener to the current request.
	 * The difference with {@link #addViewPhaseListener(PhaseListener)} is that the given phase listener is invoked
	 * only during the current request instead of during every (postback) request on the same view.
	 * @param listener The phase listener to be added to the current request.
	 * @since 2.0
	 * @see CallbackPhaseListener
	 */
	public static void addRequestPhaseListener(PhaseListener listener) {
		CallbackPhaseListener.add(listener);
	}

	/**
	 * Subscribe the given callback instance to the current request that get invoked before given phase ID.
	 * @param phaseId The phase ID to be observed.
	 * @param callback The callback to be invoked.
	 * @since 2.0
	 * @see #addRequestPhaseListener(PhaseListener)
	 */
	public static void subscribeToRequestBeforePhase(PhaseId phaseId, Callback.Void callback) {
		addRequestPhaseListener(createBeforePhaseListener(phaseId, Events.<PhaseEvent>wrap(callback)));
	}

	/**
	 * Subscribe the given callback instance to the current request that get invoked before given phase ID.
	 * @param phaseId The phase ID to be observed.
	 * @param callback The callback to be invoked.
	 * @since 2.0
	 * @see #addRequestPhaseListener(PhaseListener)
	 */
	public static void subscribeToRequestBeforePhase(PhaseId phaseId, Callback.WithArgument<PhaseEvent> callback) {
		addRequestPhaseListener(createBeforePhaseListener(phaseId, callback));
	}

	/**
	 * Subscribe the given callback instance to the current request that get invoked after given phase ID.
	 * @param phaseId The phase ID to be observed.
	 * @param callback The callback to be invoked.
	 * @since 2.0
	 * @see #addRequestPhaseListener(PhaseListener)
	 */
	public static void subscribeToRequestAfterPhase(PhaseId phaseId, Callback.Void callback) {
		addRequestPhaseListener(createAfterPhaseListener(phaseId, Events.<PhaseEvent>wrap(callback)));
	}

	/**
	 * Subscribe the given callback instance to the current request that get invoked after given phase ID.
	 * @param phaseId The phase ID to be observed.
	 * @param callback The callback to be invoked.
	 * @since 2.0
	 * @see #addRequestPhaseListener(PhaseListener)
	 */
	public static void subscribeToRequestAfterPhase(PhaseId phaseId, Callback.WithArgument<PhaseEvent> callback) {
		addRequestPhaseListener(createAfterPhaseListener(phaseId, callback));
	}

	// Component scoped event listeners -------------------------------------------------------------------------------

	/**
	 * Unsubscribe the given event listener on the given event from the given component. Normally, you would use
	 * {@link UIComponent#unsubscribeFromEvent(Class, ComponentSystemEventListener)} for this, but this wouldn't work
	 * when executed inside {@link ComponentSystemEventListener#processEvent(javax.faces.event.ComponentSystemEvent)},
	 * as it would otherwise end up in a <code>ConcurrentModificationException</code> while JSF is iterating over all
	 * system event listeners. The trick is to perform the unsubscribe during the after phase of the current request
	 * phase {@link #subscribeToRequestAfterPhase(PhaseId, org.omnifaces.util.Callback.Void)}.
	 * @param component The component to unsubscribe the given event listener from.
	 * @param event The event associated with the given event listener.
	 * @param listener The event listener to be unsubscribed from the given component.
	 * @throws IllegalStateException When this method is invoked during render response phase, because it would be too
	 * late to remove it from the view state.
	 * @since 2.1
	 * @see #subscribeToRequestAfterPhase(PhaseId, org.omnifaces.util.Callback.Void)
	 * @see UIComponent#unsubscribeFromEvent(Class, ComponentSystemEventListener)
	 */
	public static void unsubscribeFromComponentEvent
		(final UIComponent component, final Class<? extends SystemEvent> event, final ComponentSystemEventListener listener)
	{
		PhaseId currentPhaseId = getCurrentPhaseId();

		if (currentPhaseId == PhaseId.RENDER_RESPONSE) {
			throw new IllegalStateException(ERROR_UNSUBSCRIBE_TOO_LATE);
		}

		subscribeToRequestAfterPhase(currentPhaseId, new Callback.Void() {

			@Override
			public void invoke() {
				component.unsubscribeFromEvent(event, listener);
			}
		});
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	private static <A> Callback.WithArgument<A> wrap(final Callback.Void callback) {
		return new Callback.WithArgument<A>() {

			@Override
			public void invoke(A argument) {
				callback.invoke();
			}
		};
	}

	private static <A> Callback.SerializableWithArgument<A> wrap(final Callback.SerializableVoid callback) {
		return new Callback.SerializableWithArgument<A>() {

			private static final long serialVersionUID = 1L;

			@Override
			public void invoke(A argument) {
				callback.invoke();
			}
		};
	}

	private static SystemEventListener createSystemEventListener(final Callback.SerializableWithArgument<SystemEvent> callback) {
		return new DefaultViewEventListener() {

			@Override
			public void processEvent(SystemEvent event) throws AbortProcessingException {
				callback.invoke(event);
			}
		};
	}

	private static PhaseListener createBeforePhaseListener(PhaseId phaseId, final Callback.WithArgument<PhaseEvent> callback) {
		return new DefaultPhaseListener(phaseId) {

			private static final long serialVersionUID = 1L;

			@Override
			public void beforePhase(PhaseEvent event) {
				callback.invoke(event);
			}
		};
	}

	private static PhaseListener createAfterPhaseListener(PhaseId phaseId, final Callback.WithArgument<PhaseEvent> callback) {
		return new DefaultPhaseListener(phaseId) {

			private static final long serialVersionUID = 1L;

			@Override
			public void afterPhase(PhaseEvent event) {
				callback.invoke(event);
			}
		};
	}

}