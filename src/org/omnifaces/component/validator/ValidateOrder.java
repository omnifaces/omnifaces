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
import java.util.List;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

/**
 * Base class which is to be shared between all field value ordering validators. The default message is
 * <blockquote>{0}: Please fill out the values of all those fields in order</blockquote>
 * <p>
 * For general usage instructions, refer {@link ValidateMultipleFields} documentation.
 *
 * @author Bauke Scholtz
 * @since 1.1
 */
public abstract class ValidateOrder extends ValidateMultipleFields {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String DEFAULT_MESSAGE = "{0}: Please fill out the values of all those fields in order";
	private static final String ERROR_VALUES_NOT_COMPARABLE = "All values must implement java.lang.Comparable.";

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * The default constructor sets the default message.
	 */
	public ValidateOrder() {
		super(DEFAULT_MESSAGE);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Validate if all values are in specified order.
	 */
	@Override
	protected boolean validateValues(FacesContext context, List<UIInput> components, List<Object> values) {
		try {
			return validateOrder(context, components, castToComparables(values));
		}
		catch (ClassCastException e) {
			throw new IllegalArgumentException(ERROR_VALUES_NOT_COMPARABLE, e);
		}
	}

	/**
	 * Helper method to properly cast them.
	 */
	@SuppressWarnings("unchecked")
	private <T extends Comparable<? super T>> List<T> castToComparables(List<Object> values) {
		List<T> comparables = new ArrayList<T>();

		for (Object value : values) {
			comparables.add((T) value);
		}

		return comparables;
	}

	/**
	 * Validate if all comparable values are in specified order.
	 * @see #validateValues(FacesContext, List, List)
	 */
	protected abstract <T extends Comparable<? super T>> boolean
		validateOrder(FacesContext context, List<UIInput> components, List<T> values);

}