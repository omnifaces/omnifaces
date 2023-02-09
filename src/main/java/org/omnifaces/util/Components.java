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
package org.omnifaces.util;

import static jakarta.faces.application.ResourceHandler.JSF_SCRIPT_LIBRARY_NAME;
import static jakarta.faces.application.ResourceHandler.JSF_SCRIPT_RESOURCE_NAME;
import static jakarta.faces.application.StateManager.IS_BUILDING_INITIAL_STATE;
import static jakarta.faces.component.UIComponent.getCompositeComponentParent;
import static jakarta.faces.component.behavior.ClientBehaviorContext.BEHAVIOR_EVENT_PARAM_NAME;
import static jakarta.faces.component.behavior.ClientBehaviorContext.BEHAVIOR_SOURCE_PARAM_NAME;
import static jakarta.faces.component.search.SearchExpressionContext.createSearchExpressionContext;
import static jakarta.faces.component.search.SearchExpressionHint.IGNORE_NO_RESULT;
import static jakarta.faces.component.search.SearchExpressionHint.RESOLVE_SINGLE_COMPONENT;
import static jakarta.faces.component.visit.VisitContext.createVisitContext;
import static jakarta.faces.component.visit.VisitHint.SKIP_ITERATION;
import static jakarta.faces.component.visit.VisitResult.ACCEPT;
import static jakarta.faces.event.PhaseId.RENDER_RESPONSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.logging.Level.FINEST;
import static java.util.regex.Pattern.quote;
import static org.omnifaces.resourcehandler.DefaultResourceHandler.FACES_SCRIPT_RESOURCE_NAME;
import static org.omnifaces.util.Ajax.load;
import static org.omnifaces.util.Ajax.oncomplete;
import static org.omnifaces.util.Events.subscribeToRequestBeforePhase;
import static org.omnifaces.util.Faces.getContext;
import static org.omnifaces.util.Faces.getELContext;
import static org.omnifaces.util.Faces.getFaceletContext;
import static org.omnifaces.util.Faces.getRequestParameter;
import static org.omnifaces.util.Faces.getViewRoot;
import static org.omnifaces.util.Faces.isDevelopment;
import static org.omnifaces.util.Faces.setContext;
import static org.omnifaces.util.FacesLocal.createConverter;
import static org.omnifaces.util.FacesLocal.getRenderKit;
import static org.omnifaces.util.FacesLocal.getRequestQueryStringMap;
import static org.omnifaces.util.FacesLocal.getViewParameterMap;
import static org.omnifaces.util.FacesLocal.isAjaxRequestWithPartialRendering;
import static org.omnifaces.util.FacesLocal.normalizeViewId;
import static org.omnifaces.util.Hacks.isFacesScriptResourceAvailable;
import static org.omnifaces.util.Renderers.RENDERER_TYPE_JS;
import static org.omnifaces.util.Utils.isEmpty;
import static org.omnifaces.util.Utils.isOneInstanceOf;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;
import jakarta.faces.application.Application;
import jakarta.faces.application.ResourceHandler;
import jakarta.faces.application.ViewHandler;
import jakarta.faces.component.ActionSource2;
import jakarta.faces.component.EditableValueHolder;
import jakarta.faces.component.NamingContainer;
import jakarta.faces.component.UICommand;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIComponentBase;
import jakarta.faces.component.UIForm;
import jakarta.faces.component.UIInput;
import jakarta.faces.component.UIMessage;
import jakarta.faces.component.UIMessages;
import jakarta.faces.component.UINamingContainer;
import jakarta.faces.component.UIOutput;
import jakarta.faces.component.UIParameter;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.component.ValueHolder;
import jakarta.faces.component.behavior.AjaxBehavior;
import jakarta.faces.component.behavior.BehaviorBase;
import jakarta.faces.component.behavior.ClientBehavior;
import jakarta.faces.component.behavior.ClientBehaviorHolder;
import jakarta.faces.component.html.HtmlBody;
import jakarta.faces.component.search.SearchExpressionContext;
import jakarta.faces.component.search.SearchExpressionHint;
import jakarta.faces.component.visit.VisitCallback;
import jakarta.faces.component.visit.VisitContext;
import jakarta.faces.component.visit.VisitHint;
import jakarta.faces.component.visit.VisitResult;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.FacesContextWrapper;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.convert.Converter;
import jakarta.faces.event.ActionEvent;
import jakarta.faces.event.ActionListener;
import jakarta.faces.event.AjaxBehaviorEvent;
import jakarta.faces.event.BehaviorListener;
import jakarta.faces.event.MethodExpressionActionListener;
import jakarta.faces.render.RenderKit;
import jakarta.faces.view.ViewDeclarationLanguage;
import jakarta.faces.view.facelets.FaceletContext;

