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

import static org.omnifaces.util.Faces.evaluateExpressionGet;
import static org.omnifaces.util.Faces.getInitParameter;
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

import org.omnifaces.cdi.BeanStorage;
import org.omnifaces.cdi.ViewScoped;
import org.omnifaces.util.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import org.omnifaces.util.concurrentlinkedhashmap.EvictionListener;

/**
 * Manage the view scoped beans by listening on view scope and session scope creation and destroy.
 *
 * @author Radu Creanga <rdcrng@gmail.com>
 * @author Bauke Scholtz
 * @see ViewScoped
 * @see ViewScopeContext
 * @since 1.6
 */
@Named(ViewScopeManager.NAME)
@SessionScoped
public class ViewScopeManager implements ViewMapListener, Serializable {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The name on which the CDI view scope manager should be stored in the session scope. */
	public static final String NAME = "omnifaces_ViewScopeManager";

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

	private static final long serialVersionUID = 42L;
	private static final String[] PARAM_NAMES_MAX_ACTIVE_VIEW_SCOPES = {
		PARAM_NAME_MAX_ACTIVE_VIEW_SCOPES, PARAM_NAME_MOJARRA_NUMBER_OF_VIEWS, PARAM_NAME_MYFACES_NUMBER_OF_VIEWS
	};
	private static final int DEFAULT_BEANS_PER_VIEW_SCOPE = 3;
	private static final String ERROR_MAX_ACTIVE_VIEW_SCOPES = "The '%s' init param must be a number."
		+ " Encountered an invalid value of '%s'.";

	// Static variables -----------------------------------------------------------------------------------------------

	private static Integer maxActiveViewScopes;

	// Variables ------------------------------------------------------------------------------------------------------

	private ConcurrentMap<UUID, BeanStorage> activeViewScopes;

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Create a new LRU map of active view scopes with maximum weighted capacity depending on several context params.
	 * See javadoc of {@link ViewScoped} for details.
	 * Subscribe to {@link PreDestroyViewMapEvent}, so that {@link #processEvent(SystemEvent)} will be invoked when
	 * the JSF view scope is about to be destroyed.
	 */
	@PostConstruct
	public void postConstruct() {
		activeViewScopes = new ConcurrentLinkedHashMap.Builder<UUID, BeanStorage>()
			.maximumWeightedCapacity(getMaxActiveViewScopes())
			.listener(new BeanStorageEvictionListener())
			.build();
	}

	/**
	 * Create and returns the CDI view scoped managed bean from the current JSF view scope.
	 * @param contextual The CDI context to find the CDI managed bean in.
	 * @param creationalContext The CDI creational context to create the CDI managed bean in.
	 * @return The created CDI view scoped managed bean from the current JSF view scope.
	 */
	public <T> T createBean(Contextual<T> contextual, CreationalContext<T> creationalContext) {
		return activeViewScopes.get(getBeanStorageId(true)).createBean(contextual, creationalContext);
	}

	/**
	 * Returns the CDI view scoped managed bean from the current JSF view scope.
	 * @param contextual The CDI context to find the CDI managed bean in.
	 * @return The CDI view scoped managed bean from the current JSF view scope.
	 */
	public <T> T getBean(Contextual<T> contextual) {
		return activeViewScopes.get(getBeanStorageId(true)).getBean(contextual);
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
	 * be destroyed, then invoke {@link #processPreDestroyViewMapEvent()} on the current CDI managed instance of this
	 * view scope manager.
	 */
	@Override
	public void processEvent(SystemEvent event) throws AbortProcessingException {
		if (event instanceof PreDestroyViewMapEvent) {
			// This method is invoked by a separate JSF-managed instance which is registered in faces-config.xml.
			// That's why it needs to resolve the actual CDI-managed instance where the view scopes are stored.
			ViewScopeManager instance = evaluateExpressionGet("#{" + NAME + "}");

			if (instance != null) {
				instance.processPreDestroyViewMapEvent();
			}
		}
	}

	/**
	 * This method is invoked during view destroy, in that case destroy all beans in current active view scope.
	 */
	public void processPreDestroyViewMapEvent() {
		BeanStorage storage = activeViewScopes.remove(getBeanStorageId(false));

		if (storage != null) {
			storage.destroyBeans();
		}
	}

	/**
	 * This method is invoked during session destroy, in that case destroy all beans in all active view scopes.
	 */
	@PreDestroy
	public void preDestroy() {
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
	private int getMaxActiveViewScopes() {
		if (maxActiveViewScopes != null) {
			return maxActiveViewScopes;
		}

		for (String name : PARAM_NAMES_MAX_ACTIVE_VIEW_SCOPES) {
			String value = getInitParameter(name);

			if (value != null) {
				if (value.matches("[0-9]+")) {
					return (maxActiveViewScopes = Integer.valueOf(value));
				}
				else {
					throw new IllegalArgumentException(String.format(ERROR_MAX_ACTIVE_VIEW_SCOPES, name, value));
				}
			}
		}

		return (maxActiveViewScopes = DEFAULT_MAX_ACTIVE_VIEW_SCOPES);
	}

	/**
	 * Returns the unique ID from the current JSF view scope which is to be associated with the CDI bean storage.
	 * If none is found, then a new ID will be auto-created. If <code>create</code> is <code>true</code>, then a new
	 * CDI bean storage will also be auto-created.
	 */
	private UUID getBeanStorageId(boolean create) {
		UUID id = (UUID) getViewAttribute(ViewScopeManager.class.getName());

		if (id == null || activeViewScopes.get(id) == null) {
			id = UUID.randomUUID();

			if (create) {
				activeViewScopes.put(id, new BeanStorage(DEFAULT_BEANS_PER_VIEW_SCOPE));
			}

			setViewAttribute(ViewScopeManager.class.getName(), id);
		}

		return id;
	}

	// Nested classes -------------------------------------------------------------------------------------------------

	/**
	 * Listener for {@link ConcurrentLinkedHashMap} which will be invoked when an entry is evicted. It will in turn
	 * invoke {@link BeanStorage#destroyBeans()}.
	 */
	private static final class BeanStorageEvictionListener implements EvictionListener<UUID, BeanStorage> {

		@Override
		public void onEviction(UUID id, BeanStorage storage) {
			storage.destroyBeans();
		}

	}

}