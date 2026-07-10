/*
 * Copyright OmniFaces
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

	private transient int activeRequests;
	private transient boolean evicted;
	private transient boolean destroyed;

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
	 * Registers that the current HTTP request has started using this bean storage, which will keep its beans alive
	 * until {@link #release()}.
	 * @return <code>false</code> when the beans have meanwhile been destroyed, in which case this bean storage must no
	 * longer be used.
	 * @since 3.14.22
	 */
	public synchronized boolean acquire() {
		if (destroyed) {
			return false;
		}

		activeRequests++;
		return true;
	}

	/**
	 * Registers that the current HTTP request has finished using this bean storage. When it was meanwhile evicted, and
	 * this was the last HTTP request using it, then its beans are destroyed.
	 * @since 3.14.22
	 */
	public synchronized void release() {
		if (activeRequests > 0 && --activeRequests == 0 && evicted) {
			destroyBeans();
		}
	}

	/**
	 * Registers that this bean storage has been evicted. Its beans are destroyed immediately when no HTTP request is
	 * currently using it, otherwise the last HTTP request finishing with it will destroy them.
	 * @since 3.14.22
	 */
	public synchronized void evict() {
		evicted = true;

		if (activeRequests == 0) {
			destroyBeans();
		}
	}

	/**
	 * Destroy all beans managed so far. This is a no-op when they have already been destroyed.
	 */
	public synchronized void destroyBeans() { // Synchronization is necessary to keep it atomic against acquire().
		if (destroyed) {
			return;
		}

		for (Object bean : beans.values()) {
			destroy(bean);
		}

		beans.clear();
		destroyed = true;
	}

}