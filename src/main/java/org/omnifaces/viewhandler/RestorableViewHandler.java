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
import static org.omnifaces.util.FacesLocal.getApplicationAttribute;
import static org.omnifaces.util.FacesLocal.getRenderKit;

import java.io.IOException;

import javax.faces.FacesException;
import javax.faces.application.ViewExpiredException;
import javax.faces.application.ViewHandler;
import javax.faces.application.ViewHandlerWrapper;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

import org.omnifaces.cdi.ViewScoped;
import org.omnifaces.taghandler.EnableRestorableView;

/**
 * This view handler implementation will recreate the entire view state whenever the view has apparently been expired,
 * i.e. whenever {@link #restoreView(FacesContext, String)} returns <code>null</code> and the current request is a
 * postback and the view in question has <code>&lt;enableRestorableView&gt;</code> in the metadata. This effectively
 * prevents the {@link ViewExpiredException} on the view.
 * <p>
 * This view handler implementation also detects unload requests coming from {@link ViewScoped} beans and will prevent
 * any attempt to restore the view when the view state is already absent. This prevents unnecessary
 * {@link ViewExpiredException} during unload on an expired view.
 *
 * @author Bauke Scholtz
 * @since 1.3
 * @see EnableRestorableView
 * @see ViewScoped
 */
public class RestorableViewHandler extends ViewHandlerWrapper {

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
	 * First check if this is an unload request from {@link ViewScoped}. If so, and the JSF view state is absent or
	 * expired, then don't try to restore the view and return a dummy view (to avoid {@link ViewExpiredException}) and
	 * immediately complete response.
	 * Then restore the view and check if the <code>&lt;o:enableRestoreView&gt;</code> is used once in the application,
	 * and the restored view is null and the current request is a postback, then recreate and build the view. If it
	 * indeed contains the <code>&lt;o:enableRestoreView&gt;</code>, then return the newly created view, else return
	 * <code>null</code>.
	 */
	@Override
	public UIViewRoot restoreView(FacesContext context, String viewId) {
		if (isUnloadRequest(context) && getRenderKit(context).getResponseStateManager().getState(context, viewId) == null) {
			context.responseComplete();
			return new UIViewRoot();
		}

		UIViewRoot restoredView = super.restoreView(context, viewId);

		if (!(isRestorableViewEnabled(context) && restoredView == null && context.isPostback())) {
			return restoredView;
		}

		UIViewRoot createdView;

		try {
			createdView = buildView(viewId);
		}
		catch (IOException e) {
			throw new FacesException(e);
		}

		if (TRUE.equals(createdView.getAttributes().get(EnableRestorableView.class.getName()))) {
			return createdView;
		}
		else {
			return null;
		}
	}

	private boolean isRestorableViewEnabled(FacesContext context) {
		return TRUE.equals(getApplicationAttribute(context, EnableRestorableView.class.getName()));
	}

	@Override
	public ViewHandler getWrapped() {
		return wrapped;
	}

}