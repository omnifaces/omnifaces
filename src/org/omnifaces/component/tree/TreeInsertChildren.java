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
 * <strong>TreeInsertChildren</strong> is a {@link UIComponent} that represents the insertion point for the
 * children of a parent tree node which is represented by {@link TreeNodeItem}.
 * <p>
 * This component does not allow any children.
 * <p>
 * This component does not have a renderer since it doesn't render any markup by itself.
 *
 * @author Bauke Scholtz
 * @see TreeNodeItem
 */
public class TreeInsertChildren extends UIComponentBase {

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String ERROR_INVALID_PARENT =
		"TreeInsertChildren must have a parent of type TreeNodeItem, but it cannot be found.";
	private static final String ERROR_CHILDREN_DISALLOWED =
		"TreeInsertChildren should have no children. Encountered children of types '%s'.";

	// Public constants -----------------------------------------------------------------------------------------------

    /** The standard component type. */
    public static final String COMPONENT_TYPE = "org.omnifaces.component.TreeInsertChildren";

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new {@link TreeInsertChildren} instance.
	 */
	public TreeInsertChildren() {
		setRendererType(null); // This component doesn't render anything by itselves.
	}

	// UIComponent overrides ------------------------------------------------------------------------------------------

	@Override
	public String getFamily() {
		return Tree.COMPONENT_FAMILY;
	}

    /**
     * Checks the parent and children and then delegates encoding of node children to
     * {@link Tree#encodeTreeNode(FacesContext)} as it maintains the model.
     * @throws IllegalArgumentException If there is no parent of type {@link UITreeNodeItem} or if there are
     * any children.
     * @see Tree#encodeTreeNode(FacesContext)
     */
    @Override
    public void encodeAll(FacesContext context) throws IOException {
        TreeNodeItem treeNodeItem = Components.getClosestParent(this, TreeNodeItem.class);

        if (treeNodeItem == null) {
            throw new IllegalArgumentException(ERROR_INVALID_PARENT);
        }

        if (getChildCount() > 0) {
            StringBuilder childClassNames = new StringBuilder();

            for (UIComponent child : getChildren()) {
                childClassNames.append(", ").append(child.getClass().getName());
            }

            throw new IllegalArgumentException(String.format(ERROR_CHILDREN_DISALLOWED, childClassNames.substring(2)));
        }

		if (!isRendered()) {
			return;
		}

        Components.getClosestParent(treeNodeItem, Tree.class).encodeTreeNode(context);
    }

}