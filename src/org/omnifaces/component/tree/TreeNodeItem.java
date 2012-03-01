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

import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;

import org.omnifaces.model.tree.TreeModel;
import org.omnifaces.util.Components;

/**
 * <strong>TreeNodeItem</strong> is an {@link UIComponent} that represents a single child tree node within a parent
 * {@link TreeNode} component. Within this component, the <code>var</code> attribute of the parent {@link Tree}
 * component will expose the child tree node.
 * <p>
 * This component allows a child component of type {@link TreeInsertChildren} which indicates the place to insert
 * the children of the current child tree node recursively by a {@link TreeNode} component associated with the
 * children's level in the same parent {@link Tree} component.
 *
 * @author Bauke Scholtz
 * @see TreeNode
 * @see TreeInsertChildren
 */
@FacesComponent(TreeNodeItem.COMPONENT_TYPE)
public class TreeNodeItem extends TreeFamily {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The standard component type. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.tree.TreeNodeItem";

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String ERROR_INVALID_PARENT =
		"TreeNodeItem must have a parent of type TreeNode, but it cannot be found.";
	private static final String ERROR_NESTING_DISALLOWED =
		"Nesting TreeNodeItem components is disallowed. Use TreeNode instead to markup specific levels.";

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Validate the component hierarchy.
	 * @throws IllegalArgumentException When there is no parent of type {@link Tree}, or when this component is nested
	 * in another {@link TreeNodeItem}.
	 */
	@Override
	protected void validateHierarchy() {
		if (Components.getClosestParent(this, Tree.class) == null) {
			throw new IllegalArgumentException(ERROR_INVALID_PARENT);
		}

		if (Components.getClosestParent(this, TreeNodeItem.class) != null) {
			throw new IllegalArgumentException(ERROR_NESTING_DISALLOWED);
		}
	}

	/**
	 * Loop over children of the current model node, set the child as the current morel node and continue processing
	 * this component according to the rules of the given phase ID.
	 * @param context The faces context to work with.
	 * @param phaseId The current phase ID.
	 * @see Tree#setCurrentModelNode(FacesContext, TreeModel)
	 */
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" }) // For TreeModel. We don't care about its actual type anyway.
	protected void process(FacesContext context, PhaseId phaseId) {
		if (!isRendered() || getChildCount() == 0) {
			return;
		}

		Tree tree = Components.getClosestParent(this, Tree.class);

		for (TreeModel childModelNode : (Iterable<TreeModel>) tree.getCurrentModelNode()) {
			tree.setCurrentModelNode(context, childModelNode);
			processSuper(context, phaseId);
		}
	}

}