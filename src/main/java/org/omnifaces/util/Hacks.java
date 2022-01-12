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

import static jakarta.faces.application.ResourceHandler.JSF_SCRIPT_LIBRARY_NAME;
import static jakarta.faces.component.behavior.ClientBehaviorContext.BEHAVIOR_EVENT_PARAM_NAME;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static org.omnifaces.resourcehandler.DefaultResourceHandler.FACES_SCRIPT_RESOURCE_NAME;
import static org.omnifaces.util.Components.getClosestParent;
import static org.omnifaces.util.Components.getCurrentActionSource;
import static org.omnifaces.util.FacesLocal.getApplicationAttribute;
import static org.omnifaces.util.FacesLocal.getInitParameter;
import static org.omnifaces.util.FacesLocal.getRequestParameter;
import static org.omnifaces.util.FacesLocal.getSessionAttribute;
import static org.omnifaces.util.FacesLocal.isAjaxRequest;
import static org.omnifaces.util.FacesLocal.isRenderResponse;
import static org.omnifaces.util.Reflection.accessField;
import static org.omnifaces.util.Reflection.invokeMethod;
import static org.omnifaces.util.Reflection.toClassOrNull;
import static org.omnifaces.util.Utils.coalesce;
import static org.omnifaces.util.Utils.unmodifiableSet;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jakarta.faces.application.ResourceHandler;
import jakarta.faces.component.StateHelper;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.component.behavior.ClientBehaviorHolder;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.FacesContextWrapper;
import jakarta.faces.render.ResponseStateManager;
import jakarta.websocket.Session;

/**
 * Collection of JSF implementation and/or JSF component library and/or server specific hacks.
 *
 * @author Bauke Scholtz
 * @author Arjan Tijms
 * @since 1.3
 */
public final class Hacks {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String PRIMEFACES_PACKAGE_PREFIX = "org.primefaces.";
	private static final Class<UIComponent> PRIMEFACES_DIALOG_CLASS =
		toClassOrNull("org.primefaces.component.dialog.Dialog");

	private static final String MOJARRA_PACKAGE_PREFIX = "com.sun.faces.";
	private static final String MYFACES_PACKAGE_PREFIX = "org.apache.myfaces.";
	private static final Set<String> MYFACES_RESOURCE_DEPENDENCY_KEYS =
		unmodifiableSet(
			"org.apache.myfaces.RENDERED_SCRIPT_RESOURCES_SET", // MyFaces rendered @ResourceDependency(name$=.js) and <h:outputScript>
			"org.apache.myfaces.RENDERED_STYLESHEET_RESOURCES_SET"); // MyFaces rendered @ResourceDependency(name$=.css) and <h:outputStylesheet>
	private static final String MOJARRA_DEFAULT_RESOURCE_MAX_AGE = "com.sun.faces.defaultResourceMaxAge";
	private static final String MYFACES_DEFAULT_RESOURCE_MAX_AGE = "org.apache.myfaces.RESOURCE_MAX_TIME_EXPIRES";
	private static final long DEFAULT_RESOURCE_MAX_AGE = 604800000L; // 1 week.
	private static final String[] PARAM_NAMES_RESOURCE_MAX_AGE = {
		MOJARRA_DEFAULT_RESOURCE_MAX_AGE, MYFACES_DEFAULT_RESOURCE_MAX_AGE
	};
	private static final String MYFACES_RESOURCE_DEPENDENCY_UNIQUE_ID = "oam.view.resourceDependencyUniqueId";

	private static final String MOJARRA_SERIALIZED_VIEWS = "com.sun.faces.renderkit.ServerSideStateHelper.LogicalViewMap";
	private static final String MOJARRA_SERIALIZED_VIEW_KEY = "com.sun.faces.logicalViewMap";
	private static final String MOJARRA_ACTIVE_VIEW_MAPS = "com.sun.faces.application.view.activeViewMaps";
	private static final String MOJARRA_VIEW_MAP_ID = "com.sun.faces.application.view.viewMapId";
	private static final String MYFACES_SERIALIZED_VIEWS = "org.apache.myfaces.application.viewstate.ServerSideStateCacheImpl.SERIALIZED_VIEW";
	private static final String MYFACES_VIEW_SCOPE_PROVIDER = "org.apache.myfaces.spi.ViewScopeProvider.INSTANCE";

