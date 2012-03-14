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
package org.omnifaces.component.tree;

import java.util.HashMap;
import java.util.Map;

import javax.el.ValueExpression;
import javax.faces.component.FacesComponent;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UINamingContainer;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;

import org.omnifaces.component.EditableValueHolderStateHelper;
import org.omnifaces.model.tree.ListTreeModel;
import org.omnifaces.model.tree.TreeModel;
import org.omnifaces.util.Components;

/**
 * <strong>Tree</strong> is an {@link UIComponent} that supports data binding to a tree of data objects represented by
 * a {@link TreeModel} instance, which is the current value of this component itself (typically established via a
 * {@link ValueExpression}). During iterative processing over the nodes of tree in the tree model, the object for the
 * current node is exposed as a request attribute under the key specified by the <code>var</code> attribute. The node
 * itself is exposed as a request attribute under the key specified by the <code>varNode</code> attribute.
 * <p>
 * Only children of type {@link TreeNode} are allowed and processed by this component.
 * <p>
 * This component does not have a renderer since it does not render any markup by itself. This allows the developers to
 * have full control over the markup of the tree by declaring the appropriate JSF components or HTML elements in the
 * markup. Here is a basic usage example:
 * <pre>
 * &lt;o:tree value="#{bean.treeModel}" var="item" varNode="node"&gt;
 *   &lt;o:treeNode&gt;
 *     &lt;ul&gt;
 *       &lt;o:treeNodeItem&gt;
 *         &lt;li&gt;
 *           #{node.index} #{item.someProperty}
 *           &lt;o:treeInsertChildren /&gt;
 *         &lt;/li&gt;
 *       &lt;/o:treeNodeItem&gt;
 *     &lt;/ul&gt;
 *   &lt;/o:treeNode&gt;
 * &lt;/o:tree&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 * @see TreeModel
 * @see TreeNode
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
	private static final String ERROR_NESTING_DISALLOWED =
		"Nesting Tree components is disallowed. Use TreeNode instead to markup specific levels.";
	private static final String ERROR_NO_CHILDREN =
		"Tree must have children of type TreeNode. Currently none are encountered.";
	private static final String ERROR_INVALID_CHILD =
		"Tree accepts only children of type TreeNode. Encountered child of type '%s'.";
	private static final String ERROR_DUPLICATE_NODE =
		"TreeNode with level '%s' is already declared. Choose a different level or remove it.";

	private enum PropertyKeys {
		// Cannot be uppercased. They have to exactly match the attribute names.
		value, var, varNode;
	}

	// Variables ------------------------------------------------------------------------------------------------------

	private TreeModel model;
	private Map<Integer, TreeNode> nodes;
	private TreeModel currentModelNode;

	// UIComponent overrides ------------------------------------------------------------------------------------------

	/**
	 * An override which appends the index of the current model node to the client ID chain, if any available.
	 * @see TreeModel#getIndex()
	 */
	@Override
	public String getContainerClientId(FacesContext context) {
		String containerClientId = super.getContainerClientId(context);

		if (currentModelNode != null) {
			containerClientId = new StringBuilder(containerClientId)
				.append(UINamingContainer.getSeparatorChar(context))
				.append(currentModelNode.getIndex())
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

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Validate the component hierarchy.
	 * @throws IllegalArgumentException When this component is nested in another {@link Tree}, or when there aren't any
	 * children of type {@link TreeNode}.
	 */
	@Override
	protected void validateHierarchy() {
		if (Components.getClosestParent(this, Tree.class) != null) {
			throw new IllegalArgumentException(ERROR_NESTING_DISALLOWED);
		}

		if (getChildCount() == 0) {
			throw new IllegalArgumentException(ERROR_NO_CHILDREN);
		}
	}

	/**
	 * Set the root node and delegate the call to {@link #processTreeNode(FacesContext, PhaseId)}.
	 * @param context The faces context to work with.
	 * @param phaseId The current phase ID.
	 */
	@Override
	protected void process(FacesContext context, PhaseId phaseId) {
		if (!isRendered()) {
			return;
		}

		if (phaseId == PhaseId.APPLY_REQUEST_VALUES || phaseId == PhaseId.RENDER_RESPONSE) {
			prepareNodes();
			prepareModel();
		}

		Object[] originalVars = captureOriginalVars(context);

		try {
			setCurrentModelNode(context, model);
			processTreeNode(context, phaseId);
		}
		finally {
			setVars(context, originalVars);
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
		if (!currentModelNode.isLeaf()) {
			TreeNode treeNode = nodes.get(currentModelNode.getLevel());

			if (treeNode == null) {
				treeNode = nodes.get(null);
			}

			if (treeNode == null) {
				return;
			}

			treeNode.process(context, phaseId);
		}
	}

	/**
	 * Prepare the tree nodes by finding direct {@link TreeNode} children and collecting them by their level attribute.
	 * @throws IllegalArgumentException When a direct child component isn't of type {@link TreeNode}, or when there are
	 * multiple {@link TreeNode} components with the same level.
	 */
	private void prepareNodes() {
		nodes = new HashMap<Integer, TreeNode>(getChildCount());

		for (UIComponent child : getChildren()) {
			if (child instanceof TreeNode) {
				TreeNode node = (TreeNode) child;

				if (nodes.put(node.getLevel(), node) != null) {
					throw new IllegalArgumentException(String.format(ERROR_DUPLICATE_NODE, node.getLevel()));
				}
			}
			else {
				throw new IllegalArgumentException(String.format(ERROR_INVALID_CHILD, child.getClass().getName()));
			}
		}
	}

	/**
	 * Prepare the tree model associated with the <code>value</code> attribute.
	 * @throws IllegalArgumentException When the <code>value</code> isn't of type {@link TreeModel}.
	 */
	private void prepareModel() {
		Object value = getValue();

		if (value == null) {
			model = new ListTreeModel();
		}
		else if (value instanceof TreeModel) {
			model = (TreeModel) value;
		}
		else {
			throw new IllegalArgumentException(String.format(ERROR_INVALID_MODEL, value.getClass().getName()));
		}
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
		return getStateHelper().eval(PropertyKeys.value);
	}

	/**
	 * Sets the tree model.
	 * @param value The tree model.
	 */
	public void setValue(Object value) {
		getStateHelper().put(PropertyKeys.value, value);
	}

	/**
	 * Returns the name of the request attribute which exposes the wrapped data of the current node of the tree model.
	 * @return The name of the request attribute which exposes the wrapped data of the current node of the tree model.
	 */
	public String getVar() {
		return (String) getStateHelper().eval(PropertyKeys.var);
	}

	/**
	 * Sets the name of the request attribute which exposes the wrapped data of the current node of the tree model.
	 * @param var The name of the request attribute which exposes the wrapped data of the current node of the tree
	 * model.
	 */
	public void setVar(String var) {
		getStateHelper().put(PropertyKeys.var, var);
	}

	/**
	 * Returns the name of the request attribute which exposes the current node of the tree model.
	 * @return The name of the request attribute which exposes the current node of the tree model.
	 */
	public String getVarNode() {
		return (String) getStateHelper().eval(PropertyKeys.varNode);
	}

	/**
	 * Sets the name of the request attribute which exposes the current node of the tree model.
	 * @param varNode The name of the request attribute which exposes the current node of the tree model.
	 */
	public void setVarNode(String varNode) {
		getStateHelper().put(PropertyKeys.varNode, varNode);
	}

}