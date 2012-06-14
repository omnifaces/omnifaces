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

import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;

import org.omnifaces.eventlistener.DefaultPhaseListener;

/**
 * Collection of utility methods for the JSF API with respect to working with Faces events.
 *
 * @author Arjan Tijms
 */
public final class Events {

	// Constructors ---------------------------------------------------------------------------------------------------

	private Events() {
		// Hide constructor.
	}

	// PhaseListener --------------------------------------------------------------------------------------------------

	/**
	 * Adds a phase listener to the current view root of the current faces context.
	 * @param phaseListener The phase listener to be added to the current view root of the current faces context.
	 */
	public static void addPhaseListener(PhaseListener phaseListener) {
		Faces.getViewRoot().addPhaseListener(phaseListener);
	}

	/**
	 * Removes a phase listener from the current view root of the current faces context.
	 * @param phaseListener The phase listener to be removed from the current view root of the current faces context.
	 */
	public static void removePhaseListener(PhaseListener phaseListener) {
		Faces.getViewRoot().removePhaseListener(phaseListener);
	}

	/**
	 * Adds a phase listener to the current view that invokes the given callback everytime before given phase ID.
	 * @param phaseId The phase ID to invoke the given callback everytime before.
	 * @param callback The callback to be invoked everytime before the given phase ID of the current view.
	 */
	public static void addBeforePhaseListener(PhaseId phaseId, final Callback.Void callback) {
		addPhaseListener(new DefaultPhaseListener(phaseId) {

			private static final long serialVersionUID = -5078199683615308073L;

			@Override
			public void beforePhase(PhaseEvent event) {
				callback.invoke();
			}
		});
	}

	/**
	 * Adds a phase listener to the current view that invokes the given callback everytime after given phase.
	 * @param phaseId The phase ID to invoke the given callback everytime after.
	 * @param callback The callback to be invoked everytime after the given phase ID of the current view.
	 */
	public static void addAfterPhaseListener(PhaseId phaseId, final Callback.Void callback) {
		addPhaseListener(new DefaultPhaseListener(phaseId) {

			private static final long serialVersionUID = -7760218897262285339L;

			@Override
			public void afterPhase(PhaseEvent event) {
				callback.invoke();
			}
		});
	}

	/**
	 * Adds the given system event listener to the application that get invoked everytime when the given system event
	 * type is been published.
	 * @param type The system event type to listen on.
	 * @param listener The system event listener to be invoked.
	 * @since 1.1
	 */
	public static void subscribeToEvent(Class<? extends SystemEvent> type, SystemEventListener listener) {
		Faces.getApplication().subscribeToEvent(type, listener);
	}

}