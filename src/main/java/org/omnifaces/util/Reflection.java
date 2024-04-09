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

import static java.beans.PropertyEditorManager.findEditor;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableList;
import static java.util.Comparator.reverseOrder;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.logging.Level.FINEST;
import static org.omnifaces.util.Beans.unwrapIfNecessary;
import static org.omnifaces.util.Utils.getPrimitiveType;
import static org.omnifaces.util.Utils.isEmpty;
import static org.omnifaces.util.Utils.isOneInstanceOf;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.enterprise.inject.Typed;

/**
 * Collection of utility methods for working with reflection.
 *
 * @author Arjan Tijms
 * @author Bauke Scholtz
 * @author Andre Wachsmuth
 * @since 2.0
 */
@Typed
public final class Reflection {

	private static final Logger logger = Logger.getLogger(Reflection.class.getName());

	private static final String ERROR_LOAD_CLASS = "Cannot load class '%s'.";
	private static final String ERROR_CREATE_INSTANCE = "Cannot create instance of class '%s'.";
	private static final String ERROR_ACCESS_FIELD = "Cannot access field '%s' of class '%s'.";
	private static final String ERROR_MODIFY_FIELD = "Cannot modify field '%s' of class '%s' with value %s.";
	private static final String ERROR_INVOKE_METHOD = "Cannot invoke method '%s' of class '%s' with arguments %s.";


	// Nested classes -------------------------------------------------------------------------------------------------

	/**
	 * This class represents a property path. This is intended to be immutable.
	 * This is primarily used in {@link Reflection#getBaseBeanPropertyPaths(Object)} and {@link Reflection#setBeanProperties(Object, Map)}.
	 *
	 * @author Bauke Scholtz
	 * @since 3.8
	 */
	public static final class PropertyPath implements Comparable<PropertyPath>, Serializable {

		private static final long serialVersionUID = 1L;

		private final List<Comparable<? extends Serializable>> nodes;

		private PropertyPath(List<Comparable<? extends Serializable>> nodes) {
			this.nodes = nodes;
		}

		/**
		 * Create a new property path composed of given nodes.
		 * @param nodes Nodes of property path.
		 * @return A new property path composed of given nodes.
		 * @throws NullPointerException When one of the nodes is null.
		 * @throws IllegalArgumentException When one of the nodes is actually not an instance of Serializable.
		 */
		@SafeVarargs
		public static PropertyPath of(Comparable<? extends Serializable>... nodes) {
			for (Comparable<? extends Serializable> node : nodes) {
				requireNonNull(node, "node");

				if (!(node instanceof Serializable)) {
					throw new IllegalArgumentException("Node " + node + " (" + node.getClass() + ") must be an instance of Serializable."); // For @SafeVarargs.
				}
			}

			return new PropertyPath(asList(nodes));
		}

		/**
		 * Create a new property path composed of the nodes of the current property path with the given node added.
		 * E.g. if the current property path is "person" and the given node is "name", then this returns a new property
		 * path representing "person.name". Or, if the current property path is "list" and the given node is "0", then
		 * this returns a new property path representing "list[0]". Or, if the current property path is "persons[0]"
		 * and the given node is "name", then this returns a new property path representing "persons[0].name"
		 * @param node Node to extend the current property path with.
		 * @return A new property path composed of the nodes of the current property path added with the given node.
		 * @throws NullPointerException When node is null.
		 */
		public PropertyPath with(Comparable<? extends Serializable> node) {
			requireNonNull(node, "node");
			List<Comparable<? extends Serializable>> newNodes = new ArrayList<>(this.nodes);
			newNodes.add(node);
			return new PropertyPath(unmodifiableList(newNodes));
		}

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public int compareTo(PropertyPath other) {
			if (this.nodes.size() != other.nodes.size()) {
				return this.nodes.size() < other.nodes.size() ? -1 : 1;
			}

			for (int index = 0; index < this.nodes.size(); index++) {
				Comparable thisNode = this.nodes.get(index);
				Comparable otherNode = other.nodes.get(index);

				if (!(thisNode.getClass().isInstance(otherNode) && otherNode.getClass().isInstance(thisNode))) {
					thisNode = thisNode.toString();
					otherNode = otherNode.toString();
				}

				int compare = thisNode.compareTo(otherNode);

				if (compare != 0) {
					return compare;
				}
			}

			return 0;
		}

