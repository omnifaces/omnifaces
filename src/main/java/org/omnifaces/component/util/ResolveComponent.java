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
package org.omnifaces.component.util;

import static java.lang.String.format;
import static org.omnifaces.component.util.ResolveComponent.PropertyKeys.name;
import static org.omnifaces.component.util.ResolveComponent.PropertyKeys.scope;
import static org.omnifaces.util.Components.findComponentRelatively;
import static org.omnifaces.util.Events.subscribeToViewEvent;
import static org.omnifaces.util.Faces.isPostback;
import static org.omnifaces.util.Faces.setRequestAttribute;
import static org.omnifaces.util.Utils.isEmpty;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UIComponent;
import jakarta.faces.event.ComponentSystemEvent;
import jakarta.faces.event.PostRestoreStateEvent;
import jakarta.faces.event.PreRenderViewEvent;
import jakarta.faces.event.SystemEvent;
import jakarta.faces.event.SystemEventListener;
import jakarta.faces.view.facelets.FaceletContext;

import org.omnifaces.el.ReadOnlyValueExpression;
import org.omnifaces.taghandler.ComponentExtraHandler;
import org.omnifaces.util.Components;
import org.omnifaces.util.FunctionalInterfaces.SerializableSupplier;
import org.omnifaces.util.State;

/**
 * <p>
 * The <code>&lt;o:resolveComponent&gt;</code> component is a utility component via which a component can be looked up
 * by its ID and a reference to it put in either the "facelet scope" (default) or the request scope.
 *
 * @since 2.0
 * @author Arjan Tijms
 * @see FaceletContextConsumer
 * @see ComponentExtraHandler
 * @see UtilFamily
 */
@FacesComponent(ResolveComponent.COMPONENT_TYPE)
public class ResolveComponent extends UtilFamily implements FaceletContextConsumer, SystemEventListener {

    /** The component type, which is {@value org.omnifaces.component.util.ResolveComponent#COMPONENT_TYPE}. */
    public static final String COMPONENT_TYPE = "org.omnifaces.component.util.ResolveComponent";

    private static final String ERROR_COMPONENT_NOT_FOUND =
        "A component with ID '%s' as specified by the 'for' attribute of the ResolveComponent with Id '%s' could not be found.";

    private static final String ERROR_ILLEGAL_SCOPE =
        "o:resolveComponent 'scope' attribute only supports 'facelet' (default) or 'request'. Encountered an invalid value of '%s'.";

    /** The default scope, which is "facelet". */
    public static final String DEFAULT_SCOPE = "facelet";

    private ReadOnlyValueExpression readOnlyValueExpression;

    private final State state = new State(getStateHelper());

    enum PropertyKeys {
        name, scope, /* for */
    }

    /**
     * Constructs the component.
     */
    public ResolveComponent() {
        if (!isPostback()) { // For an initial (GET) request, there's no restore
                                // state event and we use pre-render view
            subscribeToViewEvent(PreRenderViewEvent.class, this);
        }
    }

    // Actions --------------------------------------------------------------------------------------------------------

    @Override
    public void setFaceletContext(FaceletContext faceletContext) {
        if (DEFAULT_SCOPE.equals(getScope())) {

            readOnlyValueExpression = new ReadOnlyValueExpression(UIComponent.class);

            faceletContext.getVariableMapper().setVariable(getName(), readOnlyValueExpression);
        }
    }

    @Override
    public boolean isListenerForSource(Object source) {
        return true;
    }

    @Override
    public void processEvent(SystemEvent event) {
        doProcess();
    }

    @Override
    public void processEvent(ComponentSystemEvent event) {
        if (event instanceof PostRestoreStateEvent) { // For a postback we use the post-restore state event.
            doProcess();
        }
    }

    private void doProcess() {
        var forValue = getFor();

        if (!isEmpty(forValue)) {
            var component = findComponentRelatively(this, forValue);

            if (component == null) {
                component = findComponent(forValue);
            }

            if (component == null) {
                throw new IllegalArgumentException(format(ERROR_COMPONENT_NOT_FOUND, forValue, getId()));
            }

            var scope = getScope();  // TODO: refactor "scope" to a reusable enum, together with those of a.o. Cache.

            if (DEFAULT_SCOPE.equals(scope)) {
                // Component will be resolved again dynamically when the value expression is evaluated.
                if (readOnlyValueExpression != null) {
                    readOnlyValueExpression.setCallback(new ComponentClientIdResolver(component.getClientId()));
                }
            }
            else if ("request".equals(scope)) {
                setRequestAttribute(getName(), component);
            }
            else {
                throw new IllegalArgumentException(format(ERROR_ILLEGAL_SCOPE, scope));
            }
        }
    }

    private static class ComponentClientIdResolver implements SerializableSupplier<Object> {

        private static final long serialVersionUID = 1L;

        private final String foundComponentId;
        private transient UIComponent foundComponent;

        public ComponentClientIdResolver(String foundComponentId) {
            this.foundComponentId = foundComponentId;
        }

        @Override
        public Object get() {
            if (foundComponent == null) {
                foundComponent = Components.findComponent(foundComponentId);
            }
            return foundComponent;
        }
    }

    // Attribute getters/setters --------------------------------------------------------------------------------------

    /**
     * Returns name under which the component will be made available to EL.
     * @return Name under which the component will be made available to EL.
     */
    public String getName() {
        return state.get(name);
    }

    /**
     * Sets name under which the component will be made available to EL, scoped to the body of the Facelets tag (default)
     * or to the request.
     * @param nameValue Name under which the component will be made available to EL.
     */
    public void setName(String nameValue) {
        state.put(name, nameValue);
    }

    /**
     * Returns ID of the component that will be resolved (looked-up) and if found a reference of it made available to EL.
     * @return ID of the component that will be resolved (looked-up) and if found a reference of it made available to EL.
     */
    public String getFor() {
        return state.get("for");
    }

    /**
     * Sets ID of the component that will be resolved (looked-up) and if found a reference of it made available to EL.
     * @param forValue ID of the component that will be resolved (looked-up) and if found a reference of it made available to EL.
     */
    public void setFor(String forValue) {
        state.put("for", forValue);
    }

    /**
     * Returns optional scope identifier used to set the scope in which the component reference is inserted. Default is <code>facelet</code>.
     * @return Optional scope identifier used to set the scope in which the component reference is inserted.
     */
    public String getScope() {
        return state.get(scope, DEFAULT_SCOPE);
    }

    /**
     * Optional scope identifier used to set the scope in which the component reference is inserted. If no scope is specified,
     * the default scope "facelet" will be used.
     * <p>
     * Values values are "facelet" (default) and "request".
     * @param scopeValue Optional scope identifier used to set the scope in which the component reference is inserted.
     */
    public void setScope(String scopeValue) {
        state.put(scope, scopeValue);
    }

}