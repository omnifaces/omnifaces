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
package org.omnifaces.component.validator;

import static org.omnifaces.util.Utils.isEmpty;

import java.util.List;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UIInput;
import jakarta.faces.context.FacesContext;

import org.omnifaces.validator.MultiFieldValidator;

/**
 * <p>
 * The <code>&lt;o:validateOne&gt;</code> validates if ONLY ONE of the given {@link UIInput} components have been filled
 * out.
 * <p>
 * The default message is
 * <blockquote>{0}: Please fill out only one of those fields</blockquote>
 * <p>
 * For general usage instructions, refer {@link ValidateMultipleFields} documentation.
 *
 * @author Bauke Scholtz
 * @since 1.2
 * @see ValidateMultipleFields
 * @see ValidatorFamily
 * @see MultiFieldValidator
 */
@FacesComponent(ValidateOne.COMPONENT_TYPE)
public class ValidateOne extends ValidateMultipleFields {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The component type, which is {@value org.omnifaces.component.validator.ValidateOne#COMPONENT_TYPE}. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.validator.ValidateOne";

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Validate if only one is filled out.
	 */
	@Override
	public boolean validateValues(FacesContext context, List<UIInput> inputs, List<Object> values) {
		boolean hasValue = false;

		for (Object value : values) {
			if (!isEmpty(value)) {
				if (hasValue) {
					return false;
				}

				hasValue = true;
			}
		}

		return hasValue;
	}

}