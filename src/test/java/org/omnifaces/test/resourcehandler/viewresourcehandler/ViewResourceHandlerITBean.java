/*
 * Copyright 2020 OmniFaces
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
package org.omnifaces.test.resourcehandler.viewresourcehandler;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

@Named
@RequestScoped
public class ViewResourceHandlerITBean {

	private static final ZonedDateTime LAST_MODIFIED = ZonedDateTime.of(2020, 12, 22, 15, 20, 10, 0, ZoneOffset.ofHours(-4));
	private List<ViewResourceHandlerITEntity> entities;

	@PostConstruct
	public void init() {
		entities = new ArrayList<>();
		entities.add(new ViewResourceHandlerITEntity(1L, LAST_MODIFIED.toInstant()));
		entities.add(new ViewResourceHandlerITEntity(2L, LAST_MODIFIED.toLocalDate()));
		entities.add(new ViewResourceHandlerITEntity(3L, LAST_MODIFIED.toLocalDateTime()));
		entities.add(new ViewResourceHandlerITEntity(4L, LAST_MODIFIED));
	}

	public List<ViewResourceHandlerITEntity> getEntities() {
		return entities;
	}

}
