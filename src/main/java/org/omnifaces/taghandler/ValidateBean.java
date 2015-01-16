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

import static java.util.logging.Level.SEVERE;
import static javax.faces.event.PhaseId.PROCESS_VALIDATIONS;
import static javax.faces.event.PhaseId.UPDATE_MODEL_VALUES;
import static javax.faces.view.facelets.ComponentHandler.isNew;
import static org.omnifaces.el.ExpressionInspector.getValueReference;
import static org.omnifaces.util.Components.forEachComponent;
import static org.omnifaces.util.Components.getClosestParent;
import static org.omnifaces.util.Components.getCurrentForm;
import static org.omnifaces.util.Components.hasInvokedSubmit;
import static org.omnifaces.util.Events.subscribeToViewAfterPhase;
import static org.omnifaces.util.Events.subscribeToViewBeforePhase;
import static org.omnifaces.util.Events.subscribeToViewEvent;
import static org.omnifaces.util.Facelets.getBoolean;
import static org.omnifaces.util.Facelets.getObject;
import static org.omnifaces.util.Facelets.getString;
import static org.omnifaces.util.FacesLocal.evaluateExpressionGet;
import static org.omnifaces.util.Messages.createError;
import static org.omnifaces.util.Platform.getBeanValidator;
import static org.omnifaces.util.Reflection.instance;
import static org.omnifaces.util.Reflection.setProperties;
import static org.omnifaces.util.Reflection.toClass;
import static org.omnifaces.util.Utils.csvToList;
import static org.omnifaces.util.Utils.isEmpty;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.el.ValueExpression;
import javax.el.ValueReference;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.PostValidateEvent;
import javax.faces.event.PreValidateEvent;
import javax.faces.event.SystemEventListener;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;
import javax.validation.ConstraintViolation;

import org.omnifaces.eventlistener.BeanValidationEventListener;
import org.omnifaces.util.Callback;
import org.omnifaces.util.Callback.WithArgument;
import org.omnifaces.util.Faces;
import org.omnifaces.util.copier.CloneCopier;
import org.omnifaces.util.copier.Copier;
import org.omnifaces.util.copier.CopyCtorCopier;
import org.omnifaces.util.copier.MultiStrategyCopier;
import org.omnifaces.util.copier.NewInstanceCopier;
import org.omnifaces.util.copier.SerializationCopier;

