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

import static jakarta.faces.event.PhaseId.RENDER_RESPONSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static org.omnifaces.config.OmniFaces.OMNIFACES_LIBRARY_NAME;
import static org.omnifaces.config.OmniFaces.OMNIFACES_SCRIPT_NAME;
import static org.omnifaces.util.ComponentsLocal.addFacesScriptResource;
import static org.omnifaces.util.ComponentsLocal.addScriptResource;
import static org.omnifaces.util.ComponentsLocal.getCurrentForm;
import static org.omnifaces.util.Events.subscribeToRequestBeforePhase;
import static org.omnifaces.util.Faces.getContext;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UIForm;
import jakarta.faces.component.UIInput;
import jakarta.faces.component.visit.VisitContext;
import jakarta.faces.component.visit.VisitHint;
import jakarta.faces.component.visit.VisitResult;
import jakarta.faces.context.FacesContext;

import org.omnifaces.util.State;

/**
 * <p>
 * The <code>&lt;o:highlight&gt;</code> is a helper component which highlights all invalid {@link UIInput} components
 * and the associated labels by adding an error style class to them. Additionally, it by default focuses the first
 * invalid {@link UIInput} component. The <code>&lt;o:highlight /&gt;</code> component can be placed anywhere in the
 * view, as long as there's only one of it. Preferably put it somewhere in the master template for forms.
 * <pre>
 * &lt;h:form&gt;
 *     &lt;h:inputText value="#{bean.input1}" required="true" /&gt;
 *     &lt;h:inputText value="#{bean.input2}" required="true" /&gt;
 *     &lt;h:commandButton value="Submit" action="#{bean.submit}" /&gt;
 * &lt;/h:form&gt;
 * &lt;o:highlight /&gt;
 * </pre>
 * <p>
 * The default error style class name is <code>error</code>. You need to specify a CSS style associated with the class
 * yourself. For example,
 * <pre>
 * label.error {
 *     color: #f00;
 * }
 * input.error, select.error, textarea.error {
 *     background-color: #fee;
 * }
 * </pre>
 * <p>
 * You can override the default error style class by the <code>styleClass</code> attribute:
 * <pre>
 * &lt;o:highlight styleClass="invalid" /&gt;
 * </pre>
 * <p>
 * You can disable the default focus on the first invalid input element setting the <code>focus</code> attribute.
 * <pre>
 * &lt;o:highlight styleClass="invalid" focus="false" /&gt;
 * </pre>
 * <p>
 * Since version 2.5, the error style class will be removed from the input element and its associated label when the
 * enduser starts using the input element.
 *
 * @author Bauke Scholtz
 * @see OnloadScript
 * @see ScriptFamily
 */
@FacesComponent(Highlight.COMPONENT_TYPE)
public class Highlight extends OnloadScript {

    // Public constants -----------------------------------------------------------------------------------------------

    /** The component type, which is {@value org.omnifaces.component.script.Highlight#COMPONENT_TYPE}. */
    public static final String COMPONENT_TYPE = "org.omnifaces.component.script.Highlight";

    // Private constants ----------------------------------------------------------------------------------------------

    private static final Set<VisitHint> VISIT_HINTS = EnumSet.of(VisitHint.SKIP_UNRENDERED);
    private static final String DEFAULT_STYLECLASS = "error";
    private static final Boolean DEFAULT_FOCUS = TRUE;
    private static final String SCRIPT = "OmniFaces.Highlight.apply([%s], '%s', %s);";

    private enum PropertyKeys {
        // Cannot be uppercased. They have to exactly match the attribute names.
        styleClass, focus
    }

    // Variables ------------------------------------------------------------------------------------------------------

    private final State state = new State(getStateHelper());

    // Init -----------------------------------------------------------------------------------------------------------

    /**
     * The constructor instructs Faces to register all scripts during the render response phase if necessary.
     */
    public Highlight() {
        subscribeToRequestBeforePhase(RENDER_RESPONSE, this::registerScriptsIfNecessary);
    }

    private void registerScriptsIfNecessary() {
        // This is supposed to be declared via @ResourceDependency, but JSF 3 and Faces 4 use a different script
        // resource name which cannot be resolved statically.
        var context = getContext();
        addFacesScriptResource(context); // Required for jsf.ajax.request.
        addScriptResource(context, OMNIFACES_LIBRARY_NAME, OMNIFACES_SCRIPT_NAME);
    }

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * Visit all components of the current {@link UIForm}, check if they are an instance of {@link UIInput} and are not
     * {@link UIInput#isValid()} and finally append them to an array in JSON format and render the script.
     * <p>
     * Note that the {@link FacesContext#getClientIdsWithMessages()} could also be consulted, but it does not indicate
     * whether the components associated with those client IDs are actually {@link UIInput} components which are not
     * {@link UIInput#isValid()}. Also note that the highlighting is been done by delegating the job to JavaScript
     * instead of directly changing the component's own <code>styleClass</code> attribute; this is chosen so because we
     * don't want the changed style class to be saved in the server side view state as it may result in potential
     * inconsistencies because it's supposed to be an one-time change.
     */
    @Override
    public void encodeChildren(FacesContext context) throws IOException {
        var form = getCurrentForm(context);

        if (form == null) {
            return;
        }

        var clientIds = new StringBuilder();
        form.visitTree(VisitContext.createVisitContext(context, null, VISIT_HINTS), (visitContext, component) -> {
            if (component instanceof UIInput && !((UIInput) component).isValid()) {
                if (clientIds.length() > 0) {
                    clientIds.append(',');
                }

                var clientId = component.getClientId(visitContext.getFacesContext());
                clientIds.append('"').append(clientId).append('"');
            }

            return VisitResult.ACCEPT;
        });

        if (clientIds.length() > 0) {
            context.getResponseWriter().write(format(SCRIPT, clientIds, getStyleClass(), isFocus()));
        }
    }

    /**
     * This component is per definiton only rendered when the current request is a postback request and the
     * validation has failed.
     */
    @Override
    public boolean isRendered() {
        var context = getFacesContext();
        return context.isPostback() && context.isValidationFailed() && super.isRendered();
    }

    // Getters/setters ------------------------------------------------------------------------------------------------

    /**
     * Returns the error style class which is to be applied on invalid inputs. Defaults to <code>error</code>.
     * @return The error style class which is to be applied on invalid inputs.
     */
    public String getStyleClass() {
        return state.get(PropertyKeys.styleClass, DEFAULT_STYLECLASS);
    }

    /**
     * Sets the error style class which is to be applied on invalid inputs.
     * @param styleClass The error style class which is to be applied on invalid inputs.
     */
    public void setStyleClass(String styleClass) {
        state.put(PropertyKeys.styleClass, styleClass);
    }

    /**
     * Returns whether the first error element should gain focus. Defaults to <code>true</code>.
     * @return Whether the first error element should gain focus.
     */
    public boolean isFocus() {
        return state.get(PropertyKeys.focus, DEFAULT_FOCUS);
    }

    /**
     * Sets whether the first error element should gain focus.
     * @param focus Whether the first error element should gain focus.
     */
    public void setFocus(boolean focus) {
        state.put(PropertyKeys.focus, focus);
    }

}