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
package org.omnifaces.test.component.scriptparam;

import java.io.Serializable;

import jakarta.inject.Named;
import jakarta.json.JsonObject;

import org.omnifaces.cdi.PostScriptParam;
import org.omnifaces.cdi.ViewScoped;

@Named
@ViewScoped
public class ScriptParamITBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private Integer clientTimeZoneOffset;
	private JsonObject navigator;
	private boolean postScriptParamInvoked;

	@PostScriptParam
	public void initScriptParams() {
		postScriptParamInvoked = true;
	}

	public Integer getClientTimeZoneOffset() {
		return clientTimeZoneOffset;
	}

	public void setClientTimeZoneOffset(Integer clientTimeZoneOffset) {
		this.clientTimeZoneOffset = clientTimeZoneOffset;
	}

	public JsonObject getNavigator() {
		return navigator;
	}

	public void setNavigator(JsonObject navigator) {
		this.navigator = navigator;
	}

	public boolean isPostScriptParamInvoked() {
		return postScriptParamInvoked;
	}

}