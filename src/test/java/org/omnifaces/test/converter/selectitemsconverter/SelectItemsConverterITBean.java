/*
 * Copyright OmniFaces
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
package org.omnifaces.test.converter.selectitemsconverter;

import static java.util.Arrays.asList;

import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

@Named
@RequestScoped
public class SelectItemsConverterITBean {

	private SelectItemsConverterITEntity selectedEntity;
	private List<SelectItemsConverterITEntity> availableEntities;

	@PostConstruct
	public void init() {
		availableEntities = asList(
			new SelectItemsConverterITEntity(1L),
			new SelectItemsConverterITEntity(2L),
			new SelectItemsConverterITEntity(3L)
		);
	}

	public SelectItemsConverterITEntity getSelectedEntity() {
		return selectedEntity;
	}

	public void setSelectedEntity(SelectItemsConverterITEntity selectedEntity) {
		this.selectedEntity = selectedEntity;
	}

	public List<SelectItemsConverterITEntity> getAvailableEntities() {
		return availableEntities;
	}

}