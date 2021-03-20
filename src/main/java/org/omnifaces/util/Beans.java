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
package org.omnifaces.util;

import static java.util.logging.Level.FINE;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static org.omnifaces.util.Reflection.toClassOrNull;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.AlterableContext;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 * <p>
 * Collection of utility methods for the CDI API that are mainly shortcuts for obtaining stuff from the
 * {@link BeanManager}.
 *
 * <h3>Usage</h3>
 * <p>
 * Some examples:
 * <pre>
 * // Get the CDI managed bean reference (proxy) of the given bean class.
 * SomeBean someBean = Beans.getReference(SomeBean.class);
 * </pre>
 * <pre>
 * // Get the CDI managed bean instance (actual) of the given bean class.
 * SomeBean someBean = Beans.getInstance(SomeBean.class);
 * </pre>
 * <pre>
 * // Check if CDI session scope is active in current context.
 * Beans.isActive(SessionScope.class);
 * </pre>
 * <pre>
 * // Get all currently active CDI managed bean instances in the session scope.
 * Map&lt;Object, String&gt; activeSessionScopedBeans = Beans.getActiveInstances(SessionScope.class);
 * </pre>
 * <pre>
 * // Destroy any currently active CDI managed bean instance of given bean class.
 * Beans.destroy(SomeBean.class);
 * </pre>
 * <pre>
 * // Fire a CDI event.
 * Beans.fireEvent(someEvent);
 * </pre>
 * <p>
 *
 * @author Bauke Scholtz
 * @since 1.6.1
 */
@Typed
public final class Beans {

	private static final Logger logger = Logger.getLogger(Beans.class.getName());

	private static final String[] PROXY_INTERFACE_NAMES = {
		"org.jboss.weld.proxy.WeldConstruct",
		"org.apache.webbeans.proxy.OwbNormalScopeProxy"
	};

	// Both Weld and OWB generate proxy class names as "BeanClassName[...]$$[...]Proxy[...]" with a "$$" and "Proxy" in it.
	// Hopefully unknown CDI proxy implementations follow the same de-facto standard.
	private static final Pattern PATTERN_GENERATED_PROXY_CLASS_NAME = Pattern.compile("(.+)(\\$\\$(.*)Proxy|Proxy(.*)\\$\\$)(.*)", CASE_INSENSITIVE);


	// Constructors ---------------------------------------------------------------------------------------------------

	private Beans() {
		// Hide constructor.
	}

	// Utility --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the CDI bean manager.
	 * @return The CDI bean manager.
	 * @since 2.0
	 * @see CDI#getBeanManager()
	 */
	public static BeanManager getManager() {
		try {
			return CDI.current().getBeanManager();
		}
		catch (Exception | LinkageError e) {
			logger.log(FINE, "Cannot get BeanManager from CDI.current(); falling back to JNDI.", e);
			return JNDI.lookup("java:comp/BeanManager");
		}
	}

	/**
	 * Returns the CDI managed bean representation of the given bean class, optionally with the given qualifiers.
	 * @param <T> The generic CDI managed bean type.
	 * @param beanClass The CDI managed bean class.
	 * @param qualifiers The CDI managed bean qualifiers, if any.
	 * @return The CDI managed bean representation of the given bean class, or <code>null</code> if there is none.
	 * @see BeanManager#getBeans(java.lang.reflect.Type, Annotation...)
	 * @see BeanManager#resolve(java.util.Set)
	 */
	public static <T> Bean<T> resolve(Class<T> beanClass, Annotation... qualifiers) {
		return BeansLocal.resolve(getManager(), beanClass, qualifiers);
	}

