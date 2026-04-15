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
package org.omnifaces.component.input;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.context.FacesContext;

import org.omnifaces.component.input.componentidparam.ConditionalWriterListener;
import org.omnifaces.config.OmniFaces;

/**
 * <p>
 * The <code>&lt;o:componentIdParam&gt;</code> component allows to render just one or more components on a view via a GET parameter.
 * <p>
 * Components can be identified via both their client id or simple component id. Via the former it's possible to e.g. render only a specific row in a table. For
 * specific cases, it's possible to render only the parent component and omit any children.
 * <p>
 * Among the use cases for this is creating simple mashups from various Faces based views, and scripts needing to obtain markup for specific components on an
 * initial (non-faces) request.
 * <p>
 * Note that this is a rather specialized component and for many common use cases the user is advised to investigate if the existing AJAX and partial page
 * requests in Faces don't already cover the requirements. For the moment this component only supports the direct output of the original markup and does not
 * wrap it into any "partial response" envelope.
 *
 * @since 1.1
 * @author Arjan Tijms
 */
@FacesComponent(value = ComponentIdParam.COMPONENT_TYPE, namespace = OmniFaces.OMNIFACES_NAMESPACE)
public class ComponentIdParam extends ViewParam {

    /** The component type, which is {@value org.omnifaces.component.input.ComponentIdParam#COMPONENT_TYPE}. */
    public static final String COMPONENT_TYPE = "org.omnifaces.component.input.ComponentIdParam";

    private enum PropertyKeys {
        // Cannot be uppercased. They have to exactly match the attribute names.
        componentIdName,
        clientIdName,
        renderChildren
    }

    @Override
    public void decode(FacesContext context) {
        List<String> componentIds = getRequestValues(context, getComponentIdName());
        List<String> clientIds = getRequestValues(context, getClientIdName());

        // Installs a PhaseListener on the view root that will replace the response writer before
        // and after rendering with one that only renders when the current component has one of the Ids
        // that we receive from the request here.
        if (!componentIds.isEmpty() || !clientIds.isEmpty()) {
            context.getViewRoot().addPhaseListener(new ConditionalWriterListener(componentIds, clientIds, isRenderChildren()));
        }
    }

    @Override
    public void processValidators(FacesContext context) {
        // NOOP. This component doesn't have a model value anyway.
    }

    @Override
    public void processUpdates(FacesContext context) {
        // NOOP. This component doesn't have a model value anyway.
    }

    // Attribute getters/setters --------------------------------------------------------------------------------------

    /**
     * Returns the name of the request parameters from which the values are retrieved on an initial request that represent component ids of those components
     * from which the markup should appear in the response (i.e. which should be rendered).
     *
     * @return The component id parameter name.
     */
    public String getComponentIdName() {
        return (String) getStateHelper().eval(PropertyKeys.componentIdName);
    }

    /**
     * Sets the name of the request parameters from which the values are retrieved on an initial request that represent component ids of those components from
     * which the markup should appear in the response (i.e. which should be rendered).
     *
     * @param componentIdName The component id parameter name.
     */
    public void setComponentIdName(String componentIdName) {
        getStateHelper().put(PropertyKeys.componentIdName, componentIdName);
    }

    /**
     * Returns the name of the request parameters from which the values are retrieved on an initial request that represent client ids of those components from
     * which the markup should appear in the response (i.e. which should be rendered).
     *
     * @return The client id parameter name.
     */
    public String getClientIdName() {
        return (String) getStateHelper().eval(PropertyKeys.clientIdName);
    }

    /**
     * Sets the name of the request parameters from which the values are retrieved on an initial request that represent client ids of those components from
     * which the markup should appear in the response (i.e. which should be rendered).
     *
     * @param clientIdName The client id parameter name.
     */
    public void setClientIdName(String clientIdName) {
        getStateHelper().put(PropertyKeys.clientIdName, clientIdName);
    }

    /**
     * Returns whether children of the components identified by clientIdName or componentIdName are rendered in addition to the component itself. Defaults to
     * {@code true}.
     *
     * @return Whether children should be rendered.
     */
    public boolean isRenderChildren() {
        return (boolean) getStateHelper().eval(PropertyKeys.renderChildren, true);
    }

    /**
     * Sets whether children of the components identified by clientIdName or componentIdName are rendered in addition to the component itself. Defaults to
     * {@code true}.
     *
     * @param renderChildren Whether children should be rendered.
     */
    public void setRenderChildren(boolean renderChildren) {
        getStateHelper().put(PropertyKeys.renderChildren, renderChildren);
    }

    // Helpers --------------------------------------------------------------------------------------------------------

    /**
     * Gets the list of request values for the given request parameter name.
     *
     * @param context FacesContext for the request we are processing.
     * @param paramName The request parameter name for which values are returned.
     * @return All values in the request corresponding to the given parameter name.
     */
    private static List<String> getRequestValues(FacesContext context, String paramName) {
        if (paramName != null) {
            String[] values = context.getExternalContext().getRequestParameterValuesMap().get(paramName);
            if (values != null) {
                return Arrays.asList(values);
            }
        }

        return Collections.emptyList();
    }

}
