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
package org.omnifaces.cdi.viewscope;

import static jakarta.faces.render.ResponseStateManager.VIEW_STATE_PARAM;
import static java.lang.String.format;
import static java.util.logging.Level.FINEST;
import static org.omnifaces.config.OmniFaces.OMNIFACES_EVENT_PARAM_NAME;
import static org.omnifaces.config.OmniFaces.OMNIFACES_LIBRARY_NAME;
import static org.omnifaces.config.OmniFaces.OMNIFACES_SCRIPT_NAME;
import static org.omnifaces.util.BeansLocal.getInstance;
import static org.omnifaces.util.ComponentsLocal.addFacesScriptResource;
import static org.omnifaces.util.ComponentsLocal.addFormIfNecessary;
import static org.omnifaces.util.ComponentsLocal.addScript;
import static org.omnifaces.util.ComponentsLocal.addScriptResource;
import static org.omnifaces.util.Faces.getContext;
import static org.omnifaces.util.Faces.getViewId;
import static org.omnifaces.util.FacesLocal.getRequest;
import static org.omnifaces.util.FacesLocal.getRequestParameter;
import static org.omnifaces.util.FacesLocal.isAjaxRequestWithPartialRendering;
import static org.omnifaces.util.FacesLocal.isPostback;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.faces.application.ViewExpiredException;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

import org.omnifaces.cdi.BeanStorage;
import org.omnifaces.cdi.ViewScoped;
import org.omnifaces.util.cache.LruCache;

/**
 * Manages view scoped bean creation and destroy. The creation is initiated by {@link ViewScopeContext} which is
 * registered by {@link ViewScopeExtension} and the destroy is initiated by {@link ViewScopeEventListener} which is
 * registered in <code>faces-config.xml</code>.
 * <p>
 * Depending on {@link ViewScoped#saveInViewState()}, this view scope manager will delegate the creation and destroy
 * further to either {@link ViewScopeStorageInSession} or {@link ViewScopeStorageInViewState} which saves the concrete
 * bean instances in respectively HTTP session or Faces view state.
 *
 * @author Radu Creanga {@literal <rdcrng@gmail.com>}
 * @author Bauke Scholtz
 * @see ViewScoped
 * @see ViewScopeContext
 * @since 1.6
 */
@ApplicationScoped
public class ViewScopeManager {

    // Public constants -----------------------------------------------------------------------------------------------

    /** OmniFaces specific context parameter name of maximum active view scopes in session. */
    public static final String PARAM_NAME_MAX_ACTIVE_VIEW_SCOPES =
        "org.omnifaces.VIEW_SCOPE_MANAGER_MAX_ACTIVE_VIEW_SCOPES";

    /** Mojarra specific context parameter name of maximum number of logical views in session. */
    public static final String PARAM_NAME_MOJARRA_NUMBER_OF_VIEWS =
        "com.sun.faces.numberOfLogicalViews";

    /** MyFaces specific context parameter name of maximum number of views in session. */
    public static final String PARAM_NAME_MYFACES_NUMBER_OF_VIEWS =
        "org.apache.myfaces.NUMBER_OF_VIEWS_IN_SESSION";

    /** Default value of maximum active view scopes in session. */
    public static final int DEFAULT_MAX_ACTIVE_VIEW_SCOPES = 20; // Mojarra's default is 15 and MyFaces' default is 20.

    // Private constants ----------------------------------------------------------------------------------------------

    private static final Logger logger = Logger.getLogger(ViewScopeManager.class.getName());

    private static final String SCRIPT_INIT = "OmniFaces.Unload.init('%s')";
    private static final int DEFAULT_BEANS_PER_VIEW_SCOPE = 3;

    private static final String WARNING_UNSUPPORTED_STATE_SAVING = "@ViewScoped %s"
            + " requires non-stateless views in order to be able to properly destroy the bean."
            + " The current view %s is stateless and this may cause memory leaks."
            + " Consider subclassing the bean with @jakarta.faces.view.ViewScoped annotation.";

    private static final String ERROR_INVALID_STATE_SAVING = "@ViewScoped(saveInViewState=true) %s"
            + " requires web.xml context parameter 'jakarta.faces.STATE_SAVING_METHOD' being set to 'client'.";

    private static final String ERROR_VIEW_ALREADY_UNLOADED = "View %s was already unloaded.";

    // Variables ------------------------------------------------------------------------------------------------------

    @Inject
    private BeanManager manager;

    @Inject
    private ViewScopeStorageInSession storageInSession;

    @Inject
    private ViewScopeStorageInViewState storageInViewState;

    private Map<String, Boolean> recentlyDestroyedViewStates = new LruCache<>(DEFAULT_MAX_ACTIVE_VIEW_SCOPES);

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * Returns the CDI view scoped managed bean from the current Faces view scope, and auto-creates one if it doesn't exist.
     * @param <T> The expected return type.
     * @param type The contextual type of the CDI managed bean.
     * @param context The CDI context to create the CDI managed bean in.
     * @return The auto-created CDI view scoped managed bean from the current Faces view scope.
     * @since 4.5
     */
    public <T> T getBean(Contextual<T> type, CreationalContext<T> context) {
        return getBeanStorage(type).getBean(type, context);
    }

