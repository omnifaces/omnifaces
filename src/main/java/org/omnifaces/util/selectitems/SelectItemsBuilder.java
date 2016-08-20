/*
 * Copyright 2016 OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnifaces.util.selectitems;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

/**
 * Helper class to build an array of SelectItems using various means, like via the builder pattern or a given <code>Map</code>.
 *
 * @author Arjan Tijms
 */
public class SelectItemsBuilder {

	private List<SelectItem> selectItems = new ArrayList<>();

	public SelectItemsBuilder add(Object value, String label) {
		selectItems.add(new SelectItem(value, label));
		return this;
	}

	public SelectItem[] build() {
		return selectItems.toArray(new SelectItem[selectItems.size()]);
	}

	public List<SelectItem> buildList() {
		return selectItems;
	}

	/**
	 * Builds a <code>List</code> of <code>SelectItem</code>s from the given <code>Map</code> argument.
	 *
	 * @param map the Map
	 * @return <code>List</code> of <code>SelectItem</code>s having the map's value as value and the map's key as label.
	 */
	public static List<SelectItem> fromMap(Map<?, ?> map) {
		List<SelectItem> items = new ArrayList<>(map.size());
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			items.add(new SelectItem(entry.getValue(), entry.getKey().toString()));
		}

		return items;
	}

}
