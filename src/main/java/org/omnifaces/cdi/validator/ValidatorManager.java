/*
 * Copyright 2017 OmniFaces
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
package org.omnifaces.cdi.validator;

import static org.omnifaces.util.BeansLocal.getReference;
import static org.omnifaces.util.BeansLocal.resolve;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.Specializes;
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
import org.omnifaces.application.OmniApplicationFactory;

/**
 * <p>
 * The <code>@FacesValidator</code> is by default not eligible for dependency injection by <code>@Inject</code> nor <code>@EJB</code>.
 * There is a <a href="http://balusc.omnifaces.org/2011/09/communication-in-jsf-20.html#GettingAnEJBInFacesConverterAndFacesValidator">workaround</a>
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
 * <strong>without any further modification</strong>.
 * <p>
 * The {@link ValidatorManager} provides access to all {@link FacesValidator} annotated {@link Validator} instances which are made eligible for CDI.
 *
 * <h3>bean-discovery-mode</h3>
 * <p>
 * In Java EE 7's CDI 1.1, when having a CDI 1.1 compatible <code>beans.xml</code>, by default only classes with an
 * explicit CDI managed bean scope annotation will be registered for dependency injection support. In order to cover
 * {@link FacesValidator} annotated classes as well, you need to explicitly set <code>bean-discovery-mode="all"</code>
 * attribute in <code>beans.xml</code>. This was not necessary in Mojarra versions older than 2.2.9 due to an
 * <a href="http://stackoverflow.com/q/29458023/157882">oversight</a>.
 *
 * <h3>AmbiguousResolutionException</h3>
 * <p>
 * In case you have a {@link FacesValidator} annotated class extending another {@link FacesValidator} annotated class
 * which in turn extends a standard validator, then you may with <code>bean-discovery-mode="all"</code> face an
 * {@link AmbiguousResolutionException}. This can be solved by placing {@link Specializes} annotation on the subclass.
 *
 * @author Radu Creanga {@literal <rdcrng@gmail.com>}
 * @author Bauke Scholtz
 * @see OmniApplication
 * @see OmniApplicationFactory
 * @since 1.6
 */
@ApplicationScoped
@SuppressWarnings("rawtypes")
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