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
package org.omnifaces.security;

import java.io.IOException;

import jakarta.faces.component.UIComponent;
import jakarta.faces.view.facelets.FaceletContext;
import jakarta.faces.view.facelets.TagConfig;
import jakarta.security.enterprise.SecurityContext;

import org.omnifaces.config.OmniFaces;
import org.omnifaces.vdl.FacesTagHandler;

/**
 * <p>
 * The <code>&lt;sec:isAnonymous&gt;</code> tag conditionally renders its content only when the user is anonymous (not authenticated). This is useful for
 * displaying login forms, welcome messages for guests, or other content that should only be visible to non-authenticated users.
 *
 *
 * <h2 id="usage"><a href="#usage">Usage</a></h2>
 * <p>
 * To use the security taglib, declare the <code>omnifaces.security</code> namespace in your Facelets view:
 *
 * <pre>
 * &lt;html xmlns:sec="omnifaces.security"&gt;
 * </pre>
 * <p>
 * The <code>&lt;sec:isAnonymous&gt;</code> tag has no attributes. Simply wrap the content you want to show only to anonymous users.
 *
 *
 * <h2 id="example-login"><a href="#example-login">Example: Login form for anonymous users</a></h2>
 * <p>
 * Display a login link only when the user is not authenticated:
 *
 * <pre>
 * &lt;sec:isAnonymous&gt;
 *     &lt;h:link value="Login" outcome="/login" /&gt;
 * &lt;/sec:isAnonymous&gt;
 * </pre>
 *
 *
 * <h2 id="example-welcome"><a href="#example-welcome">Example: Welcome message for guests</a></h2>
 * <p>
 * Show a different welcome message for anonymous users:
 *
 * <pre>
 * &lt;sec:isAnonymous&gt;
 *     &lt;h:outputText value="Welcome, Guest! Please login to access all features." /&gt;
 * &lt;/sec:isAnonymous&gt;
 * </pre>
 *
 *
 * <h2 id="example-combined"><a href="#example-combined">Example: Combined with isAuthenticated</a></h2>
 * <p>
 * Use together with <code>&lt;sec:isAuthenticated&gt;</code> to show different content based on authentication status:
 *
 * <pre>
 * &lt;sec:isAnonymous&gt;
 *     &lt;h:form&gt;
 *         &lt;h:outputLabel for="username" value="Username:" /&gt;
 *         &lt;h:inputText id="username" value="#{loginBean.username}" /&gt;
 *         &lt;h:commandButton value="Login" action="#{loginBean.login}" /&gt;
 *     &lt;/h:form&gt;
 * &lt;/sec:isAnonymous&gt;
 *
 * &lt;sec:isAuthenticated&gt;
 *     &lt;h:outputText value="Welcome back, #{request.remoteUser}!" /&gt;
 * &lt;/sec:isAuthenticated&gt;
 * </pre>
 *
 *
 * <h2 id="implementation"><a href="#implementation">Implementation details</a></h2>
 * <p>
 * This tag checks if {@link SecurityContext#getCallerPrincipal()} returns <code>null</code>. If the principal is <code>null</code>, the user is considered
 * anonymous and the content will be rendered.
 *
 *
 * <h2 id="configuration"><a href="#configuration">Configuration</a></h2>
 * <p>
 * This tag requires {@link SecurityContext} from <code>jakarta.security.enterprise</code> to be available. If the security context is not available, a warning
 * will be logged and no content will be rendered. Make sure your application has Jakarta Security properly configured.
 *
 * @see BaseSecurityTagHandler
 * @see AuthenticatedTagHandler
 * @see AuthorizeTagHandler
 * @author Leonardo Bernardes (@redddcyclone)
 * @author Bauke Scholtz
 * @since 5.0
 */
@FacesTagHandler(namespace = OmniFaces.OMNIFACES_SECURITY_NAMESPACE, tagName = "isAnonymous")
public class AnonymousTagHandler extends BaseSecurityTagHandler {

    /**
     * Constructor for the TagHandler
     *
     * @param config TagConfig
     */
    public AnonymousTagHandler(TagConfig config) {
        super(config);
    }

    @Override
    public void apply(FaceletContext context, UIComponent parent) throws IOException {
        var identity = getSecurityContext();

        if (identity.isEmpty()) {
            return;
        }

        if (identity.get().getCallerPrincipal() == null) {
            this.nextHandler.apply(context, parent);
        }
    }

}
