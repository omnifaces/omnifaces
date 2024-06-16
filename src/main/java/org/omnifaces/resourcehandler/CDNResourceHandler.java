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
package org.omnifaces.resourcehandler;

import static java.lang.Boolean.parseBoolean;
import static org.omnifaces.util.Faces.evaluateExpressionGet;
import static org.omnifaces.util.Faces.getInitParameter;
import static org.omnifaces.util.Utils.isEmpty;
import static org.omnifaces.util.Utils.splitAndTrim;

import java.util.HashMap;
import java.util.Map;

import jakarta.faces.application.Resource;
import jakarta.faces.application.ResourceDependency;
import jakarta.faces.application.ResourceHandler;

/**
 * <p>
 * This {@link ResourceHandler} implementation allows the developer to provide external (CDN) URLs instead of the
 * default local URLs for Faces resources. This also works on auto-included resources provided as
 * {@link ResourceDependency} by the Faces implementation and/or component libraries. For example, Faces' own
 * <code>jakarta.faces:faces.js</code> resource or PrimeFaces' <code>primefaces:jquery/jquery.js</code> resource could be
 * pointed to a CDN.
 *
 * <h2>Installation</h2>
 * <p>
 * To get it to run, this handler needs be registered as follows in <code>faces-config.xml</code>:
 * <pre>
 * &lt;application&gt;
 *     &lt;resource-handler&gt;org.omnifaces.resourcehandler.CDNResourceHandler&lt;/resource-handler&gt;
 * &lt;/application&gt;
 * </pre>
 *
 * <h2>Standard configuration</h2>
 * <p>
 * To configure the CDN URLs, a {@value org.omnifaces.resourcehandler.CDNResourceHandler#PARAM_NAME_CDN_RESOURCES}
 * context parameter has to be provided wherein the CDN resources are been specified as a comma separated string of
 * <code>libraryName:resourceName=https://cdn.example.com/url</code> key=value pairs. The key represents the default
 * Faces resource identifier and the value represents the full CDN URL, including the scheme. The CDN URL is not validated
 * by this resource handler, so you need to make absolutely sure yourself that it is valid.
 * <p>
 * Here is an example configuration:
 * <pre>
 * &lt;context-param&gt;
 *     &lt;param-name&gt;org.omnifaces.CDN_RESOURCE_HANDLER_URLS&lt;/param-name&gt;
 *     &lt;param-value&gt;
 *         js/script1.js=https://cdn.example.com/js/script1.js,
 *         somelib:js/script2.js=https://cdn.example.com/somelib/js/script2.js,
 *         otherlib:style.css=https://cdn.example.com/otherlib/style.css,
 *         somelib:images/logo.png=https://cdn.example.com/somelib/logo.png
 *     &lt;/param-value&gt;
 * &lt;/context-param&gt;
 * </pre>
 * <p>
 * With the above configuration, the following resources:
 * <pre>
 * &lt;h:outputScript name="js/script1.js" /&gt;
 * &lt;h:outputScript library="somelib" name="js/script2.js" /&gt;
 * &lt;h:outputStylesheet library="otherlib" name="style.css" /&gt;
 * &lt;h:graphicImage library="somelib" name="images/logo.png" /&gt;
 * </pre>
 * <p>
 * Will be rendered as:
 * <pre>
 * &lt;script src="https://cdn.example.com/js/script1.js"&gt;&lt;/script&gt;
 * &lt;script src="https://cdn.example.com/somelib/js/script2.js"&gt;&lt;/script&gt;
 * &lt;link rel="stylesheet" href="https://cdn.example.com/otherlib/style.css" /&gt;
 * &lt;img src="https://cdn.example.com/logo.png" /&gt;
 * </pre>
 * <p>
 * Here is a real world example with Bootstrap:
 * <pre>
 * &lt;context-param&gt;
 *     &lt;param-name&gt;org.omnifaces.CDN_RESOURCE_HANDLER_URLS&lt;/param-name&gt;
 *     &lt;param-value&gt;
 *         cdn:bootstrap.css=https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css,
 *         cdn:bootstrap.js=https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js
 *     &lt;/param-value&gt;
 * &lt;/context-param&gt;
 * </pre>
 * <p>
 * With the above configuration, the following resources:
 * <pre>
 * &lt;h:outputStylesheet library="cdn" name="bootstrap.css" /&gt;
 * &lt;h:outputScript library="cdn" name="bootstrap.js" /&gt;
 * </pre>
 * <p>
 * Will be rendered as:
 * <pre>
 * &lt;link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css" /&gt;
 * &lt;script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"&gt;&lt;/script&gt;
 * </pre>
 *
 * <h2>Wildcard configuration</h2>
 * <p>
 * You can also use the wildcard syntax to map every single resource of a specific library to a common CDN URL. To
 * achieve that, just use <code>*</code> as the sole resource name and make sure that the CDN URL ends with
 * <code>/*</code>. Here's an example:
 * <pre>
 * &lt;context-param&gt;
 *     &lt;param-name&gt;org.omnifaces.CDN_RESOURCE_HANDLER_URLS&lt;/param-name&gt;
 *     &lt;param-value&gt;jquery-cdn:*=https://code.jquery.com/*&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * </pre>
 * With the above configuration, the following resources:
 * <pre>
 * &lt;h:outputScript library="jquery-cdn" name="jquery-1.9.1.js" /&gt;
 * &lt;h:outputScript library="jquery-cdn" name="ui/1.10.3/jquery-ui.js" /&gt;
 * </pre>
 * <p>
 * Will be rendered as:
 * <pre>
 * &lt;script src="https://code.jquery.com/jquery-1.9.1.js"&gt;&lt;/script&gt;
 * &lt;script src="https://code.jquery.com/ui/1.10.3/jquery-ui.js"&gt;&lt;/script&gt;
 * </pre>
 *
 * <h2>EL expressions</h2>
 * <p>The CDN resource handler supports evaluating EL expessions in the CDN URL. Here's an example:</p>
 * <pre>
 * &lt;context-param&gt;
 *     &lt;param-name&gt;org.omnifaces.CDN_RESOURCE_HANDLER_URLS&lt;/param-name&gt;
 *     &lt;param-value&gt;jquery-cdn:*=https://#{settings.jqueryCDN}/*&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * </pre>
 * <p>The EL expression is resolved on a per-request basis.</p>
 *
 * <h2>Conditionally disable CDN resource handler</h2>
 * <p>
 * If you'd like to supply a context parameter which conditionally disables the CDN resource handler, then set the
 * context parameter {@value org.omnifaces.resourcehandler.CDNResourceHandler#PARAM_NAME_CDN_DISABLED} accordingly.
 * <pre>
 * &lt;context-param&gt;
 *     &lt;param-name&gt;org.omnifaces.CDN_RESOURCE_HANDLER_DISABLED&lt;/param-name&gt;
 *     &lt;param-value&gt;true&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * &lt;!-- or --&gt;
 * &lt;context-param&gt;
 *     &lt;param-name&gt;org.omnifaces.CDN_RESOURCE_HANDLER_DISABLED&lt;/param-name&gt;
 *     &lt;param-value&gt;#{facesContext.application.projectStage eq 'Development'}&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * &lt;!-- or --&gt;
 * &lt;context-param&gt;
 *     &lt;param-name&gt;org.omnifaces.CDN_RESOURCE_HANDLER_DISABLED&lt;/param-name&gt;
 *     &lt;param-value&gt;#{someBean.someBooleanProperty}&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * </pre>
 * <p>The EL expression is resolved on a per-request basis.</p>
 *
 * <h2>CombinedResourceHandler</h2>
 * <p>
 * If you're also using the {@link CombinedResourceHandler}, then you need to understand that CDN resources can
 * simply not be combined, as that would defeat the CDN purpose. The {@link CombinedResourceHandler} will therefore
 * automatically exclude all CDN resources from combining.
 *
 * @author Bauke Scholtz
 * @since 1.2
 * @see RemappedResource
 * @see DefaultResourceHandler
 */
