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
package org.omnifaces.component.stylesheet;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.AbortProcessingException;
import jakarta.faces.event.ComponentSystemEvent;
import jakarta.faces.event.ListenerFor;
import jakarta.faces.event.PostAddToViewEvent;

import org.omnifaces.renderer.CriticalStylesheetRenderer;

/**
 * <p>
 * The <code>&lt;o:criticalStylesheet&gt;</code> is a component based on the standard <code>&lt;h:outputStylesheet&gt;</code>
 * which renders a <code>&lt;link rel="preload" as="style"&gt;</code> instead of <code>&lt;link rel="stylesheet"&gt;</code>
 * and automatically changes the <code>rel="preload"</code> to <code>rel="stylesheet"</code> during window load event.
 * Additionally, it will automatically be moved to the very top of the head.
 * 
 * <h2>Usage</h2>
 * <p>
 * Just use it the same way as a <code>&lt;h:outputStylesheet&gt;</code>, with a <code>library</code> and <code>name</code>.
 * <pre>
 * &lt;o:criticalStylesheet library="yourlibrary" name="scripts/filename.js" /&gt;
 * </pre>
 * <p>
 * You can even explicitly configure third-party stylesheet resources to be loaded this way, such as PrimeFaces stylesheets.
 * <pre>
 * &lt;o:criticalStylesheet library="primefaces" name="components.css" /&gt;
 * &lt;o:criticalStylesheet library="primefaces" name="layout.css" /&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 * @since 4.5
 * @see StylesheetFamily
 * @see CriticalStylesheetRenderer
 */
@FacesComponent(CriticalStylesheet.COMPONENT_TYPE)
@ListenerFor(systemEventClass=PostAddToViewEvent.class)
public class CriticalStylesheet extends StylesheetFamily {

    // Public constants -----------------------------------------------------------------------------------------------

    /** The component type, which is {@value org.omnifaces.component.script.CriticalStylesheet#COMPONENT_TYPE}. */
    public static final String COMPONENT_TYPE = "org.omnifaces.component.stylesheet.CriticalStylesheet";

    // Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Construct a new {@link CriticalStylesheet} component whereby the renderer type is set to
     * {@link CriticalStylesheetRenderer#RENDERER_TYPE}.
     */
    public CriticalStylesheet() {
        setRendererType(CriticalStylesheetRenderer.RENDERER_TYPE);
    }

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * Makes sure the critical style sheet gets added to top of head.
     */
    @Override
    public void processEvent(ComponentSystemEvent event) throws AbortProcessingException {
        if (event instanceof PostAddToViewEvent) {
            FacesContext context = event.getFacesContext();
            context.getViewRoot().addComponentResource(context, event.getComponent(), "head");
        }
    }

}