/*
 * Copyright 2020 OmniFaces
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
package org.omnifaces.component.tree;

import static java.lang.String.format;
import static org.omnifaces.util.Components.validateHasChild;
import static org.omnifaces.util.Components.validateHasNoParent;
import static org.omnifaces.util.Components.validateHasOnlyChildren;

import java.util.HashMap;
import java.util.Map;

import javax.el.ValueExpression;
import javax.faces.component.FacesComponent;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UINamingContainer;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitHint;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;
import javax.faces.event.FacesEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PostValidateEvent;
import javax.faces.event.PreValidateEvent;

import org.omnifaces.component.EditableValueHolderStateHelper;
import org.omnifaces.event.FacesEventWrapper;
import org.omnifaces.model.tree.AbstractTreeModel;
import org.omnifaces.model.tree.ListTreeModel;
import org.omnifaces.model.tree.SortedTreeModel;
import org.omnifaces.model.tree.TreeModel;
import org.omnifaces.util.Callback;
import org.omnifaces.util.State;

/**
 * <p>
 * The <code>&lt;o:tree&gt;</code> allows the developers to have full control over the markup of a tree
 * hierarchy by declaring the appropriate JSF components or HTML elements in the markup. The <code>&lt;o:tree&gt;</code>
 * does namely not render any HTML markup by itself.
 * <p>
 * The component value must point to a tree of data objects represented by a {@link TreeModel} instance, typically
 * established via a {@link ValueExpression}. During iterative processing over the nodes of tree in the tree model,
 * the object for the current node is exposed as a request attribute under the key specified by the <code>var</code>
 * attribute. The node itself is exposed as a request attribute under the key specified by the <code>varNode</code>
 * attribute.
 * <p>
 * The <code>&lt;o:tree&gt;</code> tag supports only child tags of type <code>&lt;o:treeNode&gt;</code>, representing
 * parent tree nodes. There can be multiple <code>&lt;o:treeNode&gt;</code> tags, each representing a separate parent
 * tree node level, so that different markup could be declared for each tree node level, if necessary. The
 * <code>&lt;o:treeNode&gt;</code> tag in turn supports child tag <code>&lt;o:treeNodeItem&gt;</code> which represents
 * each child of the current parent tree node. The <code>&lt;o:treeNodeItem&gt;</code> in turn supports child tag
 * <code>&lt;o:treeInsertChildren&gt;</code> which represents the insertion point of the grand children.
 * <p>
 * Here is a basic usage example where each parent tree node level is treated the same way via a single
 * <code>&lt;o:treeNode&gt;</code>:
 * <pre>
 * &lt;o:tree value="#{bean.treeModel}" var="item" varNode="node"&gt;
 *     &lt;o:treeNode&gt;
 *         &lt;ul&gt;
 *             &lt;o:treeNodeItem&gt;
 *                 &lt;li&gt;
 *                     #{node.index} #{item.someProperty}
 *                     &lt;o:treeInsertChildren /&gt;
 *                 &lt;/li&gt;
 *             &lt;/o:treeNodeItem&gt;
 *         &lt;/ul&gt;
 *     &lt;/o:treeNode&gt;
 * &lt;/o:tree&gt;
 * </pre>
 *
 * <h3>treeNode</h3>
 * <p>
 * The <code>&lt;o:treeNode&gt;</code> represents the parent tree node. Within this component, the <code>var</code>
 * attribute of the <code>&lt;o:tree&gt;</code> will expose the parent tree node. Each of its children is processed by
 * <code>&lt;o:treeNodeItem&gt;</code> on which the <code>var</code> attribute of the <code>&lt;o:tree&gt;</code> in
 * turn exposes each child of the parent tree node.
 * <p>
 * The optional <code>level</code> attribute can be used to specify for which tree node level as obtained by
 * {@link TreeModel#getLevel()} the <code>&lt;o:treeNode&gt;</code> should be rendered. The root tree node has level 0.
 * If the <code>level</code> attribute is unspecified, then the <code>&lt;o:treeNode&gt;</code> will be rendered for any
 * tree node level which hasn't already a <code>&lt;o:treeNode level="x"&gt;</code> specified.
 *
 * <h3>treeNodeItem</h3>
 * <p>
 * The <code>&lt;o:treeNodeItem&gt;</code> represents the child item of the parent tree note as represented by
 * <code>&lt;o:treeNode&gt;</code>. Within this component, the <code>var</code> attribute of the parent
 * <code>&lt;o:tree&gt;</code> component will expose the child tree node.
 * <p>
 * Within <code>&lt;o:treeNodeItem&gt;</code> you can use <code>&lt;o:treeInsertChildren&gt;</code> to declare the
 * place where to recursively render the <code>&lt;o:treeNode&gt;</code> whereby the current child item is in turn
 * interpreted as a parent tree node (i.e. where you'd like to insert the grand-children).
 *
 * <h3>treeInsertChildren</h3>
 * <p>
 * The <code>&lt;o:treeInsertChildren&gt;</code> represents the insertion point for the grand children. This is in turn
 * further interpreted as <code>&lt;o:treeNode&gt;</code>.
 *
 * @author Bauke Scholtz
 * @see TreeNode
 * @see TreeNodeItem
 * @see TreeInsertChildren
 * @see TreeFamily
 * @see TreeModel
 * @see AbstractTreeModel
 * @see ListTreeModel
 * @see SortedTreeModel
 */