	/**
	 * Returns the CDI managed bean representation of exactly the given bean class, optionally with the given qualifiers.
	 * This will ignore any subclasses.
	 * @param <T> The generic CDI managed bean type.
	 * @param beanClass The CDI managed bean class.
	 * @param qualifiers The CDI managed bean qualifiers, if any.
	 * @return The CDI managed bean representation of the given bean class, or <code>null</code> if there is none.
	 * @see BeanManager#getBeans(java.lang.reflect.Type, Annotation...)
	 * @see BeanManager#resolve(java.util.Set)
	 * @since 3.1
	 */
	public static <T> Bean<T> resolveExact(Class<T> beanClass, Annotation... qualifiers) {
		return BeansLocal.resolveExact(getManager(), beanClass, qualifiers);
	}

	/**
	 * Returns the CDI managed bean reference (proxy) of the given bean class, optionally with the given qualifiers.
	 * Note that this actually returns a client proxy and the underlying actual instance is thus always auto-created.
	 * @param <T> The expected return type.
	 * @param beanClass The CDI managed bean class.
	 * @param qualifiers The CDI managed bean qualifiers, if any.
	 * @return The CDI managed bean reference (proxy) of the given class, or <code>null</code> if there is none.
	 * @see #resolve(Class, Annotation...)
	 * @see #getReference(Bean)
	 * @see #resolve(Class, Annotation...)
	 */
	public static <T> T getReference(Class<T> beanClass, Annotation... qualifiers) {
		return BeansLocal.getReference(getManager(), beanClass, qualifiers);
	}

	/**
	 * Returns the CDI managed bean reference (proxy) of the given bean representation.
	 * Note that this actually returns a client proxy and the underlying actual instance is thus always auto-created.
	 * @param <T> The expected return type.
	 * @param bean The CDI managed bean representation.
	 * @return The CDI managed bean reference (proxy) of the given bean, or <code>null</code> if there is none.
	 * @see BeanManager#createCreationalContext(javax.enterprise.context.spi.Contextual)
	 * @see BeanManager#getReference(Bean, java.lang.reflect.Type, javax.enterprise.context.spi.CreationalContext)
	 */
	public static <T> T getReference(Bean<T> bean) {
		return BeansLocal.getReference(getManager(), bean);
	}

	/**
	 * Returns the CDI managed bean instance (actual) of the given bean class, optionally with the given qualifiers,
	 * and creates one if one doesn't exist.
	 * @param <T> The expected return type.
	 * @param beanClass The CDI managed bean class.
	 * @param qualifiers The CDI managed bean qualifiers, if any.
	 * @return The CDI managed bean instance (actual) of the given bean class.
	 * @since 1.8
	 * @see #getInstance(Class, boolean, Annotation...)
	 */
	public static <T> T getInstance(Class<T> beanClass, Annotation... qualifiers) {
		return BeansLocal.getInstance(getManager(), beanClass, qualifiers);
	}

	/**
	 * Returns the CDI managed bean instance (actual) of the given bean class, optionally with the given qualifiers,
	 * and creates one if one doesn't exist and <code>create</code> argument is <code>true</code>, otherwise don't
	 * create one and return <code>null</code> if there's no current instance.
	 * @param <T> The expected return type.
	 * @param beanClass The CDI managed bean class.
	 * @param create Whether to create create CDI managed bean instance if one doesn't exist.
	 * @param qualifiers The CDI managed bean qualifiers, if any.
	 * @return The CDI managed bean instance (actual) of the given bean class, or <code>null</code> if there is none
	 * and/or the <code>create</code> argument is <code>false</code>.
	 * @since 1.7
	 * @see #resolve(Class, Annotation...)
	 * @see #getInstance(Bean, boolean)
	 */
	public static <T> T getInstance(Class<T> beanClass, boolean create, Annotation... qualifiers) {
		return BeansLocal.getInstance(getManager(), beanClass, create, qualifiers);
	}

