package org.omnifaces.util;

import static jakarta.faces.application.ResourceHandler.JSF_SCRIPT_LIBRARY_NAME;
import static jakarta.faces.application.ResourceHandler.JSF_SCRIPT_RESOURCE_NAME;
import static jakarta.faces.application.StateManager.IS_BUILDING_INITIAL_STATE;
import static jakarta.faces.component.UIComponent.getCompositeComponentParent;
import static jakarta.faces.component.behavior.ClientBehaviorContext.BEHAVIOR_EVENT_PARAM_NAME;
import static jakarta.faces.component.behavior.ClientBehaviorContext.BEHAVIOR_SOURCE_PARAM_NAME;
import static jakarta.faces.component.search.SearchExpressionContext.createSearchExpressionContext;
import static jakarta.faces.component.search.SearchExpressionHint.IGNORE_NO_RESULT;
import static jakarta.faces.component.search.SearchExpressionHint.RESOLVE_SINGLE_COMPONENT;
import static jakarta.faces.component.visit.VisitContext.createVisitContext;
import static jakarta.faces.component.visit.VisitHint.SKIP_ITERATION;
import static jakarta.faces.event.PhaseId.RENDER_RESPONSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.logging.Level.FINEST;
import static java.util.regex.Pattern.quote;
import static org.omnifaces.resourcehandler.DefaultResourceHandler.FACES_SCRIPT_RESOURCE_NAME;
import static org.omnifaces.util.AjaxLocal.load;
import static org.omnifaces.util.AjaxLocal.oncomplete;
import static org.omnifaces.util.Components.addExpressionStringIfNotNull;
import static org.omnifaces.util.Components.createScriptResource;
import static org.omnifaces.util.Components.findComponentsInChildren;
import static org.omnifaces.util.Components.getBehaviorListeners;
import static org.omnifaces.util.Components.getClosestParent;
import static org.omnifaces.util.Components.isRendered;
import static org.omnifaces.util.Components.setAttribute;
import static org.omnifaces.util.Events.subscribeToRequestBeforePhase;
import static org.omnifaces.util.Faces.getApplicationAttribute;
import static org.omnifaces.util.Faces.setContext;
import static org.omnifaces.util.FacesLocal.createConverter;
import static org.omnifaces.util.FacesLocal.getFaceletContext;
import static org.omnifaces.util.FacesLocal.getRenderKit;
import static org.omnifaces.util.FacesLocal.getRequestParameter;
import static org.omnifaces.util.FacesLocal.getRequestQueryStringMap;
import static org.omnifaces.util.FacesLocal.getViewParameterMap;
import static org.omnifaces.util.FacesLocal.isAjaxRequestWithPartialRendering;
import static org.omnifaces.util.FacesLocal.isDevelopment;
import static org.omnifaces.util.FacesLocal.normalizeViewId;
import static org.omnifaces.util.Hacks.isFacesScriptResourceAvailable;
import static org.omnifaces.util.Messages.addError;
import static org.omnifaces.util.Reflection.accessField;
import static org.omnifaces.util.Utils.coalesce;
import static org.omnifaces.util.Utils.isEmpty;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;
import jakarta.faces.application.Application;
import jakarta.faces.application.ResourceHandler;
import jakarta.faces.application.ViewHandler;
import jakarta.faces.component.ActionSource2;
import jakarta.faces.component.EditableValueHolder;
import jakarta.faces.component.NamingContainer;
import jakarta.faces.component.UICommand;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIComponentBase;
import jakarta.faces.component.UIForm;
import jakarta.faces.component.UIInput;
import jakarta.faces.component.UIMessage;
import jakarta.faces.component.UIMessages;
import jakarta.faces.component.UINamingContainer;
import jakarta.faces.component.UIOutput;
import jakarta.faces.component.UIParameter;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.component.ValueHolder;
import jakarta.faces.component.behavior.AjaxBehavior;
import jakarta.faces.component.behavior.ClientBehavior;
import jakarta.faces.component.behavior.ClientBehaviorHolder;
import jakarta.faces.component.html.HtmlBody;
import jakarta.faces.component.search.SearchExpressionContext;
import jakarta.faces.component.search.SearchExpressionHint;
import jakarta.faces.component.visit.VisitCallback;
import jakarta.faces.component.visit.VisitContext;
import jakarta.faces.component.visit.VisitHint;
import jakarta.faces.component.visit.VisitResult;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.FacesContextWrapper;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.convert.Converter;
import jakarta.faces.event.ActionEvent;
import jakarta.faces.event.ActionListener;
import jakarta.faces.event.AjaxBehaviorEvent;
import jakarta.faces.event.BehaviorListener;
import jakarta.faces.event.MethodExpressionActionListener;
import jakarta.faces.render.RenderKit;
import jakarta.faces.view.ViewDeclarationLanguage;
import jakarta.faces.view.facelets.FaceletContext;

import org.omnifaces.component.ParamHolder;
import org.omnifaces.component.input.Form;
import org.omnifaces.config.OmniFaces;
import org.omnifaces.el.ScopedRunner;

/**
 * <p>
 * Collection of utility methods for the Faces API with respect to working with {@link UIComponent}. There are several
 * traversal/lookup methods, there are several {@link UIForm} and {@link UIInput} related methods which makes it easier
 * to deal with forms and inputs.
 * <p>
 * The difference with {@link Components} is that no one method of {@link ComponentsLocal} obtains the {@link FacesContext}
 * from the current thread by {@link FacesContext#getCurrentInstance()}. This job is up to the caller. This is more
 * efficient in situations where multiple utility methods needs to be called at the same time. Invoking
 * {@link FacesContext#getCurrentInstance()} is at its own an extremely cheap operation, however as it's to be obtained
 * as a {@link ThreadLocal} variable, it's during the call still blocking all other running threads for some nanoseconds
 * or so.
 *
 * @author Bauke Scholtz
 * @since 4.6
 * @see Components
 */
public final class ComponentsLocal {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final Logger logger = Logger.getLogger(ComponentsLocal.class.getName());

    /** The name of the label attribute. */
    public static final String LABEL_ATTRIBUTE = "label";

    /** The name of the value attribute. */
    public static final String VALUE_ATTRIBUTE = "value";

    private static final String ERROR_MISSING_PARENT =
            "Component '%s' must have a parent of type '%s', but it cannot be found.";
    private static final String ERROR_MISSING_DIRECT_PARENT =
            "Component '%s' must have a direct parent of type '%s', but it cannot be found.";
    private static final String ERROR_MISSING_CHILD =
            "Component '%s' must have at least one child of type '%s', but it cannot be found.";
    private static final String ERROR_ILLEGAL_PARENT =
            "Component '%s' may not have a parent of type '%s'.";
    private static final String ERROR_ILLEGAL_CHILDREN =
            "Component '%s' may only have children of type '%s'. Encountered children of types '%s'.";
    private static final String ERROR_CHILDREN_DISALLOWED =
            "Component '%s' may not have any children. Encountered children of types '%s'.";
    private static final String ERROR_ILLEGAL_UIINPUT =
            "Relative client ID '%s' must represent an UIInput component, but encountered '%s'.";

    private static final Set<SearchExpressionHint> RESOLVE_LABEL_FOR = EnumSet.of(RESOLVE_SINGLE_COMPONENT, IGNORE_NO_RESULT);

    // Constructors ---------------------------------------------------------------------------------------------------