@FacesComponent(Tree.COMPONENT_TYPE)
@SuppressWarnings("rawtypes") // For TreeModel. We don't care about its actual type anyway.
public class Tree extends TreeFamily implements NamingContainer {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The standard component type. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.tree.Tree";

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String ERROR_EXPRESSION_DISALLOWED =
		"A value expression is disallowed on 'var' and 'varNode' attributes of Tree.";
	private static final String ERROR_INVALID_MODEL =
		"Tree accepts only model of type TreeModel. Encountered model of type '%s'.";
	private static final String ERROR_DUPLICATE_NODE =
		"TreeNode with level '%s' is already declared. Choose a different level or remove it.";

	private enum PropertyKeys {
		// Cannot be uppercased. They have to exactly match the attribute names.
		value, var, varNode;
	}

	// Variables ------------------------------------------------------------------------------------------------------

	private final State state = new State(getStateHelper());
	private TreeModel currentModel;
	private Map<Integer, TreeNode> nodes;
	private TreeModel currentModelNode;

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * An override which appends the index of the current model node to the client ID chain, if any available.
	 * @see TreeModel#getIndex()
	 */
	@Override
	public String getContainerClientId(FacesContext context) {
		String containerClientId = super.getContainerClientId(context);
		String currentModelNodeIndex = (currentModelNode != null) ? currentModelNode.getIndex() : null;

		if (currentModelNodeIndex != null) {
			containerClientId = new StringBuilder(containerClientId)
				.append(UINamingContainer.getSeparatorChar(context))
				.append(currentModelNodeIndex)
				.toString();
		}

		return containerClientId;
	}

	/**
	 * An override which checks if this isn't been invoked on <code>var</code> or <code>varNode</code> attribute.
	 * Finally it delegates to the super method.
	 * @throws IllegalArgumentException When this value expression is been set on <code>var</code> or
	 * <code>varNode</code> attribute.
	 */
	@Override
	public void setValueExpression(String name, ValueExpression binding) {
		if (PropertyKeys.var.toString().equals(name) || PropertyKeys.varNode.toString().equals(name)) {
			throw new IllegalArgumentException(ERROR_EXPRESSION_DISALLOWED);
		}

		super.setValueExpression(name, binding);
	}

	/**
	 * An override which wraps the given faces event in a specific faces event which remembers the current model node.
	 */
	@Override
	public void queueEvent(FacesEvent event) {
		super.queueEvent(new TreeFacesEvent(event, this, getCurrentModelNode()));
	}

