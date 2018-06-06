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
package org.omnifaces.util;

import static java.util.logging.Level.WARNING;
import static javax.faces.validator.BeanValidator.VALIDATOR_FACTORY_KEY;
import static javax.validation.Validation.buildDefaultValidatorFactory;
import static org.omnifaces.util.Faces.getApplicationAttribute;
import static org.omnifaces.util.Faces.getLocale;
import static org.omnifaces.util.Faces.setApplicationAttribute;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.faces.webapp.FacesServlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.validation.ConstraintViolation;
import javax.validation.MessageInterpolator;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

/**
 * This class provides access to (Java EE 6) platform services from the view point of JSF.
 * <p>
 * Note that this utility class can only be used in a JSF environment and is thus not
 * a Java EE general way to obtain platform services.
 *
 * @since 1.6
 * @author Arjan Tijms
 */
public final class Platform {

	// Constants ------------------------------------------------------------------------------------------------------

	public static final String BEAN_VALIDATION_AVAILABLE = "org.omnifaces.BEAN_VALIDATION_AVAILABLE";

	private static final Logger logger = Logger.getLogger(Platform.class.getName());

	// Constructors ---------------------------------------------------------------------------------------------------

	private Platform() {
		// Hide constructor.
	}


	// Bean Validation ------------------------------------------------------------------------------------------------

	/**
	 * Returns <code>true</code> if Bean Validation is available. This is remembered in the application scope.
	 * @return <code>true</code> if Bean Validation is available.
	 */
	public static boolean isBeanValidationAvailable() {
		Boolean beanValidationAvailable = getApplicationAttribute(BEAN_VALIDATION_AVAILABLE);

		if (beanValidationAvailable == null) {
			try {
				Class.forName("javax.validation.Validation");
				getBeanValidator();
				beanValidationAvailable = true;
			}
			catch (Exception | LinkageError e) {
				beanValidationAvailable = false;
				logger.log(WARNING, "Bean validation not available.", e);
			}

			setApplicationAttribute(BEAN_VALIDATION_AVAILABLE, beanValidationAvailable);
		}

		return beanValidationAvailable;
	}

	/**
	 * Returns the default bean validator factory. This is remembered in the application scope.
	 * @return The default bean validator factory.
	 */
	public static ValidatorFactory getBeanValidatorFactory() {

		ValidatorFactory validatorFactory = getApplicationAttribute(VALIDATOR_FACTORY_KEY);

		if (validatorFactory == null) {
			validatorFactory = buildDefaultValidatorFactory();
			setApplicationAttribute(VALIDATOR_FACTORY_KEY, validatorFactory);
		}

		return validatorFactory;
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
	 * Validate given bean on given group classes
	 * and return constraint violation messages mapped by property path.
	 * @param bean Bean to be validated.
	 * @param groups Bean validation groups, if any.
	 * @return Constraint violation messages mapped by property path.
	 * @since 2.7
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map<String, String> validateBean(Object bean, Class<?>... groups) {
		Set violationsRaw = getBeanValidator().validate(bean, groups);
		Set<ConstraintViolation<?>> violations = violationsRaw;
		return mapViolationMessagesByPropertyPath(violations);
	}

	/**
	 * Validate given value as if it were a property of the given bean type
	 * and return constraint violation messages mapped by property path.
	 * @param beanType Type of target bean.
	 * @param propertyName Name of property on target bean.
	 * @param value Value to be validated.
	 * @param groups Bean validation groups, if any.
	 * @return Constraint violation messages mapped by property path.
	 * @since 2.7
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map<String, String> validateBeanProperty(Class<?> beanType, String propertyName, Object value, Class<?>... groups) {
		Set violationsRaw = getBeanValidator().validateValue(beanType, propertyName, value, groups);
		Set<ConstraintViolation<?>> violations = violationsRaw;
		return mapViolationMessagesByPropertyPath(violations);
	}

	private static Map<String, String> mapViolationMessagesByPropertyPath(Collection<ConstraintViolation<?>> violations) {
		Map<String, String> violationMessagesByPropertyPath = new LinkedHashMap<>();
		for (ConstraintViolation<?> violation : violations) {
			violationMessagesByPropertyPath.put(violation.getPropertyPath().toString(), violation.getMessage());
		}
		return violationMessagesByPropertyPath;
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


	// FacesServlet ---------------------------------------------------------------------------------------------------

	/**
	 * Returns the {@link ServletRegistration} associated with the {@link FacesServlet}.
	 * @param servletContext The context to get the ServletRegistration from.
	 * @return ServletRegistration for FacesServlet, or <code>null</code> if the FacesServlet is not installed.
	 * @since 1.8
	 */
	public static ServletRegistration getFacesServletRegistration(ServletContext servletContext) {
		ServletRegistration facesServletRegistration = null;

		for (ServletRegistration registration : servletContext.getServletRegistrations().values()) {
			if (registration.getClassName().equals(FacesServlet.class.getName())) {
				facesServletRegistration = registration;
				break;
			}
		}

		return facesServletRegistration;
	}

	/**
	 * Returns the mappings associated with the {@link FacesServlet}.
	 * @param servletContext The context to get the {@link FacesServlet} from.
	 * @return The mappings associated with the {@link FacesServlet}, or an empty set.
	 * @since 2.5
	 */
	public static Collection<String> getFacesServletMappings(ServletContext servletContext) {
		ServletRegistration facesServlet = getFacesServletRegistration(servletContext);
		return (facesServlet != null) ? facesServlet.getMappings() : Collections.<String>emptySet();
	}

}