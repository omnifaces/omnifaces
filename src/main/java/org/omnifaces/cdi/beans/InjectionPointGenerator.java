/*
 * Copyright 2016 OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnifaces.cdi.beans;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import org.omnifaces.util.Beans;

/**
 * Dummy class used to take the injection point for "InjectionPoint" of, for usage in the implementation of
 * {@link Beans#getCurrentInjectionPoint(javax.enterprise.context.spi.CreationalContext)}.
 * <p>
 * The actual injectionPoint being injected is not used.
 *
 * @author Arjan Tijms
 * @since 2.0
 */
@Dependent
public class InjectionPointGenerator {

	// TODO: this is a workaround originally for older OWB versions, but while OWB is fixed, newer Weld versions
	// are now broken. It seems this needs to be fixed in CDI 2.0.
	// See https://issues.jboss.org/browse/CDI-610

	@Inject
	private InjectionPoint injectionPoint;

	public InjectionPoint getInjectionPoint() {
		return injectionPoint;
	}

}