	/**
	 * Returns the CDI managed bean instance (actual) of the given bean representation and creates one if one doesn't
	 * exist and <code>create</code> argument is <code>true</code>, otherwise don't create one and return
	 * <code>null</code> if there's no current instance.
	 * @param <T> The expected return type.
	 * @param bean The CDI managed bean representation.
	 * @param create Whether to create create CDI managed bean instance if one doesn't exist.
	 * @return The CDI managed bean instance (actual) of the given bean, or <code>null</code> if there is none and/or
	 * the <code>create</code> argument is <code>false</code>.
	 * @since 1.7
	 * @see BeanManager#getContext(Class)
	 * @see BeanManager#createCreationalContext(javax.enterprise.context.spi.Contextual)
	 * @see Context#get(javax.enterprise.context.spi.Contextual, javax.enterprise.context.spi.CreationalContext)
	 */
	public static <T> T getInstance(Bean<T> bean, boolean create) {
		return BeansLocal.getInstance(getManager(), bean, create);
	}

	/**
	 * Returns <code>true</code> if given object or class is actually a CDI proxy.
	 * @param <T> The generic CDI managed bean type.
	 * @param object The object to be checked.
	 * @return <code>true</code> if given object or class is actually a CDI proxy.
	 * @since 3.8
	 */
	public static <T> boolean isProxy(T object) {
		if (object == null) {
			return false;
		}

		Class<?> beanClass = (object instanceof Class) ? (Class<?>) object : object.getClass();

		for (String proxyInterfaceName : PROXY_INTERFACE_NAMES) {
			Class<?> proxyInterface = toClassOrNull(proxyInterfaceName);

			if (proxyInterface != null && proxyInterface.isAssignableFrom(beanClass)) {
				return true;
			}
		}

		// Fall back for unknown CDI proxy implementations.
		return PATTERN_GENERATED_PROXY_CLASS_NAME.matcher(beanClass.getSimpleName()).matches();
	}

	/**
	 * Returns the actual instance or class of the given object or class if it is actually a CDI proxy as per {@link Beans#isProxy(Object)}.
	 * @param <T> The generic CDI managed bean type.
	 * @param object The object or class to be unwrapped.
	 * @return The actual instance or class of the given object or class if it is actually a CDI proxy as per {@link Beans#isProxy(Object)}.
	 * @since 3.8
	 */
	public static <T> T unwrapIfNecessary(T object) {
		return BeansLocal.unwrapIfNecessary(getManager(), object);
	}

	/**
	 * Returns <code>true</code> when the given CDI managed bean scope is active. I.e., all beans therein can be
	 * accessed without facing {@link ContextNotActiveException}.
	 * @param <S> The generic CDI managed bean scope type.
	 * @param scope The CDI managed bean scope, e.g. <code>SessionScoped.class</code>.
	 * @return <code>true</code> when the given CDI managed bean scope is active.
	 * @since 2.3
	 * @see BeanManager#getContext(Class)
	 * @see Context#isActive()
	 */
	public static <S extends Annotation> boolean isActive(Class<S> scope) {
		return BeansLocal.isActive(getManager(), scope);
	}

	/**
	 * Returns all active CDI managed bean instances in the given CDI managed bean scope. The map key represents
	 * the active CDI managed bean instance and the map value represents the CDI managed bean name, if any.
	 * @param <S> The generic CDI managed bean scope type.
	 * @param scope The CDI managed bean scope, e.g. <code>RequestScoped.class</code>.
	 * @return All active CDI managed bean instances in the given CDI managed bean scope.
	 * @since 1.7
	 * @see BeanManager#getBeans(java.lang.reflect.Type, Annotation...)
	 * @see BeanManager#getContext(Class)
	 * @see Context#get(javax.enterprise.context.spi.Contextual)
	 */
	public static <S extends Annotation> Map<Object, String> getActiveInstances(Class<S> scope) {
		return BeansLocal.getActiveInstances(getManager(), scope);
	}

	/**
	 * Destroy the currently active instance of the given CDI managed bean class, optionally with the given qualifiers.
	 * @param <T> The generic CDI managed bean type.
	 * @param beanClass The CDI managed bean class.
	 * @param qualifiers The CDI managed bean qualifiers, if any.
	 * @since 2.0
	 * @see #resolve(Class, Annotation...)
	 * @see BeanManager#getContext(Class)
	 * @see AlterableContext#destroy(javax.enterprise.context.spi.Contextual)
	 */
	public static <T> void destroy(Class<T> beanClass, Annotation... qualifiers) {
		BeansLocal.destroy(getManager(), beanClass, qualifiers);
	}

