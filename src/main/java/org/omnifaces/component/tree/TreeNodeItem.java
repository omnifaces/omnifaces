/*
 * Copyright 2018 OmniFaces
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

import static javax.faces.component.visit.VisitHint.SKIP_ITERATION;
import static org.omnifaces.util.Components.getClosestParent;
import static org.omnifaces.util.Components.validateHasNoParent;
import static org.omnifaces.util.Components.validateHasParent;

import java.io.IOException;

import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponent;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;

import org.omnifaces.model.tree.TreeModel;
import org.omnifaces.util.Callback;

/**
 * <p>
 * The <code>&lt;o:treeNodeItem&gt;</code> is an {@link UIComponent} that represents a single child tree node within a
 * parent {@link TreeNode} component. Within this component, the <code>var</code> attribute of the parent {@link Tree}
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

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Validate the component hierarchy.
	 * @throws IllegalStateException When there is no parent of type {@link TreeNode}, or when this component is
	 * nested in another {@link TreeNodeItem}.
	 */
	@Override
	protected void validateHierarchy() {
		validateHasParent(this, TreeNode.class);
		validateHasNoParent(this, TreeNodeItem.class);
	}

	/**
	 * Suppress default behavior of {@link #encodeAll(FacesContext)} (which also checks {@link #isRendered()}) by
	 * delegating directly to {@link #encodeChildren(FacesContext)}.
	 */
	@Override
	public void encodeAll(FacesContext context) throws IOException {
		encodeChildren(context);
	}

	/**
	 * Loop over children of the current model node, set the child as the current model node and continue processing
	 * this component according to the rules of the given phase ID.
	 * @param context The faces context to work with.
	 * @param phaseId The current phase ID.
	 * @see Tree#setCurrentModelNode(FacesContext, TreeModel)
	 */
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" }) // For TreeModel. We don't care about its actual type anyway.
	protected void process(final FacesContext context, final PhaseId phaseId) {
		if (getChildCount() == 0) {
			return;
		}

		process(context, new Callback.ReturningWithArgument<Void, Tree>() {
			@Override
			public Void invoke(Tree tree) {
				if (tree.getCurrentModelNode() != null) {
					for (TreeModel childModelNode : (Iterable<TreeModel>) tree.getCurrentModelNode()) {
						tree.setCurrentModelNode(context, childModelNode);

						if (isRendered()) {
							processSuper(context, phaseId);
						}
					}
				}

				return null;
			}
		});
	}

	/**
	 * Loop over children of the current model node, set the child as the current model node and continue visiting
	 * this component according to the given visit context and callback.
	 * @param context The visit context to work with.
	 * @param callback The visit callback to work with.
	 * @see Tree#setCurrentModelNode(FacesContext, TreeModel)
	 */
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" }) // For TreeModel. We don't care about its actual type anyway.
	public boolean visitTree(final VisitContext context, final VisitCallback callback) {
		if (context.getHints().contains(SKIP_ITERATION)) {
			return super.visitTree(context, callback);
		}

		if (!isVisitable(context) || getChildCount() == 0) {
			return false;
		}

		return process(context.getFacesContext(), new Callback.ReturningWithArgument<Boolean, Tree>() {
			@Override
			public Boolean invoke(Tree tree) {
				if (tree.getCurrentModelNode() != null) {
					for (TreeModel childModelNode : (Iterable<TreeModel>) tree.getCurrentModelNode()) {
						tree.setCurrentModelNode(context.getFacesContext(), childModelNode);

						if (TreeNodeItem.super.visitTree(context, callback)) {
							return true;
						}
					}
				}

				return false;
			}
		});
	}

	/**
	 * Convenience method to handle both {@link #process(FacesContext, PhaseId)} and
	 * {@link #visitTree(VisitContext, VisitCallback)} without code duplication.
	 * @param context The faces context to work with.
	 * @param phaseId The current phase ID (not used so far in this implementation).
	 * @param callback The callback to be invoked.
	 * @return The callback result.
	 */
	@SuppressWarnings("rawtypes") // For TreeModel. We don't care about its actual type anyway.
	private <R> R process(FacesContext context, Callback.ReturningWithArgument<R, Tree> callback) {
		Tree tree = getClosestParent(this, Tree.class);
		TreeModel originalModelNode = tree.getCurrentModelNode();

		try {
			return callback.invoke(tree);
		}
		finally {
			tree.setCurrentModelNode(context, originalModelNode);
		}
	}

}