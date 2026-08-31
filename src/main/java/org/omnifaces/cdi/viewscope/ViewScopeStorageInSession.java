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
import static org.omnifaces.cdi.viewscope.ViewScopeManager.DEFAULT_MAX_ACTIVE_VIEW_SCOPES;
import static org.omnifaces.cdi.viewscope.ViewScopeManager.PARAM_NAME_MAX_ACTIVE_VIEW_SCOPES;
import static org.omnifaces.cdi.viewscope.ViewScopeManager.PARAM_NAME_MOJARRA_NUMBER_OF_VIEWS;
import static org.omnifaces.cdi.viewscope.ViewScopeManager.PARAM_NAME_MYFACES_NUMBER_OF_VIEWS;
import static org.omnifaces.cdi.viewscope.ViewScopeManager.isUnloadRequest;
import static org.omnifaces.util.Faces.getInitParameter;
import static org.omnifaces.util.Faces.getViewMap;
import static org.omnifaces.util.FacesLocal.getRequestParameter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;

import org.omnifaces.cdi.BeanStorage;
import org.omnifaces.cdi.ViewScoped;
import org.omnifaces.util.Beans;
import org.omnifaces.util.cache.LruCache;

/**
 * Stores view scoped bean instances in a LRU map in HTTP session.
 *
 * @author Bauke Scholtz
 * @see ViewScoped
 * @see ViewScopeManager
 * @since 2.6
 */
@SessionScoped
public class ViewScopeStorageInSession implements ViewScopeStorage, Serializable {

    // Private constants ----------------------------------------------------------------------------------------------

    private static final long serialVersionUID = 1L;
    private static final String[] PARAM_NAMES_MAX_ACTIVE_VIEW_SCOPES = {
        PARAM_NAME_MAX_ACTIVE_VIEW_SCOPES, PARAM_NAME_MOJARRA_NUMBER_OF_VIEWS, PARAM_NAME_MYFACES_NUMBER_OF_VIEWS
    };
    private static final String ERROR_MAX_ACTIVE_VIEW_SCOPES = "The '%s' init param must be a number."
        + " Encountered an invalid value of '%s'.";

    // Static variables -----------------------------------------------------------------------------------------------

    private static Integer maxActiveViewScopes;

    // Variables ------------------------------------------------------------------------------------------------------

    private ConcurrentMap<UUID, BeanStorage> activeViewScopes;
    private ConcurrentMap<String, Boolean> recentlyUnloadedViewStates;

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * Create a new LRU map of active view scopes with maximum weighted capacity depending on several context params. See javadoc of {@link ViewScoped} for
     * details.
     */
    @PostConstruct
    public void postConstructSession() {
        activeViewScopes = new LruCache<>(getMaxActiveViewScopes(), (uuid, storage) -> storage.evict());
        recentlyUnloadedViewStates = new LruCache<>(getMaxActiveViewScopes());
    }

    @Override
    public UUID getBeanStorageId() {
        var beanStorageId = (UUID) getViewMap().get(getClass().getName());
        return beanStorageId != null && getBeanStorage(beanStorageId) != null ? beanStorageId : null;
    }

    @Override
    public BeanStorage getBeanStorage(UUID beanStorageId) {
        var activeBeanStorages = getActiveBeanStorages(true);
        var beanStorage = activeBeanStorages.getBeanStorage(beanStorageId);

        if (beanStorage == null) {
            beanStorage = activeViewScopes.get(beanStorageId);

            if (beanStorage != null && !activeBeanStorages.acquire(beanStorageId, beanStorage)) {
                beanStorage = null; // It was concurrently evicted and destroyed, so a new one must be created.
            }
        }

        return beanStorage;
    }

    @Override
    public void setBeanStorage(UUID beanStorageId, BeanStorage beanStorage) {
        getActiveBeanStorages(true).acquire(beanStorageId, beanStorage); // Must happen before it's put in the LRU map, else a concurrent request could
                                                                         // immediately evict and destroy it.
        activeViewScopes.put(beanStorageId, beanStorage);
        getViewMap().put(getClass().getName(), beanStorageId);
    }

    /**
     * Destroys all beans associated with given bean storage identifier.
     *
     * @param context The involved faces context.
     * @param beanStorageId The bean storage identifier.
     */
    public void destroyBeans(FacesContext context, UUID beanStorageId) {
        if (isUnloadRequest(context)) {
            recentlyUnloadedViewStates.put(getRequestParameter(context, VIEW_STATE_PARAM), true);
        }

        var storage = activeViewScopes.get(beanStorageId);

        if (storage != null) {
            storage.destroyBeans();
            activeViewScopes.remove(beanStorageId);
        }

        var activeBeanStorages = getActiveBeanStorages(false);

        if (activeBeanStorages != null) {
            activeBeanStorages.release(beanStorageId); // The view scope is explicitly gone, so the current request must no longer resolve it.
        }
    }

