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
import static org.omnifaces.util.Faces.getViewRoot;

import javax.faces.application.Application;
import javax.faces.component.UIViewRoot;
import javax.faces.event.AbortProcessingException;
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
 *    {@literal @}Override
 *    public void invoke() {
 *        // ...
 *    }
 * });
 * </pre>
 * <pre>
 * // Add a callback to the current request which should run during before phase of the render response on current request.
 * Events.subscribeToRequestBeforePhase(PhaseId.RENDER_RESPONSE, new Callback.Void() {
 *     {@literal @}Override
 *     public void invoke() {
 *         // ...
 *     }
 * });
 * </pre>
 * <pre>
 * // Add a callback to the current view which should run during the pre render view event.
 * Events.subscribeToViewEvent(PreRenderViewEvent.class, new Callback.Void() {
 *     {@literal @}Override
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
 */
public final class Events {

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
	public static void subscribeToApplicationEvent(Class<? extends SystemEvent> type, Callback.Void callback) {
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
	public static void subscribeToApplicationEvent(Class<? extends SystemEvent> type, Callback.WithArgument<SystemEvent> callback) {
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
	public static void subscribeToViewEvent(Class<? extends SystemEvent> type, Callback.Void callback) {
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
	public static void subscribeToViewEvent(Class<? extends SystemEvent> type, Callback.WithArgument<SystemEvent> callback) {
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

	// Helpers --------------------------------------------------------------------------------------------------------

	private static <A> Callback.WithArgument<A> wrap(final Callback.Void callback) {
		return new Callback.WithArgument<A>() {

			@Override
			public void invoke(A argument) {
				callback.invoke();
			}
		};
	}

	private static SystemEventListener createSystemEventListener(final Callback.WithArgument<SystemEvent> callback) {
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