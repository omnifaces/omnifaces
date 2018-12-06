/*
 * Copyright 2018 OmniFaces
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
package org.omnifaces.cdi.viewscope;

import static java.lang.String.format;
import static org.omnifaces.cdi.viewscope.ViewScopeManager.DEFAULT_MAX_ACTIVE_VIEW_SCOPES;
import static org.omnifaces.cdi.viewscope.ViewScopeManager.PARAM_NAME_MAX_ACTIVE_VIEW_SCOPES;
import static org.omnifaces.cdi.viewscope.ViewScopeManager.PARAM_NAME_MOJARRA_NUMBER_OF_VIEWS;
import static org.omnifaces.cdi.viewscope.ViewScopeManager.PARAM_NAME_MYFACES_NUMBER_OF_VIEWS;
import static org.omnifaces.util.Faces.getInitParameter;
import static org.omnifaces.util.Faces.getViewAttribute;
import static org.omnifaces.util.Faces.setViewAttribute;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.SessionScoped;

import org.omnifaces.cdi.BeanStorage;
import org.omnifaces.cdi.ViewScoped;
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

	// Static variables -----------------------------------------------------------------------------------------------

	private static volatile Integer maxActiveViewScopes;

	// Variables ------------------------------------------------------------------------------------------------------

	private ConcurrentMap<UUID, BeanStorage> activeViewScopes;

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
	}

	@Override
	public UUID getBeanStorageId() {
		UUID beanStorageId = getViewAttribute(getClass().getName());
		return (beanStorageId != null && activeViewScopes.containsKey(beanStorageId)) ? beanStorageId : null;
	}

	@Override
	public BeanStorage getBeanStorage(UUID beanStorageId) {
		return activeViewScopes.get(beanStorageId);
	}

	@Override
	public void setBeanStorage(UUID beanStorageId, BeanStorage beanStorage) {
		activeViewScopes.put(beanStorageId, beanStorage);
		setViewAttribute(getClass().getName(), beanStorageId);
	}

	/**
	 * Destroys all beans associated with given bean storage identifier.
	 * @param beanStorageId The bean storage identifier.
	 */
	public void destroyBeans(UUID beanStorageId) {
		BeanStorage storage = activeViewScopes.remove(beanStorageId);

		if (storage != null) {
			storage.destroyBeans();
		}
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
	 * Listener for {@link ConcurrentLinkedHashMap} which will be invoked when an entry is evicted. It will in turn
	 * invoke {@link BeanStorage#destroyBeans()}.
	 */
	private static final class BeanStorageEvictionListener implements EvictionListener<UUID, BeanStorage>, Serializable {

		private static final long serialVersionUID = 1L;

		@Override
		public void onEviction(UUID id, BeanStorage storage) {
			storage.destroyBeans();
		}

	}

}