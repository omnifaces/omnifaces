/*
 * Copyright 2013 OmniFaces.
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
package org.omnifaces.cdi.requestparam;

import static java.beans.Introspector.getBeanInfo;
import static java.beans.PropertyEditorManager.findEditor;
import static org.omnifaces.util.Faces.evaluateExpressionGet;
import static org.omnifaces.util.Faces.getApplication;
import static org.omnifaces.util.Faces.getContext;
import static org.omnifaces.util.Faces.getRequestParameter;
import static org.omnifaces.util.Faces.getViewRoot;
import static org.omnifaces.util.Messages.createError;
import static org.omnifaces.util.Utils.isEmpty;

import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import org.omnifaces.util.Faces;

/**
 * Producer for a request parameter as defined by the {@link Param} annotation.
 *
 * @since 1.6
 * @author Arjan Tijms
 */
public class RequestParameterProducer {

	@SuppressWarnings("unchecked")
	@Produces
	@Param
	public <V> ParamValue<V> produce(InjectionPoint injectionPoint) {

		// @Param is the annotation on the injection point that holds all data for this request parameter
		Param requestParameter = getQualifier(injectionPoint, Param.class);

		FacesContext context = getContext();
		UIComponent component = getViewRoot();
		
		// TODO: Save/restore existing potentially existing label?
		component.getAttributes().put("label", getLabel(requestParameter, injectionPoint));

		// Get raw submitted value from the request
		String submittedValue = getRequestParameter(getName(requestParameter, injectionPoint));
		Object convertedValue = null;
		boolean valid = true;

		try {
			
			// Convert the submitted value
			
			Converter converter = getConverter(requestParameter, getTargetType(injectionPoint));
			if (converter != null) {
				convertedValue = converter.getAsObject(context, component, submittedValue);
			} else {
				convertedValue = submittedValue;
			}

			// Validate the converted value
			
			for (Validator validator : getValidators(requestParameter)) {
				try {
					validator.validate(context, component, convertedValue);
				} catch (ValidatorException ve) {
					valid = false;
					addValidatorMessages(context, component, submittedValue, ve, requestParameter.validatorMessage());
				}
			}
		} catch (ConverterException ce) {
			valid = false;
			addConverterMessage(context, component, submittedValue, ce, requestParameter.converterMessage());
		}

		if (!valid) {
			context.validationFailed();
			convertedValue = null;
		}

		return (ParamValue<V>) new ParamValue<Object>(convertedValue);
	}

	@SuppressWarnings("unchecked")
	private <V> Class<V> getTargetType(InjectionPoint injectionPoint) {
		Type type = injectionPoint.getType();
		if (type instanceof ParameterizedType) {
			return (Class<V>) ((ParameterizedType) type).getActualTypeArguments()[0];
		}

		return null;
	}

	private String getName(Param requestParameter, InjectionPoint injectionPoint) {

		String name = requestParameter.name();

		if (isEmpty(name)) {
			name = injectionPoint.getMember().getName();
		}

		return name;
	}
	
	private String getLabel(Param requestParameter, InjectionPoint injectionPoint) {
		
		String label = requestParameter.label();
		
		if (isEmpty(label)) {
			label = getName(requestParameter, injectionPoint);
		}
		
		return label;
	}
	
	
	private Converter getConverter(Param requestParameter, Class<?> targetType) {

		Class<? extends Converter> converterClass = requestParameter.converterClass();
		String converterName = requestParameter.converter();
		
		Converter converter = null;

		if (!isEmpty(converterName)) {
			Object expressionResult = evaluateExpressionGet(converterName);
			if (expressionResult instanceof Converter) {
				converter = (Converter) expressionResult;
			} else if (expressionResult instanceof String) {
				converter = getApplication().createConverter((String) expressionResult);
			}
		} else if (!converterClass.equals(Converter.class)) { // Converter.cass is default, representing null
			converter = instance(converterClass);
		}
		
		if (converter == null) {
			try {
				converter = Faces.getApplication().createConverter(targetType);
			} catch (Exception e) {
				return null;
			}
		}
		
		if (converter != null) {
			setAttributes(converter, getConverterAttributes(requestParameter));
		}
		
		return converter;
	}