	private static final String MOJARRA_CACHED_SERVLET_MAPPING_KEY = "com.sun.faces.INVOCATION_PATH";
	private static final String MYFACES_CACHED_SERVLET_MAPPING_KEY = "org.apache.myfaces.shared.application.DefaultViewHandlerSupport.CACHED_SERVLET_MAPPING";

	private static final String ERROR_MAX_AGE =
		"The '%s' init param must be a number. Encountered an invalid value of '%s'.";

	// Lazy loaded properties (will only be initialized when FacesContext is available) -------------------------------

	private static Boolean mojarraUsed;
	private static Boolean myFacesUsed;
	private static Boolean facesScriptResourceAvailable;
	private static Long defaultResourceMaxAge;

	// Constructors/init ----------------------------------------------------------------------------------------------

	private Hacks() {
		//
	}

	// JSF impl related -----------------------------------------------------------------------------------------------

	/**
	 * Returns true if Mojarra is used. That is, when the FacesContext instance is from the Mojarra specific package.
	 * @return Whether Mojarra is used.
	 * @since 3.9
	 */
	public static boolean isMojarraUsed() {
		if (mojarraUsed == null) {
			FacesContext context = FacesContext.getCurrentInstance();

			if (context != null) {
				while (context instanceof FacesContextWrapper) {
					context = ((FacesContextWrapper) context).getWrapped();
				}

				mojarraUsed = context.getClass().getPackage().getName().startsWith(MOJARRA_PACKAGE_PREFIX);
			}
			else {
				return false;
			}
		}

		return mojarraUsed;
	}

	/**
	 * Returns true if MyFaces is used. That is, when the FacesContext instance is from the MyFaces specific package.
	 * @return Whether MyFaces is used.
	 * @since 1.8
	 */
	public static boolean isMyFacesUsed() {
		if (myFacesUsed == null) {
			FacesContext context = FacesContext.getCurrentInstance();

			if (context != null) {
				while (context instanceof FacesContextWrapper) {
					context = ((FacesContextWrapper) context).getWrapped();
				}

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
		// MyFaces remembers rendered resource dependencies in a map which isn't cleared on change of view.
		context.getAttributes().keySet().removeAll(MYFACES_RESOURCE_DEPENDENCY_KEYS);

		if (isRenderResponse(context) || isPrimeFacesAjaxRequest(context)) {
			// Mojarra 2.3+ resource dependency state is not properly cleared during render response, so it needs to be manually cleared.
			// PrimeFaces core.js updateHead() function basically replaces the entire head instead of appending to it, so all state should be cleared nonetheless.
			context.getAttributes().keySet().remove(ResourceHandler.RESOURCE_IDENTIFIER);
		}

		// PrimeFaces puts "namelibrary=true" for every rendered resource dependency.
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
				view.getAttributes().put(MYFACES_RESOURCE_DEPENDENCY_UNIQUE_ID, FALSE);
			}
		}
	}

	/**
	 * Clear the cached faces servlet mapping as interpreted by either Mojarra or MyFaces.
	 * This is useful if you want to force the impl to recalculate the faces servlet mapping.
	 * @param context The involved faces context.
	 * @since 3.10
	 */
	public static void clearCachedFacesServletMapping(FacesContext context) {
		context.getAttributes().remove(isMyFacesUsed() ? MYFACES_CACHED_SERVLET_MAPPING_KEY : MOJARRA_CACHED_SERVLET_MAPPING_KEY);
	}

