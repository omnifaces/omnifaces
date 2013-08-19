/*
 * Copyright 2013 OmniFaces.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnifaces.application;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.ApplicationWrapper;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.validator.Validator;


/**
 * This OmniFaces application extends the standard JSF application as follows:
 * <ul>
 * <li>Support for CDI in {@link Converter}s and {@link Validator}s, so that e.g. <code>@Inject</code> and
 * <code>@EJB</code> work directly in JSF converters and validators without any further modification.</li>
 * </ul>
 * <p>
 * This application is already registered by OmniFaces' own <code>faces-config.xml</code> and thus gets
 * auto-initialized when the OmniFaces JAR is bundled in a web application, so end-users do not need to register this
 * application explicitly themselves.
 *
 * @author Radu Creanga <rdcrng@gmail.com>
 * @author Bauke Scholtz
 * @see ConverterProvider
 * @see ValidatorProvider
 * @since 1.6
 */
public class OmniApplication extends ApplicationWrapper {

	// Variables ------------------------------------------------------------------------------------------------------

	private final Application wrapped;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new OmniFaces application around the given wrapped application.
	 * @param wrapped The wrapped application.
	 */
	public OmniApplication(Application wrapped) {
		this.wrapped = wrapped;
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * If the {@link ConverterProvider} is present and there's a CDI managed {@link Converter} instance available, then
	 * return it, else delegate to {@link #getWrapped()} which may return the JSF managed {@link Converter} instance.
	 */
	@Override
	public Converter createConverter(String converterId) {
		ConverterProvider converterProvider = ConverterProvider.getInstance();

		if (converterProvider != null) {
			Converter converter = converterProvider.createConverter(converterId);

			if (converter != null) {
				return converter;
			}
		}

		return getWrapped().createConverter(converterId);
	}

	/**
	 * If the {@link ConverterProvider} is present and there's a CDI managed {@link Converter} instance available, then
	 * return it, else delegate to {@link #getWrapped()} which may return the JSF managed {@link Converter} instance.
	 */
	@Override
	public Converter createConverter(Class<?> targetClass) {
		ConverterProvider converterProvider = ConverterProvider.getInstance();

		if (converterProvider != null) {
			Converter converter = converterProvider.createConverter(targetClass);

			if (converter != null) {
				return converter;
			}
		}

		return getWrapped().createConverter(targetClass);
	}

	/**
	 * If the {@link ValidatorProvider} is present and there's a CDI managed {@link Validator} instance available, then
	 * return it, else delegate to {@link #getWrapped()} which may return the JSF managed {@link Validator} instance.
	 */
	@Override
	public Validator createValidator(String validatorId) throws FacesException {
		ValidatorProvider validatorProvider = ValidatorProvider.getInstance();

		if (validatorProvider != null) {
			Validator validator = validatorProvider.createValidator(validatorId);

			if (validator != null) {
				return validator;
			}
		}

		return getWrapped().createValidator(validatorId);
	}

	@Override
	public Application getWrapped() {
		return wrapped;
	}

	/**
	 * The same as {@link Application#evaluateExpressionGet(javax.faces.context.FacesContext, String, Class)}, but
	 * then <code>null</code>-safe. I.e. it doesn't throw NPE when {@link FacesContext}, or {@link ELContext}, or
	 * {@link ExpressionFactory} are not available. This is sometimes mandatory during early initialization stages
	 * when JSF or EL contexts are not available for some reason.
	 */
	@SuppressWarnings("unchecked")
	static <T> T safeEvaluateExpressionGet(String expression) {
		FacesContext facesContext = FacesContext.getCurrentInstance();

		if (facesContext == null) {
			return null;
		}

		ELContext elContext = facesContext.getELContext();
		ExpressionFactory elFactory = facesContext.getApplication().getExpressionFactory();

		if (elContext == null || elFactory == null) {
			return null;
		}

		return (T) elFactory.createValueExpression(elContext, expression, Object.class).getValue(elContext);
	}

}