	/**
	 * Validate the component hierarchy.
	 * @throws IllegalStateException When this component is nested in another {@link Tree}, or when there aren't any
	 * children of type {@link TreeNode}.
	 */
	@Override
	protected void validateHierarchy() {
		validateHasNoParent(this, Tree.class);
		validateHasChild(this, TreeNode.class);
		validateHasOnlyChildren(this, TreeNode.class);
	}

	/**
	 * Set the root node as current node and delegate the call to {@link #processTreeNode(FacesContext, PhaseId)}.
	 * @param context The faces context to work with.
	 * @param phaseId The current phase ID.
	 */
	@Override
	protected void process(FacesContext context, PhaseId phaseId) {
		if (!isRendered()) {
			return;
		}

		final boolean processValidations = (phaseId == PhaseId.PROCESS_VALIDATIONS);

		process(context, getModel(phaseId), () -> {
			if (processValidations) {
		        context.getApplication().publishEvent(context, PreValidateEvent.class, this);
			}

			processTreeNode(context, phaseId);

			if (processValidations) {
		        context.getApplication().publishEvent(context, PostValidateEvent.class, this);
			}

			return null;
		});
	}

	/**
	 * Set the root node as current node and delegate the call to {@link #visitTreeNode(VisitContext, VisitCallback)}.
	 * @param context The visit context to work with.
	 * @param callback The visit callback to work with.
	 * @return The visit result.
	 */
	@Override
	public boolean visitTree(VisitContext context, VisitCallback callback) {
		if (!isVisitable(context)) {
			return false;
		}

		TreeModel model = getModel(PhaseId.ANY_PHASE);

		if (model.isLeaf()) {
			return super.visitTree(context, callback);
		}

		return process(context.getFacesContext(), model, () -> {
			VisitResult result = context.invokeVisitCallback(Tree.this, callback);

			if (result == VisitResult.COMPLETE) {
				return true;
			}

			if (result == VisitResult.ACCEPT && !context.getSubtreeIdsToVisit(Tree.this).isEmpty()) {
				return visitTreeNode(context, callback);
			}

			return false;
		});
	}

	@Override
	protected boolean isVisitable(VisitContext context) {
		return super.isVisitable(context) && !context.getHints().contains(VisitHint.SKIP_ITERATION);
	}

	/**
	 * If the given event is an instance of the specific faces event which was created during our
	 * {@link #queueEvent(FacesEvent)}, then extract the node from it and set it as current node and delegate the call
	 * to the wrapped faces event.
	 */
	@Override
	public void broadcast(FacesEvent event) {
		if (event instanceof TreeFacesEvent) {
			FacesContext context = FacesContext.getCurrentInstance();
			TreeFacesEvent treeEvent = (TreeFacesEvent) event;
			FacesEvent wrapped = treeEvent.getWrapped();

			process(context, treeEvent.getNode(), () -> {
				UIComponent source = wrapped.getComponent();
				pushComponentToEL(context, getCompositeComponentParent(source));

				try {
					source.broadcast(wrapped);
				}
				finally {
					popComponentFromEL(context);
				}

				return null;
			});
		}
		else {
			super.broadcast(event);
		}
	}

	/**
	 * If the current model node isn't a leaf (i.e. it has any children), then obtain the {@link TreeNode} associated
	 * with the level of the current model node. If it isn't null, then process it according to the rules of the given
	 * phase ID. This method is also called by {@link TreeInsertChildren#process(FacesContext, PhaseId)}.
	 * @param context The faces context to work with.
	 * @param phaseId The current phase ID.
	 * @see TreeModel#isLeaf()
	 * @see TreeModel#getLevel()
	 * @see TreeInsertChildren
	 */
	protected void processTreeNode(FacesContext context, PhaseId phaseId) {
		processTreeNode(phaseId, treeNode -> {
			if (treeNode != null) {
				treeNode.process(context, phaseId);
			}

			return null;
		});
	}