public class CDNResourceHandler extends DefaultResourceHandler {

    // Constants ------------------------------------------------------------------------------------------------------

    /** The context parameter name to specify CDN URLs for the given resource identifiers. */
    public static final String PARAM_NAME_CDN_RESOURCES = "org.omnifaces.CDN_RESOURCE_HANDLER_URLS";

    /** The context parameter name to conditionally disable CDN resource handler. @since 2.0 */
    public static final String PARAM_NAME_CDN_DISABLED = "org.omnifaces.CDN_RESOURCE_HANDLER_DISABLED";

    private static final String ERROR_MISSING_INIT_PARAM =
        "Context parameter '" + PARAM_NAME_CDN_RESOURCES + "' is missing in web.xml or web-fragment.xml.";
    private static final String ERROR_INVALID_INIT_PARAM =
        "Context parameter '" + PARAM_NAME_CDN_RESOURCES + "' is in invalid syntax."
            + " It must follow 'resourceId=URL,resourceId=URL,resourceId=URL' syntax.";
    private static final String ERROR_INVALID_WILDCARD =
        "Context parameter '" + PARAM_NAME_CDN_RESOURCES + "' is in invalid syntax."
            + " Wildcard can only represent entire resource name '*' and URL suffix '/*' as in"
            + " 'libraryName:*=https://cdn.example.com/*'.";

