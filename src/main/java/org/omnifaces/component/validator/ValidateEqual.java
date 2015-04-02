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

import java.util.HashSet;
import java.util.List;

import javax.faces.component.FacesComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.omnifaces.validator.MultiFieldValidator;

/**
 * <p>
 * The <code>&lt;o:validateEqual&gt;</code> validates if ALL of the given {@link UIInput} components have the same
 * value.
 * <p>
 * The default message is
 * <blockquote>{0}: Please fill out the same value for all of those fields</blockquote>
 * <p>
 * For general usage instructions, refer {@link ValidateMultipleFields} documentation.
 *
 * @author Bauke Scholtz
 * @see ValidateMultipleFields
 * @see ValidatorFamily
 * @see MultiFieldValidator
 */
@FacesComponent(ValidateEqual.COMPONENT_TYPE)
public class ValidateEqual extends ValidateMultipleFields {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The standard component type. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.validator.ValidateEqual";

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Validate if all values are equal.
	 */
	@Override
	public boolean validateValues(FacesContext context, List<UIInput> inputs, List<Object> values) {
		return (new HashSet<Object>(values).size() == 1);
	}

}
