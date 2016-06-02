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
import static javax.faces.component.visit.VisitHint.SKIP_UNRENDERED;
import static javax.faces.event.PhaseId.PROCESS_VALIDATIONS;
import static javax.faces.event.PhaseId.RESTORE_VIEW;
import static javax.faces.event.PhaseId.UPDATE_MODEL_VALUES;
import static javax.faces.view.facelets.ComponentHandler.isNew;
import static org.omnifaces.el.ExpressionInspector.getValueReference;
import static org.omnifaces.util.Components.forEachComponent;
import static org.omnifaces.util.Components.getClosestParent;
import static org.omnifaces.util.Components.getCurrentForm;
import static org.omnifaces.util.Components.hasInvokedSubmit;
import static org.omnifaces.util.Events.subscribeToRequestAfterPhase;
import static org.omnifaces.util.Events.subscribeToRequestBeforePhase;
import static org.omnifaces.util.Events.subscribeToViewEvent;
import static org.omnifaces.util.Facelets.getBoolean;
import static org.omnifaces.util.Facelets.getString;
import static org.omnifaces.util.Facelets.getValueExpression;
import static org.omnifaces.util.Faces.getELContext;
import static org.omnifaces.util.Faces.renderResponse;
import static org.omnifaces.util.Faces.validationFailed;
import static org.omnifaces.util.FacesLocal.evaluateExpressionGet;
import static org.omnifaces.util.Messages.createError;
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
import javax.faces.FacesException;
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
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;
import javax.validation.ConstraintViolation;

