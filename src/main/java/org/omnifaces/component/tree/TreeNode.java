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
package org.omnifaces.component.tree;

import static org.omnifaces.util.ComponentsLocal.validateHasChild;
import static org.omnifaces.util.ComponentsLocal.validateHasDirectParent;
import static org.omnifaces.util.ComponentsLocal.validateHasNoParent;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.PhaseId;

import org.omnifaces.config.OmniFaces;
import org.omnifaces.model.tree.TreeModel;
import org.omnifaces.util.State;

/**
 * <p>
 * The <code>&lt;o:treeNode&gt;</code> is an {@link UIComponent} that represents a single tree node within a parent {@link Tree} component. Within this
 * component, the <code>var</code> attribute of the parent {@link Tree} component will expose the tree node. Each of its children is processed by
 * {@link TreeNodeItem}.
 * <p>
 * The <code>level</code> attribute can be used to specify for which tree node level as obtained by {@link TreeModel#getLevel()} this component should render
 * the children by {@link TreeNodeItem}. The root tree node has level 0.
 *
 * @author Bauke Scholtz
 * @see Tree
 * @see TreeNodeItem
 */
@FacesComponent(value = TreeNode.COMPONENT_TYPE, namespace = OmniFaces.OMNIFACES_NAMESPACE)
public class TreeNode extends TreeFamily {

    // Public constants -----------------------------------------------------------------------------------------------

    /** The component type, which is {@value org.omnifaces.component.tree.TreeNode#COMPONENT_TYPE}. */
    public static final String COMPONENT_TYPE = "org.omnifaces.component.tree.TreeNode";

    // Private constants ----------------------------------------------------------------------------------------------

    private enum PropertyKeys {
        // Cannot be uppercased. They have to exactly match the attribute names.
        level;
    }

    // Variables ------------------------------------------------------------------------------------------------------

    private final State state = new State(getStateHelper());

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * Validate the component hierarchy.
     * 
     * @throws IllegalStateException When the direct parent component isn't of type {@link Tree}, or when this component is nested in another {@link TreeNode},
     * or when there aren't any children of type {@link TreeNodeItem}.
     */
    @Override
    protected void validateHierarchy(FacesContext context) {
        validateHasDirectParent(context, this, Tree.class);
        validateHasNoParent(context, this, TreeNode.class);
        validateHasChild(context, this, TreeNodeItem.class);
    }

    /**
     * This method is by design only called by {@link Tree#processTreeNode(FacesContext, PhaseId)} as it maintains all the nodes.
     * 
     * @see Tree#processTreeNode(FacesContext, PhaseId)
     */
    @Override
    protected void process(FacesContext context, PhaseId phaseId) {
        if (!isRendered()) {
            return;
        }

        processSuper(context, phaseId);
    }

    // Getters/setters ------------------------------------------------------------------------------------------------

    /**
     * Returns the tree node level to render the node children for. The root level is {@code 0}.
     * 
     * @return The tree node level.
     */
    public Integer getLevel() {
        return state.get(PropertyKeys.level);
    }

    /**
     * Sets the tree node level to render the node children for. The root level is {@code 0}.
     * 
     * @param level The tree node level.
     */
    public void setLevel(Integer level) {
        state.put(PropertyKeys.level, level);
    }

}
