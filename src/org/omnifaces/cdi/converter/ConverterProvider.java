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
package org.omnifaces.cdi.converter;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.application.OmniApplication;
import org.omnifaces.util.Faces;

/**
 * Provides access to all {@link FacesConverter} annotated {@link Converter} instances which are made eligible for CDI.
 *
 * @author Radu Creanga <rdcrng@gmail.com>
 * @author Bauke Scholtz
 * @see OmniApplication
 * @since 1.6
 */
@Named(ConverterProvider.NAME)
@ApplicationScoped
public class ConverterProvider {

	// Constants ------------------------------------------------------------------------------------------------------

	static final String NAME = "omnifaces_ConverterProvider";
	private static final String EL_NAME = String.format("#{%s}", NAME);

	// Dependencies ---------------------------------------------------------------------------------------------------

	@Inject
	private ConverterExtension extension;

	@Inject
	private BeanManager manager;

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the CDI-managed converter instance associated with the given converter ID, or <code>null</code> if there
	 * is none.
	 * @param converterId
	 * @return the CDI-managed converter instance associated with the given converter ID, or <code>null</code> if there
	 * is none.
	 */
	public Converter getConverter(String converterId) {
		return getConverterReference(extension.getConvertersByID().get(converterId));
	}

	/**
	 * Returns the CDI-managed converter instance associated with the given converter for-class, or <code>null</code>
	 * if there is none.
	 * @param converterForClass
	 * @return the CDI-managed converter instance associated with the given converter for-class, or <code>null</code>
	 * if there is none.
	 */
	public Converter getConverter(Class<?> converterForClass) {
		return getConverterReference(extension.getConvertersByForClass().get(converterForClass));
	}

	private Converter getConverterReference(Bean<Converter> bean) {
		if (bean == null) {
			return null;
		}

		CreationalContext<Converter> context = manager.createCreationalContext(bean);
		return (Converter) manager.getReference(bean, Converter.class, context);
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the application scoped instance of this converter provider from the EL context, or <code>null</code> if
	 * CDI is not supported on the application.
	 * @return The application scoped instance of this converter provider from the EL context, or <code>null</code> if
	 * CDI is not supported on the application.
	 */
	public static ConverterProvider getInstance() {
		return Faces.evaluateExpressionGet(EL_NAME);
	}

}