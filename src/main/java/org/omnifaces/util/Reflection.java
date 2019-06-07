/*
 * Copyright 2018 OmniFaces
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

import static java.beans.Introspector.getBeanInfo;
import static java.beans.PropertyEditorManager.findEditor;
import static java.lang.String.format;
import static java.util.logging.Level.FINE;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.enterprise.inject.Typed;

/**
 * Collection of utility methods for working with reflection.
 *
 * @since 2.0
 * @author Arjan Tijms
 *
 */
@Typed
public final class Reflection {

	private static final Logger logger = Logger.getLogger(Reflection.class.getName());

	private static final String ERROR_LOAD_CLASS = "Cannot load class '%s'.";
	private static final String ERROR_CREATE_INSTANCE = "Cannot create instance of class '%s'.";
	private static final String ERROR_ACCESS_FIELD = "Cannot access field '%s' of class '%s'.";
	private static final String ERROR_INVOKE_METHOD = "Cannot invoke method '%s' of class '%s' with arguments %s.";

	private Reflection() {
		// Hide constructor.
	}

	/**
	 * Sets a collection of properties of a given object to the values associated with those properties.
	 * <p>
	 * In the map that represents these properties, each key represents the name of the property, with the value
	 * associated with that key being the value that is set for the property.
	 * <p>
	 * E.g. map entry key = foo, value = "bar", which "bar" an instance of String, will conceptually result in the
	 * following call: <code>object.setFoo("string");</code>
	 *
	 * <p>
	 * NOTE: This particular method assumes that there's a write method for each property in the map with the right
	 * type. No specific checking is done whether this is indeed the case.
	 *
	 * @param object
	 *            the object on which properties will be set
	 * @param propertiesToSet
	 *            the map containing properties and their values to be set on the object
	 */
	public static void setProperties(Object object, Map<String, Object> propertiesToSet) {

		try {
			Map<String, PropertyDescriptor> availableProperties = new HashMap<>();
			for (PropertyDescriptor propertyDescriptor : getBeanInfo(object.getClass()).getPropertyDescriptors()) {
				availableProperties.put(propertyDescriptor.getName(), propertyDescriptor);
			}

			for (Entry<String, Object> propertyToSet : propertiesToSet.entrySet()) {
				availableProperties.get(propertyToSet.getKey()).getWriteMethod().invoke(object, propertyToSet.getValue());
			}

		} catch (IntrospectionException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Sets a collection of properties of a given object to the (optionally coerced) values associated with those properties.
	 * <p>
	 * In the map that represents these properties, each key represents the name of the property, with the value
	 * associated with that key being the value that is set for the property.
	 * <p>
	 * E.g. map entry key = foo, value = "bar", which "bar" an instance of String, will conceptually result in the
	 * following call: <code>object.setFoo("string");</code>
	 *
	 * <p>
	 * NOTE 1: In case the value is a String, and the target type is not String, the standard property editor mechanism
	 * will be used to attempt a conversion.
	 *
	 * <p>
	 * Note 2: This method operates somewhat as the reverse of {@link Reflection#setProperties(Object, Map)}. Here only
	 * the available writable properties of the object are matched against the map with properties to set. Properties
	 * in the map for which there isn't a corresponding writable property on the object are ignored.
	 *
	 * <p>
	 * Following the above two notes, use this method when attempting to set properties on an object in a lenient best effort
	 * basis. Use {@link Reflection#setProperties(Object, Map)} when all properties need to be set with the exact type as the value
	 * appears in the map.
	 *
	 *
	 * @param object
	 *            the object on which properties will be set
	 * @param propertiesToSet
	 *            the map containing properties and their values to be set on the object
	 */
	public static void setPropertiesWithCoercion(Object object, Map<String, Object> propertiesToSet) {
		try {
			for (PropertyDescriptor property : getBeanInfo(object.getClass()).getPropertyDescriptors()) {
				Method setter = property.getWriteMethod();

				if (setter == null || !propertiesToSet.containsKey(property.getName())) {
					continue;
				}

				Object value = propertiesToSet.get(property.getName());
				if (value instanceof String && !property.getPropertyType().equals(String.class)) {

					// Try to convert Strings to the type expected by the converter

					PropertyEditor editor = findEditor(property.getPropertyType());
					editor.setAsText((String) value);
					value = editor.getValue();
				}

				property.getWriteMethod().invoke(object, value);
			}
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Finds a method based on the method name, amount of parameters and limited typing and returns <code>null</code>
	 * is none is found.
	 * <p>
	 * Note that this supports overloading, but a limited one. Given an actual parameter of type Long, this will select
	 * a method accepting Number when the choice is between Number and a non-compatible type like String. However,
	 * it will NOT select the best match if the choice is between Number and Long.
	 *
	 * @param base the object in which the method is to be found
	 * @param methodName name of the method to be found
	 * @param params the method parameters
	 * @return a method if one is found, null otherwise
	 */
	public static Method findMethod(Object base, String methodName, Object... params) {

		List<Method> methods = new ArrayList<>();

		for (Class<?> cls = base.getClass(); cls != null; cls = cls.getSuperclass()) {
			search: for (Method method : cls.getDeclaredMethods()) {
				if (method.getName().equals(methodName) && method.getParameterTypes().length == params.length) {
					for (Method added : methods) {
						if (Arrays.equals(added.getParameterTypes(), method.getParameterTypes())) {
							continue search; // Ignore overridden method from superclass.
						}
					}

					methods.add(method);
				}
			}
		}

		if (methods.size() == 1) {
			return methods.get(0);
		}
		else {
			return closestMatchingMethod(methods, params);  // Overloaded methods were found. Try to find closest match.
		}
	}

	private static Method closestMatchingMethod(List<Method> methods, Object... params) {
		for (Method method : methods) {
			Class<?>[] candidateParamTypes = method.getParameterTypes();
			boolean match = true;

			for (int i = 0; i < params.length; i++) {
				Object param = params[i];
				Class<?> paramType = param != null ? param.getClass() : null;
				Class<?> candidateParamType = candidateParamTypes[i];

				if (paramType != null && candidateParamType.isPrimitive()) {
					paramType = getPrimitiveType(paramType);
				}

				if (paramType == null ? !candidateParamType.isPrimitive() : !candidateParamType.isAssignableFrom(paramType)) {
					match = false;
					break;
				}
			}

			// If all candidate parameters were expected and for none of them the actual parameter was NOT an instance, we have a match.
			if (match) {
				return method;
			}

			// Else, at least one parameter was not an instance. Go ahead a test then next methods.
		}

		return null;
	}

	/**
	 * Returns the primitive type of the given type, if any.
	 */
	private static Class<?> getPrimitiveType(Class<?> type)
	{
		return type.isPrimitive() ? type
			: type == Boolean.class ? boolean.class
			: type == Byte.class ? byte.class
			: type == Short.class ? int.class
			: type == Character.class ? char.class
			: type == Integer.class ? int.class
			: type == Long.class ? long.class
			: type == Float.class ? float.class
			: type == Double.class ? double.class
			: null;
	}

	/**
	 * Returns the class object associated with the given class name, using the context class loader and if
	 * that fails the defining class loader of the current class.
	 * @param <T> The expected class type.
	 * @param className Fully qualified class name of the class for which a class object needs to be created.
	 * @return The class object associated with the given class name.
	 * @throws IllegalStateException If the class cannot be loaded.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Class<T> toClass(String className) {
		try {
			return (Class<T>) (Class.forName(className, true, Thread.currentThread().getContextClassLoader()));
		}
		catch (Exception e) {
			try {
				return (Class<T>) Class.forName(className);
			}
			catch (Exception ignore) {
				logger.log(FINE, "Ignoring thrown exception; previous exception will be rethrown instead.", ignore);
				// Just continue to IllegalStateException on original ClassNotFoundException.
			}

			throw new IllegalStateException(format(ERROR_LOAD_CLASS, className), e);
		}
	}

	/**
	 * Returns the class object associated with the given class name, using the context class loader and if
	 * that fails the defining class loader of the current class. If the class cannot be loaded, then return null
	 * instead of throwing illegal state exception.
	 * @param <T> The expected class type.
	 * @param className Fully qualified class name of the class for which a class object needs to be created.
	 * @return The class object associated with the given class name.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @since 2.5
	 */
	public static <T> Class<T> toClassOrNull(String className) {
		try {
			return toClass(className);
		}
		catch (Exception ignore) {
			logger.log(FINE, "Ignoring thrown exception; the sole intent is to return null instead.", ignore);
			return null;
		}
	}

	/**
	 * Finds a constructor based on the given parameter types and returns <code>null</code> is none is found.
	 * @param clazz The class object for which the constructor is to be found.
	 * @param parameterTypes The desired method parameter types.
	 * @return A constructor if one is found, null otherwise.
	 * @since 2.6
	 */
	public static <T> Constructor<T> findConstructor(Class<T> clazz, Class<?>... parameterTypes) {
		try {
			return clazz.getConstructor(parameterTypes);
		}
		catch (Exception ignore) {
			logger.log(FINE, "Ignoring thrown exception; the sole intent is to return null instead.", ignore);
			return null;
		}
	}

	/**
	 * Returns a new instance of the given class name using the default constructor.
	 * @param <T> The expected return type.
	 * @param className Fully qualified class name of the class for which an instance needs to be created.
	 * @return A new instance of the given class name using the default constructor.
	 * @throws IllegalStateException If the class cannot be loaded.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T instance(String className) {
		return (T) instance(toClass(className));
	}

	/**
	 * Returns a new instance of the given class object using the default constructor.
	 * @param <T> The generic object type.
	 * @param clazz The class object for which an instance needs to be created.
	 * @return A new instance of the given class object using the default constructor.
	 * @throws IllegalStateException If the class cannot be found, or cannot be instantiated, or when a security manager
	 * prevents this operation.
	 */
	public static <T> T instance(Class<T> clazz) {
		try {
			return clazz.newInstance();
		}
		catch (Exception e) {
			throw new IllegalStateException(format(ERROR_CREATE_INSTANCE, clazz), e);
		}
	}

	/**
	 * Returns the value of the field of the given instance on the given field name.
	 * @param <T> The expected return type.
	 * @param instance The instance to access the given field on.
	 * @param fieldName The name of the field to be accessed on the given instance.
	 * @return The value of the field of the given instance on the given field name.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @throws IllegalStateException If the field cannot be accessed.
	 * @since 2.5
	 */
	@SuppressWarnings("unchecked")
	public static <T> T accessField(Object instance, String fieldName) {
		try {
			Field field = instance.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			return (T) field.get(instance);
		}
		catch (Exception e) {
			throw new IllegalStateException(format(ERROR_ACCESS_FIELD, fieldName, instance.getClass()), e);
		}
	}

	/**
	 * Invoke a method of the given instance on the given method name with the given parameters and return the result.
	 * <p>
	 * Note: the current implementation assumes for simplicity that no one of the given parameters is null. If one of
	 * them is still null, a NullPointerException will be thrown.
	 * @param <T> The expected return type.
	 * @param instance The instance to invoke the given method on.
	 * @param methodName The name of the method to be invoked on the given instance.
	 * @param parameters The method parameters, if any.
	 * @return The result of the method invocation, if any.
	 * @throws NullPointerException When one of the given parameters is null.
	 * @throws IllegalStateException If the method cannot be invoked.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @since 2.5
	 */
	@SuppressWarnings("unchecked")
	public static <T> T invokeMethod(Object instance, String methodName, Object... parameters) {
		try {
			Method method = findMethod(instance, methodName, parameters);
			method.setAccessible(true);
			return (T) method.invoke(instance, parameters);
		}
		catch (Exception e) {
			throw new IllegalStateException(
				format(ERROR_INVOKE_METHOD, methodName, instance.getClass(), Arrays.toString(parameters)), e);
		}
	}

}