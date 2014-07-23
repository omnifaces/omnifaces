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
package org.omnifaces.component.validator;

import static org.omnifaces.util.Utils.isEmpty;

import java.util.List;

import javax.faces.component.FacesComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

/**
 * <p>
 * The <code>&lt;o:validateAll&gt;</code> validates if ALL of the given {@link UIInput} components have been filled out.
 * One could of course also just put <code>required="true"</code> on all of those {@link UIInput} components, but
 * sometimes it's desireable to invalidate all of those fields and/or to have just only one message for it, which isn't
 * possible with the standard JSF API.
 * <p>
 * The default message is
 * <blockquote>{0}: Please fill out all of those fields</blockquote>
 * <p>
 * For general usage instructions, refer {@link ValidateMultipleFields} documentation.
 *
 * @author Bauke Scholtz
 * @since 1.1
 */
@FacesComponent(ValidateAll.COMPONENT_TYPE)
public class ValidateAll extends ValidateMultipleFields {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The standard component type. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.validator.ValidateAll";

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Validate if all is filled out.
	 */
	@Override
	public boolean validateValues(FacesContext context, List<UIInput> inputs, List<Object> values) {
		for (Object value : values) {
			if (isEmpty(value)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * In an invalidating case, invalidate only those inputs which have an empty value.
	 */
	@Override
	protected boolean shouldInvalidateInput(FacesContext context, UIInput input, Object value) {
		return isEmpty(value);
	}

}