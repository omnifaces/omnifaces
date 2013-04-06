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

import static org.omnifaces.util.Utils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.el.ValueExpression;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.NamingContainer;
import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.component.UIInput;
import javax.faces.component.UIViewRoot;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitHint;
import javax.faces.context.FacesContext;

/**
 * Collection of utility methods for the JSF API with respect to working with {@link UIComponent}.
 *
 * @author Bauke Scholtz
 * @author Arjan Tijms
 */
public final class Components {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String ERROR_INVALID_PARENT =
		"Component '%s' must have a parent of type '%s', but it cannot be found.";
	private static final String ERROR_INVALID_DIRECT_PARENT =
		"Component '%s' must have a direct parent of type '%s', but it cannot be found.";
	private static final String ERROR_CHILDREN_DISALLOWED =
		"Component '%s' must have no children. Encountered children of types '%s'.";

	// Constructors ---------------------------------------------------------------------------------------------------

	private Components() {
		// Hide constructor.
	}

	// General --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the current UI component from the EL context.
	 * @return The current UI component from the EL context.
	 * @see UIComponent#getCurrentComponent(FacesContext)
	 */
	public static UIComponent getCurrentComponent() {
		return UIComponent.getCurrentComponent(FacesContext.getCurrentInstance());
	}

	/**
	 * Returns the attribute of the given component on the given name.
	 * @param <T> The expected return type.
	 * @param component The component to return the attribute of the given name for.
	 * @param name The name of the attribute of the given component to be returned.
	 * @return The attribute of the given component on the given name.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @since 1.5
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getAttribute(UIComponent component, String name) {
		return (T) component.getAttributes().get(name);
	}

	// Traversal ------------------------------------------------------------------------------------------------------

	/**
	 * Returns the UI component matching the given client ID search expression.
	 * @param clientId The client ID search expression.
	 * @return The UI component matching the given client ID search expression.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see UIComponent#findComponent(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T extends UIComponent> T findComponent(String clientId) {
		return (T) FacesContext.getCurrentInstance().getViewRoot().findComponent(clientId);
	}

	/**
	 * Returns the UI component matching the given client ID search expression relative to the point
	 * in the component tree of the given component. For this search both parents and children are
	 * consulted, increasingly moving further away from the given component. Parents are consulted
	 * first, then children.
	 *
	 * @param component the component from which the relative search is started.
	 * @param clientId The client ID search expression.
	 * @return The UI component matching the given client ID search expression.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see UIComponent#findComponent(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T extends UIComponent> T findComponentRelatively(UIComponent component, String clientId) {

		if (isEmpty(clientId)) {
			return null;
		}

		// Search first in the naming container parents of the given component
		UIComponent result = findComponentInParents(component, clientId);

		if (result == null) {
			// If not in the parents, search from the root
			result = findComponentInChildren(Faces.getViewRoot(), clientId);
		}

		return (T) result;
	}

	/**
	 * Returns the UI component matching the given client ID search expression relative to the point
	 * in the component tree of the given component, searching only in its parents.
	 *
	 * @param component the component from which the relative search is started.
	 * @param clientId The client ID search expression.
	 * @return The UI component matching the given client ID search expression.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see UIComponent#findComponent(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T extends UIComponent> T findComponentInParents(UIComponent component, String clientId) {

		if (isEmpty(clientId)) {
			return null;
		}

		UIComponent parent = component;
		while (parent != null) {

			UIComponent result = null;
			if (parent instanceof NamingContainer) {
				try {
					result = parent.findComponent(clientId);
				} catch (IllegalArgumentException e) {
					continue;
				}
			}

			if (result != null) {
				return (T) result;
			}

			parent = parent.getParent();
		}

		return null;
	}

	/**
	 * Returns the UI component matching the given client ID search expression relative to the point
	 * in the component tree of the given component, searching only in its children.
	 *
	 * @param component the component from which the relative search is started.
	 * @param clientId The client ID search expression.
	 * @return The UI component matching the given client ID search expression.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see UIComponent#findComponent(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T extends UIComponent> T findComponentInChildren(UIComponent component, String clientId) {

		if (isEmpty(clientId)) {
			return null;
		}

		for (UIComponent child : component.getChildren()) {

			UIComponent result = null;
			if (child instanceof NamingContainer) {
				try {
					result = child.findComponent(clientId);
				} catch (IllegalArgumentException e) {
					continue;
				}
			}

			if (result == null) {
				result = findComponentInChildren(child, clientId);
			}

			if (result != null) {
				return (T) result;
			}
		}

		return null;
	}

	/**
	 * Returns a list of UI components matching the given type in children of the given component.
	 * @param component The component to search in its children for UI components matching the given type.
	 * @param type The type of the UI components to be searched in children of the given component.
	 * @return A list of UI components matching the given type in children of the given component.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 */
	public static <T extends UIComponent> List<T> findComponentsInChildren(UIComponent component, Class<T> type) {
		List<T> components = new ArrayList<T>();
		findComponentsInChildren(component, type, components);
		return components;
	}

