/*
 * Copyright 2016 OmniFaces
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
package org.omnifaces.application;

import static java.lang.Boolean.parseBoolean;
import static javax.faces.convert.Converter.DATETIMECONVERTER_DEFAULT_TIMEZONE_IS_SYSTEM_TIMEZONE_PARAM_NAME;
import static org.omnifaces.util.Beans.getReference;
import static org.omnifaces.util.Faces.getInitParameter;

import java.util.TimeZone;

import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.ApplicationWrapper;
import javax.faces.convert.Converter;
import javax.faces.convert.DateTimeConverter;
import javax.faces.validator.Validator;

import org.omnifaces.cdi.converter.ConverterManager;
import org.omnifaces.cdi.validator.ValidatorManager;

/**
 * <p>
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
 * @author Radu Creanga {@literal <rdcrng@gmail.com>}
 * @author Bauke Scholtz
 * @see ConverterManager
 * @see ValidatorManager
 * @since 1.6
 */
public class OmniApplication extends ApplicationWrapper {

	// Variables ------------------------------------------------------------------------------------------------------

	private final Application wrapped;
	private final ConverterManager converterManager;
	private final ValidatorManager validatorManager;
	private final TimeZone dateTimeConverterDefaultTimeZone;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new OmniFaces application around the given wrapped application.
	 * @param wrapped The wrapped application.
	 */
	public OmniApplication(Application wrapped) {
		this.wrapped = wrapped;
		converterManager = getReference(ConverterManager.class);
		validatorManager = getReference(ValidatorManager.class);
		dateTimeConverterDefaultTimeZone =
			parseBoolean(getInitParameter(DATETIMECONVERTER_DEFAULT_TIMEZONE_IS_SYSTEM_TIMEZONE_PARAM_NAME))
				? TimeZone.getDefault()
				: null;
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * If the there's a CDI managed {@link Converter} instance available, then return it, else delegate to
	 * {@link #getWrapped()} which may return the JSF managed {@link Converter} instance.
	 */
	@Override
	public Converter createConverter(String converterId) {
		Converter converter = converterManager.createConverter(getWrapped(), converterId);

		if (converter != null) {
			setDefaultPropertiesIfNecessary(converter);
			return converter;
		}

		return super.createConverter(converterId);
	}

	/**
	 * If the there's a CDI managed {@link Converter} instance available, then return it, else delegate to
	 * {@link #getWrapped()} which may return the JSF managed {@link Converter} instance.
	 */
	@Override
	public Converter createConverter(Class<?> forClass) {
		Converter converter = converterManager.createConverter(getWrapped(), forClass);

		if (converter != null) {
			setDefaultPropertiesIfNecessary(converter);
			return converter;
		}

		return super.createConverter(forClass);
	}

	/**
	 * If the there's a CDI managed {@link Validator} instance available, then return it, else delegate to
	 * {@link #getWrapped()} which may return the JSF managed {@link Validator} instance.
	 */
	@Override
	public Validator createValidator(String validatorId) throws FacesException {
		Validator validator = validatorManager.createValidator(getWrapped(), validatorId);

		if (validator != null) {
			return validator;
		}

		return super.createValidator(validatorId);
	}

	@Override
	public Application getWrapped() {
		return wrapped;
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	private void setDefaultPropertiesIfNecessary(Converter converter) {
		if (converter instanceof DateTimeConverter && dateTimeConverterDefaultTimeZone != null) {
			((DateTimeConverter) converter).setTimeZone(dateTimeConverterDefaultTimeZone);
		}
	}

}