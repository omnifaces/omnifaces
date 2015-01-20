/*
 * Copyright 2012 OmniFaces.
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

import static org.omnifaces.util.Faces.getApplication;
import static org.omnifaces.util.Faces.getELContext;
import static org.omnifaces.util.FacesLocal.getContextAttribute;
import static org.omnifaces.util.FacesLocal.getInitParameter;
import static org.omnifaces.util.FacesLocal.setContextAttribute;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.el.MethodInfo;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialViewContext;
import javax.faces.context.PartialViewContextWrapper;

import org.omnifaces.resourcehandler.ResourceIdentifier;

/**
 * Collection of JSF implementation and/or JSF component library specific hacks.
 *
 * @author Bauke Scholtz
 * @author Arjan Tijms
 * @since 1.3
 */
public final class Hacks {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final boolean RICHFACES_INSTALLED = initRichFacesInstalled();
	private static final String RICHFACES_PVC_CLASS_NAME =
		"org.richfaces.context.ExtendedPartialViewContextImpl";
	private static final String RICHFACES_RLR_RENDERER_TYPE =
		"org.richfaces.renderkit.ResourceLibraryRenderer";
	private static final String RICHFACES_RLF_CLASS_NAME =
		"org.richfaces.resource.ResourceLibraryFactoryImpl";

	private static final boolean JUEL_SUPPORTS_METHOD_EXPRESSION = initJUELSupportsMethodExpression();
	private static final String JUEL_EF_CLASS_NAME =
		"de.odysseus.el.ExpressionFactoryImpl";
	private static final String JUEL_MINIMUM_METHOD_EXPRESSION_VERSION =
		"2.2.6";

	private static final String MYFACES_PACKAGE_PREFIX = "org.apache.myfaces.";
	private static final String MYFACES_RENDERED_SCRIPT_RESOURCES_KEY =
		"org.apache.myfaces.RENDERED_SCRIPT_RESOURCES_SET";
	private static final String MYFACES_RENDERED_STYLESHEET_RESOURCES_KEY =
		"org.apache.myfaces.RENDERED_STYLESHEET_RESOURCES_SET";
	private static final Set<String> MOJARRA_MYFACES_RESOURCE_DEPENDENCY_KEYS =
		Utils.unmodifiableSet(
			"com.sun.faces.PROCESSED_RESOURCE_DEPENDENCIES",
			MYFACES_RENDERED_SCRIPT_RESOURCES_KEY,
			MYFACES_RENDERED_STYLESHEET_RESOURCES_KEY);

	private static final String MOJARRA_DEFAULT_RESOURCE_MAX_AGE = "com.sun.faces.defaultResourceMaxAge";
	private static final String MYFACES_DEFAULT_RESOURCE_MAX_AGE = "org.apache.myfaces.RESOURCE_MAX_TIME_EXPIRES";
	private static final long DEFAULT_RESOURCE_MAX_AGE = 604800000L; // 1 week.
	private static final String[] PARAM_NAMES_RESOURCE_MAX_AGE = {
		MOJARRA_DEFAULT_RESOURCE_MAX_AGE, MYFACES_DEFAULT_RESOURCE_MAX_AGE
	};

	private static final String ERROR_MAX_AGE =
		"The '%s' init param must be a number. Encountered an invalid value of '%s'.";
	private static final String ERROR_CREATE_INSTANCE =
		"Cannot create instance of class '%s'.";
	private static final String ERROR_ACCESS_FIELD =
		"Cannot access field '%s' of class '%s'.";
	private static final String ERROR_INVOKE_METHOD =
		"Cannot invoke method '%s' of class '%s' with arguments %s.";

	private static final Object[] EMPTY_PARAMETERS = new Object[0];

	// Lazy loaded properties (will only be initialized when FacesContext is available) -------------------------------

	private static Boolean myFacesUsed;
	private static Long defaultResourceMaxAge;

	// Constructors/init ----------------------------------------------------------------------------------------------

	private Hacks() {
		//
	}

	private static boolean initRichFacesInstalled() {
		try {
			Class.forName(RICHFACES_PVC_CLASS_NAME);
			return true;
		}
		catch (ClassNotFoundException ignore) {
			return false;
		}
	}

