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
package org.omnifaces.cdi.param;

import static jakarta.faces.component.UIInput.EMPTY_STRING_AS_NULL_PARAM_NAME;
import static jakarta.faces.validator.BeanValidator.DISABLE_DEFAULT_BEAN_VALIDATOR_PARAM_NAME;
import static java.lang.Boolean.parseBoolean;
import static java.util.Arrays.asList;
import static org.omnifaces.util.Beans.getQualifier;
import static org.omnifaces.util.Components.createValueExpression;
import static org.omnifaces.util.Faces.createConverter;
import static org.omnifaces.util.Faces.createValidator;
import static org.omnifaces.util.Faces.evaluateExpressionGet;
import static org.omnifaces.util.Faces.getApplication;
import static org.omnifaces.util.Faces.getInitParameter;
import static org.omnifaces.util.FacesLocal.getMessageBundle;
import static org.omnifaces.util.FacesLocal.getRequestParameterValues;
import static org.omnifaces.util.FacesLocal.getRequestPathInfo;
import static org.omnifaces.util.Messages.createError;
import static org.omnifaces.util.Reflection.setPropertiesWithCoercion;
import static org.omnifaces.util.Utils.coalesce;
import static org.omnifaces.util.Utils.containsByClassName;
import static org.omnifaces.util.Utils.getDefaultValue;
import static org.omnifaces.util.Utils.isEmpty;
import static org.omnifaces.util.Validators.isBeanValidationAvailable;
import static org.omnifaces.util.Validators.validateBeanProperty;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.BooleanSupplier;

import jakarta.el.ValueExpression;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.faces.application.Application;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIInput;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.validator.BeanValidator;
import jakarta.faces.validator.RequiredValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;

import org.omnifaces.cdi.Param;

/**
 * Producer for a request or path parameter as defined by the <code>&#64;</code>{@link Param} annotation.
 *
 * @since 1.6
 * @author Arjan Tijms
 */
@Dependent
@SuppressWarnings({"rawtypes", "unchecked"}) // For now.
public class ParamProducer {

	private static final String DEFAULT_REQUIRED_MESSAGE = "{0}: Value is required";
	private static final String LABEL = "label";
	private static final String VALUE = "value";

	private static Boolean interpretEmptyStringSubmittedValuesAsNull;

	@SuppressWarnings("unused") // Workaround for OpenWebBeans not properly passing it as produce() method argument.
	@Inject
	private InjectionPoint injectionPoint;

	/**
	 * Returns {@link ParamValue} associated with param name derived from given injection point.
	 * @param <V> The generic {@link ParamValue} type.
	 * @param injectionPoint Injection point to derive param name from.
	 * @return {@link ParamValue} associated with param name derived from given injection point.
	 */
	@Produces
	@Param
	public <V> ParamValue<V> produce(InjectionPoint injectionPoint) {
		Param param = getQualifier(injectionPoint, Param.class);
		String name = getName(param, injectionPoint);
		String label = getLabel(param, injectionPoint);
		Type type = getType(injectionPoint);

		FacesContext context = FacesContext.getCurrentInstance();
		String[] submittedValues = param.pathIndex() > -1 ? getPathParameter(context, param.pathIndex()) : getSubmittedValues(context, name);
		Class<V> targetType = getTargetType(type);
		ParamValue<V> paramValue = new ParamValue<>(param, name, label, type, submittedValues, targetType);
		Object[] convertedValues = getConvertedValues(context, paramValue);
		V value = coerceValues(type, convertedValues);
		paramValue.setValue(validateValues(context, paramValue, convertedValues, value, injectionPoint) ? value : null);

		return paramValue;
	}

	private static String getName(Param requestParameter, InjectionPoint injectionPoint) {
		String name = requestParameter.name();

		if (isEmpty(name)) {
			if (injectionPoint.getAnnotated() instanceof AnnotatedParameter) {
				AnnotatedParameter<?> annotatedParameter = (AnnotatedParameter<?>) injectionPoint.getAnnotated();
				Parameter javaParameter = annotatedParameter.getJavaParameter();

				if (javaParameter.isNamePresent()) {
					return javaParameter.getName();
				}
			}

			name = injectionPoint.getMember().getName();
		}
		else {
			name = evaluateExpressionAsString(name);
		}

		return name;
	}

