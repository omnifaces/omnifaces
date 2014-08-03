/*
 * Copyright 2014 OmniFaces.
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
package org.omnifaces.cdi.param;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionPoint;

import org.omnifaces.cdi.Param;

/**
 * CDI extension that works around the fact that CDI insists on doing absolutely
 * guaranteed type safe injections. While generally applaudable this unfortunately 
 * impedes writing true generic producers that dynamically do conversion based on 
 * the target type.
 * <p>
 * This extension collects the target types of each injection point qualified with
 * the {@link Param} annotation and dynamically registers Beans that effectively
 * represents producers for each type. 
 *
 * @since 2.0
 * @author Arjan Tijms
 */
public class ParamExtension implements Extension {

	private Set<Type> types = new HashSet<>();
	
	public void OnProcessInjectionPoint(@Observes ProcessInjectionPoint<?, ?> event) {
		
		InjectionPoint injectionPoint = event.getInjectionPoint();

		if (injectionPoint.getAnnotated().isAnnotationPresent(Param.class)) {
			// For now only support injection into simple class types like java.lang.String etc
			if (injectionPoint.getType() instanceof Class) {
				types.add(injectionPoint.getType());
			}
		}
	}

	public void afterBean(final @Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {
		for (Type type : types) {
			afterBeanDiscovery.addBean(new DynamicParamValueProdcuer(beanManager, type));
		}
	}

}