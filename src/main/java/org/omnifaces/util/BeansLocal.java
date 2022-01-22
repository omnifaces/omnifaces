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
package org.omnifaces.util;

import static java.util.logging.Level.FINEST;
import static org.omnifaces.util.Beans.isProxy;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.enterprise.context.spi.AlterableContext;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;

import org.omnifaces.cdi.beans.InjectionPointGenerator;

/**
 * <p>
 * Collection of utility methods for the CDI API that are mainly shortcuts for obtaining stuff from the
 * {@link BeanManager}.
 * <p>
 * The difference with {@link Beans} is that no one method of {@link BeansLocal} obtains the {@link BeanManager} from
 * JNDI. This job is up to the caller. This is more efficient in situations where multiple utility methods needs to be
 * called at the same time.
 *
 * @author Bauke Scholtz
 * @since 2.0
 */
@Typed
public final class BeansLocal {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final Logger logger = Logger.getLogger(BeansLocal.class.getName());

	// Constructors ---------------------------------------------------------------------------------------------------

	private BeansLocal() {
		// Hide constructor.
	}

	// Utility --------------------------------------------------------------------------------------------------------

	/**
	 * @see Beans#resolve(Class, Annotation...)
	 */
	@SuppressWarnings("unchecked")
	public static <T> Bean<T> resolve(BeanManager beanManager, Class<T> beanClass, Annotation... qualifiers) {
		Set<Bean<?>> beans = beanManager.getBeans(beanClass, qualifiers);

		for (Bean<?> bean : beans) {
			if (bean.getBeanClass() == beanClass) {
				return (Bean<T>) beanManager.resolve(Collections.<Bean<?>>singleton(bean));
			}
		}

		return (Bean<T>) beanManager.resolve(beans);
	}

	/**
	 * @see Beans#resolveExact(Class, Annotation...)
	 */
	public static <T> Bean<T> resolveExact(BeanManager beanManager, Class<T> beanClass, Annotation... qualifiers) {
		Bean<T> bean = resolve(beanManager, beanClass, qualifiers);
		return (bean != null) && (bean.getBeanClass() == beanClass) ? bean : null;
	}

	/**
	 * @see Beans#getReference(Class, Annotation...)
	 */
	public static <T> T getReference(BeanManager beanManager, Class<T> beanClass, Annotation... qualifiers) {
		Bean<T> bean = resolve(beanManager, beanClass, qualifiers);
		return (bean != null) ? getReference(beanManager, bean, beanClass) : null;
	}

	/**
	 * @see Beans#getReference(Bean)
	 */
	public static <T> T getReference(BeanManager beanManager, Bean<T> bean) {
		return getReference(beanManager, bean, bean.getBeanClass());
	}

	@SuppressWarnings("unchecked")
	private static <T> T getReference(BeanManager beanManager, Bean<T> bean, Class<?> beanClass) {
		return (T) beanManager.getReference(bean, beanClass, beanManager.createCreationalContext(bean));
	}

	/**
	 * @see Beans#getInstance(Class, Annotation...)
	 */
	public static <T> T getInstance(BeanManager beanManager, Class<T> beanClass, Annotation... qualifiers) {
		return getInstance(beanManager, beanClass, true, qualifiers);
	}

	/**
	 * @see Beans#getInstance(Class, boolean, Annotation...)
	 */
	public static <T> T getInstance(BeanManager beanManager, Class<T> beanClass, boolean create, Annotation... qualifiers) {
		Bean<T> bean = resolve(beanManager, beanClass, qualifiers);
		return (bean != null) ? getInstance(beanManager, bean, create) : null;
	}

