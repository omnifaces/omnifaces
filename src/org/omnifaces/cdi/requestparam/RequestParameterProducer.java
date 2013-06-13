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

		Param requestParameter = getQualifier(injectionPoint, Param.class);

		Class<V> targetType = getTargetType(injectionPoint);
		String name = getName(requestParameter, injectionPoint);
		Converter converter = getConverter(requestParameter);
		List<Validator> validators = getValidators(requestParameter);

		FacesContext context = getContext();
		UIComponent component = getViewRoot();

		String value = getRequestParameter(name);

		Object convertedValue = null;

		boolean valid = true;

		try {
			if (converter != null) {
				setAttributes(converter, getConverterAttributes(requestParameter));
				convertedValue = converter.getAsObject(context, component, value);
			} else if (targetType.equals(String.class)) {
				convertedValue = value;
			}

			Map<String, Object> validatorAttributes = getValidatorAttributes(requestParameter);
			for (Validator validator : validators) {
				try {
					setAttributes(validator, validatorAttributes);
					validator.validate(context, component, convertedValue);
				} catch (ValidatorException ve) {
					valid = false;
					String clientId = component.getClientId(context);
					for (FacesMessage facesMessage : getFacesMessages(ve)) {
						context.addMessage(clientId, facesMessage);
					}
				}
			}
		} catch (ConverterException ce) {
			valid = false;
			FacesMessage message = ce.getFacesMessage();
			if (message == null) {
				// If the converter didn't add a FacesMessage, set a generic one.
				message = createError("Conversion failed for {0} because: {1}", value, ce.getMessage());
			}

			context.addMessage(component.getClientId(context), message);
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

	private Converter getConverter(Param requestParameter) {

		Class<? extends Converter> converterClass = requestParameter.converterClass();
		String converterName = requestParameter.converter();

		if (!isEmpty(converterName)) {
			Object converter = evaluateExpressionGet(converterName);
			if (converter instanceof Converter) {
				return (Converter) converter;
			} else if (converter instanceof String) {
				return getApplication().createConverter((String) converter);
			}
		}

		if (!converterClass.equals(Converter.class)) {
			return instance(converterClass);
		}

		return null;
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
