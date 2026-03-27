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

import static java.util.Arrays.stream;
import static java.util.stream.Stream.of;

import java.io.IOException;
import java.util.Objects;

import jakarta.faces.component.UIComponent;
import jakarta.faces.view.facelets.FaceletContext;
import jakarta.faces.view.facelets.TagAttribute;
import jakarta.faces.view.facelets.TagAttributeException;
import jakarta.faces.view.facelets.TagConfig;
import jakarta.security.enterprise.SecurityContext;

import org.omnifaces.config.OmniFaces;
import org.omnifaces.vdl.FacesAttribute;
import org.omnifaces.vdl.FacesTagHandler;

/**
 * <p>
 * The <code>&lt;sec:authorize&gt;</code> tag conditionally renders its content based on role-based access control using {@link SecurityContext}. It provides
 * three mutually exclusive ways to check user roles: single role check, any-of-roles check, or all-of-roles check.
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
 * The <code>&lt;sec:authorize&gt;</code> tag requires exactly one of the following attributes: <strong><code>role</code></strong>,
 * <strong><code>anyRole</code></strong>, or <strong><code>allRoles</code></strong>.
 *
 *
 * <h2 id="single-role"><a href="#single-role">Single role check</a></h2>
 * <p>
 * Use the <strong><code>role</code></strong> attribute to check if the user has a specific role. The content will only be rendered if the user has the
 * specified role.
 * 
 * <pre>
 * &lt;sec:authorize role="ADMIN"&gt;
 *     &lt;h:link value="Admin Panel" outcome="/admin" /&gt;
 * &lt;/sec:authorize&gt;
 * </pre>
 *
 *
 * <h2 id="any-role"><a href="#any-role">Any-of-roles check</a></h2>
 * <p>
 * Use the <strong><code>anyRole</code></strong> attribute with comma-separated role names to check if the user has at least one of the specified roles. The
 * content will be rendered if the user has <em>any</em> of the roles.
 * 
 * <pre>
 * &lt;sec:authorize anyRole="ADMIN, MODERATOR, EDITOR"&gt;
 *     &lt;h:link value="Content Management" outcome="/cms" /&gt;
 * &lt;/sec:authorize&gt;
 * </pre>
 *
 *
 * <h2 id="all-roles"><a href="#all-roles">All-of-roles check</a></h2>
 * <p>
 * Use the <strong><code>allRoles</code></strong> attribute with comma-separated role names to check if the user has all of the specified roles. The content
 * will only be rendered if the user has <em>all</em> of the roles.
 * 
 * <pre>
 * &lt;sec:authorize allRoles="ADMIN, AUDITOR"&gt;
 *     &lt;h:link value="Audit Logs" outcome="/audit" /&gt;
 * &lt;/sec:authorize&gt;
 * </pre>
 *
 *
 * <h2 id="var"><a href="#var">Exposing authorization result</a></h2>
 * <p>
 * The optional <strong><code>var</code></strong> attribute exposes the boolean authorization result as a view-scoped variable. This is useful when you need to
 * use the authorization result in multiple places without repeating the role check.
 * 
 * <pre>
 * &lt;sec:authorize role="ADMIN" var="isAdmin" /&gt;
 *
 * &lt;h:panelGroup rendered="#{isAdmin}"&gt;
 *     &lt;h:link value="Admin Panel" outcome="/admin" /&gt;
 * &lt;/h:panelGroup&gt;
 *
 * &lt;h:outputText value="Welcome, Administrator!" rendered="#{isAdmin}" /&gt;
 * </pre>
 * <p>
 * The variable is always set regardless of whether the content inside the tag is rendered or not.
 *
 *
 * <h2 id="configuration"><a href="#configuration">Configuration</a></h2>
 * <p>
 * This tag requires {@link SecurityContext} from <code>jakarta.security.enterprise</code> to be available. If the security context is not available, a warning
 * will be logged and no content will be rendered. Make sure your application has Jakarta Security properly configured.
 *
 * @see BaseSecurityTagHandler
 * @see AnonymousTagHandler
 * @see AuthenticatedTagHandler
 * @author Leonardo Bernardes (@redddcyclone)
 * @author Bauke Scholtz
 * @since 5.0
 */
