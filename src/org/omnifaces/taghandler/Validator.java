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

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagConfig;

import org.omnifaces.util.Components;
import org.omnifaces.util.Messages;

/**
 * The <code>&lt;o:validator&gt;</code> basically extends the <code>&lt;f:validator&gt;</code> tag family with the
 * possibility to evaluate the value expression in all attributes on a per request basis instead of on a per view
 * build time basis. This allows the developer to change the attributes on a per request basis, such as the
 * <code>disabled</code> attribute.
 * <pre>
 * &lt;o:validator validatorId="someValidatorId" disabled="#{param.disableValidation}" /&gt;
 * </pre>
 * <p>
 * When you specify for example the standard <code>&lt;f:validateLongRange&gt;</code> by
 * <code>validatorId="javax.faces.LongRange"</code>, then you'll be able to use all its attributes such as
 * <code>minimum</code> and <code>maximum</code> as per its documentation, but then with the possibility to supply
 * request based value expressions.
 * <pre>
 * &lt;o:validator validatorId="javax.faces.LongRange" minimum="#{item.minimum}" maximum="#{item.maximum}" /&gt;
 * </pre>
 * <p>
 * It is also possible to specify the validator message on a per-validator basis using the <code>message</code>
 * attribute. Any "{0}" placeholder in the message will be substituted with the label of the referenced input component.
 * Note that this attribute is ignored when the parent component has already <code>validatorMessage</code> specified.
 * <pre>
 * &lt;o:validator validatorId="javax.faces.LongRange" minimum="#{item.minimum}" maximum="#{item.maximum}"
 *   message="Please enter between #{item.minimum} and #{item.maximum} characters" /&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 */
public class Validator extends RenderTimeTagHandler {

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String ERROR_MISSING_VALIDATORID =
		"o:validator attribute 'validatorId' or 'binding' must be specified.";
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
	 * If the parent component is new, then create the {@link javax.faces.validator.Validator} based on the
	 * <code>binding</code> and/or <code>validatorId</code> attributes as per the standard JSF
	 * <code>&lt;f:validator&gt;</code> implementation and collect the render time attributes. Then create an anonymous
	 * <code>Validator</code> implementation which wraps the created <code>Validator</code> and delegates the methods
	 * to it after setting the render time attributes only and only if the <code>disabled</code> attribute evaluates
	 * <code>true</code> for the current request. Finally set the anonymous implementation on the parent component.
	 * @param context The involved facelet context.
	 * @param parent The parent component to add the <code>Validator</code> to.
	 * @throws IOException If something fails at I/O level.
	 */
	@Override
	public void apply(FaceletContext context, UIComponent parent) throws IOException {
		if (!ComponentHandler.isNew(parent)) {
			return;
		}

		final javax.faces.validator.Validator validator = createValidator(context);
		final RenderTimeAttributes attributes = collectRenderTimeAttributes(context, validator);
		final ValueExpression disabled = getValueExpression(context, "disabled", Boolean.class);
		final ValueExpression message = getValueExpression(context, "message", String.class);
		((EditableValueHolder) parent).addValidator(new javax.faces.validator.Validator() {

			@Override
			public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
				ELContext el = context.getELContext();

				if (disabled == null || Boolean.FALSE.equals(disabled.getValue(el))) {
					attributes.invokeSetters(el, validator);

					try {
						validator.validate(context, component, value);
					}
					catch (ValidatorException e) {
						if (message != null) {
							String validatorMessage = (String) message.getValue(el);

							if (validatorMessage != null) {
								String label = Components.getLabel(component);
								throw new ValidatorException(Messages.create(validatorMessage, label)
									.detail(validatorMessage, label).error().get(), e.getCause());
							}
						}

						throw e;
					}
				}
			}
		});
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Create the validator based on the <code>binding</code> and/or <code>validatorId</code> attribute.
	 * @param context The involved facelet context.
	 * @return The created validator.
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

}