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
package org.omnifaces.component.output;

import static jakarta.faces.application.ResourceHandler.FACES_SCRIPT_LIBRARY_NAME;
import static jakarta.faces.application.ResourceHandler.FACES_SCRIPT_RESOURCE_NAME;
import static java.lang.Boolean.FALSE;
import static org.omnifaces.config.OmniFaces.OMNIFACES_EVENT_PARAM_NAME;
import static org.omnifaces.config.OmniFaces.OMNIFACES_LIBRARY_NAME;
import static org.omnifaces.config.OmniFaces.OMNIFACES_SCRIPT_NAME;
import static org.omnifaces.util.Ajax.isExecuted;
import static org.omnifaces.util.Components.getParams;
import static org.omnifaces.util.ComponentsLocal.addFormIfNecessary;
import static org.omnifaces.util.ComponentsLocal.addScript;
import static org.omnifaces.util.FacesLocal.getRequestParameter;
import static org.omnifaces.util.Utils.isEmpty;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.el.MethodExpression;
import jakarta.faces.application.ResourceDependency;
import jakarta.faces.component.FacesComponent;
import jakarta.faces.context.FacesContext;

import org.omnifaces.config.OmniFaces;
import org.omnifaces.event.LazyPanelEvent;
import org.omnifaces.util.Json;
import org.omnifaces.util.State;
import org.omnifaces.vdl.FacesComponentConfig;

/**
 * <p>
 * The <code>&lt;o:lazyPanel&gt;</code> is a component that defers rendering of its children until the panel has scrolled into view. This is useful for
 * expensive regions below the fold, where building the children would otherwise happen on every page load even though the user may never scroll far enough to
 * see them.
 * <p>
 * On initial render, the component writes a wrapper element with a placeholder (optional, see "Placeholder" below) and schedules a viewport intersection
 * listener on it via <code>OmniFaces.js</code>, which uses <code>IntersectionObserver</code> when available and falls back to
 * <code>scroll</code>/<code>resize</code>/<code>orientationChange</code> listeners otherwise. As soon as the wrapper intersects the viewport, a single
 * <code>faces.ajax.request</code> targeting its own client id is fired. During that request, the component detects the trigger, invokes the optional listener
 * with a {@link LazyPanelEvent} argument, flips its <code>loaded</code> flag, and renders its children in place of the placeholder.
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * &lt;o:lazyPanel&gt;
 *     &lt;h:dataTable value="#{bean.expensiveList}" var="row"&gt;
 *         ...
 *     &lt;/h:dataTable&gt;
 * &lt;/o:lazyPanel&gt;
 * </pre>
 * <p>
 * The <code>loaded</code> attribute is a server-side escape hatch: when <code>true</code>, the children are rendered immediately without any client side
 * observer. This is useful for print views, SEO crawlers, or tests.
 *
 * <pre>
 * &lt;o:lazyPanel loaded="#{bean.printPreview}"&gt;
 *     ...
 * &lt;/o:lazyPanel&gt;
 * </pre>
 *
 * <h2>Listener</h2>
 * <p>
 * The <code>listener</code> attribute can be used to invoke a bean method with an optional {@link LazyPanelEvent} argument. It is invoked exactly once, when
 * the component is triggered by the client side intersection.
 *
 * <pre>
 * &lt;o:lazyPanel listener="#{bean.load}"&gt;
 *     ...
 * &lt;/o:lazyPanel&gt;
 * </pre>
 * <p>
 * The listener is invoked from {@link #decode(FacesContext)}, which then jumps straight to the render response phase. The component therefore effectively
 * behaves like a {@link jakarta.faces.component.UICommand} with <code>immediate="true"</code>: the process validations, update model values and invoke
 * application phases are skipped for the lazy panel ajax request, so any concurrently submitted input values in the enclosing form are <em>not</em> processed,
 * validated, or applied. If you need those values on the server, submit them through a separate ajax request before the lazy panel triggers.
 *
 * <h2>Request parameters</h2>
 * <p>
 * Nested <code>&lt;f:param&gt;</code> or <code>&lt;o:param&gt;</code> children are sent along with the lazy panel ajax request and can be retrieved by the
 * listener via {@link org.omnifaces.util.Faces#getRequestParameter(String)}. This is useful to pass context (like an entity id, filter key, or page number) so
 * that a single listener can serve multiple panels.
 *
 * <pre>
 * &lt;o:lazyPanel listener="#{bean.load}"&gt;
 *     &lt;f:param name="productId" value="#{product.id}" /&gt;
 *     ...
 * &lt;/o:lazyPanel&gt;
 * </pre>
 * <p>
 * Parameter values are <em>evaluated at initial render</em> (snapshot semantics), consistent with {@link jakarta.faces.component.UIParameter} usage elsewhere
 * in Faces. For values that must be fresh at trigger time, read them from the surrounding bean state in the listener itself.
 *
 * <h2>Layout</h2>
 * <p>
 * By default the wrapper is rendered as a <code>&lt;div&gt;</code>. Set <code>layout="inline"</code> to render a <code>&lt;span&gt;</code> instead:
 *
 * <pre>
 * &lt;o:lazyPanel layout="inline"&gt;
 *     ...
 * &lt;/o:lazyPanel&gt;
 * </pre>
 *
 * <h2>Placeholder</h2>
 * <p>
 * To show a skeleton or spinner while the lazy region is still off-screen, put it in a facet named <code>placeholder</code>:
 *
 * <pre>
 * &lt;o:lazyPanel&gt;
 *     &lt;f:facet name="placeholder"&gt;
 *         &lt;div class="skeleton"&gt;Loading...&lt;/div&gt;
 *     &lt;/f:facet&gt;
 *     ...
 * &lt;/o:lazyPanel&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 * @since 5.3
 * @see LazyPanelEvent
 * @see LazyPanelHandler
 */
