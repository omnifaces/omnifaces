/*
 * Copyright 2013 OmniFaces.
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

import java.util.Collection;
import java.util.TreeSet;

/**
 * A concrete implementation of {@link TreeModel} which holds the tree children in a {@link TreeSet}.
 *
 * @author Bauke Scholtz
 * @param <T> The type of the wrapped data of the tree node.
 * @since 1.7
 */
public class SortedTreeModel<T> extends AbstractTreeModel<T> implements Comparable<T> {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 8694627466999765186L;

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns a new {@link TreeSet}.
	 */
	@Override
	protected Collection<TreeModel<T>> createChildren() {
		return new TreeSet<>();
	}

	/**
	 * An override which throws {@link IllegalArgumentException} when given data is not <code>null</code>
	 * <strong>and</strong> not an instance of {@link Comparable}. In other words, it only accepts <code>null</code>
	 * or an instance of {@link Comparable}.
	 */
	@Override
	public TreeModel<T> addChild(T data) {
		if (data != null && !(data instanceof Comparable)) {
			throw new IllegalArgumentException();
		}

		return super.addChild(data);
	}

	@Override
	@SuppressWarnings("unchecked")
	public int compareTo(T object) {
		SortedTreeModel<T> other = (SortedTreeModel<T>) object;
		return (getData() == null) ? -1
			: (other == null || other.getData() == null) ? 1
			: ((Comparable<T>) getData()).compareTo(other.getData());
	}

}