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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A concrete implementation of {@link TreeModel} which holds the tree children in a {@link List}.
 *
 * @author Bauke Scholtz
 * @param <T> The type of the wrapped data of the tree node.
 */
public class ListTreeModel<T> implements TreeModel<T> {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 275741310298761716L;

	// Properties -----------------------------------------------------------------------------------------------------

	private T data;
	private ListTreeModel<T> parent;
	private List<TreeModel<T>> children;
	private List<TreeModel<T>> unmodifiableChildren = Collections.emptyList();
	private int index;

	// Mutators -------------------------------------------------------------------------------------------------------

	@Override
	public void setData(T data) {
		this.data = data;
	}

	@Override
	public TreeModel<T> addChild(T data) {
		ListTreeModel<T> child = new ListTreeModel<T>();
		child.data = data;
		return addChildNode(child);
	}

	@Override
	public TreeModel<T> addChildNode(TreeModel<T> child) {
		if (!(child instanceof ListTreeModel)) {
			throw new IllegalArgumentException();
		}

		if (children == null) {
			children = new ArrayList<TreeModel<T>>();
		}

		((ListTreeModel<T>) child).parent = this;
		((ListTreeModel<T>) child).index = children.size();
		children.add(child);
		return child;
	}

	@Override
	public TreeModel<T> remove() {
		if (parent != null) {
			synchronized (parent.children) {
				parent.children.remove(this);

				// Fix the indexes of the children (that's why it needs to be synchronized).
				for (int index = 0; index < parent.children.size(); index++) {
					((ListTreeModel<T>) parent.children.get(index)).index = index;
				}
			}
		}

		return parent;
	}

	// Accessors ------------------------------------------------------------------------------------------------------

	@Override
	public T getData() {
		return data;
	}

	@Override
	public TreeModel<T> getParent() {
		return parent;
	}

	@Override
	public TreeModel<T> getNextSibling() {
		return getNextSibling(parent, index + 1);
	}

	/**
	 * Recursive helper method for {@link #getNextSibling()}.
	 */
	private TreeModel<T> getNextSibling(TreeModel<T> parent, int index) {
		if (parent == null) {
			return null;
		}
		else if (index < parent.getChildCount()) {
			return parent.getChildren().get(index);
		}
		else {
			return getNextSibling(parent.getNextSibling(), 0);
		}
	}

	@Override
	public TreeModel<T> getPreviousSibling() {
		return getPreviousSibling(parent, index - 1);
	}

	/**
	 * Recursive helper method for {@link #getPreviousSibling()}.
	 */
	private TreeModel<T> getPreviousSibling(TreeModel<T> parent, int index) {
		if (parent == null) {
			return null;
		}
		else if (index >= 0) {
			return parent.getChildren().get(index);
		}
		else {
			parent = parent.getPreviousSibling();
			return getPreviousSibling(parent, (parent != null ? parent.getChildCount() : 0) - 1);
		}
	}

	@Override
	public int getChildCount() {
		return children == null ? 0 : children.size();
	}

	@Override
	public List<TreeModel<T>> getChildren() {
		if (unmodifiableChildren.size() != getChildCount()) {
			unmodifiableChildren = Collections.unmodifiableList(children);
		}

		return unmodifiableChildren;
	}

	@Override
	public Iterator<TreeModel<T>> iterator() {
		return getChildren().iterator();
	}

	@Override
	public int getLevel() {
		return parent == null ? 0 : parent.getLevel() + 1;
	}

	@Override
	public String getIndex() {
		return parent == null ? null : (parent.parent == null ? "" : parent.getIndex() + "_") + index;
	}

	// Checkers -------------------------------------------------------------------------------------------------------

	@Override
	public boolean isRoot() {
		return parent == null;
	}

	@Override
	public boolean isLeaf() {
		return getChildCount() == 0;
	}

	@Override
	public boolean isFirst() {
		return parent != null && index == 0;
	}

	@Override
	public boolean isLast() {
		return parent != null && index + 1 == parent.getChildCount();
	}

	// Object overrides -----------------------------------------------------------------------------------------------

	@Override
	@SuppressWarnings("rawtypes")
	public boolean equals(Object object) {
		// Basic checks.
		if (object == this) return true;
		if (!(object instanceof ListTreeModel)) return false;

		// Property checks.
		ListTreeModel other = (ListTreeModel) object;
		if (data == null ? other.data != null : !data.equals(other.data)) return false;
		if (parent == null ? other.parent != null : !parent.equals(other.parent)) return false;
		if (children == null ? other.children != null : !children.equals(other.children)) return false;

		// All passed.
		return true;
	}

	@Override // Eclipse-generated.
	public int hashCode() {
		final int prime = 31;
		int hashCode = 1;
		hashCode = prime * hashCode + ((children == null) ? 0 : children.hashCode());
		hashCode = prime * hashCode + ((data == null) ? 0 : data.hashCode());
		hashCode = prime * hashCode + ((parent == null) ? 0 : parent.hashCode());
		return hashCode;
	}

	@Override
	public String toString() {
		return (data == null ? "" : data) + "" + (children == null ? "" : children);
	}

}