@FacesComponent(value = LazyPanel.COMPONENT_TYPE, namespace = OmniFaces.OMNIFACES_NAMESPACE)
@ResourceDependency(library = FACES_SCRIPT_LIBRARY_NAME, name = FACES_SCRIPT_RESOURCE_NAME, target = "head") // Required for faces.ajax.request.
@ResourceDependency(library = OMNIFACES_LIBRARY_NAME, name = OMNIFACES_SCRIPT_NAME, target = "head") // Specifically LazyPanel.ts.
@FacesComponentConfig(componentHandler = LazyPanelHandler.class)
public class LazyPanel extends OutputFamily {

    // Public constants -----------------------------------------------------------------------------------------------

    /** The component type, which is {@value org.omnifaces.component.output.LazyPanel#COMPONENT_TYPE}. */
    public static final String COMPONENT_TYPE = "org.omnifaces.component.output.LazyPanel";

    /** The omnifaces event value, which is {@value org.omnifaces.component.output.LazyPanel#EVENT_VALUE}. */
    public static final String EVENT_VALUE = "loadLazyPanel";

    /** The name of the facet used to render a placeholder while the children are not yet loaded. */
    public static final String PLACEHOLDER_FACET_NAME = "placeholder";

    // Private constants ----------------------------------------------------------------------------------------------

    private static final String DEFAULT_LAYOUT = "block";
    private static final String INLINE_LAYOUT = "inline";
    private static final String SCRIPT_INIT = "OmniFaces.LazyPanel.init('%s', %s)";

    private enum PropertyKeys {
        // Cannot be uppercased. They have to exactly match the attribute names.
        layout,
        listener,
        loaded,
        styleClass
    }

    // Variables ------------------------------------------------------------------------------------------------------

    private final State state = new State(getStateHelper());

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * If this is a lazy panel request targeting this component, then invoke the listener, if any, flip the loaded flag, and jump straight to the render
     * response phase.
     */
    @Override
    public void decode(FacesContext context) {
        if (isLazyPanelRequest(context) && isExecuted(getClientId(context))) {
            var listener = getListener();

            if (listener != null) {
                var params = listener.getMethodInfo(context.getELContext()).getParamTypes().length == 0
                    ? new Object[0]
                    : new Object[] { new LazyPanelEvent(context, this) };
                listener.invoke(context.getELContext(), params);
            }

            setLoaded(true);
            context.renderResponse();
        }
    }

