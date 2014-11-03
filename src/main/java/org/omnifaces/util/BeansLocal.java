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
package org.omnifaces.util;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.enterprise.context.NormalScope;
import javax.enterprise.context.spi.AlterableContext;
import javax.enterprise.context.spi.Context;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

/**
 * <p>
 * Collection of utility methods for the CDI API that are mainly shortcuts for obtaining stuff from the
 * {@link BeanManager}.
 * <p>
 * The difference with {@link Beans} is that no one method of {@link BeansLocal} obtains the {@link BeanManager} from
 * JNDI. This job is up to the caller. This is more efficient in situations where multiple utility methods needs to be
 * called at the same time.
 *
 * @author Bauke Scholtz
 * @since 2.0
 */
@Typed
public final class BeansLocal {

	/**
	 * {@inheritDoc}
	 * @see Beans#resolve(Class)
	 */
	@SuppressWarnings("unchecked")
	public static <T> Bean<T> resolve(BeanManager beanManager, Class<T> beanClass) {
		Set<Bean<?>> beans = beanManager.getBeans(beanClass);

		for (Bean<?> bean : beans) {
			if (bean.getBeanClass() == beanClass) {
				return (Bean<T>) beanManager.resolve(Collections.<Bean<?>>singleton(bean));
			}
		}

		return (Bean<T>) beanManager.resolve(beans);
	}

	/**
	 * {@inheritDoc}
	 * @see Beans#getReference(Class)
	 */
	public static <T> T getReference(BeanManager beanManager, Class<T> beanClass) {
		Bean<T> bean = resolve(beanManager, beanClass);
		return (bean != null) ? getReference(beanManager, bean) : null;
	}

	/**
	 * {@inheritDoc}
	 * @see Beans#getReference(Bean)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getReference(BeanManager beanManager, Bean<T> bean) {
		return (T) beanManager.getReference(bean, bean.getBeanClass(), beanManager.createCreationalContext(bean));
	}

	/**
	 * {@inheritDoc}
	 * @see Beans#getInstance(Class)
	 */
	public static <T> T getInstance(BeanManager beanManager, Class<T> beanClass) {
		return getInstance(beanManager, beanClass, true);
	}

	/**
	 * {@inheritDoc}
	 * @see Beans#getInstance(Class, boolean)
	 */
	public static <T> T getInstance(BeanManager beanManager, Class<T> beanClass, boolean create) {
		Bean<T> bean = resolve(beanManager, beanClass);
		return (bean != null) ? getInstance(beanManager, bean, create) : null;
	}

	/**
	 * {@inheritDoc}
	 * @see Beans#getInstance(Bean, boolean)
	 */
	public static <T> T getInstance(BeanManager beanManager, Bean<T> bean, boolean create) {
		Context context = beanManager.getContext(bean.getScope());

		if (create) {
			return context.get(bean, beanManager.createCreationalContext(bean));
		}
		else {
			return context.get(bean);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see Beans#getActiveInstances(Class)
	 */
	public static <S extends NormalScope> Map<Object, String> getActiveInstances(BeanManager beanManager, Class<S> scope) {
		Map<Object, String> activeInstances = new HashMap<>();
		Set<Bean<?>> beans = beanManager.getBeans(Object.class);
		Context context = beanManager.getContext(scope);

		for (Bean<?> bean : beans) {
			Object instance = context.get(bean);

			if (instance != null) {
				activeInstances.put(instance, bean.getName());
			}
		}

		return Collections.unmodifiableMap(activeInstances);
	}

	/**
	 * {@inheritDoc}
	 * @see Beans#destroy(Class)
	 */
	public static <T> void destroy(BeanManager beanManager, Class<T> beanClass) {
		Bean<T> bean = resolve(beanManager, beanClass);

		if (bean != null) {
			destroy(beanManager, bean);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see Beans#destroy(Bean)
	 */
	public static <T> void destroy(BeanManager beanManager, Bean<T> bean) {
		AlterableContext context = (AlterableContext) beanManager.getContext(bean.getScope());
		context.destroy(bean);
	}

	/**
	 * {@inheritDoc}
	 * @see Beans#getAnnotation(Annotated, Class)
	 */
	public static <A extends Annotation> A getAnnotation(BeanManager beanManager, Annotated annotated, Class<A> annotationType) {

		annotated.getAnnotation(annotationType);

		if (annotated.getAnnotations().isEmpty()) {
			return null;
		}

		if (annotated.isAnnotationPresent(annotationType)) {
			return annotated.getAnnotation(annotationType);
		}

		Queue<Annotation> annotations = new LinkedList<>(annotated.getAnnotations());

		while (!annotations.isEmpty()) {
			Annotation annotation = annotations.remove();

			if (annotation.annotationType().equals(annotationType)) {
				return annotationType.cast(annotation);
			}

			if (beanManager.isStereotype(annotation.annotationType())) {
				annotations.addAll(beanManager.getStereotypeDefinition(annotation.annotationType()));
			}
		}

		return null;
	}

}