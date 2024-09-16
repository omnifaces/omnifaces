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
import static org.omnifaces.util.Components.LABEL_ATTRIBUTE;
import static org.omnifaces.util.Components.VALUE_ATTRIBUTE;
import static org.omnifaces.util.Components.findComponentsInChildren;
import static org.omnifaces.util.Components.getClosestParent;
import static org.omnifaces.util.Events.subscribeToRequestBeforePhase;
import static org.omnifaces.util.Faces.getApplicationAttribute;
import static org.omnifaces.util.Faces.getContext;
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
import static org.omnifaces.util.Renderers.RENDERER_TYPE_JS;
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
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;
import jakarta.faces.component.ActionSource2;
import jakarta.faces.component.EditableValueHolder;
import jakarta.faces.component.NamingContainer;
import jakarta.faces.component.UICommand;
import jakarta.faces.component.UIComponent;
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
import jakarta.faces.component.behavior.BehaviorBase;
import jakarta.faces.component.behavior.ClientBehaviorHolder;
import jakarta.faces.component.html.HtmlBody;
import jakarta.faces.component.search.SearchExpressionHint;
import jakarta.faces.component.visit.VisitCallback;
import jakarta.faces.component.visit.VisitHint;
import jakarta.faces.component.visit.VisitResult;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.FacesContextWrapper;
import jakarta.faces.event.ActionEvent;
import jakarta.faces.event.AjaxBehaviorEvent;
import jakarta.faces.event.BehaviorListener;
import jakarta.faces.event.MethodExpressionActionListener;
import jakarta.faces.render.RenderKit;

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
 * @since 4.6
 * @author Bauke Scholtz
 * @see Components
 */
public final class ComponentsLocal {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final Logger logger = Logger.getLogger(ComponentsLocal.class.getName());

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
     * @see Components#getCurrentComponent()
     */
    @SuppressWarnings("unchecked")
    public static <C extends UIComponent> C getCurrentComponent(FacesContext context) {
        return (C) UIComponent.getCurrentComponent(context);
    }

    // Traversal ------------------------------------------------------------------------------------------------------

    /**
     * @see Components#findComponent(String)
     */
    @SuppressWarnings("unchecked")
    public static <C extends UIComponent> C findComponent(FacesContext context, String clientId) {
        return (C) context.getViewRoot().findComponent(clientId);
    }

    /**
     * @see Components#findComponentRelatively(UIComponent, String)
     */
    @SuppressWarnings("unchecked")
    public static <C extends UIComponent> C findComponentRelatively(FacesContext context, UIComponent component, String clientId) {
        if (isEmpty(clientId)) {
            return null;
        }

        // Search first in the naming container parents of the given component
        var result = findComponentInParents(context, component, clientId);

        if (result == null) {
            // If not in the parents, search from the root
            result = findComponentInChildren(context, context.getViewRoot(), clientId);
        }

        return (C) result;
    }

    /**
     * @see Components#findComponentInParents(UIComponent, String)
     */
    @SuppressWarnings("unchecked")
    public static <C extends UIComponent> C findComponentInParents(FacesContext context, UIComponent component, String clientId) {
        if (isEmpty(clientId)) {
            return null;
        }

        for (var parent = component; parent != null; parent = parent.getParent()) {
            if (parent instanceof NamingContainer || parent instanceof UIViewRoot) {
                var result = findComponentIgnoringIAE(context, parent, clientId);

                if (result != null) {
                    return (C) result;
                }
            }
        }

        return null;
    }

