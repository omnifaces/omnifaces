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

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.regex.Pattern.quote;
import static javax.faces.component.visit.VisitContext.createVisitContext;
import static javax.faces.component.visit.VisitResult.ACCEPT;
import static org.omnifaces.util.Faces.getContext;
import static org.omnifaces.util.Faces.getELContext;
import static org.omnifaces.util.Faces.getFaceletContext;
import static org.omnifaces.util.Faces.getRequestParameter;
import static org.omnifaces.util.Faces.getViewRoot;
import static org.omnifaces.util.Faces.setContext;
import static org.omnifaces.util.FacesLocal.getRenderKit;
import static org.omnifaces.util.FacesLocal.getRequestQueryStringMap;
import static org.omnifaces.util.FacesLocal.getViewParameterMap;
import static org.omnifaces.util.FacesLocal.normalizeViewId;
import static org.omnifaces.util.Renderers.RENDERER_TYPE_JS;
import static org.omnifaces.util.Utils.isEmpty;
import static org.omnifaces.util.Utils.isOneInstanceOf;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.application.ViewHandler;
import javax.faces.component.ActionSource2;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.NamingContainer;
import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.component.UIForm;
import javax.faces.component.UIInput;
import javax.faces.component.UINamingContainer;
import javax.faces.component.UIOutput;
import javax.faces.component.UIParameter;
import javax.faces.component.UIViewRoot;
import javax.faces.component.behavior.AjaxBehavior;
import javax.faces.component.behavior.BehaviorBase;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitHint;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextWrapper;
import javax.faces.context.ResponseWriter;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.event.AjaxBehaviorListener;
import javax.faces.event.BehaviorListener;
import javax.faces.event.MethodExpressionActionListener;
import javax.faces.render.RenderKit;
import javax.faces.view.ViewDeclarationLanguage;
import javax.faces.view.facelets.FaceletContext;

import org.omnifaces.component.ParamHolder;
import org.omnifaces.component.SimpleParam;