	/**
	 * Helper method for {@link #findComponentsInChildren(UIComponent, Class)} utilizing tail recursion.
	 */
	@SuppressWarnings("unchecked")
	private static <T extends UIComponent> void findComponentsInChildren(UIComponent component, Class<T> type, List<T> matches) {
		for (UIComponent child : component.getChildren()) {
			if (type.isInstance(child)) {
				matches.add((T) child);
			}

			findComponentsInChildren(child, type, matches);
		}
	}

	/**
	 * Returns from the given component the closest parent of the given parent type, or <code>null</code> if none
	 * is found.
	 * @param <T> The generic parent type.
	 * @param component The component to return the closest parent of the given parent type for.
	 * @param parentType The parent type.
	 * @return From the given component the closest parent of the given parent type, or <code>null</code> if none
	 * is found.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 */
	public static <T extends UIComponent> T getClosestParent(UIComponent component, Class<T> parentType) {
		UIComponent parent = component.getParent();

		while (parent != null && !parentType.isInstance(parent)) {
			parent = parent.getParent();
		}

		return parentType.cast(parent);
	}

	/**
	 * Returns true if the given visit context contains the visit hint that iteration should be skipped.
	 * @param context The involved visit context.
	 * @since 1.3
	 */
	public static boolean shouldVisitSkipIteration(VisitContext context) {
		try {
			// JSF 2.1.
			return context.getHints().contains(VisitHint.valueOf("SKIP_ITERATION"));
		}
		catch (IllegalArgumentException e) {
			// JSF 2.0.
			return context.getFacesContext().getAttributes().get("javax.faces.visit.SKIP_ITERATION") == Boolean.TRUE;
		}
	}

	// Forms ----------------------------------------------------------------------------------------------------------

