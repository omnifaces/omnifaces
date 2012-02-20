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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A concrete implementation of {@link TreeModel} which holds the tree children in a {@link List}.
 *
 * @author Bauke Scholtz
 * @param <T> The type of the wrapped data of the tree node. It must implement {@link Serializable}.
 */
public class ListTreeModel<T> implements TreeModel<T> {

	// Properties -----------------------------------------------------------------------------------------------------

	private T data;
	private TreeModel<T> parent;
	private List<TreeModel<T>> children = new ArrayList<TreeModel<T>>();

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Default constructor. It does nothing special.
	 */
    public ListTreeModel() {
        // Just keep alive.
    }

    /**
     * Convenience constructor which immediately sets the given data as wrapped data of the current tree node.
     * @param data The wrapped data of current tree node.
     */
    public ListTreeModel(T data) {
        this.data = data;
    }

	// Mutators -------------------------------------------------------------------------------------------------------

	@Override
	public void setData(T data) {
		this.data = data;
	}

	@Override
	public void setParent(TreeModel<T> parent) {
		this.parent = parent;
	}

	@Override
	public TreeModel<T> addChild(T data) {
		TreeModel<T> child = new ListTreeModel<T>(data);
		child.setParent(this);
		children.add(child);
		return child;
	}

	@Override
	public void remove() {
		if (parent != null) {
			parent.getChildren().remove(this);
			parent = null;
		}
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
	public List<TreeModel<T>> getChildren() {
		return children;
	}

	@Override
	public Iterator<TreeModel<T>> iterator() {
		return children.iterator();
	}

	@Override
	public int getLevel() {
		return parent == null ? 0 : parent.getLevel() + 1;
	}

	@Override
	public String getIndex() {
		return parent == null ? null : (parent.getIndex() == null ? "" : parent.getIndex() + "_") + parent.getChildren().indexOf(this);
	}

	// Checkers -------------------------------------------------------------------------------------------------------

	@Override
	public boolean isRoot() {
		return parent == null;
	}

	@Override
	public boolean isLeaf() {
		return children.isEmpty();
	}

	@Override
	public boolean isFirst() {
		return parent != null && parent.getChildren().indexOf(this) == 0;
	}

	@Override
	public boolean isLast() {
		return parent != null && parent.getChildren().indexOf(this) + 1 == parent.getChildren().size();
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
		return data + "" + children;
	}

}