    private ComponentsLocal() {
        // Hide constructor.
    }

    // General --------------------------------------------------------------------------------------------------------

    /**
     * Returns the current UI component from the EL context.
     * @param <C> The expected component type.
     * @param context The current {@link FacesContext}
     * @return The current UI component from the EL context.
     * @throws ClassCastException When <code>C</code> is of wrong type.
     * @see UIComponent#getCurrentComponent(FacesContext)
     * @since 4.6
     */
    @SuppressWarnings("unchecked")
    public static <C extends UIComponent> C getCurrentComponent(FacesContext context) {
        return (C) UIComponent.getCurrentComponent(context);
    }

    // Traversal ------------------------------------------------------------------------------------------------------

    /**
     * Returns the UI component matching the given client ID search expression.
     * @param <C> The expected component type.
     * @param context The involved {@link FacesContext}.
     * @param clientId The client ID search expression.
     * @return The UI component matching the given client ID search expression.
     * @throws ClassCastException When <code>C</code> is of wrong type.
     * @see UIComponent#findComponent(String)
     * @since 4.6
     */
    @SuppressWarnings("unchecked")
    public static <C extends UIComponent> C findComponent(FacesContext context, String clientId) {
        return (C) context.getViewRoot().findComponent(clientId);
    }

    /**
     * Returns the UI component matching the given client ID search expression relative to the point
     * in the component tree of the given component. For this search both parents and children are
     * consulted, increasingly moving further away from the given component. Parents are consulted
     * first, then children.
     *
     * @param <C> The expected component type.
     * @param context The involved {@link FacesContext}.
     * @param component the component from which the relative search is started.
     * @param clientId The client ID search expression.
     * @return The UI component matching the given client ID search expression.
     * @throws ClassCastException When <code>C</code> is of wrong type.
     * @see UIComponent#findComponent(String)
     * @since 4.6
     */
    @SuppressWarnings("unchecked")
    public static <C extends UIComponent> C findComponentRelatively(FacesContext context, UIComponent component, String clientId) {

        if (isEmpty(clientId)) {
            return null;
        }

        // Search first in the naming container parents of the given component
        UIComponent result = findComponentInParents(context, component, clientId);

        if (result == null) {
            // If not in the parents, search from the root
            result = findComponentInChildren(context, context.getViewRoot(), clientId);
        }

        return (C) result;
    }

    /**
     * Returns the UI component matching the given client ID search expression relative to the point
     * in the component tree of the given component, searching only in its parents.
     *
     * @param <C> The expected component type.
     * @param context The involved {@link FacesContext}.
     * @param component the component from which the relative search is started.
     * @param clientId The client ID search expression.
     * @return The UI component matching the given client ID search expression.
     * @throws ClassCastException When <code>C</code> is of wrong type.
     * @see UIComponent#findComponent(String)
     * @since 4.6
     */
    @SuppressWarnings("unchecked")
    public static <C extends UIComponent> C findComponentInParents(FacesContext context, UIComponent component, String clientId) {

        if (isEmpty(clientId)) {
            return null;
        }

        for (UIComponent parent = component; parent != null; parent = parent.getParent()) {
            if (parent instanceof NamingContainer || parent instanceof UIViewRoot) {
                UIComponent result = findComponentIgnoringIAE(context, parent, clientId);

                if (result != null) {
                    return (C) result;
                }
            }
        }

        return null;
    }

    /**
     * Returns the UI component matching the given client ID search expression relative to the point
     * in the component tree of the given component, searching only in its children.
     *
     * @param <C> The expected component type.
     * @param context The involved {@link FacesContext}.
     * @param component the component from which the relative search is started.
     * @param clientId The client ID search expression.
     * @return The UI component matching the given client ID search expression.
     * @throws ClassCastException When <code>C</code> is of wrong type.
     * @see UIComponent#findComponent(String)
     * @since 4.6
     */
    @SuppressWarnings("unchecked")
    public static <C extends UIComponent> C findComponentInChildren(FacesContext context, UIComponent component, String clientId) {

        if (isEmpty(clientId)) {
            return null;
        }

        for (UIComponent child : component.getChildren()) {

            UIComponent result = null;
            if (child instanceof NamingContainer) {
                result = findComponentIgnoringIAE(context, child, clientId);
            }

            if (result == null) {
                result = findComponentInChildren(context, child, clientId);
            }

            if (result != null) {
                return (C) result;
            }
        }

        return null;
    }

    /**
     * Returns a list of UI components matching the given type in children of the currently submitted form.
     * The currently submitted form is obtained by {@link #getCurrentForm(FacesContext)}.
     * @param <C> The generic component type.
     * @param context The involved {@link FacesContext}.
     * @param type The type of the UI components to be searched in children of the currently submitted form.
     * @return A list of UI components matching the given type in children of the currently submitted form.
     * @since 4.6
     */
    public static <C extends UIComponent> List<C> findComponentsInCurrentForm(FacesContext context, Class<C> type) {
        UIForm currentForm = getCurrentForm(context);
        return currentForm != null ? findComponentsInChildren(currentForm, type) : emptyList();
    }

    // Iteration / Visiting -------------------------------------------------------------------------------------------

    /**
     * Invokes an operation on every component in the component tree.
     * <p>
     * This is a simplified version of regular component visiting that uses the builder pattern to provide the various
     * optional parameters. Includes supports for only visiting components of a certain class type and two
     * simplified functional interfaces / lambdas.
     * <p>
     * See {@link UIComponent#visitTree(VisitContext, VisitCallback)}
     *
     * @param context the faces context used for tree visiting
     * @return A new instance of {@link Components.ForEach}, using the given faces context.
     * @since 4.6
     */
    public static Components.ForEach forEachComponent(FacesContext context) {
        return new Components.ForEach(context);
    }

    // Creation -------------------------------------------------------------------------------------------------------

    /**
     * Creates a new component.
     * @param <C> The generic component type.
     * @param context The involved {@link FacesContext}.
     * @param componentType The component type to create.
     * @return The newly created component.
     * @see Application#createComponent(String)
     * @throws ClassCastException When <code>C</code> is of wrong type.
     * @since 4.6
     */
    @SuppressWarnings("unchecked")
    public static <C extends UIComponent> C createComponent(FacesContext context, String componentType) {
        return (C) context.getApplication().createComponent(componentType);
    }

    // Manipulation ---------------------------------------------------------------------------------------------------

    /**
     * Include the Facelet file at the given (relative) path as child of the given UI component parent. This has the
     * same effect as using <code>&lt;ui:include&gt;</code>. The path is relative to the current view ID and absolute
     * to the web content root.
     * @param context The involved {@link FacesContext}.
     * @param parent The parent component to include the Facelet file in.
     * @param path The (relative) path to the Facelet file.
     * @throws IOException Whenever given path cannot be read.
     * @see FaceletContext#includeFacelet(UIComponent, String)
     * @since 4.6
     */
    public static void includeFacelet(FacesContext context, UIComponent parent, String path) throws IOException {
        getFaceletContext(context).includeFacelet(parent, path);
    }

