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

import javax.servlet.ServletContext;

import org.omnifaces.ApplicationListener;
import org.omnifaces.util.Beans;
import org.omnifaces.util.JNDI;

/**
 * <p>
 * This configuration enum allows you to get the CDI <code>BeanManager</code> anyway in cases where
 * <code>&#64;Inject</code> and/or <code>CDI#current()</code> may not work, or when you'd like to test availability of
 * CDI without having any direct CDI dependency (as done in {@link ApplicationListener}). It will during initialization
 * grab the CDI bean manager instance as generic object from JNDI.
 * <p>
 * <strong>Do not use it directly.</strong> Use {@link Beans} utility class instead. It will under the covers use this
 * configuration enum. This configuration enum is basically a leftover from OmniFaces 1.x where the CDI dependency was
 * optional. The {@link #getReference(Class)} method is deprecated since OmniFaces 2.3 and will be removed in OmniFaces
 * 3.0.
 *
 * @author Bauke Scholtz
 * @since 1.6.1
 */
public enum BeanManager {

	// Enum singleton -------------------------------------------------------------------------------------------------

	/**
	 * Returns the lazily loaded enum singleton instance.
	 * Throws {@link IllegalStateException} when initialization fails.
	 */
	INSTANCE;

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String WELD_BEAN_MANAGER = "org.jboss.weld.environment.servlet.javax.enterprise.inject.spi.BeanManager";
	private static final Annotation[] NO_ANNOTATIONS = new Annotation[0];

	private static final String ERROR_CDI_API_UNAVAILABLE =
		"CDI API is not available in this environment.";
	private static final String ERROR_JNDI_UNAVAILABLE =
		"JNDI is not available in this environment.";
	private static final String ERROR_CDI_IMPL_UNAVAILABLE =
		"CDI BeanManager instance is not available in JNDI.";
	private static final String ERROR_INITIALIZATION_FAIL =
		"CDI BeanManager instance is available, but preparing getReference() method failed.";

	// Properties -----------------------------------------------------------------------------------------------------

	private volatile Object beanManager;
	private Method getBeans;
	private Method resolve;
	private Method createCreationalContext;
	private Method getReference;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Perform automatic initialization whereby the bean manager is looked up from the JNDI.
	 * @throws IllegalStateException When initialization fails.
	 */
	private BeanManager() {
		Class<?> beanManagerClass, contextualClass, beanClass, creationalContextClass;

		try {
			beanManagerClass = Class.forName("javax.enterprise.inject.spi.BeanManager");
			contextualClass = Class.forName("javax.enterprise.context.spi.Contextual");
			beanClass = Class.forName("javax.enterprise.inject.spi.Bean");
			creationalContextClass = Class.forName("javax.enterprise.context.spi.CreationalContext");
		}
		catch (Exception | LinkageError e) {
			throw new IllegalStateException(ERROR_CDI_API_UNAVAILABLE, e);
		}

		try {
			beanManager = JNDI.lookup("java:comp/BeanManager"); // CDI spec.

			if (beanManager == null) {
				beanManager = JNDI.lookup("java:comp/env/BeanManager"); // Tomcat.
			}
		}
		catch (IllegalStateException e) {
			throw new IllegalStateException(ERROR_CDI_IMPL_UNAVAILABLE, e);
		}
		catch (Exception | LinkageError e) {
			throw new IllegalStateException(ERROR_JNDI_UNAVAILABLE, e);
		}

		try {
			getBeans = beanManagerClass.getMethod("getBeans", Type.class, Annotation[].class);
			resolve = beanManagerClass.getMethod("resolve", Set.class);
			createCreationalContext = beanManagerClass.getMethod("createCreationalContext", contextualClass);
			getReference = beanManagerClass.getMethod("getReference", beanClass, Type.class, creationalContextClass);
		}
		catch (Exception e) {
			throw new IllegalStateException(ERROR_INITIALIZATION_FAIL, e);
		}
	}

	/**
	 * Perform manual initialization whereby the bean manager is looked up from servlet context when not available.
	 * @param servletContext The servlet context to obtain the BeanManager from, if necessary.
	 * @throws IllegalStateException When initialization fails.
	 */
	public void init(ServletContext servletContext) {
		if (beanManager != null) {
			return;
		}

		if (beanManager == null) {
			beanManager = servletContext.getAttribute(WELD_BEAN_MANAGER);
		}

		if (beanManager == null) {
			throw new IllegalStateException(ERROR_CDI_IMPL_UNAVAILABLE);
		}
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the CDI bean manager.
	 * <strong>It's preferred that you use {@link Beans#getManager()} for this.</strong>
	 * @param <T> The <code>javax.enterprise.inject.spi.BeanManager</code>.
	 * @return The CDI bean manager.
	 * @throws ClassCastException When you assign it to a variable which is not declared as CDI BeanManager.
	 */
	@SuppressWarnings("unchecked")
	public <T> T get() {
		return (T) beanManager;
	}

	/**
	 * Returns the CDI managed bean reference (proxy) of the given class.
	 * Note that this actually returns a client proxy and the underlying actual instance is thus always auto-created.
	 * @param <T> The expected return type.
	 * @param beanClass The CDI managed bean class.
	 * @return The CDI managed bean reference (proxy) of the given class, or <code>null</code> if there is none.
	 * @throws UnsupportedOperationException When obtaining the CDI managed bean reference failed with an exception.
	 * @deprecated Use {@link Beans#getReference(Class)} instead.
	 */
	@Deprecated // TODO: Remove in OmniFaces 3.0.
	public <T> T getReference(Class<T> beanClass) {
		try {
			Object bean = resolve.invoke(beanManager, getBeans.invoke(beanManager, beanClass, NO_ANNOTATIONS));

			if (bean == null) {
				return null;
			}

			Object creationalContext = createCreationalContext.invoke(beanManager, bean);
			Object reference = getReference.invoke(beanManager, bean, beanClass, creationalContext);
			return beanClass.cast(reference);
		}
		catch (Exception e) {
			throw new UnsupportedOperationException(e);
		}
	}

}