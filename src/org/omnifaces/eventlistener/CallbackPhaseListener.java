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

import java.util.HashSet;
import java.util.Set;

import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.omnifaces.util.Events;
import org.omnifaces.util.Faces;

/**
 * This phase listener picks up phase listener instances from the request scope by <code>addCallbackXxx()</code> methods
 * of the {@link Events} utility class and calls them back for each matching phase.
 * <p>
 * This differs in a few subtle ways from {@link Events#addPhaseListener(PhaseListener)}. Namely, the phase listener
 * registered here will be called via the global phase listener, which executes slightly earlier for its before phase
 * and slightly later for its after phase as compared to phase listeners attached to the view root.
 * <p>
 * Additionally, a phase listener registered via this method will not become part of the view state, but will execute
 * only once. Phase listeners attached to the view root will come back after each postback and have to be remove
 * manually (in Mojarra this can be difficult due to the fact iterators over listeners are kept 'open' during each
 * phase).
 *
 * @author Arjan Tijms
 * @author Bauke Scholtz
 * @since 1.2
 * @see Events#addCallbackPhaseListener(PhaseListener)
 * @see Events#addCallbackBeforePhaseListener(PhaseId, org.omnifaces.util.Callback.Void)
 * @see Events#addCallbackAfterPhaseListener(PhaseId, org.omnifaces.util.Callback.Void)
 */
public class CallbackPhaseListener implements PhaseListener {

	// Constants ------------------------------------------------------------------------------------------------------

	/** @deprecated This is not used anymore. */ @Deprecated
	public static final String CALLBACK_PHASE_LISTENER = "org.omnifaces.eventlistener.CALLBACK_PHASE_LISTENER";

	private static final long serialVersionUID = -4574664722715466481L;

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public PhaseId getPhaseId() {
		return ANY_PHASE;
	}

	@Override
	public void beforePhase(final PhaseEvent event) {
		Set<PhaseListener> phaseListeners = getCallbackPhaseListeners(false);

		if (phaseListeners == null) {
			return;
		}

		for (PhaseListener phaseListener : phaseListeners) {
			if (isPhaseMatch(event, phaseListener.getPhaseId())) {
				phaseListener.beforePhase(event);
			}
		}
	}

	@Override
	public void afterPhase(PhaseEvent event) {
		Set<PhaseListener> phaseListeners = getCallbackPhaseListeners(false);

		if (phaseListeners == null) {
			return;
		}

		for (PhaseListener phaseListener : phaseListeners) {
			if (isPhaseMatch(event, phaseListener.getPhaseId())) {
				phaseListener.afterPhase(event);
			}
		}
	}

	// Utility --------------------------------------------------------------------------------------------------------

	/**
	 * @see Events#addCallbackPhaseListener(PhaseListener)
	 */
	public static void add(PhaseListener phaseListener) {
		getCallbackPhaseListeners(true).add(phaseListener);
	}

	/**
	 * @see Events#removeCallbackPhaseListener(PhaseListener)
	 */
	public static boolean remove(PhaseListener phaseListener) {
		Set<PhaseListener> phaseListeners = getCallbackPhaseListeners(false);
		return phaseListeners == null ? false : phaseListeners.remove(phaseListener);
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	private static Set<PhaseListener> getCallbackPhaseListeners(boolean create) {
		Set<PhaseListener> set = Faces.getRequestAttribute(CallbackPhaseListener.class.getName());

		if (set == null && create) {
			set = new HashSet<PhaseListener>(1);
			Faces.setRequestAttribute(CallbackPhaseListener.class.getName(), set);
		}

		return set;
	}

	private static boolean isPhaseMatch(PhaseEvent event, PhaseId phaseId) {
		return ANY_PHASE.equals(phaseId) || event.getPhaseId().equals(phaseId);
	}

}