	/**
	 * If the current model node isn't a leaf (i.e. it has any children), then obtain the {@link TreeNode} associated
	 * with the level of the current model node. If it isn't null, then visit it according to the given visit context
	 * and callback. This method is also called by {@link TreeInsertChildren#visitTree(VisitContext, VisitCallback)}.
	 * @param context The visit context to work with.
	 * @param callback The visit callback to work with.
	 * @return <code>true</code> if the visit is complete.
	 * @see TreeModel#isLeaf()
	 * @see TreeModel#getLevel()
	 * @see TreeInsertChildren
	 */
	protected boolean visitTreeNode(VisitContext context, VisitCallback callback) {
		return processTreeNode(PhaseId.ANY_PHASE, treeNode -> {
			if (treeNode != null) {
				return treeNode.visitTree(context, callback);
			}

			return false;
		});
	}

	/**
	 * Convenience method to handle {@link #process(FacesContext, PhaseId)},
	 * {@link #visitTree(VisitContext, VisitCallback)} and {@link #broadcast(FacesEvent)} without code duplication.
	 * @param context The faces context to work with.
	 * @param node The current tree model node.
	 * @param callback The callback to be invoked.
	 * @return The callback result.
	 */
	private <R> R process(FacesContext context, TreeModel node, Callback.Returning<R> callback) {
		Object[] originalVars = captureOriginalVars(context);
		TreeModel originalModelNode = currentModelNode;
		pushComponentToEL(context, null);

		try {
			setCurrentModelNode(context, node);
			return callback.invoke();
		}
		finally {
			popComponentFromEL(context);
			setCurrentModelNode(context, originalModelNode);
			setVars(context, originalVars);
		}
	}

	/**
	 * Convenience method to handle both {@link #processTreeNode(FacesContext, PhaseId)} and
	 * {@link #visitTreeNode(VisitContext, VisitCallback)} without code duplication.
	 * @param phaseId The current phase ID.
	 * @param callback The callback to be invoked.
	 * @return The callback result.
	 */
	private <R> R processTreeNode(PhaseId phaseId, Callback.ReturningWithArgument<R, TreeNode> callback) {
		TreeNode treeNode = null;

		if (!currentModelNode.isLeaf()) {
			treeNode = getNodes(phaseId).get(currentModelNode.getLevel());

			if (treeNode == null) {
				treeNode = getNodes(phaseId).get(null);
			}
		}

		return callback.invoke(treeNode);
	}

	/**
	 * Returns the tree nodes by finding direct {@link TreeNode} children and collecting them by their level attribute.
	 * @param phaseId The current phase ID.
	 * @return The tree nodes.
	 * @throws IllegalStateException When there are multiple {@link TreeNode} components with the same level.
	 */
	private Map<Integer, TreeNode> getNodes(PhaseId phaseId) {
		if (phaseId == PhaseId.RENDER_RESPONSE || nodes == null) {
			nodes = new HashMap<>(getChildCount());

			for (UIComponent child : getChildren()) {
				TreeNode node = (TreeNode) child;

				if (nodes.put(node.getLevel(), node) != null) {
					throw new IllegalStateException(format(ERROR_DUPLICATE_NODE, node.getLevel()));
				}
			}
		}

		return nodes;
	}

	/**
	 * Returns the tree model associated with the <code>value</code> attribute.
	 * @param phaseId The current phase ID.
	 * @return The tree model.
	 * @throws IllegalArgumentException When the <code>value</code> isn't of type {@link TreeModel}.
	 */
	private TreeModel getModel(PhaseId phaseId) {
		if (phaseId == PhaseId.RENDER_RESPONSE || currentModel == null) {
			Object value = getValue();

			if (value == null) {
				currentModel = new ListTreeModel();
			}
			else if (value instanceof TreeModel) {
				currentModel = (TreeModel) value;
			}
			else {
				throw new IllegalArgumentException(format(ERROR_INVALID_MODEL, value.getClass().getName()));
			}
		}

		return currentModel;
	}