    // Properties -----------------------------------------------------------------------------------------------------

    private String disabledParam;
    private Map<ResourceIdentifier, String> cdnResources;

    // Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of this CDN resource handler which wraps the given resource handler. The CDN resources
     * will be initialized based on the {@value org.omnifaces.resourcehandler.CDNResourceHandler#PARAM_NAME_CDN_RESOURCES}
     * context parameter.
     * @param wrapped The resource handler to be wrapped.
     * @throws IllegalArgumentException When the context parameter is missing or is in invalid format.
     */
    public CDNResourceHandler(ResourceHandler wrapped) {
        super(wrapped);
        disabledParam = getInitParameter(PARAM_NAME_CDN_DISABLED);
        cdnResources = initCDNResources();
    }

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * If the given resource is not <code>null</code> and the CDN resource handler is not (conditionally) disabled for
     * the current request, then the CDN resources will be consulted if any CDN URL is available for the given resource.
     * If there is none, then just return the Faces default resource, otherwise return a wrapped resource whose
     * {@link Resource#getRequestPath()} returns the CDN URL as is been set in the
     * {@value org.omnifaces.resourcehandler.CDNResourceHandler#PARAM_NAME_CDN_RESOURCES} context parameter.
     */
    @Override
    public Resource decorateResource(Resource resource, String resourceName, String libraryName) {
        if (disabledParam != null && parseBoolean(String.valueOf(evaluateExpressionGet(disabledParam)))) {
            return resource;
        }

        String requestPath = null;

        if (cdnResources != null) {
            requestPath = cdnResources.get(new ResourceIdentifier(libraryName, resourceName));

            if (requestPath == null) {
                requestPath = cdnResources.get(new ResourceIdentifier(libraryName, "*"));

                if (requestPath != null) {
                    requestPath = requestPath.substring(0, requestPath.length() - 1) + resourceName;
                }
            }
        }

        if (requestPath == null) {
            return resource;
        }

        return new RemappedResource(resourceName, libraryName, evaluateExpressionGet(requestPath));
    }

    // Helpers --------------------------------------------------------------------------------------------------------

    /**
     * Initialize the CDN resources.
     * @return The CDN resources, or <code>null</code> if the context parameter has not been set.
     * @throws IllegalArgumentException When the context parameter value is in invalid format.
     */
    static Map<ResourceIdentifier, String> initCDNResources() {
        var cdnResourcesParam = getInitParameter(PARAM_NAME_CDN_RESOURCES);

        if (isEmpty(cdnResourcesParam)) {
            throw new IllegalArgumentException(ERROR_MISSING_INIT_PARAM);
        }

        var cdnResources = new HashMap<ResourceIdentifier, String>();

        splitAndTrim(cdnResourcesParam, ",").forEach(cdnResource -> {
            var cdnResourceIdAndURL = splitAndTrim(cdnResource, "=", 2);

            if (cdnResourceIdAndURL.length != 2) {
                throw new IllegalArgumentException(ERROR_INVALID_INIT_PARAM);
            }

            var id = new ResourceIdentifier(cdnResourceIdAndURL[0]);

            if (id.getName().contains("*") && (!"*".equals(id.getName()) || !cdnResourceIdAndURL[1].endsWith("/*"))) {
                throw new IllegalArgumentException(ERROR_INVALID_WILDCARD);
            }

            cdnResources.put(id, cdnResourceIdAndURL[1]);
        });

        return cdnResources;
    }

}