	private static String getLabel(Param requestParameter, InjectionPoint injectionPoint) {
		String label = requestParameter.label();

		if (isEmpty(label)) {
			label = getName(requestParameter, injectionPoint);
		}
		else {
			label = evaluateExpressionAsString(label);
		}

		return label;
	}

	private static Type getType(InjectionPoint injectionPoint) {
		Type type = injectionPoint.getType();

		if (type instanceof ParameterizedType && ParamValue.class.isAssignableFrom((Class<?>) ((ParameterizedType) type).getRawType())) {
			type = ((ParameterizedType) type).getActualTypeArguments()[0];
		}

		return type;
	}

	private static String[] getPathParameter(FacesContext context, int pathIndex) {
		String pathInfo = getRequestPathInfo(context);

		if (pathInfo != null) {
			String[] pathParts = pathInfo.substring(1).split("/");

			if (pathIndex < pathParts.length) {
				return new String[] { pathParts[pathIndex] };
			}
		}

		return null;
	}

	private static String[] getSubmittedValues(FacesContext context, String name) {
		String[] requestParameterValues = getRequestParameterValues(context, name);

		if (requestParameterValues == null) {
			return null;
		}

		String[] submittedValues = new String[requestParameterValues.length];

		for (int i = 0; i < requestParameterValues.length; i++) {
			String requestParameterValue = requestParameterValues[i];

			if (requestParameterValue != null && requestParameterValue.isEmpty() && interpretEmptyStringSubmittedValuesAsNull(context)) {
				submittedValues[i] = null;
			}
			else {
				submittedValues[i] = requestParameterValue;
			}
		}

		return submittedValues;
	}

	private static boolean interpretEmptyStringSubmittedValuesAsNull(FacesContext context) {
		if (interpretEmptyStringSubmittedValuesAsNull != null) {
			return interpretEmptyStringSubmittedValuesAsNull;
		}

		interpretEmptyStringSubmittedValuesAsNull = parseBoolean(context.getExternalContext().getInitParameter(EMPTY_STRING_AS_NULL_PARAM_NAME));

		return interpretEmptyStringSubmittedValuesAsNull;
	}

	static Object[] getConvertedValues(FacesContext context, ParamValue paramValue) {
		if (paramValue.submittedValues == null) {
			return null;
		}

		Object[] convertedValues = new Object[paramValue.submittedValues.length];
		boolean valid = runWithSimulatedLabelAndValueOnViewRoot(context, paramValue, () -> invokeConverter(context, paramValue, convertedValues));

		if (!valid) {
			context.validationFailed();
			return null;
		}

		return convertedValues;
	}

	private static boolean invokeConverter(FacesContext context, ParamValue paramValue, Object[] convertedValues) {
		boolean valid = true;
		Converter converter = getConverter(paramValue);

		for (int i = 0; i < paramValue.submittedValues.length; i++) {
			String submittedValue = paramValue.submittedValues[i];

			try {
				convertedValues[i] = (converter != null) ? converter.getAsObject(context, context.getViewRoot(), submittedValue) : submittedValue;
			}
			catch (ConverterException e) {
				valid = false;
				addConverterMessage(context, context.getViewRoot(), paramValue.label, submittedValue, e, getConverterMessage(paramValue.param));
			}
		}

		return valid;
	}

	private static Converter getConverter(ParamValue paramValue) {
		Object classIdentifier = paramValue.param.converterClass() == Converter.class ? paramValue.targetType : paramValue.param.converterClass();
		Converter converter = createConverter(coalesce(evaluateExpressionGet(paramValue.param.converter()), classIdentifier));

		if (converter != null) {
			setPropertiesWithCoercion(converter, getConverterAttributes(paramValue.param));
		}

		return converter;
	}

