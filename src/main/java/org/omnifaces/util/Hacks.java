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

import static jakarta.faces.application.ResourceHandler.JSF_SCRIPT_LIBRARY_NAME;
import static jakarta.faces.component.behavior.ClientBehaviorContext.BEHAVIOR_EVENT_PARAM_NAME;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.omnifaces.resourcehandler.DefaultResourceHandler.FACES_SCRIPT_RESOURCE_NAME;
import static org.omnifaces.util.Components.getClosestParent;
import static org.omnifaces.util.ComponentsLocal.getCurrentActionSource;
import static org.omnifaces.util.FacesLocal.getApplicationAttribute;
import static org.omnifaces.util.FacesLocal.getInitParameter;
import static org.omnifaces.util.FacesLocal.getPackage;
import static org.omnifaces.util.FacesLocal.getRequestParameter;
import static org.omnifaces.util.FacesLocal.getSessionAttribute;
import static org.omnifaces.util.FacesLocal.isAjaxRequest;
import static org.omnifaces.util.FacesLocal.isRenderResponse;
import static org.omnifaces.util.FacesLocal.normalizeViewId;
import static org.omnifaces.util.Reflection.accessField;
import static org.omnifaces.util.Reflection.invokeMethod;
import static org.omnifaces.util.Reflection.toClassOrNull;
import static org.omnifaces.util.Utils.coalesce;
import static org.omnifaces.util.Utils.stream;
import static org.omnifaces.util.Utils.unmodifiableSet;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.el.VariableMapper;
import jakarta.faces.FacesWrapper;
import jakarta.faces.application.ResourceHandler;
import jakarta.faces.component.StateHelper;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.component.behavior.ClientBehaviorHolder;
import jakarta.faces.context.FacesContext;
import jakarta.faces.render.ResponseStateManager;
import jakarta.websocket.Session;

/**
 * <p>
 * Collection of Faces implementation and/or Faces component library and/or server specific hacks.
 *
 * <h2>This class is not listed in showcase! Should I use it?</h2>
 * <p>
 * This class is indeed intended for internal usage only. We won't add methods here on user request. We only add methods
 * here once we encounter non-DRY code in OmniFaces codebase. The methods may be renamed/changed without notice.
 * <p>
 * We don't stop you from using it if you found it in the Javadoc and you think you find it useful, but you have to
 * accept the risk that the method signatures can be changed without notice. This utility class exists because OmniFaces
 * intends to be free of 3rd party dependencies.
 *
 * @author Bauke Scholtz
 * @author Arjan Tijms
 * @since 1.3
 */
public final class Hacks {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final Class<?> PRIMEFACES_AJAX_SOURCE_CLASS =
        toClassOrNull("org.primefaces.component.api.AjaxSource");
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
    private static final Set<String> MYFACES_SERIALIZED_VIEWS =
        unmodifiableSet(
            "org.apache.myfaces.application.viewstate.ServerSideStateCacheImpl.SERIALIZED_VIEW", // MyFaces 2.3.9
            "org.apache.myfaces.application.viewstate.StateCacheServerSide.SERIALIZED_VIEW"); // MyFaces 2.3-next-M6
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

    // Faces impl related ---------------------------------------------------------------------------------------------

