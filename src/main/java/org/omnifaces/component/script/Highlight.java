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
package org.omnifaces.component.script;

import static java.lang.Boolean.TRUE;
import static org.omnifaces.util.Components.getCurrentForm;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

import javax.faces.application.ResourceDependencies;
import javax.faces.application.ResourceDependency;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.component.UIInput;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitHint;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;

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
 *
 * @author Bauke Scholtz
 * @see OnloadScript
 * @see ScriptFamily
 */
@FacesComponent(Highlight.COMPONENT_TYPE)
@ResourceDependencies({
	@ResourceDependency(library="javax.faces", name="jsf.js", target="head"), // Required for jsf.ajax.addOnEvent.
	@ResourceDependency(library="omnifaces", name="omnifaces.js", target="head") // Specifically highlight.js.
})
public class Highlight extends OnloadScript {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The standard component type. */
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
		UIForm form = getCurrentForm();

		if (form == null) {
			return;
		}

		final StringBuilder clientIds = new StringBuilder();
		form.visitTree(VisitContext.createVisitContext(context, null, VISIT_HINTS), new VisitCallback() {

			@Override
			public VisitResult visit(VisitContext context, UIComponent component) {
				if (component instanceof UIInput && !((UIInput) component).isValid()) {
					if (clientIds.length() > 0) {
						clientIds.append(',');
					}

					String clientId = component.getClientId(context.getFacesContext());
					clientIds.append('"').append(clientId).append('"');
				}

				return VisitResult.ACCEPT;
			}
		});

		if (clientIds.length() > 0) {
			context.getResponseWriter().write(String.format(SCRIPT, clientIds, getStyleClass(), isFocus()));
		}
	}

	/**
	 * This component is per definiton only rendered when the current request is a postback request and the
	 * validation has failed.
	 */
	@Override
	public boolean isRendered() {
		FacesContext context = getFacesContext();
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