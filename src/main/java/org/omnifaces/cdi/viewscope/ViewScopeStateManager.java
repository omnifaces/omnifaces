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

import javax.faces.application.StateManager;
import javax.faces.application.StateManagerWrapper;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

import org.omnifaces.cdi.ViewScoped;

/**
 * Right before the state of the current view is saved, puts the pending bean storage of OmniFaces {@link ViewScoped}
 * beans of the current view in the active view scopes, see {@link ViewScopeStorageInSession}. A view whose state is
 * never saved, such as a view which is redirected or forwarded away from, or a view which is replaced by a file
 * download, an error page or an exception, never reaches the client as a view on which the unload script could
 * destroy its view scoped beans. The beans of such a view are therefore destroyed at the end of the request.
 * <p>
 * This state manager is already registered by OmniFaces' own <code>faces-config.xml</code> and thus gets
 * auto-initialized when the OmniFaces JAR is bundled in a web application, so end-users do not need to register this
 * state manager explicitly themselves.
 *
 * @author Bauke Scholtz
 * @see ViewScopeStorageInSession
 * @since 3.14.28
 */
public class ViewScopeStateManager extends StateManagerWrapper {

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new view scope state manager around the given wrapped state manager.
	 * @param wrapped The wrapped state manager.
	 */
	public ViewScopeStateManager(StateManager wrapped) {
		super(wrapped);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Activates the pending view scope of the current view before delegating.
	 */
	@Override
	public Object saveView(FacesContext context) {
		activate(context);
		return super.saveView(context);
	}

	/**
	 * Activates the pending view scope of the current view before delegating.
	 */
	@Override
	public String getViewState(FacesContext context) {
		activate(context);
		return super.getViewState(context);
	}

	private static void activate(FacesContext context) {
		UIViewRoot viewRoot = context.getViewRoot();

		if (viewRoot != null && !viewRoot.isTransient()) {
			ViewScopeStorageInSession.activate(viewRoot);
		}
	}

}