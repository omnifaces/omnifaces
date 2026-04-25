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
package org.omnifaces.viewhandler;

import static jakarta.faces.render.ResponseStateManager.VIEW_STATE_PARAM;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.omnifaces.cdi.viewscope.ViewScopeManager.isUnloadRequest;
import static org.omnifaces.resourcehandler.PWAResourceHandler.isServiceWorkerRequest;
import static org.omnifaces.resourcehandler.ViewResourceHandler.isViewResourceRequest;
import static org.omnifaces.taghandler.EnableRestorableView.isRestorableView;
import static org.omnifaces.taghandler.EnableRestorableView.isRestorableViewRequest;
import static org.omnifaces.util.ComponentsLocal.buildView;
import static org.omnifaces.util.Faces.isPrefixMapping;
import static org.omnifaces.util.Faces.setContext;
import static org.omnifaces.util.FacesLocal.getMimeType;
import static org.omnifaces.util.FacesLocal.getRenderKit;
import static org.omnifaces.util.FacesLocal.getRequestParameter;
import static org.omnifaces.util.FacesLocal.getRequestServletPath;
import static org.omnifaces.util.FacesLocal.getRequestURIWithQueryString;
import static org.omnifaces.util.FacesLocal.getServletContext;
import static org.omnifaces.util.FacesLocal.getSessionAttribute;
import static org.omnifaces.util.FacesLocal.hasSession;
import static org.omnifaces.util.FacesLocal.isDevelopment;
import static org.omnifaces.util.FacesLocal.isSessionNew;
import static org.omnifaces.util.FacesLocal.redirectPermanent;
import static org.omnifaces.util.Platform.getDefaultFacesServletMapping;

import java.io.IOException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.faces.FacesException;
import jakarta.faces.application.ViewExpiredException;
import jakarta.faces.application.ViewHandler;
import jakarta.faces.application.ViewHandlerWrapper;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIForm;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.ExternalContextWrapper;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.FacesContextWrapper;
import jakarta.faces.event.PreDestroyViewMapEvent;
import jakarta.faces.render.RenderKit;
import jakarta.faces.render.ResponseStateManager;

import org.omnifaces.cdi.ViewScoped;
import org.omnifaces.cdi.viewscope.ViewScopeManager;
import org.omnifaces.config.WebXml;
import org.omnifaces.resourcehandler.PWAResourceHandler;
import org.omnifaces.resourcehandler.ViewResourceHandler;
import org.omnifaces.taghandler.EnableRestorableView;
import org.omnifaces.util.FacesLocal;
import org.omnifaces.util.Hacks;