    /**
     * Create and include the composite component of the given library and resource name as child of the given UI
     * component parent, set the given attributes on it and return the created composite component.
     * This has the same effect as using <code>xmlns:my="http://xmlns.jcp.org/jsf/composite/libraryName</code> and
     * <code>&lt;my:tagName&gt;</code>. The given component ID must be unique relative to the current naming
     * container parent and is mandatory for functioning of input components inside the composite, if any.
     * <p>
     * The attribute values must represent literal values or literal EL expressions, exactly like as you would declare
     * in the view file. E.g.
     * <pre>
     * attributes.put("foo", "#{bean.foo}");
     * attributes.put("bar", "true");
     * attributes.put("baz", "#{bean.baz(" + someId + ")}");
     * </pre>
     * @param context The involved {@link FacesContext}.
     * @param parent The parent component to include the composite component in.
     * @param libraryName The library name of the composite component (path after "http://xmlns.jcp.org/jsf/composite/").
     * @param tagName The tag name of the composite component.
     * @param id The component ID of the composite component.
     * @param attributes The attributes to be set on the composite component.
     * @return The created composite component, which can if necessary be used to set more custom attributes on it.
     * @since 4.6
     */
    public static UIComponent includeCompositeComponent(FacesContext context, UIComponent parent, String libraryName, String tagName, String id, Map<String, String> attributes) {
        String taglibURI = "http://xmlns.jcp.org/jsf/composite/" + libraryName;
        Map<String, Object> attrs = (attributes == null) ? null : new HashMap<>(attributes);

        UIComponent composite = context.getApplication().getViewHandler()
                .getViewDeclarationLanguage(context, context.getViewRoot().getViewId())
                .createComponent(context, taglibURI, tagName, attrs);
        composite.setId(id);
        parent.getChildren().add(composite);
        return composite;
    }

    /**
     * Add given JavaScript code to the current view which is to be executed as an inline script when the rendering is
     * completed. When the current request is {@link Faces#isAjaxRequestWithPartialRendering()}, then it will delegate
     * to {@link Ajax#oncomplete(String...)}, else it will add given JavaScript code as inline script to end of body.
     * @param context The involved {@link FacesContext}.
     * @param script JavaScript code which is to be executed as an inline script.
     * @since 4.6
     */
    public static void addScript(FacesContext context, String script) {

        if (isAjaxRequestWithPartialRendering(context)) {
            oncomplete(context, script);
        }
        else if (context.getCurrentPhaseId() != RENDER_RESPONSE) {
            subscribeToRequestBeforePhase(RENDER_RESPONSE, () -> addScriptToBody(context, script)); // Just to avoid it misses when view rebuilds in the meanwhile.
        }
        else {
            addScriptToBody(context, script);
        }
    }

    /**
     * Add given JavaScript resource to the current view. This will first check if the resource isn't already rendered
     * as per {@link ResourceHandler#isResourceRendered(FacesContext, String, String)}. If not, then continue as below:
     * <ul>
     * <li>When the current request is a {@link Faces#isAjaxRequestWithPartialRendering()}, then it will delegate to
     * {@link Ajax#load(String, String)}.</li>
     * <li>Else when the <code>&lt;h:head&gt;</code> has not yet been rendered, then add given JavaScript resource to
     * head.</li>
     * <li>Else add given JavaScript resource to end of the <code>&lt;h:body&gt;</code>.</li>
     * </ul>
     * @param context The involved {@link FacesContext}.
     * @param libraryName Library name of the JavaScript resource.
     * @param resourceName Resource name of the JavaScript resource.
     * @since 4.6
     */
    public static void addScriptResource(FacesContext context, String libraryName, String resourceName) {

        if (!context.getApplication().getResourceHandler().isResourceRendered(context, resourceName, libraryName)) {
            if (isAjaxRequestWithPartialRendering(context)) {
                load(context, libraryName, resourceName);
            }
            else if (context.getCurrentPhaseId() != RENDER_RESPONSE) {
                addScriptResourceToHead(context, libraryName, resourceName);
                subscribeToRequestBeforePhase(RENDER_RESPONSE, () -> addScriptResourceToBody(context, libraryName, resourceName)); // Fallback in case view rebuilds in the meanwhile. It will re-check if already added.
            }
            else if (TRUE.equals(context.getAttributes().get(IS_BUILDING_INITIAL_STATE))) {
                addScriptResourceToHead(context, libraryName, resourceName);
            }
            else {
                addScriptResourceToBody(context, libraryName, resourceName);
            }
        }
    }

    /**
     * Add the Faces JavaScript resource to current view. If Faces 4.0+ is present, then it will add the
     * <code>jakarta.faces:faces.js</code> resource, else it will add the <code>jakarta.faces:jsf.js</code> resource.
     * @param context The involved {@link FacesContext}.
     * @since 4.6
     */
    public static void addFacesScriptResource(FacesContext context) {
        addScriptResource(context, JSF_SCRIPT_LIBRARY_NAME, isFacesScriptResourceAvailable() ? FACES_SCRIPT_RESOURCE_NAME : JSF_SCRIPT_RESOURCE_NAME);
    }

    private static UIComponent addScriptResourceToTarget(FacesContext context, String libraryName, String resourceName, String target) {
        String id = (libraryName != null ? (libraryName.replaceAll("\\W+", "_") + "_") : "") + resourceName.replaceAll("\\W+", "_");

        for (UIComponent existingResource : context.getViewRoot().getComponentResources(context)) {
            if (id.equals(existingResource.getId())) {
                return existingResource;
            }
        }

        UIOutput outputScript = createScriptResource();
        outputScript.setId(id);

        if (libraryName != null) {
            outputScript.getAttributes().put("library", libraryName);
        }

        outputScript.getAttributes().put("name", resourceName);
        return addComponentResource(context, outputScript, target);
    }

    private static void addScriptResourceToHead(FacesContext context, String libraryName, String resourceName) {
        addScriptResourceToTarget(context, libraryName, resourceName, "head");
    }

    private static void addScriptResourceToBody(FacesContext context, String libraryName, String resourceName) {
        addScriptResourceToTarget(context, libraryName, resourceName, "body");
    }

    private static UIComponent addComponentResource(FacesContext context, UIComponent resource, String target) {

        if (resource.getId() == null) {
            Hacks.setComponentResourceUniqueId(context, resource);
        }

        context.getViewRoot().addComponentResource(context, resource, target);
        return resource;
    }

    private static void addScriptToBody(FacesContext context, String script) {
        UIOutput outputScript = createScriptResource();
        UIOutput content = new UIOutput();
        content.setValue(script);
        outputScript.getChildren().add(content);
        addComponentResource(context, outputScript, "body");
    }

    // Building / rendering -------------------------------------------------------------------------------------------

    /**
     * Creates and builds a local view for the given view ID independently from the current view.
     * @param context The involved {@link FacesContext}.
     * @param viewId The ID of the view which needs to be created and built.
     * @return A fully populated component tree of the given view ID.
     * @throws IOException Whenever something fails at I/O level. This can happen when the given view ID is unavailable or malformed.
     * @since 4.6
     * @see ViewHandler#createView(FacesContext, String)
     * @see ViewDeclarationLanguage#buildView(FacesContext, UIViewRoot)
     */
    public static UIViewRoot buildView(FacesContext context, String viewId) throws IOException {
        String normalizedViewId = normalizeViewId(context, viewId);
        ViewHandler viewHandler = context.getApplication().getViewHandler();
        UIViewRoot view = viewHandler.createView(context, normalizedViewId);
        FacesContext temporaryContext = new TemporaryViewFacesContext(context, view);

        try {
            setContext(temporaryContext);
            viewHandler.getViewDeclarationLanguage(temporaryContext, normalizedViewId).buildView(temporaryContext, view);
        }
        finally {
            setContext(context);
        }

        return view;
    }

