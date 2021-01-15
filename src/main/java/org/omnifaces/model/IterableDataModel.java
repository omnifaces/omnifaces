/*
 * Copyright 2021 OmniFaces
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
package org.omnifaces.model;

import static org.omnifaces.util.Utils.iterableToList;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import jakarta.faces.model.DataModel;
import jakarta.faces.model.DataModelEvent;
import jakarta.faces.model.DataModelListener;
import jakarta.faces.model.ListDataModel;

/**
 * <strong>IterableDataModel</strong> is an implementation of {@link DataModel} that wraps an <code>Iterable</code>.
 * <p>
 * This can be used to encapsulate nearly every collection type, including {@link Collection} derived types such as
 * {@link List} and {@link Set}. As such this specific DataModel can be used instead of more specific DataModels like
 * {@link ListDataModel} and JSF 2.2's CollectionDataModel.
 *
 * @since 1.5
 * @author Arjan.Tijms
 *
 */
public class IterableDataModel<E> extends DataModel<E> {

	private int index = -1;
	private Iterable<E> iterable;
	private List<E> list;

	/**
	 * Construct the iterable data model based on the given iterable instance.
	 * @param iterable The iterable instance to construct the iterable data model for.
	 */
	public IterableDataModel(Iterable<E> iterable) {
		setWrappedData(iterable);
	}

	@Override
	public boolean isRowAvailable() {
		return list != null && index >= 0 && index < list.size();
	}

	@Override
	public int getRowCount() {
		if (list == null) {
			return -1;
		}

		return list.size();
	}

	@Override
	public E getRowData() {
		if (list == null) {
			return null;
		}
		if (!isRowAvailable()) {
			throw new IllegalStateException();
		}

		return list.get(index);
	}

	@Override
	public int getRowIndex() {
		return index;
	}

	@Override
	public void setRowIndex(int rowIndex) {

		if (rowIndex < -1) {
			throw new IllegalArgumentException();
		}

		int oldRowIndex = index;
		index = rowIndex;

		if (list == null) {
			return;
		}

		notifyListeners(oldRowIndex, rowIndex);
	}

	@Override
	public Object getWrappedData() {
		return iterable;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setWrappedData(Object data) {
		if (data == null) {
			iterable = null;
			list = null;
			setRowIndex(-1);
		} else {
			iterable = (Iterable<E>) data;
			list = iterableToList(iterable);
			setRowIndex(0);
		}
	}

	private E getRowDataOrNull() {
		if (isRowAvailable()) {
			return getRowData();
		}

		return null;
	}

	private void notifyListeners(int oldRowIndex, int rowIndex) {
		DataModelListener[] dataModelListeners = getDataModelListeners();
		if (oldRowIndex != rowIndex && dataModelListeners != null) {

			DataModelEvent dataModelEvent = new DataModelEvent(this, rowIndex, getRowDataOrNull());

			for (DataModelListener dataModelListener : dataModelListeners) {
				if (dataModelListener != null) {
					dataModelListener.rowSelected(dataModelEvent);
				}
			}
		}
	}

}