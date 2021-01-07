/*
 * Copyright 2021 OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.cdi;

import static org.omnifaces.util.Beans.destroy;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
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

	private static final long serialVersionUID = 1L;

	// Properties -----------------------------------------------------------------------------------------------------

	private final ConcurrentHashMap<String, Serializable> beans;

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
	 * @throws ClassCastException When the bean doesn't implement serializable.
	 */
	public <T> T createBean(Contextual<T> type, CreationalContext<T> context) {
		T bean = type.create(context);
		beans.put(getBeanId(type), (Serializable) bean);
		return bean;
	}

	/**
	 * Returns the bean associated with the given context, or <code>null</code> if there is none.
	 * @param <T> The generic bean type.
	 * @param type The contextual type of the CDI managed bean.
	 * @return The bean associated with the given context, or <code>null</code> if there is none.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getBean(Contextual<T> type) {
		return (T) beans.get(getBeanId(type));
	}

	/**
	 * Returns the bean identifier of the given type.
	 */
	private static String getBeanId(Contextual<?> type) {
		return (type instanceof PassivationCapable) ? ((PassivationCapable) type).getId() : type.getClass().getName();
	}

	/**
	 * Destroy all beans managed so far.
	 */
	public synchronized void destroyBeans() { // Not sure if synchronization is absolutely necessary. Just to be on safe side.
		for (Object bean : beans.values()) {
			destroy(bean);
		}

		beans.clear();
	}

}