    /**
     * Returns {@code true} if given faces context is recently unloaded.
     *
     * @param context The involved faces context.
     * @return {@code true} if given faces context is recently unloaded.
     * @since 2.7.27
     */
    public boolean isRecentlyUnloaded(FacesContext context) {
        return recentlyUnloadedViewStates.containsKey(getRequestParameter(context, VIEW_STATE_PARAM));
    }

    /**
     * This method is invoked during session destroy, in that case destroy all beans in all active view scopes.
     */
    @PreDestroy
    public void preDestroySession() {
        for (var storage : activeViewScopes.values()) {
            storage.destroyBeans();
        }
    }

    // Helpers --------------------------------------------------------------------------------------------------------

    /**
     * Returns the bean storages which are in use by the current HTTP request, or <code>null</code> when there are none and <code>create</code> is
     * <code>false</code>.
     */
    private static ActiveBeanStorages getActiveBeanStorages(boolean create) {
        return Beans.getInstance(ActiveBeanStorages.class, create);
    }

    /**
     * Returns the max active view scopes depending on available context params. This will be calculated lazily once and re-returned everytime; the faces
     * context is namely not available during class' initialization/construction, but only during a post construct.
     */
    private static int getMaxActiveViewScopes() {
        if (maxActiveViewScopes != null) {
            return maxActiveViewScopes;
        }

        for (var name : PARAM_NAMES_MAX_ACTIVE_VIEW_SCOPES) {
            var value = getInitParameter(name);

            if (value != null) {
                try {
                    maxActiveViewScopes = Integer.valueOf(value);
                    return maxActiveViewScopes;
                }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException(ERROR_MAX_ACTIVE_VIEW_SCOPES.formatted(name, value), e);
                }
            }
        }

        maxActiveViewScopes = DEFAULT_MAX_ACTIVE_VIEW_SCOPES;
        return maxActiveViewScopes;
    }

    // Nested classes -------------------------------------------------------------------------------------------------

    /**
     * Holds the bean storages which are in use by the current HTTP request.
     * <p>
     * The LRU map of active view scopes is bound to a maximum capacity, so a request can have its bean storage evicted by concurrent requests within the same
     * session before it has finished. Acquiring the bean storage for the duration of the request guarantees that the request keeps resolving the same bean
     * storage and that its beans are not prematurely destroyed by the eviction. An evicted bean storage is instead destroyed as soon as the last request using
     * it has finished.
     *
     * @author Bauke Scholtz
     * @see ViewScopeStorageInSession
     * @since 3.14.22
     */
    @RequestScoped
    public static class ActiveBeanStorages {

        private final Map<UUID, BeanStorage> beanStorages = new HashMap<>();

        /**
         * Returns the bean storage which the current HTTP request has acquired under the given bean storage identifier, or <code>null</code> if there is none.
         *
         * @param beanStorageId The bean storage identifier.
         * @return The acquired bean storage, or <code>null</code> if there is none.
         */
        protected BeanStorage getBeanStorage(UUID beanStorageId) {
            return beanStorages.get(beanStorageId);
        }

        /**
         * Acquires the given bean storage for the duration of the current HTTP request. This is a no-op when it has already been acquired by the current HTTP
         * request.
         *
         * @param beanStorageId The bean storage identifier.
         * @param beanStorage The bean storage.
         * @return <code>false</code> when the beans of the given bean storage have meanwhile been destroyed by a concurrent eviction, in which case a new bean
         * storage must be created.
         */
        protected boolean acquire(UUID beanStorageId, BeanStorage beanStorage) {
            if (beanStorages.containsKey(beanStorageId)) {
                return true;
            }

            if (!beanStorage.acquire()) {
                return false;
            }

            beanStorages.put(beanStorageId, beanStorage);
            return true;
        }

        /**
         * Releases the bean storage identified by the given bean storage identifier, if the current HTTP request has acquired it. This is invoked when the view
         * scope is explicitly destroyed, such as during an unload or a navigation, so that the current HTTP request will no longer resolve it.
         *
         * @param beanStorageId The bean storage identifier.
         */
        protected void release(UUID beanStorageId) {
            var beanStorage = beanStorages.remove(beanStorageId);

            if (beanStorage != null) {
                beanStorage.release();
            }
        }

        /**
         * When the current HTTP request is about to be destroyed, release all bean storages which it still has acquired.
         */
        @PreDestroy
        protected void preDestroyRequest() {
            beanStorages.values().forEach(BeanStorage::release);
            beanStorages.clear();
        }

    }

}
