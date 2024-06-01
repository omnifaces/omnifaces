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
package org.omnifaces.cdi.param;

import static jakarta.faces.component.UIInput.EMPTY_STRING_AS_NULL_PARAM_NAME;
import static jakarta.faces.validator.BeanValidator.DISABLE_DEFAULT_BEAN_VALIDATOR_PARAM_NAME;
import static java.lang.Boolean.parseBoolean;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;
import static org.omnifaces.util.Beans.getQualifier;
import static org.omnifaces.util.Components.LABEL_ATTRIBUTE;
import static org.omnifaces.util.Components.VALUE_ATTRIBUTE;
import static org.omnifaces.util.Components.createValueExpression;
import static org.omnifaces.util.Components.setAttribute;
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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.InjectionPoint;
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
        var param = getQualifier(injectionPoint, Param.class);
        var name = getName(param, injectionPoint);
        var label = getLabel(param, injectionPoint);
        var type = getType(injectionPoint);

        var context = FacesContext.getCurrentInstance();
        var submittedValues = param.pathIndex() > -1 ? getPathParameter(context, param.pathIndex()) : getSubmittedValues(context, name);
        var sourceType = getSourceType(type);
        Class<V> targetType = getTargetType(type);
        var paramValue = new ParamValue<>(param, name, label, sourceType, submittedValues, targetType);
        var convertedValues = getConvertedValues(context, paramValue);
        var value = (V) coerceValues(sourceType, convertedValues);
        paramValue.setValue(validateValues(context, paramValue, convertedValues, value, injectionPoint) ? value : null);

        return paramValue;
    }

    private static String getName(Param requestParameter, InjectionPoint injectionPoint) {
        var name = requestParameter.name();

        if (isEmpty(name)) {
            if (injectionPoint.getAnnotated() instanceof AnnotatedParameter<?> annotatedParameter) {
                var javaParameter = annotatedParameter.getJavaParameter();

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
        var label = requestParameter.label();

        if (isEmpty(label)) {
            label = getName(requestParameter, injectionPoint);
        }
        else {
            label = evaluateExpressionAsString(label);
        }

        return label;
    }

    private static Type getType(InjectionPoint injectionPoint) {
        var type = injectionPoint.getType();

        if (type instanceof ParameterizedType parameterizedType && ParamValue.class.isAssignableFrom((Class<?>) parameterizedType.getRawType())) {
            type = parameterizedType.getActualTypeArguments()[0];
        }

        return type;
    }

    private static String[] getPathParameter(FacesContext context, int pathIndex) {
        var pathInfo = getRequestPathInfo(context);

        if (!isEmpty(pathInfo)) {
            if (pathInfo.charAt(0) == '/') {
                pathInfo = pathInfo.substring(1);
            }

            var pathParts = pathInfo.split("/");

            if (pathIndex < pathParts.length) {
                return new String[] { pathParts[pathIndex] };
            }
        }

        return null;
    }

    private static String[] getSubmittedValues(FacesContext context, String name) {
        var requestParameterValues = getRequestParameterValues(context, name);

        if (requestParameterValues == null) {
            return null;
        }

        var submittedValues = new String[requestParameterValues.length];

        for (var i = 0; i < requestParameterValues.length; i++) {
            var requestParameterValue = requestParameterValues[i];

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

        var convertedValues = new Object[paramValue.submittedValues.length];
        var valid = runWithSimulatedLabelAndValueOnViewRoot(context, paramValue, () -> invokeConverter(context, paramValue, convertedValues));

        if (!valid) {
            context.validationFailed();
            return null;
        }

        return convertedValues;
    }

    private static boolean invokeConverter(FacesContext context, ParamValue paramValue, Object[] convertedValues) {
        var valid = true;
        var converter = getConverter(paramValue);

        for (var i = 0; i < paramValue.submittedValues.length; i++) {
            var submittedValue = paramValue.submittedValues[i];

            try {
                convertedValues[i] = converter != null ? converter.getAsObject(context, context.getViewRoot(), submittedValue) : submittedValue;
            }
            catch (ConverterException e) {
                valid = false;
                addConverterMessage(context, paramValue, submittedValue, e);
            }
        }

        return valid;
    }

    private static Converter getConverter(ParamValue paramValue) {
        var classIdentifier = paramValue.param.converterClass() == Converter.class ? paramValue.targetType : paramValue.param.converterClass();
        Converter converter = createConverter(coalesce(evaluateExpressionGet(paramValue.param.converter()), classIdentifier));

        if (converter != null) {
            setPropertiesWithCoercion(converter, getConverterAttributes(paramValue.param));
        }

        return converter;
    }

    private static boolean runWithSimulatedLabelAndValueOnViewRoot(FacesContext context, ParamValue paramValue, BooleanSupplier callback) {
        UIComponent component = context.getViewRoot();
        var originalLabel = getAttribute(component, LABEL_ATTRIBUTE);
        var originalValue = getAttribute(component, VALUE_ATTRIBUTE);

        try {
            setAttribute(component, LABEL_ATTRIBUTE, paramValue.label);
            setAttribute(component, VALUE_ATTRIBUTE, createValueExpression("#{param['" + paramValue.name + "']}", paramValue.targetType)); // This gives any converter the opportunity to inspect the target type.
            return callback.getAsBoolean();
        }
        finally {
            setAttribute(component, LABEL_ATTRIBUTE, originalLabel);
            setAttribute(component, VALUE_ATTRIBUTE, originalValue);
        }
    }

    private static Object getAttribute(UIComponent component, String name) {
        var valueExpression = component.getValueExpression(name);

        if (valueExpression != null) {
            return valueExpression;
        }
        else {
            return component.getAttributes().get(name);
        }
    }

    static <V> V coerceValues(Class<?> sourceType, Object... values) {
        Object coercedValue = null;

        if (values != null) {
            if (sourceType.isArray()) {
                coercedValue = Array.newInstance(sourceType.getComponentType(), values.length);

                for (var i = 0; i < values.length; i++) {
                    Array.set(coercedValue, i, coerceValues(sourceType.getComponentType(), values[i]));
                }
            }
            else if (List.class.isAssignableFrom(sourceType)) {
                coercedValue = asList(values);
            }
            else {
                coercedValue = values.length == 0 ? null : values[0];
            }
        }

        if (coercedValue == null) {
            coercedValue = getDefaultValue(sourceType);
        }

        return (V) coercedValue;
    }

    private static <V> boolean validateValues(FacesContext context, ParamValue paramValue, Object[] convertedValues, V value, InjectionPoint injectionPoint) {
        var valid = runWithSimulatedLabelAndValueOnViewRoot(context, paramValue, () -> invokeValidators(context, paramValue, convertedValues, value, injectionPoint));

        if (!valid) {
            context.validationFailed();
        }

        return valid;
    }

    private static <V> boolean invokeValidators(FacesContext context, ParamValue paramValue, Object[] convertedValues, V value, InjectionPoint injectionPoint) {
        var valid = validateRequired(context, paramValue, convertedValues);

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
            addRequiredMessage(context, paramValue);
            return false;
        }

        return true;
    }

    private static <V> boolean validateBean(FacesContext context, ParamValue paramValue, V value, InjectionPoint injectionPoint) {
        if (shouldDoBeanValidation(paramValue.param, injectionPoint)) {
            Set<ConstraintViolation<?>> violations = doBeanValidation(value, injectionPoint);

            if (!violations.isEmpty()) {
                for (var violation : violations) {
                    context.addMessage(paramValue.getClientId(context), createError(violation.getMessage(), paramValue.label));
                }

                return false;
            }
        }

        return true;
    }

    private static boolean validateFaces(FacesContext context, ParamValue paramValue, Object[] convertedValues) {
        var valid = true;

        for (var validator : getValidators(paramValue.param)) {
            var i = 0;

            for (var convertedValue : convertedValues) {
                try {
                    validator.validate(context, context.getViewRoot(), convertedValue);
                }
                catch (ValidatorException e) {
                    addValidatorMessages(context, paramValue, paramValue.submittedValues[i], e);
                    valid = false;
                }

                i++;
            }
        }

        return valid;
    }

    static Class<?> getSourceType(Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            return (Class<?>) parameterizedType.getRawType();
        }
        else {
            return (Class<?>) type;
        }
    }

    static <V> Class<V> getTargetType(Type type) {
        if (type instanceof Class<?> classType && classType.isArray()) {
            return (Class<V>) classType.getComponentType();
        }
        else if (type instanceof ParameterizedType parameterizedType) {
            return (Class<V>) parameterizedType.getActualTypeArguments()[0];
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

        var expressionResult = evaluateExpressionGet(expression);

        if (expressionResult == null) {
            return null;
        }

        return expressionResult.toString();
    }

    private static boolean shouldDoBeanValidation(Param requestParameter, InjectionPoint injectionPoint) {

        // If bean validation is explicitly disabled for this instance, immediately return false


        // Next check if bean validation has been disabled globally, but only if this hasn't been overridden locally
        // Next check if this is a field injection; other cases are not supported by Validator#validateValue().
        if (requestParameter.disableBeanValidation() || !requestParameter.overrideGlobalBeanValidationDisabled() && parseBoolean(getInitParameter(DISABLE_DEFAULT_BEAN_VALIDATOR_PARAM_NAME)) || !(injectionPoint.getMember() instanceof Field)) {
            return false;
        }

        // For all other cases, the availability of bean validation determines if we attempt bean validation or not.
        return isBeanValidationAvailable();
    }

    private static <V> Set<ConstraintViolation<?>> doBeanValidation(V value, InjectionPoint injectionPoint) {
        var base = injectionPoint.getBean().getBeanClass();
        var property = injectionPoint.getMember().getName();
        var type = injectionPoint.getType();

        // Check if the target property in which we are injecting in our special holder/wrapper type
        // ParamValue or not. If it's the latter, pre-wrap our value (otherwise types for bean validation
        // would not match)
        Object valueToValidate = value;

        if (type instanceof ParameterizedType parameterizedType) {
            var propertyRawType = parameterizedType.getRawType();
            if (propertyRawType.equals(ParamValue.class)) {
                valueToValidate = new ParamValue<>(value);
            }
        }

        return validateBeanProperty(base, property, valueToValidate);
    }

    private static List<Validator> getValidators(Param requestParameter) {
        var validators = new ArrayList<Validator>();

        for (var validatorIdentifier : requestParameter.validators()) {
            var evaluatedValidatorIdentifier = evaluateExpressionGet(validatorIdentifier);
            var validator = createValidator(evaluatedValidatorIdentifier);

            if (validator != null) {
                validators.add(validator);
            }
        }

        for (var validatorClass : requestParameter.validatorClasses()) {
            var validator = createValidator(validatorClass);

            if (validator != null) {
                validators.add(validator);
            }
        }

        // Process the default validators
        var application = getApplication();

        for (var validatorEntry : application.getDefaultValidatorInfo().entrySet()) {
            var validatorID = validatorEntry.getKey();
            var validatorClassName = validatorEntry.getValue();

            // Check that the validator ID is not the BeanValidator one which we handle in a special way.
            // And make sure the default validator is not already set manually as well.
            if (!BeanValidator.VALIDATOR_ID.equals(validatorID) && !containsByClassName(validators, validatorClassName)) {
                validators.add(application.createValidator(validatorID));
            }
        }

        // Set the attributes on all instantiated validators. We don't distinguish here
        // which attribute should go to which validator.
        var validatorAttributes = getValidatorAttributes(requestParameter);

        for (var validator : validators) {
            setPropertiesWithCoercion(validator, validatorAttributes);
        }

        return validators;
    }

    private static Map<String, Object> getConverterAttributes(Param requestParameter) {
        return getEvaluatedAttributes(requestParameter.converterAttributes());
    }

    private static Map<String, Object> getValidatorAttributes(Param requestParameter) {
        return getEvaluatedAttributes(requestParameter.validatorAttributes());
    }

    private static Map<String, Object> getEvaluatedAttributes(Attribute[] attributes) {
        return stream(attributes).collect(toMap(Attribute::name, attribute -> evaluateExpressionGet(attribute.value())));
    }

    private static void addConverterMessage(FacesContext context, ParamValue paramValue, String submittedValue, ConverterException ce) {
        var converterMessage = getConverterMessage(paramValue.param);
        FacesMessage message;

        if (!isEmpty(converterMessage)) {
            message = createError(converterMessage, submittedValue, paramValue.label);
        }
        else {
            message = ce.getFacesMessage();

            if (message == null) {
                // If the converter didn't add a FacesMessage, set a generic one.
                message = createError("Conversion failed for {0} because: {1}", submittedValue, ce.getMessage());
            }
        }

        context.addMessage(paramValue.getClientId(context), message);
    }

    private static void addRequiredMessage(FacesContext context, ParamValue paramValue) {
        var requiredMessage = getRequiredMessage(paramValue.param);
        FacesMessage message = null;

        if (!isEmpty(requiredMessage)) {
            message = createError(requiredMessage, null, paramValue.label);
        }
        else {
            // (Ab)use RequiredValidator to get the same message that all required attributes are using.
            try {
                new RequiredValidator().validate(context, context.getViewRoot(), null);
            }
            catch (ValidatorException ve) {
                message = ve.getFacesMessage();
            }

            if (message == null) {
                // RequiredValidator didn't throw or its exception did not have a message set.
                var messageBundle = getMessageBundle(context);
                var defaultRequiredMessage = messageBundle != null ? messageBundle.getString(UIInput.REQUIRED_MESSAGE_ID) : null;
                message = createError(coalesce(defaultRequiredMessage, requiredMessage, DEFAULT_REQUIRED_MESSAGE), paramValue.label);
            }
        }

        context.addMessage(paramValue.getClientId(context), message);
    }

    private static void addValidatorMessages(FacesContext context, ParamValue paramValue, String submittedValue, ValidatorException ve) {
        var validatorMessage = getValidatorMessage(paramValue.param);
        var clientId = paramValue.getClientId(context);

        if (!isEmpty(validatorMessage)) {
            context.addMessage(clientId, createError(validatorMessage, submittedValue, paramValue.label));
        }
        else {
            for (var facesMessage : getFacesMessages(ve)) {
                context.addMessage(clientId, facesMessage);
            }
        }
    }

    private static List<FacesMessage> getFacesMessages(ValidatorException ve) {
        var facesMessages = new ArrayList<FacesMessage>();

        if (ve.getFacesMessages() != null) {
            facesMessages.addAll(ve.getFacesMessages());
        }
        else if (ve.getFacesMessage() != null) {
            facesMessages.add(ve.getFacesMessage());
        }

        return facesMessages;
    }

}