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
	private static final String LOG_INITIALIZATION_ERROR = "BeanManager enum singleton failed to initialize.";
	private static final Annotation[] NO_ANNOTATIONS = new Annotation[0];

	// Properties -----------------------------------------------------------------------------------------------------

	private AtomicBoolean initialized = new AtomicBoolean();
	private Object beanManager;
	private Method getBeans;
	private Method resolve;
	private Method createCreationalContext;
	private Method getReference;

	// Init -----------------------------------------------------------------------------------------------------------

	/**
	 * Perform automatic initialization whereby the bean manager is looked up from the JNDI. If the bean manager is
	 * found, then invoke {@link #init(Object)} with the found bean manager.
	 */
	private void init() {
		if (!initialized.getAndSet(true)) {
			try {
				Class.forName("javax.enterprise.inject.spi.BeanManager"); // Is CDI present?
				JNDI.lookup("java:comp"); // Is JNDI present? (not on Google App Engine)
			}
			catch (Exception e) {
				return; // CDI or JNDI not supported on this environment.
			}

			try {
				Object beanManager = JNDI.lookup("java:comp/BeanManager"); // CDI spec.

				if (beanManager == null) {
					beanManager = JNDI.lookup("java:comp/env/BeanManager"); // Tomcat.
				}

				if (beanManager == null) {
					return; // CDI not registered on this environment.
				}

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
			catch (RuntimeException e) {
				return; // CDI most likely just not supported on this environment.
			}
			catch (Exception e) {
				initialized.set(false);
				logger.log(Level.SEVERE, LOG_INITIALIZATION_ERROR, e);
				throw new RuntimeException(e);
			}
		}
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the CDI managed bean instance of the given class, or <code>null</code> if there is none.
	 * @param beanClass The type of the CDI managed bean instance.
	 * @return The CDI managed bean instance of the given class, or <code>null</code> if there is none.
	 */
	public <T> T getReference(Class<T> beanClass) {
		// This init() call is performed here instead of in constructor, because WebLogic loads this enum as a CDI
		// managed bean (in spite of having a VetoAnnotatedTypeExtension) which in turn implicitly invokes the enum
		// constructor and thus causes an init while CDI context isn't fully initialized and thus the bean manager
		// isn't available in JNDI yet. Perhaps it's fixed in newer WebLogic versions.
		init();

		if (beanManager == null) {
			return null; // CDI not supported on this environment.
		}

		try {
			Object bean = resolve.invoke(beanManager, getBeans.invoke(beanManager, beanClass, NO_ANNOTATIONS));
			Object creationalContext = createCreationalContext.invoke(beanManager, bean);
			Object reference = getReference.invoke(beanManager, bean, beanClass, creationalContext);
			return beanClass.cast(reference);
		}
		catch (Exception e) {
			return null;
		}
	}

}