	private static boolean initJUELSupportsMethodExpression() {
		Package juelPackage = Package.getPackage("de.odysseus.el");
		if (juelPackage == null) {
			return false;
		}

		String juelVersion = juelPackage.getImplementationVersion();
		if (juelVersion == null) {
			return false;
		}

		return isSameOrHigherVersion(juelVersion, JUEL_MINIMUM_METHOD_EXPRESSION_VERSION);
	}

	// RichFaces related ----------------------------------------------------------------------------------------------

	/**
	 * Returns true if RichFaces 4.0-4.3 is installed. That is, when the RichFaces 4.0-4.3 specific
	 * ExtendedPartialViewContextImpl is present in the runtime classpath. As this is usually auto-registered, we may
	 * safely assume that RichFaces 4.0-4.3 is installed.
	 * <p>
	 * Note: in RichFaces 4.5, this class has changed to ExtendedPartialViewContext which finally properly extends
	 * PartialViewContextWrapper with among others the correct implementation for getRenderIds(), so the
	 * {@link #getRichFacesPartialViewContext()} hack isn't anymore necessary for the purpose.
	 * Also note that RichFaces 4.4 doesn't exist.
	 * @return Whether RichFaces 4.0-4.3 is installed.
	 */
	public static boolean isRichFacesInstalled() {
		return RICHFACES_INSTALLED;
	}

	/**
	 * RichFaces PartialViewContext implementation does not extend from PartialViewContextWrapper. So a hack wherin the
	 * exact fully qualified class name needs to be known has to be used to properly extract it from the
	 * {@link FacesContext#getPartialViewContext()}.
	 * @return The RichFaces PartialViewContext implementation.
	 */
	public static PartialViewContext getRichFacesPartialViewContext() {
		PartialViewContext context = Ajax.getContext();

		while (!context.getClass().getName().equals(RICHFACES_PVC_CLASS_NAME)
			&& context instanceof PartialViewContextWrapper)
		{
			context = ((PartialViewContextWrapper) context).getWrapped();
		}

		if (context.getClass().getName().equals(RICHFACES_PVC_CLASS_NAME)) {
			return context;
		}
		else {
			return null;
		}
	}

	/**
	 * RichFaces PartialViewContext implementation does not have the getRenderIds() method properly implemented. So a
	 * hack wherin the exact name of the private field needs to be known has to be used to properly extract it from the
	 * RichFaces PartialViewContext implementation.
	 * @return The render IDs from the RichFaces PartialViewContext implementation.
	 */
	public static Collection<String> getRichFacesRenderIds() {
		PartialViewContext richFacesContext = getRichFacesPartialViewContext();

		if (richFacesContext != null) {
			Collection<String> renderIds = accessField(richFacesContext, "componentRenderIds");

			if (renderIds != null) {
				return renderIds;
			}
		}

		return Collections.emptyList();
	}

	/**
	 * RichFaces PartialViewContext implementation does not have any getWrapped() method to return the wrapped
	 * PartialViewContext. So a reflection hack is necessary to return it from the private field.
	 * @return The wrapped PartialViewContext from the RichFaces PartialViewContext implementation.
	 */
	public static PartialViewContext getRichFacesWrappedPartialViewContext() {
		PartialViewContext richFacesContext = getRichFacesPartialViewContext();

		if (richFacesContext != null) {
			return accessField(richFacesContext, "wrappedViewContext");
		}

		return null;
	}

	/**
	 * Returns true if the given renderer type is recognizeable as RichFaces resource library renderer.
	 * @param rendererType The renderer type to be checked.
	 * @return Whether the given renderer type is recognizeable as RichFaces resource library renderer.
	 */
	public static boolean isRichFacesResourceLibraryRenderer(String rendererType) {
		return RICHFACES_RLR_RENDERER_TYPE.equals(rendererType);
	}