	/**
	 * @see Beans#getInstance(Bean, boolean)
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
	 * @see Beans#unwrapIfNecessary(Object)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T unwrapIfNecessary(BeanManager beanManager, T object) {
		if (object == null) {
			return null;
		}

		if (!isProxy(object)) {
			return object;
		}

		if (object instanceof Class) {
			return (T) ((Class<?>) object).getSuperclass();
		}
		else {
			return (T) getInstance(beanManager, object.getClass().getSuperclass());
		}
	}

	/**
	 * @see Beans#isActive(Class)
	 */
	public static <S extends Annotation> boolean isActive(BeanManager beanManager, Class<S> scope) {
		try {
			return beanManager.getContext(scope).isActive();
		}
		catch (Exception ignore) {
			logger.log(FINEST, "Ignoring thrown exception; given scope is very unlikely active anyway.", ignore);
			return false;
		}
	}

	/**
	 * @see Beans#getActiveInstances(Class)
	 */
	public static <S extends Annotation> Map<Object, String> getActiveInstances(BeanManager beanManager, Class<S> scope) {
		Map<Object, String> activeInstances = new HashMap<>();
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

	/**
	 * @see Beans#destroy(Class, Annotation...)
	 */
	public static <T> void destroy(BeanManager beanManager, Class<T> beanClass, Annotation... qualifiers) {
		Bean<T> bean = resolve(beanManager, beanClass, qualifiers);

		if (bean != null) {
			destroy(beanManager, bean);
		}
	}

	/**
	 * @see Beans#destroy(Bean)
	 */
	public static <T> void destroy(BeanManager beanManager, Bean<T> bean) {
		Context context = beanManager.getContext(bean.getScope());

		if (context instanceof AlterableContext) {
			((AlterableContext) context).destroy(bean);
		}
		else {
			T instance = context.get(bean);

			if (instance != null) {
				destroy(beanManager, bean, instance);
			}
		}
	}

	/**
	 * @see Beans#destroy(Object)
	 */
	@SuppressWarnings("unchecked")
	public static <T> void destroy(BeanManager beanManager, T instance) {
		if (instance instanceof Class) { // Java prefers T over Class<T> when varargs is not specified :(
			destroy(beanManager, (Class<T>) instance, new Annotation[0]);
		}
		else {
			for (Class<?> beanClass = instance.getClass(); beanClass != Object.class; beanClass = beanClass.getSuperclass()) {
				Bean<T> bean = (Bean<T>) resolve(beanManager, beanClass);

				if (bean != null) {
					destroy(beanManager, bean, instance);
					return;
				}
			}
		}
	}

	private static <T> void destroy(BeanManager beanManager, Bean<T> bean, T instance) {
		bean.destroy(instance, beanManager.createCreationalContext(bean));
	}

	/**
	 * @see Beans#getAnnotation(Annotated, Class)
	 */
	public static <A extends Annotation> A getAnnotation(BeanManager beanManager, Annotated annotated, Class<A> annotationType) {

		annotated.getAnnotation(annotationType);

		if (annotated.getAnnotations().isEmpty()) {
			return null;
		}

		if (annotated.isAnnotationPresent(annotationType)) {
			return annotated.getAnnotation(annotationType);
		}

		Queue<Annotation> annotations = new LinkedList<>(annotated.getAnnotations());

		while (!annotations.isEmpty()) {
			Annotation annotation = annotations.remove();

			if (annotation.annotationType().equals(annotationType)) {
				return annotationType.cast(annotation);
			}

			if (beanManager.isStereotype(annotation.annotationType())) {
				annotations.addAll(beanManager.getStereotypeDefinition(annotation.annotationType()));
			}
		}

		return null;
	}

	/**
	 * @see Beans#getAnnotation(Annotated, Class)
	 */
	public static InjectionPoint getCurrentInjectionPoint(BeanManager beanManager, CreationalContext<?> creationalContext) {
		Bean<InjectionPointGenerator> bean = resolve(beanManager, InjectionPointGenerator.class);
		return (bean != null) ? (InjectionPoint) beanManager.getInjectableReference(bean.getInjectionPoints().iterator().next(), creationalContext) : null;
	}

	/**
	 * @see Beans#fireEvent(Object, Annotation...)
	 */
	public static void fireEvent(BeanManager beanManager, Object event, Annotation... qualifiers) {
		beanManager.fireEvent(event, qualifiers);
	}

}