/**
 * <p>
 * The <code>&lt;o:validateBean&gt;</code> allows the developer to control bean validation on a per-{@link UICommand}
 * or {@link UIInput} component basis, as well as validating a given bean at the class level.
 *
 * <p>
 * The standard <code>&lt;f:validateBean&gt;</code> only allows validation control on a per-form
 * or a per-request basis (by using multiple tags and conditional EL expressions in its attributes) which may end up in
 * boilerplate code.
 *
 * <p>
 * The standard <code>&lt;f:validateBean&gt;</code> also, despite its name, does not actually have any facilities to
 * validate a bean at all.
 *
 * <h3>Usage</h3>
 * <p>
 * Some examples
 *
 * <p>
 * <b>Control bean validation per component</b>
 * <pre>
 * &lt;h:commandButton value="submit" action="#{bean.submit}"&gt;
 *     &lt;o:validateBean validationGroups="javax.validation.groups.Default,com.example.MyGroup"/&gt;
 * &lt;/h:commandButton&gt;
 * </pre>
 * <pre>
 * &lt;h:selectOneMenu value="#{bean.selectedItem}"&gt;
 *     &lt;f:selectItems value="#{bean.availableItems}"
 *     &lt;o:validateBean disabled="true" /&gt;
 *     &lt;f:ajax execute="@form" listener="#{bean.itemChanged}" render="@form" /&gt;
 * &lt;/h:commandButton&gt;
 * </pre>
 *
 * <p>
 * <b>Validate a bean at the class level</b>
 * <pre>
 *  &lt;h:inputText value="#{bean.product.item}" / &gt;
 *  &lt;h:inputText value="#{bean.product.order}" / &gt;
 *
 *  &lt;o:validateBean value="#{bean.product}" validationGroups="com.example.MyGroup" / &gt;
 * </pre>
 *
 * <h3>Class level validation details</h3>
 * <p>
 * In order to validate a bean at the class level, all values from input components should first be actually set on that bean
 * and only thereafter should the bean be validated. This however does not play well with the JSF approach where a model
 * is only updated when validation passes. But for class level validation we seemingly can not validate until the model
 * is updated. To break this tie, a <em>copy</em> of the model bean is made first, and then values are stored in this copy
 * and validated there. If validation passes, the original bean is updated.
 *
 * <p>
 * A bean is copied using the following strategies (in the order indicated):
 * <ol>
 * <li> <b>Cloning</b> - Bean must implement the {@link Cloneable} interface and support cloning according to the rules of that interface. See {@link CloneCopier}
 * <li> <b>Serialization</b> - Bean must implement the {@link Serializable} interface and support serialization according to the rules of that interface. See {@link SerializationCopier}
 * <li> <b>Copy constructor</b> - Bean must have an additional constructor (next to the default constructor) taking a single argument of its own
 *      type that initializes itself with the values of that passed in type. See {@link CopyCtorCopier}
 * <li> <b>New instance</b> - Bean should have a public no arguments (default) constructor. Every official JavaBean satisfies this requirement. Note
 *      that in this case no copy is made of the original bean, but just a new instance is created. See {@link NewInstanceCopier}
 * </ol>
 *
 * <p>
 * If the above order is not ideal, or if an custom copy strategy is needed (e.g. when it's only needed to copy a few fields for the validation)
 * a strategy can be supplied explicitly via the <code>copier</code> attribute. The value of this attribute can be any of the build-in copier implementations
 * given above, or can be a custom implementation of the {@link Copier} interface.
 *
 *
 * @author Bauke Scholtz
 * @author Arjan Tijms
 * @see BeanValidationEventListener
 */
public class ValidateBean extends TagHandler {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String ERROR_INVALID_PARENT =
		"Parent component of o:validateBean must be an instance of UICommand or UIInput.";

	private static final Logger logger = Logger.getLogger(ValidateBean.class.getName());

	private static final Class<?>[] CLASS_ARRAY = new Class<?>[0];


	// Enums ----------------------------------------------------------------------------------------------------------

	private static enum ValidateMethod {
		validateCopy, validateActual
	}


	// Variables ------------------------------------------------------------------------------------------------------

