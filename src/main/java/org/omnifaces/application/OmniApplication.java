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
package org.omnifaces.application;

import static org.omnifaces.util.Beans.getReference;
import static org.omnifaces.util.Utils.getDefaultValue;

import javax.faces.application.Application;
import javax.faces.application.ApplicationWrapper;
import javax.faces.convert.Converter;
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
@SuppressWarnings("rawtypes")
public class OmniApplication extends ApplicationWrapper {

	// Variables ------------------------------------------------------------------------------------------------------

	private final ConverterManager converterManager;
	private final ValidatorManager validatorManager;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new OmniFaces application around the given wrapped application.
	 * @param wrapped The wrapped application.
	 */
	public OmniApplication(Application wrapped) {
		super(wrapped);
		converterManager = getReference(ConverterManager.class);
		validatorManager = getReference(ValidatorManager.class);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * If the there's a CDI managed {@link Converter} instance available, then return it, else delegate to
	 * {@link #getWrapped()} which may return the JSF managed {@link Converter} instance.
	 */
	@Override
	public Converter createConverter(String converterId) {
		return converterManager.createConverter(getWrapped(), converterId);
	}

	/**
	 * If the there's a CDI managed {@link Converter} instance available, then return it, else delegate to
	 * {@link #getWrapped()} which may return the JSF managed {@link Converter} instance.
	 */
	@Override
	public Converter createConverter(Class<?> forClass) {
		Class<?> converterForClass = forClass.isPrimitive() ? getDefaultValue(forClass).getClass() : forClass;
		return converterManager.createConverter(getWrapped(), converterForClass);
	}

	/**
	 * If the there's a CDI managed {@link Validator} instance available, then return it, else delegate to
	 * {@link #getWrapped()} which may return the JSF managed {@link Validator} instance.
	 */
	@Override
	public Validator createValidator(String validatorId) {
		return validatorManager.createValidator(getWrapped(), validatorId);
	}

}