		@Override
		public boolean equals(Object object) {
			return object == this || (object instanceof PropertyPath && ((PropertyPath) object).nodes.equals(this.nodes));
		}

		@Override
		public int hashCode() {
			return nodes.hashCode();
		}

		/**
		 * Returns the property path as string, conform the EL rules.
		 */
		@Override
		public String toString() {
			StringBuilder stringBuilder = new StringBuilder();

			for (Comparable<?> node : nodes) {
				if (node instanceof String) {
					if (stringBuilder.length() > 0) {
						stringBuilder.append('.');
					}

					stringBuilder.append(node);
				}
				else {
					stringBuilder.append('[').append(node).append(']');
				}
			}

			return stringBuilder.toString();
		}
	}

	// Constructors ---------------------------------------------------------------------------------------------------

	private Reflection() {
		// Hide constructor.
	}


	// Beans ----------------------------------------------------------------------------------------------------------

	/**
	 * Sets a collection of properties of a given bean to the values associated with those properties.
	 * <p>
	 * In the map that represents these properties, each key represents the name of the property, with the value
	 * associated with that key being the value that is set for the property.
	 * <p>
	 * E.g. map entry key = foo, value = "bar", which "bar" an instance of String, will conceptually result in the
	 * following call: <code>bean.setFoo("string");</code>
	 *
	 * <p>
	 * NOTE: This particular method assumes that there's a write method for each property in the map with the right
	 * type. No specific checking is done whether this is indeed the case.
	 *
	 * <p>
	 * If you need to set nested properties recursively as well, use {@link #setBeanProperties(Object, Map)} instead.
	 *
	 * @param bean
	 *            the bean on which properties will be set
	 * @param propertiesToSet
	 *            the map containing properties and their values to be set on the bean
	 */
	public static void setProperties(Object bean, Map<String, Object> propertiesToSet) {
		Map<String, PropertyDescriptor> availableProperties = getPropertyDescriptors(bean.getClass());

		for (Entry<String, Object> propertyToSet : propertiesToSet.entrySet()) {
			setBeanProperty(bean, propertyToSet.getValue(), availableProperties.get(propertyToSet.getKey()));
		}
	}

	/**
	 * Sets a collection of properties of a given bean to the (optionally coerced) values associated with those properties.
	 * <p>
	 * In the map that represents these properties, each key represents the name of the property, with the value
	 * associated with that key being the value that is set for the property.
	 * <p>
	 * E.g. map entry key = foo, value = "bar", which "bar" an instance of String, will conceptually result in the
	 * following call: <code>bean.setFoo("string");</code>
	 *
	 * <p>
	 * NOTE 1: In case the value is a String, and the target type is not String, the standard property editor mechanism
	 * will be used to attempt a conversion.
	 *
	 * <p>
	 * Note 2: This method operates somewhat as the reverse of {@link Reflection#setProperties(Object, Map)}. Here only
	 * the available writable properties of the bean are matched against the map with properties to set. Properties
	 * in the map for which there isn't a corresponding writable property on the bean are ignored.
	 *
	 * <p>
	 * Following the above two notes, use this method when attempting to set properties on an bean in a lenient best effort
	 * basis. Use {@link Reflection#setProperties(Object, Map)} when all properties need to be set with the exact type as the value
	 * appears in the map.
	 *
	 *
	 * @param bean
	 *            the bean on which properties will be set
	 * @param propertiesToSet
	 *            the map containing properties and their values to be set on the object
	 */
	public static void setPropertiesWithCoercion(Object bean, Map<String, Object> propertiesToSet) {
		for (PropertyDescriptor property : getPropertyDescriptors(bean.getClass()).values()) {
			Method setter = property.getWriteMethod();

			if (setter == null || !propertiesToSet.containsKey(property.getName())) {
				continue;
			}

			Object value = propertiesToSet.get(property.getName());

			if (value instanceof String && !property.getPropertyType().equals(String.class)) {
				try {
					// Try to convert Strings to the type expected by the converter
					PropertyEditor editor = findEditor(property.getPropertyType());
					editor.setAsText((String) value);
					value = editor.getValue();
				}
				catch (Exception e) {
					throw new IllegalStateException(e);
				}
			}

			setBeanProperty(bean, value, property);
		}
	}

