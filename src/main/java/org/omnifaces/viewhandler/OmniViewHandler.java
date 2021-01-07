/*
 * Copyright 2020 OmniFaces
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
package org.omnifaces.viewhandler;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.faces.component.visit.VisitHint.SKIP_ITERATION;
import static org.omnifaces.cdi.viewscope.ViewScopeManager.isUnloadRequest;
import static org.omnifaces.resourcehandler.ViewResourceHandler.isViewResourceRequest;
import static org.omnifaces.taghandler.EnableRestorableView.isRestorableView;
import static org.omnifaces.taghandler.EnableRestorableView.isRestorableViewRequest;
import static org.omnifaces.util.Components.buildView;
import static org.omnifaces.util.Components.forEachComponent;
import static org.omnifaces.util.Components.getClosestParent;
import static org.omnifaces.util.Faces.isPrefixMapping;
import static org.omnifaces.util.Faces.responseComplete;
import static org.omnifaces.util.FacesLocal.getMimeType;
import static org.omnifaces.util.FacesLocal.getRenderKit;
import static org.omnifaces.util.FacesLocal.getRequestServletPath;
import static org.omnifaces.util.FacesLocal.getRequestURIWithQueryString;
import static org.omnifaces.util.FacesLocal.getServletContext;
import static org.omnifaces.util.FacesLocal.isAjaxRequest;
import static org.omnifaces.util.FacesLocal.isDevelopment;
import static org.omnifaces.util.FacesLocal.isSessionNew;
import static org.omnifaces.util.FacesLocal.redirectPermanent;
import static org.omnifaces.util.Platform.getDefaultFacesServletMapping;

import java.io.IOException;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.application.ViewExpiredException;
import javax.faces.application.ViewHandler;
import javax.faces.application.ViewHandlerWrapper;
import javax.faces.component.UIForm;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.ExternalContextWrapper;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextWrapper;
import javax.faces.event.PreDestroyViewMapEvent;
import javax.faces.render.ResponseStateManager;

import org.omnifaces.cdi.ViewScoped;
import org.omnifaces.cdi.viewscope.ViewScopeManager;
import org.omnifaces.resourcehandler.ViewResourceHandler;
import org.omnifaces.taghandler.EnableRestorableView;
import org.omnifaces.util.Callback;
import org.omnifaces.util.Hacks;

/**
 * OmniFaces view handler.
 * This class was before version 2.5 known as <code>RestorableViewHandler</code>.
 * This view handler performs the following tasks:
 * <ol>
 * <li>Since 1.3: Recreate entire view when {@link EnableRestorableView} tag is in the metadata. This effectively
 * prevents the {@link ViewExpiredException} on the view.
 * <li>Since 2.2: Detect unload requests coming from {@link ViewScoped} beans. This will create a dummy view and only
 * restore the view scoped state instead of building and restoring the entire view.
 * <li>Since 2.5: If project stage is development, then throw an {@link IllegalStateException} when there's a nested
 * {@link UIForm} component.
 * <li>Since 3.10: If {@link ViewResourceHandler#isViewResourceRequest(FacesContext)} is <code>true</code>, then
 * replace the HTML response writer with a XML response writer in {@link #renderView(FacesContext, UIViewRoot)}, and
 * ensure that proper action URL is returned in {@link #getActionURL(FacesContext, String)}.
 * </ol>
 *
 * @author Bauke Scholtz
 * @since 1.3
 * @see EnableRestorableView
 * @see ViewScopeManager
 */
public class OmniViewHandler extends ViewHandlerWrapper {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String XML_CONTENT_TYPE = "text/xml";
	private static final NestedFormsChecker NESTED_FORMS_CHECKER = new NestedFormsChecker();

	private static final String ERROR_NESTED_FORM_ENCOUNTERED =
		"Nested form with ID '%s' encountered inside parent form with ID '%s'. This is illegal in HTML.";

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new OmniFaces view handler around the given wrapped view handler.
	 * @param wrapped The wrapped view handler.
	 */
	public OmniViewHandler(ViewHandler wrapped) {
		super(wrapped);
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
			return unloadView(context, viewId);
		}

		UIViewRoot restoredView = super.restoreView(context, viewId);

		if (isRestorableViewRequest(context, restoredView)) {
			return createRestorableViewIfNecessary(viewId);
		}

