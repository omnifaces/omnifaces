/*
 * Copyright 2019 OmniFaces
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
package org.omnifaces.validator;

import java.util.List;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.omnifaces.component.validator.ValidateMultipleFields;

/**
 * Generic interface for multi field validator.
 *
 * @author Bauke Scholtz
 * @since 1.7
 * @see ValidateMultipleFields
 */
public interface MultiFieldValidator {

	/**
	 * Perform the validation on the collected values of the input components and returns whether the validation is
	 * successful. Note: this validator does NOT throw {@link ValidatorException}! This job is up to the
	 * {@link ValidateMultipleFields} implementation who will call this method.
	 * @param context The faces context to work with.
	 * @param components The input components whose values are to be validated.
	 * @param values The values of the input components to be validated, in the same order as the components.
	 * @return <code>true</code> if validation is successful, otherwise <code>false</code> (and thus show the message).
	 */
	boolean validateValues(FacesContext context, List<UIInput> components, List<Object> values);

}