	/**
	 * Returns an ordered set of all JSF resource identifiers for the given RichFaces resource library resources.
	 * @param id The resource identifier of the RichFaces resource library (e.g. org.richfaces:ajax.reslib).
	 * @return An ordered set of all JSF resource identifiers for the given RichFaces resource library resources.
	 */
	@SuppressWarnings("rawtypes")
	public static Set<ResourceIdentifier> getRichFacesResourceLibraryResources(ResourceIdentifier id) {
		Object resourceFactory = createInstance(RICHFACES_RLF_CLASS_NAME);
		String name = id.getName().split("\\.")[0];
		Object resourceLibrary = invokeMethod(resourceFactory, "getResourceLibrary", name, id.getLibrary());
		Iterable resources = invokeMethod(resourceLibrary, "getResources");
		Set<ResourceIdentifier> resourceIdentifiers = new LinkedHashSet<ResourceIdentifier>();

		for (Object resource : resources) {
			String libraryName = invokeMethod(resource, "getLibraryName");
			String resourceName = invokeMethod(resource, "getResourceName");
			resourceIdentifiers.add(new ResourceIdentifier(libraryName, resourceName));
		}

		return resourceIdentifiers;
	}

	// JUEL related ---------------------------------------------------------------------------------------------------

	public static boolean isJUELUsed() {
		return isJUELUsed(getApplication().getExpressionFactory());
	}

	public static boolean isJUELUsed(ExpressionFactory factory) {
		return factory.getClass().getName().equals(JUEL_EF_CLASS_NAME);
	}

	public static boolean isJUELSupportingMethodExpression() {
		return JUEL_SUPPORTS_METHOD_EXPRESSION;
	}

	/**
	 * Checks if the given version1 is the same or a higher version than version2.
	 *
	 * @param version1 the first version in the comparison
	 * @param version2 the second version in the comparison
	 * @return true if version1 is the same or a higher version than version2, false otherwise
	 */
	private static boolean isSameOrHigherVersion(String version1, String version2) {

		List<Integer> version1Elements = toVersionElements(version1);
		List<Integer> version2Elements = toVersionElements(version2);

		int maxLength = Math.max(version1Elements.size(), version2Elements.size());

		for (int i = 0; i< maxLength; i++) {
			int version1Element = getVersionElement(version1Elements, i);
			int version2Element = getVersionElement(version2Elements, i);

			if (version1Element > version2Element) {
				return true;
			}
			if (version1Element < version2Element) {
				return false;
			}
		}

		return true;
	}

	private static List<Integer> toVersionElements(String version) {

		List<Integer> versionElements = new ArrayList<Integer>();
		for (String string : version.split("\\.")) {
			versionElements.add(Integer.valueOf(string));
		}

		return versionElements;
	}

	private static int getVersionElement(List<Integer> versionElements, int index) {
		if (index < versionElements.size()) {
			return versionElements.get(index);
		}

		return 0;
	}

	// EL related -----------------------------------------------------------------------------------------------------

