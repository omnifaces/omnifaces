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
package org.omnifaces.util;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

/**
 * Helper class to build an array of SelectItems using the builder pattern.
 * 
 * @author Arjan Tijms
 */
public class SelectItemsBuilder {

	List<SelectItem> selectItems = new ArrayList<SelectItem>();
	
	public SelectItemsBuilder add(Object value, String label) {
		selectItems.add(new SelectItem(value, label));
		return this;
	}
	
	public SelectItem[] build() {
		return selectItems.toArray(new SelectItem[selectItems.size()]);
	}
	
}
