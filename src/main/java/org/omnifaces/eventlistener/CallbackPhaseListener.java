/*
 * Copyright OmniFaces
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
package org.omnifaces.eventlistener;

import static jakarta.faces.event.PhaseId.ANY_PHASE;
import static org.omnifaces.util.Faces.getContext;
import static org.omnifaces.util.FacesLocal.getRequestAttribute;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.faces.context.FacesContext;
import jakarta.faces.event.PhaseEvent;
import jakarta.faces.event.PhaseId;
import jakarta.faces.event.PhaseListener;

/**
 * <p>
 * This phase listener picks up phase listener instances and phase event callbacks from the request scope subscribed via
 * <code>subscribeToRequestXxxPhase()</code> methods of the {@link org.omnifaces.util.Events} utility class and calls
 * them back for each matching phase.
 * <p>
 * This differs in a few subtle ways from <code>subscribeToViewXxxPhase()</code> methods of the
 * {@link org.omnifaces.util.Events} class which subscribes to the view scope. Namely, this phase listener will execute
 * slightly earlier for its before phase and slightly later for its after phase as compared to the view scoped ones.
 * Additionally, the phase listener instances and phase event callbacks registered via this phase listener will not
 * become part of the view state, but will execute only once during the current request instead of during every
 * (postback) request on the same view.
 *
 * @author Arjan Tijms
 * @author Bauke Scholtz
 * @since 1.2
 * @see org.omnifaces.util.Events
 */
public class CallbackPhaseListener implements PhaseListener {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final long serialVersionUID = 1L;

    // Actions --------------------------------------------------------------------------------------------------------

    @Override
    public PhaseId getPhaseId() {
        return ANY_PHASE;
    }

    @Override
    public void beforePhase(PhaseEvent event) {
        for (PhaseListener phaseListener : getCallbackPhaseListenersForEvent(event)) {
            phaseListener.beforePhase(event);
        }
    }

    @Override
    public void afterPhase(PhaseEvent event) {
        for (PhaseListener phaseListener : getCallbackPhaseListenersForEvent(event)) {
            phaseListener.afterPhase(event);
        }
    }

    // Utility --------------------------------------------------------------------------------------------------------

    /**
     * Adds the given phase listener to the current request scope.
     * @param phaseListener The phase listener to be added to the current request scope.
     */
    public static void add(PhaseListener phaseListener) {
        add(getContext(),phaseListener);
    }

    /**
     * Adds the given phase listener to the current request scope.
     * @param context The current {@link FacesContext}
     * @param phaseListener The phase listener to be added to the current request scope.
     */
    public static void add(FacesContext context,PhaseListener phaseListener) {
        getCallbackPhaseListeners(context, true).add(phaseListener);
    }

    /**
     * Removes the given phase listener from the current request scope.
     * @param phaseListener The phase listener to be removed from the current request scope.
     * @return <code>true</code> if the current request scope indeed contained the given phase listener.
     */
    public static boolean remove(PhaseListener phaseListener) {
        return remove(getContext(),phaseListener);
    }

    /**
     * Removes the given phase listener from the current request scope.
     * @param phaseListener The phase listener to be removed from the current request scope.
     * @return <code>true</code> if the current request scope indeed contained the given phase listener.
     */
    public static boolean remove(FacesContext context,PhaseListener phaseListener) {
        Set<PhaseListener> phaseListeners = getCallbackPhaseListeners(context, false);
        return phaseListeners != null && phaseListeners.remove(phaseListener);
    }

    // Helpers --------------------------------------------------------------------------------------------------------

    private static Set<PhaseListener> getCallbackPhaseListeners(FacesContext context, boolean create) {
        return getRequestAttribute(context, CallbackPhaseListener.class.getName(), () -> create ? new HashSet<>(1) : null);
    }

    private static Set<PhaseListener> getCallbackPhaseListenersForEvent(PhaseEvent event) {
        Set<PhaseListener> phaseListeners = getCallbackPhaseListeners(event.getFacesContext(), false);

        if (phaseListeners == null) {
            return Collections.emptySet();
        }

        Set<PhaseListener> phaseListenersForEvent = new HashSet<>();
        for (PhaseListener phaseListener : phaseListeners) {
            if (isPhaseMatch(event, phaseListener.getPhaseId())) {
                phaseListenersForEvent.add(phaseListener);
            }
        }
        return Collections.unmodifiableSet(phaseListenersForEvent);
    }

    private static boolean isPhaseMatch(PhaseEvent event, PhaseId phaseId) {
        return ANY_PHASE.equals(phaseId) || event.getPhaseId().equals(phaseId);
    }

}