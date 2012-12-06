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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.faces.application.Resource;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialViewContext;
import javax.faces.context.PartialViewContextWrapper;

import org.omnifaces.resourcehandler.ResourceIdentifier;

/**
 * Collection of JSF implementation and/or JSF component library specific hacks. So far now there are only RichFaces
 * specific hacks to get OmniFaces to work nicely together with RichFaces.
 *
 * @author Bauke Scholtz
 * @since 1.3
 */
public final class Hacks {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final boolean RICHFACES_INSTALLED = initRichFacesInstalled();
	private static final boolean RICHFACES_RESOURCE_OPTIMIZATION_ENABLED =
		RICHFACES_INSTALLED && Boolean.valueOf(Faces.getInitParameter("org.richfaces.resourceOptimization.enabled"));
	private static final String RICHFACES_PVC_CLASS_NAME =
		"org.richfaces.context.ExtendedPartialViewContextImpl";
	private static final String RICHFACES_RLR_RENDERER_TYPE =
		"org.richfaces.renderkit.ResourceLibraryRenderer";
	private static final String RICHFACES_RLF_CLASS_NAME =
		"org.richfaces.resource.ResourceLibraryFactoryImpl";

	private static final String ERROR_CREATE_INSTANCE =
		"Cannot create instance of class '%s'.";
	private static final String ERROR_ACCESS_FIELD =
		"Cannot access field '%s' of class '%s'.";
	private static final String ERROR_INVOKE_METHOD =
		"Cannot invoke method '%s' of class '%s' with arguments %s.";

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

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Returns true if RichFaces is installed. That is, when the RichFaces specific ExtendedPartialViewContextImpl is
	 * present in the runtime classpath -which is present in RF 4.x. As this is usually auto-registered, we may safely
	 * assume that RichFaces is installed.
	 * @return Whether RichFaces is installed.
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
	 * RichFaces "resource optimization" do not support {@link Resource#getURL()} and {@link Resource#getInputStream()}.
	 * The combined resource handler has to manually create the URL based on {@link Resource#getRequestPath()} and the
	 * current request domain URL whenever RichFaces "resource optimization" is enabled.
	 * @return Whether RichFaces resource optimization is enabled.
	 */
	public static boolean isRichFacesResourceOptimizationEnabled() {
		return RICHFACES_RESOURCE_OPTIMIZATION_ENABLED;
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