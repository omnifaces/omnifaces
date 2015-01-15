/*
 * Copyright 2014 OmniFaces.
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
package org.omnifaces.el.functions;

import static org.omnifaces.util.Faces.getELContext;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;

/**
 * Collection of EL functions for working with components.
 *
 * @since 2.0
 * @author Arjan Tijms
 */
public final class Components {

	// Constructors ---------------------------------------------------------------------------------------------------

	private Components() {
		// Hide constructor.
	}

	// Utility --------------------------------------------------------------------------------------------------------

	/**
	 * Evaluates an attribute of a component by first checking if there's a value expression associated with it, and only if there isn't one
	 * look at a component property with that name.
	 * <p>
	 * The regular attribute collection ({@link UIComponent#getAttributes()}) does exactly the reverse; it looks at a component property
	 * first, then at the attribute collection and only looks at a value binding as the last option.
	 *
	 * @param component The component for which the attribute is to be evaluated
	 * @param name Name of attribute that is to be evaluated
	 * @return The value of the attribute, or null if either the component is null or if there's isn't an attribute by that name
	 */
	public static Object evalAttribute(UIComponent component, String name) {
		if (component == null) {
			return null;
		}

		ValueExpression valueExpression = component.getValueExpression(name);
		if (valueExpression != null) {
			return valueExpression.getValue(getELContext());
		} else {
			return component.getAttributes().get(name);
		}
	}
}
