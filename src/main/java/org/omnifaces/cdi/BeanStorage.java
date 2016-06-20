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
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.PassivationCapable;

/**
 * CDI bean storage. This class is theoretically reusable for multiple CDI scopes. It's currently however only used by
 * the OmniFaces CDI view scope.
 *
 * @author Radu Creanga {@literal <rdcrng@gmail.com>}
 * @author Bauke Scholtz
 * @since 1.6
 */
public class BeanStorage implements Serializable {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 42L;

	// Properties -----------------------------------------------------------------------------------------------------

	private final ConcurrentMap<String, Bean<?>> beans;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new CDI bean storage with the given initial capacity of the map holding all beans.
	 * @param initialCapacity The initial capacity of the map holding all beans.
	 */
	public BeanStorage(int initialCapacity) {
		beans = new ConcurrentHashMap<>(initialCapacity);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Create and return the bean associated with given context and creational context.
	 * @param <T> The generic bean type.
	 * @param type The contextual type of the CDI managed bean.
	 * @param context The context to create the bean in.
	 * @return The bean associated with given context and creational context.
	 */
	public <T> T createBean(Contextual<T> type, CreationalContext<T> context) {
		Bean<T> bean = new Bean<>(type, context);
		beans.put(((PassivationCapable) type).getId(), bean);
		return bean.getInstance();
	}

	/**
	 * Returns the bean associated with the given context, or <code>null</code> if there is none.
	 * @param <T> The generic bean type.
	 * @param type The contextual type of the CDI managed bean.
	 * @param manager The bean manager used to create the creational context, if necessary.
	 * @return The bean associated with the given context, or <code>null</code> if there is none.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getBean(Contextual<T> type, BeanManager manager) {
		Bean<T> bean = (Bean<T>) beans.get(((PassivationCapable) type).getId());

		if (bean == null) {
			return null;
		}

		if (!bean.hasContext()) { // May happen after passivation.
			bean.setContext(type, manager.createCreationalContext(type));
		}

		return bean.getInstance();
	}

	/**
	 * Destroy all beans managed so far.
	 */
	public synchronized void destroyBeans() { // Not sure if synchronization is absolutely necessary. Just to be on safe side.
		for (Bean<?> bean : beans.values()) {
			bean.destroy();
		}

		beans.clear();
	}

	// Nested classes -------------------------------------------------------------------------------------------------

	/**
	 * This class represents a bean instance. It merely offers a hook to obtain and destroy the bean instance.
	 */
	static final class Bean<T> implements Serializable {

		private static final long serialVersionUID = 42L;

		private transient Contextual<T> type;
		private transient CreationalContext<T> context;
		private final T instance;

		public Bean(Contextual<T> type, CreationalContext<T> context) {
			setContext(type, context);
			instance = type.create(context);
		}

		public void setContext(Contextual<T> type, CreationalContext<T> context) {
			this.type = type;
			this.context = context;
		}

		public boolean hasContext() {
			return type != null && context != null;
		}

		public T getInstance() {
			return instance;
		}

		public void destroy() {
			if (hasContext()) {
				type.destroy(instance, context);
			}
		}

	}

}