	private static boolean runWithSimulatedLabelAndValueOnViewRoot(FacesContext context, ParamValue paramValue, BooleanSupplier callback) {
		UIComponent component = context.getViewRoot();
		Object originalLabel = getAttribute(component, LABEL);
		Object originalValue = getAttribute(component, VALUE);

		try {
			setAttribute(component, LABEL, paramValue.label);
			setAttribute(component, VALUE, createValueExpression("#{param['" + paramValue.name + "']}", paramValue.targetType)); // This gives any converter the opportunity to inspect the target type.
			return callback.getAsBoolean();
		}
		finally {
			setAttribute(component, LABEL, originalLabel);
			setAttribute(component, VALUE, originalValue);
		}
	}

	private static Object getAttribute(UIComponent component, String name) {
		ValueExpression valueExpression = component.getValueExpression(name);

		if (valueExpression != null) {
			return valueExpression;
		}
		else {
			return component.getAttributes().get(name);
		}
	}

	private static void setAttribute(UIComponent component, String name, Object value) {
		if (value instanceof ValueExpression) {
			component.setValueExpression(name, (ValueExpression) value);
		}
		else if (value != null) {
			component.getAttributes().put(name, value);
		}
		else {
			component.getAttributes().remove(name);
		}
	}

	static <V> V coerceValues(Type type, Object... values) {
		if (type instanceof ParameterizedType) {
			return coerceValues(((ParameterizedType) type).getRawType(), values);
		}

		if (!(type instanceof Class)) {
			return null;
		}

		Class<?> cls = (Class<?>) type;
		Object coercedValue = null;

		if (values != null) {
			if (cls.isArray()) {
				coercedValue = Array.newInstance(cls.getComponentType(), values.length);

				for (int i = 0; i < values.length; i++) {
					Array.set(coercedValue, i, coerceValues(cls.getComponentType(), values[i]));
				}
			}
			else if (List.class.isAssignableFrom(cls)) {
				coercedValue = asList(values);
			}
			else {
				coercedValue = values.length == 0 ? null : values[0];
			}
		}

		if (coercedValue == null) {
			coercedValue = getDefaultValue(cls);
		}

		return (V) coercedValue;
	}

	private static <V> boolean validateValues(FacesContext context, ParamValue paramValue, Object[] convertedValues, V value, InjectionPoint injectionPoint) {
		boolean valid = runWithSimulatedLabelAndValueOnViewRoot(context, paramValue, () -> invokeValidators(context, paramValue, convertedValues, value, injectionPoint));

		if (!valid) {
			context.validationFailed();
		}

		return valid;
	}

	private static <V> boolean invokeValidators(FacesContext context, ParamValue paramValue, Object[] convertedValues, V value, InjectionPoint injectionPoint) {
		boolean valid = validateRequired(context, paramValue, convertedValues);

		if (valid) {
			valid = validateBean(context, paramValue, value, injectionPoint);
		}

		if (valid && convertedValues != null) {
			valid = validateFaces(context, paramValue, convertedValues);
		}

		return valid;
	}

	private static boolean validateRequired(FacesContext context, ParamValue paramValue, Object[] convertedValues) {
		if (paramValue.param.required() && (isEmpty(convertedValues) || asList(convertedValues).contains(null))) {
			addRequiredMessage(context, context.getViewRoot(), paramValue.label, getRequiredMessage(paramValue.param));
			return false;
		}

		return true;
	}

	private static <V> boolean validateBean(FacesContext context, ParamValue paramValue, V value, InjectionPoint injectionPoint) {
		if (shouldDoBeanValidation(paramValue.param, injectionPoint)) {
			Set<ConstraintViolation<?>> violations = doBeanValidation(value, injectionPoint);

			if (!violations.isEmpty()) {
				for (ConstraintViolation<?> violation : violations) {
					context.addMessage(context.getViewRoot().getClientId(context), createError(violation.getMessage(), paramValue.label));
				}

				return false;
			}
		}

		return true;
	}

