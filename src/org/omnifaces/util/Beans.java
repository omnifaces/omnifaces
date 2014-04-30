/*
 * Copyright 2013 OmniFaces.
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
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.spi.Context;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

/**
 * Collection of utility methods for the CDI API with respect to working with {@link BeanManager}.
 * <p>
 * If you need a dependency-free way of obtaining the CDI managed bean instance (e.g. when you want to write code which
 * should also run on Tomcat), use {@link org.omnifaces.config.BeanManager} instead.
 *
 * @author Bauke Scholtz
 * @since 1.6.1
 */
public final class Beans {

	/**
	 * Resolve and returns the CDI managed bean of the given class from the given bean manager.
	 * @param beanManager The involved CDI bean manager.
	 * @param beanClass The type of the CDI managed bean instance.
	 * @return The resolved CDI managed bean of the given class from the given bean manager.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Bean<T> resolve(BeanManager beanManager, Class<T> beanClass) {
		for (Bean<?> bean : beanManager.getBeans(beanClass)) {
			if (bean.getBeanClass() == beanClass) {
				return (Bean<T>) beanManager.resolve(Collections.<Bean<?>>singleton(bean));
			}
		}

		return null;
	}

	/**
	 * Returns the CDI managed bean reference of the given class from the given bean manager.
	 * Note that this actually returns a client proxy and the underlying instance is thus always auto-created.
	 * @param beanManager The involved CDI bean manager.
	 * @param beanClass The type of the CDI managed bean instance.
	 * @return The CDI managed bean reference of the given class from the given bean manager.
	 */
	public static <T> T getReference(BeanManager beanManager, Class<T> beanClass) {
		return getReference(beanManager, resolve(beanManager, beanClass));
	}

	/**
	 * Returns the CDI managed bean reference of the given resolved bean from the given bean manager.
	 * Note that this actually returns a client proxy and the underlying instance is thus always auto-created.
	 * @param beanManager The involved CDI bean manager.
	 * @param bean The resolved bean of the CDI managed bean instance.
	 * @return The CDI managed bean reference of the given resolved bean from the given bean manager.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getReference(BeanManager beanManager, Bean<T> bean) {
		return (T) beanManager.getReference(bean, bean.getBeanClass(), beanManager.createCreationalContext(bean));
	}
	
	/**
	 * Returns the CDI managed bean instance of the given class from the given bean manager and creates one if
	 * one doesn't exist.
	 * @param beanManager The involved CDI bean manager.
	 * @param beanClass The type of the CDI managed bean instance.
	 * @return The CDI managed bean instance of the given class from the given bean manager.
	 * @since 1.8
	 */
	public static <T> T getInstance(BeanManager beanManager, Class<T> beanClass) {
		return getInstance(beanManager, resolve(beanManager, beanClass), true);
	}

	/**
	 * Returns the CDI managed bean instance of the given class from the given bean manager and creates one if
	 * one doesn't exist and <code>create</code> argument is <code>true</code>, otherwise don't create one and return
	 * <code>null</code> if there's no current instance.
	 * @param beanManager The involved CDI bean manager.
	 * @param beanClass The type of the CDI managed bean instance.
	 * @param create If <code>true</code>, then create one if one doesn't exist, otherwise don't create one and return
	 * <code>null</code> if there's no current instance.
	 * @return The CDI managed bean instance of the given class from the given bean manager.
	 * @since 1.7
	 */
	public static <T> T getInstance(BeanManager beanManager, Class<T> beanClass, boolean create) {
		return getInstance(beanManager, resolve(beanManager, beanClass), create);
	}

	/**
	 * Returns the CDI managed bean instance of the given resolved bean from the given bean manager and creates one if
	 * one doesn't exist and <code>create</code> argument is <code>true</code>, otherwise don't create one and return
	 * <code>null</code> if there's no current instance.
	 * @param beanManager The involved CDI bean manager.
	 * @param bean The resolved bean of the CDI managed bean instance.
	 * @param create If <code>true</code>, then create one if one doesn't exist, otherwise don't create one and return
	 * <code>null</code> if there's no current instance.
	 * @return The CDI managed bean instance of the given class from the given bean manager.
	 * @since 1.7
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
	 * Returns all active CDI managed bean instances in the given CDI managed bean scope. The map key represents
	 * the active CDI managed bean instance and the map value represents the CDI managed bean name, if any.
	 * @param beanManager The involved CDI bean manager.
	 * @param scope The CDI managed bean scope, e.g. <code>RequestScoped.class</code>.
	 * @return All active CDI managed bean instances in the given CDI managed bean scope.
	 * @since 1.7
	 */
	public static Map<Object, String> getActiveInstances(BeanManager beanManager, Class<? extends Annotation> scope) {
		Map<Object, String> activeInstances = new HashMap<Object, String>();
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

}