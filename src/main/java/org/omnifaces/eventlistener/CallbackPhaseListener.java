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
import static org.omnifaces.util.Faces.getContext;
import static org.omnifaces.util.FacesLocal.getRequestAttribute;
import static org.omnifaces.util.FacesLocal.setRequestAttribute;

import java.util.HashSet;
import java.util.Set;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.omnifaces.util.Events;

/**
 * <p>
 * This phase listener picks up phase listener instances and phase event callbacks from the request scope subscribed via
 * <code>subscribeToRequestXxxPhase()</code> methods of the {@link Events} utility class and calls them back for each
 * matching phase.
 * <p>
 * This differs in a few subtle ways from <code>subscribeToViewXxxPhase()</code> methods of the {@link Events} class
 * which subscribes to the view scope. Namely, this phase listener will execute slightly earlier for its before phase
 * and slightly later for its after phase as compared to the view scoped ones. Additionally, the phase listener
 * instances and phase event callbacks registered via this phase listener will not become part of the view state, but
 * will execute only once during the current request instead of during every (postback) request on the same view.
 *
 * @author Arjan Tijms
 * @author Bauke Scholtz
 * @since 1.2
 * @see Events
 */
public class CallbackPhaseListener implements PhaseListener {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 3611407485061585042L;

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public PhaseId getPhaseId() {
		return ANY_PHASE;
	}

	@Override
	public void beforePhase(final PhaseEvent event) {
		Set<PhaseListener> phaseListeners = getCallbackPhaseListeners(event.getFacesContext(), false);

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
		Set<PhaseListener> phaseListeners = getCallbackPhaseListeners(event.getFacesContext(), false);

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
	 * @see Events#addRequestPhaseListener(PhaseListener)
	 */
	public static void add(PhaseListener phaseListener) {
		getCallbackPhaseListeners(getContext(), true).add(phaseListener);
	}

	/**
	 * @see Events#removeRequestPhaseListener(PhaseListener)
	 */
	public static boolean remove(PhaseListener phaseListener) {
		Set<PhaseListener> phaseListeners = getCallbackPhaseListeners(getContext(), false);
		return phaseListeners == null ? false : phaseListeners.remove(phaseListener);
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	private static Set<PhaseListener> getCallbackPhaseListeners(FacesContext context, boolean create) {
		Set<PhaseListener> set = getRequestAttribute(context, CallbackPhaseListener.class.getName());

		if (set == null && create) {
			set = new HashSet<>(1);
			setRequestAttribute(context, CallbackPhaseListener.class.getName(), set);
		}

		return set;
	}

	private static boolean isPhaseMatch(PhaseEvent event, PhaseId phaseId) {
		return ANY_PHASE.equals(phaseId) || event.getPhaseId().equals(phaseId);
	}

}