	private TagAttribute validationGroupsAttribute;
	private TagAttribute disabledAttribute;
	private TagAttribute copierAttribute;
	private TagAttribute methodAttribute;
	private TagAttribute valueAttribute;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * The tag constructor.
	 * @param config The tag config.
	 */
	public ValidateBean(TagConfig config) {
		super(config);
		validationGroupsAttribute = getAttribute("validationGroups");
		disabledAttribute = getAttribute("disabled");
		copierAttribute = getAttribute("copier");
		methodAttribute = getAttribute("method");
		valueAttribute = getAttribute("value");

	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 *
	 */
	@Override
	public void apply(FaceletContext context, final UIComponent parent) throws IOException {
		if (valueAttribute == null && !(parent instanceof UICommand || parent instanceof UIInput)) {
			throw new IllegalArgumentException(ERROR_INVALID_PARENT);
		}

		if (!isNew(parent)) {
			return;
		}

		final boolean disabled = getBoolean(disabledAttribute, context);
		final String validationGroups = getString(validationGroupsAttribute, context);
		final Object targetBase = getObject(valueAttribute, context);
		final String copierName = getString(copierAttribute, context);
		final String method = getString(methodAttribute, context);

		if (targetBase != null) {

			final List<Class<?>> groups = toClasses(validationGroups);

			switch (getMethod(method)) {
				case validateActual:
					Callback.Void validateTargetBase = new TargetFormInvoker(parent, new WithArgument<UIForm>() { @Override	public void invoke(UIForm targetForm) {

			        	final FacesContext context = FacesContext.getCurrentInstance();

		                Set<ConstraintViolation<?>> violations = validate(targetBase, groups);

		                if (!violations.isEmpty()) {
		                    context.validationFailed();
		                    for (ConstraintViolation<?> violation : violations) {
		    					context.addMessage(targetForm.getClientId(context), createError(violation.getMessage()));
		    				}
		                }

					}});

					subscribeToViewAfterPhase(UPDATE_MODEL_VALUES, validateTargetBase);
					break;

				case validateCopy:
					final Map<String, Object> properties = new HashMap<>();

					// Callback that adds a validator to each input for which its value binding resolves to a base that is the same as the target
			        // of the o:validateBean. This validator will then not actually validate, but just capture the value for that input.
			        //
			        // E.g. in "h:inputText value=bean.target.property and o:validateBean value=bean.target", this will collect property=[captured value].

			        Callback.Void collectPropertyValues = new TargetFormInvoker(parent, new WithArgument<UIForm>() { @Override	public void invoke(UIForm targetForm) {

			        	final FacesContext context = FacesContext.getCurrentInstance();

			        	forEachInputWithMatchingBase(context, targetForm, targetBase, new Operation() { @Override public void invoke(EditableValueHolder v, ValueReference vr) {
			        		/* (v, vr) -> */ addCollectingValidator(v, vr, properties);
		            	}});

					}});


			        // Callback that uses the values collected by our previous callback (collectPropertyValues) to initialize a copied bean with, and which
			        // then validated this copy.

			        Callback.Void checkConstraints = new TargetFormInvoker(parent, new WithArgument<UIForm>() { @Override	public void invoke(UIForm targetForm) {

			        	final FacesContext context = FacesContext.getCurrentInstance();

			        	// First remove the collecting validator again, since it will otherwise be state saved with the component at the end of the lifecycle.

			        	forEachInputWithMatchingBase(context, targetForm, targetBase, new Operation() { @Override public void invoke(EditableValueHolder v, ValueReference vr) {
			        		/* (v, vr) -> */ removeCollectingValidator(v);
		            	}});

			        	// Copy our target base instance, so validation can be done against that instead of against the real instance.
			        	// This is done so that in case of a validation error the real base (model) isn't polluted with invalid values.

			        	Object targetBaseCopy = getCopier(context, copierName).copy(targetBase);

			        	// Set all properties on the copied base instance exactly as the input components are
			        	// going to do this on the real base
			        	setProperties(targetBaseCopy, properties);

		                Set<ConstraintViolation<?>> violations = validate(targetBaseCopy, groups);

		                if (!violations.isEmpty()) {
		                    context.validationFailed();
		                    context.renderResponse();
		                    for (ConstraintViolation<?> violation : violations) {
		    					context.addMessage(targetForm.getClientId(context), createError(violation.getMessage()));
		    				}
		                }

					}});

			        subscribeToViewBeforePhase(PROCESS_VALIDATIONS, collectPropertyValues);
			        subscribeToViewAfterPhase(PROCESS_VALIDATIONS, checkConstraints);

			        break;
			}
		} else {
			subscribeToViewBeforePhase(PROCESS_VALIDATIONS, new Callback.Void() {

				@Override
				public void invoke() {
					if (hasInvokedSubmit(parent)) {
						SystemEventListener listener = new BeanValidationEventListener(validationGroups, disabled);
						subscribeToViewEvent(PreValidateEvent.class, listener);
						subscribeToViewEvent(PostValidateEvent.class, listener);
					}
				}
			});
		}
	}

	private void forEachInputWithMatchingBase(final FacesContext context, UIComponent targetForm, final Object targetBase, final Operation operation) {
		forEachComponent(context)
			.fromRoot(targetForm)
			.ofTypes(EditableValueHolder.class)
			.invoke(new Callback.WithArgument<UIComponent>() { @Override public void invoke(UIComponent component) {

				ValueExpression valueExpression = component.getValueExpression("value");
				if (valueExpression != null) {
					ValueReference valueReference = getValueReference(context.getELContext(), valueExpression);
					if (valueReference.getBase().equals(targetBase)) {
						operation.invoke((EditableValueHolder) component, valueReference);
					}
				}
			}}
	  );
	}

	public static final class CollectingValidator implements Validator {

		private final Map<String, Object> propertyValues;
		private final String property;

		public CollectingValidator(Map<String, Object> propertyValues, String property) {
			this.propertyValues = propertyValues;
			this.property = property;
		}

		@Override
		public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
			propertyValues.put(property, value);
		}
	}

