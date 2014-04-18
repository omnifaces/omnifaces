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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.Resource;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.NamingContainer;
import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.component.UIForm;
import javax.faces.component.UIInput;
import javax.faces.component.UIPanel;
import javax.faces.component.UIViewRoot;
import javax.faces.component.behavior.AjaxBehavior;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitHint;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.event.AjaxBehaviorListener;
import javax.faces.event.MethodExpressionActionListener;
import javax.faces.view.facelets.FaceletContext;

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

	/**
	 * Returns whether the given UI component and all of its parents is rendered. This thus not only checks the
	 * component's own <code>rendered</code> attribute, but also of all of its parents.
	 * @param component The component to be checked.
	 * @return <code>true</code> if the given UI component and all of its parents is rendered.
	 * @since 1.8
	 */
	public static boolean isRendered(UIComponent component) {
		for (UIComponent current = component; current.getParent() != null; current = current.getParent()) {
			if (!current.isRendered()) {
				return false;
			}
		}

		return true;
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

	// Manipulation ---------------------------------------------------------------------------------------------------

	/**
	 * Include the Facelet file at the given (relative) path as child of the given UI component parent. This has the
	 * same effect as using <code>&lt;ui:include&gt;</code>. The path is relative to the current view ID and absolute
	 * to the webcontent root.
	 * @param parent The parent component to include the Facelet file in.
	 * @param path The (relative) path to the Facelet file.
	 * @throws IOException Whenever something fails at I/O level. The caller should preferably not catch it, but just
	 * redeclare it in the action method. The servletcontainer will handle it.
	 * @see FaceletContext#includeFacelet(UIComponent, String)
	 * @since 1.5
	 */
	public static void includeFacelet(UIComponent parent, String path) throws IOException {
		Faces.getFaceletContext().includeFacelet(parent, path);
	}

	/**
	 * Create and include the composite component of the given library and resource name as child of the given UI
	 * component parent and return the created composite component.
	 * This has the same effect as using <code>&lt;my:resourceName&gt;</code>. The given component ID must be unique
	 * relative to the current naming container parent and is mandatory for functioning of input components inside the
	 * composite, if any.
	 * @param parent The parent component to include the composite component in.
	 * @param libraryName The library name of the composite component.
	 * @param resourceName The resource name of the composite component.
	 * @param id The component ID of the composite component.
	 * @return The created composite component, which can if necessary be further used to set custom attributes or
	 * value expressions on it.
	 * @since 1.5
	 */
	public static UIComponent includeCompositeComponent(UIComponent parent, String libraryName, String resourceName, String id) {
		FacesContext context = Faces.getContext();
		Application application = context.getApplication();
		FaceletContext faceletContext = FacesLocal.getFaceletContext(context);

		// This basically creates <ui:component> based on <composite:interface>.
		Resource resource = application.getResourceHandler().createResource(resourceName, libraryName);
		UIComponent composite = application.createComponent(context, resource);
		composite.setId(id); // Mandatory for the case composite is part of UIForm! Otherwise JSF can't find inputs.

		// This basically creates <composite:implementation>.
		UIComponent implementation = application.createComponent(UIPanel.COMPONENT_TYPE);
		implementation.setRendererType("javax.faces.Group");
		composite.getFacets().put(UIComponent.COMPOSITE_FACET_NAME, implementation);

		// Now include the composite component file in the given parent.
		parent.getChildren().add(composite);
		parent.pushComponentToEL(context, composite); // This makes #{cc} available.
		try {
			faceletContext.includeFacelet(implementation, resource.getURL());
		}
		catch (IOException e) {
			throw new FacesException(e);
		}
		finally {
			parent.popComponentFromEL(context);
		}

		return composite;
	}

	// Forms ----------------------------------------------------------------------------------------------------------

	/**
	 * Returns the currently submitted UI form component, or <code>null</code> if there is none, which may happen when
	 * the current request is not a postback request at all, or when the view has been changed by for example a
	 * successful navigation. If the latter is the case, you'd better invoke this method before navigation.
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

			UIComponent component = findComponentIgnoringIAE(viewRoot, stripIterationIndexFromClientId(name));

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

		return null;
	}

	/**
	 * Returns the currently invoked UI command component, or <code>null</code> if there is none, which may happen when
	 * the current request is not a postback request at all, or when the view has been changed by for example a
	 * successful navigation. If the latter is the case, you'd better invoke this method before navigation.
	 * @return The currently invoked UI command component.
	 * @since 1.6
	 */
	public static UICommand getCurrentCommand() {
		FacesContext facesContext = FacesContext.getCurrentInstance();

		if (!facesContext.isPostback()) {
			return null;
		}

		UIViewRoot viewRoot = facesContext.getViewRoot();
	    Map<String, String> params = facesContext.getExternalContext().getRequestParameterMap();

	    if (facesContext.getPartialViewContext().isAjaxRequest()) {
	    	String source = params.get("javax.faces.source");

	    	if (source != null) {
    	        UIComponent component = findComponentIgnoringIAE(viewRoot, stripIterationIndexFromClientId(source));

				if (component instanceof UICommand) {
					return (UICommand) component;
				}
	    	}
	    }

	    for (String name : params.keySet()) {
			if (name.startsWith("javax.faces.")) {
				continue; // Quick skip.
			}

	        UIComponent component = findComponentIgnoringIAE(viewRoot, stripIterationIndexFromClientId(name));

			if (component instanceof UICommand) {
				return (UICommand) component;
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

	// Expressions ----------------------------------------------------------------------------------------------------

	/**
	 * Create an editable value expression based on the given EL expression and the given type.
	 * @param expression The EL expression to represent an editable value expression.
	 * @param type The type of the property referenced by the value expression.
	 * @return The created editable value expression, ready to be used as
	 * {@link UIComponent#setValueExpression(String, ValueExpression)}.
	 */
	public static ValueExpression createValueExpression(String expression, Class<?> type) {
		FacesContext context = FacesContext.getCurrentInstance();
		return context.getApplication().getExpressionFactory().createValueExpression(
			context.getELContext(), expression, type);
	}

	/**
	 * <p>Create a method expression based on the given EL expression, the given return type and the given parameter types,
	 * if any. As an example, the following action method examples,
	 * <ul>
	 * <li><code>public void submit1()</code></li>
	 * <li><code>public String submit2()</code></li>
	 * <li><code>public void submit3(String argument)</code></li>
	 * <li><code>public String submit4(String argument)</code></li>
	 * <li><code>public void submit5(String argument1, Long argument2)</code></li>
	 * <li><code>public String submit6(Long argument1, String argument2)</code></li>
	 * </ul>
	 * <p>can be created as follows:
	 * <ul>
	 * <li><code>createMethodExpression("#{bean.submit1}", Void.class);</code></li>
	 * <li><code>createMethodExpression("#{bean.submit2}", String.class);</code></li>
	 * <li><code>createMethodExpression("#{bean.submit3('foo')}", Void.class, String.class);</code></li>
	 * <li><code>createMethodExpression("#{bean.submit4('foo')}", String.class, String.class);</code></li>
	 * <li><code>createMethodExpression("#{bean.submit5('foo', 0)}", Void.class, String.class, Long.class);</code></li>
	 * <li><code>createMethodExpression("#{bean.submit6(0, 'foo')}", String.class, Long.class, String.class);</code></li>
	 * </ul>
	 * @param expression The EL expression to create a method expression for.
	 * @param returnType The return type of the method expression. Can be <code>null</code> if you don't care about the
	 * return type (e.g. <code>void</code> or <code>String</code>).
	 * @param parameterTypes The parameter types of the method expression.
	 * @return The created method expression, ready to be used as
	 * {@link UICommand#setActionExpression(MethodExpression)}.
	 */
	public static MethodExpression createMethodExpression
		(String expression, Class<?> returnType, Class<?>... parameterTypes)
	{
		FacesContext context = FacesContext.getCurrentInstance();
		return context.getApplication().getExpressionFactory().createMethodExpression(
			context.getELContext(), expression, returnType, parameterTypes);
	}

	/**
	 * <p>Create a void method expression based on the given EL expression and the given parameter types, if any.
	 * As an example, the following action method examples,
	 * <ul>
	 * <li><code>public void submit1()</code></li>
	 * <li><code>public void submit3(String argument)</code></li>
	 * <li><code>public void submit5(String argument1, Long argument2)</code></li>
	 * </ul>
	 * <p>can be created as follows:
	 * <ul>
	 * <li><code>createVoidMethodExpression("#{bean.submit1}");</code></li>
	 * <li><code>createVoidMethodExpression("#{bean.submit3('foo')}", String.class);</code></li>
	 * <li><code>createVoidMethodExpression("#{bean.submit5('foo', 0)}", String.class, Long.class);</code></li>
	 * </ul>
	 * @param expression The EL expression to create a void method expression for.
	 * @param parameterTypes The parameter types of the void method expression.
	 * @return The created void method expression, ready to be used as
	 * {@link UICommand#setActionExpression(MethodExpression)}.
	 */
	public static MethodExpression createVoidMethodExpression(String expression, Class<?>... parameterTypes) {
		return createMethodExpression(expression, Void.class, parameterTypes);
	}

	/**
	 * Create an action listener method expression based on the given EL expression. The target method must take an
	 * {@link ActionEvent} as argument.
	 * As an example, the following action method example,
	 * <ul>
	 * <li><code>public void actionListener(ActionEvent event)</code></li>
	 * </ul>
	 * <p>can be created as follows:
	 * <ul>
	 * <li><code>createActionListenerMethodExpression("#{bean.actionListener}");</code></li>
	 * </ul>
	 * @param expression The EL expression to create an action listener method expression for.
	 * @return The created action listener method expression, ready to be used as
	 * {@link UICommand#addActionListener(javax.faces.event.ActionListener)}.
	 */
	public static MethodExpressionActionListener createActionListenerMethodExpression(String expression) {
		return new MethodExpressionActionListener(createVoidMethodExpression(expression, ActionEvent.class));
	}

	/**
	 * Create an ajax behavior which should invoke an ajax listener method expression based on the given EL expression.
	 * The target method must take an {@link AjaxBehaviorEvent} as argument.
	 * As an example, the following ajax listener example,
	 * <ul>
	 * <li><code>public void ajaxListener(AjaxBehaviorEvent event)</code></li>
	 * </ul>
	 * <p>can be created as follows:
	 * <ul>
	 * <li><code>createAjaxBehavior("#{bean.ajaxListener}");</code></li>
	 * </ul>
	 * <p>Note that this is essentially the programmatic equivalent of <code>&lt;f:ajax&gt;</code>. So if you intented
	 * to create for example a <code>&lt;p:ajax&gt;</code> programmatically, then don't use this method.
	 * @param expression The EL expression to be invoked when the created ajax behavior is processed.
	 * @return The created ajax behavior, ready to be used as
	 * {@link UIComponentBase#addClientBehavior(String, ClientBehavior)} whereby the string argument represents the
	 * client event name, such as "action", "valueChange", "click", "blur", etc.
	 */
	public static AjaxBehavior createAjaxBehavior(String expression) {
		FacesContext context = FacesContext.getCurrentInstance();
		AjaxBehavior behavior = (AjaxBehavior) context.getApplication().createBehavior(AjaxBehavior.BEHAVIOR_ID);
		final MethodExpression method = createVoidMethodExpression(expression, AjaxBehaviorEvent.class);
		behavior.addAjaxBehaviorListener(new AjaxBehaviorListener() {
			@Override
			public void processAjaxBehavior(AjaxBehaviorEvent event) throws AbortProcessingException {
				method.invoke(FacesContext.getCurrentInstance().getELContext(), new Object[] { event });
			}
		});
		return behavior;
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

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Strip UIData/UIRepeat iteration index in pattern <code>:[0-9+]:</code> from given component client ID.
	 */
	private static String stripIterationIndexFromClientId(String clientId) {
		return clientId.replaceAll(":[0-9]+:", ":");
	}

	/**
	 * Use {@link UIViewRoot#findComponent(String)} and ignore the potential {@link IllegalArgumentException} by
	 * returning null instead.
	 */
	private static UIComponent findComponentIgnoringIAE(UIViewRoot viewRoot, String clientId) {
		try {
			return viewRoot.findComponent(clientId);
		}
		catch (IllegalArgumentException ignore) {
			// May occur when view has changed by for example a successful navigation.
			return null;
		}
	}

}