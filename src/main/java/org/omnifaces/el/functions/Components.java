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
public class Components {
	
	// Constructors ---------------------------------------------------------------------------------------------------

	private Components() {
		// Hide constructor.
	}

	// Utility --------------------------------------------------------------------------------------------------------

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