	/**
	 * Capture the original values of the request attributes associated with the component attributes <code>var</code>
	 * and <code>varNode</code>.
	 * @param context The faces context to work with.
	 * @return An object array with the two values.
	 */
	private Object[] captureOriginalVars(FacesContext context) {
		Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
		String[] names = { getVar(), getVarNode() };
		Object[] vars = new Object[names.length];

		for (int i = 0; i < names.length; i++) {
			if (names[i] != null) {
				vars[i] = requestMap.get(names[i]);
			}
		}

		return vars;
	}

	/**
	 * Set the values associated with the <code>var</code> and <code>varNode</code> attributes.
	 * @param context The faces context to work with.
	 * @param vars An object array with the two values.
	 */
	private void setVars(FacesContext context, Object... vars) {
		Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
		String[] names = { getVar(), getVarNode() };

		for (int i = 0; i < names.length; i++) {
			if (names[i] != null) {
				if (vars[i] != null) {
					requestMap.put(names[i], vars[i]);
				}
				else {
					requestMap.remove(names[i]);
				}
			}
		}
	}

	// Internal getters/setters ---------------------------------------------------------------------------------------

	/**
	 * Sets the current node of the tree model. Its wrapped data will be set as request attribute associated with the
	 * <code>var</code> attribute, if any. The node itself will also be set as request attribute associated with the
	 * <code>varNode</code> attribute, if any.
	 * @param context The faces context to work with.
	 * @param currentModelNode The current node of the tree model.
	 */
	protected void setCurrentModelNode(FacesContext context, TreeModel currentModelNode) {

		// Save state of any child input fields of previous model node before setting new model node.
		EditableValueHolderStateHelper.save(context, getStateHelper(), getFacetsAndChildren());

		this.currentModelNode = currentModelNode;
		setVars(context, (currentModelNode != null ? currentModelNode.getData() : null), currentModelNode);

		// Restore any saved state of any child input fields of current model node before continuing.
		EditableValueHolderStateHelper.restore(context, getStateHelper(), getFacetsAndChildren());
	}

	/**
	 * Returns the current node of the tree model.
	 * @return The current node of the tree model.
	 */
	protected TreeModel getCurrentModelNode() {
		return currentModelNode;
	}

	// Attribute getters/setters --------------------------------------------------------------------------------------

	/**
	 * Returns the tree model.
	 * @return The tree model
	 */
	public Object getValue() {
		return state.get(PropertyKeys.value);
	}

	/**
	 * Sets the tree model.
	 * @param value The tree model.
	 */
	public void setValue(Object value) {
		state.put(PropertyKeys.value, value);
	}

	/**
	 * Returns the name of the request attribute which exposes the wrapped data of the current node of the tree model.
	 * @return The name of the request attribute which exposes the wrapped data of the current node of the tree model.
	 */
	public String getVar() {
		return state.get(PropertyKeys.var);
	}

	/**
	 * Sets the name of the request attribute which exposes the wrapped data of the current node of the tree model.
	 * @param var The name of the request attribute which exposes the wrapped data of the current node of the tree
	 * model.
	 */
	public void setVar(String var) {
		state.put(PropertyKeys.var, var);
	}

	/**
	 * Returns the name of the request attribute which exposes the current node of the tree model.
	 * @return The name of the request attribute which exposes the current node of the tree model.
	 */
	public String getVarNode() {
		return state.get(PropertyKeys.varNode);
	}

	/**
	 * Sets the name of the request attribute which exposes the current node of the tree model.
	 * @param varNode The name of the request attribute which exposes the current node of the tree model.
	 */
	public void setVarNode(String varNode) {
		state.put(PropertyKeys.varNode, varNode);
	}

	// Nested classes -------------------------------------------------------------------------------------------------

	/**
	 * This faces event implementation remembers the current model node at the moment the faces event was queued.
	 *
	 * @author Bauke Scholtz
	 */
	private static class TreeFacesEvent extends FacesEventWrapper {

		private static final long serialVersionUID = 1L;

		private TreeModel node;

		public TreeFacesEvent(FacesEvent wrapped, Tree tree, TreeModel node) {
			super(wrapped, tree);
			this.node = node;
		}

		public TreeModel getNode() {
			return node;
		}

	}

}