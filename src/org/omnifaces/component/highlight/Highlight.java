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
package org.omnifaces.component.highlight;

import java.io.IOException;

import javax.faces.application.ResourceDependency;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.component.UIForm;
import javax.faces.component.UIInput;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.omnifaces.util.Components;

/**
 * <strong>Highlight</strong> is a helper component which highlights all invalid {@link UIInput} components by adding
 * an error style class to them. Additionally, it by default focuses the first invalid {@link UIInput} component. The
 * <code>&lt;o:highlight /&gt;</code> component needs to be placed in the view <i>after</i> any of the {@link UIInput}
 * components.
 * <pre>
 * &lt;h:form&gt;
 *   &lt;h:inputText value="#{bean.input1}" required="true" /&gt;
 *   &lt;h:inputText value="#{bean.input1}" required="true" /&gt;
 *   &lt;h:commandButton value="Submit" action="#{bean.submit}" /&gt;
 *   &lt;o:highlight /&gt;
 * &lt;/h:form&gt;
 * </pre>
 * <p>
 * The default error style class name is <tt>error</tt>. You need to specify a CSS style associated with the class
 * yourself. For example,
 * <pre>
 * .error {
 *   background-color: #fee;
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
 */
@FacesComponent(Highlight.COMPONENT_TYPE)
@ResourceDependency(library="omnifaces", name="omnifaces.js", target="head")
public class Highlight extends UIComponentBase {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The standard component family. */
	public static final String COMPONENT_FAMILY = "org.omnifaces.component.highlight";

	/** The standard component type. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.highlight.Highlight";

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String DEFAULT_STYLECLASS = "error";
	private static final Boolean DEFAULT_FOCUS = Boolean.TRUE;
	private static final String SCRIPT = "OmniFaces.Highlight.addErrorClass(%s, '%s', %s)";

	private enum PropertyKeys {
		styleClass, focus
	}

	// UIComponent overrides ------------------------------------------------------------------------------------------

	/**
	 * Returns {@link #COMPONENT_FAMILY}.
	 */
	@Override
	public String getFamily() {
		return COMPONENT_FAMILY;
	}

	/**
	 * Returns <code>false</code>.
	 */
	@Override
	public boolean getRendersChildren() {
		return false;
	}

	/**
	 * Visit all componants of the parent {@link UIForm}, check if they are an instance of {@link UIInput} and are not
	 * {@link UIInput#isValid()} and finally append them to an array in JSON format and render the script call.
	 * <p>
	 * Note that the {@link FacesContext#getClientIdsWithMessages()} could also be consulted, but it does not indicate
	 * whether the components associated with those client IDs are actually {@link UIInput} components which are not
	 * {@link UIInput#isValid()}. Also note that the highlighting is been done by delegated the job to JavaScript
	 * instead of directly changing the component's own <code>styleClass</code> attribute; this is chosen so because we
	 * don't want it to be saved in the server side view state.
	 */
	@Override
	public void encodeAll(FacesContext context) throws IOException {
		Components.validateHasParent(this, UIForm.class);
		Components.validateHasNoChildren(this);

		final StringBuilder clientIdsAsJSON = new StringBuilder();
		UIForm form = Components.getClosestParent(this, UIForm.class);
		form.visitTree(VisitContext.createVisitContext(context), new VisitCallback() {

			@Override
			public VisitResult visit(VisitContext context, UIComponent component) {
				if (component instanceof UIInput && !((UIInput) component).isValid()) {
		        	if (clientIdsAsJSON.length() > 0) {
		        		clientIdsAsJSON.append(',');
		        	}

		        	clientIdsAsJSON.append('"').append(component.getClientId(context.getFacesContext())).append('"');
				}

				return VisitResult.ACCEPT;
			}
		});

		if (clientIdsAsJSON.length() > 0) {
			clientIdsAsJSON.insert(0, '[').append(']');

			ResponseWriter writer = context.getResponseWriter();
	        writer.startElement("script", null);
	        writer.writeAttribute("type", "text/javascript", null);
	        writer.write(String.format(SCRIPT, clientIdsAsJSON, getStyleClass(), isFocus()));
	        writer.endElement("script");
		}
	}

	// Getters/setters ------------------------------------------------------------------------------------------------

	/**
	 * Returns the error style class which is to be applied on invalid inputs. Defaults to <tt>error</tt>.
	 * @return The error style class which is to be applied on invalid inputs.
	 */
	public String getStyleClass() {
		return (String) getStateHelper().eval(PropertyKeys.styleClass, DEFAULT_STYLECLASS);
	}

	/**
	 * Sets the error style class which is to be applied on invalid inputs.
	 * @param styleClass The error style class which is to be applied on invalid inputs.
	 */
	public void setStyleClass(String styleClass) {
		getStateHelper().put(PropertyKeys.styleClass, styleClass);
	}

	/**
	 * Returns whether the first error element should gain focus. Defaults to <code>true</code>.
	 * @return Whether the first error element should gain focus.
	 */
	public boolean isFocus() {
		return Boolean.valueOf(getStateHelper().eval(PropertyKeys.focus, DEFAULT_FOCUS).toString());
	}

	/**
	 * Sets whether the first error element should gain focus.
	 * @param focus Whether the first error element should gain focus.
	 */
	public void setFocus(boolean focus) {
		getStateHelper().put(PropertyKeys.focus, focus);
	}

}