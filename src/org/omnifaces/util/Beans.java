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
	 * Returns the CDI managed bean instance of the given class from the given bean manager.
	 * @param beanManager The involved CDI bean manager.
	 * @param beanClass The type of the CDI managed bean instance.
	 * @return The CDI managed bean instance of the given class from the given bean manager.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getReference(BeanManager beanManager, Class<T> beanClass) {
		return getReference(beanManager, (Bean<T>) beanManager.resolve(beanManager.getBeans(beanClass)));
	}

	/**
	 * Returns the CDI managed bean instance of the given resolved bean from the given bean manager.
	 * @param beanManager The involved CDI bean manager.
	 * @param bean The resolved bean of the CDI managed bean instance.
	 * @return The CDI managed bean instance of the given resolved bean from the given bean manager.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getReference(BeanManager beanManager, Bean<T> bean) {
		return (T) beanManager.getReference(bean, bean.getBeanClass(), beanManager.createCreationalContext(bean));
	}

}