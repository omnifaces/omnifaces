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
package org.omnifaces.event;

import jakarta.faces.context.FacesContext;
import jakarta.faces.event.FacesEvent;
import jakarta.faces.event.FacesListener;

import org.omnifaces.component.output.LazyPanel;

/**
 * <p>
 * This event is passed as optional method argument of <code>&lt;o:lazyPanel listener&gt;</code>. It has convenient {@link #getComponent()} and
 * {@link #getClientId()} methods.
 *
 * <pre>
 *
 * public void listener(LazyPanelEvent event) {
 *     String clientId = event.getClientId();
 *     // ...
 * }
 * </pre>
 *
 * @author Bauke Scholtz
 * @since 5.3
 * @see LazyPanel
 */
public class LazyPanelEvent extends FacesEvent {

    private static final long serialVersionUID = 1L;

    private final String clientId;

    /**
     * Constructs a new lazy panel event.
     *
     * @param context The involved faces context.
     * @param lazyPanel The involved lazy panel component.
     */
    public LazyPanelEvent(FacesContext context, LazyPanel lazyPanel) {
        super(context, lazyPanel);
        this.clientId = lazyPanel.getClientId(context);
    }

    /**
     * Returns the client id of the lazy panel component.
     *
     * @return The client id of the lazy panel component.
     */
    public String getClientId() {
        return clientId;
    }

    @Override
    public boolean isAppropriateListener(FacesListener listener) {
        return false;
    }

    @Override
    public void processListener(FacesListener listener) {
        // NOOP.
    }

}