	private static boolean validateFaces(FacesContext context, ParamValue paramValue, Object[] convertedValues) {
		boolean valid = true;

		for (Validator validator : getValidators(paramValue.param)) {
			int i = 0;

			for (Object convertedValue : convertedValues) {
				try {
					validator.validate(context, context.getViewRoot(), convertedValue);
				}
				catch (ValidatorException e) {
					addValidatorMessages(context, context.getViewRoot(), paramValue.label, paramValue.submittedValues[i], e, getValidatorMessage(paramValue.param));
					valid = false;
				}

				i++;
			}
		}

		return valid;
	}

	static <V> Class<V> getTargetType(Type type) {
		if (type instanceof Class && ((Class<?>) type).isArray()) {
			return (Class<V>) ((Class<?>) type).getComponentType();
		}
		else if (type instanceof ParameterizedType) {
			return (Class<V>) ((ParameterizedType) type).getActualTypeArguments()[0];
		}
		else {
			return (Class<V>) type;
		}
	}

	private static String getValidatorMessage(Param requestParameter) {
		return evaluateExpressionAsString(requestParameter.validatorMessage());
	}

	private static String getConverterMessage(Param requestParameter) {
		return evaluateExpressionAsString(requestParameter.converterMessage());
	}

	private static String getRequiredMessage(Param requestParameter) {
		return evaluateExpressionAsString(requestParameter.requiredMessage());
	}

	private static String evaluateExpressionAsString(String expression) {
		if (isEmpty(expression)) {
			return expression;
		}

		Object expressionResult = evaluateExpressionGet(expression);

		if (expressionResult == null) {
			return null;
		}

		return expressionResult.toString();
	}

	private static boolean shouldDoBeanValidation(Param requestParameter, InjectionPoint injectionPoint) {

		// If bean validation is explicitly disabled for this instance, immediately return false
		if (requestParameter.disableBeanValidation()) {
			return false;
		}

		// Next check if bean validation has been disabled globally, but only if this hasn't been overridden locally
		if (!requestParameter.overrideGlobalBeanValidationDisabled() && parseBoolean(getInitParameter(DISABLE_DEFAULT_BEAN_VALIDATOR_PARAM_NAME))) {
			return false;
		}

		// Next check if this is a field injection; other cases are not supported by Validator#validateValue().
		if (!(injectionPoint.getMember() instanceof Field)) {
			return false;
		}

		// For all other cases, the availability of bean validation determines if we attempt bean validation or not.
		return isBeanValidationAvailable();
	}

	private static <V> Set<ConstraintViolation<?>> doBeanValidation(V value, InjectionPoint injectionPoint) {
		Class<?> base = injectionPoint.getBean().getBeanClass();
		String property = injectionPoint.getMember().getName();
		Type type = injectionPoint.getType();

		// Check if the target property in which we are injecting in our special holder/wrapper type
		// ParamValue or not. If it's the latter, pre-wrap our value (otherwise types for bean validation
		// would not match)
		Object valueToValidate = value;

		if (type instanceof ParameterizedType) {
			Type propertyRawType = ((ParameterizedType) type).getRawType();
			if (propertyRawType.equals(ParamValue.class)) {
				valueToValidate = new ParamValue<>(value);
			}
		}

		return validateBeanProperty(base, property, valueToValidate);
	}

