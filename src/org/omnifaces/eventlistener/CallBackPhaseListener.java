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

import static javax.faces.event.PhaseId.ANY_PHASE;

import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.omnifaces.util.Events;
import org.omnifaces.util.Faces;

/**
 * This phase listener picks up phase listener instances from the request scope and calls them back for each matching phase.
 * <p>
 * This differs in a few subtle ways from {@link Events#addPhaseListener(PhaseListener)}. Namely, the phase listener registered here will be called
 * via the global phase listener, which executes slightly earlier for its before phase and slightly later for its after phase as compared to phase
 * listeners attached to the view root.
 * <p>
 * Additionally, a phase listener registered via this method will not become part of the view state, but will execute only once. Phase listeners
 * attached to the view root will come back after each postback and have to be remove manually (in Mojarra this can be difficult due to the fact
 * iterators over listeners are kept 'open' during each phase).
 * <p>
 * See {@link Events#setCallBackPhaseListener(PhaseListener)}
 * 
 * @author Arjan Tijms
 * @since 1.2
 * 
 */
public class CallBackPhaseListener implements PhaseListener {

	private static final long serialVersionUID = -4574664722715466481L;

	public static final String CALL_BACK_PHASE_LISTENER = "org.omnifaces.eventlistener.CALL_BACK_PHASE_LISTENER";

	public static void setCallBackPhaseListener(PhaseListener callBackPhaseListener) {
		Faces.setRequestAttribute(CALL_BACK_PHASE_LISTENER, callBackPhaseListener);
	}

	public static PhaseListener getCallBackPhaseListener() {
		return Faces.<PhaseListener> getRequestAttribute(CALL_BACK_PHASE_LISTENER); // (Explicit generic because of JDK 6 bug)
	}

	@Override
	public PhaseId getPhaseId() {
		return ANY_PHASE;
	}

	@Override
	public void beforePhase(final PhaseEvent event) {
		PhaseListener callBackPhaseListener = getCallBackPhaseListener();
		if (callBackPhaseListener != null && isPhaseMatch(event, callBackPhaseListener.getPhaseId())) {
			callBackPhaseListener.beforePhase(event);
		}
	}

	@Override
	public void afterPhase(PhaseEvent event) {
		PhaseListener callBackPhaseListener = getCallBackPhaseListener();
		if (callBackPhaseListener != null && isPhaseMatch(event, callBackPhaseListener.getPhaseId())) {
			callBackPhaseListener.afterPhase(event);
		}
	}

	private boolean isPhaseMatch(PhaseEvent event, PhaseId phaseId) {
		return ANY_PHASE.equals(phaseId) || event.getPhaseId().equals(phaseId);
	}

}
