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
import javax.faces.application.Application;
import javax.faces.application.NavigationHandler;
import javax.faces.application.ResourceHandler;
import javax.faces.convert.Converter;
import javax.faces.event.ActionListener;
import javax.faces.event.PhaseListener;
import javax.faces.event.SystemEventListener;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.inject.Inject;

import org.omnifaces.application.OmniApplication;

/**
 * <p>
 * The <code>@FacesValidator</code> is by default not eligible for dependency injection by <code>@Inject</code> nor <code>@EJB</code>.
 * There is a <a href="http://balusc.blogspot.com/2011/09/communication-in-jsf-20.html#GettingAnEJBInFacesConverterAndFacesValidator">workaround</a>
 * for EJB, but this is nasty and doesn't work out for CDI. <a href="http://stackoverflow.com/q/7572335/157882">Another way</a>
 * would be to make it a JSF or CDI managed bean, however this doesn't register the validator instance into the JSF application context,
 * and hence you won't be able to make use of {@link Application#createValidator(String)} on it.
 * <p>
 * Initially, this should be solved in JSF 2.2 which comes with new support for dependency injection in among others all
 * <code>javax.faces.*.*Factory</code>, {@link NavigationHandler}, {@link ResourceHandler},
 * {@link ActionListener}, {@link PhaseListener} and {@link SystemEventListener} instances.
 * The {@link Converter} and {@link Validator} were initially also among them, but they broke a TCK test and were at the
 * last moment removed from dependency injection support.
 * <p>
 * The support is expected to come back in JSF 2.3, but we just can't wait any longer.
 * <a href="http://myfaces.apache.org/extensions/cdi/">MyFaces CODI</a> has support for it,
 * but it requires an additional <code>@Advanced</code> annotation.
 * OmniFaces solves this by implicitly making all {@link FacesValidator} instances eligible for dependency injection
 * <strong>without any further modification</strong>, like as JSF 2.2 would initially do, hereby providing a transparent
 * bridge of the code to the upcoming JSF 2.3.
 * <p>
 * The {@link ValidatorManager} provides access to all {@link FacesValidator} annotated {@link Validator} instances which are made eligible for CDI.
 *
 * @author Radu Creanga {@literal <rdcrng@gmail.com>}
 * @author Bauke Scholtz
 * @see OmniApplication
 * @since 1.6
 */
@ApplicationScoped
public class ValidatorManager {

	// Dependencies ---------------------------------------------------------------------------------------------------

	@Inject
	private BeanManager manager;
	private Map<String, Bean<Validator>> validatorsById = new HashMap<>();

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the validator instance associated with the given validator ID,
	 * or <code>null</code> if there is none.
	 * @param application The involved JSF application.
	 * @param validatorId The validator ID of the desired validator instance.
	 * @return the validator instance associated with the given validator ID,
	 * or <code>null</code> if there is none.
	 */
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