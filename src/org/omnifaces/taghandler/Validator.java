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
package org.omnifaces.taghandler;

import java.io.IOException;

import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

/**
 * The <code>&lt;o:validator&gt;</code> basically extends the <code>&lt;f:validator&gt;</code> with the possibility to
 * evaluate the value expression in the <code>disabled</code> attribute on a per request basis instead of on a per view
 * build time basis. This allows the developer to change the <code>disabled</code> attribute on a per request basis.
 *
 * @author Bauke Scholtz
 */
public class Validator extends TagHandler {

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String ERROR_MISSING_VALIDATORID =
		"o:validator attribute 'validatorId' must be specified.";
	private static final String ERROR_INVALID_VALIDATORID =
		"o:validator attribute 'validatorId' must refer an valid validator ID. The validator ID '%s' cannot be found.";

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * The tag constructor.
	 * @param config The tag config.
	 */
	public Validator(TagConfig config) {
		super(config);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * If the parent component is new, then create the <code>Validator</code> based on the <code>binding</code> and/or
	 * <code>validatorId</code> attributes as per the standard JSF <code>&lt;f:validator&gt;</code> implementation and
	 * add it to the parent input component as an anonymous {@link javax.faces.validator.Validator} implementation
	 * which wraps the created <code>Validator</code> and invokes its
	 * {@link Validator#validate(FacesContext, UIComponent, Object)} only and only if the <code>disabled</code>
	 * attribute evaluates <code>true</code> for the current request.
	 * @param context The involved facelet context.
	 * @param parent The parent component to attach the validator on.
	 * @throws IOException If something fails at I/O level.
	 */
	@Override
	public void apply(FaceletContext context, UIComponent parent) throws IOException {
		if (!ComponentHandler.isNew(parent)) {
			return;
		}

		final javax.faces.validator.Validator validator = createValidator(context);
		final ValueExpression disabled = getValueExpression(context, "disabled", Boolean.class);
		((EditableValueHolder) parent).addValidator(new javax.faces.validator.Validator() {

			@Override
			public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
				if (disabled == null || Boolean.FALSE.equals(disabled.getValue(context.getELContext()))) {
					validator.validate(context, component, value);
				}
			}
		});
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Create the {@link javax.faces.validator.Validator} based on the <code>binding</code> and/or
	 * <code>validatorId</code> attribute.
	 * @param context The involved facelet context.
	 * @return The created {@link javax.faces.validator.Validator}.
	 * @throws IllegalArgumentException If the <code>validatorId</code> attribute is invalid or missing while the
	 * <code>binding</code> attribute is also missing.
	 * @see Application#createValidator(String)
	 */
	private javax.faces.validator.Validator createValidator(FaceletContext context) {
		ValueExpression binding = getValueExpression(context, "binding", javax.faces.validator.Validator.class);
		ValueExpression validatorId = getValueExpression(context, "validatorId", String.class);
		javax.faces.validator.Validator validator = null;

		if (binding != null) {
			validator = (javax.faces.validator.Validator) binding.getValue(context);
		}

		if (validatorId != null) {
			try {
				validator = context.getFacesContext().getApplication()
					.createValidator((String) validatorId.getValue(context));
			}
			catch (FacesException e) {
				throw new IllegalArgumentException(String.format(ERROR_INVALID_VALIDATORID, validatorId));
			}

			if (binding != null) {
				binding.setValue(context, validator);
			}
		}
		else if (validator == null) {
			throw new IllegalArgumentException(ERROR_MISSING_VALIDATORID);
		}

		return validator;
	}

	/**
	 * Convenience method to get the given attribute as a {@link ValueExpression}, or <code>null</code> if there is no
	 * such attribute.
	 * @param context The involved facelet context.
	 * @param name The attribute name to return the value expression for.
	 * @param type The type of the value expression.
	 * @return The given attribute as a {@link ValueExpression}.
	 */
	private <T> ValueExpression getValueExpression(FaceletContext context, String name, Class<T> type) {
		TagAttribute attribute = getAttribute(name);
		return (attribute != null) ? attribute.getValueExpression(context, type) : null;
	}

}