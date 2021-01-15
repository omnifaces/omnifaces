/*
 * Copyright 2021 OmniFaces
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
package org.omnifaces.taghandler;

import static org.omnifaces.taghandler.DeferredTagHandlerHelper.collectDeferredAttributes;
import static org.omnifaces.taghandler.DeferredTagHandlerHelper.getValueExpression;
import static org.omnifaces.util.Components.getLabel;

import java.io.IOException;
import java.io.Serializable;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.ValidatorException;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagHandlerDelegate;
import javax.faces.view.facelets.ValidatorConfig;
import javax.faces.view.facelets.ValidatorHandler;

import org.omnifaces.cdi.validator.ValidatorManager;
import org.omnifaces.taghandler.DeferredTagHandlerHelper.DeferredAttributes;
import org.omnifaces.taghandler.DeferredTagHandlerHelper.DeferredTagHandler;
import org.omnifaces.taghandler.DeferredTagHandlerHelper.DeferredTagHandlerDelegate;
import org.omnifaces.util.Messages;

/**
 * <p>
 * The <code>&lt;o:validator&gt;</code> is a taghandler that extends the standard <code>&lt;f:validator&gt;</code> tag
 * family with support for deferred value expressions in all attributes. In other words, the validator attributes are
 * not evaluated anymore on a per view build time basis, but just on every access like as with UI components and bean
 * properties. This has among others the advantage that they can be evaluated on a per-iteration basis inside an
 * iterating component, and that they can be set on a custom validator without needing to explicitly register it in a
 * tagfile.
 *
 * <h3>Usage</h3>
 * <p>
 * When you specify for example the standard <code>&lt;f:validateLongRange&gt;</code> by
 * <code>validatorId="javax.faces.LongRange"</code>, then you'll be able to use all its attributes such as
 * <code>minimum</code> and <code>maximum</code> as per its documentation, but then with the possibility to supply
 * deferred value expressions.
 * <pre>
 * &lt;o:validator validatorId="javax.faces.LongRange" minimum="#{item.minimum}" maximum="#{item.maximum}" /&gt;
 * </pre>
 * <p>
 * The validator ID of all standard JSF validators can be found in
 * <a href="https://jakarta.ee/specifications/platform/8/apidocs/javax/faces/validator/package-summary.html">their javadocs</a>.
 * First go to the javadoc of the class of interest, then go to <code>VALIDATOR_ID</code> in its field summary
 * and finally click the Constant Field Values link to see the value.
 * <p>
 * It is also possible to specify the validator message on a per-validator basis using the <code>message</code>
 * attribute. Any "{0}" placeholder in the message will be substituted with the label of the referenced input component.
 * Note that this attribute is ignored when the parent component has already <code>validatorMessage</code> specified.
 * <pre>
 * &lt;o:validator validatorId="javax.faces.LongRange" minimum="#{item.minimum}" maximum="#{item.maximum}"
 *     message="Please enter between #{item.minimum} and #{item.maximum} characters" /&gt;
 * </pre>
 *
 * <h3>JSF 2.3 compatibility</h3>
 * <p>
 * The <code>&lt;o:validator&gt;</code> is currently not compatible with validators which are managed via JSF 2.3's
 * new <code>managed=true</code> attribute set on the {@link FacesValidator} annotation, at least not when using
 * Mojarra. Internally, the converters are wrapped in another instance which doesn't have the needed setter methods
 * specified. In order to get them to work with <code>&lt;o:validator&gt;</code>, the <code>managed=true</code>
 * attribute needs to be removed, so that OmniFaces {@link ValidatorManager} will automatically manage them.
 *
 * @author Bauke Scholtz
 * @see DeferredTagHandlerHelper
 */
