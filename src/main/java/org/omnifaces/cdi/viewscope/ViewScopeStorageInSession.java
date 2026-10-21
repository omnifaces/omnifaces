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

import static java.lang.String.format;
import static javax.faces.event.PhaseId.ANY_PHASE;
import static javax.faces.event.PhaseId.RENDER_RESPONSE;
import static javax.faces.render.ResponseStateManager.VIEW_STATE_PARAM;
import static org.omnifaces.cdi.viewscope.ViewScopeManager.DEFAULT_MAX_ACTIVE_VIEW_SCOPES;
import static org.omnifaces.cdi.viewscope.ViewScopeManager.PARAM_NAME_MAX_ACTIVE_VIEW_SCOPES;
import static org.omnifaces.cdi.viewscope.ViewScopeManager.PARAM_NAME_MOJARRA_NUMBER_OF_VIEWS;
import static org.omnifaces.cdi.viewscope.ViewScopeManager.PARAM_NAME_MYFACES_NUMBER_OF_VIEWS;
import static org.omnifaces.cdi.viewscope.ViewScopeManager.isUnloadRequest;
import static org.omnifaces.util.Events.subscribeToRequestAfterPhase;
import static org.omnifaces.util.Faces.getInitParameter;
import static org.omnifaces.util.Faces.getViewRoot;
import static org.omnifaces.util.FacesLocal.getRequestParameter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.TransientStateHelper;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;

