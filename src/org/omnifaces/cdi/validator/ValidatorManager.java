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
package org.omnifaces.cdi.validator;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.application.OmniApplication;
import org.omnifaces.application.ValidatorProvider;

/**
 * Provides access to all {@link FacesValidator} annotated {@link Validator} instances which are made eligible for CDI.
 *
 * @author Radu Creanga <rdcrng@gmail.com>
 * @author Bauke Scholtz
 * @see OmniApplication
 * @since 1.6
 */
@Named(ValidatorProvider.NAME)
@ApplicationScoped
public class ValidatorManager extends ValidatorProvider {

	// Dependencies ---------------------------------------------------------------------------------------------------

	@Inject
	private ValidatorExtension extension;

	@Inject
	private BeanManager manager;

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public Validator createValidator(String validatorId) {
		Bean<Validator> bean = extension.getValidatorsById().get(validatorId);

		if (bean == null) {
			return null;
		}

		CreationalContext<Validator> context = manager.createCreationalContext(bean);
		return (Validator) manager.getReference(bean, Validator.class, context);
	}

}