import org.omnifaces.eventlistener.BeanValidationEventListener;
import org.omnifaces.util.Callback;
import org.omnifaces.util.Platform;
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
 *     &lt;o:validateBean validationGroups="javax.validation.groups.Default,com.example.MyGroup" /&gt;
 * &lt;/h:commandButton&gt;
 * </pre>
 * <pre>
 * &lt;h:selectOneMenu value="#{bean.selectedItem}"&gt;
 *     &lt;f:selectItems value="#{bean.availableItems}" /&gt;
 *     &lt;o:validateBean disabled="true" /&gt;
 *     &lt;f:ajax execute="@form" listener="#{bean.itemChanged}" render="@form" /&gt;
 * &lt;/h:selectOneMenu&gt;
 * </pre>
 *
 * <p>
 * <b>Validate a bean at the class level</b>
 * <pre>
 * &lt;h:inputText value="#{bean.product.item}" /&gt;
 * &lt;h:inputText value="#{bean.product.order}" /&gt;
 *
 * &lt;o:validateBean value="#{bean.product}" validationGroups="com.example.MyGroup" /&gt;
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

	private static final Logger logger = Logger.getLogger(ValidateBean.class.getName());

	private static final String ERROR_MISSING_FORM =
		"o:validateBean must be nested in an UIForm.";
	private static final String ERROR_INVALID_PARENT =
		"o:validateBean parent must be an instance of UIInput or UICommand.";

	// Enums ----------------------------------------------------------------------------------------------------------

	private static enum ValidateMethod {
		validateCopy, validateActual;

		public static ValidateMethod of(String name) {
			if (isEmpty(name)) {
				return validateCopy;
			}

			return valueOf(name);
		}
	}

	// Variables ------------------------------------------------------------------------------------------------------

	private ValueExpression value;
	private boolean disabled;
	private ValidateMethod method;
	private String groups;
	private String copier;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * The tag constructor.
	 * @param config The tag config.
	 */
	public ValidateBean(TagConfig config) {
		super(config);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * If the parent component has the <code>value</code> attribute or is an instance of {@link UICommand} or
	 * {@link UIInput} and is new and we're in the restore view phase of a postback, then delegate to
	 * {@link #processValidateBean(UIComponent)}.
	 * @throws IllegalArgumentException When the <code>value</code> attribute is absent and the parent component is not
	 * an instance of {@link UICommand} or {@link UIInput}.
	 */
	@Override
	public void apply(FaceletContext context, final UIComponent parent) throws IOException {
		if (getAttribute("value") == null && (!(parent instanceof UICommand || parent instanceof UIInput))) {
			throw new IllegalArgumentException(ERROR_INVALID_PARENT);
		}

		FacesContext facesContext = context.getFacesContext();

		if (!(isNew(parent) && facesContext.isPostback() && facesContext.getCurrentPhaseId() == RESTORE_VIEW)) {
			return;
		}

		value = getValueExpression(context, getAttribute("value"), Object.class);
		disabled = getBoolean(context, getAttribute("disabled"));
		method = ValidateMethod.of(getString(context, getAttribute("method")));
		groups = getString(context, getAttribute("validationGroups"));
		copier = getAttribute("copier") == null? null : getAttribute("copier").getValue();

		// We can't use getCurrentForm() or hasInvokedSubmit() before the component is added to view, because the client ID isn't available.
		// Hence, we subscribe this check to after phase of restore view.
		subscribeToRequestAfterPhase(RESTORE_VIEW, new Callback.Void() { @Override public void invoke() {
			processValidateBean(parent);
		}});
	}

	/**
	 * Check if the given component has participated in submitting the current form or action and if so, then perform
	 * the bean validation depending on the attributes set.
	 * @param component The involved component.
	 * @throws IllegalArgumentException When the parent form is missing.
	 */
	protected void processValidateBean(UIComponent component) {
		UIForm form = (component instanceof UIForm) ? ((UIForm) component) : getClosestParent(component, UIForm.class);

		if (form == null) {
			throw new IllegalArgumentException(ERROR_MISSING_FORM);
		}

		if (!form.equals(getCurrentForm()) || (component instanceof UICommand && !hasInvokedSubmit(component))) {
			return;
		}

		Object bean = (value != null) ? value.getValue(getELContext()) : null;

		if (bean == null) {
			validateForm(groups, disabled);
			return;
		}

		if (!disabled) {
			if (method == ValidateMethod.validateActual) {
				validateActualBean(form, bean, groups);
			}
			else {
				validateCopiedBean(form, bean, copier, groups);
			}
		}
	}

	/**
	 * After update model values phase, validate actual bean. But don't proceed to render response on fail.
	 */
	private void validateActualBean(final UIForm form, final Object bean, final String groups) {
		ValidateBeanCallback validateActualBean = new ValidateBeanCallback() { @Override public void run() {
			FacesContext context = FacesContext.getCurrentInstance();
			validate(context, form, bean, groups, false);
		}};

		subscribeToRequestAfterPhase(UPDATE_MODEL_VALUES, validateActualBean);
	}

	/**
	 * Before validations phase of current request, collect all bean properties.
	 *
	 * After validations phase of current request, create a copy of the bean, set all collected properties there,
	 * then validate copied bean and proceed to render response on fail.
	 */
	private void validateCopiedBean(final UIForm form, final Object bean, final String copier, final String groups) {
		final Map<String, Object> properties = new HashMap<>();

		ValidateBeanCallback collectBeanProperties = new ValidateBeanCallback() { @Override public void run() {
			FacesContext context = FacesContext.getCurrentInstance();

			forEachInputWithMatchingBase(context, form, bean, new Callback.WithArgument<EditableValueHolder>() { @Override public void invoke(EditableValueHolder v) {
				addCollectingValidator(v, properties);
			}});
		}};

		ValidateBeanCallback checkConstraints = new ValidateBeanCallback() { @Override public void run() {
			FacesContext context = FacesContext.getCurrentInstance();

			forEachInputWithMatchingBase(context, form, bean, new Callback.WithArgument<EditableValueHolder>() { @Override public void invoke(EditableValueHolder v) {
				removeCollectingValidator(v);
			}});

			Object copiedBean = getCopier(context, copier).copy(bean);
			setProperties(copiedBean, properties);
			validate(context, form, copiedBean, groups, true);
		}};

		subscribeToRequestBeforePhase(PROCESS_VALIDATIONS, collectBeanProperties);
		subscribeToRequestAfterPhase(PROCESS_VALIDATIONS, checkConstraints);
	}

	/**
	 * Before validations phase of current request, subscribe the {@link BeanValidationEventListener} to validate the form based on groups.
	 */
	private void validateForm(final String validationGroups, final boolean disabled) {
		ValidateBeanCallback validateForm = new ValidateBeanCallback() { @Override public void run() {
			SystemEventListener listener = new BeanValidationEventListener(validationGroups, disabled);
			subscribeToViewEvent(PreValidateEvent.class, listener);
			subscribeToViewEvent(PostValidateEvent.class, listener);
		}};

		subscribeToRequestBeforePhase(PROCESS_VALIDATIONS, validateForm);
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	private static void forEachInputWithMatchingBase(final FacesContext context, UIComponent form, final Object base, final Callback.WithArgument<EditableValueHolder> callback) {
		forEachComponent(context)
			.fromRoot(form)
			.ofTypes(EditableValueHolder.class)
			.withHints(SKIP_UNRENDERED/*, SKIP_ITERATION*/) // SKIP_ITERATION fails in Apache EL (Tomcat 8.0.32 tested) but works in Oracle EL.
			.invoke(new Callback.WithArgument<UIComponent>() { @Override public void invoke(UIComponent component) {
				ValueExpression valueExpression = component.getValueExpression("value");

				if (valueExpression != null) {
					ValueReference valueReference = getValueReference(context.getELContext(), valueExpression);

					if (valueReference.getBase().equals(base)) {
						callback.invoke((EditableValueHolder) component);
					}
				}
			}});
	}

	private static void addCollectingValidator(EditableValueHolder valueHolder, Map<String, Object> propertyValues) {
		valueHolder.addValidator(new CollectingValidator(propertyValues));
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

	private static Copier getCopier(FacesContext context, String copierName) {
		Copier copier = null;

		if (!isEmpty(copierName)) {
			Object expressionResult = evaluateExpressionGet(context, copierName);

			if (expressionResult instanceof Copier) {
				copier = (Copier) expressionResult;
			}
			else if (expressionResult instanceof String) {
				copier = instance((String) expressionResult);
			}
		}

		if (copier == null) {
			copier = new MultiStrategyCopier();
		}

		return copier;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void validate(FacesContext context, UIForm form, Object bean, String groups, boolean renderResponseOnFail) {
		List<Class> groupClasses = new ArrayList<>();

		for (String group : csvToList(groups)) {
			groupClasses.add(toClass(group));
		}

		Set violationsRaw = Platform.getBeanValidator().validate(bean, groupClasses.toArray(new Class[groupClasses.size()]));
		Set<ConstraintViolation<?>> violations = violationsRaw;

		if (!violations.isEmpty()) {
			context.validationFailed();
			String formId = form.getClientId(context);

			for (ConstraintViolation<?> violation : violations) {
				context.addMessage(formId, createError(violation.getMessage()));
			}

			if (renderResponseOnFail) {
				context.renderResponse();
			}
		}
	}

	// Nested classes -------------------------------------------------------------------------------------------------

	public static final class CollectingValidator implements Validator {

		private final Map<String, Object> propertyValues;

		public CollectingValidator(Map<String, Object> propertyValues) {
			this.propertyValues = propertyValues;
		}

		@Override
		public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
			ValueExpression valueExpression = component.getValueExpression("value");

			if (valueExpression != null) {
				ValueReference valueReference = getValueReference(context.getELContext(), valueExpression);
				propertyValues.put(valueReference.getProperty().toString(), value);
			}
		}
	}

	// Callbacks ------------------------------------------------------------------------------------------------------

	private abstract static class ValidateBeanCallback implements Callback.Void {

		@Override
		public void invoke() {
			try {
				run();
			}
			catch (Exception e) {
				// Explicitly log since exceptions in PhaseListeners will be largely swallowed and ignored by JSF runtime.
				logger.log(SEVERE, "Exception occured while doing validation.", e);

				// Set validation failed and proceed to render response.
				validationFailed();
				renderResponse();

				throw new FacesException(e); // Rethrow, but JSF runtime will do little with it.
			}

		}

		public abstract void run();
	}

}