	private List<Validator> getValidators(Param requestParameter) {

		List<Validator> validators = new ArrayList<Validator>();

		Class<? extends Validator>[] validatorClasses = requestParameter.validatorClasses();
		String[] validatorNames = requestParameter.validators();

		for (String validatorName : validatorNames) {
			Object validator = evaluateExpressionGet(validatorName);
			if (validator instanceof Validator) {
				validators.add((Validator) validator);
			} else if (validator instanceof String) {
				validators.add(getApplication().createValidator(validatorName));
			}
		}

		for (Class<? extends Validator> validatorClass : validatorClasses) {
			validators.add(instance(validatorClass));
		}
		
		// Set the attributes on all instantiated validators. We don't distinguish here
		// which attribute should go to which validator.
		Map<String, Object> validatorAttributes = getValidatorAttributes(requestParameter);
		for (Validator validator : validators) {
			setAttributes(validator, validatorAttributes);
		}

		return validators;
	}

	private Map<String, Object> getConverterAttributes(Param requestParameter) {

		Map<String, Object> attributeMap = new HashMap<String, Object>();

		Attribute[] attributes = requestParameter.converterAttributes();
		for (Attribute attribute : attributes) {
			attributeMap.put(attribute.name(), evaluateExpressionGet(attribute.value()));
		}

		return attributeMap;
	}

	private Map<String, Object> getValidatorAttributes(Param requestParameter) {

		Map<String, Object> attributeMap = new HashMap<String, Object>();

		Attribute[] attributes = requestParameter.validatorAttributes();
		for (Attribute attribute : attributes) {
			attributeMap.put(attribute.name(), evaluateExpressionGet(attribute.value()));
		}

		return attributeMap;
	}

	private void setAttributes(Object object, Map<String, Object> attributes) {
		try {
			for (PropertyDescriptor property : getBeanInfo(object.getClass()).getPropertyDescriptors()) {
				Method setter = property.getWriteMethod();

				if (setter == null) {
					continue;
				}

				if (attributes.containsKey(property.getName())) {

					Object value = attributes.get(property.getName());
					if (value instanceof String && !property.getPropertyType().equals(String.class)) {

						// Try to convert Strings to the type expected by the converter

						PropertyEditor editor = findEditor(property.getPropertyType());
						editor.setAsText((String) value);
						value = editor.getValue();
					}

					property.getWriteMethod().invoke(object, value);
				}

			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	private void addConverterMessage(FacesContext context, UIComponent component, String submittedValue, ConverterException ce,	String converterMessage) {
		FacesMessage message = null;

		if (!isEmpty(converterMessage)) {
			message = createError(converterMessage, submittedValue);
		} else {
			message = ce.getFacesMessage();
			if (message == null) {
				// If the converter didn't add a FacesMessage, set a generic one.
				message = createError("Conversion failed for {0} because: {1}", submittedValue, ce.getMessage());
			}
		}

		context.addMessage(component.getClientId(context), message);
	}
	
	private void addValidatorMessages(FacesContext context, UIComponent component, String submittedValue, ValidatorException ve, String validatorMessage) {
		
		String clientId = component.getClientId(context);
		
		if (!isEmpty(validatorMessage)) {
			context.addMessage(clientId, createError(validatorMessage, submittedValue));
		} else {
			for (FacesMessage facesMessage : getFacesMessages(ve)) {
				context.addMessage(clientId, facesMessage);
			}
		}
	}

	private static <T> T instance(Class<T> clazz) {
		try {
			return clazz.newInstance();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static List<FacesMessage> getFacesMessages(ValidatorException ve) {
		List<FacesMessage> facesMessages = new ArrayList<FacesMessage>();
		if (ve.getFacesMessages() != null) {
			facesMessages.addAll(ve.getFacesMessages());
		} else if (ve.getFacesMessage() != null) {
			facesMessages.add(ve.getFacesMessage());
		}

		return facesMessages;
	}

	@SuppressWarnings("unchecked")
	private static <T> T getQualifier(InjectionPoint injectionPoint, Class<T> annotationClass) {
		for (Annotation annotation : injectionPoint.getQualifiers()) {
			if (annotationClass.isAssignableFrom(annotation.getClass())) {
				return (T) annotation;
			}
		}

		return null;
	}

}