	/**
	 * Returns the currently submitted UI form component, or <code>null</code> if there is none, which may happen when
	 * the current request is not a postback request at all, or when the view has been changed by for example a
	 * successful navigation.
	 * @return The currently submitted UI form component.
	 * @see UIForm#isSubmitted()
	 */
	public static UIForm getCurrentForm() {
		FacesContext facesContext = FacesContext.getCurrentInstance();

		if (!facesContext.isPostback()) {
			return null;
		}

		UIViewRoot viewRoot = facesContext.getViewRoot();

		// The initial implementation has visited the tree for UIForm components which returns true on isSubmitted().
		// But with testing it turns out to return false on ajax requests where the form is not included in execute!
		// The current implementation just walks through the request parameter map instead.

		for (String name : facesContext.getExternalContext().getRequestParameterMap().keySet()) {
			if (name.startsWith("javax.faces.")) {
				continue; // Quick skip.
			}

			try {
				UIComponent component = viewRoot.findComponent(name);

				if (component instanceof UIForm) {
					return (UIForm) component;
				}
				else if (component != null) {
					UIForm form = getClosestParent(component, UIForm.class);

					if (form != null) {
						return form;
					}
				}
			}
			catch (IllegalArgumentException ignore) {
				// May occur on findComponent() when view has changed by for example a successful navigation.
				// TODO: check for a way to detect this beforehand so that the whole loop can be skipped.
			}
		}

		return null;
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
	 * Returns the value of the <code>label</code> attribute associated with the given UI component if any, else
	 * the client ID. It never returns null.
	 * @param input The UI input component for which the label is to be retrieved.
	 * @return The value of the <code>label</code> attribute associated with the given UI component if any, else
	 * the client ID.
	 */
	public static String getLabel(UIComponent input) {
		String label = getOptionalLabel(input);
		return (label != null) ? label : input.getClientId();
	}

	/**
	 * Returns the value of the <code>label</code> attribute associated with the given UI component if any, else
	 * null.
	 * @param input The UI input component for which the label is to be retrieved.
	 * @return The value of the <code>label</code> attribute associated with the given UI component if any, else
	 * null.
	 */
	public static String getOptionalLabel(UIComponent input) {
		Object label = input.getAttributes().get("label");

		if (Utils.isEmpty(label)) {
			ValueExpression labelExpression = input.getValueExpression("label");

			if (labelExpression != null) {
				label = labelExpression.getValue(FacesContext.getCurrentInstance().getELContext());
			}
		}

		return (label != null) ? label.toString() : null;
	}

	/**
	 * Returns the value of the given editable value holder component without the need to know if the given component
	 * has already been converted/validated or not. Note that it thus returns the unconverted submitted string value
	 * when the conversion/validation hasn't been taken place for the given component and it returns the converted
	 * object value -if applicable- when conversion/validation has been taken place for the given component.
	 * @param component The editable value holder component to obtain the value for.
	 * @return The value of the given editable value holder component.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getValue(EditableValueHolder component) {
		Object submittedValue = component.getSubmittedValue();
		return (T) ((submittedValue != null) ? submittedValue : component.getLocalValue());
	}

	/**
	 * Returns the value of the given input component whereby any unconverted submitted string value will immediately
	 * be converted/validated as this method is called. This method thus always returns the converted/validated value.
	 * @param input The input component to obtain the converted/validated value for.
	 * @return The converted/validated value of the given input component.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @since 1.2
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getImmediateValue(UIInput input) {
		if (input.isValid() && input.getSubmittedValue() != null) {
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
		return !Utils.isEmpty(component.getSubmittedValue());
	}

	/**
	 * Returns whether the given component has invoked the form submit. In non-ajax requests, that can only be an
	 * {@link UICommand} component. In ajax requests, that can also be among others an {@link UIInput} component.
	 * @param component The component to be checked.
	 * @return <code>true</code> if the given component has invoked the form submit.
	 * @since 1.3
	 */
	public static boolean hasInvokedSubmit(UIComponent component) {
		FacesContext context = FacesContext.getCurrentInstance();

		if (context.isPostback()) {
			String clientId = component.getClientId(context);
			Map<String, String> params = context.getExternalContext().getRequestParameterMap();

			if (context.getPartialViewContext().isAjaxRequest()) {
				return clientId.equals(params.get("javax.faces.source"));
			}
			else {
				return component instanceof UICommand && params.get(clientId) != null;
			}
		}
		else {
			return false;
		}
	}

	// Validation -----------------------------------------------------------------------------------------------------

	/**
	 * Validate if the given component has a parent of the given parent type.
	 * @param component The component to be validated.
	 * @param parentType The parent type to be checked.
	 * @throws IllegalArgumentException When the given component doesn't have any parent of the given type.
	 */
	public static <T extends UIComponent> void validateHasParent(UIComponent component, Class<T> parentType)
		throws IllegalArgumentException
	{
		if (getClosestParent(component, parentType) == null) {
			throw new IllegalArgumentException(String.format(
				ERROR_INVALID_PARENT, component.getClass().getSimpleName(), parentType));
		}
	}

	/**
	 * Validate if the given component has a direct parent of the given parent type.
	 * @param component The component to be validated.
	 * @param parentType The parent type to be checked.
	 * @throws IllegalArgumentException When the given component doesn't have a direct parent of the given type.
	 */
	public static <T extends UIComponent> void validateHasDirectParent(UIComponent component, Class<T> parentType)
		throws IllegalArgumentException
	{
		if (!parentType.isInstance(component.getParent())) {
			throw new IllegalArgumentException(String.format(
				ERROR_INVALID_DIRECT_PARENT, component.getClass().getSimpleName(), parentType));
		}
	}

	/**
	 * Validate if the given component has no children.
	 * @param component The component to be validated.
	 * @throws IllegalArgumentException When the given component has any children.
	 */
	public static void validateHasNoChildren(UIComponent component) throws IllegalArgumentException {
		if (component.getChildCount() > 0) {
			StringBuilder childClassNames = new StringBuilder();

			for (UIComponent child : component.getChildren()) {
				if (childClassNames.length() > 0) {
					childClassNames.append(", ");
				}

				childClassNames.append(child.getClass().getName());
			}

			throw new IllegalArgumentException(String.format(
				ERROR_CHILDREN_DISALLOWED, component.getClass().getSimpleName(), childClassNames));
		}
	}

}