	/**
	 * Returns {@code true} if <code>jakarta.faces:faces.js</code> script resource is available.
	 * @return {@code true} if <code>jakarta.faces:faces.js</code> script resource is available.
	 * @since 4.0
	 */
	public static boolean isFacesScriptResourceAvailable() {
		if (facesScriptResourceAvailable == null) {
			FacesContext context = FacesContext.getCurrentInstance();

			if (context == null) {
				return false;
			}

			facesScriptResourceAvailable = context.getApplication().getResourceHandler().createResource(FACES_SCRIPT_RESOURCE_NAME, JSF_SCRIPT_LIBRARY_NAME) != null;
		}

		return facesScriptResourceAvailable;
	}

	//  JSF state saving related --------------------------------------------------------------------------------------

	/**
	 * Remove server side JSF view state (and view scoped beans) associated with current request.
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
			Object stateId = invokeMethod(stateCache, "getServerStateId", context, state);
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
				Map<String, Integer> viewScopeIdCounts = accessField(viewCollection, "_viewScopeIdCounts");

				if (viewScopeIds == null || viewScopeIdCounts == null || viewScopeIds.get(key) == null) {
					return; // Most likely cached page with client side state saving.
				}

				String viewScopeId = viewScopeIds.remove(key);
				int count = coalesce(viewScopeIdCounts.get(viewScopeId), 1) - 1;

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
			Map<String, Object> serializedViews = getSessionAttribute(context, MOJARRA_SERIALIZED_VIEWS);

			if (serializedViews != null) {
				serializedViews.remove(context.getAttributes().get(MOJARRA_SERIALIZED_VIEW_KEY));
			}

			Map<String, Object> activeViewMaps = getSessionAttribute(context, MOJARRA_ACTIVE_VIEW_MAPS);

			if (activeViewMaps != null) {
				activeViewMaps.remove(context.getViewRoot().getTransientStateHelper().getTransient(MOJARRA_VIEW_MAP_ID));
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

	// JSF component related ------------------------------------------------------------------------------------------

	/**
	 * Returns f:metadata facet from UIViewRoot.
	 * MyFaces 3.x unexpectedly doesn't use {@link UIViewRoot#METADATA_FACET_NAME} anymore to identify the facet.
	 * @param viewRoot The UIViewRoot to obtain f:metadata facet from.
	 * @return f:metadata facet from UIViewRoot.
	 * @since 4.0
	 */
	public static UIComponent getMetadataFacet(UIViewRoot viewRoot) {
		UIComponent metadataFacet = viewRoot.getFacet(UIViewRoot.METADATA_FACET_NAME);

		if (metadataFacet == null && isMyFacesUsed()) {
			metadataFacet = viewRoot.getFacet("UIViewRoot_faces_metadata");
		}

		return metadataFacet;
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
	 * Returns true if the current request is a PrimeFaces ajax request.
	 * @param context The involved faces context.
	 * @return Whether the current request is a PrimeFaces ajax request.
	 * @since 2.7.12
	 */
	public static boolean isPrimeFacesAjaxRequest(FacesContext context) {
		if (!isAjaxRequest(context)) {
			return false;
		}

		UIComponent actionSource = getCurrentActionSource(context, context.getViewRoot());

		if (actionSource == null) {
			return false;
		}

		if (isPrimeFacesClass(actionSource)) {
			return true;
		}

		if (!(actionSource instanceof ClientBehaviorHolder)) {
			return false;
		}

		String ajaxEvent = getRequestParameter(context, BEHAVIOR_EVENT_PARAM_NAME);

		if (ajaxEvent == null) {
			return false;
		}

		ClientBehaviorHolder ajaxSource = (ClientBehaviorHolder) actionSource;

		if (ajaxSource.getClientBehaviors().get(ajaxEvent).stream().anyMatch(Hacks::isPrimeFacesClass)) {
			return true;
		}

		return false;
	}

	private static boolean isPrimeFacesClass(Object object) {
		return object.getClass().getPackage().getName().startsWith(PRIMEFACES_PACKAGE_PREFIX);
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