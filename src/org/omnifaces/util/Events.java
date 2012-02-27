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

import org.omnifaces.event.DefaultPhaseListener;

/**
 * Collection of utility methods for the JSF API with respect to working with Faces events.
 * 
 * @author Arjan Tijms
 */
public class Events {
	
	/**
	 * Adds a phase listener to the view root of the current faces context.
	 * @param newPhaseListener The phase listener to be added to the view root of the current faces context.
	 */
	public static void addPhaseListener(PhaseListener newPhaseListener) {
		Faces.getViewRoot().addPhaseListener(newPhaseListener);
	}

	/**
	 * Adds a phase listener that executes the given SAM (Single Abstract Method) before the given phase ID.
	 * @param phaseId The phase ID to execute the given SAM before.
	 * @param runnable The SAM to be executed before the given phase ID.
	 */
	public static void addBeforePhaseListener(PhaseId phaseId, final Runnable runnable) {
		addPhaseListener(new DefaultPhaseListener(phaseId) {

			private static final long serialVersionUID = -7055586428104563512L;

			@Override
			public void beforePhase(PhaseEvent event) {
				runnable.run();
			}
		});
	}

	/**
	 * Adds a phase listener that executes the given SAM (Single Abstract Method) after the given phase.
	 * @param phaseId The phase ID to execute the given SAM after.
	 * @param runnable The SAM to be executed after the given phase ID.
	 */
	public static void addAfterPhaseListener(PhaseId phaseId, final Runnable runnable) {
		addPhaseListener(new DefaultPhaseListener(phaseId) {

			private static final long serialVersionUID = 8859817625884748405L;

			@Override
			public void afterPhase(PhaseEvent event) {
				runnable.run();
			}
		});
	}
	

}