	/**
	 * Destroy the currently active instance of the given CDI managed bean representation.
	 * @param <T> The generic CDI managed bean type.
	 * @param bean The CDI managed bean representation.
	 * @throws IllegalArgumentException When the given CDI managed bean type is actually not put in an alterable
	 * context.
	 * @since 2.0
	 * @see BeanManager#getContext(Class)
	 * @see AlterableContext#destroy(javax.enterprise.context.spi.Contextual)
	 */
	public static <T> void destroy(Bean<T> bean) {
		BeansLocal.destroy(getManager(), bean);
	}

	/**
	 * Destroy the currently active instance of the given CDI managed bean instance.
	 * @param <T> The generic CDI managed bean type.
	 * @param instance The CDI managed bean instance.
	 * @throws IllegalArgumentException When the given CDI managed bean type is actually not put in an alterable
	 * context.
	 * @since 2.5
	 * @see #resolve(Class, Annotation...)
	 * @see BeanManager#createCreationalContext(Contextual)
	 * @see Contextual#destroy(Object, CreationalContext)
	 */
	public static <T> void destroy(T instance) {
		BeansLocal.destroy(getManager(), instance);
	}

	/**
	 * Get program element annotation of a certain annotation type. The difference with
	 * {@link Annotated#getAnnotation(Class)} is that this method will recursively search inside all {@link Stereotype}
	 * annotations.
	 * @param <A> The generic annotation type.
	 * @param annotated A Java program element that can be annotated.
	 * @param annotationType The class of the annotation type.
	 * @return The program element annotation of the given annotation type if it could be found, otherwise
	 * <code>null</code>.
	 * @since 1.8
	 */
	public static <A extends Annotation> A getAnnotation(Annotated annotated, Class<A> annotationType) {
		return BeansLocal.getAnnotation(getManager(), annotated, annotationType);
	}

	/**
	 * Gets the current injection point when called from a context where injection is taking place (e.g. from a producer).
	 * <p>
	 * This is mostly intended to be used from within a dynamic producer {@link Bean}. For a "regular" producer (using {@link Produces})
	 * an <code>InjectionPoint</code> can either be injected into the bean that contains the producer method, or directly provided as argument
	 * of said method.
	 *
	 * @param creationalContext a {@link CreationalContext} used to manage objects with a
	 *        {@link javax.enterprise.context.Dependent} scope
	 * @return the current injection point when called from a context where injection is taking place (e.g. from a producer)
	 */
	public static InjectionPoint getCurrentInjectionPoint(CreationalContext<?> creationalContext) {
		return BeansLocal.getCurrentInjectionPoint(getManager(), creationalContext);
	}

	/**
	 * Returns the qualifier annotation of the given qualifier class from the given injection point.
	 * @param <A> The generic annotation type.
	 * @param injectionPoint The injection point to obtain the qualifier annotation of the given qualifier class from.
	 * @param qualifierClass The class of the qualifier annotation to be looked up in the given injection point.
	 * @return The qualifier annotation of the given qualifier class from the given injection point.
	 * @since 2.1
	 */
	public static <A extends Annotation> A getQualifier(InjectionPoint injectionPoint, Class<A> qualifierClass) {
		for (Annotation annotation : injectionPoint.getQualifiers()) {
			if (qualifierClass.isAssignableFrom(annotation.getClass())) {
				return qualifierClass.cast(annotation);
			}
		}

		return null;
	}

	/**
	 * Fires the given CDI event, optionally with the given qualifiers.
	 * @param event The event object.
	 * @param qualifiers The event qualifiers, if any.
	 * @since 2.3
	 * @see BeanManager#fireEvent(Object, Annotation...)
	 */
	public static void fireEvent(Object event, Annotation... qualifiers) {
		BeansLocal.fireEvent(getManager(), event, qualifiers);
	}

}