import org.omnifaces.component.ParamHolder;
import org.omnifaces.component.SimpleParam;
import org.omnifaces.component.input.Form;
import org.omnifaces.config.OmniFaces;
import org.omnifaces.el.ScopedRunner;

/**
 * <p>
 * Collection of utility methods for the Faces API with respect to working with {@link UIComponent}. There are several
 * traversal/lookup methods, there are several {@link UIForm} and {@link UIInput} related methods which makes it easier
 * to deal with forms and inputs.
 *
 * <h2>Usage</h2>
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
 * // Get the label of the given UIInput component as Faces uses for validation messages.
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

	private static final Logger logger = Logger.getLogger(Components.class.getName());

	/** The name of the label attribute. */
	public static final String LABEL_ATTRIBUTE = "label";

	/** The name of the value attribute. */
	public static final String VALUE_ATTRIBUTE = "value";

	private static final String ERROR_MISSING_PARENT =
		"Component '%s' must have a parent of type '%s', but it cannot be found.";
	private static final String ERROR_MISSING_DIRECT_PARENT =
		"Component '%s' must have a direct parent of type '%s', but it cannot be found.";
	private static final String ERROR_MISSING_CHILD =
		"Component '%s' must have at least one child of type '%s', but it cannot be found.";
	private static final String ERROR_ILLEGAL_PARENT =
		"Component '%s' may not have a parent of type '%s'.";
	private static final String ERROR_ILLEGAL_CHILDREN =
		"Component '%s' may only have children of type '%s'. Encountered children of types '%s'.";
	private static final String ERROR_CHILDREN_DISALLOWED =
		"Component '%s' may not have any children. Encountered children of types '%s'.";

	private static final Set<SearchExpressionHint> RESOLVE_LABEL_FOR = EnumSet.of(RESOLVE_SINGLE_COMPONENT, IGNORE_NO_RESULT);

	// Constructors ---------------------------------------------------------------------------------------------------

	private Components() {
		// Hide constructor.
	}

	// General --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the current UI component from the EL context.
	 * @param <C> The expected component type.
	 * @return The current UI component from the EL context.
	 * @throws ClassCastException When <code>C</code> is of wrong type.
	 * @see UIComponent#getCurrentComponent(FacesContext)
	 */
	@SuppressWarnings("unchecked")
	public static <C extends UIComponent> C getCurrentComponent() {
		return (C) UIComponent.getCurrentComponent(getContext());
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
			if (parent instanceof NamingContainer || parent instanceof UIViewRoot) {
				UIComponent result = findComponentIgnoringIAE(parent, clientId);

				if (result != null) {
					return (C) result;
				}
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
				result = findComponentIgnoringIAE(child, clientId);
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
	 * Returns a list of UI components matching the given type in children of the currently submitted form.
	 * The currently submitted form is obtained by {@link #getCurrentForm()}.
	 * @param <C> The generic component type.
	 * @param type The type of the UI components to be searched in children of the currently submitted form.
	 * @return A list of UI components matching the given type in children of the currently submitted form.
	 * @since 3.1
	 */
	public static <C extends UIComponent> List<C> findComponentsInCurrentForm(Class<C> type) {
		UIForm currentForm = getCurrentForm();
		return currentForm != null ? findComponentsInChildren(currentForm, type) : emptyList();
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

	/**
	 * Finds from the given component the closest parent of the given parent type.
	 * @param <C> The generic component type.
	 * @param component The component to find the closest parent of the given parent type for.
	 * @param parentType The parent type.
	 * @return From the given component the closest parent of the given parent type.
	 * @since 3.11
	 */
	public static <C extends UIComponent> Optional<C> findClosestParent(UIComponent component, Class<C> parentType) {
		return Optional.ofNullable(getClosestParent(component, parentType));
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
	 * Builder class used to collect a number of query parameters for a visit (for each) of components in the Faces
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
				Collections.addAll(hintsSet, hints);
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
		 * @param <C> The expected component type.
		 * @param operation the operation to invoke on each component
		 * @throws ClassCastException When <code>C</code> is of wrong type.
		 */
		@SuppressWarnings("unchecked")
		public <C extends UIComponent> void invoke(Consumer<C> operation) {
			invoke((context, target) -> {
				operation.accept((C) target);
				return ACCEPT;
			});
		}

		/**
		 * Invokes the given operation on the components as specified by the
		 * query parameters set via this builder.
		 *
		 * @param operation the operation to invoke on each component
		 */
		public void invoke(VisitCallback operation) {
			VisitContext visitContext = createVisitContext(getFacesContext(), getIds(), getHints());
			VisitCallback visitCallback = (types == null) ? operation : new TypesVisitCallback(types, operation);

			if (getFacesContext().getViewRoot().equals(getRoot())) {
				getRoot().visitTree(visitContext, visitCallback);
			}
			else {
				forEachComponent().havingIds(getRoot().getClientId()).invoke(viewRoot -> viewRoot.visitTree(visitContext, visitCallback));
			}
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
	 * @throws IOException Whenever given path cannot be read.
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
		Map<String, Object> attrs = (attributes == null) ? null : new HashMap<>(attributes);

		FacesContext context = FacesContext.getCurrentInstance();
		UIComponent composite = context.getApplication().getViewHandler()
			.getViewDeclarationLanguage(context, context.getViewRoot().getViewId())
			.createComponent(context, taglibURI, tagName, attrs);
		composite.setId(id);
		parent.getChildren().add(composite);
		return composite;
	}

	/**
	 * Add given JavaScript code to the current view which is to be executed as an inline script when the rendering is
	 * completed. When the current request is {@link Faces#isAjaxRequestWithPartialRendering()}, then it will delegate
	 * to {@link Ajax#oncomplete(String...)}, else it will add given JavaScript code as inline script to end of body.
	 * @param script JavaScript code which is to be executed as an inline script.
	 * @since 3.6
	 */
	public static void addScript(String script) {
		FacesContext context = FacesContext.getCurrentInstance();

		if (isAjaxRequestWithPartialRendering(context)) {
			oncomplete(script);
		}
		else if (context.getCurrentPhaseId() != RENDER_RESPONSE) {
			subscribeToRequestBeforePhase(RENDER_RESPONSE, () -> addScriptToBody(script)); // Just to avoid it misses when view rebuilds in the meanwhile.
		}
		else {
			addScriptToBody(script);
		}
	}

	/**
	 * Add given JavaScript resource to the current view. This will first check if the resource isn't already rendered
	 * as per {@link ResourceHandler#isResourceRendered(FacesContext, String, String)}. If not, then continue as below:
	 * <ul>
	 * <li>When the current request is a {@link Faces#isAjaxRequestWithPartialRendering()}, then it will delegate to
	 * {@link Ajax#load(String, String)}.</li>
	 * <li>Else when the <code>&lt;h:head&gt;</code> has not yet been rendered, then add given JavaScript resource to
	 * head.</li>
	 * <li>Else add given JavaScript resource to end of the <code>&lt;h:body&gt;</code>.</li>
	 * </ul>
	 * @param libraryName Library name of the JavaScript resource.
	 * @param resourceName Resource name of the JavaScript resource.
	 * @since 3.6
	 */
	public static void addScriptResource(String libraryName, String resourceName) {
		FacesContext context = FacesContext.getCurrentInstance();

		if (!context.getApplication().getResourceHandler().isResourceRendered(context, resourceName, libraryName)) {
			if (isAjaxRequestWithPartialRendering(context)) {
				load(libraryName, resourceName);
			}
			else if (context.getCurrentPhaseId() != RENDER_RESPONSE) {
				addScriptResourceToHead(libraryName, resourceName);
				subscribeToRequestBeforePhase(RENDER_RESPONSE, () -> addScriptResourceToBody(libraryName, resourceName)); // Fallback in case view rebuilds in the meanwhile. It will re-check if already added.
			}
			else if (TRUE.equals(context.getAttributes().get(IS_BUILDING_INITIAL_STATE))) {
				addScriptResourceToHead(libraryName, resourceName);
			}
			else {
				addScriptResourceToBody(libraryName, resourceName);
			}
		}
	}

	/**
	 * Add the Faces JavaScript resource to current view. If Faces 4.0+ is present, then it will add the
	 * <code>jakarta.faces:faces.js</code> resource, else it will add the <code>jakarta.faces:jsf.js</code> resource.
	 * @since 4.0
	 */
	public static void addFacesScriptResource() {
		addScriptResource(JSF_SCRIPT_LIBRARY_NAME, isFacesScriptResourceAvailable() ? FACES_SCRIPT_RESOURCE_NAME : JSF_SCRIPT_RESOURCE_NAME);
	}

	private static UIOutput createScriptResource() {
		UIOutput outputScript = new UIOutput();
		outputScript.setRendererType(RENDERER_TYPE_JS);
		return outputScript;
	}

	private static UIComponent addScriptResourceToTarget(String libraryName, String resourceName, String target) {
		FacesContext context = FacesContext.getCurrentInstance();
		String id = (libraryName != null ? (libraryName.replaceAll("\\W+", "_") + "_") : "") + resourceName.replaceAll("\\W+", "_");

		for (UIComponent existingResource : context.getViewRoot().getComponentResources(context)) {
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

	private static void addScriptResourceToHead(String libraryName, String resourceName) {
		addScriptResourceToTarget(libraryName, resourceName, "head");
	}

	private static void addScriptResourceToBody(String libraryName, String resourceName) {
		addScriptResourceToTarget(libraryName, resourceName, "body");
	}

	private static UIComponent addComponentResource(UIComponent resource, String target) {
		FacesContext context = FacesContext.getCurrentInstance();

		if (resource.getId() == null) {
			Hacks.setComponentResourceUniqueId(context, resource);
		}

		context.getViewRoot().addComponentResource(context, resource, target);
		return resource;
	}

	private static void addScriptToBody(String script) {
		UIOutput outputScript = createScriptResource();
		UIOutput content = new UIOutput();
		content.setValue(script);
		outputScript.getChildren().add(content);
		addComponentResource(outputScript, "body");
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
	 * not have the desired effect. If the given view does not have any component resources, Faces forms, dynamically
	 * added components, component event listeners, then it should mostly be safe.
	 * In other words, use this at most for "simple templates" only, e.g. a HTML based mail template, which usually
	 * already doesn't have a HTML head nor body.
	 * @param component The component to capture HTML output for.
	 * @return The encoded HTML output of the given component.
	 * @throws UncheckedIOException Whenever something fails at I/O level. This would be quite unexpected as it happens locally.
	 * @since 2.2
	 * @see UIComponent#encodeAll(FacesContext)
	 */
	public static String encodeHtml(UIComponent component) {
		FacesContext context = FacesContext.getCurrentInstance();
		ResponseWriter originalWriter = context.getResponseWriter();
		StringWriter output = new StringWriter();
		context.setResponseWriter(getRenderKit(context).createResponseWriter(output, "text/html", "UTF-8"));

		try {
			component.encodeAll(context);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
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

		if (viewRoot == null) {
			return null;
		}

		// The initial implementation has visited the tree for UIForm components which returns true on isSubmitted().
		// But with testing it turns out to return false on ajax requests where the form is not included in execute!
		// The current implementation just walks through the request parameter map instead.

		for (String name : context.getExternalContext().getRequestParameterMap().keySet()) {
			if (name.startsWith("jakarta.faces.")) {
				continue; // Quick skip.
			}

			UIComponent component = findComponentIgnoringIAE(viewRoot, name);

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
	 * @param <C> The expected component type.
	 * @return The source of the currently invoked action.
	 * @since 2.4
	 */
	@SuppressWarnings("unchecked")
	public static <C extends UIComponent> C getCurrentActionSource() {
		FacesContext context = FacesContext.getCurrentInstance();

		if (!context.isPostback()) {
			return null;
		}

		return (C) getCurrentActionSource(context, context.getViewRoot());
	}

	/**
	 * Helper method for {@link #getCurrentActionSource()}.
	 */
	static UIComponent getCurrentActionSource(FacesContext context, UIComponent parent) {
		if (parent == null) {
			return null;
		}

		Map<String, String> params = context.getExternalContext().getRequestParameterMap();

		if (context.getPartialViewContext().isAjaxRequest()) {
			String sourceClientId = params.get(BEHAVIOR_SOURCE_PARAM_NAME);

			if (sourceClientId != null) {
				UIComponent actionSource = findComponentIgnoringIAE(parent, sourceClientId);

				if (actionSource != null) {
					return actionSource;
				}
			}
		}

		for (String name : params.keySet()) {
			if (name.startsWith("jakarta.faces.")) {
				continue; // Quick skip.
			}

			UIComponent actionSource = findComponentIgnoringIAE(parent, name);

			if (actionSource instanceof UICommand) {
				return actionSource;
			}
		}

		if (parent instanceof UIViewRoot) { // If still not found and parent is UIViewRoot, then it can happen when prependId="false" is set on form. Hopefully it will be deprecated one day.
			return getCurrentActionSource(context, getCurrentForm());
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
		return isRendered(input)
			&& !Boolean.TRUE.equals(input.getAttributes().get("disabled"))
			&& !Boolean.TRUE.equals(input.getAttributes().get("readonly"));
	}

	/**
	 * Returns the value of the <code>label</code> attribute associated with the given UI component if any, else
	 * the client ID. It never returns null.
	 * @param component The UI component for which the label is to be retrieved.
	 * @return The value of the <code>label</code> attribute associated with the given UI component if any, else
	 * the client ID.
	 */
	public static String getLabel(UIComponent component) {
		String label = getOptionalLabel(component);
		return (label != null) ? label : component.getClientId();
	}

	/**
	 * Returns the value of the <code>label</code> attribute associated with the given UI component if any, else
	 * null.
	 * @param component The UI component for which the label is to be retrieved.
	 * @return The value of the <code>label</code> attribute associated with the given UI component if any, else
	 * null.
	 */
	public static String getOptionalLabel(UIComponent component) {
		Object[] result = new Object[1];

		new ScopedRunner(getContext()).with("cc", getCompositeComponentParent(component)).invoke(() -> {
			Object label = component.getAttributes().get(LABEL_ATTRIBUTE);

			if (isEmpty(label)) {
				ValueExpression labelExpression = component.getValueExpression(LABEL_ATTRIBUTE);

				if (labelExpression != null) {
					label = labelExpression.getValue(getELContext());
				}
			}

			result[0] = label;
		});

		return (result[0] != null) ? result[0].toString() : null;
	}

	/**
	 * Sets the <code>label</code> attribute of the given UI component with the given value.
	 * @param component The UI component for which the label is to be set.
	 * @param label The label to be set on the given UI component.
	 */
	public static void setLabel(UIComponent component, Object label) {
		if (label instanceof ValueExpression) {
			component.setValueExpression(LABEL_ATTRIBUTE, (ValueExpression) label);
		}
		else if (label != null) {
			component.getAttributes().put(LABEL_ATTRIBUTE, label);
		}
		else {
			component.getAttributes().remove(LABEL_ATTRIBUTE);
		}
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
	 * Returns the expected type of the "value" attribute of the given component. This is useful in among others a
	 * "generic entity converter".
	 * @param <T> The expected type of the expected type of the "value" attribute of the given component.
	 * @param component The component to obtain the expected type of the "value" attribute for.
	 * @return The expected type of the "value" attribute of the given component, or <code>null</code> when there is no such value.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @since 3.8
	 */
	@SuppressWarnings("unchecked")
	public static <T> Class<T> getExpectedValueType(UIComponent component) {
		ValueExpression valueExpression = component.getValueExpression(VALUE_ATTRIBUTE);

		if (valueExpression != null) {
			return getExpectedType(valueExpression);
		}
		else {
			Object value = component.getAttributes().get(VALUE_ATTRIBUTE);

			if (value != null) {
				return (Class<T>) value.getClass();
			}

			return null;
		}
	}

	/**
	 * Returns the expected type of the given value expression. This first inspects if the
	 * {@link ValueExpression#getExpectedType()} returns a specific type, i.e. not <code>java.lang.Object</code>, and
	 * then returns it, else it inspects the actual type of the property behind the expression string.
	 * @param <T> The expected type of the expected type of the given value expression.
	 * @param valueExpression The value expression to obtain the expected type for.
	 * @return The expected type of the given value expression.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @since 3.8
	 */
	@SuppressWarnings("unchecked")
	public static <T> Class<T> getExpectedType(ValueExpression valueExpression) {
		Class<?> expectedType = valueExpression.getExpectedType();

		if (expectedType == Object.class) {
			expectedType = valueExpression.getType(getELContext());
		}

		return (Class<T>) expectedType;
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
	 * @param <T> The type of the param value.
	 * @param component The parent component to retrieve all child {@link UIParameter} components from.
	 * @return An unmodifiable list with all child {@link UIParameter} components having a non-empty name and not
	 * disabled.
	 * @since 2.1
	 */
	public static <T> List<ParamHolder<T>> getParams(UIComponent component) {
		if (component.getChildCount() == 0) {
			return Collections.emptyList();
		}

		List<ParamHolder<T>> params = new ArrayList<>(component.getChildCount());

		for (UIComponent child : component.getChildren()) {
			if (child instanceof UIParameter) {
				UIParameter param = (UIParameter) child;

				if (!isEmpty(param.getName()) && !param.isDisable()) {
					params.add(new SimpleParam<>(param));
				}
			}
		}

		return Collections.unmodifiableList(params);
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

		for (ParamHolder<Object> param : getParams(component)) {
			String value = param.getValue();

			if (isEmpty(value)) {
				continue;
			}

			params.put(param.getName(), asList(value));
		}

		return Collections.unmodifiableMap(params);
	}

	/**
	 * Returns the {@link UIMessage} component associated with given {@link UIInput} component.
	 * This returns <code>null</code> if none can be found.
	 * @param input The UI input component to find the associated message component for.
	 * @return The {@link UIMessage} component associated with given {@link UIInput} component.
	 * @since 2.5
	 */
	public static UIMessage getMessageComponent(UIInput input) {
		UIMessage[] found = new UIMessage[1];

		forEachComponent().ofTypes(UIMessage.class).withHints(SKIP_ITERATION).invoke((context, target) -> {
			UIMessage messageComponent = (UIMessage) target;
			String forValue = messageComponent.getFor();

			if (!isEmpty(forValue)) {
				FacesContext facesContext = context.getFacesContext();
				SearchExpressionContext searchExpressionContext = createSearchExpressionContext(facesContext, messageComponent, RESOLVE_LABEL_FOR, null);
				String forClientId = facesContext.getApplication().getSearchExpressionHandler().resolveClientId(searchExpressionContext, forValue);
				UIComponent forComponent = findComponentRelatively(messageComponent, forClientId);

				if (input.equals(forComponent)) {
					found[0] = messageComponent;
					return VisitResult.COMPLETE;
				}
			}

			return VisitResult.ACCEPT;
		});

		return found[0];
	}

	/**
	 * Returns the first {@link UIMessages} component found in the current view.
	 * This returns <code>null</code> if none can be found.
	 * @return The first {@link UIMessages} component found in the current view.
	 * @since 2.5
	 */
	public static UIMessages getMessagesComponent() {
		UIMessages[] found = new UIMessages[1];

		forEachComponent().ofTypes(UIMessages.class).withHints(SKIP_ITERATION).invoke((context, target) -> {
			found[0] = (UIMessages) target;
			return VisitResult.COMPLETE;
		});

		return found[0];
	}

	/**
	 * Reset all child {@link UIInput} components enclosed in the given {@link UIForm} component, or the closest
	 * {@link UIForm} parent of it.
	 * @param component The component representing the {@link UIForm} itself, or to find the closest {@link UIForm}
	 * parent for.
	 * @throws IllegalArgumentException When given component is not an {@link UIForm}, or does not have a {@link UIForm}
	 * parent.
	 * @since 2.5
	 */
	public static void resetForm(UIComponent component) {
		UIForm form = (component instanceof UIForm) ? (UIForm) component : getClosestParent(component, UIForm.class);

		if (form == null) {
			throw new IllegalArgumentException(format(ERROR_MISSING_PARENT, component.getClass(), UIForm.class));
		}

		resetInputs(form);
	}

	/**
	 * Reset all child {@link UIInput} components enclosed in the given parent component.
	 * @param component The parent component to reset all child {@link UIInput} components in.
	 * @since 2.5
	 */
	public static void resetInputs(UIComponent component) {
		forEachComponent().fromRoot(component).ofTypes(UIInput.class).invoke(UIInput::resetValue);
	}

	/**
	 * Add an {@link UIForm} to the current view if absent.
	 * This might be needed for scripts which rely on Faces view state identifier and/or on functioning of jsf.ajax.request().
	 * @since 3.6
	 */
	public static void addFormIfNecessary() {
		FacesContext context = FacesContext.getCurrentInstance();

		if (isAjaxRequestWithPartialRendering(context)) {
			return; // It's impossible to have this condition without an UIForm in first place.
		}

		UIViewRoot viewRoot = context.getViewRoot();

		if (viewRoot == null || viewRoot.getChildCount() == 0) {
			return; // Empty view. Nothing to do against. The client should probably find a better moment to invoke this.
		}

		VisitCallback visitCallback = (visitContext, target) -> (target instanceof UIForm) ? VisitResult.COMPLETE : VisitResult.ACCEPT;
		boolean formFound = viewRoot.visitTree(createVisitContext(context, null, EnumSet.of(VisitHint.SKIP_ITERATION)), visitCallback);

		if (formFound) {
			return; // UIForm present. No need to add a new one.
		}

		Optional<UIComponent> body = viewRoot.getChildren().stream().filter(HtmlBody.class::isInstance).findFirst();

		if (!body.isPresent()) {
			return; // No <h:body> present. Not possible to add a new UIForm then.
		}

		Form form = new Form();
		form.setId(OmniFaces.OMNIFACES_DYNAMIC_FORM_ID);
		form.getAttributes().put("style", "display:none"); // Just to be on the safe side. There might be CSS which puts visible style such as margin/padding/border on any <form> for some reason.
		body.get().getChildren().add(form);
	}

	/**
	 * Convert given value of given value holder to string using either the converter attached to the given value holder
	 * or the one obtained via {@link Application#createConverter(Class)} based on the type of the given value.
	 * @param <T> The generic value type.
	 * @param context The involved faces context.
	 * @param holder The value holder.
	 * @param value The value to be converted to string.
	 * @return The conversion result, may be {@code null}, depending on the value and the converter implementation.
	 * @since 4.1
	 */
	@SuppressWarnings("unchecked")
	public static <T> String convertToString(FacesContext context, ValueHolder holder, T value) {
		Converter<T> converter = holder.getConverter();

		if (converter == null && value != null) {
			converter = createConverter(context, value.getClass());
		}

		if (converter != null) {
			UIComponent component = null;

			if (holder instanceof UIComponent) {
				component = (UIComponent) holder;
			}
			else if (holder instanceof ParamHolder) {
				UIParameter parameter = new UIParameter();
				parameter.setName(((ParamHolder<T>) holder).getName());
				parameter.setValue(value);
				component = parameter;
			}
			else {
				UIOutput output = new UIOutput();
				output.setValue(value);
				component = output;
			}

			return converter.getAsString(context, component, value);
		}

		return (value == null) ? null : value.toString();
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
	 * {@link UICommand#addActionListener(jakarta.faces.event.ActionListener)}.
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
		MethodExpression method = createVoidMethodExpression(expression, AjaxBehaviorEvent.class);
		behavior.addAjaxBehaviorListener(event -> method.invoke(getELContext(), new Object[] { event }));
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
	public static List<String> getActionExpressionsAndListeners(UIComponent component) {
		List<String> actions = new ArrayList<>();

		if (component instanceof ActionSource2) {
			ActionSource2 source = (ActionSource2) component;
			addExpressionStringIfNotNull(source.getActionExpression(), actions);

			for (ActionListener listener : source.getActionListeners()) {
				addExpressionStringIfNotNull(getField(listener.getClass(), MethodExpression.class, listener), actions);
			}
		}

		if (component instanceof ClientBehaviorHolder) {
			String behaviorEvent = getRequestParameter(BEHAVIOR_EVENT_PARAM_NAME);

			if (behaviorEvent != null) {
				for (BehaviorListener listener : getBehaviorListeners((ClientBehaviorHolder) component, behaviorEvent)) {
					addExpressionStringIfNotNull(getField(listener.getClass(), MethodExpression.class, listener), actions);
				}
			}
		}

		return unmodifiableList(actions);
	}

	// Hierarchy validation -------------------------------------------------------------------------------------------

	/**
	 * Validate in development stage if the given component has a parent of given parent type.
	 * @param <C> The generic component type.
	 * @param component The component to be validated.
	 * @param parentType The parent type to be checked.
	 * @throws IllegalStateException When the given component doesn't have any parent of the given type.
	 */
	public static <C extends UIComponent> void validateHasParent(UIComponent component, Class<C> parentType) {
		if (!isDevelopment()) {
			return;
		}

		if (getClosestParent(component, parentType) == null) {
			throw new IllegalStateException(format(
				ERROR_MISSING_PARENT, component.getClass().getSimpleName(), parentType));
		}
	}

	/**
	 * Validate in development stage if the given component has a direct parent of given parent type.
	 * @param <C> The generic component type.
	 * @param component The component to be validated.
	 * @param parentType The parent type to be checked.
	 * @throws IllegalStateException When the given component doesn't have a direct parent of the given type.
	 */
	public static <C extends UIComponent> void validateHasDirectParent(UIComponent component, Class<C> parentType) {
		if (!isDevelopment()) {
			return;
		}

		if (!parentType.isInstance(component.getParent())) {
			throw new IllegalStateException(format(
				ERROR_MISSING_DIRECT_PARENT, component.getClass().getSimpleName(), parentType));
		}
	}

	/**
	 * Validate in development stage if the given component has no parent of given parent type.
	 * @param <C> The generic component type.
	 * @param component The component to be validated.
	 * @param parentType The parent type to be checked.
	 * @throws IllegalStateException When the given component does have a parent of the given type.
	 * @since 2.5
	 */
	public static <C extends UIComponent> void validateHasNoParent(UIComponent component, Class<C> parentType) {
		if (!isDevelopment()) {
			return;
		}

		if (getClosestParent(component, parentType) != null) {
			throw new IllegalStateException(format(
				ERROR_ILLEGAL_PARENT, component.getClass().getSimpleName(), parentType));
		}
	}

	/**
	 * Validate in development stage if the given component has at least a child of given child type.
	 * @param <C> The generic component type.
	 * @param component The component to be validated.
	 * @param childType The child type to be checked.
	 * @throws IllegalStateException When the given component doesn't have any children of the given type.
	 * @since 2.5
	 */
	public static <C extends UIComponent> void validateHasChild(UIComponent component, Class<C> childType) {
		if (!isDevelopment()) {
			return;
		}

		if (findComponentsInChildren(component, childType).isEmpty()) {
			throw new IllegalStateException(format(
				ERROR_MISSING_CHILD, component.getClass().getSimpleName(), childType));
		}
	}

	/**
	 * Validate in development stage if the given component has only children of given child type.
	 * @param <C> The generic component type.
	 * @param component The component to be validated.
	 * @param childType The child type to be checked.
	 * @throws IllegalStateException When the given component has children of a different type.
	 * @since 2.5
	 */
	public static <C extends UIComponent> void validateHasOnlyChildren(UIComponent component, Class<C> childType) {
		if (!isDevelopment() || component.getChildCount() == 0) {
			return;
		}

		StringBuilder childClassNames = new StringBuilder();

		for (UIComponent child : component.getChildren()) {
			if (!childType.isAssignableFrom(child.getClass())) {
				if (childClassNames.length() > 0) {
					childClassNames.append(", ");
				}

				childClassNames.append(child.getClass().getName());
			}
		}

		if (childClassNames.length() > 0) {
			throw new IllegalStateException(format(
				ERROR_ILLEGAL_CHILDREN, component.getClass().getSimpleName(), childType, childClassNames));
		}
	}

	/**
	 * Validate in development stage if the given component has no children.
	 * @param component The component to be validated.
	 * @throws IllegalStateException When the given component has any children.
	 */
	public static void validateHasNoChildren(UIComponent component) {
		if (!isDevelopment()) {
			return;
		}

		if (component.getChildCount() > 0) {
			StringBuilder childClassNames = new StringBuilder();

			for (UIComponent child : component.getChildren()) {
				if (childClassNames.length() > 0) {
					childClassNames.append(", ");
				}

				childClassNames.append(child.getClass().getName());
			}

			throw new IllegalStateException(format(
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
	 * Use {@link UIComponent#findComponent(String)} and ignore the potential {@link IllegalArgumentException} by
	 * returning null instead.
	 */
	private static UIComponent findComponentIgnoringIAE(UIComponent parent, String clientId) {
		try {
			return parent.findComponent(stripIterationIndexFromClientId(clientId));
		}
		catch (IllegalArgumentException ignore) {
			logger.log(FINEST, "Ignoring thrown exception; this may occur when view has changed by for example a successful navigation.", ignore);
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
	 * Get all behavior listeners of given behavior event from the given component.
	 */
	@SuppressWarnings("unchecked")
	private static List<BehaviorListener> getBehaviorListeners(ClientBehaviorHolder component, String behaviorEvent) {
		List<ClientBehavior> behaviors = component.getClientBehaviors().get(behaviorEvent);

		if (behaviors == null) {
			return emptyList();
		}

		List<BehaviorListener> allListeners = new ArrayList<>();

		for (ClientBehavior behavior : behaviors) {
			List<BehaviorListener> listeners = getField(BehaviorBase.class, List.class, behavior);

			if (listeners != null) {
				allListeners.addAll(listeners);
			}
		}

		return allListeners;
	}

	/**
	 * Get first matching field of given field type from the given class type and get the value from the given instance.
	 * (this is a rather specific helper for getBehaviorListeners() and getActionExpressionsAndListeners() and may not
	 * work in other cases).
	 */
	@SuppressWarnings("unchecked")
	private static <C, F> F getField(Class<? extends C> classType, Class<F> fieldType, C instance) {
		for (Class<?> type = classType; type != Object.class; type = type.getSuperclass()) {
			for (Field field : type.getDeclaredFields()) {
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

		private UIViewRoot temporaryView;

		public TemporaryViewFacesContext(FacesContext wrapped, UIViewRoot temporaryView) {
			super(wrapped);
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

	}

}