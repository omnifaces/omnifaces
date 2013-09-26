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
package org.omnifaces.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omnifaces.util.JNDI;

/**
 * Get reference to CDI managed beans without having any CDI dependency. It's using JNDI to grab the CDI bean manager
 * and if it's not <code>null</code>, then it's using reflection to get the necessary methods and invoke them.
 * <p>
 * If you already have a CDI bean manager instance at hands (and thus having a CDI dependency is no problem), then use
 * {@link org.omnifaces.util.Beans} instead.
 *
 * @author Bauke Scholtz
 * @since 1.6.1
 */
public enum BeanManager {

	// Enum singleton -------------------------------------------------------------------------------------------------

	/**
	 * Returns the lazily loaded enum singleton instance.
	 */
	INSTANCE;

	// Private constants ----------------------------------------------------------------------------------------------

	private static final Logger logger = Logger.getLogger(BeanManager.class.getName());
	private static final String LOG_INITIALIZATION_ERROR = "Beans failed to initialize.";
	private static final Annotation[] NO_ANNOTATIONS = new Annotation[0];

	// Properties -----------------------------------------------------------------------------------------------------

	private AtomicBoolean initialized = new AtomicBoolean();
	private Object beanManager;
	private Method getBeans;
	private Method resolve;
	private Method createCreationalContext;
	private Method getReference;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Perform initialization.
	 */
	private BeanManager() {
		Object beanManager = JNDI.lookup("java:comp/BeanManager"); // CDI spec.

		if (beanManager == null) {
			beanManager = JNDI.lookup("java:comp/env/BeanManager"); // Tomcat.
		}

		if (beanManager != null) {
			init(beanManager);
		}
	}

	/**
	 * Perform manual initialization with the given bean manager, if not already initialized yet. If the given bean
	 * manager is <code>null</code> and this instance is not initialized yet, then it remains uninitialized.
	 * @param beanManager The bean manager to obtain the CDI bean reference from.
	 * @return The current {@link BeanManager} instance, initialized and all if given bean manager was not <code>null</code>.
	 */
	public BeanManager init(Object beanManager) {
		if (beanManager != null && !initialized.getAndSet(true)) {
			try {
				this.beanManager = beanManager;
				Class<?> beanManagerClass = beanManager.getClass();
				Class<?> contextualClass = Class.forName("javax.enterprise.context.spi.Contextual");
				Class<?> beanClass = Class.forName("javax.enterprise.inject.spi.Bean");
				Class<?> creationalContextClass = Class.forName("javax.enterprise.context.spi.CreationalContext");
				getBeans = beanManagerClass.getMethod("getBeans", Type.class, Annotation[].class);
				resolve = beanManagerClass.getMethod("resolve", Set.class);
				createCreationalContext = beanManagerClass.getMethod("createCreationalContext", contextualClass);
				getReference = beanManagerClass.getMethod("getReference", beanClass, Type.class, creationalContextClass);
			}
			catch (Exception e) {
				logger.log(Level.SEVERE, LOG_INITIALIZATION_ERROR, e);
				throw new RuntimeException(e);
			}
		}

		return this;
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the CDI managed bean instance of the given class, or <code>null</code> if there is none.
	 * @param beanClass The type of the CDI managed bean instance.
	 * @return The CDI managed bean instance of the given class, or <code>null</code> if there is none.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getReference(Class<T> beanClass) {
		if (beanClass == null || beanManager == null) {
			return null;
		}

		try {
			Object bean = resolve.invoke(beanManager, getBeans.invoke(beanManager, beanClass, NO_ANNOTATIONS));
			Object creationalContext = createCreationalContext.invoke(beanManager, bean);
			return (T) getReference.invoke(beanManager, bean, beanClass, creationalContext);
		} catch (Exception e) {
			return null;
		}

	}

}