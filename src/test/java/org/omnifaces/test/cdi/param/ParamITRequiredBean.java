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
package org.omnifaces.test.cdi.param;

import static org.omnifaces.util.Faces.isValidationFailed;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

import org.omnifaces.cdi.Param;

@Named
@RequestScoped
public class ParamITRequiredBean {

	@Param(required = true)
	private String requiredStringParam;

	private String initResult;

	@PostConstruct
	public void init() {
		if (isValidationFailed()) {
			initResult = "initValidationFailed";
		}
		else {
			initResult = "initSuccess";
		}
	}

	public String getRequiredStringParam() {
		return requiredStringParam;
	}

	public String getInitResult() {
		return initResult;
	}
}