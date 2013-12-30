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
 * @param <T> The type of the wrapped data of the tree node.
 */
public interface TreeModel<T> extends Iterable<TreeModel<T>>, Serializable {

	// Mutators -------------------------------------------------------------------------------------------------------

	/**
	 * Sets the wrapped data of the current tree node.
	 * @param data The wrapped data of current tree node.
	 */
	void setData(T data);

	/**
	 * Creates and adds a child tree node with the given wrapped data to the current tree node. It returns the newly
	 * created and added child tree node to ease further building.
	 * @param data The wrapped data of the child tree node to be created and added.
	 * @return The newly created and added child tree node of the current tree node.
	 * @throws IllegalArgumentException When the given wrapped data is not acceptable by the tree model implementation.
	 * @throws UnsupportedOperationException When the child tree node cannot be instantiated using default constructor.
	 */
	TreeModel<T> addChild(T data);

	/**
	 * Adds the given child tree node to the current tree node. It returns the added child tree node to ease further
	 * building.
	 * @param child The child tree node to be added.
	 * @return The same child treenode.
	 * @throws IllegalArgumentException When the given child is not an instance of the same class as the parent.
	 * @since 1.1
	 */
	TreeModel<T> addChildNode(TreeModel<T> child);

	/**
	 * Removes the current tree node from its parent, if any. It returns the parent to ease further building.
	 * @return The parent tree node of the current tree node.
	 * @since 1.1
	 */
	TreeModel<T> remove();

	// Accessors ------------------------------------------------------------------------------------------------------

	/**
	 * Returns the wrapped data of the current tree node.
	 * @return The wrapped data of the current tree node.
	 */
	T getData();

	/**
	 * Returns the parent tree node of the current tree node. Returns <code>null</code> if there is none.
	 * @return The parent tree node of the current tree node.
	 */
	TreeModel<T> getParent();

	/**
	 * Returns the next tree node sibling of the current tree node. Returns <code>null</code> if there is none.
	 * @return The next tree node sibling of the current tree node.
	 * @since 1.4
	 */
	TreeModel<T> getNextSibling();

	/**
	 * Returns the previous tree node sibling of the current tree node. Returns <code>null</code> if there is none.
	 * @return The previous tree node sibling of the current tree node.
	 * @since 1.4
	 */
	TreeModel<T> getPreviousSibling();

	/**
	 * Returns the count of the children of the current tree node.
	 * @return The count of the children of the current tree node.
	 */
	int getChildCount();

	/**
	 * Returns an unmodifiable list of all child tree nodes of the current tree node. Adding and removing elements is
	 * not supported on the list. Adding new children should be done by the {@link #addChild(Object)} method on the
	 * tree node parent. Removing children should be done by the {@link #remove()} method on the tree node itself.
	 * @return An unmodifiable list of all child tree nodes of the current tree node.
	 */
	List<TreeModel<T>> getChildren();

	/**
	 * Returns an unmodifiable iterator over the children of the current tree node. Adding/inserting/removing elements
	 * is not supported on the iterator.
	 * @return An unmodifiable iterator over the children of the current tree node.
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
	 * index <code>0</code>. The second child of first child has index <code>0_1</code>. The first child of second child
	 * of third child has index <code>2_1_0</code>.
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