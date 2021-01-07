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
package org.omnifaces.test.taghandler.validatebean;
import static org.omnifaces.util.Faces.isValidationFailed;
import static org.omnifaces.util.Messages.addGlobalInfo;
import static org.omnifaces.util.Messages.addGlobalWarn;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Named
@RequestScoped
public class ValidateBeanITDefaultBean {

	@Pattern(regexp=".{2,}", message="default")
	@Size(min=2, groups=ValidateBeanITValidationGroup.class, message="group")
	private String input;

	@NotNull
	private String unused; // This one is not referenced in JSF form and should therefore not trigger validation fail on it.

	public void action() {
		if (isValidationFailed()) {
			addGlobalWarn("actionValidationFailed");
		}
		else {
			addGlobalInfo("actionSuccess");
		}
	}

	public String getInput() {
		return input;
	}

	public void setInput(String input) {
		this.input = input;
	}

	public String getUnused() {
		return unused;
	}

	public void setUnused(String unused) {
		this.unused = unused;
	}

}