@FacesTagHandler(namespace = OmniFaces.OMNIFACES_SECURITY_NAMESPACE, tagName = "authorize")
public class AuthorizeTagHandler extends BaseSecurityTagHandler {

    @FacesAttribute(description = "Allows content to be rendered if the user has the role specified here. Mutually exclusive with anyRole and allRoles.")
    private final TagAttribute role;

    @FacesAttribute(
        description = "Allows content to be rendered if the user has any of the comma-separated roles added here. Mutually exclusive with role and allRoles."
    )
    private final TagAttribute anyRole;

    @FacesAttribute(
        description = "Allows content to be rendered if the user has all of the comma-separated roles added here. Mutually exclusive with role and anyRole."
    )
    private final TagAttribute allRoles;

    @FacesAttribute(
        name = "var", description = "Name for a boolean EL variable containing the authorization result. Always set regardless of whether content is rendered."
    )
    private final TagAttribute varAttribute;

    /**
     * Constructor for the TagHandler
     *
     * @param config TagConfig
     */
    public AuthorizeTagHandler(TagConfig config) {
        super(config);
        role = getAttribute("role");
        anyRole = getAttribute("anyRole");
        allRoles = getAttribute("allRoles");
        varAttribute = getAttribute("var");
    }

    @Override
    public void apply(FaceletContext context, UIComponent parent) throws IOException {
        var optionalIdentity = getSecurityContext();

        if (optionalIdentity.isEmpty()) {
            return;
        }

        validateExactlyOneAttribute();

        var authorized = determineAuthorization(context, optionalIdentity.get());

        if (authorized) {
            this.nextHandler.apply(context, parent);
        }

        setVarIfSpecified(context, authorized);
    }

    private void validateExactlyOneAttribute() {
        var count = of(role, anyRole, allRoles).filter(Objects::nonNull).count();

        if (count == 0) {
            throw new TagAttributeException(null, "One of the following attributes must be specified: role, anyRole, or allRoles");
        }

        if (count > 1) {
            throw new TagAttributeException(null, "Only one of the following attributes may be specified: role, anyRole, or allRoles");
        }
    }

    private boolean determineAuthorization(FaceletContext context, SecurityContext identity) {
        if (role != null) {
            return checkSingleRole(identity);
        }

        if (anyRole != null) {
            return checkAnyRole(context, identity);
        }

        if (allRoles != null) {
            return checkAllRoles(context, identity);
        }

        throw new IllegalStateException();
    }

    private boolean checkSingleRole(SecurityContext identity) {
        var roleValue = role.getValue();

        if (roleValue == null || roleValue.isBlank()) {
            return false;
        }

        if (roleValue.contains(",")) {
            throw new TagAttributeException(role, "The role attribute expects a single role, not multiple comma-separated roles");
        }

        return identity.isCallerInRole(roleValue.strip());
    }

    private boolean checkAnyRole(FaceletContext context, SecurityContext identity) {
        var rolesValue = anyRole.getValue(context);

        if (rolesValue == null || rolesValue.isBlank()) {
            return false;
        }

        return stream(rolesValue.split(",")).map(String::strip).anyMatch(identity::isCallerInRole);
    }

    private boolean checkAllRoles(FaceletContext context, SecurityContext identity) {
        var allRolesValue = allRoles.getValue(context);

        if (allRolesValue == null || allRolesValue.isBlank()) {
            return false;
        }

        return stream(allRolesValue.split(",")).map(String::strip).allMatch(identity::isCallerInRole);
    }

    private void setVarIfSpecified(FaceletContext context, boolean authorized) {
        if (varAttribute == null) {
            return;
        }

        var varValue = varAttribute.getValue();

        if (varValue != null && !varValue.isEmpty()) {
            context.setAttribute(varValue, authorized);
        }
    }

}
