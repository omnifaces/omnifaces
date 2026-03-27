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
 * The <code>&lt;sec:isAuthenticated&gt;</code> tag conditionally renders its content only when the user is authenticated (logged in). This is useful for
 * displaying user-specific content, logout buttons, or other features that should only be available to authenticated users.
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
 * The <code>&lt;sec:isAuthenticated&gt;</code> tag has no attributes. Simply wrap the content you want to show only to authenticated users.
 *
 *
 * <h2 id="example-welcome"><a href="#example-welcome">Example: Welcome message for authenticated users</a></h2>
 * <p>
 * Display a personalized welcome message only when the user is authenticated:
 * 
 * <pre>
 * &lt;sec:isAuthenticated&gt;
 *     &lt;h:outputText value="Welcome back, #{request.remoteUser}!" /&gt;
 * &lt;/sec:isAuthenticated&gt;
 * </pre>
 *
 *
 * <h2 id="example-logout"><a href="#example-logout">Example: Logout button for authenticated users</a></h2>
 * <p>
 * Show a logout button only when the user is authenticated:
 * 
 * <pre>
 * &lt;sec:isAuthenticated&gt;
 *     &lt;h:form&gt;
 *         &lt;h:commandButton value="Logout" action="#{loginBean.logout}" /&gt;
 *     &lt;/h:form&gt;
 * &lt;/sec:isAuthenticated&gt;
 * </pre>
 *
 *
 * <h2 id="example-navigation"><a href="#example-navigation">Example: User-specific navigation</a></h2>
 * <p>
 * Display navigation links that are only available to authenticated users:
 * 
 * <pre>
 * &lt;sec:isAuthenticated&gt;
 *     &lt;ul&gt;
 *         &lt;li&gt;&lt;h:link value="My Profile" outcome="/profile" /&gt;&lt;/li&gt;
 *         &lt;li&gt;&lt;h:link value="Settings" outcome="/settings" /&gt;&lt;/li&gt;
 *         &lt;li&gt;&lt;h:link value="My Orders" outcome="/orders" /&gt;&lt;/li&gt;
 *     &lt;/ul&gt;
 * &lt;/sec:isAuthenticated&gt;
 * </pre>
 *
 *
 * <h2 id="example-combined"><a href="#example-combined">Example: Combined with isAnonymous</a></h2>
 * <p>
 * Use together with <code>&lt;sec:isAnonymous&gt;</code> to show different navigation based on authentication status:
 * 
 * <pre>
 * &lt;sec:isAnonymous&gt;
 *     &lt;h:link value="Login" outcome="/login" /&gt;
 *     &lt;h:link value="Register" outcome="/register" /&gt;
 * &lt;/sec:isAnonymous&gt;
 *
 * &lt;sec:isAuthenticated&gt;
 *     &lt;h:link value="Profile" outcome="/profile" /&gt;
 *     &lt;h:form&gt;
 *         &lt;h:commandLink value="Logout" action="#{loginBean.logout}" /&gt;
 *     &lt;/h:form&gt;
 * &lt;/sec:isAuthenticated&gt;
 * </pre>
 *
 *
 * <h2 id="example-authorization"><a href="#example-authorization">Example: Combined with authorize</a></h2>
 * <p>
 * Use together with <code>&lt;sec:authorize&gt;</code> to combine authentication and role-based authorization:
 * 
 * <pre>
 * &lt;sec:isAuthenticated&gt;
 *     &lt;h:link value="Dashboard" outcome="/dashboard" /&gt;
 *
 *     &lt;sec:authorize role="ADMIN"&gt;
 *         &lt;h:link value="Admin Panel" outcome="/admin" /&gt;
 *     &lt;/sec:authorize&gt;
 * &lt;/sec:isAuthenticated&gt;
 * </pre>
 *
 *
 * <h2 id="implementation"><a href="#implementation">Implementation details</a></h2>
 * <p>
 * This tag checks if {@link SecurityContext#getCallerPrincipal()} returns a non-<code>null</code> value. If the principal is not <code>null</code>, the user is
 * considered authenticated and the content will be rendered.
 *
 *
 * <h2 id="configuration"><a href="#configuration">Configuration</a></h2>
 * <p>
 * This tag requires {@link SecurityContext} from <code>jakarta.security.enterprise</code> to be available. If the security context is not available, a warning
 * will be logged and no content will be rendered. Make sure your application has Jakarta Security properly configured.
 *
 * @see BaseSecurityTagHandler
 * @see AnonymousTagHandler
 * @see AuthorizeTagHandler
 * @author Leonardo Bernardes (@redddcyclone)
 * @author Bauke Scholtz
 * @since 5.0
 */
@FacesTagHandler(namespace = OmniFaces.OMNIFACES_SECURITY_NAMESPACE, tagName = "isAuthenticated")
public class AuthenticatedTagHandler extends BaseSecurityTagHandler {

    /**
     * Constructor for the TagHandler
     *
     * @param config TagConfig
     */
    public AuthenticatedTagHandler(TagConfig config) {
        super(config);
    }

    @Override
    public void apply(FaceletContext context, UIComponent parent) throws IOException {
        var identity = getSecurityContext();

        if (identity.isEmpty()) {
            return;
        }

        if (identity.get().getCallerPrincipal() != null) {
            this.nextHandler.apply(context, parent);
        }
    }

}
