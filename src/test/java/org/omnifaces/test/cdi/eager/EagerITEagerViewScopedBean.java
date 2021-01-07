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
package org.omnifaces.test.cdi.eager;

import static java.lang.System.nanoTime;

import java.io.Serializable;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import org.omnifaces.cdi.Eager;

@Eager(viewId="/EagerIT.xhtml")
@Named
@ViewScoped
public class EagerITEagerViewScopedBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long initTime;

	@PostConstruct
	public void init() {
		initTime = nanoTime();
	}

	public Long getInitTime() {
		return initTime;
	}

}