/**
 * OmniFaces view handler. This class was before version 2.5 known as <code>RestorableViewHandler</code>. This view handler performs the following tasks:
 * <ol>
 * <li>Since 1.3: Recreate entire view when {@link EnableRestorableView} tag is in the metadata. This effectively prevents the {@link ViewExpiredException} on
 * the view.
 * <li>Since 2.2: Detect unload requests coming from {@link ViewScoped} beans. This will create a dummy view and only restore the view scoped state instead of
 * building and restoring the entire view.
 * <li>Since 2.5: If project stage is development, then throw an {@link IllegalStateException} when there's a nested {@link UIForm} component.
 * <li>Since 3.10: If {@link ViewResourceHandler#isViewResourceRequest(FacesContext)} is <code>true</code>, then replace the HTML response writer with a XML
 * response writer in {@link #renderView(FacesContext, UIViewRoot)}, and ensure that proper action URL is returned in
 * {@link #getActionURL(FacesContext, String)}.
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

    private static final String ERROR_NESTED_FORM_ENCOUNTERED = "Nested form with ID '%s' encountered inside parent form with ID '%s'. This is illegal in HTML.";

    private static final String SESSION_ATTRIBUTE_PENDING_VIEW_STATE_REMOVALS = "omnifaces.PendingViewStateRemovals";

    // Variables ------------------------------------------------------------------------------------------------------

    private final boolean usePendingViewStateRemoval;

    // Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Construct a new OmniFaces view handler around the given wrapped view handler.
     *
     * @param wrapped The wrapped view handler.
     */
    public OmniViewHandler(ViewHandler wrapped) {
        super(wrapped);
        usePendingViewStateRemoval = WebXml.instance().isDistributable() && !Hacks.isSpringWebFlowViewHandler(wrapped);
    }

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * If the current request is a sw.js request from {@link PWAResourceHandler}, then create a dummy view and trigger {@link FacesContext#responseComplete()}
     * so that it won't be built nor rendered.
     */
    @Override
    public UIViewRoot createView(FacesContext context, String viewId) {
        if (isServiceWorkerRequest(context)) {
            return createServiceWorkerView(context, viewId);
        }

        if (usePendingViewStateRemoval) {
            performPendingViewStateRemovals(context);
        }

        return super.createView(context, viewId);
    }

    /**
     * If the current request is an unload request from {@link ViewScoped}, then create a dummy view, restore only the view root state and then immediately
     * explicitly destroy the view scoped beans, else restore the view as usual. If the <code>&lt;o:enableRestoreView&gt;</code> is used once in the
     * application, and the restored view is null and the current request is a postback, then recreate and rebuild the view from scratch. If it indeed contains
     * the <code>&lt;o:enableRestoreView&gt;</code>, then return the newly created view, else return <code>null</code>.
     */
    @Override
    public UIViewRoot restoreView(FacesContext context, String viewId) {
        if (isUnloadRequest(context)) {
            return unloadView(context, viewId);
        }

        var restoredView = super.restoreView(context, viewId);

        if (isRestorableViewRequest(context, restoredView)) {
            return createRestorableViewIfNecessary(context, viewId);
        }

        return restoredView;
    }

    @Override
    public void renderView(FacesContext context, UIViewRoot viewToRender) throws IOException {
        if (isDevelopment(context)) {
            validateComponentTreeStructure(context, viewToRender);
        }

        if (isViewResourceRequest(context)) {
            var contentType = getMimeType(context, getRequestServletPath(context));
            var characterEncoding = UTF_8.name();

            var externalContext = context.getExternalContext();
            externalContext.setResponseContentType(contentType);
            externalContext.setResponseCharacterEncoding(characterEncoding);
            context
                .setResponseWriter(context.getRenderKit().createResponseWriter(externalContext.getResponseOutputWriter(), XML_CONTENT_TYPE, characterEncoding));
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
     * Create a dummy view and trigger {@link FacesContext#responseComplete()} so that it won't be built nor rendered.
     */
    private UIViewRoot createServiceWorkerView(FacesContext context, String viewId) {
        var createdView = super.createView(context, viewId);
        context.responseComplete();
        return createdView;
    }

    /**
     * Create a dummy view, restore only the view root state and, if present, then immediately explicitly destroy the view scoped beans. On a distributable
     * deployment (<code>&lt;distributable&gt;</code> in <code>web.xml</code>) the actual
     * {@link Hacks#removeViewState(FacesContext, ResponseStateManager, String)} call is deferred to the next {@link #createView(FacesContext, String)} call via
     * {@link #registerPendingViewStateRemoval(FacesContext, String)}, so that the unload beacon no longer concurrently mutates the session and last-writer-wins
     * conflicts in distributed session stores are prevented (see issue #941). On a non-distributable deployment, or when Spring WebFlow's
     * {@code FlowViewHandler} is detected in the wrapped chain, the removal is performed synchronously here; deferring it is pointless on a non-distributable
     * deployment, and on Spring WebFlow the captured view ID is tied to a transient flow execution and no longer resolves during the next request (see issue
     * #952). Or, if the session is new (during an unload request, it implies it had expired), then explicitly send a permanent redirect to the original request
     * URI. This way any authentication framework which remembers the "last requested restricted URL" will redirect back to correct (non-unload) URL after login
     * on a new session.
     */
    private UIViewRoot unloadView(FacesContext context, String viewId) {
        var createdView = super.createView(context, viewId);
        var manager = getRenderKit(context).getResponseStateManager();

        if (restoreViewRootState(context, manager, createdView)) {
            context.setProcessingEvents(true);
            context.getApplication().publishEvent(context, PreDestroyViewMapEvent.class, UIViewRoot.class, createdView);

            // Use createdView.getViewId() rather than the raw URL viewId so that the canonical (post-deriveLogicalViewId)
            // form is used when reconstructing the MyFaces SerializedViewKey. Otherwise the extensionless mapping case
            // (e.g. raw "/pages/desktop" vs. canonical "/pages/desktop.xhtml") yields a different viewId.hashCode() and
            // the view state is never actually removed (see issue #952).
            var canonicalViewId = createdView.getViewId();

            if (usePendingViewStateRemoval) {
                registerPendingViewStateRemoval(context, canonicalViewId);
            }
            else {
                Hacks.removeViewState(context, manager, canonicalViewId);
            }
        }
        else if (isSessionNew(context)) {
            redirectPermanent(context, getRequestURIWithQueryString(context));
        }

        context.responseComplete();
        return createdView;
    }

    /**
     * Restore only the view root state. This ensures that the view scope map and all view root component system event listeners are also restored (including
     * those for {@link PreDestroyViewMapEvent}). This is done so because calling <code>super.restoreView()</code> would implicitly also build the entire view
     * and restore state of all other components in the tree. This is unnecessary during an unload request.
     */
    private static boolean restoreViewRootState(FacesContext context, ResponseStateManager manager, UIViewRoot view) {
        var state = manager.getState(context, view.getViewId());

        if (!(state instanceof Object[] stateArray) || stateArray.length < 2) {
            return false;
        }

        var componentState = stateArray[1];
        Object viewRootState = null;

        if (componentState instanceof Map<?, ?> componentStateMap) { // Partial state saving.
            if (view.getId() == null) { // MyFaces.
                view.setId(view.createUniqueId(context, null));
                view.markInitialState();
            }

            viewRootState = componentStateMap.get(view.getClientId(context));
        }
        else if (componentState instanceof Object[] componentStateArray) { // Full state saving.
            viewRootState = componentStateArray[0];
        }

        if (viewRootState != null) {
            var viewId = view.getViewId();
            view.restoreState(context, viewRootState);
            view.setViewId(viewId);
            context.setViewRoot(view);
            return true;
        }

        return false;
    }

    private static void registerPendingViewStateRemoval(FacesContext context, String viewId) {
        getSessionAttribute(context, SESSION_ATTRIBUTE_PENDING_VIEW_STATE_REMOVALS, ConcurrentLinkedQueue::new)
            .add(new SimpleImmutableEntry<>(getRequestParameter(context, VIEW_STATE_PARAM), viewId)); // Map.entry is not Serializable!
    }

    private void performPendingViewStateRemovals(FacesContext context) {
        if (hasSession(context)) {
            Queue<Entry<String, String>> queue = getSessionAttribute(context, SESSION_ATTRIBUTE_PENDING_VIEW_STATE_REMOVALS);

            if (queue != null) {
                Entry<String, String> pending;

                while ((pending = queue.poll()) != null) {
                    var viewRoot = createViewForViewStateRemoval(context, pending.getValue());
                    var manager = getRenderKit(context).getResponseStateManager();
                    var temporaryContext = new RemoveViewStateFacesContext(context, viewRoot, pending.getKey());

                    try {
                        setContext(temporaryContext);

                        if (restoreViewRootState(temporaryContext, manager, viewRoot)) {
                            Hacks.removeViewState(temporaryContext, manager, viewRoot.getViewId());
                        }
                    }
                    finally {
                        setContext(context);
                    }
                }
            }
        }
    }

    /**
     * Create a placeholder view used during {@link #performPendingViewStateRemovals(FacesContext)} to carry the view root state long enough for the associated
     * view state to be located and removed. The view is neither built nor rendered.
     * <p>
     * Unlike {@link #unloadView(FacesContext, String)}, which runs during the actual unload request, this runs during a later unrelated request where
     * {@code super.createView(context, viewId)} may return {@code null} - observed on Spring {@code FlowViewHandler}, which previously caused a
     * {@link NullPointerException} in {@link #restoreViewRootState(FacesContext, ResponseStateManager, UIViewRoot)} (see issue #952). In that case this method
     * works around it by obtaining the view directly from the view declaration language. As a defensive last resort, if that also returns {@code null}, a bare
     * {@link UIViewRoot} is instantiated; this is guaranteed to work on Mojarra, whereas on MyFaces the absence of a generated id is already compensated for in
     * {@link #restoreViewRootState(FacesContext, ResponseStateManager, UIViewRoot)}.
     */
    private UIViewRoot createViewForViewStateRemoval(FacesContext context, String viewId) {
        var viewRoot = super.createView(context, viewId);

        if (viewRoot == null) {
            var vdl = context.getApplication().getViewHandler().getViewDeclarationLanguage(context, viewId);

            if (vdl != null) {
                viewRoot = vdl.createView(context, viewId);
            }

            if (viewRoot == null) {
                viewRoot = new UIViewRoot();
                viewRoot.setViewId(viewId);
            }
        }

        return viewRoot;
    }

    /**
     * Create and build the view and return it if it indeed contains {@link EnableRestorableView}, else return null.
     */
    private static UIViewRoot createRestorableViewIfNecessary(FacesContext context, String viewId) {
        try {
            var createdView = buildView(context, viewId);
            return isRestorableView(createdView) ? createdView : null;
        }
        catch (IOException e) {
            throw new FacesException(e);
        }
    }

    private void validateComponentTreeStructure(FacesContext context, UIViewRoot view) {
        checkNestedForms(context, view, null);
    }

    private void checkNestedForms(FacesContext context, UIComponent parent, UIForm nestedParent) {
        for (var child : parent.getChildren()) { // Historical note: UIViewRoot#visitTree() is inappropriate for this task: #653
            UIForm form = null;

            if (child instanceof UIForm formChild) {
                form = formChild;

                if (nestedParent != null && (!Hacks.isNestedInPrimeFacesDialog(form) || Hacks.isNestedInPrimeFacesDialog(form, nestedParent))) {
                    throw new IllegalStateException(
                        ERROR_NESTED_FORM_ENCOUNTERED.formatted(form.getClientId(context), nestedParent.getClientId(context))
                    );
                }
            }

            checkNestedForms(context, child, form);
        }
    }

    // Inner classes -------------------------------------------------------------------------------------------------

    private static class RemoveViewStateFacesContext extends FacesContextWrapper {

        private static final int ATTRIBUTES_SET_DURING_REMOVE_VIEW_STATE = 4;

        private UIViewRoot viewRoot;
        private final Map<Object, Object> attributes;
        private final ExternalContext externalContext;

        public RemoveViewStateFacesContext(FacesContext wrapped, UIViewRoot viewRoot, String viewState) {
            super(wrapped);
            this.viewRoot = viewRoot;
            this.attributes = new HashMap<>(ATTRIBUTES_SET_DURING_REMOVE_VIEW_STATE);
            this.externalContext = new RemoveViewStateExternalContext(wrapped.getExternalContext(), viewState);
        }

        @Override
        public UIViewRoot getViewRoot() {
            return viewRoot;
        }

        @Override
        public void setViewRoot(UIViewRoot viewRoot) {
            this.viewRoot = viewRoot;
        }

        @Override
        public Map<Object, Object> getAttributes() {
            return attributes;
        }

        @Override
        public ExternalContext getExternalContext() {
            return externalContext;
        }

        @Override
        public RenderKit getRenderKit() {
            return FacesLocal.getRenderKit(this);
        }

    }

    private static class RemoveViewStateExternalContext extends ExternalContextWrapper {

        private final Map<String, String> requestParameterMap;

        private RemoveViewStateExternalContext(ExternalContext wrapped, String viewState) {
            super(wrapped);
            this.requestParameterMap = Map.of(VIEW_STATE_PARAM, viewState);
        }

        @Override
        public Map<String, String> getRequestParameterMap() {
            return requestParameterMap;
        }

    }

    private static class RenderViewResourceFacesContext extends FacesContextWrapper {

        private final ExternalContext externalContext;

        private RenderViewResourceFacesContext(FacesContext wrapped) {
            super(wrapped);
            var defaultMapping = getDefaultFacesServletMapping(getServletContext(getWrapped()));
            var prefixMapping = isPrefixMapping(defaultMapping);
            var requestPathInfo = prefixMapping ? defaultMapping : null;
            var requestServletPath = getRequestServletPath(getWrapped()) + (prefixMapping ? "" : defaultMapping);
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
