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
package org.omnifaces.model.tree;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import org.omnifaces.component.tree.Tree;

/**
 * A generic and simple tree data model which is to be used by {@link Tree} component.
 *
 * @author Bauke Scholtz
 * @param <T> The type of the wrapped data of the tree node. It must implement {@link Serializable}.
 */
public interface TreeModel<T> extends Iterable<TreeModel<T>> {

	// Mutators -------------------------------------------------------------------------------------------------------

	/**
	 * Sets the wrapped data of the current tree node.
	 * @param data The wrapped data of current tree node.
	 */
	void setData(T data);

	/**
	 * Sets the parent of the current tree node.
	 * @param parent The parent of current tree node.
	 */
	void setParent(TreeModel<T> parent);

	/**
	 * Creates and adds a child tree node with the given data to the current tree node and return it.
	 * @return The newly created and added child tree node of the current tree node.
	 */
	TreeModel<T> addChild(T data);

	/**
	 * Removes the current tree node and all of its children from its parent, if any.
	 */
	void remove();

	// Accessors ------------------------------------------------------------------------------------------------------

	/**
	 * Returns the wrapped data of the current tree node.
	 * @return the wrapped data of the current tree node.
	 */
	T getData();

	/**
	 * Returns the parent tree node of the current tree node.
	 * @return the parent tree node of the current tree node.
	 */
	TreeModel<T> getParent();

	/**
	 * Returns the child tree nodes of the current tree node.
	 * @return The child tree nodes of the current tree node.
	 */
	List<TreeModel<T>> getChildren();

	/**
	 * Returns an iterator over the children of the current tree node.
	 * @return An iterator over the children of the current tree node.
	 * @see Iterable
	 */
	@Override
	Iterator<TreeModel<T>> iterator();

	/**
	 * Returns the level of the current tree node. The root node has level 0.
	 * @return The level of the current tree node.
	 */
	int getLevel();

	/**
	 * Returns the zero-based unique index of the current tree node. This is an underscore separated representation of
	 * the position of the node in the tree hierarchy. The root node has index of <code>null</code>. The first child has
	 * index 0. The second child of first child has index 0_1. The first child of second child of third child has index
	 * 2_1_0.
	 * @return The unique index of the current tree node.
	 */
	String getIndex();

	// Checkers -------------------------------------------------------------------------------------------------------

	/**
	 * Returns whether the current tree node is the root node. That is, when it has no parent.
	 * @return <code>true</code> if the current tree node is the root node, otherwise <code>false</code>.
	 */
	boolean isRoot();

	/**
	 * Returns whether the current tree node is a leaf node. That is, when it has no children.
	 * @return <code>true</code> if the current tree node is a leaf node, otherwise <code>false</code>.
	 */
	boolean isLeaf();

	/**
	 * Returns whether the current tree node is the first child of its parent, if any.
	 * @return <code>true</code> if the current tree node is the first child of its parent, otherwise <code>false</code>.
	 */
	boolean isFirst();

	/**
	 * Returns whether the current tree node is the last child of its parent, if any.
	 * @return <code>true</code> if the current tree node is the last child of its parent, otherwise <code>false</code>.
	 */
	boolean isLast();

}