	/**
	 * This method wraps a <code>MethodExpression</code> in a <code>Method</code> which can be statically invoked.
	 * <p>
	 * Since Method is a final class with only a non-public constructor, various reflective tricks have been used to
	 * create an instance of this class and make sure it calls the given method expression. It has been tested on the
	 * Sun/Oracle JDK versions 6 and 7, and it should work on OpenJDK 6 and 7 as well. Other JDKs might not work.
	 *
	 * @param context
	 *            the context used for evaluation of the method expression when it's invoked later. NOTE, this reference
	 *            is retained by the returned method.
	 *
	 * @param methodExpression
	 *            the method expression to be wrapped
	 * @return a Method instance that when invoked causes the wrapped method expression to be invoked.
	 */
	public static Method methodExpressionToStaticMethod(final ELContext context, final MethodExpression methodExpression) {

		MethodInfo methodInfo = methodExpression.getMethodInfo(getELContext());

		try {
			// Create a Method instance with the signature (return type, name, parameter types) corresponding
			// to the method the MethodExpression references.
			Constructor<Method> methodConstructor = Method.class.getDeclaredConstructor(Class.class,
				String.class, Class[].class, Class.class,
				Class[].class, int.class, int.class, String.class, byte[].class, byte[].class, byte[].class
			);
			methodConstructor.setAccessible(true);
			Method staticMethod = methodConstructor.newInstance(null,
				methodInfo.getName(), methodInfo.getParamTypes(), methodInfo.getReturnType(),
				null, 0, 0, null, null, null, null
			);

			// The Sun/Oracle/OpenJDK Method makes use of a private delegator called MethodAccessor.
			// Though specific to those JDKs, this is what we can use to let our Method instance execute something
			// we want. (simply overriding the invoke method would be much better, but unfortunately Method is final)
			Class<?> methodAccessorClass = Class.forName("sun.reflect.MethodAccessor");

			// Create a proxy for our MethodAccessor, so we don't have to reference the actual type at compile-time.
			Object MethodAccessor = Proxy.newProxyInstance(Method.class.getClassLoader(), new Class[] { methodAccessorClass },
					new InvocationHandler() {

						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

							Object[] params = null;
							if (args != null && args.length > 1) {
								// args[0] should be the Object on which the method is to be invoked (null in case of static call)
								// args[1] should be the parameters for the method invocation (possibly empty)
								params = (Object[]) args[1];
							} else {
								params = EMPTY_PARAMETERS;
							}

							return methodExpression.invoke(context, params);
						}
					});

			Method setMethodAccessor = Method.class.getDeclaredMethod("setMethodAccessor", methodAccessorClass);
			setMethodAccessor.setAccessible(true);
			setMethodAccessor.invoke(staticMethod, MethodAccessor);

			// Another private implementation detail of the Sun/Oracle/OpenJDK Method - unless override is set
			// to true, a couple of nasty language checks are done before invoking the MethodAccessor
			Field override = AccessibleObject.class.getDeclaredField("override");
			override.setAccessible(true);
			override.set(staticMethod, true);

			return staticMethod;
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	// MyFaces related ------------------------------------------------------------------------------------------------

	/**
	 * Returns true if MyFaces is used. That is, when the FacesContext instance is from the MyFaces specific package.
	 * @return Whether MyFaces is used.
	 * @since 1.8
	 */
	public static boolean isMyFacesUsed() {
		if (myFacesUsed == null) {
			FacesContext context = FacesContext.getCurrentInstance();

			if (context != null) {
				myFacesUsed = context.getClass().getPackage().getName().startsWith(MYFACES_PACKAGE_PREFIX);
			}
			else {
				return false;
			}
		}

		return myFacesUsed;
	}

	// JSF resource handling related ----------------------------------------------------------------------------------

	/**
	 * Set the given script resource as rendered.
	 * @param context The involved faces context.
	 * @param id The resource identifier.
	 * @since 1.8
	 */
	public static void setScriptResourceRendered(FacesContext context, ResourceIdentifier id) {
		setMojarraResourceRendered(context, id);

		if (isMyFacesUsed()) {
			setMyFacesResourceRendered(context, MYFACES_RENDERED_SCRIPT_RESOURCES_KEY, id);
		}
	}

	/**
	 * Returns whether the given script resource is rendered.
	 * @param context The involved faces context.
	 * @param id The resource identifier.
	 * @return Whether the given script resource is rendered.
	 * @since 1.8
	 */
	public static boolean isScriptResourceRendered(FacesContext context, ResourceIdentifier id) {
		boolean rendered = isMojarraResourceRendered(context, id);

		if (!rendered && isMyFacesUsed()) {
			return isMyFacesResourceRendered(context, MYFACES_RENDERED_SCRIPT_RESOURCES_KEY, id);
		}
		else {
			return rendered;
		}
	}

	/**
	 * Set the given stylesheet resource as rendered.
	 * @param context The involved faces context.
	 * @param id The resource identifier.
	 * @since 1.8
	 */
	public static void setStylesheetResourceRendered(FacesContext context, ResourceIdentifier id) {
		setMojarraResourceRendered(context, id);

		if (isMyFacesUsed()) {
			setMyFacesResourceRendered(context, MYFACES_RENDERED_STYLESHEET_RESOURCES_KEY, id);
		}
	}

	private static void setMojarraResourceRendered(FacesContext context, ResourceIdentifier id) {
		context.getAttributes().put(id.getName() + id.getLibrary(), true);
	}

	private static boolean isMojarraResourceRendered(FacesContext context, ResourceIdentifier id) {
		return context.getAttributes().containsKey(id.getName() + id.getLibrary());
	}

	private static void setMyFacesResourceRendered(FacesContext context, String key, ResourceIdentifier id) {
		getMyFacesResourceMap(context, key).put(getMyFacesResourceKey(id), true);
	}

	private static boolean isMyFacesResourceRendered(FacesContext context, String key, ResourceIdentifier id) {
		return getMyFacesResourceMap(context, key).containsKey(getMyFacesResourceKey(id));
	}

	private static Map<String, Boolean> getMyFacesResourceMap(FacesContext context, String key) {
		Map<String, Boolean> map = getContextAttribute(context, key);

		if (map == null) {
			map = new HashMap<String, Boolean>();
			setContextAttribute(context, key, map);
		}

		return map;
	}

	private static String getMyFacesResourceKey(ResourceIdentifier id) {
		String library = id.getLibrary();
		String name = id.getName();
		return (library != null) ? (library + '/' + name) : name;
	}

	/**
	 * Remove the resource dependency processing related attributes from the given faces context.
	 * @param context The involved faces context.
	 */
	public static void removeResourceDependencyState(FacesContext context) {
		// Mojarra and MyFaces remembers processed resource dependencies in a map.
		context.getAttributes().keySet().removeAll(MOJARRA_MYFACES_RESOURCE_DEPENDENCY_KEYS);

		// Mojarra and PrimeFaces puts "namelibrary=true" for every processed resource dependency.
		// TODO: This may possibly conflict with other keys with value=true. So far tested, this is harmless.
		context.getAttributes().values().removeAll(Collections.singleton(true));
	}

	/**
	 * Returns the default resource maximum age in milliseconds.
	 * @return The default resource maximum age in milliseconds.
	 */
	public static long getDefaultResourceMaxAge() {
		if (defaultResourceMaxAge != null) {
			return defaultResourceMaxAge;
		}

		FacesContext context = FacesContext.getCurrentInstance();

		if (context == null) {
			return DEFAULT_RESOURCE_MAX_AGE;
		}

		for (String name : PARAM_NAMES_RESOURCE_MAX_AGE) {
			String value = getInitParameter(context, name);

			if (value != null) {
				try {
					return (defaultResourceMaxAge = Long.valueOf(value));
				}
				catch (NumberFormatException e) {
					throw new IllegalArgumentException(String.format(ERROR_MAX_AGE, name, value), e);
				}
			}
		}

		return (defaultResourceMaxAge = DEFAULT_RESOURCE_MAX_AGE);
	}

	// PrimeFaces related ---------------------------------------------------------------------------------------------

	/**
	 * Returns true if the current request is a PrimeFaces dynamic resource request.
	 * @param context The involved faces context.
	 * @return Whether the current request is a PrimeFaces dynamic resource request.
	 * @since 1.8
	 */
	public static boolean isPrimeFacesDynamicResourceRequest(FacesContext context) {
		Map<String, String> params = context.getExternalContext().getRequestParameterMap();
		return "primefaces".equals(params.get("ln")) && params.get("pfdrid") != null;
	}

	// Some reflection helpers ----------------------------------------------------------------------------------------

	/**
	 * Create an instance of the given class using the default constructor and return the instance.
	 */
	private static Object createInstance(String className) {
		try {
			return Class.forName(className).newInstance();
		}
		catch (Exception e) {
			throw new RuntimeException(
				String.format(ERROR_CREATE_INSTANCE, className), e);
		}
	}

	/**
	 * Access a field of the given instance on the given field name and return the field value.
	 */
	@SuppressWarnings("unchecked")
	private static <T> T accessField(Object instance, String fieldName) {
		try {
			Field field = instance.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			return (T) field.get(instance);
		}
		catch (Exception e) {
			throw new RuntimeException(
				String.format(ERROR_ACCESS_FIELD, fieldName, instance.getClass()), e);
		}
	}

	/**
	 * Invoke a method of the given instance on the given method name with the given parameters and return the result.
	 * Note: the current implementation assumes for simplicity that no one of the given parameters is null. If one of
	 * them is still null, a NullPointerException will be thrown. The implementation should be changed accordingly to
	 * take that into account (but then varargs cannot be used anymore and you end up creating ugly arrays).
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static <T> T invokeMethod(Object instance, String methodName, Object... parameters) {
		Class[] parameterTypes = new Class[parameters.length];

		for (int i = 0; i < parameters.length; i++) {
			parameterTypes[i] = parameters[i].getClass();
		}

		try {
			Method method = instance.getClass().getMethod(methodName, parameterTypes);
			method.setAccessible(true);
			return (T) method.invoke(instance, parameters);
		}
		catch (Exception e) {
			throw new RuntimeException(
				String.format(ERROR_INVOKE_METHOD, methodName, instance.getClass(), Arrays.toString(parameters)), e);
		}
	}

}