	public final class TargetFormInvoker implements Callback.Void {

		private final UIComponent parent;
		private WithArgument<UIForm> operation;

		public TargetFormInvoker(UIComponent parent, WithArgument<UIForm> operation) {
			this.parent = parent;
			this.operation = operation;
		}

		@Override
		public void invoke() {

			// Check if any form has been submitted at all
			UIForm submittedForm = getCurrentForm();
			if (submittedForm == null) {
				return;
			}

			// A form has been submitted, get the form we're nested in
			UIForm targetForm = getTargetForm(parent);

			// Check if the form that was submitted is the same one as we're nested in
			if (submittedForm.equals(targetForm)) {
				try {
					operation.invoke(targetForm);
				} catch (Exception e) {
					// Log and set validation to failed, since exceptions in PhaseListeners will
					// be largely swallowed and ignored by JSF runtime
					logger.log(SEVERE, "Exception occured while doing validation", e);
					Faces.validationFailed();
					Faces.renderResponse();
					throw e; // Rethrow, but JSF runtime will do little with it.
				}
			}
		}
	}

	private abstract static class Operation implements Callback.WithArgument<Object[]> {
		@Override
		public void invoke(Object[] args) {
			invoke((EditableValueHolder) args[0], (ValueReference) args[1]);
		}

		public abstract void invoke(EditableValueHolder valueHolder, ValueReference valueReference);
	}

	private static void addCollectingValidator(EditableValueHolder valueHolder, ValueReference valueReference,  Map<String, Object> propertyValues) {
		valueHolder.addValidator(new CollectingValidator(propertyValues, valueReference.getProperty().toString()));
	}

	private static void removeCollectingValidator(EditableValueHolder valueHolder) {
		Validator collectingValidator = null;
		for (Validator validator : valueHolder.getValidators()) {
			if (validator instanceof CollectingValidator) {
				collectingValidator = validator;
				break;
			}
		}

		if (collectingValidator != null) {
			valueHolder.removeValidator(collectingValidator);
		}
	}

	private List<Class<?>> toClasses(String validationGroups) {
		final List<Class<?>> groups = new ArrayList<>();

		for (String type : csvToList(validationGroups)) {
			groups.add(toClass(type));
		}

		return groups;
	}

	private Copier getCopier(FacesContext context, String copierName) {

		Copier copier = null;

		if (!isEmpty(copierName)) {
			Object expressionResult = evaluateExpressionGet(context, copierName);
			if (expressionResult instanceof Copier) {
				copier = (Copier) expressionResult;
			} else if (expressionResult instanceof String) {
				copier = instance((String) expressionResult);
			}
		}

		if (copier == null) {
			copier = new MultiStrategyCopier();
		}

		return copier;
	}

	private ValidateMethod getMethod(String methodName) {
		if (isEmpty(methodName)) {
			return ValidateMethod.validateCopy;
		}

		return ValidateMethod.valueOf(methodName);
	}


	private UIForm getTargetForm(UIComponent parent) {
		 if (parent instanceof UIForm) {
             return (UIForm) parent;
         }

         return getClosestParent(parent, UIForm.class);
	}

	private Set<ConstraintViolation<?>> validate(Object value, List<Class<?>> groups) {
		@SuppressWarnings("rawtypes")
        Set violationsRaw = getBeanValidator().validate(value, groups.toArray(CLASS_ARRAY));

        @SuppressWarnings("unchecked")
        Set<ConstraintViolation<?>> violations = violationsRaw;

        return violations;
	}

}