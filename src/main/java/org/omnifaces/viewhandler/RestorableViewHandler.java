/*
 * Copyright 2012 OmniFaces.
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
package org.omnifaces.viewhandler;

import static java.lang.Boolean.TRUE;
import static org.omnifaces.cdi.viewscope.ViewScopeManager.isUnloadRequest;
import static org.omnifaces.util.Components.buildView;
import static org.omnifaces.util.Faces.responseComplete;
import static org.omnifaces.util.FacesLocal.getApplicationAttribute;
import static org.omnifaces.util.FacesLocal.getRenderKit;

import java.io.IOException;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.application.ViewExpiredException;
import javax.faces.application.ViewHandler;
import javax.faces.application.ViewHandlerWrapper;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.PreDestroyViewMapEvent;
import javax.faces.render.ResponseStateManager;

import org.omnifaces.cdi.ViewScoped;
import org.omnifaces.taghandler.EnableRestorableView;
import org.omnifaces.util.Hacks;

/**
 * This view handler implementation will recreate the entire view state whenever the view has apparently been expired,
 * i.e. whenever {@link #restoreView(FacesContext, String)} returns <code>null</code> and the current request is a
 * postback and the view in question has <code>&lt;enableRestorableView&gt;</code> in the metadata. This effectively
 * prevents the {@link ViewExpiredException} on the view.
 * <p>
 * Since OmniFaces 2.2, this view handler implementation also detects unload requests coming from {@link ViewScoped}
 * beans and will create a dummy view and only restore the view scoped state instead of building and restoring the
 * entire view.
 *
 * @author Bauke Scholtz
 * @since 1.3
 * @see EnableRestorableView
 * @see ViewScoped
 */
public class RestorableViewHandler extends ViewHandlerWrapper { // TODO: rename to OmniViewHandler.

	// Properties -----------------------------------------------------------------------------------------------------

	private ViewHandler wrapped;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new restorable view handler around the given wrapped view handler.
	 * @param wrapped The wrapped view handler.
	 */
	public RestorableViewHandler(ViewHandler wrapped) {
		this.wrapped = wrapped;
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * If the current request is an unload request from {@link ViewScoped}, then create a dummy view, restore only the
	 * view root state and then immediately explicitly destroy the view, else restore the view as usual. If the
	 * <code>&lt;o:enableRestoreView&gt;</code> is used once in the application, and the restored view is null and the
	 * current request is a postback, then recreate and rebuild the view from scratch. If it indeed contains the
	 * <code>&lt;o:enableRestoreView&gt;</code>, then return the newly created view, else return <code>null</code>.
	 */
	@Override
	public UIViewRoot restoreView(FacesContext context, String viewId) {
		if (isUnloadRequest(context)) {
			UIViewRoot createdView = createView(context, viewId);
			ResponseStateManager manager = getRenderKit(context).getResponseStateManager();

			if (restoreViewRootState(context, manager, createdView)) {
				context.setProcessingEvents(true);
				context.getApplication().publishEvent(context, PreDestroyViewMapEvent.class, UIViewRoot.class, createdView);
				Hacks.removeViewState(context, manager, viewId);
			}

			responseComplete();
			return createdView;
		}

		UIViewRoot restoredView = super.restoreView(context, viewId);

		if (!(isRestorableViewEnabled(context) && restoredView == null && context.isPostback())) {
			return restoredView;
		}

		try {
			UIViewRoot createdView = buildView(viewId);
			return isRestorableView(createdView) ? createdView : null;
		}
		catch (IOException e) {
			throw new FacesException(e);
		}
	}

	/**
	 * Restore only the view root state. This ensures that the view scope map and all view root component system event
	 * listeners are also restored. Calling <code>super.restoreView()</code> would implicitly also build the entire view
	 * and restore state of all other components in the tree. This is unnecessary during an unload request.
	 */
	@SuppressWarnings("unchecked")
	private boolean restoreViewRootState(FacesContext context, ResponseStateManager manager, UIViewRoot view) {
		Object state = manager.getState(context, view.getViewId());

		// TODO: this assumes partial state saving not full state saving (thus doesn't work on full state saving).
		if (state == null || !(state instanceof Object[]) || ((Object[]) state).length < 2 || !(((Object[]) state)[1] instanceof Map)) {
			return false;
		}

		Map<String, Object> states = (Map<String, Object>) ((Object[]) state)[1]; // Fortunately both Mojarra and MyFaces have same structure, otherwise this had to be in Hacks.

		if (view.getId() == null) { // MyFaces.
			view.setId(view.createUniqueId(context, null));
			view.markInitialState();
		}

		Object viewRootState = states.get(view.getClientId(context));
		view.restoreState(context, viewRootState);
		context.setViewRoot(view);
		return true;
	}

	private boolean isRestorableViewEnabled(FacesContext context) {
		return TRUE.equals(getApplicationAttribute(context, EnableRestorableView.class.getName()));
	}

	private boolean isRestorableView(UIViewRoot view) {
		return TRUE.equals(view.getAttributes().get(EnableRestorableView.class.getName()));
	}

	@Override
	public ViewHandler getWrapped() {
		return wrapped;
	}

}