/*
 * Copyright 2017 OmniFaces
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

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static org.omnifaces.util.Components.getClosestParent;
import static org.omnifaces.util.FacesLocal.getApplicationAttribute;
import static org.omnifaces.util.FacesLocal.getContextAttribute;
import static org.omnifaces.util.FacesLocal.getInitParameter;
import static org.omnifaces.util.FacesLocal.getSessionAttribute;
import static org.omnifaces.util.FacesLocal.setContextAttribute;
import static org.omnifaces.util.Reflection.accessField;
import static org.omnifaces.util.Reflection.instance;
import static org.omnifaces.util.Reflection.invokeMethod;
import static org.omnifaces.util.Reflection.toClassOrNull;
import static org.omnifaces.util.Utils.unmodifiableSet;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.component.StateHelper;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialViewContext;
import javax.faces.context.PartialViewContextWrapper;
import javax.faces.render.ResponseStateManager;
import javax.websocket.Session;

import org.omnifaces.resourcehandler.ResourceIdentifier;

/**
 * Collection of JSF implementation and/or JSF component library and/or server specific hacks.
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

	private static final Class<UIComponent> PRIMEFACES_DIALOG_CLASS =
		toClassOrNull("org.primefaces.component.dialog.Dialog");

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
    private static final String MYFACES_RESOURCE_DEPENDENCY_UNIQUE_ID = "oam.view.resourceDependencyUniqueId";

	private static final String MOJARRA_SERIALIZED_VIEWS = "com.sun.faces.renderkit.ServerSideStateHelper.LogicalViewMap";
	private static final String MOJARRA_SERIALIZED_VIEW_KEY = "com.sun.faces.logicalViewMap";
	private static final String MYFACES_SERIALIZED_VIEWS = "org.apache.myfaces.application.viewstate.ServerSideStateCacheImpl.SERIALIZED_VIEW";
	private static final String MYFACES_VIEW_SCOPE_PROVIDER = "org.apache.myfaces.spi.ViewScopeProvider.INSTANCE";

	private static final String ERROR_MAX_AGE =
		"The '%s' init param must be a number. Encountered an invalid value of '%s'.";

	// Lazy loaded properties (will only be initialized when FacesContext is available) -------------------------------

	private static volatile Boolean myFacesUsed;
	private static volatile Long defaultResourceMaxAge;

	// Constructors/init ----------------------------------------------------------------------------------------------

	private Hacks() {
		//
	}

	private static boolean initRichFacesInstalled() {
		for (String richFacesPvcClassName : RICHFACES_PVC_CLASS_NAMES) {
			if (toClassOrNull(richFacesPvcClassName) != null) {
				return true;
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
		Object resourceFactory = instance(RICHFACES_RLF_CLASS_NAME);
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
						throw new IllegalArgumentException(format(ERROR_MAX_AGE, name, value), e);
					}
				}
			}

			defaultResourceMaxAge = resourceMaxAge;
		}

		return defaultResourceMaxAge;
	}

	/**
	 * Remove the resource dependency processing related attributes from the given faces context.
	 * @param context The involved faces context.
	 */
	public static void removeResourceDependencyState(FacesContext context) {
		// Mojarra and MyFaces remembers processed resource dependencies in a map.
		context.getAttributes().keySet().removeAll(MOJARRA_MYFACES_RESOURCE_DEPENDENCY_KEYS);

		// Mojarra and PrimeFaces puts "namelibrary=true" for every processed resource dependency.
		// NOTE: This may possibly conflict with other keys with value=true. So far tested, this is harmless.
		context.getAttributes().values().removeAll(Collections.singleton(true));
 	}

	/**
	 * Set the unique ID of the component resource, taking into account MyFaces-specific way of generating a
	 * resource specific unique ID.
	 * @param context The involved faces context.
	 * @param resource The involved component resource.
	 * @since 2.6.1
	 */
	public static void setComponentResourceUniqueId(FacesContext context, UIComponent resource) {
		UIViewRoot view = context.getViewRoot();

		if (isMyFacesUsed()) {
			view.getAttributes().put(MYFACES_RESOURCE_DEPENDENCY_UNIQUE_ID, TRUE);
		}

		try {
			resource.setId(view.createUniqueId(context, null));
		}
		finally {
			if (isMyFacesUsed()) {
				view.getAttributes().remove(MYFACES_RESOURCE_DEPENDENCY_UNIQUE_ID);
			}
		}
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

	/**
	 * Returns true if the given components are nested in (same) PrimeFaces dialog.
	 * @param components The components to be checked.
	 * @return Whether the given components are nested in (same) PrimeFaces dialog.
	 * @since 2.6
	 */
	public static boolean isNestedInPrimeFacesDialog(UIComponent... components) {
		if (PRIMEFACES_DIALOG_CLASS == null) {
			return false;
		}

		Set<UIComponent> dialogs = new HashSet<>();

		for (UIComponent component : components) {
			dialogs.add(getClosestParent(component, PRIMEFACES_DIALOG_CLASS));
		}

		return dialogs.size() == 1 && dialogs.iterator().next() != null;
	}

	// Tomcat related -------------------------------------------------------------------------------------------------

	/**
	 * Returns true if the given WS session is from Tomcat and given illegal state exception is caused by a push bomb
	 * which Tomcat couldn't handle. See also https://bz.apache.org/bugzilla/show_bug.cgi?id=56026 and
	 * https://github.com/omnifaces/omnifaces/issues/234
	 * @param session The WS session.
	 * @param illegalStateException The illegal state exception.
	 * @return Whether it was Tomcat who couldn't handle the push bomb.
	 * @since 2.5
	 */
	public static boolean isTomcatWebSocketBombed(Session session, IllegalStateException illegalStateException) {
		return session.getClass().getName().startsWith("org.apache.tomcat.websocket.")
			&& illegalStateException.getMessage().contains("[TEXT_FULL_WRITING]");
	}

}