import org.omnifaces.cdi.BeanStorage;
import org.omnifaces.cdi.ViewScoped;
import org.omnifaces.util.Beans;
import org.omnifaces.util.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import org.omnifaces.util.concurrentlinkedhashmap.EvictionListener;

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
	private static final String BEAN_STORAGE_ID = ViewScopeStorageInSession.class.getName();

	// Static variables -----------------------------------------------------------------------------------------------

	private static Integer maxActiveViewScopes;

	// Variables ------------------------------------------------------------------------------------------------------

	private ConcurrentMap<UUID, BeanStorage> activeViewScopes;
	private ConcurrentMap<String, Boolean> recentlyUnloadedViewStates;

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Create a new LRU map of active view scopes with maximum weighted capacity depending on several context params.
	 * See javadoc of {@link ViewScoped} for details.
	 */
	@PostConstruct
	public void postConstructSession() {
		activeViewScopes = new ConcurrentLinkedHashMap.Builder<UUID, BeanStorage>()
			.maximumWeightedCapacity(getMaxActiveViewScopes())
			.listener(new BeanStorageEvictionListener())
			.build();
		recentlyUnloadedViewStates = new ConcurrentLinkedHashMap.Builder<String, Boolean>()
			.maximumWeightedCapacity(getMaxActiveViewScopes())
			.build();
	}

	@Override
	public UUID getBeanStorageId() {
		UUID beanStorageId = getViewBeanStorageId();
		return beanStorageId != null && getBeanStorage(beanStorageId) != null ? beanStorageId : null;
	}

	@Override
	public BeanStorage getBeanStorage(UUID beanStorageId) {
		ActiveBeanStorages activeBeanStorages = getActiveBeanStorages(true);
		BeanStorage beanStorage = activeBeanStorages.getBeanStorage(beanStorageId);

		if (beanStorage == null) {
			beanStorage = activeViewScopes.get(beanStorageId);

			if (beanStorage != null && !activeBeanStorages.acquire(beanStorageId, beanStorage)) {
				beanStorage = null; // It was concurrently evicted and destroyed, so a new one must be created.
			}
		}

		return beanStorage;
	}

	/**
	 * Sets the given bean storage as the one of the current view. It is only put in the LRU map of active view scopes
	 * when the state of the current view is saved, see {@link ViewScopeUnloadScriptRenderer} and
	 * {@link ViewScopeStateManager}. Until then it is pending in the current HTTP request, and when the JSF lifecycle of
	 * the current HTTP request ends without saving the state of the current view, then its beans are destroyed, because
	 * the client never received a view on which the unload script could destroy them.
	 */
	@Override
	public void setBeanStorage(UUID beanStorageId, BeanStorage beanStorage) {
		getActiveBeanStorages(true).acquirePending(beanStorageId, beanStorage);
		getViewRoot().getTransientStateHelper().putTransient(BEAN_STORAGE_ID, beanStorageId);
	}

	/**
	 * Puts the bean storage of the current view, if it is still pending, in the LRU map of active view scopes and
	 * remembers its identifier in the view scope. This must be invoked right before the state of the current view is
	 * saved, so that the view scope is saved along with it. This is invoked by {@link ViewScopeUnloadScriptRenderer}
	 * when the unload script is rendered at the end of the body, and by
	 * {@link ViewScopeStateManager#getViewState(FacesContext)} when the view state is written into an ajax response.
	 * @param context The involved faces context.
	 */
	static void activate(FacesContext context) {
		UIViewRoot viewRoot = context.getViewRoot();

		if (viewRoot == null || viewRoot.isTransient()) {
			return;
		}

		UUID beanStorageId = (UUID) viewRoot.getTransientStateHelper().getTransient(BEAN_STORAGE_ID);

		if (beanStorageId != null) {
			ViewScopeStorageInSession instance = Beans.getInstance(ViewScopeStorageInSession.class, false);

			if (instance != null) {
				instance.activate(viewRoot, beanStorageId);
			}
		}
	}

	private void activate(UIViewRoot viewRoot, UUID beanStorageId) {
		ActiveBeanStorages activeBeanStorages = getActiveBeanStorages(false);
		BeanStorage beanStorage = activeBeanStorages != null ? activeBeanStorages.activate(beanStorageId) : null;

		if (beanStorage != null) {
			activeViewScopes.put(beanStorageId, beanStorage);
			viewRoot.getViewMap().put(BEAN_STORAGE_ID, beanStorageId);
		}
	}

	/**
	 * Destroys all beans associated with given bean storage identifier. When a concurrent HTTP request is still using
	 * them, then they are destroyed as soon as the last one has finished. See {@link ActiveBeanStorages}.
     * @param context The involved faces context.
	 * @param beanStorageId The bean storage identifier.
	 */
	public void destroyBeans(FacesContext context, UUID beanStorageId) {
		if (isUnloadRequest(context)) {
			recentlyUnloadedViewStates.put(getRequestParameter(context, VIEW_STATE_PARAM), true);
		}

		ActiveBeanStorages activeBeanStorages = getActiveBeanStorages(false);
		BeanStorage storage = activeBeanStorages != null ? activeBeanStorages.getBeanStorage(beanStorageId) : null;

		if (storage == null) {
			storage = activeViewScopes.get(beanStorageId);
		}

		if (storage != null) {
			storage.evict();
		}

		if (activeBeanStorages != null) {
			activeBeanStorages.release(beanStorageId); // The view scope is explicitly gone, so the current request must no longer resolve it.
		}

		activeViewScopes.remove(beanStorageId);
	}

	/**
	 * Returns {@code true} if given faces context is recently unloaded.
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
		for (BeanStorage storage : activeViewScopes.values()) {
			storage.destroyBeans();
		}
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the bean storage identifier of the current view. It is remembered in the transient state of the view root
	 * for the duration of the current HTTP request, so that the current HTTP request keeps resolving it when the view map
	 * is concurrently destroyed. On MyFaces, performing the pending view state removal of an unloaded view destroys its
	 * view map.
	 */
	private UUID getViewBeanStorageId() {
		UIViewRoot viewRoot = getViewRoot();
		TransientStateHelper transientState = viewRoot.getTransientStateHelper();
		UUID beanStorageId = (UUID) transientState.getTransient(BEAN_STORAGE_ID);

		if (beanStorageId == null) {
			Map<String, Object> viewMap = viewRoot.getViewMap(false); // Don't create the view scope for nothing.
			beanStorageId = viewMap != null ? (UUID) viewMap.get(BEAN_STORAGE_ID) : null;

			if (beanStorageId != null) {
				transientState.putTransient(BEAN_STORAGE_ID, beanStorageId);
			}
		}

		return beanStorageId;
	}

	/**
	 * Returns the bean storages which are in use by the current HTTP request, or <code>null</code> when there are none
	 * and <code>create</code> is <code>false</code>.
	 */
	private static ActiveBeanStorages getActiveBeanStorages(boolean create) {
		return Beans.getInstance(ActiveBeanStorages.class, create);
	}

	/**
	 * Returns the max active view scopes depending on available context params. This will be calculated lazily once
	 * and re-returned everytime; the faces context is namely not available during class' initialization/construction,
	 * but only during a post construct.
	 */
	private static int getMaxActiveViewScopes() {
		if (maxActiveViewScopes != null) {
			return maxActiveViewScopes;
		}

		for (String name : PARAM_NAMES_MAX_ACTIVE_VIEW_SCOPES) {
			String value = getInitParameter(name);

			if (value != null) {
				try {
					maxActiveViewScopes = Integer.valueOf(value);
					return maxActiveViewScopes;
				}
				catch (NumberFormatException e) {
					throw new IllegalArgumentException(format(ERROR_MAX_ACTIVE_VIEW_SCOPES, name, value), e);
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
	 * The LRU map of active view scopes is bound to a maximum capacity, so a request can have its bean storage evicted
	 * by concurrent requests within the same session before it has finished. Acquiring the bean storage for the
	 * duration of the request guarantees that the request keeps resolving the same bean storage and that its beans are
	 * not prematurely destroyed by the eviction. An evicted bean storage is instead destroyed as soon as the last
	 * request using it has finished.
	 *
	 * @author Bauke Scholtz
	 * @see ViewScopeStorageInSession
	 * @since 3.14.22
	 */
	@RequestScoped
	protected static class ActiveBeanStorages {

		private final Map<UUID, BeanStorage> beanStorages = new HashMap<>();
		private final Set<UUID> pendingBeanStorageIds = new HashSet<>();
		private boolean destroyPendingWhenLifecycleEndsSubscribed;

		/**
		 * Returns the bean storage which the current HTTP request has acquired under the given bean storage
		 * identifier, or <code>null</code> if there is none.
		 * @param beanStorageId The bean storage identifier.
		 * @return The acquired bean storage, or <code>null</code> if there is none.
		 */
		protected BeanStorage getBeanStorage(UUID beanStorageId) {
			return beanStorages.get(beanStorageId);
		}

		/**
		 * Acquires the given bean storage for the duration of the current HTTP request. This is a no-op when it has
		 * already been acquired by the current HTTP request.
		 * @param beanStorageId The bean storage identifier.
		 * @param beanStorage The bean storage.
		 * @return <code>false</code> when the beans of the given bean storage have meanwhile been destroyed by a
		 * concurrent eviction, in which case a new bean storage must be created.
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
		 * Acquires the given new bean storage for the duration of the current HTTP request, and marks it as pending
		 * until {@link #activate(UUID)}. When the JSF lifecycle of the current HTTP request ends while it is still
		 * pending, then its beans are destroyed.
		 * @param beanStorageId The bean storage identifier.
		 * @param beanStorage The new bean storage.
		 * @since 3.14.28
		 */
		protected void acquirePending(UUID beanStorageId, BeanStorage beanStorage) {
			if (!destroyPendingWhenLifecycleEndsSubscribed) {
				subscribeToRequestAfterPhase(ANY_PHASE, this::destroyPendingWhenLifecycleEnds);
				destroyPendingWhenLifecycleEndsSubscribed = true;
			}

			beanStorage.acquire();
			beanStorages.put(beanStorageId, beanStorage);
			pendingBeanStorageIds.add(beanStorageId);
		}

		/**
		 * Marks the bean storage identified by the given bean storage identifier as no longer pending. The returned bean
		 * storage is already acquired by the current HTTP request, so it is safe to put it in the LRU map, where a
		 * concurrent request could immediately evict it.
		 * @param beanStorageId The bean storage identifier.
		 * @return The bean storage if it was pending, else <code>null</code>.
		 * @since 3.14.28
		 */
		protected BeanStorage activate(UUID beanStorageId) {
			return pendingBeanStorageIds.remove(beanStorageId) ? beanStorages.get(beanStorageId) : null;
		}

		/**
		 * Releases the bean storage identified by the given bean storage identifier, if the current HTTP request has
		 * acquired it. This is invoked when the view scope is explicitly destroyed, such as during an unload or a
		 * navigation, so that the current HTTP request will no longer resolve it.
		 * @param beanStorageId The bean storage identifier.
		 */
		protected void release(UUID beanStorageId) {
			BeanStorage beanStorage = beanStorages.get(beanStorageId);

			if (beanStorage != null) {
				beanStorage.release();
				beanStorages.remove(beanStorageId);
			}

			pendingBeanStorageIds.remove(beanStorageId);
		}

		/**
		 * When the current HTTP request is about to be destroyed, destroy the beans of all bean storages which are
		 * still pending, and release all bean storages which it still has acquired. Pending bean storages are normally
		 * already destroyed at the end of the JSF lifecycle, but not when it was aborted by an exception, nor when they
		 * were acquired after it had ended.
		 */
		@PreDestroy
		protected void preDestroyRequest() {
			destroyPending();
			beanStorages.values().forEach(BeanStorage::release);
			beanStorages.clear();
		}

		/**
		 * The JSF lifecycle ends after the render response, or else after the phase in which the response was
		 * completed, such as by a redirect or a file download. The beans are then destroyed while the faces context is
		 * still available to their pre destroy methods.
		 */
		private void destroyPendingWhenLifecycleEnds(PhaseEvent event) {
			if (event.getPhaseId() == RENDER_RESPONSE || event.getFacesContext().getResponseComplete()) {
				destroyPending();
			}
		}

		/**
		 * The bean storage stays resolvable while its beans are destroyed, so that their pre destroy methods can still
		 * reach other view scoped beans of the same view, as long as the current view is still the view of the bean
		 * storage, which is not the case after a forward navigation.
		 */
		private void destroyPending() {
			for (UUID beanStorageId : new ArrayList<>(pendingBeanStorageIds)) {
				beanStorages.get(beanStorageId).evict();
				release(beanStorageId);
			}
		}

	}

	/**
	 * Listener for {@link ConcurrentLinkedHashMap} which will be invoked when an entry is evicted. It will in turn
	 * invoke {@link BeanStorage#evict()}, which destroys the beans only when no unfinished HTTP request is still using
	 * the bean storage. See {@link ActiveBeanStorages}.
	 */
	private static final class BeanStorageEvictionListener implements EvictionListener<UUID, BeanStorage>, Serializable {

		private static final long serialVersionUID = 1L;

		@Override
		public void onEviction(UUID id, BeanStorage storage) {
			storage.evict();
		}

	}

}