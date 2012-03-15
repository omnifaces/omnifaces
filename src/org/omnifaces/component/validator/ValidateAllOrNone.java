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
 * <strong>ValidateAllOrNone</strong> validates if at least ALL of the given <code>UIInput</code> components have been
 * filled in or that NONE of the given <code>UIInput</code> components have been filled in. The default message is
 * <blockquote>{0}: Please fill out all or none of those fields</blockquote>
 * <p>
 * For general usage instructions, refer {@link ValidateMultipleFields} documentation.
 *
 * @author Bauke Scholtz
 */
@FacesComponent(ValidateAllOrNone.COMPONENT_TYPE)
public class ValidateAllOrNone extends ValidateMultipleFields {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The standard component type. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.validator.ValidateAllOrNone";

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String DEFAULT_MESSAGE = "{0}: Please fill out all or none of those fields";

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * The default constructor sets the default message.
	 */
	public ValidateAllOrNone() {
		super(DEFAULT_MESSAGE);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Validate if all or none is filled in.
	 */
	@Override
	protected boolean validateValues(FacesContext context, List<UIInput> inputs, List<Object> values) {
		boolean hasValue = false;
		boolean hasNoValue = false;

		for (Object value : values) {
			boolean currentHasValue = !Utils.isEmpty(value);
			hasValue |= currentHasValue;
			hasNoValue |= !currentHasValue;
		}

		return (hasValue != hasNoValue);
	}

}