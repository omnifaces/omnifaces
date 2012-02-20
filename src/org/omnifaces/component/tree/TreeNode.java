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
import javax.swing.tree.TreeModel;

/**
 * <strong>TreeNode</strong> is a {@link UIComponent} that represents a single tree node within a parent
 * {@link Tree} component. Within this component, the <code>var</code> attribute of the parent {@link Tree}
 * component will expose the tree node. Each of its children is processed by {@link TreeNodeItem}.
 * <p>
 * The <code>level</code> attribute can be used to specify for which tree node level as obtained by
 * {@link TreeModel#getLevel()} this component should render the children by {@link TreeNodeItem}. The root tree node
 * has level 0.
 * <p>
 * This component does not have a renderer since it does not render any markup by itself.
 *
 * @author Bauke Scholtz
 * @see Tree
 * @see TreeNodeItem
 */
public class TreeNode extends UIComponentBase {

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String ERROR_INVALID_PARENT =
		"TreeNode must have a direct parent of type Tree, but it cannot be found.";

	private enum PropertyKeys {
		level;
	}

	// Public constants -----------------------------------------------------------------------------------------------

    /** The standard component type. */
    public static final String COMPONENT_TYPE = "org.omnifaces.component.TreeNode";

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new {@link TreeNode} instance.
	 */
	public TreeNode() {
		setRendererType(null); // This component doesn't render anything by itselves.
	}

	// UIComponent overrides ------------------------------------------------------------------------------------------

	@Override
	public String getFamily() {
		return Tree.COMPONENT_FAMILY;
	}

	/**
	 * Checks the direct parent component type. This method is by design already not called when the direct parent is
	 * already of type {@link Tree} as it only calls {@link #encodeChildren(FacesContext)} on this instance. This
	 * method will only be called when the TreeNode component is been used without any Tree parent.
	 * @throws IllegalArgumentException If the direct parent component isn't of type {@link Tree}.
	 * @see Tree#encodeTreeNode(FacesContext)
	 */
	@Override
	public void encodeAll(FacesContext context) throws IOException {
		if (!(getParent() instanceof Tree)) {
			throw new IllegalArgumentException(String.format(ERROR_INVALID_PARENT, getParent().getClass().getName()));
		}
	}

    // Getters/setters ------------------------------------------------------------------------------------------------

	/**
	 * Returns the level for which this node should render the items.
	 * @return The level for which this node should render the items.
	 */
	public Integer getLevel() {
		return (Integer) getStateHelper().eval(PropertyKeys.level);
	}

	/**
	 * Sets the level for which this node should render the items.
	 * @param level The level for which this node should render the items.
	 */
	public void setLevel(Integer level) {
		getStateHelper().put(PropertyKeys.level, level);
	}

}