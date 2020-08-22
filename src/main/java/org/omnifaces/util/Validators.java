/*
 * Copyright 2020 OmniFaces
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
package org.omnifaces.util;

import static jakarta.faces.validator.BeanValidator.VALIDATOR_FACTORY_KEY;
import static java.lang.String.format;
import static java.util.logging.Level.WARNING;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.omnifaces.util.Faces.getApplicationAttribute;
import static org.omnifaces.util.Faces.getLocale;
import static org.omnifaces.util.Reflection.getBeanProperty;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ElementKind;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.Path.Node;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 *
 * @author Bauke Scholtz
 * @author Andre Wachsmuth
 * @since 3.8
 */
public final class Validators {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final Logger logger = Logger.getLogger(Validators.class.getName());

	public static final String BEAN_VALIDATION_AVAILABLE = "org.omnifaces.BEAN_VALIDATION_AVAILABLE";
	private static final String ERROR_RESOLVE_BASE = "Cannot resolve base from property '%s' of bean '%s'.";
	private static final String ERROR_ACCESS_KEY = "Cannot access '%s' of '%s' by key '%s'.";
	private static final String ERROR_ACCESS_INDEX = "Cannot access '%s' of '%s' by index '%s'.";


	// Constructors ---------------------------------------------------------------------------------------------------

	private Validators() {
		// Hide constructor.
	}


	// Utils ----------------------------------------------------------------------------------------------------------

	/**
	 * Returns <code>true</code> if Bean Validation is available. This is remembered in the application scope.
	 * @return <code>true</code> if Bean Validation is available.
	 */
	public static boolean isBeanValidationAvailable() {
		return getApplicationAttribute(BEAN_VALIDATION_AVAILABLE, () -> {
			try {
				Class.forName("jakarta.validation.Validation");
				getBeanValidator();
				return true;
			}
			catch (Exception | LinkageError e) {
				logger.log(WARNING, "Bean validation not available.", e);
				return false;
			}
		});
	}

	/**
	 * Returns the default bean validator factory. This is remembered in the application scope.
	 * @return The default bean validator factory.
	 */
	public static ValidatorFactory getBeanValidatorFactory() {
		return getApplicationAttribute(VALIDATOR_FACTORY_KEY, Validation::buildDefaultValidatorFactory);
	}

	/**
	 * Returns the bean validator which is aware of the JSF locale.
	 * @return The bean validator which is aware of the JSF locale.
	 * @see Faces#getLocale()
	 */
	public static Validator getBeanValidator() {
		ValidatorFactory validatorFactory = getBeanValidatorFactory();
        return validatorFactory.usingContext()
        	.messageInterpolator(new FacesLocaleAwareMessageInterpolator(validatorFactory.getMessageInterpolator()))
        	.getValidator();
	}

	/**
	 * Validate given bean on given group classes.
	 * @param bean Bean to be validated.
	 * @param groups Bean validation groups, if any.
	 * @return Constraint violations.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Set<ConstraintViolation<?>> validateBean(Object bean, Class<?>... groups) {
		Set violationsRaw = getBeanValidator().validate(bean, groups);
		Set<ConstraintViolation<?>> violations = violationsRaw;
		return violations;
	}

	/**
	 * Validate given value as if it were a property of the given bean type.
	 * @param beanType Type of target bean.
	 * @param propertyName Name of property on target bean.
	 * @param value Value to be validated.
	 * @param groups Bean validation groups, if any.
	 * @return Constraint violations.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Set<ConstraintViolation<?>> validateBeanProperty(Class<?> beanType, String propertyName, Object value, Class<?>... groups) {
		Set violationsRaw = getBeanValidator().validateValue(beanType, propertyName, value, groups);
		Set<ConstraintViolation<?>> violations = violationsRaw;
		return violations;
	}

	/**
	 * Resolve violated base from given bean based on given violation.
	 * @param bean Bean to resolve violated base on.
	 * @param violation Constraint violation to resolve violated base from.
	 * @return Violated base from given bean based on given violation.
	 */
	public static Object resolveViolatedBase(Object bean, ConstraintViolation<?> violation) {
		try {
			Object base = bean;

			for (Iterator<Node> iterator = violation.getPropertyPath().iterator(); iterator.hasNext();) {
				if (base == null) {
					return null;
				}

				Node node = iterator.next();
				ElementKind kind = node.getKind();

				switch (kind) {
					case BEAN:
					case PROPERTY:
					case CONTAINER_ELEMENT: // List, Map, Array, etc
						if (iterator.hasNext() || kind != ElementKind.PROPERTY) { // PROPERTY may not be the last one.
							base = resolveProperty(base, node);
						}
						break;

					default:
						// Rest is applicable to property validations.
						throw new UnsupportedOperationException(node + " kind " + kind + " is not supported.");
				}
			}

			return base;
		}
		catch (Exception e) {
			String propertyPath = getPropertyNodes(violation).stream().map(Node::toString).collect(joining("."));
			logger.log(WARNING, format(ERROR_RESOLVE_BASE, propertyPath, bean == null ? "null" : bean.getClass()), e);
			return violation.getInvalidValue(); // Fall back.
		}
	}

	/**
	 * Returns a list of property path nodes from the given constraint violation.
	 * @param violation The constraint violation to return a list of property path nodes from.
	 * @return A list of property path nodes from the given constraint violation.
	 */
	public static List<Node> getPropertyNodes(ConstraintViolation<?> violation) {
		return StreamSupport.stream(violation.getPropertyPath().spliterator(), false).collect(toList());
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	private static Object resolveProperty(Object base, Node node) {
		if (node.getIndex() != null) {
			return accessIndex(base, node);
		}
		if (node.getKey() != null) {
			return accessKey(base, node);
		}
		else if (node.getName() != null) {
			return getBeanProperty(base, node.getName());
		}
		else {
			throw new UnsupportedOperationException(format(ERROR_RESOLVE_BASE, node, base.getClass()));
		}
	}

	private static Object accessIndex(Object base, Node node) {
		int index = node.getIndex().intValue();

		if (base instanceof List<?>) {
			return ((List<?>) base).get(index);
		}

		if (base.getClass().isArray()) {
			return Array.get(base, index);
		}

		throw new UnsupportedOperationException(format(ERROR_ACCESS_INDEX, node, base.getClass(), index));
	}

	private static Object accessKey(Object base, Node node) {
		Object key = node.getKey();

		if (base instanceof Map<?, ?>) {
			return ((Map<?, ?>) base).get(key);
		}

		throw new UnsupportedOperationException(format(ERROR_ACCESS_KEY, node, base.getClass(), key));
	}

    private static class FacesLocaleAwareMessageInterpolator implements MessageInterpolator {

        private MessageInterpolator wrapped;

        public FacesLocaleAwareMessageInterpolator(MessageInterpolator wrapped) {
            this.wrapped = wrapped;
        }

        @Override
		public String interpolate(String message, MessageInterpolator.Context context) {
            return wrapped.interpolate(message, context, getLocale());
        }

        @Override
		public String interpolate(String message, MessageInterpolator.Context context, Locale locale) {
            return wrapped.interpolate(message, context, locale);
        }
    }

}