	private static List<Validator> getValidators(Param requestParameter) {
		List<Validator> validators = new ArrayList<>();

		for (String validatorIdentifier : requestParameter.validators()) {
			Object evaluatedValidatorIdentifier = evaluateExpressionGet(validatorIdentifier);
			Validator validator = createValidator(evaluatedValidatorIdentifier);

			if (validator != null) {
				validators.add(validator);
			}
		}

		for (Class<? extends Validator> validatorClass : requestParameter.validatorClasses()) {
			Validator validator = createValidator(validatorClass);

			if (validator != null) {
				validators.add(validator);
			}
		}

		// Process the default validators
		Application application = getApplication();

		for (Entry<String, String> validatorEntry :	application.getDefaultValidatorInfo().entrySet()) {

			String validatorID = validatorEntry.getKey();
			String validatorClassName = validatorEntry.getValue();

			// Check that the validator ID is not the BeanValidator one which we handle in a special way.
			// And make sure the default validator is not already set manually as well.
			if (!validatorID.equals(BeanValidator.VALIDATOR_ID) && !containsByClassName(validators, validatorClassName)) {
				validators.add(application.createValidator(validatorID));
			}
		}

		// Set the attributes on all instantiated validators. We don't distinguish here
		// which attribute should go to which validator.
		Map<String, Object> validatorAttributes = getValidatorAttributes(requestParameter);

		for (Validator validator : validators) {
			setPropertiesWithCoercion(validator, validatorAttributes);
		}

		return validators;
	}

	private static Map<String, Object> getConverterAttributes(Param requestParameter) {
		Map<String, Object> attributeMap = new HashMap<>();

		Attribute[] attributes = requestParameter.converterAttributes();
		for (Attribute attribute : attributes) {
			attributeMap.put(attribute.name(), evaluateExpressionGet(attribute.value()));
		}

		return attributeMap;
	}

	private static Map<String, Object> getValidatorAttributes(Param requestParameter) {
		Map<String, Object> attributeMap = new HashMap<>();

		Attribute[] attributes = requestParameter.validatorAttributes();
		for (Attribute attribute : attributes) {
			attributeMap.put(attribute.name(), evaluateExpressionGet(attribute.value()));
		}

		return attributeMap;
	}

	private static void addConverterMessage(FacesContext context, UIComponent component, String label, String submittedValue, ConverterException ce, String converterMessage) {
		FacesMessage message;

		if (!isEmpty(converterMessage)) {
			message = createError(converterMessage, submittedValue, label);
		}
		else {
			message = ce.getFacesMessage();

			if (message == null) {
				// If the converter didn't add a FacesMessage, set a generic one.
				message = createError("Conversion failed for {0} because: {1}", submittedValue, ce.getMessage());
			}
		}

		context.addMessage(component.getClientId(context), message);
	}

	private static void addRequiredMessage(FacesContext context, UIComponent component, String label, String requiredMessage) {

		FacesMessage message = null;

		if (!isEmpty(requiredMessage)) {
			message = createError(requiredMessage, null, label);
		}
		else {
			// (Ab)use RequiredValidator to get the same message that all required attributes are using.
			try {
				new RequiredValidator().validate(context, component, null);
			}
			catch (ValidatorException ve) {
				message = ve.getFacesMessage();
			}

			if (message == null) {
				// RequiredValidator didn't throw or its exception did not have a message set.
				ResourceBundle messageBundle = getMessageBundle(context);
				String defaultRequiredMessage = (messageBundle != null) ? messageBundle.getString(UIInput.REQUIRED_MESSAGE_ID) : null;
				message = createError(coalesce(defaultRequiredMessage, requiredMessage, DEFAULT_REQUIRED_MESSAGE), label);
			}
		}

		context.addMessage(component.getClientId(context), message);
	}

	private static void addValidatorMessages(FacesContext context, UIComponent component, String label, String submittedValue, ValidatorException ve, String validatorMessage) {
		String clientId = component.getClientId(context);

		if (!isEmpty(validatorMessage)) {
			context.addMessage(clientId, createError(validatorMessage, submittedValue, label));
		}
		else {
			for (FacesMessage facesMessage : getFacesMessages(ve)) {
				context.addMessage(clientId, facesMessage);
			}
		}
	}

	private static List<FacesMessage> getFacesMessages(ValidatorException ve) {
		List<FacesMessage> facesMessages = new ArrayList<>();

		if (ve.getFacesMessages() != null) {
			facesMessages.addAll(ve.getFacesMessages());
		}
		else if (ve.getFacesMessage() != null) {
			facesMessages.add(ve.getFacesMessage());
		}

		return facesMessages;
	}

}