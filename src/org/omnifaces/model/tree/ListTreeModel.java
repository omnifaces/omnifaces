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

	// Properties -----------------------------------------------------------------------------------------------------

	private T data;
	private ListTreeModel<T> parent;
	private List<TreeModel<T>> children;
	private List<TreeModel<T>> unmodifiableChildren;
	private int index;

	// Mutators -------------------------------------------------------------------------------------------------------

	@Override
	public void setData(T data) {
		this.data = data;
	}

	@Override
	public TreeModel<T> addChild(T data) {
		if (children == null) {
			children = new ArrayList<TreeModel<T>>();
		}

		ListTreeModel<T> child = new ListTreeModel<T>();
		child.data = data;
		child.parent = this;
		child.index = children.size();
		children.add(child);
		return child;
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
	public int getChildCount() {
		return children == null ? 0 : children.size();
	}

	@Override
	public List<TreeModel<T>> getChildren() {
		if (unmodifiableChildren == null && children == null) {
			unmodifiableChildren = Collections.emptyList();
		} else if (unmodifiableChildren == null || unmodifiableChildren.size() != children.size()) {
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
		return parent == null ? null : (parent.getIndex() == null ? "" : parent.getIndex() + "_") + index;
	}

	// Checkers -------------------------------------------------------------------------------------------------------

	@Override
	public boolean isRoot() {
		return parent == null;
	}

	@Override
	public boolean isLeaf() {
		return children == null;
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
		if (!(object instanceof TreeModel)) return false;

		// Property checks.
		TreeModel other = (TreeModel) object;
		if (data == null ? other.getData() != null : !data.equals(other.getData())) return false;
		if (parent == null ? other.getParent() != null : !parent.equals(other.getParent())) return false;
		if (children == null ? other.getChildren() != null : !children.equals(other.getChildren())) return false;

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