    /**
     * Returns the CDI view scoped managed bean from the current Faces view scope, or <code>null</code> if there is none.
     * @param <T> The expected return type.
     * @param type The contextual type of the CDI managed bean.
     * @return The CDI view scoped managed bean from the current Faces view scope, or <code>null</code> if there is none.
     */
    public <T> T getBean(Contextual<T> type) {
        return getBeanStorage(type).getBean(type);
    }

    /**
     * This method is invoked during view destroy by {@link ViewScopeEventListener}, in that case destroy all beans in
     * current active view scope.
     */
    public void preDestroyView() {
        var context = getContext();
        UUID beanStorageId = null;

        if (isUnloadRequest(context)) {
            try {
                beanStorageId = UUID.fromString(getRequestParameter(context, "id"));
            }
            catch (Exception ignore) {
                logger.log(FINEST, "Ignoring thrown exception; this can only be a hacker attempt.", ignore);
                return;
            }

            recentlyDestroyedViewStates.put(getRequestParameter(context, VIEW_STATE_PARAM), true);
        }
        else if (isAjaxRequestWithPartialRendering(context)) {
            context.getApplication().getResourceHandler().markResourceRendered(context, OMNIFACES_SCRIPT_NAME, OMNIFACES_LIBRARY_NAME); // Otherwise MyFaces will load a new one during createViewScope() when still in same document (e.g. navigation).
        }

        if (getInstance(manager, ViewScopeStorageInSession.class, false) != null) { // Avoid unnecessary session creation when accessing storageInSession for nothing.
            if (beanStorageId == null) {
                beanStorageId = storageInSession.getBeanStorageId();
            }

            if (beanStorageId != null) {
                storageInSession.destroyBeans(beanStorageId);
            }
        }

        // View scoped beans stored in client side Faces view state are per definition undestroyable, therefore storageInViewState is ignored here.
    }

    // Helpers --------------------------------------------------------------------------------------------------------

    private <T> BeanStorage getBeanStorage(Contextual<T> type) {
        ViewScopeStorage storage = storageInSession;
        var beanClass = ((Bean<T>) type).getBeanClass();
        var annotation = beanClass.getAnnotation(ViewScoped.class);

        if (annotation != null && annotation.saveInViewState()) { // Can be null when declared on producer method.
            checkStateSavingMethod(beanClass);
            storage = storageInViewState;
        }

        var beanStorageId = storage.getBeanStorageId();

        if (beanStorageId == null) {
            beanStorageId = UUID.randomUUID();

            if (storage instanceof ViewScopeStorageInSession) {
                var context = getContext();
                if (context.getViewRoot().isTransient()) {
                    logger.log(Level.WARNING, format(WARNING_UNSUPPORTED_STATE_SAVING, beanClass.getName(), getViewId()));
                }
                else {
                    registerUnloadScript(context, beanStorageId);
                }
            }
        }

        var beanStorage = storage.getBeanStorage(beanStorageId);

        if (beanStorage == null) {
            var context = getContext();

            if (isPostback(context) && recentlyDestroyedViewStates.containsKey(getRequestParameter(context, VIEW_STATE_PARAM))) {
                var viewId = context.getViewRoot().getViewId();
                throw new ViewExpiredException(format(ERROR_VIEW_ALREADY_UNLOADED, viewId), viewId);
            }

            beanStorage = new BeanStorage(DEFAULT_BEANS_PER_VIEW_SCOPE);
            storage.setBeanStorage(beanStorageId, beanStorage);
        }

        return beanStorage;
    }

    private static void checkStateSavingMethod(Class<?> beanClass) {
        var context = getContext();

        if (!context.getApplication().getStateManager().isSavingStateInClient(context)) {
            throw new IllegalStateException(format(ERROR_INVALID_STATE_SAVING, beanClass.getName()));
        }
    }

    /**
     * Register unload script.
     */
    private static void registerUnloadScript(FacesContext context, UUID beanStorageId) {
        addFormIfNecessary(context); // Required to get view state ID.
        addFacesScriptResource(context); // Ensure it's always included BEFORE omnifaces.js.
        addScriptResource(context, OMNIFACES_LIBRARY_NAME, OMNIFACES_SCRIPT_NAME);
        addScript(context, format(SCRIPT_INIT, beanStorageId));
    }

    /**
     * Returns <code>true</code> if the current request is triggered by an unload request.
     * @param context The involved faces context.
     * @return <code>true</code> if the current request is triggered by an unload request.
     * @since 2.2
     */
    public static boolean isUnloadRequest(FacesContext context) {
        return isUnloadRequest(getRequest(context));
    }

    /**
     * Returns <code>true</code> if the given request is triggered by an unload request.
     * @param request The involved request.
     * @return <code>true</code> if the given request is triggered by an unload request.
     * @since 3.1
     */
    public static boolean isUnloadRequest(HttpServletRequest request) {
        return "unload".equals(request.getParameter(OMNIFACES_EVENT_PARAM_NAME));
    }

}