	/**
	 * Recursively set given properties on given bean.
	 * It will automatically prepopulate nested lists, maps, arrays and beans where necessary.
	 * @param bean Bean to recursively set properties on.
	 * @param properties Properties to recursively set on bean. The map key represents the property path and the map value represents the property value.
	 * @since 3.8
	 */
	public static void setBeanProperties(Object bean, Map<PropertyPath, Object> properties) {
		Map<Class<?>, Map<String, PropertyDescriptor>> cachedDescriptors = new HashMap<>();
		Map<PropertyPath, Object> sortedProperties = new TreeMap<>(reverseOrder()); // Reverse order ensures that e.g. "list[4].property" comes before e.g. "list[0].property", so that the code knows how many items to prepopulate.
		sortedProperties.putAll(properties);

		for (Entry<PropertyPath, Object> entry : sortedProperties.entrySet()) {
			PropertyPath path = entry.getKey();

			if (!path.nodes.isEmpty()) {
				Object base = getBase(bean, path, cachedDescriptors);
				setProperty(base, path.nodes.get(path.nodes.size() - 1), entry.getValue(), cachedDescriptors);
			}
		}
	}

	private static Object getBase(Object bean, PropertyPath path, Map<Class<?>, Map<String, PropertyDescriptor>> cachedDescriptors) {
		Object base = bean;

		for (int index = 0; index < path.nodes.size() - 1; index++) {
			Comparable<?> node = path.nodes.get(index);

			if (base instanceof List) {
				base = ((List<?>) base).get((Integer) node);
			}
			else if (base instanceof Map) {
				base = ((Map<?, ?>) base).get(node);
			}
			else if (base.getClass().isArray()) {
				base = Array.get(base, (Integer) node);
			}
			else {
				base = getBeanProperty(base, (String) node, cachedDescriptors, path.nodes.get(index + 1));
			}
		}

		return base;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void setProperty(Object base, Comparable<?> property, Object value, Map<Class<?>, Map<String, PropertyDescriptor>> cachedDescriptors) {
		if (base == null) {
			return;
		}

		if (base instanceof List) {
			((List) base).set((Integer) property, value);
		}
		else if (base instanceof Map) {
			((Map) base).put(property, value);
		}
		else if (base.getClass().isArray()) {
			Array.set(base, (Integer) property, value);
		}
		else {
			setBeanProperty(base, value, getPropertyDescriptor(base.getClass(), (String) property, cachedDescriptors));
		}
	}

	private static void setBeanProperty(Object bean, Object value, PropertyDescriptor propertyDescriptor) {
		try {
			propertyDescriptor.getWriteMethod().invoke(bean, value);
		}
		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Obtain given property from given bean.
	 * @param bean Bean to obtain property from.
	 * @param property Property name.
	 * @return Value of given property of given bean.
	 * @since 3.8
	 */
	public static Object getBeanProperty(Object bean, String property) {
		return getBeanProperty(bean, property, new HashMap<>(), null);
	}

	private static Object getBeanProperty(Object bean, PropertyDescriptor propertyDescriptor) {
		try {
			return propertyDescriptor.getReadMethod().invoke(bean);
		}
		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
	}

	private static Object getBeanProperty(Object bean, String property, Map<Class<?>, Map<String, PropertyDescriptor>> cachedDescriptors, Comparable<?> nextPropertyNode) {
		PropertyDescriptor propertyDescriptor = getPropertyDescriptor(bean.getClass(), property, cachedDescriptors);
		Object value = getBeanProperty(bean, propertyDescriptor);

		if (isEmpty(value) && nextPropertyNode != null) {
			value = setBeanPropertyWithDefaultValue(bean, propertyDescriptor, nextPropertyNode);
		}

		return value;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object setBeanPropertyWithDefaultValue(Object bean, PropertyDescriptor propertyDescriptor, Comparable<?> nextPropertyNode) {
		Class<?> type = propertyDescriptor.getPropertyType();
		Object value;

		if (List.class.isAssignableFrom(type)) {
			value = new ArrayList<>();
			Class<?> elementType = (Class<?>) ((ParameterizedType) propertyDescriptor.getReadMethod().getGenericReturnType()).getActualTypeArguments()[0];
			Integer size = ((Integer) nextPropertyNode) + 1;

			for (int index = 0; index < size; index++) {
				((List) value).add(createDefaultValueIfNecessary(elementType));
			}
		}
		else if (Map.class.isAssignableFrom(type)) {
			value = new LinkedHashMap<>();
		}
		else if (type.isArray()) {
			Integer length = ((Integer) nextPropertyNode) + 1;
			value = Array.newInstance(type.getComponentType(), length);

			for (int index = 0; index < length; index++) {
				Array.set(value, index, createDefaultValueIfNecessary(type.getComponentType()));
			}
		}
		else {
			value = instance(type);
		}

		if (propertyDescriptor.getWriteMethod() != null) {
			setBeanProperty(bean, value, propertyDescriptor);
		}
		else {
			modifyField(bean, propertyDescriptor.getName(), value);
		}

		return value;
	}

	private static Object createDefaultValueIfNecessary(Class<?> type) {
		return isNeedsFurtherRecursion(type) ? instance(type) : null;
	}

	private static Map<String, PropertyDescriptor> getPropertyDescriptors(Class<?> type, Map<Class<?>, Map<String, PropertyDescriptor>> cachedDescriptors) {
		return cachedDescriptors.computeIfAbsent(type, Reflection::getPropertyDescriptors);
	}

	private static Map<String, PropertyDescriptor> getPropertyDescriptors(Class<?> type) {
		try {
			return stream(Introspector.getBeanInfo(type).getPropertyDescriptors())
				.filter(propertyDescriptor -> propertyDescriptor.getReadMethod() != null)
				.collect(Collectors.toMap(PropertyDescriptor::getName, identity()));
		}
		catch (IntrospectionException e) {
			throw new IllegalStateException(e);
		}
	}

	private static PropertyDescriptor getPropertyDescriptor(Class<?> type, String property, Map<Class<?>, Map<String, PropertyDescriptor>> cachedDescriptors) {
		return getPropertyDescriptors(type, cachedDescriptors).get(property);
	}

	/**
	 * Recursively collect all base bean property paths from the given bean which resolve to non-null bases. A "base" is
	 * represented by the bean itself and all of its nested lists, maps, arrays and beans. This does not include the
	 * non-nested properties of any base. E.g. "person.address.street" will return a map with actual instances of
	 * "person" and "person.address" as keys. Note that the "street" is not included as it does not represent a base.
	 * @param bean The given bean.
	 * @return All base bean property paths which resolve to non-null values, mapped by the base.
	 * @since 3.8
	 */
	public static Map<Object, PropertyPath> getBaseBeanPropertyPaths(Object bean) {
		return getBaseBeanPropertyPaths(bean, recursableGetter -> true);
	}

	/**
	 * Recursively collect all base bean property paths from the given bean which resolve to non-null bases and are
	 * recursable. A "base" is represented by the bean itself and all of its nested lists, maps, arrays and beans. This
	 * does not include the non-nested properties of any base. E.g. "person.address.street" will return a map with
	 * actual instances of "person" and "person.address" as keys. Note that the "street" is not included as it does not
	 * represent a base.
	 * @param bean The given bean.
	 * @param recursableGetter Whether the given getter method is recursable.
	 * @return All base bean property paths which resolve to non-null values, mapped by the base.
	 * @since 3.9
	 */
	public static Map<Object, PropertyPath> getBaseBeanPropertyPaths(Object bean, Predicate<Method> recursableGetter) {
		Map<Class<?>, Map<String, PropertyDescriptor>> cachedDescriptors = new HashMap<>();
		Map<Object, PropertyPath> collectedBasePropertyPaths = new IdentityHashMap<>();
		PropertyPath basePath = PropertyPath.of();
		collectedBasePropertyPaths.put(bean, basePath);
		collectBasePropertyPaths(bean, basePath, recursableGetter, cachedDescriptors, collectedBasePropertyPaths);
		return collectedBasePropertyPaths;
	}

	private static void collectBasePropertyPaths(Object base, PropertyPath basePath, Predicate<Method> recursableGetter, Map<Class<?>, Map<String, PropertyDescriptor>> cachedDescriptors, Map<Object, PropertyPath> collectedBasePropertyPaths) {
		if (base == null) {
			return;
		}
		else if (base instanceof List) {
			collectBasePropertyPathsFromList((List<?>) base, basePath, recursableGetter, cachedDescriptors, collectedBasePropertyPaths);
		}
		else if (base instanceof Map) {
			collectBasePropertyPathsFromMap((Map<?, ?>) base, basePath, recursableGetter, cachedDescriptors, collectedBasePropertyPaths);
		}
		else if (base.getClass().isArray()) {
			collectBasePropertyPathsFromArray((Object[]) base, basePath, recursableGetter, cachedDescriptors, collectedBasePropertyPaths);
		}
		else {
			collectBasePropertyPathsFromBean(unwrapIfNecessary(base), basePath, recursableGetter, cachedDescriptors, collectedBasePropertyPaths);
		}
	}

	private static void collectBasePropertyPathsFromList(List<?> list, PropertyPath basePath, Predicate<Method> recursableGetter, Map<Class<?>, Map<String, PropertyDescriptor>> cachedDescriptors, Map<Object, PropertyPath> collectedBasePropertyPaths) {
		for (int index = 0; index < list.size(); index++) {
			collectBasePropertyPath(list.get(index), recursableGetter, basePath, cachedDescriptors, collectedBasePropertyPaths, index);
		}
	}

	@SuppressWarnings("unchecked")
	private static void collectBasePropertyPathsFromMap(Map<?, ?> map, PropertyPath basePath, Predicate<Method> recursableGetter, Map<Class<?>, Map<String, PropertyDescriptor>> cachedDescriptors, Map<Object, PropertyPath> collectedBasePropertyPaths) {
		for (Entry<?, ?> entry : map.entrySet()) {
			Object key = entry.getKey();

			if (key instanceof Comparable && key instanceof Serializable) {
				collectBasePropertyPath(entry.getValue(), recursableGetter, basePath, cachedDescriptors, collectedBasePropertyPaths, (Comparable<? extends Serializable>) key);
			}
		}
	}

	private static void collectBasePropertyPathsFromArray(Object[] array, PropertyPath basePath, Predicate<Method> recursableGetter, Map<Class<?>, Map<String, PropertyDescriptor>> cachedDescriptors, Map<Object, PropertyPath> collectedBasePropertyPaths) {
		for (int index = 0; index < array.length; index++) {
			collectBasePropertyPath(array[index], recursableGetter, basePath, cachedDescriptors, collectedBasePropertyPaths, index);
		}
	}

	private static void collectBasePropertyPathsFromBean(Object bean, PropertyPath basePath, Predicate<Method> recursableGetter, Map<Class<?>, Map<String, PropertyDescriptor>> cachedDescriptors, Map<Object, PropertyPath> collectedBasePropertyPaths) {
		for (PropertyDescriptor propertyDescriptor : getPropertyDescriptors(bean.getClass(), cachedDescriptors).values()) {
			if (recursableGetter.test(propertyDescriptor.getReadMethod())) {
				collectBasePropertyPath(getBeanProperty(bean, propertyDescriptor), recursableGetter, basePath, cachedDescriptors, collectedBasePropertyPaths, propertyDescriptor.getName());
			}
		}
	}

	private static void collectBasePropertyPath(Object value, Predicate<Method> recursableGetter, PropertyPath basePath, Map<Class<?>, Map<String, PropertyDescriptor>> cachedDescriptors, Map<Object, PropertyPath> collectedBasePropertyPaths, Comparable<? extends Serializable> property) {
		if (value != null && isNeedsFurtherRecursion(value.getClass()) && !collectedBasePropertyPaths.containsKey(value)) {
			PropertyPath path = basePath.with(property);
			collectedBasePropertyPaths.put(value, path);
			collectBasePropertyPaths(value, path, recursableGetter, cachedDescriptors, collectedBasePropertyPaths);
		}
	}

	private static boolean isNeedsFurtherRecursion(Class<?> type) {
		if (type.isPrimitive()) {
			return false; // These don't have properties anyway.
		}
		else if (isOneInstanceOf(type, Type.class, Boolean.class, Number.class, CharSequence.class, Enum.class, Calendar.class, Date.class, Temporal.class)) {
			return false; // Don't recurse common property types which are guaranteed not beans.
		}
		else if (Iterable.class.isAssignableFrom(type) && !isOneInstanceOf(type, List.class, Map.class)) {
			return false; // We only support iterating List and Map for now.
		}
		else {
			return true;
		}
	}


	// Methods --------------------------------------------------------------------------------------------------------

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
			collectMethods(methods, cls, false, methodName, params);

			for (Class<?> iface : cls.getInterfaces()) {
				collectInterfaceMethods(methods, iface, methodName, params);
			}
		}

		if (methods.size() == 1) {
			return methods.get(0);
		}
		else {
			return closestMatchingMethod(methods, params);  // Overloaded methods were found. Try to find closest match.
		}
	}

	private static void collectInterfaceMethods(List<Method> methods, Class<?> iface, String methodName, Object... params) {
		collectMethods(methods, iface, true, methodName, params);

		for (Class<?> superiface : iface.getInterfaces()) {
			collectInterfaceMethods(methods, superiface, methodName, params);
		}
	}

	private static void collectMethods(List<Method> methods, Class<?> type, boolean iface, String methodName, Object... params) {
		for (Method method : type.getDeclaredMethods()) {
			if ((!iface || method.isDefault()) && method.getName().equals(methodName) && method.getParameterTypes().length == params.length && isNotOverridden(methods, method)) {
				methods.add(method);
			}
		}
	}

	private static boolean isNotOverridden(List<Method> methodsWithSameName, Method method) {
		for (Method methodWithSameName : methodsWithSameName) {
			if (Arrays.equals(methodWithSameName.getParameterTypes(), method.getParameterTypes())) {
				return false;
			}
		}

		return true;
	}

	private static Method closestMatchingMethod(List<Method> methods, Object... params) {
		for (Method method : methods) {
			Class<?>[] candidateParamTypes = method.getParameterTypes();
			boolean match = true;

			for (int i = 0; i < params.length; i++) {
				if (!isAssignable(params[i], candidateParamTypes[i])) {
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
	 * Finds methods having the given annotation.
	 * @param base The object in which the methods are to be found.
	 * @param annotation Annotation of the method to be found.
	 * @return List of matching methods.
	 * @since 3.6
	 */
	public static <A extends Annotation> List<Method> findMethods(Object base, Class<A> annotation) {

		List<Method> methods = new ArrayList<>();

		for (Class<?> cls = base.getClass(); cls != null; cls = cls.getSuperclass()) {
			for (Method method : cls.getDeclaredMethods()) {
				if (method.isAnnotationPresent(annotation) && isNotOverridden(methods, method)) {
					methods.add(method);
				}
			}
		}

		return methods;
	}


	// Classes --------------------------------------------------------------------------------------------------------

	/**
	 * Returns true if given source is assignable to target type, taking into account autoboxing.
	 * Java returns namely false on int.class.isAssignableFrom(Integer.class).
	 * @param source The source to be checked.
	 * @param targetType The target type to be checked.
	 * @return True if the given source is assignable to the given target type.
	 * @since 2.7.2
	 */
	public static boolean isAssignable(Object source, Class<?> targetType) {
		Class<?> sourceType = source instanceof Class ? (Class<?>) source : source != null ? source.getClass() : null;

		if (sourceType != null && targetType.isPrimitive()) {
			sourceType = getPrimitiveType(sourceType);
		}

		return (sourceType == null) ? !targetType.isPrimitive() : targetType.isAssignableFrom(sourceType);
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
				logger.log(FINEST, "Ignoring thrown exception; previous exception will be rethrown instead.", ignore);
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
			logger.log(FINEST, "Ignoring thrown exception; the sole intent is to return null instead.", ignore);
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
			logger.log(FINEST, "Ignoring thrown exception; the sole intent is to return null instead.", ignore);
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


	// Fields ---------------------------------------------------------------------------------------------------------

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
	 * Modifies the value of the field of the given instance on the given field name with the given value.
	 * @param <T> The field type.
	 * @param instance The instance to access the given field on.
	 * @param fieldName The name of the field to be accessed on the given instance.
	 * @param value The new value of the field of the given instance on the given field name.
	 * @return The old value of the field of the given instance on the given field name.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @throws IllegalStateException If the field cannot be modified.
	 * @since 3.8
	 */
	public static <T> T modifyField(Object instance, String fieldName, T value) {
		try {
			return modifyField(instance, instance.getClass().getDeclaredField(fieldName), value);
		}
		catch (Exception e) {
			throw new IllegalStateException(format(ERROR_MODIFY_FIELD, fieldName, instance != null ? instance.getClass() : null, value), e);
		}
	}

	/**
	 * Modifies the value of the given field of the given instance with the given value.
	 * @param <T> The field type.
	 * @param instance The instance to access the given field on.
	 * @param field The field to be accessed on the given instance.
	 * @param value The new value of the given field of the given instance.
	 * @return The old value of the given field of the given instance.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @throws IllegalStateException If the field cannot be modified.
	 * @since 3.6
	 */
	@SuppressWarnings("unchecked")
	public static <T> T modifyField(Object instance, Field field, T value) {
		try {
			field.setAccessible(true);
			Object oldValue = field.get(instance);
			field.set(instance, value);
			return (T) oldValue;
		}
		catch (Exception e) {
			throw new IllegalStateException(format(ERROR_MODIFY_FIELD, field != null ? field.getName() : null, instance != null ? instance.getClass() : null, value), e);
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
	public static <T> T invokeMethod(Object instance, String methodName, Object... parameters) {
		try {
			Method method = findMethod(instance, methodName, parameters);

			if (method == null) {
				throw new NoSuchMethodException();
			}

			return invokeMethod(instance, method, parameters);
		}
		catch (Exception e) {
			throw new IllegalStateException(format(ERROR_INVOKE_METHOD, methodName, instance != null ? instance.getClass() : null, Arrays.toString(parameters)), e);
		}
	}

	/**
	 * Invoke given method of the given instance with the given parameters and return the result.
	 * @param <T> The expected return type.
	 * @param instance The instance to invoke the given method on.
	 * @param method The method to be invoked on the given instance.
	 * @param parameters The method parameters, if any.
	 * @return The result of the method invocation, if any.
	 * @throws IllegalStateException If the method cannot be invoked.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @since 3.6
	 */
	@SuppressWarnings("unchecked")
	public static <T> T invokeMethod(Object instance, Method method, Object... parameters) {
		try {
			method.setAccessible(true);
			return (T) method.invoke(instance, parameters);
		}
		catch (Exception e) {
			throw new IllegalStateException(format(ERROR_INVOKE_METHOD, method != null ? method.getName() : null, instance != null ? instance.getClass() : null, Arrays.toString(parameters)), e);
		}
	}

	/**
	 * Invoke methods of the given instance having the given annotation.
	 * @param instance The instance to invoke the methods having the given annotation on.
	 * @param annotation Annotation of the methods to be invoked.
	 * @throws IllegalStateException If the method cannot be invoked.
	 * @since 3.6
	 */
	public static <A extends Annotation> void invokeMethods(Object instance, Class<A> annotation) {
		for (Method method : findMethods(instance, annotation)) {
			try {
				invokeMethod(instance, method);
			}
			catch (Exception e) {
				throw new IllegalStateException(
					format(ERROR_INVOKE_METHOD, method.getName(), instance.getClass(), "[]"), e);
			}
		}
	}

}