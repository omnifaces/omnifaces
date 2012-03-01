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
package org.omnifaces.util;

import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

/**
 * Collection of utility methods for the JSF API that are mainly shortcuts for obtaining stuff from the
 * {@link UIComponent}.
 *
 * @author Bauke Scholtz
 */
public final class Components {

	// Constructors ---------------------------------------------------------------------------------------------------

	private Components() {
		// Hide constructor.
	}

	// Utility --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the current UI component from the EL context.
	 * @return The current UI component from the EL context.
	 * @see UIComponent#getCurrentComponent(FacesContext)
	 */
	public static UIComponent getCurrentComponent() {
		return UIComponent.getCurrentComponent(FacesContext.getCurrentInstance());
	}

	/**
	 * Returns the UI component matching the given client ID search expression.
	 * @param expression The client ID search expression.
	 * @return The UI component matching the given client ID search expression.
	 * @see UIComponent#findComponent(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T extends UIComponent> T findComponent(String expression) {
		return (T) FacesContext.getCurrentInstance().getViewRoot().findComponent(expression);
	}

	/**
	 * Returns from the given component the closest parent of the given parent type, or <code>null</code> if none
	 * is found.
	 * @param <T> The generic parent type.
	 * @param component The component to return the closest parent of the given parent type for.
	 * @param parentType The parent type.
	 * @return From the given component the closest parent of the given parent type, or <code>null</code> if none
	 * is found.
	 */
	public static <T extends UIComponent> T getClosestParent(UIComponent component, Class<T> parentType) {
		UIComponent parent = component.getParent();

		while (parent != null && !parentType.isInstance(parent)) {
			parent = parent.getParent();
		}

		return parentType.cast(parent);
	}

	/**
	 * Returns whether the given UI input component is editable. That is when it is rendered, not disabled and not
	 * readonly.
	 * @param input The UI input component to be checked.
	 * @return <code>true</code> if the given UI input component is editable.
	 */
	public static boolean isEditable(UIInput input) {
		return input.isRendered()
			&& !Boolean.TRUE.equals(input.getAttributes().get("disabled"))
			&& !Boolean.TRUE.equals(input.getAttributes().get("readonly"));
	}

	/**
	 * Returns the value of the <code>label</code> attribute associated with the given UI input component if any, else
	 * the client ID. It never returns null.
	 * @param input The UI input component for which the label is to be retrieved.
	 * @return The value of the <code>label</code> attribute associated with the given UI input component if any, else
	 * the client ID.
	 */
	public static String getLabel(UIInput input) {
		Object label = input.getAttributes().get("label");
		return (label != null) ? label.toString() : input.getClientId();
	}

	/**
	 * Returns the converted/validated value of the given UI input component without the need to know if the given
	 * component has already been converted/validated or not. If the conversion/validation has failed for the UI input
	 * component, then this method will return <code>null</code>.
	 * @param <T> The return type to cast the obtained value to.
	 * @param input The UI input component to obtain the converted/validated value for.
	 * @return The converted/validated value of the given UI input component.
	 * @throws ClassCastException When T is of the wrong type.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getValue(UIInput input) {

		// Components are converted/validated in order as they appear in the component tree. If isValid() returns true
		// and getSubmittedValue() returns non-null, then it means that it has yet to be converted/validated by the
		// validate() method before we can grab the value by getValue(). If isLocalValueSet() returns true, then it
		// means that it's successfully been converted/validated and thus we can safely grab the value by getValue().

		if (input.isValid() && hasSubmittedValue(input)) {
			input.validate(FacesContext.getCurrentInstance());
		}

		return input.isLocalValueSet() ? (T) input.getValue() : null;
	}

	/**
	 * Returns whether the given editable value holder component has a submitted value.
	 * @param component The editable value holder component to be checked.
	 * @return <code>true</code> if the given editable value holder component has a submitted value, otherwise
	 * <code>false</code>.
	 */
	public static boolean hasSubmittedValue(EditableValueHolder component) {
		Object submittedValue = component.getSubmittedValue();
		return (submittedValue != null && !String.valueOf(submittedValue).isEmpty());
	}

}