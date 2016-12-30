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
package org.omnifaces.cdi.param;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessManagedBean;

import org.omnifaces.cdi.Param;

/**
 * CDI extension that works around the fact that CDI insists on doing absolutely
 * guaranteed type safe injections. While generally applaudable this unfortunately
 * impedes writing true generic producers that dynamically do conversion based on
 * the target type.
 * <p>
 * This extension collects the target types of each injection point qualified with
 * the <code>&#64;</code>{@link Param} annotation and dynamically registers Beans that effectively
 * represents producers for each type.
 *
 * @since 2.0
 * @author Arjan Tijms
 */
public class ParamExtension implements Extension {

	private Set<Type> types = new HashSet<>();

	public <T> void collect(@Observes ProcessManagedBean<T> event) {
		for (AnnotatedField<?> field : event.getAnnotatedBeanClass().getFields()) {
			addAnnotatedTypeIfNecessary(field);
		}

		for (AnnotatedConstructor<?> constructor : event.getAnnotatedBeanClass().getConstructors()) {
			for (AnnotatedParameter<?> parameter : constructor.getParameters()) {
				addAnnotatedTypeIfNecessary(parameter);
			}
		}
	}

	private void addAnnotatedTypeIfNecessary(Annotated annotated) {
		if (annotated.isAnnotationPresent(Param.class)) {
			Type type = annotated.getBaseType();

			// Skip ParamValue as it is already handled by RequestParameterProducer.
			if (type instanceof ParameterizedType && ParamValue.class.isAssignableFrom((Class<?>) ((ParameterizedType) type).getRawType())) {
				return;
			}

			types.add(type);
		}
	}

	public void afterBean(@Observes AfterBeanDiscovery afterBeanDiscovery) {
		for (Type type : types) {
			afterBeanDiscovery.addBean(new DynamicParamValueProducer(type));
		}
	}

}