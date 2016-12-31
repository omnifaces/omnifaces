/*
 * Copyright 2016 OmniFaces
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

import static java.lang.Boolean.TRUE;
import static java.util.logging.Level.FINE;
import static org.omnifaces.util.Ajax.load;
import static org.omnifaces.util.Ajax.oncomplete;
import static org.omnifaces.util.BeansLocal.getInstance;
import static org.omnifaces.util.Components.addScriptResourceToBody;
import static org.omnifaces.util.Components.addScriptResourceToHead;
import static org.omnifaces.util.Components.addScriptToBody;
import static org.omnifaces.util.FacesLocal.getRequestParameter;
import static org.omnifaces.util.FacesLocal.isAjaxRequestWithPartialRendering;

import java.util.UUID;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.faces.application.StateManager;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.inject.Inject;

import org.omnifaces.cdi.BeanStorage;
import org.omnifaces.cdi.ViewScoped;
import org.omnifaces.resourcehandler.ResourceIdentifier;
import org.omnifaces.util.Hacks;

/**
 * Manages view scoped bean creation and destroy. The creation is initiated by {@link ViewScopeContext} which is
 * registered by {@link ViewScopeExtension} and the destroy is initiated by {@link ViewScopeEventListener} which is
 * registered in <code>faces-config.xml</code>.
 * <p>
 * Depending on {@link ViewScoped#saveInViewState()}, this view scope manager will delegate the creation and destroy
 * further to either {@link ViewScopeStorageInSession} or {@link ViewScopeStorageInViewState} which saves the concrete
 * bean instances in respectively HTTP session or JSF view state.
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

	private static final ResourceIdentifier SCRIPT_ID = new ResourceIdentifier("omnifaces", "omnifaces.js");
	private static final String SCRIPT_INIT = "OmniFaces.Unload.init('%s')";
	private static final int DEFAULT_BEANS_PER_VIEW_SCOPE = 3;

	private static final String ERROR_INVALID_STATE_SAVING = "@ViewScoped(saveInViewState=true) %s"
		+ " requires web.xml context parameter 'javax.faces.STATE_SAVING_METHOD' being set to 'client'.";

	// Variables ------------------------------------------------------------------------------------------------------

	@Inject
	private BeanManager manager;

	@Inject
	private ViewScopeStorageInSession storageInSession;

	@Inject
	private ViewScopeStorageInViewState storageInViewState;

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Create and returns the CDI view scoped managed bean from the current JSF view scope.
	 * @param <T> The expected return type.
	 * @param type The contextual type of the CDI managed bean.
	 * @param context The CDI context to create the CDI managed bean in.
	 * @return The created CDI view scoped managed bean from the current JSF view scope.
	 */
	public <T> T createBean(Contextual<T> type, CreationalContext<T> context) {
		return getBeanStorage(type).createBean(type, context);
	}

	/**
	 * Returns the CDI view scoped managed bean from the current JSF view scope.
	 * @param <T> The expected return type.
	 * @param type The contextual type of the CDI managed bean.
	 * @return The CDI view scoped managed bean from the current JSF view scope.
	 */
	public <T> T getBean(Contextual<T> type) {
		return getBeanStorage(type).getBean(type, manager);
	}

	/**
	 * This method is invoked during view destroy by {@link ViewScopeEventListener}, in that case destroy all beans in
	 * current active view scope.
	 */
	public void preDestroyView() {
		FacesContext context = FacesContext.getCurrentInstance();
		UUID beanStorageId = null;

		if (isUnloadRequest(context)) {
			try {
				beanStorageId = UUID.fromString(getRequestParameter(context, "id"));
			}
			catch (Exception ignore) {
				logger.log(FINE, "Ignoring thrown exception; this can only be a hacker attempt.", ignore);
				return;
			}
		}
		else if (isAjaxRequestWithPartialRendering(context)) {
			Hacks.setScriptResourceRendered(context, SCRIPT_ID); // Otherwise MyFaces will load a new one during createViewScope() when still in same document (e.g. navigation).
		}

		if (getInstance(manager, ViewScopeStorageInSession.class, false) != null) { // Avoid unnecessary session creation when accessing storageInSession for nothing.
			if (beanStorageId == null) {
				beanStorageId = storageInSession.getBeanStorageId();
			}

			if (beanStorageId != null) {
				storageInSession.destroyBeans(beanStorageId);
			}
		}

		// View scoped beans stored in client side JSF view state are per definition undestroyable, therefore storageInViewState is ignored here.
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	private <T> BeanStorage getBeanStorage(Contextual<T> type) {
		ViewScopeStorage storage = storageInSession;
		Class<?> beanClass = ((Bean<T>) type).getBeanClass();

		if (beanClass.getAnnotation(ViewScoped.class).saveInViewState()) {
			checkStateSavingMethod(beanClass);
			storage = storageInViewState;
		}

		UUID beanStorageId = storage.getBeanStorageId();

		if (beanStorageId == null) {
			beanStorageId = UUID.randomUUID();

			if (storage instanceof ViewScopeStorageInSession) {
				registerUnloadScript(beanStorageId);
			}
		}

		BeanStorage beanStorage = storage.getBeanStorage(beanStorageId);

		if (beanStorage == null) {
			beanStorage = new BeanStorage(DEFAULT_BEANS_PER_VIEW_SCOPE);
			storage.setBeanStorage(beanStorageId, beanStorage);
		}

		return beanStorage;
	}

	private void checkStateSavingMethod(Class<?> beanClass) {
		FacesContext context = FacesContext.getCurrentInstance();

		if (!context.getApplication().getStateManager().isSavingStateInClient(context)) {
			throw new IllegalStateException(String.format(ERROR_INVALID_STATE_SAVING, beanClass.getName()));
		}
	}

	/**
	 * Register unload script.
	 */
	private static void registerUnloadScript(UUID beanStorageId) {
		FacesContext context = FacesContext.getCurrentInstance();
		boolean ajaxRequestWithPartialRendering = isAjaxRequestWithPartialRendering(context);

		if (!Hacks.isScriptResourceRendered(context, SCRIPT_ID)) {
			if (ajaxRequestWithPartialRendering) {
				load("omnifaces", "unload.js");
			}
			else if (context.getCurrentPhaseId() != PhaseId.RENDER_RESPONSE || TRUE.equals(context.getAttributes().get(StateManager.IS_BUILDING_INITIAL_STATE))) {
				addScriptResourceToHead("omnifaces", "omnifaces.js");
			}
			else {
				addScriptResourceToBody("omnifaces", "unload.js");
			}
		}

		String script = String.format(SCRIPT_INIT, beanStorageId);

		if (ajaxRequestWithPartialRendering) {
			oncomplete(script);
		}
		else {
			addScriptToBody(script);
		}
	}

	/**
	 * Returns <code>true</code> if the current request is triggered by an unload request.
	 * @param context The involved faces context.
	 * @return <code>true</code> if the current request is triggered by an unload request.
	 * @since 2.2
	 */
	public static boolean isUnloadRequest(FacesContext context) {
		return "unload".equals(getRequestParameter(context, "omnifaces.event"));
	}

}