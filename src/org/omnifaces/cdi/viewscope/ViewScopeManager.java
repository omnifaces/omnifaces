/*
 * Copyright 2013 OmniFaces.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnifaces.cdi.viewscope;

import static org.omnifaces.util.Events.subscribeToEvent;
import static org.omnifaces.util.Faces.getViewAttribute;
import static org.omnifaces.util.Faces.setViewAttribute;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.faces.component.UIViewRoot;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.PreDestroyViewMapEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.ViewMapListener;
import javax.inject.Named;

import org.omnifaces.cdi.BeanManager;
import org.omnifaces.component.output.cache.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import org.omnifaces.component.output.cache.concurrentlinkedhashmap.EvictionListener;

/**
 * Manage the view scoped beans by listening on view scope and session scope creation and destroy.
 *
 * @author Radu Creanga <rdcrng@gmail.com>
 * @author Bauke Scholtz
 * @since 1.6
 */
@Named(ViewScopeManager.NAME)
@SessionScoped
public class ViewScopeManager implements ViewMapListener, Serializable {

	// Constants ------------------------------------------------------------------------------------------------------

	/**
	 * The name on which the CDI view scope manager should be stored in the session scope.
	 */
	public static final String NAME = "omnifaces_ViewScopeManager";

	private static final long serialVersionUID = 42L;
	private static final int MAX_ACTIVE_VIEW_SCOPES = 25; // TODO: this should depend on JSF impl specific "max views per session" configuration setting or at least be configurable.

	// Variables ------------------------------------------------------------------------------------------------------

	private final ConcurrentMap<UUID, BeanManager> activeViewScopes =
		new ConcurrentLinkedHashMap.Builder<UUID, BeanManager>()
			.maximumWeightedCapacity(MAX_ACTIVE_VIEW_SCOPES)
			.listener(new BeanManagerEvictionListener())
			.build();

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Subscribe to {@link PreDestroyViewMapEvent}, so that {@link #processEvent(SystemEvent)} will be invoked when
	 * the JSF view scope is about to be destroyed.
	 */
	@PostConstruct
	public void postConstruct() {
		subscribeToEvent(PreDestroyViewMapEvent.class, this);
	}

	/**
	 * Create and returns the CDI view scoped managed bean from the current JSF view scope.
	 * @param contextual The CDI context to find the CDI managed bean in.
	 * @param creationalContext The CDI creational context to create the CDI managed bean in.
	 * @return The created CDI view scoped managed bean from the current JSF view scope.
	 */
	public <T> T createBean(Contextual<T> contextual, CreationalContext<T> creationalContext) {
		UUID beanManagerId = getBeanManagerId();
		activeViewScopes.putIfAbsent(beanManagerId, new BeanManager());
		return activeViewScopes.get(beanManagerId).createBean(contextual, creationalContext);
	}

	/**
	 * Returns the CDI view scoped managed bean from the current JSF view scope.
	 * @param contextual The CDI context to find the CDI managed bean in.
	 * @return The CDI view scoped managed bean from the current JSF view scope.
	 */
	public <T> T getBean(Contextual<T> contextual) {
		BeanManager manager = activeViewScopes.get(getBeanManagerId());
		return (manager == null) ? null : manager.getBean(contextual);
	}

	/**
	 * Returns <code>true</code> if given source is an instance of {@link UIViewRoot}.
	 */
	@Override
	public boolean isListenerForSource(Object source) {
		return (source instanceof UIViewRoot);
	}

	/**
	 * If the event is an instance of {@link PreDestroyViewMapEvent}, which means that the JSF view scope is about to
	 * be destroyed, then destroy all CDI managed beans associated with the JSF view scope.
	 */
	@Override
	public void processEvent(SystemEvent event) throws AbortProcessingException {
		if (event instanceof PreDestroyViewMapEvent) {
			BeanManager manager = activeViewScopes.remove(getBeanManagerId());

			if (manager != null) {
				manager.destroyBeans();
			}
		}
	}

	/**
	 * This method is invoked during session destroy, in that case destroy all beans in all active view scopes.
	 */
	@PreDestroy
	public void preDestroy() {
		for (BeanManager manager : activeViewScopes.values()) {
			manager.destroyBeans();
		}
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the unique ID from the current JSF view scope which is to be associated with the CDI bean manager.
	 */
	private UUID getBeanManagerId() {
		UUID id = (UUID) getViewAttribute(ViewScopeManager.class.getName());

		if (id == null) {
			id = UUID.randomUUID();
			setViewAttribute(ViewScopeManager.class.getName(), id);
		}

		return id;
	}

	// Nested classes -------------------------------------------------------------------------------------------------

	/**
	 * Listener for {@link ConcurrentLinkedHashMap} which will be invoked when an entry is evicted. It will in turn
	 * invoke {@link BeanManager#destroyBeans()}.
	 */
	private static final class BeanManagerEvictionListener implements EvictionListener<UUID, BeanManager> {

		@Override
		public void onEviction(UUID id, BeanManager manager) {
			manager.destroyBeans();
		}

	}

}