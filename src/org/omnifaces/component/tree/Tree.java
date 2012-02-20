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

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.component.UINamingContainer;
import javax.faces.context.FacesContext;
import javax.faces.render.Renderer;

import org.omnifaces.model.tree.ListTreeModel;
import org.omnifaces.model.tree.TreeModel;
import org.omnifaces.util.Components;

/**
 * <strong>Tree</strong> is a {@link UIComponent} that supports data binding to a tree of data objects represented by
 * a {@link TreeModel} instance, which is the current value of this component itself (typically established via a
 * {@link ValueExpression}). During iterative processing over the nodes of tree in the tree model, the object for the
 * current node is exposed as a request attribute under the key specified by the <code>var</code> attribute. The node
 * itself is exposed as a request attribute under the key specified by the <code>varNode</code> attribute.
 * <p>
 * Only children of type {@link TreeNode} are allowed and processed by this component.
 * <p>
 * This component does not have a renderer since it does not render any markup by itself. This allows the developers to
 * have full control over the markup of the tree by declaring the appropriate JSF components or HTML elements in the
 * markup.
 *
 * @author Bauke Scholtz
 * @see TreeModel
 * @see TreeNode
 *
 * TODO: Implement processXxx() methods so that we can have UIInput components as children.
 */
@SuppressWarnings({ "rawtypes", "unchecked" }) // For TreeModel. We don't care about its actual type anyway.
public class Tree extends UINamingContainer {

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String ERROR_EXPRESSION_DISALLOWED =
		"A value expression is disallowed on 'var' and 'varNode' attributes.";
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
		value, var, varNode;
	}

	// Public constants -----------------------------------------------------------------------------------------------

	/** The standard component type. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.Tree";

	/** The standard component family. */
	public static final String COMPONENT_FAMILY = "org.omnifaces.component";

	// Properties -----------------------------------------------------------------------------------------------------

	private TreeModel model;
	private TreeModel currentModelNode;
	private Map<Integer, TreeNode> nodes;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new {@link Tree} instance.
	 */
	public Tree() {
		setRendererType(null); // This component doesn't render anything by itselves.
	}

	// UIComponent overrides ------------------------------------------------------------------------------------------

	@Override
	public String getContainerClientId(FacesContext context) {
		String containerClientId = super.getContainerClientId(context);

		if (currentModelNode != null) {
	        return new StringBuilder(containerClientId)
		    	.append(UINamingContainer.getSeparatorChar(context))
		    	.append(currentModelNode.getIndex())
		    	.toString();
		}

		return containerClientId;
	}

	@Override
	public String getFamily() {
		return COMPONENT_FAMILY;
	}

	/**
	 * Returns <code>true</code> even though this component doesn't have a {@link Renderer}, because we want to encode
	 * all children anyway.
	 */
	@Override
	public boolean getRendersChildren() {
		return true;
	}

	/**
	 * An override which sets the model to <code>null</code> whenever the <code>value</code> attribute is been set and
	 * checks if this isn't been invoked on <code>var</code> or <code>varNode</code> attribute. Finally it delegates to
	 * the super method.
	 * @throws IllegalArgumentException If this value expression is been set on <code>var</code> or <code>varNode</code>
	 * attribute.
	 */
	@Override
	public void setValueExpression(String name, ValueExpression binding) {
		if ("value".equals(name)) {
			this.model = null;
		}
		else if ("var".equals(name) || "varNode".equals(name)) {
			throw new IllegalArgumentException(ERROR_EXPRESSION_DISALLOWED);
		}

		super.setValueExpression(name, binding);
	}

	/**
	 * Checks if there are any children of {@link TreeNode} and collect them in a mapping by their level attribute.
	 * @throws IllegalArgumentException When this component is nested in another {@link Tree}, or when there aren't any
	 * children of type {@link TreeNode}, or when a child isn't of type {@link TreeNode} or when there are multiple
	 * {@link TreeNode} children with the same level attribute.
	 */
	@Override
	public void encodeBegin(FacesContext context) throws IOException {
		if (Components.getClosestParent(this, Tree.class) != null) {
			throw new IllegalArgumentException(ERROR_NESTING_DISALLOWED);
		}

		if (getChildCount() == 0) {
			throw new IllegalArgumentException(ERROR_NO_CHILDREN);
		}

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

		super.encodeBegin(context);
	}

	/**
	 * If this component is to be rendered, set the associated <code>var</code> and/or <code>varNode</code> values and
	 * encode the current {@link TreeNode}.
	 * @see #setCurrentModelNode(TreeModel)
	 * @see #encodeTreeNode(FacesContext)
	 */
	@Override
	public void encodeChildren(FacesContext context) throws IOException {
		if (!isRendered()) {
			return;
		}

		setCurrentModelNode(getModel());
		encodeTreeNode(context);
		setCurrentModelNode(null);
	}

	// Internal actions -----------------------------------------------------------------------------------------------

	/**
	 * If the current model node isn't a leaf (i.e. it has any children), then obtain the {@link TreeNode} associated
	 * with the level of the current model node. If it isn't null, then encode it.
	 * @param context The faces context to work with.
	 * @throws IOException If something fails at I/O level.
	 * @see TreeModel#isLeaf()
	 * @see TreeModel#getLevel()
	 */
	protected void encodeTreeNode(FacesContext context) throws IOException {
		if (!currentModelNode.isLeaf()) {
			TreeNode node = nodes.get(currentModelNode.getLevel());

			if (node == null) {
				node = nodes.get(null);
			}

			if (node != null) {
				node.encodeChildren(context);
			}
		}
	}

	/**
	 * For every child of the current model node, set the associated <code>var</code> and/or <code>varNode</code> values
	 * and encode the children of the given {@link TreeNodeItem}.
	 * @param context The faces context to work with.
	 * @param treeNodeItem The tree node item to encode its children.
	 * @throws IOException If something fails at I/O level.
	 * @see #setCurrentModelNode(TreeModel)
	 */
	protected void encodeTreeNodeItem(FacesContext context, TreeNodeItem treeNodeItem) throws IOException {
		for (TreeModel child : (List<TreeModel>) currentModelNode.getChildren()) {
			setCurrentModelNode(child);
			treeNodeItem.encodeChildren(context);
			setCurrentModelNode(null);
		}
	}

	// Internal getters/setters ---------------------------------------------------------------------------------------

	/**
	 * Returns the tree model associated with the <code>value</code> attribute.
	 * @return The tree model associated with the <code>value</code> attribute.
	 * @throws IllegalArgumentException If the tree model isn't of type {@link TreeModel}.
	 */
	private TreeModel getModel() {
		if (model != null) {
			return model;
		}

		Object value = getValue();

		if (value == null) {
			model = new ListTreeModel();
		} else if (value instanceof TreeModel) {
			model = (TreeModel) value;
		} else {
			throw new IllegalArgumentException(String.format(ERROR_INVALID_MODEL, value.getClass()));
		}

		return model;
	}

	/**
	 * Sets the current node of the tree model. Its wrapped data will be set as request attribute associated with the
	 * <code>var</code> attribute, if any. The node itself will also be set as request attribute associated with the
	 * <code>varNode</code> attribute, if any.
	 * @param currentModelNode The current node of the tree model.
	 */
	private void setCurrentModelNode(TreeModel currentModelNode) {
		this.currentModelNode = currentModelNode;
		Map<String, Object> requestMap = FacesContext.getCurrentInstance().getExternalContext().getRequestMap();
		String var = getVar();
		String varNode = getVarNode();

		if (var != null) {
			requestMap.put(var, currentModelNode != null ? currentModelNode.getData() : null);
		}

		if (varNode != null) {
			requestMap.put(varNode, currentModelNode);
		}

		resetClientIds(getFacetsAndChildren());
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Reset the cached client IDs of any nested facets and children so that their client ID will be regenerated based
	 * on {@link #getContainerClientId(FacesContext)} of the current tree component.
	 * @param facetsAndChildren The iterator with all facets and children.
	 */
	private void resetClientIds(Iterator<UIComponent> facetsAndChildren) {
		while (facetsAndChildren.hasNext()) {
			UIComponent child = facetsAndChildren.next();
			child.setId(child.getId()); // This implicitly resets the cached client ID. See JSF spec 3.1.6.
			resetClientIds(child.getFacetsAndChildren());
		}
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
		model = null;
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
	 * @param var The name of the request attribute which exposes the wrapped data of the current node of the tree model.
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