    /**
     * Returns true if Mojarra is used. That is, when the FacesContext instance is from the Mojarra specific package.
     * @return Whether Mojarra is used.
     * @since 3.9
     */
    public static boolean isMojarraUsed() {
        if (mojarraUsed == null) {
            var context = FacesContext.getCurrentInstance();

            if (context != null) {
                mojarraUsed = getPackage(context).getName().startsWith(MOJARRA_PACKAGE_PREFIX);
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
            var context = FacesContext.getCurrentInstance();

            if (context != null) {
                myFacesUsed = getPackage(context).getName().startsWith(MYFACES_PACKAGE_PREFIX);
            }
            else {
                return false;
            }
        }

        return myFacesUsed;
    }

    // Faces resource handling related --------------------------------------------------------------------------------

    /**
     * Returns the default resource maximum age in milliseconds.
     * @return The default resource maximum age in milliseconds.
     */
    public static long getDefaultResourceMaxAge() {
        if (defaultResourceMaxAge == null) {
            var resourceMaxAge = DEFAULT_RESOURCE_MAX_AGE;
            var context = FacesContext.getCurrentInstance();

            if (context == null) {
                return resourceMaxAge;
            }

            for (var name : PARAM_NAMES_RESOURCE_MAX_AGE) {
                var value = getInitParameter(context, name);

                if (value != null) {
                    try {
                        resourceMaxAge = Long.parseLong(value);
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
            context.getAttributes().remove(ResourceHandler.RESOURCE_IDENTIFIER);
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
        var view = context.getViewRoot();

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
            var context = FacesContext.getCurrentInstance();

            if (context == null) {
                return false;
            }

            facesScriptResourceAvailable = context.getApplication().getResourceHandler().createResource(FACES_SCRIPT_RESOURCE_NAME, JSF_SCRIPT_LIBRARY_NAME) != null;
        }

        return facesScriptResourceAvailable;
    }

    //  Faces state saving related ------------------------------------------------------------------------------------

    /**
     * Remove server side Faces view state (and view scoped beans) associated with current request.
     * @param context The involved faces context.
     * @param manager The involved response state manager.
     * @param viewId The view ID of the involved view.
     * @since 2.3
     */
    public static void removeViewState(FacesContext context, ResponseStateManager manager, String viewId) {
        if (isMyFacesUsed()) {
            var state = invokeMethod(manager, "getSavedState", context);

            if (!(state instanceof String)) {
                return;
            }

            var viewCollection = MYFACES_SERIALIZED_VIEWS.stream().map(k -> getSessionAttribute(context, k)).filter(Objects::nonNull).findFirst().orElse(null);

            if (viewCollection == null) {
                return;
            }

            var stateCache = invokeMethod(manager, "getStateCache", context);
            var stateId = invokeMethod(stateCache, "getServerStateId", context, state);
            var key = invokeMethod(invokeMethod(stateCache, "getSessionViewStorageFactory"), "createSerializedViewKey", context, normalizeViewId(context, viewId), stateId);

            List<Serializable> keys = accessField(viewCollection, "_keys");
            Map<Serializable, Object> serializedViews = accessField(viewCollection, "_serializedViews");
            Map<Serializable, Serializable> precedence = accessField(viewCollection, "_precedence");

            synchronized (viewCollection) { // Those fields are not concurrent maps.
                keys.remove(key);
                serializedViews.remove(key);
                var previousKey = precedence.remove(key);

                if (previousKey != null) {
                    for (var entry : precedence.entrySet()) {
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

                var viewScopeId = viewScopeIds.remove(key);
                var count = coalesce(viewScopeIdCounts.get(viewScopeId), 1) - 1;

                if (count < 1) {
                    viewScopeIdCounts.remove(viewScopeId);
                    var viewScopeProvider = getApplicationAttribute(context, MYFACES_VIEW_SCOPE_PROVIDER);

                    if (viewScopeProvider != null) { // This was removed in MyFaces 4.x and leveraged to CDI, see #729.
                        invokeMethod(viewScopeProvider, "destroyViewScopeMap", context, viewScopeId);
                    }
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

    // Faces component related ----------------------------------------------------------------------------------------

    /**
     * Returns f:metadata facet from UIViewRoot.
     * MyFaces 3.x unexpectedly doesn't use {@link UIViewRoot#METADATA_FACET_NAME} anymore to identify the facet.
     * @param viewRoot The UIViewRoot to obtain f:metadata facet from.
     * @return f:metadata facet from UIViewRoot.
     * @since 4.0
     */
    public static UIComponent getMetadataFacet(UIViewRoot viewRoot) {
        var metadataFacet = viewRoot.getFacet(UIViewRoot.METADATA_FACET_NAME);

        if (metadataFacet == null && isMyFacesUsed()) {
            metadataFacet = viewRoot.getFacet("UIViewRoot_faces_metadata");
        }

        return metadataFacet;
    }


    // EL related -----------------------------------------------------------------------------------------------------

    /**
     * Finds the wrapped variable mapper of the given variable mapper.
     * @param mapper The variable mapper to find wrapped variable mapper for.
     * @return The wrapped variable mapper of the given variable mapper.
     * @since 3.14.4
     */
    @SuppressWarnings("unchecked")
    public static VariableMapper findWrappedVariableMapper(VariableMapper mapper) {
        if (mapper instanceof FacesWrapper) { // MyFaces
            return ((FacesWrapper<VariableMapper>) mapper).getWrapped();
        }
        else { // Mojarra
            return accessField(mapper, VariableMapper.class);
        }
    }


    // PrimeFaces related ---------------------------------------------------------------------------------------------

    /**
     * Returns true if the current request is a PrimeFaces dynamic resource request.
     * @param context The involved faces context.
     * @return Whether the current request is a PrimeFaces dynamic resource request.
     * @since 1.8
     */
    public static boolean isPrimeFacesDynamicResourceRequest(FacesContext context) {
        var params = context.getExternalContext().getRequestParameterMap();
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

        var actionSource = getCurrentActionSource(context);

        if (actionSource == null) {
            return false;
        }

        if (isPrimeFacesAjaxSource(actionSource)) {
            return true;
        }

        if (!(actionSource instanceof ClientBehaviorHolder)) {
            return false;
        }

        var ajaxEvent = getRequestParameter(context, BEHAVIOR_EVENT_PARAM_NAME);

        if (ajaxEvent == null) {
            return false;
        }

        var ajaxSource = (ClientBehaviorHolder) actionSource;
        return ajaxSource.getClientBehaviors().get(ajaxEvent).stream().anyMatch(Hacks::isPrimeFacesAjaxSource);
    }

    private static boolean isPrimeFacesAjaxSource(Object object) {
        return PRIMEFACES_AJAX_SOURCE_CLASS != null && PRIMEFACES_AJAX_SOURCE_CLASS.isInstance(object);
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

        var dialogs = stream(components).map(component -> getClosestParent(component, PRIMEFACES_DIALOG_CLASS)).collect(toSet());
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