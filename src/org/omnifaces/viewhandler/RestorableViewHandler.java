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

import java.io.IOException;

import javax.faces.FacesException;
import javax.faces.application.ViewExpiredException;
import javax.faces.application.ViewHandler;
import javax.faces.application.ViewHandlerWrapper;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

import org.omnifaces.taghandler.EnableRestorableView;
import org.omnifaces.util.Faces;

/**
 * This view handler implementation will recreate the entire view state whenever the view has apparently been expired,
 * i.e. whenever {@link #restoreView(FacesContext, String)} returns <code>null</code> and the current request is a
 * postback and the view in question has <code>&lt;enableRestorableView&gt;</code> in the metadata. This effectively
 * prevents the {@link ViewExpiredException} on the view.
 *
 * @author Bauke Scholtz
 * @since 1.3
 * @see EnableRestorableView
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
	 * First try to restore the view. If it returns null and the current request is a postback, then recreate and build
	 * the view. If it contains the <code>&lt;o:enableRestoreView&gt;</code>, then return the newly created view, else
	 * return <code>null</code>.
	 */
	@Override
	public UIViewRoot restoreView(FacesContext context, String viewId) {
		UIViewRoot restoredView = super.restoreView(context, viewId);

		if (!(restoredView == null && context.isPostback())) {
			return restoredView;
		}

		viewId = Faces.normalizeViewId(viewId);
		UIViewRoot createdView = createView(context, viewId);
		context.setViewRoot(createdView);

		try {
			getViewDeclarationLanguage(context, viewId).buildView(context, createdView);
		}
		catch (IOException e) {
			throw new FacesException(e);
		}

		if (createdView.getAttributes().get(EnableRestorableView.class.getName()) == Boolean.TRUE) {
			return createdView;
		}
		else {
			context.setViewRoot(null);
			return null;
		}
	}

	@Override
	public ViewHandler getWrapped() {
		return wrapped;
	}

}