    /**
     * Encodes the given component locally as HTML, with UTF-8 character encoding, independently from the current view.
     * The current implementation, however, uses the current faces context. The same managed beans as in the current
     * faces context will be available as well, including request scoped ones. But, depending on the nature of the
     * provided component, the state of the faces context may be affected because the attributes of the context,
     * request, view, session and application scope could be (in)directly manipulated during the encode. This may or may
     * not have the desired effect. If the given view does not have any component resources, Faces forms, dynamically
     * added components, component event listeners, then it should mostly be safe.
     * In other words, use this at most for "simple templates" only, e.g. an HTML based mail template, which usually
     * already doesn't have an HTML head nor body.
     * @param context The involved {@link FacesContext}.
     * @param component The component to capture HTML output for.
     * @return The encoded HTML output of the given component.
     * @throws UncheckedIOException Whenever something fails at I/O level. This would be quite unexpected as it happens locally.
     * @since 4.6
     * @see UIComponent#encodeAll(FacesContext)
     */
    public static String encodeHtml(FacesContext context, UIComponent component) {
        ResponseWriter originalWriter = context.getResponseWriter();
        StringWriter output = new StringWriter(2048); // is 2kb enough for simple templates?
        context.setResponseWriter(getRenderKit(context).createResponseWriter(output, "text/html", "UTF-8"));

        try {
            component.encodeAll(context);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        finally {
            if (originalWriter != null) {
                context.setResponseWriter(originalWriter);
            }
        }

        return output.toString();
    }

    // Forms ----------------------------------------------------------------------------------------------------------

    /**
     * Returns the currently submitted UI form component, or <code>null</code> if there is none, which may happen when
     * the current request is not a postback request at all, or when the view has been changed by for example a
     * successful navigation. If the latter is the case, you'd better invoke this method before navigation.
     * @param context The current {@link FacesContext}
     * @return The currently submitted UI form component.
     * @see UIForm#isSubmitted()
     * @since 4.6
     */
    public static UIForm getCurrentForm(FacesContext context) {

        if (!context.isPostback()) {
            return null;
        }

        UIViewRoot viewRoot = context.getViewRoot();

        if (viewRoot == null) {
            return null;
        }

        // The initial implementation has visited the tree for UIForm components which returns true on isSubmitted().
        // But with testing it turns out to return false on ajax requests where the form is not included in execute!
        // The current implementation just walks through the request parameter map instead.

        for (String name : context.getExternalContext().getRequestParameterMap().keySet()) {
            if (name.startsWith("jakarta.faces.")) {
                continue; // Quick skip.
            }

            UIComponent component = findComponentIgnoringIAE(context, viewRoot, name);

            if (component instanceof UIForm) {
                return (UIForm) component;
            }
            else if (component != null) {
                UIForm form = getClosestParent(component, UIForm.class);

                if (form != null) {
                    return form;
                }
            }
        }

        return null;
    }

    /**
     * Returns the currently invoked UI command component, or <code>null</code> if there is none, which may happen when
     * the current request is not a postback request at all, or when the view has been changed by for example a
     * successful navigation. If the latter is the case, you'd better invoke this method before navigation.
     * @param context The involved {@link FacesContext}.
     * @return The currently invoked UI command component.
     * @since 4.6
     */
    public static UICommand getCurrentCommand(FacesContext context) {
        UIComponent source = getCurrentActionSource(context);
        return source instanceof UICommand ? (UICommand) source : null;
    }

    /**
     * Returns the source of the currently invoked action, or <code>null</code> if there is none, which may happen when
     * the current request is not a postback request at all, or when the view has been changed by for example a
     * successful navigation. If the latter is the case, you'd better invoke this method before navigation.
     * @param <C> The expected component type.
     * @param context The involved {@link FacesContext}.
     * @return The source of the currently invoked action.
     * @since 4.6
     */
    @SuppressWarnings("unchecked")
    public static <C extends UIComponent> C getCurrentActionSource(FacesContext context) {

        if (!context.isPostback()) {
            return null;
        }

        return (C) getCurrentActionSource(context, context.getViewRoot());
    }

    /**
     * Helper method for {@link #getCurrentActionSource(FacesContext)}.
     */
    static UIComponent getCurrentActionSource(FacesContext context, UIComponent parent) {
        if (parent == null) {
            return null;
        }

        Map<String, String> params = context.getExternalContext().getRequestParameterMap();

        if (context.getPartialViewContext().isAjaxRequest()) {
            String sourceClientId = params.get(BEHAVIOR_SOURCE_PARAM_NAME);

            if (sourceClientId != null) {
                UIComponent actionSource = findComponentIgnoringIAE(context, parent, sourceClientId);

                if (actionSource != null) {
                    return actionSource;
                }
            }
        }

        for (String name : params.keySet()) {
            if (name.startsWith("jakarta.faces.")) {
                continue; // Quick skip.
            }

            UIComponent actionSource = findComponentIgnoringIAE(context, parent, name);

            if (actionSource instanceof UICommand) {
                return actionSource;
            }
        }

        if (parent instanceof UIViewRoot) { // If still not found and parent is UIViewRoot, then it can happen when prependId="false" is set on form. Hopefully it will be deprecated one day.
            return getCurrentActionSource(context, getCurrentForm(context));
        }

        return null;
    }

    /**
     * Returns the value of the <code>label</code> attribute associated with the given UI component if any, else
     * the client ID. It never returns null.
     * @param context The involved {@link FacesContext}.
     * @param component The UI component for which the label is to be retrieved.
     * @return The value of the <code>label</code> attribute associated with the given UI component if any, else
     * the client ID.
     * @since 4.6
     */
    public static String getLabel(FacesContext context, UIComponent component) {
        String label = getOptionalLabel(context, component);
        return (label != null) ? label : component.getClientId();
    }

    /**
     * Returns the value of the <code>label</code> attribute associated with the given UI component if any, else
     * null.
     * @param context The involved {@link FacesContext}.
     * @param component The UI component for which the label is to be retrieved.
     * @return The value of the <code>label</code> attribute associated with the given UI component if any, else
     * null.
     * @since 4.6
     */
    public static String getOptionalLabel(FacesContext context, UIComponent component) {
        Object[] result = new Object[1];

        new ScopedRunner(context).with("cc", getCompositeComponentParent(component)).invoke(() -> {
            Object label = component.getAttributes().get(LABEL_ATTRIBUTE);

            if (isEmpty(label)) {
                ValueExpression labelExpression = component.getValueExpression(LABEL_ATTRIBUTE);

                if (labelExpression != null) {
                    label = labelExpression.getValue(context.getELContext());
                }
            }

            result[0] = label;
        });

        return (result[0] != null) ? result[0].toString() : null;
    }

    /**
     * Returns the value of the given input component whereby any unconverted submitted string value will immediately
     * be converted/validated as this method is called. This method thus always returns the converted/validated value.
     * @param <T> The expected return type.
     * @param input The input component to obtain the converted/validated value for.
     * @return The converted/validated value of the given input component.
     * @throws ClassCastException When <code>T</code> is of wrong type.
     * @since 4.6
     */
    @SuppressWarnings("unchecked")
    public static <T> T getImmediateValue(FacesContext context, UIInput input) {
        if (input.isValid() && input.getSubmittedValue() != null) {
            input.validate(context);
        }

        return input.isLocalValueSet() ? (T) input.getValue() : null;
    }

    /**
     * Returns the expected type of the "value" attribute of the given component. This is useful in among others a
     * "generic entity converter".
     * @param <T> The expected type of the expected type of the "value" attribute of the given component.
     * @param component The component to obtain the expected type of the "value" attribute for.
     * @return The expected type of the "value" attribute of the given component, or <code>null</code> when there is no such value.
     * @throws ClassCastException When <code>T</code> is of wrong type.
     * @since 4.6
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> getExpectedValueType(FacesContext context, UIComponent component) {
        ValueExpression valueExpression = component.getValueExpression(VALUE_ATTRIBUTE);

        if (valueExpression != null) {
            return getExpectedType(context, valueExpression);
        }
        else {
            Object value = component.getAttributes().get(VALUE_ATTRIBUTE);

            if (value != null) {
                return (Class<T>) value.getClass();
            }

            return null;
        }
    }

    /**
     * Returns the expected type of the given value expression. This first inspects if the
     * {@link ValueExpression#getExpectedType()} returns a specific type, i.e. not <code>java.lang.Object</code>, and
     * then returns it, else it inspects the actual type of the property behind the expression string.
     * @param <T> The expected type of the expected type of the given value expression.
     * @param valueExpression The value expression to obtain the expected type for.
     * @return The expected type of the given value expression.
     * @throws ClassCastException When <code>T</code> is of wrong type.
     * @since 4.6
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> getExpectedType(FacesContext context, ValueExpression valueExpression) {
        Class<?> expectedType = valueExpression.getExpectedType();

        if (expectedType == Object.class) {
            expectedType = valueExpression.getType(context.getELContext());
        }

        return (Class<T>) expectedType;
    }

    /**
     * Returns whether the given component has invoked the form submit. In non-ajax requests, that can only be an
     * {@link UICommand} component. In ajax requests, that can also be among others an {@link UIInput} component.
     * @param context The involved {@link FacesContext}.
     * @param component The component to be checked.
     * @return <code>true</code> if the given component has invoked the form submit.
     * @since 4.6
     */
    public static boolean hasInvokedSubmit(FacesContext context, UIComponent component) {
        UIComponent source = getCurrentActionSource(context);
        return source != null && source.equals(component);
    }

    /**
     * Returns an unmodifiable map with all request query string or view parameters, appended with all child
     * {@link UIParameter} components (<code>&lt;f|o:param&gt;</code>) of the given parent component. Those with
     * <code>disabled=true</code> or an empty name or an empty value are skipped. The <code>&lt;f|o:param&gt;</code>
     * will override any included view or request parameters on the same name.
     * @param context The involved {@link FacesContext}.
     * @param component The parent component to retrieve all child {@link UIParameter} components from.
     * @param includeRequestParams Whether or not to include request query string parameters.
     * When set to <code>true</code>, then this overrides the <code>includeViewParams</code>.
     * @param includeViewParams Whether or not to include view parameters.
     * @return An unmodifiable list with all request query string or view parameters, appended with all child
     * {@link UIParameter} components having a non-empty name and not disabled.
     * @since 4.6
     */
    public static Map<String, List<String>> getParams(FacesContext context, UIComponent component, boolean includeRequestParams, boolean includeViewParams) {
        Map<String, List<String>> params;

        if (includeRequestParams) {
            params = getRequestQueryStringMap(context);
        }
        else if (includeViewParams) {
            params = getViewParameterMap(context);
        }
        else {
            params = new LinkedHashMap<>(0);
        }

        for (ParamHolder<Object> param : Components.getParams(component)) {
            String value = param.getValue();

            if (isEmpty(value)) {
                continue;
            }

            params.put(param.getName(), singletonList(value));
        }

        return Collections.unmodifiableMap(params);
    }

    /**
     * Returns the {@link UIMessage} component associated with given {@link UIInput} component.
     * This returns <code>null</code> if none can be found.
     * @param context The involved {@link FacesContext}.
     * @param input The UI input component to find the associated message component for.
     * @return The {@link UIMessage} component associated with given {@link UIInput} component.
     * @since 4.6
     */
    public static UIMessage getMessageComponent(FacesContext context, UIInput input) {
        UIMessage[] found = new UIMessage[1];

        forEachComponent(context).ofTypes(UIMessage.class).withHints(SKIP_ITERATION).invoke((visitContext, target) -> {
            UIMessage messageComponent = (UIMessage) target;
            String forValue = messageComponent.getFor();

            if (!isEmpty(forValue)) {
                FacesContext facesContext = visitContext.getFacesContext();
                SearchExpressionContext searchExpressionContext = createSearchExpressionContext(facesContext, messageComponent, RESOLVE_LABEL_FOR, null);
                String forClientId = facesContext.getApplication().getSearchExpressionHandler().resolveClientId(searchExpressionContext, forValue);
                UIComponent forComponent = findComponentRelatively(facesContext, messageComponent, forClientId);

                if (input.equals(forComponent)) {
                    found[0] = messageComponent;
                    return VisitResult.COMPLETE;
                }
            }

            return VisitResult.ACCEPT;
        });

        return found[0];
    }

    /**
     * Returns the first {@link UIMessages} component found in the current view.
     * This returns <code>null</code> if none can be found.
     * @param context The involved {@link FacesContext}.
     * @return The first {@link UIMessages} component found in the current view.
     * @since 4.6
     */
    public static UIMessages getMessagesComponent(FacesContext context) {
        UIMessages[] found = new UIMessages[1];

        forEachComponent(context).ofTypes(UIMessages.class).withHints(SKIP_ITERATION).invoke((visitContext, target) -> {
            found[0] = (UIMessages) target;
            return VisitResult.COMPLETE;
        });

        return found[0];
    }

    /**
     * Reset all child {@link UIInput} components enclosed in the given {@link UIForm} component, or the closest
     * {@link UIForm} parent of it.
     * @param component The component representing the {@link UIForm} itself, or to find the closest {@link UIForm}
     * parent for.
     * @throws IllegalArgumentException When given component is not an {@link UIForm}, or does not have a {@link UIForm}
     * parent.
     * @since 4.6
     */
    public static void resetForm(FacesContext context, UIComponent component) {
        UIForm form = (component instanceof UIForm) ? (UIForm) component : getClosestParent(component, UIForm.class);

        if (form == null) {
            throw new IllegalArgumentException(format(ERROR_MISSING_PARENT, component.getClass(), UIForm.class));
        }

        resetInputs(context, form);
    }

    /**
     * Reset all child {@link UIInput} components enclosed in the given parent component.
     * @param component The parent component to reset all child {@link UIInput} components in.
     * @since 4.6
     */
    public static void resetInputs(FacesContext context, UIComponent component) {
        forEachComponent(context).fromRoot(component).ofTypes(UIInput.class).invoke(UIInput::resetValue);
    }

    /**
     * Disable the passed {@link UIInput} component.
     * @param input The {@link UIInput} component to disable.
     * @since 4.6
     */
    public static void disableInput(UIInput input) {
        setAttribute(input, "disabled", true);
    }

    /**
     * Disable the {@link UIInput} component matching the given client ID search expression.
     * @param context The involved {@link FacesContext}.
     * @param clientId The client ID search expression.
     * @since 4.6
     */
    public static void disableInput(FacesContext context, String clientId) {
        disableInput(findComponent(context, clientId));
    }

    /**
     * Add an {@link UIForm} to the current view if absent.
     * This might be needed for scripts which rely on Faces view state identifier and/or on functioning of jsf.ajax.request().
     * @since 4.6
     */
    public static void addFormIfNecessary(FacesContext context) {

        if (isAjaxRequestWithPartialRendering(context)) {
            return; // It's impossible to have this condition without an UIForm in first place.
        }

        UIViewRoot viewRoot = context.getViewRoot();

        if (viewRoot == null || viewRoot.getChildCount() == 0) {
            return; // Empty view. Nothing to do against. The client should probably find a better moment to invoke this.
        }

        VisitCallback visitCallback = (visitContext, target) -> (target instanceof UIForm) ? VisitResult.COMPLETE : VisitResult.ACCEPT;
        boolean formFound = viewRoot.visitTree(createVisitContext(context, null, EnumSet.of(VisitHint.SKIP_ITERATION)), visitCallback);

        if (formFound) {
            return; // UIForm present. No need to add a new one.
        }

        Optional<UIComponent> body = viewRoot.getChildren().stream().filter(HtmlBody.class::isInstance).findFirst();

        if (body.isEmpty()) {
            return; // No <h:body> present. Not possible to add a new UIForm then.
        }

        Form form = new Form();
        form.setId(OmniFaces.OMNIFACES_DYNAMIC_FORM_ID);
        form.getAttributes().put("style", "display:none"); // Just to be on the safe side. There might be CSS which puts visible style such as margin/padding/border on any <form> for some reason.
        body.get().getChildren().add(form);
    }

    /**
     * Convert given value of given value holder to string using either the converter attached to the given value holder
     * or the one obtained via {@link Application#createConverter(Class)} based on the type of the given value.
     * @param <T> The generic value type.
     * @param context The involved {@link FacesContext}.
     * @param holder The value holder.
     * @param value The value to be converted to string.
     * @return The conversion result, may be {@code null}, depending on the value and the converter implementation.
     * @since 4.6
     */
    @SuppressWarnings("unchecked")
    public static <T> String convertToString(FacesContext context, ValueHolder holder, T value) {
        Converter<T> converter = holder.getConverter();

        if (converter == null && value != null) {
            converter = createConverter(context, value.getClass());
        }

        if (converter != null) {
            final UIComponent component;

            if (holder instanceof UIComponent) {
                component = (UIComponent) holder;
            }
            else if (holder instanceof ParamHolder) {
                UIParameter parameter = new UIParameter();
                parameter.setName(((ParamHolder<T>) holder).getName());
                parameter.setValue(value);
                component = parameter;
            }
            else {
                UIOutput output = new UIOutput();
                output.setValue(value);
                component = output;
            }

            return converter.getAsString(context, component, value);
        }

        return (value == null) ? null : value.toString();
    }

    /**
     * Get the rendered value of given value holder. If the given value holder is an instance of
     * {@link EditableValueHolder}, and its {@link EditableValueHolder#getSubmittedValue()} is non-{@code null}, then
     * return it, or if its {@link EditableValueHolder#isLocalValueSet()} is true, then use
     * {@link EditableValueHolder#getLocalValue()} as base value. Else use {@link ValueHolder#getValue()} as base value.
     * Finally return the result of {@link #convertToString(FacesContext, ValueHolder, Object)} with base value as value
     * argument. The result should be exactly the same as displayed during the render response phase.
     * @param context The involved {@link FacesContext}.
     * @param holder The value holder.
     * @return The rendered value, never {@code null}. If the final result was {@code null}, then an empty string is
     * returned.
     * @since 4.6
     */
    public static String getRenderedValue(FacesContext context, ValueHolder holder) {
        Object value = null;

        if (holder instanceof EditableValueHolder) {
            EditableValueHolder editableValueHolder = (EditableValueHolder) holder;
            Object submittedValue = editableValueHolder.getSubmittedValue();

            if (submittedValue != null) {
                return submittedValue.toString();
            }

            if (editableValueHolder.isLocalValueSet()) {
                value = editableValueHolder.getLocalValue();
            }
        }
        else {
            value = holder.getValue();
        }

        return Objects.toString(convertToString(context, holder, value), "");
    }

    /**
     * Invalidate {@link UIInput} components identified by given relative client IDs. They will first be searched using
     * {@link #findComponentRelatively(FacesContext, UIComponent, String)} within the {@link UIForm} returned by
     * {@link #getCurrentForm(FacesContext)} with a fallback to the {@link UIViewRoot}.
     * Then the {@link EditableValueHolder#setValid(boolean)} will be set with {@code false}.
     * @param context The involved {@link FacesContext}.
     * @param relativeClientIds The relative client IDs of {@link UIInput} components to be invalidated.
     * @throws IllegalArgumentException When a relative client ID does not represent an {@link UIInput} component.
     * @since 4.6
     */
    public static void invalidateInputs(FacesContext context, String... relativeClientIds) {
        findAndInvalidateInputs(context, relativeClientIds);
    }

    /**
     * Invalidate {@link UIInput} component identified by given relative client ID via {@link #invalidateInputs(FacesContext,String...)}
     * and calls {@link Messages#addError(String, String, Object...)} on it with the given message body which is
     * formatted with the given parameters.
     * @param context The involved {@link FacesContext}.
     * @param relativeClientId The relative client ID of {@link UIInput} component to be invalidated.
     * @param message The message to be added to the invalidated {@link UIInput} component.
     * @param params The message format parameters, if any.
     * @throws IllegalArgumentException When the relative client ID does not represent an {@link UIInput} component.
     * @since 4.6
     */
    public static void invalidateInput(FacesContext context, String relativeClientId, String message, Object... params) {
        addError(findAndInvalidateInputs(context,relativeClientId).iterator().next(), message, params);
    }

    static Set<String> findAndInvalidateInputs(FacesContext context, String... relativeClientIds) {
        UIComponent root = coalesce(getCurrentForm(context), context.getViewRoot());
        boolean needsVisit = stream(relativeClientIds).anyMatch(clientId -> containsIterationIndex(context, clientId));
        Set<String> fullClientIds = new LinkedHashSet<>();

        for (String relativeClientId : relativeClientIds) {
            UIComponent component = findComponentRelatively(context, root, relativeClientId);

            if (!(component instanceof UIInput)) {
                String type = (component == null) ? "null" : component.getClass().getName();
                throw new IllegalArgumentException(format(ERROR_ILLEGAL_UIINPUT, relativeClientId, type));
            }

            UIInput input = (UIInput) component;
            String fullClientId = input.getClientId();

            if (!needsVisit) {
                fullClientIds.add(fullClientId);
                input.setValid(false);
            }
            else {
                fullClientIds.add(stripIterationIndex(context,fullClientId).replaceAll(quote(stripIterationIndex(context,relativeClientId)) + "$", relativeClientId));
            }
        }

        if (needsVisit) {
            forEachComponent(context).havingIds(fullClientIds).<UIInput>invoke(input -> input.setValid(false));
        }

        context.validationFailed();
        return fullClientIds;
    }


    // Expressions ----------------------------------------------------------------------------------------------------

    /**
     * Create an editable value expression based on the given EL expression and the given type.
     * @param context The current {@link FacesContext}
     * @param expression The EL expression to represent an editable value expression.
     * @param type The type of the property referenced by the value expression.
     * @return The created editable value expression, ready to be used as
     * {@link UIComponent#setValueExpression(String, ValueExpression)}.
     */
    public static ValueExpression createValueExpression(FacesContext context, String expression, Class<?> type) {
        return context.getApplication()
                      .getExpressionFactory()
                      .createValueExpression(context.getELContext(), expression, type);
    }

    /**
     * <p>Create a method expression based on the given EL expression, the given return type and the given parameter types,
     * if any. As an example, the following action method examples,
     * <ul>
     * <li><code>public void submit1()</code></li>
     * <li><code>public String submit2()</code></li>
     * <li><code>public void submit3(String argument)</code></li>
     * <li><code>public String submit4(String argument)</code></li>
     * <li><code>public void submit5(String argument1, Long argument2)</code></li>
     * <li><code>public String submit6(Long argument1, String argument2)</code></li>
     * </ul>
     * <p>can be created as follows:
     * <ul>
     * <li><code>createMethodExpression("#{bean.submit1}", Void.class);</code></li>
     * <li><code>createMethodExpression("#{bean.submit2}", String.class);</code></li>
     * <li><code>createMethodExpression("#{bean.submit3('foo')}", Void.class, String.class);</code></li>
     * <li><code>createMethodExpression("#{bean.submit4('foo')}", String.class, String.class);</code></li>
     * <li><code>createMethodExpression("#{bean.submit5('foo', 0)}", Void.class, String.class, Long.class);</code></li>
     * <li><code>createMethodExpression("#{bean.submit6(0, 'foo')}", String.class, Long.class, String.class);</code></li>
     * </ul>
     * @param expression The EL expression to create a method expression for.
     * @param returnType The return type of the method expression. Can be <code>null</code> if you don't care about the
     * return type (e.g. <code>void</code> or <code>String</code>).
     * @param parameterTypes The parameter types of the method expression.
     * @return The created method expression, ready to be used as
     * {@link UICommand#setActionExpression(MethodExpression)}.
     */
    public static MethodExpression createMethodExpression
    (FacesContext context, String expression, Class<?> returnType, Class<?>... parameterTypes)
    {
        return context.getApplication()
                      .getExpressionFactory()
                      .createMethodExpression(context.getELContext(), expression, returnType, parameterTypes);
    }

    /**
     * <p>Create a void method expression based on the given EL expression and the given parameter types, if any.
     * As an example, the following action method examples,
     * <ul>
     * <li><code>public void submit1()</code></li>
     * <li><code>public void submit3(String argument)</code></li>
     * <li><code>public void submit5(String argument1, Long argument2)</code></li>
     * </ul>
     * <p>can be created as follows:
     * <ul>
     * <li><code>createVoidMethodExpression("#{bean.submit1}");</code></li>
     * <li><code>createVoidMethodExpression("#{bean.submit3('foo')}", String.class);</code></li>
     * <li><code>createVoidMethodExpression("#{bean.submit5('foo', 0)}", String.class, Long.class);</code></li>
     * </ul>
     * @param context The current {@link FacesContext}
     * @param expression The EL expression to create a void method expression for.
     * @param parameterTypes The parameter types of the void method expression.
     * @return The created void method expression, ready to be used as
     * {@link UICommand#setActionExpression(MethodExpression)}.
     */
    public static MethodExpression createVoidMethodExpression(FacesContext context, String expression, Class<?>... parameterTypes) {
        return createMethodExpression(context, expression, Void.class, parameterTypes);
    }

    /**
     * Create an action listener method expression based on the given EL expression. The target method must take an
     * {@link ActionEvent} as argument.
     * As an example, the following action method example,
     * <ul>
     * <li><code>public void actionListener(ActionEvent event)</code></li>
     * </ul>
     * <p>can be created as follows:
     * <ul>
     * <li><code>createActionListenerMethodExpression("#{bean.actionListener}");</code></li>
     * </ul>
     * @param context The current {@link FacesContext}
     * @param expression The EL expression to create an action listener method expression for.
     * @return The created action listener method expression, ready to be used as
     * {@link UICommand#addActionListener(jakarta.faces.event.ActionListener)}.
     */
    public static MethodExpressionActionListener createActionListenerMethodExpression(FacesContext context, String expression) {
        return new MethodExpressionActionListener(createVoidMethodExpression(context,expression, ActionEvent.class));
    }

    /**
     * Create an ajax behavior which should invoke an ajax listener method expression based on the given EL expression.
     * The target method must take an {@link AjaxBehaviorEvent} as argument.
     * As an example, the following ajax listener example,
     * <ul>
     * <li><code>public void ajaxListener(AjaxBehaviorEvent event)</code></li>
     * </ul>
     * <p>can be created as follows:
     * <ul>
     * <li><code>createAjaxBehavior("#{bean.ajaxListener}");</code></li>
     * </ul>
     * <p>Note that this is essentially the programmatic equivalent of <code>&lt;f:ajax&gt;</code>. So if you intented
     * to create for example a <code>&lt;p:ajax&gt;</code> programmatically, then don't use this method.
     * @param expression The EL expression to be invoked when the created ajax behavior is processed.
     * @return The created ajax behavior, ready to be used as
     * {@link UIComponentBase#addClientBehavior(String, ClientBehavior)} whereby the string argument represents the
     * client event name, such as "action", "valueChange", "click", "blur", etc.
     */
    public static AjaxBehavior createAjaxBehavior(FacesContext context, String expression) {
        AjaxBehavior behavior = (AjaxBehavior) context.getApplication().createBehavior(AjaxBehavior.BEHAVIOR_ID);
        MethodExpression method = createVoidMethodExpression(context, expression, AjaxBehaviorEvent.class);
        behavior.addAjaxBehaviorListener(event -> method.invoke(context.getELContext(), new Object[] { event }));
        return behavior;
    }

    /**
     * Returns a list of all action expressions and listeners associated with given component. This covers expressions
     * in <code>action</code> attribute of command components and <code>listener</code> attribute of ajax components.
     * Any method expressions are in format <code>#{bean.method}</code> and any action listeners are added as fully
     * qualified class names. This list is primarily useful for logging postback actions in a phase listener. You can
     * use {@link #getCurrentActionSource(FacesContext)} to obtain the current action source.
     * @param context The current {@link FacesContext}
     * @param component The component to retrieve all action expressions and listeners from.
     * @return A list of all action expressions and listeners associated with given component.
     * @since 4.6
     */
    public static List<String> getActionExpressionsAndListeners(FacesContext context, UIComponent component) {
        List<String> actions = new ArrayList<>();

        if (component instanceof ActionSource2) {
            ActionSource2 source = (ActionSource2) component;
            addExpressionStringIfNotNull(source.getActionExpression(), actions);

            for (ActionListener listener : source.getActionListeners()) {
                addExpressionStringIfNotNull(accessField(listener, MethodExpression.class), actions);
            }
        }

        if (component instanceof ClientBehaviorHolder) {
            String behaviorEvent = getRequestParameter(context, BEHAVIOR_EVENT_PARAM_NAME);

            if (behaviorEvent != null) {
                for (BehaviorListener listener : getBehaviorListeners((ClientBehaviorHolder) component, behaviorEvent)) {
                    addExpressionStringIfNotNull(accessField(listener, MethodExpression.class), actions);
                }
            }
        }

        return unmodifiableList(actions);
    }

    // Hierarchy validation -------------------------------------------------------------------------------------------

    /**
     * Validate in development stage if the given component has a parent of given parent type.
     * @param <C> The generic component type.
     * @param component The component to be validated.
     * @param parentType The parent type to be checked.
     * @throws IllegalStateException When the given component doesn't have any parent of the given type.
     */
    public static <C extends UIComponent> void validateHasParent(FacesContext context, UIComponent component, Class<C> parentType) {
        if (!isDevelopment(context)) {
            return;
        }

        if (getClosestParent(component, parentType) == null) {
            throw new IllegalStateException(format(
                    ERROR_MISSING_PARENT, component.getClass().getSimpleName(), parentType));
        }
    }

    /**
     * Validate in development stage if the given component has a direct parent of given parent type.
     * @param <C> The generic component type.
     * @param context The current {@link FacesContext}
     * @param component The component to be validated.
     * @param parentType The parent type to be checked.
     * @throws IllegalStateException When the given component doesn't have a direct parent of the given type.
     */
    public static <C extends UIComponent> void validateHasDirectParent(FacesContext context, UIComponent component, Class<C> parentType) {
        if (!isDevelopment(context)) {
            return;
        }

        if (!parentType.isInstance(component.getParent())) {
            throw new IllegalStateException(format(
                    ERROR_MISSING_DIRECT_PARENT, component.getClass().getSimpleName(), parentType));
        }
    }

    /**
     * Validate in development stage if the given component has no parent of given parent type.
     * @param <C> The generic component type.
     * @param context The current {@link FacesContext}
     * @param component The component to be validated.
     * @param parentType The parent type to be checked.
     * @throws IllegalStateException When the given component does have a parent of the given type.
     * @since 4.6
     */
    public static <C extends UIComponent> void validateHasNoParent(FacesContext context, UIComponent component, Class<C> parentType) {
        if (!isDevelopment(context)) {
            return;
        }

        if (getClosestParent(component, parentType) != null) {
            throw new IllegalStateException(format(
                    ERROR_ILLEGAL_PARENT, component.getClass().getSimpleName(), parentType));
        }
    }

    /**
     * Validate in development stage if the given component has at least a child of given child type.
     * @param <C> The generic component type.
     * @param component The component to be validated.
     * @param childType The child type to be checked.
     * @throws IllegalStateException When the given component doesn't have any children of the given type.
     * @since 4.6
     */
    public static <C extends UIComponent> void validateHasChild(FacesContext context, UIComponent component, Class<C> childType) {
        if (!isDevelopment(context)) {
            return;
        }

        if (findComponentsInChildren(component, childType).isEmpty()) {
            throw new IllegalStateException(format(
                    ERROR_MISSING_CHILD, component.getClass().getSimpleName(), childType));
        }
    }

    /**
     * Validate in development stage if the given component has only children of given child type.
     * @param <C> The generic component type.
     * @param component The component to be validated.
     * @param childType The child type to be checked.
     * @throws IllegalStateException When the given component has children of a different type.
     * @since 4.6
     */
    public static <C extends UIComponent> void validateHasOnlyChildren(FacesContext context, UIComponent component, Class<C> childType) {
        if (!isDevelopment(context) || component.getChildCount() == 0) {
            return;
        }

        StringBuilder childClassNames = new StringBuilder();

        for (UIComponent child : component.getChildren()) {
            if (!childType.isAssignableFrom(child.getClass())) {
                if (childClassNames.length() > 0) {
                    childClassNames.append(", ");
                }

                childClassNames.append(child.getClass().getName());
            }
        }

        if (childClassNames.length() > 0) {
            throw new IllegalStateException(format(
                    ERROR_ILLEGAL_CHILDREN, component.getClass().getSimpleName(), childType, childClassNames));
        }
    }

    /**
     * Validate in development stage if the given component has no children.
     * @param context The current {@link FacesContext}
     * @param component The component to be validated.
     * @throws IllegalStateException When the given component has any children.
     * @since 4.6
     */
    public static void validateHasNoChildren(FacesContext context, UIComponent component) {
        if (!isDevelopment(context)) {
            return;
        }

        if (component.getChildCount() > 0) {
            StringBuilder childClassNames = new StringBuilder();

            for (UIComponent child : component.getChildren()) {
                if (childClassNames.length() > 0) {
                    childClassNames.append(", ");
                }

                childClassNames.append(child.getClass().getName());
            }

            throw new IllegalStateException(format(
                    ERROR_CHILDREN_DISALLOWED, component.getClass().getSimpleName(), childClassNames));
        }
    }

    // Helpers --------------------------------------------------------------------------------------------------------

    /**
     * Strip UIData/UIRepeat iteration index in pattern <code>:[0-9+]:</code> from given component client ID.
     */
    static String stripIterationIndex(FacesContext context, String clientId) {
        return getIterationIndexPattern(context).matcher(clientId).replaceAll(result -> result.group(1) + result.group(3));
    }

    /**
     * Checks if given component client ID contains UIData/UIRepeat iteration index in pattern <code>:[0-9+]:</code>.
     */
    private static boolean containsIterationIndex(FacesContext context, String clientId) {
        return getIterationIndexPattern(context).matcher(clientId).matches();
    }

    private static Pattern getIterationIndexPattern(FacesContext context) {
        final String separatorChar = Character.toString(UINamingContainer.getSeparatorChar(context));
        return getApplicationAttribute(
                "omnifaces.IterationIndexPattern",
                () -> Pattern.compile("(^|.*" + quote(separatorChar) + ")([0-9]+" + quote(separatorChar) + ")(.*)")
        );
    }

    /**
     * Use {@link UIComponent#findComponent(String)} and ignore the potential {@link IllegalArgumentException} by
     * returning null instead.
     */
    static UIComponent findComponentIgnoringIAE(FacesContext context, UIComponent parent, String clientId) {
        try {
            return parent.findComponent(stripIterationIndex(context, clientId));
        }
        catch (IllegalArgumentException ignore) {
            logger.log(FINEST, "Ignoring thrown exception; this may occur when view has changed by for example a successful navigation.", ignore);
            return null;
        }
    }

    // Inner classes --------------------------------------------------------------------------------------------------

    /**
     * This faces context wrapper allows returning the given temporary view on {@link #getViewRoot()} and its
     * associated renderer in {@link #getRenderKit()}. This can then be used in cases when a different view needs to be
     * built within the current view. Using {@link FacesContext#setViewRoot(UIViewRoot)} isn't desired as it can't be
     * cleared afterwards when the current view is actually <code>null</code>. The {@link #setViewRoot(UIViewRoot)}
     * doesn't accept a <code>null</code> being set.
     *
     * @author Bauke Scholtz
     */
    static class TemporaryViewFacesContext extends FacesContextWrapper {

        private final UIViewRoot temporaryView;

        public TemporaryViewFacesContext(FacesContext wrapped, UIViewRoot temporaryView) {
            super(wrapped);
            this.temporaryView = temporaryView;
        }

        @Override
        public UIViewRoot getViewRoot() {
            return temporaryView;
        }

        @Override
        public RenderKit getRenderKit() {
            return FacesLocal.getRenderKit(this);
        }

    }

}
