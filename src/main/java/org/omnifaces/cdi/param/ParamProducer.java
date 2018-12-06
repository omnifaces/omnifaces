/*
 * Copyright 2018 OmniFaces
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
package org.omnifaces.cdi.param;

import static java.lang.Boolean.parseBoolean;
import static java.util.Arrays.asList;
import static javax.faces.component.UIInput.EMPTY_STRING_AS_NULL_PARAM_NAME;
import static javax.faces.validator.BeanValidator.DISABLE_DEFAULT_BEAN_VALIDATOR_PARAM_NAME;
import static org.omnifaces.util.Beans.getQualifier;
import static org.omnifaces.util.Components.setLabel;
import static org.omnifaces.util.Faces.createConverter;
import static org.omnifaces.util.Faces.createValidator;
import static org.omnifaces.util.Faces.evaluateExpressionGet;
import static org.omnifaces.util.Faces.getApplication;
import static org.omnifaces.util.Faces.getInitParameter;
import static org.omnifaces.util.FacesLocal.getMessageBundle;
import static org.omnifaces.util.FacesLocal.getRequestParameterValues;
import static org.omnifaces.util.FacesLocal.getRequestPathInfo;
import static org.omnifaces.util.Messages.createError;
import static org.omnifaces.util.Platform.isBeanValidationAvailable;
import static org.omnifaces.util.Platform.validateBeanProperty;
import static org.omnifaces.util.Reflection.setPropertiesWithCoercion;
import static org.omnifaces.util.Utils.coalesce;
import static org.omnifaces.util.Utils.containsByClassName;
import static org.omnifaces.util.Utils.getDefaultValue;
import static org.omnifaces.util.Utils.isEmpty;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.validator.BeanValidator;
import javax.faces.validator.RequiredValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import javax.inject.Inject;

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

	private static volatile Boolean interpretEmptyStringSubmittedValuesAsNull;

	@SuppressWarnings("unused") // Workaround for OpenWebBeans not properly passing it as produce() method argument.
	@Inject
	private InjectionPoint injectionPoint;

	@Produces
	@Param
	public <V> ParamValue<V> produce(InjectionPoint injectionPoint) {
		Param param = getQualifier(injectionPoint, Param.class);
		String name = getName(param, injectionPoint);
		int pathIndex = param.pathIndex();
		String label = getLabel(param, injectionPoint);
		Type type = injectionPoint.getType();

		if (type instanceof ParameterizedType && ParamValue.class.isAssignableFrom((Class<?>) ((ParameterizedType) type).getRawType())) {
			type = ((ParameterizedType) type).getActualTypeArguments()[0];
		}

		FacesContext context = FacesContext.getCurrentInstance();
		String[] submittedValues = pathIndex > -1 ? getPathParameter(context, pathIndex) : getRequestParameterValues(context, name);
		Object[] convertedValues = getConvertedValues(context, param, label, submittedValues, type);
		V paramValue = coerceValues(type, convertedValues);

		if (!validateValues(context, param, label, submittedValues, convertedValues, paramValue, injectionPoint)) {
			paramValue = null;
		}

		return new ParamValue<>(submittedValues, param, type, paramValue);
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

	static Object[] getConvertedValues(FacesContext context, Param param, String label, String[] submittedValues, Type type) {
		if (submittedValues == null) {
			return null;
		}

		Object[] convertedValues = new Object[submittedValues.length];
		UIComponent component = context.getViewRoot();
		Object originalLabel = component.getAttributes().get("label");
		boolean valid = true;

		try {
			setLabel(component, label);
			Converter converter = getConverter(param, getTargetType(type));

			for (int i = 0; i < submittedValues.length; i++) {
				String submittedValue = submittedValues[i];

				if (submittedValue != null && interpretEmptyStringSubmittedValuesAsNull(context) && submittedValue.isEmpty()) {
					submittedValue = null;
					submittedValues[i] = null;
				}

				try {
					convertedValues[i] = (converter != null) ? converter.getAsObject(context, component, submittedValue) : submittedValue;
				}
				catch (ConverterException e) {
					valid = false;
					addConverterMessage(context, component, label, submittedValue, e, getConverterMessage(param));
				}
			}
		}
		finally {
			setLabel(component, originalLabel);
		}

		if (!valid) {
			context.validationFailed();
			return null;
		}

		return convertedValues;
	}

	private static boolean interpretEmptyStringSubmittedValuesAsNull(FacesContext context) {
		if (interpretEmptyStringSubmittedValuesAsNull != null) {
			return interpretEmptyStringSubmittedValuesAsNull;
		}

		interpretEmptyStringSubmittedValuesAsNull = parseBoolean(context.getExternalContext().getInitParameter(EMPTY_STRING_AS_NULL_PARAM_NAME));

		return interpretEmptyStringSubmittedValuesAsNull;
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

	private static <V> boolean validateValues(FacesContext context, Param param, String label, String[] submittedValues, Object[] convertedValues, V paramValue, InjectionPoint injectionPoint) {
		boolean valid = true;

		UIComponent component = context.getViewRoot();
		Object originalLabel = component.getAttributes().get("label");

		try {
			setLabel(component, label);
			valid = validateRequired(context, param, label, convertedValues);

			if (valid) {
				valid = validateBean(context, param, label, paramValue, injectionPoint);
			}

			if (valid && convertedValues != null) {
				valid = validateFaces(context, param, label, convertedValues, submittedValues);
			}
		}
		finally {
			setLabel(component, originalLabel);
		}

		if (!valid) {
			context.validationFailed();
		}

		return valid;
	}

	private static boolean validateRequired(FacesContext context, Param param, String label, Object[] convertedValues) {
		if (param.required() && isEmpty(convertedValues)) {
			addRequiredMessage(context, context.getViewRoot(), label, getRequiredMessage(param));
			return false;
		}

		return true;
	}

	private static <V> boolean validateBean(FacesContext context, Param param, String label, V paramValue, InjectionPoint injectionPoint) {
		if (shouldDoBeanValidation(param, injectionPoint)) {
			Map<String, String> violations = doBeanValidation(paramValue, injectionPoint);

			if (!violations.isEmpty()) {
				for (String message : violations.values()) {
					context.addMessage(context.getViewRoot().getClientId(context), createError(message, label));
				}

				return false;
			}
		}

		return true;
	}

	private static boolean validateFaces(FacesContext context, Param param, String label, Object[] convertedValues, String[] submittedValues) {
		boolean valid = true;

		for (Validator validator : getValidators(param)) {
			int i = 0;

			for (Object convertedValue : convertedValues) {
				try {
					validator.validate(context, context.getViewRoot(), convertedValue);
				}
				catch (ValidatorException e) {
					addValidatorMessages(context, context.getViewRoot(), label, submittedValues[i], e, getValidatorMessage(param));
					valid = false;
				}

				i++;
			}
		}

		return valid;
	}

	private static Converter getConverter(Param requestParameter, Class<?> targetType) {
		Object classIdentifier = requestParameter.converterClass() == Converter.class ? targetType : requestParameter.converterClass();
		Converter converter = createConverter(coalesce(evaluateExpressionGet(requestParameter.converter()), classIdentifier));

		if (converter != null) {
			setPropertiesWithCoercion(converter, getConverterAttributes(requestParameter));
		}

		return converter;
	}

	private static <V> Class<V> getTargetType(Type type) {
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

	private static String getName(Param requestParameter, InjectionPoint injectionPoint) {

		String name = requestParameter.name();

		if (isEmpty(name)) {
			name = injectionPoint.getMember().getName();
		} else {
			name = evaluateExpressionAsString(name);
		}

		return name;
	}

	private static String getLabel(Param requestParameter, InjectionPoint injectionPoint) {

		String label = requestParameter.label();

		if (isEmpty(label)) {
			label = getName(requestParameter, injectionPoint);
		} else {
			label = evaluateExpressionAsString(label);
		}

		return label;
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

	private static <V> Map<String, String> doBeanValidation(V paramValue, InjectionPoint injectionPoint) {

		Class<?> base = injectionPoint.getBean().getBeanClass();
		String property = injectionPoint.getMember().getName();
		Type type = injectionPoint.getType();

		// Check if the target property in which we are injecting in our special holder/wrapper type
		// ParamValue or not. If it's the latter, pre-wrap our value (otherwise types for bean validation
		// would not match)
		Object valueToValidate = paramValue;

		if (type instanceof ParameterizedType) {
			Type propertyRawType = ((ParameterizedType) type).getRawType();
			if (propertyRawType.equals(ParamValue.class)) {
				valueToValidate = new ParamValue<>(null, null, null, paramValue);
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
		} else {
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
		} else {
			// (Ab)use RequiredValidator to get the same message that all required attributes are using.
			try {
				new RequiredValidator().validate(context, component, null);
			} catch (ValidatorException ve) {
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
		} else {
			for (FacesMessage facesMessage : getFacesMessages(ve)) {
				context.addMessage(clientId, facesMessage);
			}
		}
	}

	private static List<FacesMessage> getFacesMessages(ValidatorException ve) {
		List<FacesMessage> facesMessages = new ArrayList<>();
		if (ve.getFacesMessages() != null) {
			facesMessages.addAll(ve.getFacesMessages());
		} else if (ve.getFacesMessage() != null) {
			facesMessages.add(ve.getFacesMessage());
		}

		return facesMessages;
	}

}