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
package org.omnifaces.model.tree;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A concrete implementation of {@link TreeModel} which holds the tree children in an {@link ArrayList}.
 *
 * @author Bauke Scholtz
 * @param <T> The type of the wrapped data of the tree node.
 */
public class ListTreeModel<T> extends AbstractTreeModel<T> {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns a new {@link ArrayList}.
	 */
	@Override
	protected Collection<TreeModel<T>> createChildren() {
		return new ArrayList<>();
	}

}