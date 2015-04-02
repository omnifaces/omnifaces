/*
 * Copyright 2014 OmniFaces.
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

import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Collection of utility methods for working with reflection.
 *
 * @since 2.0
 * @author Arjan Tijms
 *
 */
public final class Reflection {

	private Reflection() {
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
			Map<String, PropertyDescriptor> availableProperties = new HashMap<String, PropertyDescriptor>();
			for (PropertyDescriptor propertyDescriptor : getBeanInfo(object.getClass()).getPropertyDescriptors()) {
				availableProperties.put(propertyDescriptor.getName(), propertyDescriptor);
			}

			for (Entry<String, Object> propertyToSet : propertiesToSet.entrySet()) {
				availableProperties.get(propertyToSet.getKey()).getWriteMethod().invoke(object, propertyToSet.getValue());
			}

		} catch (Exception e) {
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

				if (setter == null) {
					continue;
				}

				if (propertiesToSet.containsKey(property.getName())) {

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
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Finds a method based on the method name, amount of parameters and limited typing, if necessary prefixed with "get".
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
	public static Method findMethod(Object base, String methodName, Object[] params) {

		List<Method> methods = new ArrayList<Method>();
		for (Method method : base.getClass().getMethods()) {
			if (method.getName().equals(methodName) && method.getParameterTypes().length == params.length) {
				methods.add(method);
			}
		}

		if (methods.size() == 1) {
			return methods.get(0);
		}

		if (methods.size() > 1) {
			// Overloaded methods were found. Try to get a match
			for (Method method : methods) {
				boolean match = true;
				Class<?>[] candidateParams = method.getParameterTypes();
				for (int i = 0; i < params.length; i++) {
					if (!candidateParams[i].isInstance(params[i])) {
						match = false;
						break;
					}
				}

				// If all candidate parameters were expected and for none of them the actual
				// parameter was NOT an instance, we have a match
				if (match) {
					return method;
				}

				// Else, at least one parameter was not an instance
				// Go ahead a test then next methods
			}
		}

		return null;
	}

	/**
	 * Returns the Class instance associated with the class of the given string, using the context class
	 * loader and if that fails the defining class loader of the current class.
	 *
	 * @param className fully qualified class name of the class for which a Class instance needs to be created
	 * @return the Class object for the class with the given name.
	 * @throws IllegalStateException if the class cannot be found.
	 */
	public static Class<?> toClass(String className) {
		try {
			return (Class.forName(className, true, Thread.currentThread().getContextClassLoader()));
		}
		catch (ClassNotFoundException e) {
			try {
				return Class.forName(className);
			}
			catch (Exception ignore) {
				ignore = null; // Just continue to IllegalStateException on original ClassNotFoundException.
			}

			throw new IllegalStateException(e);
		}
	}

	/**
	 * Creates an instance of a class with the given fully qualified class name.
	 *
	 * @param <T> The generic object type.
	 * @param className fully qualified class name of the class for which an instance needs to be created
	 * @return an instance of the class denoted by className
	 * @throws IllegalStateException if the class cannot be found
	 */
	@SuppressWarnings("unchecked")
	public static <T> T instance(String className) {
		return (T) instance(toClass(className));
	}

	/**
	 * Creates a new instance of the class represented by the given Class object
	 *
	 * @param <T> The generic object type.
	 * @param clazz the Class object for which an instance needs to be created
	 * @return an instance of the class as given by the clazz parameter
	 * @throws IllegalStateException if the class cannot be found, or cannot be instantiated or when a security manager prevents this operation
	 */
	public static <T> T instance(Class<T> clazz) {
		try {
			return clazz.newInstance();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

}
