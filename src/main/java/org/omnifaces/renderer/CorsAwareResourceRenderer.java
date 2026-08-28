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
package org.omnifaces.renderer;

import static org.omnifaces.util.FacesLocal.createResource;
import static org.omnifaces.util.FacesLocal.getApplicationAttribute;
import static org.omnifaces.util.FacesLocal.getInitParameter;
import static org.omnifaces.util.FacesLocal.getRequestDomainURL;
import static org.omnifaces.util.Utils.coalesce;
import static org.omnifaces.util.Utils.isOneOf;
import static org.omnifaces.util.Utils.startsWithOneOf;

import java.io.IOException;

import jakarta.faces.application.Resource;
import jakarta.faces.application.ResourceDependency;
import jakarta.faces.application.ResourceHandler;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.AbortProcessingException;
import jakarta.faces.event.ComponentSystemEvent;
import jakarta.faces.event.ComponentSystemEventListener;
import jakarta.faces.event.ListenerFor;
import jakarta.faces.event.PostAddToViewEvent;
import jakarta.faces.render.Renderer;
import jakarta.faces.render.RendererWrapper;

import org.omnifaces.component.script.DeferredScript;
import org.omnifaces.component.stylesheet.CriticalStylesheet;
import org.omnifaces.renderkit.OmniRenderKit;
import org.omnifaces.renderkit.OmniRenderKitFactory;
import org.omnifaces.resourcehandler.CDNResource;
import org.omnifaces.resourcehandler.CDNResourceHandler;
import org.omnifaces.resourcehandler.CombinedResourceHandler;
import org.omnifaces.resourcehandler.ResourceIdentifier;

/**
 * <p>
 * The {@link CorsAwareResourceRenderer} is intended as an extension to the standard script and stylesheet resource renderer in order to add the
 * <code>crossorigin</code> and <code>integrity</code> attributes as a pass-through attribute. Each attribute has its own condition.
 * <p>
 * The <code>crossorigin</code> attribute is only set when the {@link Resource#getRequestPath()} points to another origin than the one of the current request.
 * It will then by default be set to <code>anonymous</code>. Resources which are served from the same origin are left alone, as a <code>crossorigin</code>
 * attribute has no security benefit on those, while it does force the browser into CORS mode, which in turn breaks script loaders which rely on a synchronous
 * <code>XMLHttpRequest</code> to fetch and evaluate a dynamically added script.
 * <p>
 * The <code>integrity</code> attribute is only set when the {@link ResourceHandler#createResource(String)} returns an instance of {@link CDNResource}. It will
 * then be set with a base64 encoded sha384 hash of the local content. A {@link CDNResource} is returned by a {@link ResourceHandler} which you write yourself
 * and which uploads your own resources to your own CDN host, so that the CDN content stays byte for byte identical to the local content. Note that the
 * {@link CDNResourceHandler} does not return one, hence the resources remapped by it do get a <code>crossorigin</code> attribute but no <code>integrity</code>
 * attribute.
 * <p>
 * This includes declarative resources created by <code>&lt;h:outputScript&gt;</code> and <code>&lt;h:outputStylesheet&gt;</code>, annotated resources created
 * by {@link ResourceDependency}, combined resources created by {@link CombinedResourceHandler}, deferred scripts created by {@link DeferredScript} and critical
 * stylesheets created by {@link CriticalStylesheet}. Basically any resource which will be served by {@link ResourceHandler#createResource(String)}.
 *
 * <h2>Installation</h2>
 * <p>
 * You do not need to explicitly register this renderer in your <code>faces-config.xml</code>. It's already automatically registered.
 *
 * <h2>Configuration</h2>
 * <p>
 * Currently only the following context parameter is available: <code>{@value org.omnifaces.renderer.CorsAwareResourceRenderer#PARAM_NAME_CROSSORIGIN}</code>.
 * This sets the desired value of <code>crossorigin</code> attribute of cross origin resources. Supported values are specified in
 * <a href="https://developer.mozilla.org/en-US/docs/Web/HTML/Attributes/crossorigin">MDN</a>. An empty string is also allowed, it will then completely skip the
 * task of the current renderer, including the <code>integrity</code> attribute. The default value when the context parameter is not set is
 * <code>anonymous</code> (i.e. no cookies are transferred at all).
 *
 * <h2>Usage</h2>
 * <p>
 * Eveything is automatic. In case you wish to override the default/configured outcome of one of the attributes on a specific resource component, then simply
 * explicitly set it as a passthrough attribute yourself. For example,
 *
 * <pre>
 * &lt;... xmlns:h="jakarta.faces.html" xmlns:a="jakarta.faces.passthrough"&gt;
 * &lt;h:outputScript name="..." a:crossorigin="use-credentials" /&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 * @since 5.0
 * @see OmniRenderKit
 * @see OmniRenderKitFactory
 * @see <a href=
 * "https://developer.mozilla.org/en-US/docs/Web/HTML/Attributes/crossorigin">https://developer.mozilla.org/en-US/docs/Web/HTML/Attributes/crossorigin</a>
 * @see <a href=
 * "https://developer.mozilla.org/en-US/docs/Web/Security/Subresource_Integrity">https://developer.mozilla.org/en-US/docs/Web/Security/Subresource_Integrity</a>
 */
@ListenerFor(systemEventClass = PostAddToViewEvent.class)
public class CorsAwareResourceRenderer extends RendererWrapper implements ComponentSystemEventListener {

    /** The default value of the 'crossorigin' attribute. */
    public static final String DEFAULT_CROSSORIGIN = "anonymous";