    /**
     * Always writes the wrapper element with the component client id so that the ajax partial update can replace it, then renders either the placeholder (when
     * not yet loaded) or the actual children.
     */
    @Override
    public void encodeChildren(FacesContext context) throws IOException {
        var writer = context.getResponseWriter();
        var element = INLINE_LAYOUT.equals(getLayout()) ? "span" : "div";
        writer.startElement(element, this);
        writer.writeAttribute("id", getClientId(context), "id");
        writer.writeAttribute("class", getStyleClass(), null);

        if (isLoaded()) {
            super.encodeChildren(context);
        }
        else {
            encodePlaceholder(context);
        }

        writer.endElement(element);
    }

    private void encodePlaceholder(FacesContext context) throws IOException {
        addFormIfNecessary(context); // Required by faces.ajax.request.
        addScript(context, SCRIPT_INIT.formatted(getClientId(context), Json.encode(collectParams())));

        var placeholder = getFacet(PLACEHOLDER_FACET_NAME);

        if (placeholder != null && placeholder.isRendered()) {
            placeholder.encodeAll(context);
        }
    }

    private Map<String, Object> collectParams() {
        Map<String, Object> params = new LinkedHashMap<>();

        for (var param : getParams(this)) {
            var value = param.getValue();

            if (!isEmpty(value)) {
                params.put(param.getName(), value);
            }
        }

        return params;
    }

    // Attribute getters/setters --------------------------------------------------------------------------------------

    /**
     * Returns the layout of the wrapper element. Supported values are <code>"block"</code> (renders a <code>&lt;div&gt;</code>) and <code>"inline"</code>
     * (renders a <code>&lt;span&gt;</code>). Default is <code>"block"</code>.
     *
     * @return The layout of the wrapper element.
     */
    public String getLayout() {
        return state.get(PropertyKeys.layout, DEFAULT_LAYOUT);
    }

    /**
     * Sets the layout of the wrapper element. Supported values are <code>"block"</code> (renders a <code>&lt;div&gt;</code>) and <code>"inline"</code> (renders
     * a <code>&lt;span&gt;</code>).
     *
     * @param layout The layout of the wrapper element.
     */
    public void setLayout(String layout) {
        state.put(PropertyKeys.layout, layout);
    }

    /**
     * Returns the method expression to invoke when the lazy panel is triggered. The method may optionally accept a {@link LazyPanelEvent} argument.
     *
     * @return The listener method expression, or <code>null</code> if none was specified.
     */
    public MethodExpression getListener() {
        return state.get(PropertyKeys.listener);
    }

    /**
     * Sets the method expression to invoke when the lazy panel is triggered.
     *
     * @param listener The listener method expression.
     */
    public void setListener(MethodExpression listener) {
        state.put(PropertyKeys.listener, listener);
    }

    /**
     * Returns whether the children have been loaded. This is <code>true</code> when the component has been triggered by an intersection, or when the
     * <code>loaded</code> attribute has been explicitly set to <code>true</code> by the user (server-side escape hatch). Default is <code>false</code>.
     *
     * @return Whether the children have been loaded.
     */
    public boolean isLoaded() {
        return state.get(PropertyKeys.loaded, FALSE);
    }

    /**
     * Sets whether the children are considered loaded and should be rendered immediately. This can be used as a server-side escape hatch for print views, SEO
     * crawlers, or tests.
     *
     * @param loaded Whether the children have been loaded.
     */
    public void setLoaded(boolean loaded) {
        state.put(PropertyKeys.loaded, loaded);
    }

    /**
     * Returns the CSS style class to render on the wrapper element.
     *
     * @return The CSS style class to render on the wrapper element.
     */
    public String getStyleClass() {
        return state.get(PropertyKeys.styleClass);
    }

    /**
     * Sets the CSS style class to render on the wrapper element.
     *
     * @param styleClass The CSS style class to render on the wrapper element.
     */
    public void setStyleClass(String styleClass) {
        state.put(PropertyKeys.styleClass, styleClass);
    }

    // Helpers --------------------------------------------------------------------------------------------------------

    /**
     * Returns <code>true</code> if the current request is triggered by a lazy panel ajax request.
     *
     * @param context The involved faces context.
     * @return <code>true</code> if the current request is triggered by a lazy panel ajax request.
     */
    public static boolean isLazyPanelRequest(FacesContext context) {
        return context.isPostback() && EVENT_VALUE.equals(getRequestParameter(context, OMNIFACES_EVENT_PARAM_NAME));
    }

}
