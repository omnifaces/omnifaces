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

import static org.omnifaces.util.Beans.getReference;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.faces.application.Application;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;

import org.omnifaces.application.ConverterProvider;
import org.omnifaces.application.OmniApplication;
import org.omnifaces.util.Beans;

/**
 * Provides access to all {@link FacesConverter} annotated {@link Converter} instances which are made eligible for CDI.
 *
 * @author Radu Creanga <rdcrng@gmail.com>
 * @author Bauke Scholtz
 * @see OmniApplication
 * @since 1.6
 */
@ApplicationScoped
public class ConverterManager implements ConverterProvider {

	// Dependencies ---------------------------------------------------------------------------------------------------

	@Inject
	private BeanManager manager;
	private Map<String, Bean<Converter>> convertersById = new HashMap<String, Bean<Converter>>();
    private Map<Class<?>, Bean<Converter>> convertersByForClass = new HashMap<Class<?>, Bean<Converter>>();

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	@SuppressWarnings("unchecked")
	public Converter createConverter(Application application, String converterId) {
		Bean<Converter> bean = convertersById.get(converterId);

		if (bean == null && !convertersById.containsKey(converterId)) {
			Converter converter = application.createConverter(converterId);

			if (converter != null) {
				bean = (Bean<Converter>) Beans.resolve(manager, converter.getClass());
			}

			convertersById.put(converterId, bean);
		}

		return (bean != null) ? getReference(manager, bean) : null;
	}

	@Override
	public Converter createConverter(Application application, Class<?> converterForClass) {
		Bean<Converter> bean = convertersByForClass.get(converterForClass);

		if (bean == null && !convertersByForClass.containsKey(converterForClass)) {
			Converter converter = application.createConverter(converterForClass);

			if (converter != null) {
				bean = (Bean<Converter>) Beans.resolve(manager, converter.getClass());
			}

			convertersByForClass.put(converterForClass, bean);
		}

		return (bean != null) ? getReference(manager, bean) : null;
	}

}