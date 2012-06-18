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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.component.FacesComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

/**
 * <strong>ValidateOrderLte</strong> validates if the values of the given <code>UIInput</code> components are in exactly
 * the same order from least to greatest, allowing duplicates, as specified in the <code>components</code> attribute.
 * The default message is
 * <blockquote>{0}: Please fill out the values of all those fields in order</blockquote>
 * <p>
 * For general usage instructions, refer {@link ValidateMultipleFields} documentation.
 * <p>
 * This validator has the additional requirement that the to-be-validated values must implement {@link Comparable}.
 * This validator throws an {@link IllegalArgumentException} when one or more of the values do not implement it.
 *
 * @author Bauke Scholtz
 * @since 1.1
 */
@FacesComponent(ValidateOrderLte.COMPONENT_TYPE)
public class ValidateOrderLte extends ValidateOrder {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The standard component type. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.validator.ValidateOrderLte";

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	protected <T extends Comparable<T>> boolean validateOrder(FacesContext context, List<UIInput> components, List<T> values) {
		List<T> sortedValues = new ArrayList<T>(values);
		Collections.sort(sortedValues);
		return sortedValues.equals(values);
	}

}