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
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;

import org.omnifaces.util.Components;

/**
 * <strong>TreeInsertChildren</strong> is an {@link UIComponent} that represents the insertion point for the
 * children of a parent tree node which is represented by {@link TreeNodeItem}.
 * <p>
 * This component does not allow any children.
 *
 * @author Bauke Scholtz
 * @see TreeNodeItem
 */
@FacesComponent(TreeInsertChildren.COMPONENT_TYPE)
public class TreeInsertChildren extends TreeFamily {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The standard component type. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.tree.TreeInsertChildren";

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Validate the component hierarchy.
	 * @throws IllegalArgumentException When there is no parent of type {@link TreeNodeItem}, or when there are any
	 * children.
	 */
	@Override
	protected void validateHierarchy() {
		Components.validateHasParent(this, TreeNodeItem.class);
		Components.validateHasNoChildren(this);
	}

	/**
	 * Delegate processing of the tree node to {@link Tree#processTreeNode(FacesContext, PhaseId)}.
	 * @see Tree#processTreeNode(FacesContext, PhaseId)
	 */
	@Override
	protected void process(FacesContext context, PhaseId phaseId) {
		Components.getClosestParent(this, Tree.class).processTreeNode(context, phaseId);
	}

	/**
	 * Delegate visiting of the tree node to {@link Tree#visitTreeNode(VisitContext, VisitCallback)}.
	 * @see Tree#visitTreeNode(VisitContext, VisitCallback)
	 */
	@Override
	public boolean visitTree(VisitContext context, VisitCallback callback) {
		if (Components.shouldVisitSkipIteration(context)) {
			return super.visitTree(context, callback);
		}

		return Components.getClosestParent(this, Tree.class).visitTreeNode(context, callback);
	}

}