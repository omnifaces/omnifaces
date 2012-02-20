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

import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;

import org.omnifaces.util.Components;

/**
 * <strong>TreeNodeItem</strong> is a {@link UIComponent} that represents a single child tree node within a parent
 * {@link TreeNode} component. Within this component, the <code>var</code> attribute of the parent {@link Tree}
 * component will expose the child tree node.
 * <p>
 * This component allows a child component of type {@link TreeInsertChildren} which indicates the place to insert
 * the children of the current child tree node recursively by a {@link TreeNode} component associated with the
 * children's level in the same parent {@link Tree} component.
 * <p>
 * This component does not have a renderer since it does not render any markup by itself.
 *
 * @author Bauke Scholtz
 * @see TreeNode
 * @see TreeInsertChildren
 */
public class TreeNodeItem extends UIComponentBase {

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String ERROR_INVALID_PARENT =
		"TreeNodeItem must have a parent of type TreeNode, but it cannot be found.";

	// Public constants -----------------------------------------------------------------------------------------------

    /** The standard component type. */
    public static final String COMPONENT_TYPE = "org.omnifaces.component.TreeNodeItem";

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new {@link TreeNodeItem} instance.
	 */
	public TreeNodeItem() {
		setRendererType(null); // This component doesn't render anything by itselves.
	}

	// UIComponent overrides ------------------------------------------------------------------------------------------

	@Override
	public String getFamily() {
		return Tree.COMPONENT_FAMILY;
	}

	/**
	 * Checks the parent component type and then delegates encoding of children to
	 * {@link Tree#encodeTreeNodeItem(FacesContext, TreeNodeItem)} as it maintains the model.
	 * @throws IllegalArgumentException If there is no parent of type {@link TreeNode}.
	 * @see Tree#encodeTreeNodeItem(FacesContext, TreeNodeItem)
	 */
	@Override
	public void encodeAll(FacesContext context) throws IOException {
	    TreeNode treeNode = Components.getClosestParent(this, TreeNode.class);

		if (treeNode == null) {
			throw new IllegalArgumentException(ERROR_INVALID_PARENT);
		}

		((Tree) treeNode.getParent()).encodeTreeNodeItem(context, this);
	}

}