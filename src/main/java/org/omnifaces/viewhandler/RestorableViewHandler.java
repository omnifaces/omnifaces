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
import static org.omnifaces.util.Faces.normalizeViewId;
import static org.omnifaces.util.Faces.setContext;
import static org.omnifaces.util.FacesLocal.getApplicationAttribute;

import java.io.IOException;

import javax.faces.FacesException;
import javax.faces.FactoryFinder;
import javax.faces.application.ViewExpiredException;
import javax.faces.application.ViewHandler;
import javax.faces.application.ViewHandlerWrapper;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextWrapper;
import javax.faces.render.RenderKit;
import javax.faces.render.RenderKitFactory;

import org.omnifaces.taghandler.EnableRestorableView;

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
	 * First try to restore the view. If the <code>&lt;o:enableRestoreView&gt;</code> is used once in the application,
	 * and the restored view returns null and the current request is a postback, then recreate and build the view.
	 * If it contains the <code>&lt;o:enableRestoreView&gt;</code>, then return the newly created view, else
	 * return <code>null</code>.
	 */
	@Override
	public UIViewRoot restoreView(FacesContext context, String viewId) {
		UIViewRoot restoredView = super.restoreView(context, viewId);

		if (!(isEnabled(context) && restoredView == null && context.isPostback())) {
			return restoredView;
		}

		String normalizedViewId = normalizeViewId(viewId);
		UIViewRoot createdView = createView(context, normalizedViewId);
		FacesContext temporaryContext = new TemporaryViewFacesContext(context, createdView);

		try {
			setContext(temporaryContext);
			getViewDeclarationLanguage(temporaryContext, normalizedViewId).buildView(temporaryContext, createdView);
		}
		catch (IOException e) {
			throw new FacesException(e);
		}
		finally {
			setContext(context);
		}

		if (TRUE.equals(createdView.getAttributes().get(EnableRestorableView.class.getName()))) {
			return createdView;
		}
		else {
			return null;
		}
	}

	private boolean isEnabled(FacesContext context) {
		return TRUE.equals(getApplicationAttribute(context, EnableRestorableView.class.getName()));
	}

	@Override
	public ViewHandler getWrapped() {
		return wrapped;
	}

	// Inner classes --------------------------------------------------------------------------------------------------

	/**
	 * This faces context wrapper allows returning the given (temporary) view on {@link #getViewRoot()} and its
	 * associated renderer in {@link #getRenderKit()}. This can then be used in cases when
	 * {@link FacesContext#setViewRoot(UIViewRoot)} isn't desired as it can't be cleared afterwards (the
	 * {@link #setViewRoot(UIViewRoot)} doesn't accept a <code>null</code> being set).
	 *
	 * @author Bauke Scholtz
	 */
	private static class TemporaryViewFacesContext extends FacesContextWrapper {

		private FacesContext wrapped;
		private UIViewRoot temporaryView;

		public TemporaryViewFacesContext(FacesContext wrapped, UIViewRoot temporaryView) {
			this.wrapped = wrapped;
			this.temporaryView = temporaryView;
		}

		@Override
		public UIViewRoot getViewRoot() {
			return temporaryView;
		}

		@Override
		public RenderKit getRenderKit() {
			return ((RenderKitFactory) FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY))
				.getRenderKit(this, temporaryView.getRenderKitId());
		}

		@Override
		public FacesContext getWrapped() {
			return wrapped;
		}

	}

}