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
package org.omnifaces.cdi;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

/**
 * CDI bean manager. This class is theoretically reusable for multiple CDI scopes. It's currently however only used by
 * the OmniFaces CDI view scope.
 *
 * @author Radu Creanga <rdcrng@gmail.com>
 * @author Bauke Scholtz
 * @since 1.6
 */
public class BeanManager implements Serializable {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 42L;

	// Properties -----------------------------------------------------------------------------------------------------

	private final ConcurrentMap<Contextual<?>, Bean<?>> beans;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new CDI bean manager with the given initial capacity of the map holding all beans.
	 * @param initialCapacity The initial capacity of the map holding all beans.
	 */
	public BeanManager(int initialCapacity) {
		beans = new ConcurrentHashMap<Contextual<?>, Bean<?>>(initialCapacity);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Create and return the bean associated with given context and creational context.
	 * @param contextual The context to return the bean for.
	 * @param creationalContext The context to create the bean in.
	 * @return The bean associated with given context and creational context.
	 */
	public <T> T createBean(Contextual<T> contextual, CreationalContext<T> creationalContext) {
		Bean<T> bean = new Bean<T>(contextual, creationalContext);
		beans.put(contextual, bean);
		return bean.getInstance();
	}

	/**
	 * Returns the bean associated with the given context, or <code>null</code> if there is none.
	 * @param contextual The context to return the bean for.
	 * @return The bean associated with the given context, or <code>null</code> if there is none.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getBean(Contextual<T> contextual) {
		Bean<?> bean = beans.get(contextual);
		return (bean != null) ? (T) bean.getInstance() : null;
	}

	/**
	 * Destroy all beans managed so far.
	 */
	public void destroyBeans() {
		for (Bean<?> bean : beans.values()) {
			bean.destroy();
		}
	}

	// Nested classes -------------------------------------------------------------------------------------------------

	/**
	 * This class represents a bean instance. It merely offers a hook to obtain and destroy the bean instance.
	 */
	static class Bean<T> implements Serializable {

		private static final long serialVersionUID = 42L;

		private final Contextual<T> contextual;
		private final CreationalContext<T> creationalContext;
		private final T instance;

		public Bean(Contextual<T> contextual, CreationalContext<T> creationalContext) {
			this.contextual = contextual;
			this.creationalContext = creationalContext;
			instance = this.contextual.create(this.creationalContext);
		}

		public T getInstance() {
			return instance;
		}

		public void destroy() {
			contextual.destroy(instance, creationalContext);
		}

	}

}