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

import static org.omnifaces.util.FacesLocal.getApplicationAttribute;
import static org.omnifaces.util.FacesLocal.getContextAttribute;
import static org.omnifaces.util.FacesLocal.getInitParameter;
import static org.omnifaces.util.FacesLocal.getSessionAttribute;
import static org.omnifaces.util.FacesLocal.setContextAttribute;
import static org.omnifaces.util.Reflection.findMethod;
import static org.omnifaces.util.Utils.unmodifiableSet;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.component.StateHelper;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialViewContext;
import javax.faces.context.PartialViewContextWrapper;
import javax.faces.render.ResponseStateManager;

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

	private static final Set<String> RICHFACES_PVC_CLASS_NAMES =
		unmodifiableSet(
			"org.richfaces.context.ExtendedPartialViewContextImpl", // RichFaces 4.0-4.3.
			"org.richfaces.context.ExtendedPartialViewContext"); // RichFaces 4.5+.
	private static final boolean RICHFACES_INSTALLED = initRichFacesInstalled();
	private static final String RICHFACES_RLR_RENDERER_TYPE =
		"org.richfaces.renderkit.ResourceLibraryRenderer";
	private static final String RICHFACES_RLF_CLASS_NAME =
		"org.richfaces.resource.ResourceLibraryFactoryImpl";

	private static final String MYFACES_PACKAGE_PREFIX = "org.apache.myfaces.";
	private static final String MYFACES_RENDERED_SCRIPT_RESOURCES_KEY =
		"org.apache.myfaces.RENDERED_SCRIPT_RESOURCES_SET";
	private static final String MYFACES_RENDERED_STYLESHEET_RESOURCES_KEY =
		"org.apache.myfaces.RENDERED_STYLESHEET_RESOURCES_SET";
	private static final Set<String> MOJARRA_MYFACES_RESOURCE_DEPENDENCY_KEYS =
		unmodifiableSet(
			"com.sun.faces.PROCESSED_RESOURCE_DEPENDENCIES",
			MYFACES_RENDERED_SCRIPT_RESOURCES_KEY,
			MYFACES_RENDERED_STYLESHEET_RESOURCES_KEY);

	private static final String MOJARRA_DEFAULT_RESOURCE_MAX_AGE = "com.sun.faces.defaultResourceMaxAge";
	private static final String MYFACES_DEFAULT_RESOURCE_MAX_AGE = "org.apache.myfaces.RESOURCE_MAX_TIME_EXPIRES";
	private static final long DEFAULT_RESOURCE_MAX_AGE = 604800000L; // 1 week.
	private static final String[] PARAM_NAMES_RESOURCE_MAX_AGE = {
		MOJARRA_DEFAULT_RESOURCE_MAX_AGE, MYFACES_DEFAULT_RESOURCE_MAX_AGE
	};

	private static final String MOJARRA_SERIALIZED_VIEWS = "com.sun.faces.renderkit.ServerSideStateHelper.LogicalViewMap";
	private static final String MOJARRA_SERIALIZED_VIEW_KEY = "com.sun.faces.logicalViewMap";
	private static final String MYFACES_SERIALIZED_VIEWS = "org.apache.myfaces.application.viewstate.ServerSideStateCacheImpl.SERIALIZED_VIEW";
	private static final String MYFACES_VIEW_SCOPE_PROVIDER = "org.apache.myfaces.spi.ViewScopeProvider.INSTANCE";

	private static final String ERROR_MAX_AGE =
		"The '%s' init param must be a number. Encountered an invalid value of '%s'.";
	private static final String ERROR_CREATE_INSTANCE =
		"Cannot create instance of class '%s'.";
	private static final String ERROR_ACCESS_FIELD =
		"Cannot access field '%s' of class '%s'.";
	private static final String ERROR_INVOKE_METHOD =
		"Cannot invoke method '%s' of class '%s' with arguments %s.";

	// Lazy loaded properties (will only be initialized when FacesContext is available) -------------------------------

	private static volatile Boolean myFacesUsed;
	private static volatile Long defaultResourceMaxAge;

	// Constructors/init ----------------------------------------------------------------------------------------------

	private Hacks() {
		//
	}

	private static boolean initRichFacesInstalled() {
		for (String richFacesPvcClassName : RICHFACES_PVC_CLASS_NAMES) {
			try {
				Class.forName(richFacesPvcClassName);
				return true;
			}
			catch (ClassNotFoundException ignore) {
				continue;
			}
		}

		return false;
	}

	// RichFaces related ----------------------------------------------------------------------------------------------

	/**
	 * Returns true if RichFaces 4.x is installed. That is, when the RichFaces 4.0-4.3 specific
	 * ExtendedPartialViewContextImpl, or RichFaces 4.5+ specific ExtendedPartialViewContext is present in the runtime
	 * classpath. As this is usually auto-registered, we may safely assume that RichFaces 4.x is installed.
	 * <p>
	 * Note that RichFaces 4.4 doesn't exist.
	 * @return Whether RichFaces 4.x is installed.
	 */
	public static boolean isRichFacesInstalled() {
		return RICHFACES_INSTALLED;
	}

	/**
	 * RichFaces 4.0-4.3 ExtendedPartialViewContextImpl does not extend from PartialViewContextWrapper. So a hack wherin
	 * the exact fully qualified class name needs to be known has to be used to properly extract it from the
	 * {@link FacesContext#getPartialViewContext()}.
	 * @return The RichFaces PartialViewContext implementation.
	 */
	public static PartialViewContext getRichFacesPartialViewContext() {
		PartialViewContext context = Ajax.getContext();

		while (!RICHFACES_PVC_CLASS_NAMES.contains(context.getClass().getName())
			&& context instanceof PartialViewContextWrapper)
		{
			context = ((PartialViewContextWrapper) context).getWrapped();
		}

		if (RICHFACES_PVC_CLASS_NAMES.contains(context.getClass().getName())) {
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
	 * RichFaces 4.0-4.3 ExtendedPartialViewContextImpl does not have any getWrapped() method to return the wrapped
	 * PartialViewContext. So a reflection hack is necessary to return it from the private field.
	 * @return The wrapped PartialViewContext from the RichFaces 4.0-4.3 ExtendedPartialViewContextImpl.
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
		Set<ResourceIdentifier> resourceIdentifiers = new LinkedHashSet<>();

		for (Object resource : resources) {
			String libraryName = invokeMethod(resource, "getLibraryName");
			String resourceName = invokeMethod(resource, "getResourceName");
			resourceIdentifiers.add(new ResourceIdentifier(libraryName, resourceName));
		}

		return resourceIdentifiers;
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
			map = new HashMap<>();
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
		if (defaultResourceMaxAge == null) {
			Long resourceMaxAge = DEFAULT_RESOURCE_MAX_AGE;
			FacesContext context = FacesContext.getCurrentInstance();

			if (context == null) {
				return resourceMaxAge;
			}


			for (String name : PARAM_NAMES_RESOURCE_MAX_AGE) {
				String value = getInitParameter(context, name);

				if (value != null) {
					try {
						resourceMaxAge = Long.valueOf(value);
						break;
					}
					catch (NumberFormatException e) {
						throw new IllegalArgumentException(String.format(ERROR_MAX_AGE, name, value), e);
					}
				}
			}

			defaultResourceMaxAge = resourceMaxAge;
		}

		return defaultResourceMaxAge;
	}


	//  JSF state saving related --------------------------------------------------------------------------------------

	/**
	 * Remove server side JSF view state associated with current request.
	 * @param context The involved faces context.
	 * @param manager The involved response state manager.
	 * @param viewId The view ID of the involved view.
	 * @since 2.3
	 */
	public static void removeViewState(FacesContext context, ResponseStateManager manager, String viewId) {
		if (isMyFacesUsed()) {
			Object state = invokeMethod(manager, "getSavedState", context);

			if (!(state instanceof String)) {
				return;
			}

			Object viewCollection = getSessionAttribute(context, MYFACES_SERIALIZED_VIEWS);

			if (viewCollection == null) {
				return;
			}

			Object stateCache = invokeMethod(manager, "getStateCache", context);
			Integer stateId = invokeMethod(stateCache, "getServerStateId", context, state);
			Serializable key = invokeMethod(invokeMethod(stateCache, "getSessionViewStorageFactory"), "createSerializedViewKey", context, viewId, stateId);

			List<Serializable> keys = accessField(viewCollection, "_keys");
			Map<Serializable, Object> serializedViews = accessField(viewCollection, "_serializedViews");
			Map<Serializable, Serializable> precedence = accessField(viewCollection, "_precedence");

			synchronized (viewCollection) { // Those fields are not concurrent maps.
				keys.remove(key);
				serializedViews.remove(key);
				Serializable previousKey = precedence.remove(key);

				if (previousKey != null) {
					for (Entry<Serializable, Serializable> entry : precedence.entrySet()) {
						if (entry.getValue().equals(key)) {
							entry.setValue(previousKey);
						}
					}
				}

				Map<Serializable, String> viewScopeIds = accessField(viewCollection, "_viewScopeIds");

				if (viewScopeIds == null) {
					return;
				}

				Map<String, Integer> viewScopeIdCounts = accessField(viewCollection, "_viewScopeIdCounts");
				String viewScopeId = viewScopeIds.remove(key);
				int count = viewScopeIdCounts.get(viewScopeId) - 1;

				if (count < 1) {
					viewScopeIdCounts.remove(viewScopeId);
					invokeMethod(getApplicationAttribute(context, MYFACES_VIEW_SCOPE_PROVIDER), "destroyViewScopeMap", context, viewScopeId);
				}
				else {
					viewScopeIdCounts.put(viewScopeId, count);
				}
			}
		}
		else { // Well, let's assume Mojarra.
			Map<String, Object> views = getSessionAttribute(context, MOJARRA_SERIALIZED_VIEWS);

			if (views != null) {
				views.remove(context.getAttributes().get(MOJARRA_SERIALIZED_VIEW_KEY));
			}
		}
	}

	/**
	 * Expose protected state helper into public.
	 * @param component The component to obtain state helper for.
	 * @return The state helper of the given component.
	 * @since 2.3
	 */
	public static StateHelper getStateHelper(UIComponent component) {
		return invokeMethod(component, "getStateHelper");
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
			throw new IllegalArgumentException(
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
			throw new IllegalArgumentException(
				String.format(ERROR_ACCESS_FIELD, fieldName, instance.getClass()), e);
		}
	}

	/**
	 * Invoke a method of the given instance on the given method name with the given parameters and return the result.
	 * Note: the current implementation assumes for simplicity that no one of the given parameters is null. If one of
	 * them is still null, a NullPointerException will be thrown. The implementation should be changed accordingly to
	 * take that into account (but then varargs cannot be used anymore and you end up creating ugly arrays).
	 */
	@SuppressWarnings("unchecked")
	private static <T> T invokeMethod(Object instance, String methodName, Object... parameters) {
		try {
			Method method = findMethod(instance, methodName, parameters);
			method.setAccessible(true);
			return (T) method.invoke(instance, parameters);
		}
		catch (Exception e) {
			throw new IllegalArgumentException(
				String.format(ERROR_INVOKE_METHOD, methodName, instance.getClass(), Arrays.toString(parameters)), e);
		}
	}

}