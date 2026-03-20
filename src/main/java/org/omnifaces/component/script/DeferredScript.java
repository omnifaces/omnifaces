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
package org.omnifaces.component.script;

import static org.omnifaces.config.OmniFaces.OMNIFACES_LIBRARY_NAME;
import static org.omnifaces.config.OmniFaces.OMNIFACES_SCRIPT_NAME;

import jakarta.faces.application.ResourceDependency;
import jakarta.faces.component.FacesComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ComponentSystemEvent;
import jakarta.faces.event.ListenerFor;
import jakarta.faces.event.PostAddToViewEvent;
import jakarta.faces.event.PostRestoreStateEvent;

import org.omnifaces.renderer.DeferredScriptRenderer;
import org.omnifaces.util.State;
import org.omnifaces.config.OmniFaces;
import org.omnifaces.vdl.FacesAttribute;

/**
 * <p>
 * The <code>&lt;o:deferredScript&gt;</code> is a component based on the standard <code>&lt;h:outputScript&gt;</code>
 * which defers the loading of the given script resource to the window load event. In other words, the given script
 * resource is only loaded when the window is really finished with loading. So, the enduser can start working with the
 * webpage without waiting for the additional scripts to be loaded. Usually, it are those kind of scripts which are just
 * for progressive enhancement and thus not essential for the functioning of the webpage.
 * <p>
 * This will give bonus points with among others the Google PageSpeed tool, on the contrary to placing the script at
 * bottom of body, or using <code>defer="true"</code> or even <code>async="true"</code>.
 * <p>
 * Since 2.4 this will render the <code>crossorigin</code> attribute with a value of <code>anonymous</code>.
 * Since 3.13 this will also render the <code>integrity</code> attribute with a base64 encoded sha384 hash as SRI, see
 * also <a href="https://developer.mozilla.org/en-US/docs/Web/Security/Subresource_Integrity">MDN</a>.
 *
 * <h2>Usage</h2>
 * <p>
 * Just use it the same way as a <code>&lt;h:outputScript&gt;</code>, with a <code>library</code> and <code>name</code>.
 * <pre>
 * &lt;o:deferredScript library="yourlibrary" name="scripts/filename.js" /&gt;
 * </pre>
 * <p>
 * You can use the optional <code>onbegin</code>, <code>onsuccess</code> and <code>onerror</code> attributes
 * to declare JavaScript code which needs to be executed respectively right before the script is loaded,
 * right after the script is successfully loaded, and/or when the script loading failed.
 *
 * @author Bauke Scholtz
 * @since 1.8
 * @see DeferredScriptRenderer
 * @see ScriptFamily
 */
@FacesComponent(value = DeferredScript.COMPONENT_TYPE, namespace = OmniFaces.OMNIFACES_NAMESPACE)
@ResourceDependency(library=OMNIFACES_LIBRARY_NAME, name=OMNIFACES_SCRIPT_NAME, target="head") // Specifically DeferredScript.ts.
@ListenerFor(systemEventClass=PostAddToViewEvent.class)
@ListenerFor(systemEventClass=PostRestoreStateEvent.class)
public class DeferredScript extends ScriptFamily {

    // Public constants -----------------------------------------------------------------------------------------------

    /** The component type, which is {@value org.omnifaces.component.script.DeferredScript#COMPONENT_TYPE}. */
    public static final String COMPONENT_TYPE = "org.omnifaces.component.script.DeferredScript";

    enum PropertyKeys {
        library,
        name,
        group,
        onbegin,
        onsuccess,
        onerror
    }

    // Variables ------------------------------------------------------------------------------------------------------

    private final State state = new State(getStateHelper());

    // Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Construct a new {@link DeferredScript} component whereby the renderer type is set to
     * {@link DeferredScriptRenderer#RENDERER_TYPE}.
     */
    public DeferredScript() {
        setRendererType(DeferredScriptRenderer.RENDERER_TYPE);
    }

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * Move this component to body using {@link #moveToBody(ComponentSystemEvent)}. If successfully moved,
     * then set the script resource as rendered, so that Faces won't auto-include it.
     */
    @Override
    public void processEvent(ComponentSystemEvent event) {
        if (moveToBody(event)) {
            FacesContext context = event.getFacesContext();
            context.getApplication().getResourceHandler().markResourceRendered(context, getName(), getLibrary());
        }
    }

    // Attribute getters/setters --------------------------------------------------------------------------------------

    /**
     * Returns the "library name" part of the resource identifier.
     * @return The library name.
     */
    public String getLibrary() {
        return state.get(PropertyKeys.library);
    }

    /**
     * Sets the "library name" part of the resource identifier.
     * @param library The library name.
     */
    public void setLibrary(String library) {
        state.put(PropertyKeys.library, library);
    }

    /**
     * Returns the "resource name" part of the resource identifier.
     * @return The resource name.
     */
    public String getName() {
        return state.get(PropertyKeys.name);
    }

    /**
     * Sets the "resource name" part of the resource identifier.
     * @param name The resource name.
     */
    @FacesAttribute(required = true)
    public void setName(String name) {
        state.put(PropertyKeys.name, name);
    }

    /**
     * Returns the group name on which the deferred script resources should be combined by CombinedResourceHandler.
     * All deferred scripts resources having the same combined group name will be combined into a single deferred
     * script in the place where the first member of the group occurs in the component tree.
     * @return The group name.
     */
    public String getGroup() {
        return state.get(PropertyKeys.group);
    }

    /**
     * Sets the group name on which the deferred script resources should be combined by CombinedResourceHandler.
     * All deferred scripts resources having the same combined group name will be combined into a single deferred
     * script in the place where the first member of the group occurs in the component tree.
     * @param group The group name.
     */
    public void setGroup(String group) {
        state.put(PropertyKeys.group, group);
    }

    /**
     * Returns the JavaScript code to execute right before the script loading begins.
     * @return The onbegin script.
     */
    public String getOnbegin() {
        return state.get(PropertyKeys.onbegin);
    }

    /**
     * Sets the JavaScript code to execute right before the script loading begins.
     * @param onbegin The onbegin script.
     */
    public void setOnbegin(String onbegin) {
        state.put(PropertyKeys.onbegin, onbegin);
    }

    /**
     * Returns the JavaScript code to execute when the script loading is successfully completed.
     * @return The onsuccess script.
     */
    public String getOnsuccess() {
        return state.get(PropertyKeys.onsuccess);
    }

    /**
     * Sets the JavaScript code to execute when the script loading is successfully completed.
     * @param onsuccess The onsuccess script.
     */
    public void setOnsuccess(String onsuccess) {
        state.put(PropertyKeys.onsuccess, onsuccess);
    }

    /**
     * Returns the JavaScript code to execute when the script loading has failed.
     * @return The onerror script.
     */
    public String getOnerror() {
        return state.get(PropertyKeys.onerror);
    }

    /**
     * Sets the JavaScript code to execute when the script loading has failed.
     * @param onerror The onerror script.
     */
    public void setOnerror(String onerror) {
        state.put(PropertyKeys.onerror, onerror);
    }

}