		return restoredView;
	}

	@Override
	public void renderView(FacesContext context, UIViewRoot viewToRender) throws IOException {
		if (isDevelopment(context)) {
			validateComponentTreeStructure(context, viewToRender);
		}

		if (isAjaxRequest(context)) {
			context.getAttributes().put("facelets.ContentType", XML_CONTENT_TYPE); // Work around for nasty Mojarra 2.3.4+ bug reported as #4484.
		}

		if (isViewResourceRequest(context)) {
			String contentType = getMimeType(context, getRequestServletPath(context));
			String characterEncoding = UTF_8.name();

			ExternalContext externalContext = context.getExternalContext();
			externalContext.setResponseContentType(contentType);
			externalContext.setResponseCharacterEncoding(characterEncoding);
			context.setResponseWriter(context.getRenderKit().createResponseWriter(externalContext.getResponseOutputWriter(), XML_CONTENT_TYPE, characterEncoding));
			context.getAttributes().put("facelets.ContentType", contentType); // Work around for MyFaces ignoring the setResponseContentType.

			try {
				Hacks.clearCachedFacesServletMapping(context);
				super.renderView(new RenderViewResourceFacesContext(context), viewToRender);
			}
			finally {
				Hacks.clearCachedFacesServletMapping(context);
			}
		}
		else {
			super.renderView(context, viewToRender);
		}
	}

	/**
	 * Create a dummy view, restore only the view root state and, if present, then immediately explicitly destroy the
	 * view state. Or, if the session is new (during an unload request, it implies it had expired), then explicitly send
	 * a permanent redirect to the original request URI. This way any authentication framework which remembers the "last
	 * requested restricted URL" will redirect back to correct (non-unload) URL after login on a new session.
	 */
	private UIViewRoot unloadView(FacesContext context, String viewId) {
		UIViewRoot createdView = createView(context, viewId);
		ResponseStateManager manager = getRenderKit(context).getResponseStateManager();

		if (restoreViewRootState(context, manager, createdView)) {
			context.setProcessingEvents(true);
			context.getApplication().publishEvent(context, PreDestroyViewMapEvent.class, UIViewRoot.class, createdView);
			Hacks.removeViewState(context, manager, viewId);
		}
		else if (isSessionNew(context)) {
			redirectPermanent(context, getRequestURIWithQueryString(context));
		}

		responseComplete();
		return createdView;
	}

	/**
	 * Restore only the view root state. This ensures that the view scope map and all view root component system event
	 * listeners are also restored (including those for {@link PreDestroyViewMapEvent}). This is done so because calling
	 * <code>super.restoreView()</code> would implicitly also build the entire view and restore state of all other
	 * components in the tree. This is unnecessary during an unload request.
	 */
	@SuppressWarnings("rawtypes")
	private boolean restoreViewRootState(FacesContext context, ResponseStateManager manager, UIViewRoot view) {
		Object state = manager.getState(context, view.getViewId());

		if (!(state instanceof Object[]) || ((Object[]) state).length < 2) {
			return false;
		}

		Object componentState = ((Object[]) state)[1];
		Object viewRootState = null;

		if (componentState instanceof Map) { // Partial state saving.
			if (view.getId() == null) { // MyFaces.
				view.setId(view.createUniqueId(context, null));
				view.markInitialState();
			}

			viewRootState = ((Map) componentState).get(view.getClientId(context));
		}
		else if (componentState instanceof Object[]) { // Full state saving.
			viewRootState = ((Object[]) componentState)[0];
		}

		if (viewRootState != null) {
			view.restoreState(context, viewRootState);
			context.setViewRoot(view);
			return true;
		}

		return false;
	}

	/**
	 * Create and build the view and return it if it indeed contains {@link EnableRestorableView}, else return null.
	 */
	private UIViewRoot createRestorableViewIfNecessary(String viewId) {
		try {
			UIViewRoot createdView = buildView(viewId);
			return isRestorableView(createdView) ? createdView : null;
		}
		catch (IOException e) {
			throw new FacesException(e);
		}
	}

	private void validateComponentTreeStructure(FacesContext context, UIViewRoot view) {
		checkNestedForms(context, view);
	}

	private void checkNestedForms(FacesContext context, UIViewRoot view) {
		forEachComponent(context).fromRoot(view).ofTypes(UIForm.class).withHints(SKIP_ITERATION).invoke(NESTED_FORMS_CHECKER);
	}

	// Inner classes -------------------------------------------------------------------------------------------------

	private static class NestedFormsChecker implements Callback.WithArgument<UIForm> {
		@Override
		public void invoke(UIForm form) {
			UIForm nestedParent = getClosestParent(form, UIForm.class);

			if (nestedParent != null && (!Hacks.isNestedInPrimeFacesDialog(form) || Hacks.isNestedInPrimeFacesDialog(form, nestedParent))) {
				throw new IllegalStateException(
					format(ERROR_NESTED_FORM_ENCOUNTERED, form.getClientId(), nestedParent.getClientId()));
			}
		}
	}

	private static class RenderViewResourceFacesContext extends FacesContextWrapper {

		private final ExternalContext externalContext;

		private RenderViewResourceFacesContext(FacesContext wrapped) {
			super(wrapped);
			String defaultMapping = getDefaultFacesServletMapping(getServletContext(getWrapped()));
			boolean prefixMapping = isPrefixMapping(defaultMapping);
			String requestPathInfo = prefixMapping ? defaultMapping : null;
			String requestServletPath = getRequestServletPath(getWrapped()) + (prefixMapping ? "" : defaultMapping);
			this.externalContext = new RenderViewResourceExternalContext(getWrapped().getExternalContext(), requestPathInfo, requestServletPath);
		}

		@Override
		public ExternalContext getExternalContext() {
			return externalContext;
		}
	}

	private static class RenderViewResourceExternalContext extends ExternalContextWrapper {

		private final String requestPathInfo;
		private final String requestServletPath;

		private RenderViewResourceExternalContext(ExternalContext wrapped, String requestPathInfo, String requestServletPath) {
			super(wrapped);
			this.requestPathInfo = requestPathInfo;
			this.requestServletPath = requestServletPath;
		}

		@Override
		public String getRequestPathInfo() {
			return requestPathInfo;
		}

		@Override
		public String getRequestServletPath() {
			return requestServletPath;
		}

		@Override
		public String encodeActionURL(String url) {
			return super.encodeActionURL(url).replaceAll(";jsessionid=[^&?#]*", "");
		}
	}
}