    /**
     * The context parameter name to specify the value of the 'crossorigin' attribute for cross origin resources. The value defaults to
     * {@value CorsAwareResourceRenderer#DEFAULT_CROSSORIGIN}.
     */
    public static final String PARAM_NAME_CROSSORIGIN = "org.omnifaces.DEFAULT_CROSSORIGIN";

    private static final String PROTOCOL_RELATIVE_PREFIX = "//";
    private static final String ATTRIBUTE_NAME_CROSSORIGIN = CorsAwareResourceRenderer.class.getName() + ".CROSSORIGIN";

    /**
     * Do not use this constructor. It's merely there for {@link ComponentSystemEventListener}.
     */
    @SuppressWarnings("deprecation")
    public CorsAwareResourceRenderer() {
        // Keep default c'tor alive for ComponentSystemEventListener.
    }

    /**
     * Creates a new instance of this CORS resource renderer which wraps the given resource renderer.
     *
     * @param wrapped The resource renderer to be wrapped.
     */
    public CorsAwareResourceRenderer(Renderer<?> wrapped) {
        super(wrapped);
    }

    /**
     * If the wrapped script/stylesheet resource renderer is an instance of {@link ComponentSystemEventListener} then delegate the component system event to it.
     * Generally these will further relocate the component resource depending on their <code>target</code> attribute.
     */
    @Override
    public void processEvent(ComponentSystemEvent event) throws AbortProcessingException {
        if (getWrapped() instanceof ComponentSystemEventListener listener) {
            listener.processEvent(event);
        }
    }

    /**
     * When the associated resource has a non-{@code null} name and no explicitly set passthrough attributes for <code>crossorigin</code> nor
     * <code>integrity</code>, then set these as new passthrough attributes so that the default script/stylesheet resource renderer will write them.
     */
    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        var passThroughAttributes = component.getPassThroughAttributes();

        if (
            !getCrossorigin(context).isEmpty()
                && component.getAttributes().get("name") != null
                && passThroughAttributes.get("integrity") == null
                && isOneOf(passThroughAttributes.get("crossorigin"), null, DEFAULT_CROSSORIGIN)
        ) {
            var resource = createResource(context, component);
            var resourceCrossorigin = getCrossoriginIfNecessary(context, resource);

            if (!resourceCrossorigin.isEmpty()) {
                passThroughAttributes.put("crossorigin", resourceCrossorigin);
            }

            var integrity = getIntegrityIfNecessary(context, resource);

            if (!integrity.isEmpty()) {
                passThroughAttributes.put("integrity", integrity);
            }
        }

        super.encodeBegin(context, component);
    }

    /**
     * Returns the configured crossorigin. Defaults to {@value CorsAwareResourceRenderer#DEFAULT_CROSSORIGIN}.
     *
     * @param context The involved faces context.
     * @return The configured crossorigin.
     */
    public static String getCrossorigin(FacesContext context) {
        return getApplicationAttribute(
            context, ATTRIBUTE_NAME_CROSSORIGIN, () -> coalesce(getInitParameter(context, PARAM_NAME_CROSSORIGIN), DEFAULT_CROSSORIGIN)
        );
    }

    /**
     * Returns whether the integrity is needed. Defaults to {@code true}.
     *
     * @param context The involved faces context.
     * @return Whether the integrity is needed.
     */
    public static boolean isNeedsIntegrity(FacesContext context) {
        return DEFAULT_CROSSORIGIN.equals(getCrossorigin(context));
    }

    /**
     * Returns the crossorigin of the given resource if necessary. It will only return the {@link #getCrossorigin(FacesContext)} when the request path of the
     * given resource points to another origin than the one of the current request, because the <code>crossorigin</code> attribute is only relevant for those.
     * The outcome cannot be memoized per resource identifier, as a CDN URL can be an EL expression which is resolved on a per request basis.
     *
     * @param context The involved faces context.
     * @param resource The resource to get crossorigin for.
     * @return The crossorigin of the given resource if necessary.
     * @since 5.5.1
     */
    public static String getCrossoriginIfNecessary(FacesContext context, Resource resource) {
        return isCrossOrigin(context, resource) ? getCrossorigin(context) : "";
    }

    /**
     * Returns the integrity of the given resource if necessary. It will only return a base64 encoded sha384 hash when the given resource is an instance of
     * {@link CDNResource} and the {@link #getCrossorigin(FacesContext)} equals to {@value CorsAwareResourceRenderer#DEFAULT_CROSSORIGIN}.
     *
     * @param context The involved faces context.
     * @param resource The resource to get integrity for.
     * @return The integrity of the given resource if necessary.
     */
    public static String getIntegrityIfNecessary(FacesContext context, Resource resource) {
        return resource instanceof CDNResource && isNeedsIntegrity(context) ? new ResourceIdentifier(resource).getIntegrity(context) : "";
    }

    private static boolean isCrossOrigin(FacesContext context, Resource resource) {
        var requestPath = resource != null ? resource.getRequestPath() : null;

        if (requestPath == null) {
            return false;
        }
        else if (requestPath.startsWith(PROTOCOL_RELATIVE_PREFIX)) {
            var origin = getRequestDomainURL(context);
            return !requestPath.startsWith(origin.substring(origin.indexOf(PROTOCOL_RELATIVE_PREFIX)) + "/");
        }
        else if (startsWithOneOf(requestPath, "http://", "https://")) {
            return !requestPath.startsWith(getRequestDomainURL(context) + "/");
        }
        else {
            return false;
        }
    }

}