    /**
     * @see Components#findComponentInChildren(UIComponent, String)
     */
    @SuppressWarnings("unchecked")
    public static <C extends UIComponent> C findComponentInChildren(FacesContext context, UIComponent component, String clientId) {
        if (isEmpty(clientId)) {
            return null;
        }

        for (var child : component.getChildren()) {

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
     * @see Components#findComponentsInCurrentForm(Class)
     */
    public static <C extends UIComponent> List<C> findComponentsInCurrentForm(FacesContext context, Class<C> type) {
        var currentForm = getCurrentForm(context);
        return currentForm != null ? findComponentsInChildren(currentForm, type) : emptyList();
    }

    // Iteration / Visiting -------------------------------------------------------------------------------------------

    /**
     * @see Components#forEachComponent()
     */
    public static Components.ForEach forEachComponent(FacesContext context) {
        return new Components.ForEach(context);
    }

    // Creation -------------------------------------------------------------------------------------------------------

    /**
     * @see Components#createComponent(String)
     */
    @SuppressWarnings("unchecked")
    public static <C extends UIComponent> C createComponent(FacesContext context, String componentType) {
        return (C) context.getApplication().createComponent(componentType);
    }

    // Manipulation ---------------------------------------------------------------------------------------------------

    /**
     * @see Components#includeFacelet(UIComponent, String)
     */
    public static void includeFacelet(FacesContext context, UIComponent parent, String path) throws IOException {
        getFaceletContext(context).includeFacelet(parent, path);
    }

    /**
     * @see Components#includeCompositeComponent(UIComponent, String, String, String)
     */
    public static UIComponent includeCompositeComponent(FacesContext context, UIComponent parent, String libraryName, String tagName, String id) {
        return includeCompositeComponent(context, parent, libraryName, tagName, id, null);
    }

    /**
     * @see Components#includeCompositeComponent(UIComponent, String, String, String, Map)
     */
    public static UIComponent includeCompositeComponent(FacesContext context, UIComponent parent, String libraryName, String tagName, String id, Map<String, String> attributes) {
        var taglibURI = "http://xmlns.jcp.org/jsf/composite/" + libraryName;
        var attrs = attributes == null ? null : new HashMap<String, Object>(attributes);

        var composite = context.getApplication().getViewHandler()
                .getViewDeclarationLanguage(context, context.getViewRoot().getViewId())
                .createComponent(context, taglibURI, tagName, attrs);

        composite.setId(id);
        parent.getChildren().add(composite);
        return composite;
    }

    /**
     * @see Components#addScript(String)
     */
    public static void addScript(FacesContext context, String script) {
        if (isAjaxRequestWithPartialRendering(context)) {
            oncomplete(context, script);
        }
        else if (context.getCurrentPhaseId() != RENDER_RESPONSE) {
            subscribeToRequestBeforePhase(RENDER_RESPONSE, () -> addScriptToBody(getContext(), script)); // Just to avoid it misses when view rebuilds in the meanwhile.
        }
        else {
            addScriptToBody(context, script);
        }
    }

    /**
     * @see Components#addScriptResource(String, String)
     */
    public static void addScriptResource(FacesContext context, String libraryName, String resourceName) {
        if (!context.getApplication().getResourceHandler().isResourceRendered(context, resourceName, libraryName)) {
            if (isAjaxRequestWithPartialRendering(context)) {
                load(context, libraryName, resourceName);
            }
            else if (context.getCurrentPhaseId() != RENDER_RESPONSE) {
                addScriptResourceToHead(context, libraryName, resourceName);
                subscribeToRequestBeforePhase(RENDER_RESPONSE, () -> addScriptResourceToBody(getContext(), libraryName, resourceName)); // Fallback in case view rebuilds in the meanwhile. It will re-check if already added.
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
     * @see Components#addFacesScriptResource()
     */
    public static void addFacesScriptResource(FacesContext context) {
        addScriptResource(context, JSF_SCRIPT_LIBRARY_NAME, isFacesScriptResourceAvailable() ? FACES_SCRIPT_RESOURCE_NAME : JSF_SCRIPT_RESOURCE_NAME);
    }

    private static UIComponent addScriptResourceToTarget(FacesContext context, String libraryName, String resourceName, String target) {
        var id = (libraryName != null ? libraryName.replaceAll("\\W+", "_") + "_" : "") + resourceName.replaceAll("\\W+", "_");

        for (var existingResource : context.getViewRoot().getComponentResources(context)) {
            if (id.equals(existingResource.getId())) {
                return existingResource;
            }
        }

        var outputScript = createScriptResource();
        outputScript.setId(id);

        if (libraryName != null) {
            outputScript.getAttributes().put("library", libraryName);
        }

        outputScript.getAttributes().put("name", resourceName);
        return addComponentResource(context, outputScript, target);
    }

    private static UIOutput createScriptResource() {
        var outputScript = new UIOutput();
        outputScript.setRendererType(RENDERER_TYPE_JS);
        return outputScript;
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
        var outputScript = createScriptResource();
        var content = new UIOutput();
        content.setValue(script);
        outputScript.getChildren().add(content);
        addComponentResource(context, outputScript, "body");
    }

    // Building / rendering -------------------------------------------------------------------------------------------

    /**
     * @see Components#buildView(String)
     */
    public static UIViewRoot buildView(FacesContext context, String viewId) throws IOException {
        var normalizedViewId = normalizeViewId(context, viewId);
        var viewHandler = context.getApplication().getViewHandler();
        var view = viewHandler.createView(context, normalizedViewId);
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
     * @see Components#encodeHtml(UIComponent)
     */
    public static String encodeHtml(FacesContext context, UIComponent component) {
        var originalWriter = context.getResponseWriter();
        var output = new StringWriter(2048); // is 2kb enough for simple templates?
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
     * @see Components#getCurrentForm()
     */
    public static UIForm getCurrentForm(FacesContext context) {

        if (!context.isPostback()) {
            return null;
        }

        var viewRoot = context.getViewRoot();

        if (viewRoot == null) {
            return null;
        }

        // The initial implementation has visited the tree for UIForm components which returns true on isSubmitted().
        // But with testing it turns out to return false on ajax requests where the form is not included in execute!
        // The current implementation just walks through the request parameter map instead.

        for (var name : context.getExternalContext().getRequestParameterMap().keySet()) {
            if (name.startsWith("jakarta.faces.")) {
                continue; // Quick skip.
            }

            var component = findComponentIgnoringIAE(context, viewRoot, name);

            if (component instanceof UIForm) {
                return (UIForm) component;
            }
            else if (component != null) {
                var form = getClosestParent(component, UIForm.class);

                if (form != null) {
                    return form;
                }
            }
        }

        return null;
    }

    /**
     * @see Components#getCurrentCommand()
     */
    public static UICommand getCurrentCommand(FacesContext context) {
        var source = getCurrentActionSource(context);
        return source instanceof UICommand ? (UICommand) source : null;
    }

    /**
     * @see Components#getCurrentActionSource()
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
    private static UIComponent getCurrentActionSource(FacesContext context, UIComponent parent) {
        if (parent == null) {
            return null;
        }

        var params = context.getExternalContext().getRequestParameterMap();

        if (context.getPartialViewContext().isAjaxRequest()) {
            var sourceClientId = params.get(BEHAVIOR_SOURCE_PARAM_NAME);

            if (sourceClientId != null) {
                var actionSource = findComponentIgnoringIAE(context, parent, sourceClientId);

                if (actionSource != null) {
                    return actionSource;
                }
            }
        }

        for (var name : params.keySet()) {
            if (name.startsWith("jakarta.faces.")) {
                continue; // Quick skip.
            }

            var actionSource = findComponentIgnoringIAE(context, parent, name);

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
     * @see Components#getLabel(UIComponent)
     */
    public static String getLabel(FacesContext context, UIComponent component) {
        var label = getOptionalLabel(context, component);
        return label != null ? label : component.getClientId();
    }

    /**
     * @see Components#getOptionalLabel(UIComponent)
     */
    public static String getOptionalLabel(FacesContext context, UIComponent component) {
        var result = new Object[1];

        new ScopedRunner(context).with("cc", getCompositeComponentParent(component)).invoke(() -> {
            var label = component.getAttributes().get(LABEL_ATTRIBUTE);

            if (isEmpty(label)) {
                var labelExpression = component.getValueExpression(LABEL_ATTRIBUTE);

                if (labelExpression != null) {
                    label = labelExpression.getValue(context.getELContext());
                }
            }

            result[0] = label;
        });

        return result[0] != null ? result[0].toString() : null;
    }

    /**
     * @see Components#getImmediateValue(UIInput)
     */
    @SuppressWarnings("unchecked")
    public static <T> T getImmediateValue(FacesContext context, UIInput input) {
        if (input.isValid() && input.getSubmittedValue() != null) {
            input.validate(context);
        }

        return input.isLocalValueSet() ? (T) input.getValue() : null;
    }

    /**
     * @see Components#getExpectedValueType(UIComponent)
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> getExpectedValueType(FacesContext context, UIComponent component) {
        var valueExpression = component.getValueExpression(VALUE_ATTRIBUTE);

        if (valueExpression != null) {
            return getExpectedType(context, valueExpression);
        }
        else {
            var value = component.getAttributes().get(VALUE_ATTRIBUTE);

            if (value != null) {
                return (Class<T>) value.getClass();
            }

            return null;
        }
    }

    /**
     * @see Components#getExpectedType(ValueExpression)
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> getExpectedType(FacesContext context, ValueExpression valueExpression) {
        var expectedType = valueExpression.getExpectedType();

        if (expectedType == Object.class) {
            expectedType = valueExpression.getType(context.getELContext());
        }

        return (Class<T>) expectedType;
    }

    /**
     * @see Components#hasInvokedSubmit(UIComponent)
     */
    public static boolean hasInvokedSubmit(FacesContext context, UIComponent component) {
        var source = getCurrentActionSource(context);
        return source != null && source.equals(component);
    }

    /**
     * @see Components#getParams(UIComponent, boolean, boolean)
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

        for (var param : Components.getParams(component)) {
            var value = param.getValue();

            if (isEmpty(value)) {
                continue;
            }

            params.put(param.getName(), singletonList(value));
        }

        return Collections.unmodifiableMap(params);
    }

    /**
     * @see Components#getMessageComponent(UIInput)
     */
    public static UIMessage getMessageComponent(FacesContext context, UIInput input) {
        var found = new UIMessage[1];

        forEachComponent(context).ofTypes(UIMessage.class).withHints(SKIP_ITERATION).invoke((visitContext, target) -> {
            var messageComponent = (UIMessage) target;
            var forValue = messageComponent.getFor();

            if (!isEmpty(forValue)) {
                var facesContext = visitContext.getFacesContext();
                var searchExpressionContext = createSearchExpressionContext(facesContext, messageComponent, RESOLVE_LABEL_FOR, null);
                var forClientId = facesContext.getApplication().getSearchExpressionHandler().resolveClientId(searchExpressionContext, forValue);
                var forComponent = findComponentRelatively(facesContext, messageComponent, forClientId);

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
     * @see Components#getMessagesComponent()
     */
    public static UIMessages getMessagesComponent(FacesContext context) {
        var found = new UIMessages[1];

        forEachComponent(context).ofTypes(UIMessages.class).withHints(SKIP_ITERATION).invoke((visitContext, target) -> {
            found[0] = (UIMessages) target;
            return VisitResult.COMPLETE;
        });

        return found[0];
    }

    /**
     * @see Components#resetForm(UIComponent)
     */
    public static void resetForm(FacesContext context, UIComponent component) {
        var form = component instanceof UIForm ? (UIForm) component : getClosestParent(component, UIForm.class);

        if (form == null) {
            throw new IllegalArgumentException(format(ERROR_MISSING_PARENT, component.getClass(), UIForm.class));
        }

        resetInputs(context, form);
    }

    /**
     * @see Components#resetInputs(UIComponent)
     */
    public static void resetInputs(FacesContext context, UIComponent component) {
        forEachComponent(context).fromRoot(component).ofTypes(UIInput.class).invoke(UIInput::resetValue);
    }

    /**
     * @see Components#disableInput(String)
     */
    public static void disableInput(FacesContext context, String clientId) {
        Components.disableInput(findComponent(context, clientId));
    }

    /**
     * @see Components#addFormIfNecessary()
     */
    public static void addFormIfNecessary(FacesContext context) {
        if (isAjaxRequestWithPartialRendering(context)) {
            return; // It's impossible to have this condition without an UIForm in first place.
        }

        var viewRoot = context.getViewRoot();

        if (viewRoot == null || viewRoot.getChildCount() == 0) {
            return; // Empty view. Nothing to do against. The client should probably find a better moment to invoke this.
        }

        VisitCallback visitCallback = (visitContext, target) -> target instanceof UIForm ? VisitResult.COMPLETE : VisitResult.ACCEPT;
        var formFound = viewRoot.visitTree(createVisitContext(context, null, EnumSet.of(VisitHint.SKIP_ITERATION)), visitCallback);

        if (formFound) {
            return; // UIForm present. No need to add a new one.
        }

        var body = viewRoot.getChildren().stream().filter(HtmlBody.class::isInstance).findFirst();

        if (body.isEmpty()) {
            return; // No <h:body> present. Not possible to add a new UIForm then.
        }

        var form = new Form();
        form.setId(OmniFaces.OMNIFACES_DYNAMIC_FORM_ID);
        form.getAttributes().put("style", "display:none"); // Just to be on the safe side. There might be CSS which puts visible style such as margin/padding/border on any <form> for some reason.
        body.get().getChildren().add(form);
    }

    /**
     * @see Components#convertToString(FacesContext, ValueHolder, Object)
     */
    @SuppressWarnings("unchecked")
    public static <T> String convertToString(FacesContext context, ValueHolder holder, T value) {
        var converter = holder.getConverter();

        if (converter == null && value != null) {
            converter = createConverter(context, value.getClass());
        }

        if (converter != null) {
            final UIComponent component;

            if (holder instanceof UIComponent) {
                component = (UIComponent) holder;
            }
            else if (holder instanceof ParamHolder) {
                var parameter = new UIParameter();
                parameter.setName(((ParamHolder<T>) holder).getName());
                parameter.setValue(value);
                component = parameter;
            }
            else {
                var output = new UIOutput();
                output.setValue(value);
                component = output;
            }

            return converter.getAsString(context, component, value);
        }

        return value == null ? null : value.toString();
    }

    /**
     * @see Components#getRenderedValue(FacesContext, ValueHolder)
     */
    public static String getRenderedValue(FacesContext context, ValueHolder holder) {
        Object value = null;

        if (holder instanceof EditableValueHolder) {
            var editableValueHolder = (EditableValueHolder) holder;
            var submittedValue = editableValueHolder.getSubmittedValue();

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
     * @see Components#invalidateInputs(String...)
     */
    public static void invalidateInputs(FacesContext context, String... relativeClientIds) {
        findAndInvalidateInputs(context, relativeClientIds);
    }

    /**
     * @see Components#invalidateInput(String, String, Object...)
     */
    public static void invalidateInput(FacesContext context, String relativeClientId, String message, Object... params) {
        addError(findAndInvalidateInputs(context,relativeClientId).iterator().next(), message, params);
    }

    private static Set<String> findAndInvalidateInputs(FacesContext context, String... relativeClientIds) {
        var root = coalesce(getCurrentForm(context), context.getViewRoot());
        var needsVisit = stream(relativeClientIds).anyMatch(clientId -> containsIterationIndex(context, clientId));
        var fullClientIds = new LinkedHashSet<String>();

        for (var relativeClientId : relativeClientIds) {
            var component = findComponentRelatively(context, root, relativeClientId);

            if (!(component instanceof UIInput)) {
                var type = component == null ? "null" : component.getClass().getName();
                throw new IllegalArgumentException(format(ERROR_ILLEGAL_UIINPUT, relativeClientId, type));
            }

            var input = (UIInput) component;
            var fullClientId = input.getClientId();

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
     * @see Components#createValueExpression(String, Class)
     */
    public static ValueExpression createValueExpression(FacesContext context, String expression, Class<?> type) {
        return context
                .getApplication()
                .getExpressionFactory()
                .createValueExpression(context.getELContext(), expression, type);
    }

    /**
     * @see Components#createMethodExpression(String, Class, Class[])
     */
    public static MethodExpression createMethodExpression(FacesContext context, String expression, Class<?> returnType, Class<?>... parameterTypes) {
        return context
                .getApplication()
                .getExpressionFactory()
                .createMethodExpression(context.getELContext(), expression, returnType, parameterTypes);
    }

    /**
     * @see Components#createVoidMethodExpression(String, Class[])
     */
    public static MethodExpression createVoidMethodExpression(FacesContext context, String expression, Class<?>... parameterTypes) {
        return createMethodExpression(context, expression, Void.class, parameterTypes);
    }

    /**
     * @see Components#createActionListenerMethodExpression(String)
     */
    public static MethodExpressionActionListener createActionListenerMethodExpression(FacesContext context, String expression) {
        return new MethodExpressionActionListener(createVoidMethodExpression(context,expression, ActionEvent.class));
    }

    /**
     * @see Components#createAjaxBehavior(String)
     */
    public static AjaxBehavior createAjaxBehavior(FacesContext context, String expression) {
        var behavior = (AjaxBehavior) context.getApplication().createBehavior(AjaxBehavior.BEHAVIOR_ID);
        var method = createVoidMethodExpression(context, expression, AjaxBehaviorEvent.class);
        behavior.addAjaxBehaviorListener(event -> method.invoke(context.getELContext(), new Object[] { event }));
        return behavior;
    }

    /**
     * @see Components#getActionExpressionsAndListeners(UIComponent)
     */
    public static List<String> getActionExpressionsAndListeners(FacesContext context, UIComponent component) {
        var actions = new ArrayList<String>();

        if (component instanceof ActionSource2) {
            var source = (ActionSource2) component;
            addExpressionStringIfNotNull(source.getActionExpression(), actions);

            for (var listener : source.getActionListeners()) {
                addExpressionStringIfNotNull(accessField(listener, MethodExpression.class), actions);
            }
        }

        if (component instanceof ClientBehaviorHolder) {
            var behaviorEvent = getRequestParameter(context, BEHAVIOR_EVENT_PARAM_NAME);

            if (behaviorEvent != null) {
                for (var listener : getBehaviorListeners((ClientBehaviorHolder) component, behaviorEvent)) {
                    addExpressionStringIfNotNull(accessField(listener, MethodExpression.class), actions);
                }
            }
        }

        return unmodifiableList(actions);
    }

    // Hierarchy validation -------------------------------------------------------------------------------------------

    /**
     * @see Components#validateHasParent(UIComponent, Class)
     */
    public static <C extends UIComponent> void validateHasParent(FacesContext context, UIComponent component, Class<C> parentType) {
        if (!isDevelopment(context)) {
            return;
        }

        if (getClosestParent(component, parentType) == null) {
            throw new IllegalStateException(format(ERROR_MISSING_PARENT, component.getClass().getSimpleName(), parentType));
        }
    }

    /**
     * @see Components#validateHasDirectParent(UIComponent, Class)
     */
    public static <C extends UIComponent> void validateHasDirectParent(FacesContext context, UIComponent component, Class<C> parentType) {
        if (!isDevelopment(context)) {
            return;
        }

        if (!parentType.isInstance(component.getParent())) {
            throw new IllegalStateException(format(ERROR_MISSING_DIRECT_PARENT, component.getClass().getSimpleName(), parentType));
        }
    }

    /**
     * @see Components#validateHasNoParent(UIComponent, Class)
     */
    public static <C extends UIComponent> void validateHasNoParent(FacesContext context, UIComponent component, Class<C> parentType) {
        if (!isDevelopment(context)) {
            return;
        }

        if (getClosestParent(component, parentType) != null) {
            throw new IllegalStateException(format(ERROR_ILLEGAL_PARENT, component.getClass().getSimpleName(), parentType));
        }
    }

    /**
     * @see Components#validateHasChild(UIComponent, Class)
     */
    public static <C extends UIComponent> void validateHasChild(FacesContext context, UIComponent component, Class<C> childType) {
        if (!isDevelopment(context)) {
            return;
        }

        if (findComponentsInChildren(component, childType).isEmpty()) {
            throw new IllegalStateException(format(ERROR_MISSING_CHILD, component.getClass().getSimpleName(), childType));
        }
    }

    /**
     * @see Components#validateHasOnlyChildren(UIComponent, Class)
     */
    public static <C extends UIComponent> void validateHasOnlyChildren(FacesContext context, UIComponent component, Class<C> childType) {
        if (!isDevelopment(context) || component.getChildCount() == 0) {
            return;
        }

        var childClassNames = new StringBuilder();

        for (var child : component.getChildren()) {
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
     * @see Components#validateHasNoChildren(UIComponent)
     */
    public static void validateHasNoChildren(FacesContext context, UIComponent component) {
        if (!isDevelopment(context)) {
            return;
        }

        if (component.getChildCount() > 0) {
            var childClassNames = new StringBuilder();

            for (var child : component.getChildren()) {
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
    private static String stripIterationIndex(FacesContext context, String clientId) {
        return getIterationIndexPattern(context).matcher(clientId).replaceAll(result -> result.group(1) + result.group(3));
    }

    /**
     * Checks if given component client ID contains UIData/UIRepeat iteration index in pattern <code>:[0-9+]:</code>.
     */
    private static boolean containsIterationIndex(FacesContext context, String clientId) {
        return getIterationIndexPattern(context).matcher(clientId).matches();
    }

    private static Pattern getIterationIndexPattern(FacesContext context) {
        return getApplicationAttribute("omnifaces.IterationIndexPattern", () -> {
            var quotedSeparatorChar = quote(Character.toString(UINamingContainer.getSeparatorChar(context)));
            return Pattern.compile("(^|.*" + quotedSeparatorChar + ")([0-9]+" + quotedSeparatorChar + ")(.*)");
        });
    }

    /**
     * Use {@link UIComponent#findComponent(String)} and ignore the potential {@link IllegalArgumentException} by
     * returning null instead.
     */
    private static UIComponent findComponentIgnoringIAE(FacesContext context, UIComponent parent, String clientId) {
        try {
            return parent.findComponent(stripIterationIndex(context, clientId));
        }
        catch (IllegalArgumentException ignore) {
            logger.log(FINEST, "Ignoring thrown exception; this may occur when view has changed by for example a successful navigation.", ignore);
            return null;
        }
    }

    /**
     * If given method expression is not null, extract expression string from it and add to given list.
     */
    private static void addExpressionStringIfNotNull(MethodExpression expression, List<String> list) {
        if (expression != null) {
            list.add(expression.getExpressionString());
        }
    }

    /**
     * Get all behavior listeners of given behavior event from the given component.
     */
    @SuppressWarnings("unchecked")
    private static List<BehaviorListener> getBehaviorListeners(ClientBehaviorHolder component, String behaviorEvent) {
        var behaviors = component.getClientBehaviors().get(behaviorEvent);

        if (behaviors == null) {
            return emptyList();
        }

        var allListeners = new ArrayList<BehaviorListener>();

        for (var behavior : behaviors) {
            var listeners = accessField(behavior, BehaviorBase.class, List.class);

            if (listeners != null) {
                allListeners.addAll(listeners);
            }
        }

        return allListeners;
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