public class Validator extends ValidatorHandler implements DeferredTagHandler {

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * The constructor.
	 * @param config The validator config.
	 */
	public Validator(ValidatorConfig config) {
		super(config);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Create a {@link javax.faces.validator.Validator} based on the <code>binding</code> and/or
	 * <code>validatorId</code> attributes as per the standard JSF <code>&lt;f:validator&gt;</code> implementation and
	 * collect the render time attributes. Then create an anonymous <code>Validator</code> implementation which wraps
	 * the created <code>Validator</code> and delegates the methods to it after setting the render time attributes only
	 * and only if the <code>disabled</code> attribute evaluates <code>true</code> for the current request. Finally set
	 * the anonymous implementation on the parent component.
	 * @param context The involved facelet context.
	 * @param parent The parent component to add the <code>Validator</code> to.
	 * @throws IOException If something fails at I/O level.
	 */
	@Override
	public void apply(FaceletContext context, UIComponent parent) throws IOException {
		boolean insideCompositeComponent = UIComponent.getCompositeComponentParent(parent) != null;

		if (!ComponentHandler.isNew(parent) && !insideCompositeComponent) {
			// If it's not new nor inside a composite component, we're finished.
			return;
		}

		if (!(parent instanceof EditableValueHolder) || (insideCompositeComponent && getAttribute("for") == null)) {
			// It's inside a composite component and not reattached. TagHandlerDelegate will pickup it and pass the target component back if necessary.
			super.apply(context, parent);
			return;
		}

		ValueExpression binding = getValueExpression(context, this, "binding", Object.class);
		ValueExpression id = getValueExpression(context, this, "validatorId", String.class);
		ValueExpression disabled = getValueExpression(context, this, "disabled", Boolean.class);
		ValueExpression message = getValueExpression(context, this, "message", String.class);
		javax.faces.validator.Validator<Object> validator = createInstance(context.getFacesContext(), context, binding, id);
		DeferredAttributes attributes = collectDeferredAttributes(context, this, validator);
		((EditableValueHolder) parent).addValidator(new DeferredValidator(validator, binding, id, disabled, message, attributes));
	}

	@Override
	public TagAttribute getTagAttribute(String name) {
		return getAttribute(name);
	}

	@Override
	protected TagHandlerDelegate getTagHandlerDelegate() {
		return new DeferredTagHandlerDelegate(this, super.getTagHandlerDelegate());
	}

	@Override
	public boolean isDisabled(FaceletContext context) {
		return false; // Let the deferred validator handle it.
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private static javax.faces.validator.Validator<Object> createInstance(FacesContext facesContext, ELContext elContext, ValueExpression binding, ValueExpression id) {
		return DeferredTagHandlerHelper.createInstance(elContext, binding, id, facesContext.getApplication()::createValidator, "validator");
	}

	// Nested classes -------------------------------------------------------------------------------------------------

	/**
	 * So that we can have a serializable validator.
	 *
	 * @author Bauke Scholtz
	 */
	protected static class DeferredValidator implements javax.faces.validator.Validator<Object>, Serializable {
		private static final long serialVersionUID = 1L;

		private transient javax.faces.validator.Validator<Object> validator;
		private final ValueExpression binding;
		private final ValueExpression id;
		private final ValueExpression disabled;
		private final ValueExpression message;
		private final DeferredAttributes attributes;

		public DeferredValidator(javax.faces.validator.Validator<Object> validator, ValueExpression binding, ValueExpression id, ValueExpression disabled, ValueExpression message, DeferredAttributes attributes) {
			this.validator = validator;
			this.binding = binding;
			this.id = id;
			this.disabled = disabled;
			this.message = message;
			this.attributes = attributes;
		}

		@Override
		public void validate(FacesContext context, UIComponent component, Object value) {
			if (disabled == null || Boolean.FALSE.equals(disabled.getValue(context.getELContext()))) {
				try {
					getValidator(context).validate(context, component, value);
				}
				catch (ValidatorException e) {
					rethrowValidatorException(context, component, message, e);
				}
			}
		}

		private javax.faces.validator.Validator<Object> getValidator(FacesContext context) {
			if (validator == null) {
				validator = Validator.createInstance(context, context.getELContext(), binding, id);
			}

			attributes.invokeSetters(context.getELContext(), validator);
			return validator;
		}

		private void rethrowValidatorException(FacesContext context, UIComponent component, ValueExpression message, ValidatorException e) {
			if (message != null) {
				String validatorMessage = (String) message.getValue(context.getELContext());

				if (validatorMessage != null) {
					String label = getLabel(component);
					throw new ValidatorException(Messages.create(validatorMessage, label)
						.detail(validatorMessage, label).error().get(), e.getCause());
				}
			}

			throw e;
		}
	}

}