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

import java.util.List;

import javax.faces.component.FacesComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.omnifaces.util.Utils;

/**
 * <strong>ValidateOneOrNone</strong> validates if ONLY ONE of the given <code>UIInput</code> components has been
 * filled out or that NONE of the given <code>UIInput</code> components have been filled out. The default message is
 * <blockquote>{0}: Please fill out only one or none of those fields</blockquote>
 * <p>
 * For general usage instructions, refer {@link ValidateMultipleFields} documentation.
 *
 * @author Bauke Scholtz
 * @since 1.2
 */
@FacesComponent(ValidateOneOrNone.COMPONENT_TYPE)
public class ValidateOneOrNone extends ValidateMultipleFields {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The standard component type. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.validator.ValidateOneOrNone";

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String DEFAULT_MESSAGE = "{0}: Please fill out only one or none of those fields";

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * The default constructor sets the default message.
	 */
	public ValidateOneOrNone() {
		super(DEFAULT_MESSAGE);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Validate if only one or none is filled out.
	 */
	@Override
	public boolean validateValues(FacesContext context, List<UIInput> inputs, List<Object> values) {
		boolean hasValue = false;

		for (Object value : values) {
			if (!Utils.isEmpty(value)) {
				if (hasValue) {
					return false;
				}

				hasValue = true;
			}
		}

		return true;
	}

}