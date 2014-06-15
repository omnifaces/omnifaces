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

import static org.omnifaces.util.Beans.getReference;
import static org.omnifaces.util.Beans.resolve;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.faces.application.Application;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.inject.Inject;

import org.omnifaces.application.OmniApplication;
import org.omnifaces.application.ValidatorProvider;

/**
 * Provides access to all {@link FacesValidator} annotated {@link Validator} instances which are made eligible for CDI.
 * <p>
 * Previously, in OmniFaces 1.6, all validators were proactively collected in an {@link Extension} instance. However,
 * this construct failed when OmniFaces was installed in multiple WARs of an EAR. The extension was EAR-wide, but each
 * WAR got its own instance injected and only one of them will have access to the collected validators. Since OmniFaces
 * 1.6.1, the whole extension is removed and the validators are now lazily collected in this manager class. See also
 * <a href="https://code.google.com/p/omnifaces/issues/detail?id=251">issue 251</a>.
 *
 * @author Radu Creanga <rdcrng@gmail.com>
 * @author Bauke Scholtz
 * @see OmniApplication
 * @since 1.6
 */
@ApplicationScoped
public class ValidatorManager implements ValidatorProvider {

	// Dependencies ---------------------------------------------------------------------------------------------------

	@Inject
	private BeanManager manager;
	private Map<String, Bean<Validator>> validatorsById = new HashMap<String, Bean<Validator>>();

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	@SuppressWarnings("unchecked")
	public Validator createValidator(Application application, String validatorId) {
		Bean<Validator> bean = validatorsById.get(validatorId);

		if (bean == null && !validatorsById.containsKey(validatorId)) {
			Validator validator = application.createValidator(validatorId);

			if (validator != null) {
				bean = (Bean<Validator>) resolve(manager, validator.getClass());
			}

			validatorsById.put(validatorId, bean);
		}

		return (bean != null) ? getReference(manager, bean) : null;
	}

}