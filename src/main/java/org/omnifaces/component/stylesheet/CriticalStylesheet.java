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
import jakarta.faces.event.AbortProcessingException;
import jakarta.faces.event.ComponentSystemEvent;
import jakarta.faces.event.ListenerFor;
import jakarta.faces.event.PostAddToViewEvent;

import org.omnifaces.config.OmniFaces;
import org.omnifaces.renderer.CriticalStylesheetRenderer;
import org.omnifaces.util.State;
import org.omnifaces.vdl.FacesAttribute;

/**
 * <p>
 * The <code>&lt;o:criticalStylesheet&gt;</code> is a component based on the standard <code>&lt;h:outputStylesheet&gt;</code> which renders a
 * <code>&lt;link rel="preload" as="style"&gt;</code> instead of <code>&lt;link rel="stylesheet"&gt;</code> and automatically changes the
 * <code>rel="preload"</code> to <code>rel="stylesheet"</code> during window load event. Additionally, it will automatically be moved to the very top of the
 * head.
 *
 * <h2>Usage</h2>
 * <p>
 * Just use it the same way as a <code>&lt;h:outputStylesheet&gt;</code>, with a <code>library</code> and <code>name</code>.
 *
 * <pre>
 * &lt;o:criticalStylesheet library="yourlibrary" name="scripts/filename.js" /&gt;
 * </pre>
 * <p>
 * You can even explicitly configure third-party stylesheet resources to be loaded this way, such as PrimeFaces stylesheets.
 *
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
@FacesComponent(value = CriticalStylesheet.COMPONENT_TYPE, namespace = OmniFaces.OMNIFACES_NAMESPACE)
@ListenerFor(systemEventClass = PostAddToViewEvent.class)
public class CriticalStylesheet extends StylesheetFamily {

    // Public constants -----------------------------------------------------------------------------------------------

    /** The component type, which is {@value org.omnifaces.component.stylesheet.CriticalStylesheet#COMPONENT_TYPE}. */
    public static final String COMPONENT_TYPE = "org.omnifaces.component.stylesheet.CriticalStylesheet";

    enum PropertyKeys {
        library,
        name,
        media
    }

    // Variables ------------------------------------------------------------------------------------------------------

    private final State state = new State(getStateHelper());

    // Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Construct a new {@link CriticalStylesheet} component whereby the renderer type is set to {@link CriticalStylesheetRenderer#RENDERER_TYPE}.
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
            var context = event.getFacesContext();
            context.getViewRoot().addComponentResource(context, this, "head");
        }
    }

    // Attribute getters/setters --------------------------------------------------------------------------------------

    /**
     * Returns the "library name" part of the resource identifier.
     *
     * @return The library name.
     */
    public String getLibrary() {
        return state.get(PropertyKeys.library);
    }

    /**
     * Sets the "library name" part of the resource identifier.
     *
     * @param library The library name.
     */
    public void setLibrary(String library) {
        state.put(PropertyKeys.library, library);
    }

    /**
     * Returns the "resource name" part of the resource identifier.
     *
     * @return The resource name.
     */
    public String getName() {
        return state.get(PropertyKeys.name);
    }

    /**
     * Sets the "resource name" part of the resource identifier.
     *
     * @param name The resource name.
     */
    @FacesAttribute(required = true)
    public void setName(String name) {
        state.put(PropertyKeys.name, name);
    }

    /**
     * Returns the media that the stylesheet applies to.
     *
     * @return The media type.
     */
    public String getMedia() {
        return state.get(PropertyKeys.media);
    }

    /**
     * Sets the media that the stylesheet applies to.
     *
     * @param media The media type.
     */
    public void setMedia(String media) {
        state.put(PropertyKeys.media, media);
    }

}
