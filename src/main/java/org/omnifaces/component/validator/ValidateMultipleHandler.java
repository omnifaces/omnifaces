/*
 * Copyright OmniFaces
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

import static java.util.logging.Level.FINEST;

import java.util.List;
import java.util.logging.Logger;

import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.facelets.ComponentConfig;
import jakarta.faces.view.facelets.ComponentHandler;
import jakarta.faces.view.facelets.FaceletContext;
import jakarta.faces.view.facelets.TagAttribute;

import org.omnifaces.validator.MultiFieldValidator;

/**
 * A handler for {@link ValidateMultiple} component, which will take care of setting the <code>validator</code>
 * attribute the right way as either {@link ValueExpression} or {@link MethodExpression}.
 *
 * @author Juliano Marques
 * @author Bauke Scholtz
 * @since 1.7
 */
public class ValidateMultipleHandler extends ComponentHandler {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final Logger logger = Logger.getLogger(ValidateMultipleHandler.class.getName());

	private static final Class<?>[] PARAM_TYPES = { FacesContext.class, List.class, List.class };

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct the tag handler for {@link ValidateMultiple} component.
	 * @param config The component config.
	 */
	public ValidateMultipleHandler(ComponentConfig config) {
		super(config);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Delegate to super and then try to get a {@link ValueExpression} representing a {@link MultiFieldValidator}
	 * implementation and set it as <code>validator</code> property of current {@link ValidateMultiple} component, or
	 * if it couldn't be obtained, then get a {@link MethodExpression} representing a method with the same signature
	 * as {@link MultiFieldValidator#validateValues(FacesContext, List, List)} and set it as <code>validateMethod</code>
	 * property of current {@link ValidateMultiple} component.
	 */
	@Override
	public void setAttributes(FaceletContext context, Object component) {
		super.setAttributes(context, component);
		ValidateMultiple validateMultiple = (ValidateMultiple) component;
		TagAttribute attribute = getRequiredAttribute("validator");

		try {
			ValueExpression valueExpression = attribute.getValueExpression(context, MultiFieldValidator.class);
			MultiFieldValidator validator = (MultiFieldValidator) valueExpression.getValue(context);
			validateMultiple.setValidator(validator);
		}
		catch (Exception ignore) { // At least, ClassCastException and PropertyNotFoundException are expected.
			logger.log(FINEST, "Ignoring thrown exception; there is really no clean way to distinguish a ValueExpression from a MethodExpression.", ignore);
			MethodExpression validateMethod = attribute.getMethodExpression(context, boolean.class, PARAM_TYPES);
			validateMultiple.setValidateMethod(validateMethod);
		}
	}

}