/*
 * Copyright 2012 Omnifaces.
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

import javax.faces.FacesException;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.faces.render.Renderer;
import javax.swing.tree.TreeModel;

/**
 * <strong>TreeNode</strong> is an {@link UIComponent} that represents a single tree node within a parent
 * {@link Tree} component. Within this component, the <code>var</code> attribute of the parent {@link Tree}
 * component will expose the tree node. Each of its children is processed by {@link TreeNodeItem}.
 * <p>
 * The <code>level</code> attribute can be used to specify for which tree node level as obtained by
 * {@link TreeModel#getLevel()} this component should render the children by {@link TreeNodeItem}. The root tree node
 * has level 0.
 *
 * @author Bauke Scholtz
 * @see Tree
 * @see TreeNodeItem
 */
@FacesComponent(TreeNode.COMPONENT_TYPE)
public class TreeNode extends UIComponentBase {

	// Public constants -----------------------------------------------------------------------------------------------

    /** The standard component type. */
    public static final String COMPONENT_TYPE = "org.omnifaces.component.tree.TreeNode";

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String ERROR_INVALID_PARENT =
		"TreeNode must have a direct parent of type Tree. Encountered parent of type '%s'.";

	private enum PropertyKeys {
		level;
	}

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
	 * Returns <code>true</code> even though this component doesn't have a {@link Renderer}, because we want to encode
	 * all children anyway.
	 */
	@Override
	public boolean getRendersChildren() {
		return true;
	}

    @Override
    public void processDecodes(FacesContext context) {
    	validateHierarchy();
    	process(context, PhaseId.APPLY_REQUEST_VALUES);
    }

    @Override
    public void processValidators(FacesContext context) {
    	process(context, PhaseId.PROCESS_VALIDATIONS);
    }

    @Override
    public void processUpdates(FacesContext context) {
    	process(context, PhaseId.UPDATE_MODEL_VALUES);
    }

    @Override
    public void encodeAll(FacesContext context) throws IOException {
    	validateHierarchy();
    	process(context, PhaseId.RENDER_RESPONSE);
    }

    // Internal actions -----------------------------------------------------------------------------------------------

	/**
	 * This method is by design only called by {@link Tree#processTreeNode(FacesContext, PhaseId)}.
	 * @see Tree#processTreeNode(FacesContext, PhaseId)
	 */
	protected void process(FacesContext context, PhaseId phaseId) {
		if (!isRendered()) {
			return;
		}

		if (phaseId == PhaseId.APPLY_REQUEST_VALUES) {
            super.processDecodes(context);
        } else if (phaseId == PhaseId.PROCESS_VALIDATIONS) {
        	super.processValidators(context);
        } else if (phaseId == PhaseId.UPDATE_MODEL_VALUES) {
        	super.processUpdates(context);
        } else if (phaseId == PhaseId.RENDER_RESPONSE) {
        	try {
        		super.encodeChildren(context);
			} catch (IOException e) {
				throw new FacesException(e);
			}
        }
	}

	/**
	 * Validate the component hierarchy.
	 * @throws IllegalArgumentException When the direct parent component isn't of type {@link Tree}.
	 */
	private void validateHierarchy() {
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