/**
 * <p>
 * Collection of utility methods for the JSF API with respect to working with {@link UIComponent}. There are several
 * traversal/lookup methods, there are several {@link UIForm} and {@link UIInput} related methods which makes it easier
 * to deal with forms and inputs.
 *
 * <h3>Usage</h3>
 * <p>
 * Some examples:
 * <pre>
 * // Get closest parent of given type.
 * UIForm form = Components.getClosestParent(someUIInputComponent, UIForm.class);
 * </pre>
 * <pre>
 * // Get currently submitted form.
 * UIForm form = Components.getCurrentForm();
 * </pre>
 * <pre>
 * // Get currently invoked command, useful for logging actions in a phase listener.
 * UICommand command = Components.getCurrentCommand();
 * </pre>
 * <pre>
 * // Get the label of the given UIInput component as JSF uses for validation messages.
 * String label = Components.getLabel(someUIInputComponent);
 * </pre>
 * <pre>
 * // Inside decode() and/or encode() of some custom component, validate if it has no children.
 * Components.validateHasNoChildren(this);
 * </pre>
 * <pre>
 * // Programmatically include composite component.
 * Components.includeCompositeComponent(someParentComponent, libraryName, tagName, id);
 * </pre>
 * <pre>
 * // Programmatically create value and action expressions.
 * UICommand command = new HtmlCommandButton();
 * command.setId("foo");
 * command.setValue(Components.createValueExpression("#{bundle['button.foo']}", String.class));
 * command.addClientBehavior("action", Components.createAjaxBehavior("#{bean.ajaxListener}"));
 * command.addActionListener(Components.createActionListenerMethodExpression("#{bean.actionListener}"));
 * command.setActionExpression(Components.createVoidMethodExpression("#{bean.action}"));
 * </pre>
 * <pre>
 * // Programmatically capture HTML output of a given view.
 * String mailHtml = Components.encodeHtml(Components.buildView("/WEB-INF/mail-template.xhtml"));
 * </pre>
 * <pre>
 * // Collecting all queued actions and action listeners as method expression strings in a logging phase listener.
 * List&lt;String&gt; actions = Components.getActionExpressionsAndListeners(Components.getCurrentActionSource());
 * </pre>
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
		return UIComponent.getCurrentComponent(getContext());
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
	 * @param <C> The expected component type.
	 * @param clientId The client ID search expression.
	 * @return The UI component matching the given client ID search expression.
	 * @throws ClassCastException When <code>C</code> is of wrong type.
	 * @see UIComponent#findComponent(String)
	 */
	@SuppressWarnings("unchecked")
	public static <C extends UIComponent> C findComponent(String clientId) {
		return (C) getViewRoot().findComponent(clientId);
	}

	/**
	 * Returns the UI component matching the given client ID search expression relative to the point
	 * in the component tree of the given component. For this search both parents and children are
	 * consulted, increasingly moving further away from the given component. Parents are consulted
	 * first, then children.
	 *
	 * @param <C> The expected component type.
	 * @param component the component from which the relative search is started.
	 * @param clientId The client ID search expression.
	 * @return The UI component matching the given client ID search expression.
	 * @throws ClassCastException When <code>C</code> is of wrong type.
	 * @see UIComponent#findComponent(String)
	 */
	@SuppressWarnings("unchecked")
	public static <C extends UIComponent> C findComponentRelatively(UIComponent component, String clientId) {

		if (isEmpty(clientId)) {
			return null;
		}

		// Search first in the naming container parents of the given component
		UIComponent result = findComponentInParents(component, clientId);

		if (result == null) {
			// If not in the parents, search from the root
			result = findComponentInChildren(getViewRoot(), clientId);
		}

		return (C) result;
	}

	/**
	 * Returns the UI component matching the given client ID search expression relative to the point
	 * in the component tree of the given component, searching only in its parents.
	 *
	 * @param <C> The expected component type.
	 * @param component the component from which the relative search is started.
	 * @param clientId The client ID search expression.
	 * @return The UI component matching the given client ID search expression.
	 * @throws ClassCastException When <code>C</code> is of wrong type.
	 * @see UIComponent#findComponent(String)
	 */
	@SuppressWarnings("unchecked")
	public static <C extends UIComponent> C findComponentInParents(UIComponent component, String clientId) {

		if (isEmpty(clientId)) {
			return null;
		}

		for (UIComponent parent = component; parent != null; parent = parent.getParent()) {

			UIComponent result = null;
			if (parent instanceof NamingContainer) {
				try {
					result = parent.findComponent(clientId);
				} catch (IllegalArgumentException e) {
					continue;
				}
			}

			if (result != null) {
				return (C) result;
			}
		}

		return null;
	}

	/**
	 * Returns the UI component matching the given client ID search expression relative to the point
	 * in the component tree of the given component, searching only in its children.
	 *
	 * @param <C> The expected component type.
	 * @param component the component from which the relative search is started.
	 * @param clientId The client ID search expression.
	 * @return The UI component matching the given client ID search expression.
	 * @throws ClassCastException When <code>C</code> is of wrong type.
	 * @see UIComponent#findComponent(String)
	 */
	@SuppressWarnings("unchecked")
	public static <C extends UIComponent> C findComponentInChildren(UIComponent component, String clientId) {

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
				return (C) result;
			}
		}

		return null;
	}

	/**
	 * Returns a list of UI components matching the given type in children of the given component.
	 * @param <C> The generic component type.
	 * @param component The component to search in its children for UI components matching the given type.
	 * @param type The type of the UI components to be searched in children of the given component.
	 * @return A list of UI components matching the given type in children of the given component.
	 */
	public static <C extends UIComponent> List<C> findComponentsInChildren(UIComponent component, Class<C> type) {
		List<C> components = new ArrayList<>();
		findComponentsInChildren(component, type, components);
		return components;
	}

	/**
	 * Helper method for {@link #findComponentsInChildren(UIComponent, Class)} utilizing tail recursion.
	 */
	@SuppressWarnings("unchecked")
	private static <C extends UIComponent> void findComponentsInChildren(UIComponent component, Class<C> type, List<C> matches) {
		for (UIComponent child : component.getChildren()) {
			if (type.isInstance(child)) {
				matches.add((C) child);
			}

			findComponentsInChildren(child, type, matches);
		}
	}

	/**
	 * Returns from the given component the closest parent of the given parent type, or <code>null</code> if none
	 * is found.
	 * @param <C> The generic component type.
	 * @param component The component to return the closest parent of the given parent type for.
	 * @param parentType The parent type.
	 * @return From the given component the closest parent of the given parent type, or <code>null</code> if none
	 * is found.
	 */
	public static <C extends UIComponent> C getClosestParent(UIComponent component, Class<C> parentType) {
		UIComponent parent = component.getParent();

		while (parent != null && !parentType.isInstance(parent)) {
			parent = parent.getParent();
		}

		return parentType.cast(parent);
	}

	// Iteration / Visiting -------------------------------------------------------------------------------------------

	/**
	 * Invokes an operation on every component in the component tree.
	 * <p>
	 * This is a simplified version of regular component visiting that uses the builder pattern to provide the various
	 * optional parameters. Includes supports for only visiting components of a certain class type and two
	 * simplified functional interfaces / lambdas.
	 * <p>
	 * See {@link UIComponent#visitTree(VisitContext, VisitCallback)}
	 *
	 * @return A new instance of {@link ForEach}.
	 * @since 2.0
	 */
	public static ForEach forEachComponent() {
		return new ForEach();
	}

	/**
	 * Invokes an operation on every component in the component tree.
	 * <p>
	 * This is a simplified version of regular component visiting that uses the builder pattern to provide the various
	 * optional parameters. Includes supports for only visiting components of a certain class type and two
	 * simplified functional interfaces / lambdas.
	 * <p>
	 * See {@link UIComponent#visitTree(VisitContext, VisitCallback)}
	 *
	 * @param facesContext the faces context used for tree visiting
	 * @return A new instance of {@link ForEach}, using the given faces context.
	 * @since 2.0
	 */
	public static ForEach forEachComponent(FacesContext facesContext) {
		return new ForEach(facesContext);
	}

	/**
	 * Builder class used to collect a number of query parameters for a visit (for each) of components in the JSF
	 * component tree. The chain of collecting parameters is terminated by calling one of the invoke methods.
	 *
	 * @since 2.0
	 * @author Arjan Tijms
	 *
	 */
	public static class ForEach {

		private FacesContext facesContext;
		private UIComponent root;
		private Collection<String> ids;
		private Set<VisitHint> hints;
		private Class<?>[] types;

		public ForEach() {
			facesContext = Faces.getContext();
		}

		public ForEach(FacesContext facesContext) {
			this.facesContext = facesContext;
		}

		/**
		 * The root component where tree visiting starts
		 *
		 * @param root the root component where tree visiting starts
		 * @return the intermediate builder object to continue the builder chain
		 */
		public ForEach fromRoot(UIComponent root) {
			this.root = root;
			return this;
		}

		/**
		 * The IDs of the components that are visited
		 *
		 * @param ids the IDs of the components that are visited
		 * @return the intermediate builder object to continue the builder chain
		 */
		public ForEach havingIds(Collection<String> ids) {
			this.ids = ids;
			return this;
		}

		/**
		 * The IDs of the components that are to be visited
		 *
		 * @param ids the IDs of the components that are to be visited
		 * @return the intermediate builder object to continue the builder chain
		 */
		public ForEach havingIds(String... ids) {
			this.ids = asList(ids);
			return this;
		}

		/**
		 * The VisitHints that are used for the visit.
		 *
		 * @param hints the VisitHints that are used for the visit.
		 * @return the intermediate builder object to continue the builder chain
		 */
		public ForEach withHints(Set<VisitHint> hints) {
			this.hints = hints;
			return this;
		}

		/**
		 * The VisitHints that are used for the visit.
		 *
		 * @param hints the VisitHints that are used for the visit.
		 * @return the intermediate builder object to continue the builder chain
		 */
		public ForEach withHints(VisitHint... hints) {

			if (hints.length > 0) {
				EnumSet<VisitHint> hintsSet = EnumSet.noneOf(hints[0].getDeclaringClass());
				for (VisitHint hint : hints) {
					hintsSet.add(hint);
				}

				this.hints = hintsSet;

			}
			return this;
		}

		/**
		 * The types of the components that are to be visited
		 *
		 * @param types the types of the components that are to be visited
		 * @return the intermediate builder object to continue the builder chain
		 */
		@SafeVarargs
		public final ForEach ofTypes(Class<?>... types) {
			this.types = types;
			return this;
		}

		/**
		 * Invokes the given operation on the components as specified by the
		 * query parameters set via this builder.
		 *
		 * @param operation the operation to invoke on each component
		 */
		public void invoke(final Callback.WithArgument<UIComponent> operation) {
			invoke(new VisitCallback() {
				@Override
				public VisitResult visit(VisitContext context, UIComponent target) {
					operation.invoke(target);
					return ACCEPT;
				}
			});
		}

		/**
		 * Invokes the given operation on the components as specified by the
		 * query parameters set via this builder.
		 *
		 * @param operation the operation to invoke on each component
		 */
		public void invoke(final Callback.ReturningWithArgument<VisitResult, UIComponent> operation) {
			invoke(new VisitCallback() {
				@Override
				public VisitResult visit(VisitContext context, UIComponent target) {
					return operation.invoke(target);
				}
			});
		}

		/**
		 * Invokes the given operation on the components as specified by the
		 * query parameters set via this builder.
		 *
		 * @param operation the operation to invoke on each component
		 */
		public void invoke(final VisitCallback operation) {
			VisitCallback callback = operation;
			if (types != null) {
				callback = new TypesVisitCallback(types, callback);
			}

			getRoot().visitTree(createVisitContext(getContext(), getIds(), getHints()), callback);
		}

		protected FacesContext getFacesContext() {
			return facesContext;
		}

		protected UIComponent getRoot() {
			return root != null ? root : getFacesContext().getViewRoot();
		}

		protected Collection<String> getIds() {
			return ids;
		}

		protected Set<VisitHint> getHints() {
			return hints;
		}

		private static class TypesVisitCallback implements VisitCallback {

			private Class<?>[] types;
			private VisitCallback next;

			public TypesVisitCallback(Class<?>[] types, VisitCallback next) {
				this.types = types;
				this.next = next;
			}

			@Override
			public VisitResult visit(VisitContext context, UIComponent target) {
				if (isOneInstanceOf(target.getClass(), types)) {
					return next.visit(context, target);
				}
				return ACCEPT;
			}
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
		getFaceletContext().includeFacelet(parent, path);
	}

	/**
	 * Create and include the composite component of the given library and resource name as child of the given UI
	 * component parent and return the created composite component.
	 * This has the same effect as using <code>xmlns:my="http://xmlns.jcp.org/jsf/composite/libraryName</code> and
	 * <code>&lt;my:tagName&gt;</code>. The given component ID must be unique relative to the current naming
	 * container parent and is mandatory for functioning of input components inside the composite, if any.
	 * @param parent The parent component to include the composite component in.
	 * @param libraryName The library name of the composite component (path after "http://xmlns.jcp.org/jsf/composite/").
	 * @param tagName The tag name of the composite component.
	 * @param id The component ID of the composite component.
	 * @return The created composite component, which can if necessary be used to set more custom attributes on it.
	 * @since 1.5
	 */
	public static UIComponent includeCompositeComponent(UIComponent parent, String libraryName, String tagName, String id) {
		return includeCompositeComponent(parent, libraryName, tagName, id, null);
	}

	/**
	 * Create and include the composite component of the given library and resource name as child of the given UI
	 * component parent, set the given attributes on it and return the created composite component.
	 * This has the same effect as using <code>xmlns:my="http://xmlns.jcp.org/jsf/composite/libraryName</code> and
	 * <code>&lt;my:tagName&gt;</code>. The given component ID must be unique relative to the current naming
	 * container parent and is mandatory for functioning of input components inside the composite, if any.
	 * <p>
	 * The attribute values must represent literal values or literal EL expressions, exactly like as you would declare
	 * in the view file. E.g.
	 * <pre>
	 * attributes.put("foo", "#{bean.foo}");
	 * attributes.put("bar", "true");
	 * attributes.put("baz", "#{bean.baz(" + someId + ")}");
	 * </pre>
	 * @param parent The parent component to include the composite component in.
	 * @param libraryName The library name of the composite component (path after "http://xmlns.jcp.org/jsf/composite/").
	 * @param tagName The tag name of the composite component.
	 * @param id The component ID of the composite component.
	 * @param attributes The attributes to be set on the composite component.
	 * @return The created composite component, which can if necessary be used to set more custom attributes on it.
	 * @since 2.2
	 */
	public static UIComponent includeCompositeComponent(UIComponent parent, String libraryName, String tagName, String id, Map<String, String> attributes) {
	    String taglibURI = "http://xmlns.jcp.org/jsf/composite/" + libraryName;
	    Map<String, Object> attrs = (attributes == null) ? null : new HashMap<String, Object>(attributes);

	    FacesContext context = FacesContext.getCurrentInstance();
	    UIComponent composite = context.getApplication().getViewHandler()
	    	.getViewDeclarationLanguage(context, context.getViewRoot().getViewId())
	        .createComponent(context, taglibURI, tagName, attrs);
	    composite.setId(id);
	    parent.getChildren().add(composite);
		return composite;
	}

	/**
	 * Add given JavaScript code as inline script to end of body of the current view.
	 * Note: this doesn't have any effect during ajax postbacks. Rather use {@link Ajax#oncomplete(String...)} instead.
	 * @param script JavaScript code to be added as inline script to end of body of the current view.
	 * @return The created script component.
	 * @since 2.2
	 */
	public static UIComponent addScriptToBody(String script) {
		UIOutput outputScript = createScriptResource();
		UIOutput content = new UIOutput();
		content.setValue(script);
		outputScript.getChildren().add(content);
		return addComponentResource(outputScript, "body");
	}

	/**
	 * Add given JavaScript resource to end of body of the current view.
	 * Note: this doesn't have any effect during non-@all ajax postbacks.
	 * @param libraryName Library name of the JavaScript resource.
	 * @param resourceName Resource name of the JavaScript resource.
	 * @return The created script component resource.
	 * @since 2.2
	 */
	public static UIComponent addScriptResourceToBody(String libraryName, String resourceName) {
		return addScriptResource(libraryName, resourceName, "body");
	}

	/**
	 * Add given JavaScript resource to end of head of the current view.
	 * Note: this doesn't have any effect during non-@all ajax postbacks, nor during render response phase when the
	 * <code>&lt;h:head&gt;</code> has already been encoded. During render response, rather use
	 * {@link #addScriptResourceToBody(String, String)} instead.
	 * @param libraryName Library name of the JavaScript resource.
	 * @param resourceName Resource name of the JavaScript resource.
	 * @return The created script component resource.
	 * @since 2.2
	 */
	public static UIComponent addScriptResourceToHead(String libraryName, String resourceName) {
		return addScriptResource(libraryName, resourceName, "head");
	}

	private static UIOutput createScriptResource() {
		UIOutput outputScript = new UIOutput();
		outputScript.setRendererType(RENDERER_TYPE_JS);
		return outputScript;
	}

	private static UIComponent addScriptResource(String libraryName, String resourceName, String target) {
		FacesContext context = FacesContext.getCurrentInstance();
		String id = libraryName + "_" + resourceName.replaceAll("\\W+", "_");

		for (UIComponent existingResource : context.getViewRoot().getComponentResources(context, target)) {
			if (id.equals(existingResource.getId())) {
				return existingResource;
			}
		}

		UIOutput outputScript = createScriptResource();
		outputScript.setId(id);

		if (libraryName != null) {
			outputScript.getAttributes().put("library", libraryName);
		}

		outputScript.getAttributes().put("name", resourceName);
		return addComponentResource(outputScript, target);
	}

	private static UIComponent addComponentResource(UIComponent resource, String target) {
		FacesContext context = FacesContext.getCurrentInstance();
		context.getViewRoot().addComponentResource(context, resource, target);
		return resource;
	}

	// Building / rendering -------------------------------------------------------------------------------------------

	/**
	 * Creates and builds a local view for the given view ID independently from the current view.
	 * @param viewId The ID of the view which needs to be created and built.
	 * @return A fully populated component tree of the given view ID.
	 * @throws IOException Whenever something fails at I/O level. This can happen when the given view ID is unavailable or malformed.
	 * @since 2.2
	 * @see ViewHandler#createView(FacesContext, String)
	 * @see ViewDeclarationLanguage#buildView(FacesContext, UIViewRoot)
	 */
	public static UIViewRoot buildView(String viewId) throws IOException {
		FacesContext context = FacesContext.getCurrentInstance();
		String normalizedViewId = normalizeViewId(context, viewId);
		ViewHandler viewHandler = context.getApplication().getViewHandler();
		UIViewRoot view = viewHandler.createView(context, normalizedViewId);
		FacesContext temporaryContext = new TemporaryViewFacesContext(context, view);

		try {
			setContext(temporaryContext);
			viewHandler.getViewDeclarationLanguage(temporaryContext, normalizedViewId).buildView(temporaryContext, view);
		}
		finally {
			setContext(context);
		}

		return view;
	}

	/**
	 * Encodes the given component locally as HTML, with UTF-8 character encoding, independently from the current view.
	 * The current implementation, however, uses the current faces context. The same managed beans as in the current
	 * faces context will be available as well, including request scoped ones. But, depending on the nature of the
	 * provided component, the state of the faces context may be affected because the attributes of the context,
	 * request, view, session and application scope could be (in)directly manipulated during the encode. This may or may
	 * not have the desired effect. If the given view does not have any component resources, JSF forms, dynamically
	 * added components, component event listeners, then it should mostly be safe.
	 * In other words, use this at most for "simple templates" only, e.g. a HTML based mail template, which usually
	 * already doesn't have a HTML head nor body.
	 * @param component The component to capture HTML output for.
	 * @return The encoded HTML output of the given component.
	 * @throws IOException Whenever something fails at I/O level. This would be quite unexpected as it happens locally.
	 * @since 2.2
	 * @see UIComponent#encodeAll(FacesContext)
	 */
	public static String encodeHtml(UIComponent component) throws IOException {
		FacesContext context = FacesContext.getCurrentInstance();
		ResponseWriter originalWriter = context.getResponseWriter();
		StringWriter output = new StringWriter();
		context.setResponseWriter(getRenderKit(context).createResponseWriter(output, "text/html", "UTF-8"));

		try {
			component.encodeAll(context);
		}
		finally {
			if (originalWriter != null) {
				context.setResponseWriter(originalWriter);
			}
		}

		return output.toString();
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
		FacesContext context = FacesContext.getCurrentInstance();

		if (!context.isPostback()) {
			return null;
		}

		UIViewRoot viewRoot = context.getViewRoot();

		// The initial implementation has visited the tree for UIForm components which returns true on isSubmitted().
		// But with testing it turns out to return false on ajax requests where the form is not included in execute!
		// The current implementation just walks through the request parameter map instead.

		for (String name : context.getExternalContext().getRequestParameterMap().keySet()) {
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
		UIComponent source = getCurrentActionSource();
		return source instanceof UICommand ? (UICommand) source : null;
	}

	/**
	 * Returns the source of the currently invoked action, or <code>null</code> if there is none, which may happen when
	 * the current request is not a postback request at all, or when the view has been changed by for example a
	 * successful navigation. If the latter is the case, you'd better invoke this method before navigation.
	 * @return The source of the currently invoked action.
	 * @since 2.4
	 */
	public static UIComponent getCurrentActionSource() {
		FacesContext context = FacesContext.getCurrentInstance();

		if (!context.isPostback()) {
			return null;
		}

		UIViewRoot viewRoot = context.getViewRoot();
		Map<String, String> params = context.getExternalContext().getRequestParameterMap();
		UIComponent actionSource = null;

		if (context.getPartialViewContext().isAjaxRequest()) {
			String sourceClientId = params.get("javax.faces.source");

			if (sourceClientId != null) {
				actionSource = findComponentIgnoringIAE(viewRoot, stripIterationIndexFromClientId(sourceClientId));
			}
		}

		if (actionSource == null) {
			for (String name : params.keySet()) {
				if (name.startsWith("javax.faces.")) {
					continue; // Quick skip.
				}

				actionSource = findComponentIgnoringIAE(viewRoot, stripIterationIndexFromClientId(name));

				if (actionSource instanceof UICommand) {
					break;
				}
			}
		}

		return actionSource;
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
				label = labelExpression.getValue(getELContext());
			}
		}

		return (label != null) ? label.toString() : null;
	}

	/**
	 * Returns the value of the given editable value holder component without the need to know if the given component
	 * has already been converted/validated or not. Note that it thus returns the unconverted submitted string value
	 * when the conversion/validation hasn't been taken place for the given component and it returns the converted
	 * object value -if applicable- when conversion/validation has been taken place for the given component.
	 * @param <T> The expected return type.
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
	 * @param <T> The expected return type.
	 * @param input The input component to obtain the converted/validated value for.
	 * @return The converted/validated value of the given input component.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @since 1.2
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getImmediateValue(UIInput input) {
		if (input.isValid() && input.getSubmittedValue() != null) {
			input.validate(getContext());
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
		UIComponent source = getCurrentActionSource();
		return source != null && source.equals(component);
	}

	/**
	 * Returns an unmodifiable list with all child {@link UIParameter} components (<code>&lt;f|o:param&gt;</code>) of
	 * the given parent component as a list of {@link ParamHolder} instances. Those with <code>disabled=true</code> and
	 * an empty name are skipped.
	 * @param component The parent component to retrieve all child {@link UIParameter} components from.
	 * @return An unmodifiable list with all child {@link UIParameter} components having a non-empty name and not
	 * disabled.
	 * @since 2.1
	 */
	public static List<ParamHolder> getParams(UIComponent component) {
		if (component.getChildCount() > 0) {
			List<ParamHolder> params = new ArrayList<>(component.getChildCount());

			for (UIComponent child : component.getChildren()) {
				if (child instanceof UIParameter) {
					UIParameter param = (UIParameter) child;

					if (!isEmpty(param.getName()) && !param.isDisable()) {
						params.add(new SimpleParam(param));
					}
				}
			}

			return Collections.unmodifiableList(params);
		}
		else {
			return Collections.emptyList();
		}
	}

	/**
	 * Returns an unmodifiable map with all request query string or view parameters, appended with all child
	 * {@link UIParameter} components (<code>&lt;f|o:param&gt;</code>) of the given parent component. Those with
	 * <code>disabled=true</code> or an empty name or an empty value are skipped. The <code>&lt;f|o:param&gt;</code>
	 * will override any included view or request parameters on the same name.
	 * @param component The parent component to retrieve all child {@link UIParameter} components from.
	 * @param includeRequestParams Whether or not to include request query string parameters.
	 * When set to <code>true</code>, then this overrides the <code>includeViewParams</code>.
	 * @param includeViewParams Whether or not to include view parameters.
	 * @return An unmodifiable list with all request query string or view parameters, appended with all child
	 * {@link UIParameter} components having a non-empty name and not disabled.
	 * @since 2.4
	 */
	public static Map<String, List<String>> getParams(UIComponent component, boolean includeRequestParams, boolean includeViewParams) {
		FacesContext context = FacesContext.getCurrentInstance();
		Map<String, List<String>> params;

		if (includeRequestParams) {
			params = getRequestQueryStringMap(context);
		}
		else if (includeViewParams) {
			params = getViewParameterMap(context);
		}
		else {
			params = new LinkedHashMap<>(0);
		}

		for (ParamHolder param : getParams(component)) {
			Object value = param.getValue();

			if (isEmpty(value)) {
				continue;
			}

			params.put(param.getName(), asList(value.toString()));
		}

		return Collections.unmodifiableMap(params);
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
				method.invoke(getELContext(), new Object[] { event });
			}
		});
		return behavior;
	}

	/**
	 * Returns a list of all action expressions and listeners associated with given component. This covers expressions
	 * in <code>action</code> attribute of command components and <code>listener</code> attribute of ajax components.
	 * Any method expressions are in format <code>#{bean.method}</code> and any action listeners are added as fully
	 * qualified class names. This list is primarily useful for logging postback actions in a phase listener. You can
	 * use {@link #getCurrentActionSource()} to obtain the current action source.
	 * @param component The component to retrieve all action expressions and listeners from.
	 * @return A list of all action expressions and listeners associated with given component.
	 * @since 2.4
	 */
	@SuppressWarnings("unchecked")
	public static List<String> getActionExpressionsAndListeners(UIComponent component) {
        List<String> actions = new ArrayList<>();

        if (component instanceof ActionSource2) {
        	ActionSource2 source = (ActionSource2) component;
        	addExpressionStringIfNotNull(source.getActionExpression(), actions);

            for (ActionListener actionListener : source.getActionListeners()) {
            	actions.add(actionListener.getClass().getName());
            }
        }

        if (component instanceof ClientBehaviorHolder) {
            String behaviorEvent = getRequestParameter("javax.faces.behavior.event");

            if (behaviorEvent != null) {
            	List<ClientBehavior> behaviors = ((ClientBehaviorHolder) component).getClientBehaviors().get(behaviorEvent);

            	if (behaviors != null) {
            		for (ClientBehavior behavior : behaviors) {
            			List<BehaviorListener> listeners = getField(BehaviorBase.class, List.class, behavior);

            			if (listeners != null) {
            				for (BehaviorListener listener : listeners) {
            					addExpressionStringIfNotNull(getField(listener.getClass(), MethodExpression.class, listener), actions);
            				}
            			}
            		}
            	}
            }
        }

        return unmodifiableList(actions);
	}

	// Validation -----------------------------------------------------------------------------------------------------

	/**
	 * Validate if the given component has a parent of the given parent type.
	 * @param <C> The generic component type.
	 * @param component The component to be validated.
	 * @param parentType The parent type to be checked.
	 * @throws IllegalArgumentException When the given component doesn't have any parent of the given type.
	 */
	public static <C extends UIComponent> void validateHasParent(UIComponent component, Class<C> parentType)
		throws IllegalArgumentException
	{
		if (getClosestParent(component, parentType) == null) {
			throw new IllegalArgumentException(String.format(
				ERROR_INVALID_PARENT, component.getClass().getSimpleName(), parentType));
		}
	}

	/**
	 * Validate if the given component has a direct parent of the given parent type.
	 * @param <C> The generic component type.
	 * @param component The component to be validated.
	 * @param parentType The parent type to be checked.
	 * @throws IllegalArgumentException When the given component doesn't have a direct parent of the given type.
	 */
	public static <C extends UIComponent> void validateHasDirectParent(UIComponent component, Class<C> parentType)
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
		String separatorChar = Character.toString(UINamingContainer.getSeparatorChar(getContext()));
		return clientId.replaceAll(quote(separatorChar) + "[0-9]+" + quote(separatorChar), separatorChar);
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

	/**
	 * If given method expression is not null, extract expression string from it and add to given list.
	 */
	private static void addExpressionStringIfNotNull(MethodExpression expression, List<String> list) {
		if (expression != null) {
			list.add(expression.getExpressionString());
		}
	}

	/**
	 * Get first matching field of given field type from the given class type and get the value from the given instance.
	 * (this is a rather specific helper for getActionExpressionsAndListeners() and may not work in other cases).
	 */
    @SuppressWarnings("unchecked")
	private static <C, F> F getField(Class<? extends C> classType, Class<F> fieldType, C instance) {
        for (Field field : classType.getDeclaredFields()) {
            if (fieldType.isAssignableFrom(field.getType())) {
                field.setAccessible(true);

                try {
                	return (F) field.get(instance);
				}
                catch (IllegalAccessException e) {
					throw new IllegalStateException(e);
				}
            }
        }

        return null;
    }

	// Inner classes --------------------------------------------------------------------------------------------------

	/**
	 * This faces context wrapper allows returning the given temporary view on {@link #getViewRoot()} and its
	 * associated renderer in {@link #getRenderKit()}. This can then be used in cases when a different view needs to be
	 * built within the current view. Using {@link FacesContext#setViewRoot(UIViewRoot)} isn't desired as it can't be
	 * cleared afterwards when the current view is actually <code>null</code>. The {@link #setViewRoot(UIViewRoot)}
	 * doesn't accept a <code>null</code> being set.
	 *
	 * @author Bauke Scholtz
	 */
	private static class TemporaryViewFacesContext extends FacesContextWrapper {

		private FacesContext wrapped;
		private UIViewRoot temporaryView;

		public TemporaryViewFacesContext(FacesContext wrapped, UIViewRoot temporaryView) {
			this.wrapped = wrapped;
			this.temporaryView = temporaryView;
		}

		@Override
		public UIViewRoot getViewRoot() {
			return temporaryView;
		}

		@Override
		public RenderKit getRenderKit() {
			return FacesLocal.getRenderKit(this);
		}

		@Override
		public FacesContext getWrapped() {
			return wrapped;
		}

	}

}