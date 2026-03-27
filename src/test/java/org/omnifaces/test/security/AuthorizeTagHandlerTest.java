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
package org.omnifaces.test.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import jakarta.faces.view.facelets.FaceletContext;
import jakarta.faces.view.facelets.Tag;
import jakarta.faces.view.facelets.TagAttribute;
import jakarta.faces.view.facelets.TagAttributeException;
import jakarta.faces.view.facelets.TagAttributes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.omnifaces.security.AuthorizeTagHandler;

@ExtendWith(MockitoExtension.class)
class AuthorizeTagHandlerTest extends BaseSecurityTagHandlerTest {

    @Mock
    private Tag tag;

    @Mock
    private TagAttributes tagAttributes;

    private final Map<String, TagAttribute> mockedAttributes = new HashMap<>();

    @BeforeEach
    void setUp() {
        mockedAttributes.clear();
        when(tagConfig.getTag()).thenReturn(tag);
        when(tag.getAttributes()).thenReturn(tagAttributes);
        when(tagAttributes.get(anyString())).thenAnswer(invocation -> mockedAttributes.get(invocation.getArgument(0)));
    }

    @Test
    void testRoleAttribute_authorized() throws Throwable {
        mockAttribute("role", "ADMIN");
        when(securityContext.isCallerInRole("ADMIN")).thenReturn(true);
        var handler = new AuthorizeTagHandler(tagConfig);

        withMockedCDI(() -> {
            assertDoesNotThrow(() -> handler.apply(faceletContext, parent));
            verify(nextHandler).apply(faceletContext, parent);
        });
    }

    @Test
    void testRoleAttribute_unauthorized() throws Throwable {
        mockAttribute("role", "ADMIN");
        when(securityContext.isCallerInRole("ADMIN")).thenReturn(false);
        var handler = new AuthorizeTagHandler(tagConfig);

        withMockedCDI(() -> {
            assertDoesNotThrow(() -> handler.apply(faceletContext, parent));
            verify(nextHandler, never()).apply(any(), any());
        });
    }

    @Test
    void testAnyRoleAttribute_userHasOneRole() throws Throwable {
        mockAttribute("anyRole", "ADMIN, MODERATOR, EDITOR");
        lenient().when(securityContext.isCallerInRole("ADMIN")).thenReturn(false);
        lenient().when(securityContext.isCallerInRole("MODERATOR")).thenReturn(true);
        lenient().when(securityContext.isCallerInRole("EDITOR")).thenReturn(false);
        var handler = new AuthorizeTagHandler(tagConfig);

        withMockedCDI(() -> {
            assertDoesNotThrow(() -> handler.apply(faceletContext, parent));
            verify(nextHandler).apply(faceletContext, parent);
        });
    }

    @Test
    void testAnyRoleAttribute_userHasNoRoles() throws Throwable {
        mockAttribute("anyRole", "ADMIN, MODERATOR");
        when(securityContext.isCallerInRole(anyString())).thenReturn(false);
        var handler = new AuthorizeTagHandler(tagConfig);

        withMockedCDI(() -> {
            assertDoesNotThrow(() -> handler.apply(faceletContext, parent));
            verify(nextHandler, never()).apply(any(), any());
        });
    }

    @Test
    void testAllRolesAttribute_userHasAllRoles() throws Throwable {
        mockAttribute("allRoles", "ADMIN, AUDITOR");
        when(securityContext.isCallerInRole("ADMIN")).thenReturn(true);
        when(securityContext.isCallerInRole("AUDITOR")).thenReturn(true);
        var handler = new AuthorizeTagHandler(tagConfig);

        withMockedCDI(() -> {
            assertDoesNotThrow(() -> handler.apply(faceletContext, parent));
            verify(nextHandler).apply(faceletContext, parent);
        });
    }

    @Test
    void testAllRolesAttribute_userMissingOneRole() throws Throwable {
        mockAttribute("allRoles", "ADMIN, AUDITOR");
        when(securityContext.isCallerInRole("ADMIN")).thenReturn(true);
        when(securityContext.isCallerInRole("AUDITOR")).thenReturn(false);
        var handler = new AuthorizeTagHandler(tagConfig);

        withMockedCDI(() -> {
            assertDoesNotThrow(() -> handler.apply(faceletContext, parent));
            verify(nextHandler, never()).apply(any(), any());
        });
    }

    @Test
    void testVarAttribute_authorized() throws Throwable {
        mockAttribute("role", "ADMIN");
        mockAttribute("var", "isAdmin");
        when(securityContext.isCallerInRole("ADMIN")).thenReturn(true);
        var handler = new AuthorizeTagHandler(tagConfig);

        withMockedCDI(() -> {
            assertDoesNotThrow(() -> handler.apply(faceletContext, parent));
            verify(faceletContext).setAttribute("isAdmin", true);
        });
    }

    @Test
    void testVarAttribute_unauthorized() throws Throwable {
        mockAttribute("role", "ADMIN");
        mockAttribute("var", "isAdmin");
        when(securityContext.isCallerInRole("ADMIN")).thenReturn(false);
        var handler = new AuthorizeTagHandler(tagConfig);

        withMockedCDI(() -> {
            assertDoesNotThrow(() -> handler.apply(faceletContext, parent));
            verify(faceletContext).setAttribute("isAdmin", false);
        });
    }

    @Test
    void testNoAttributesSpecified_throwsException() throws Throwable {
        var handler = new AuthorizeTagHandler(tagConfig);

        withMockedCDI(() -> {
            assertThrows(TagAttributeException.class, () -> handler.apply(faceletContext, parent));
        });
    }

    @Test
    void testMultipleAttributesSpecified_throwsException() throws Throwable {
        mockAttribute("role", "ADMIN");
        mockAttribute("anyRole", "USER");
        var handler = new AuthorizeTagHandler(tagConfig);

        withMockedCDI(() -> {
            assertThrows(TagAttributeException.class, () -> handler.apply(faceletContext, parent));
        });
    }

    @Test
    void testRoleAttributeWithCommas_throwsException() throws Throwable {
        mockAttribute("role", "ADMIN,USER");
        var handler = new AuthorizeTagHandler(tagConfig);

        withMockedCDI(() -> {
            assertThrows(TagAttributeException.class, () -> handler.apply(faceletContext, parent));
        });
    }

    @Test
    void testNullSecurityContext_noException() throws Throwable {
        mockAttribute("role", "ADMIN");
        when(securityContextInstance.get()).thenReturn(null);
        var handler = new AuthorizeTagHandler(tagConfig);

        withMockedCDI(() -> {
            assertDoesNotThrow(() -> handler.apply(faceletContext, parent));
            verify(nextHandler, never()).apply(any(), any());
        });
    }

    @Test
    void testEmptyRoleValue_noException() throws Throwable {
        mockAttribute("role", "");
        var handler = new AuthorizeTagHandler(tagConfig);

        withMockedCDI(() -> {
            assertDoesNotThrow(() -> handler.apply(faceletContext, parent));
            verify(nextHandler, never()).apply(any(), any());
        });
    }

    @Test
    void testBlankRoleValue_noException() throws Throwable {
        mockAttribute("role", " ");
        var handler = new AuthorizeTagHandler(tagConfig);

        withMockedCDI(() -> {
            assertDoesNotThrow(() -> handler.apply(faceletContext, parent));
            verify(nextHandler, never()).apply(any(), any());
        });
    }

    @Test
    void testRoleWithWhitespace_processedCorrectly() throws Throwable {
        mockAttribute("role", " ADMIN ");
        when(securityContext.isCallerInRole("ADMIN")).thenReturn(true);
        var handler = new AuthorizeTagHandler(tagConfig);

        withMockedCDI(() -> {
            assertDoesNotThrow(() -> handler.apply(faceletContext, parent));
            verify(nextHandler).apply(faceletContext, parent);
        });
    }

    @Test
    void testAnyRoleWithWhitespace_processedCorrectly() throws Throwable {
        mockAttribute("anyRole", " ADMIN , USER ");
        when(securityContext.isCallerInRole("ADMIN")).thenReturn(false);
        when(securityContext.isCallerInRole("USER")).thenReturn(true);
        var handler = new AuthorizeTagHandler(tagConfig);

        withMockedCDI(() -> {
            assertDoesNotThrow(() -> handler.apply(faceletContext, parent));
            verify(nextHandler).apply(faceletContext, parent);
        });
    }

    @Test
    void testEmptyRoleValue_varStillSet() throws Throwable {
        mockAttribute("role", "");
        mockAttribute("var", "hasRole");
        var handler = new AuthorizeTagHandler(tagConfig);

        withMockedCDI(() -> {
            assertDoesNotThrow(() -> handler.apply(faceletContext, parent));
            verify(nextHandler, never()).apply(any(), any());
            verify(faceletContext).setAttribute("hasRole", false);
        });
    }

    @Test
    void testBlankRoleValue_varStillSet() throws Throwable {
        mockAttribute("role", "   ");
        mockAttribute("var", "hasRole");
        var handler = new AuthorizeTagHandler(tagConfig);

        withMockedCDI(() -> {
            assertDoesNotThrow(() -> handler.apply(faceletContext, parent));
            verify(nextHandler, never()).apply(any(), any());
            verify(faceletContext).setAttribute("hasRole", false);
        });
    }

    @Test
    void testEmptyAnyRoleValue_varStillSet() throws Throwable {
        mockAttribute("anyRole", "");
        mockAttribute("var", "hasAnyRole");
        var handler = new AuthorizeTagHandler(tagConfig);

        withMockedCDI(() -> {
            assertDoesNotThrow(() -> handler.apply(faceletContext, parent));
            verify(nextHandler, never()).apply(any(), any());
            verify(faceletContext).setAttribute("hasAnyRole", false);
        });
    }

    @Test
    void testEmptyAllRolesValue_varStillSet() throws Throwable {
        mockAttribute("allRoles", "");
        mockAttribute("var", "hasAllRoles");
        var handler = new AuthorizeTagHandler(tagConfig);

        withMockedCDI(() -> {
            assertDoesNotThrow(() -> handler.apply(faceletContext, parent));
            verify(nextHandler, never()).apply(any(), any());
            verify(faceletContext).setAttribute("hasAllRoles", false);
        });
    }

    private void mockAttribute(String name, String value) {
        var attr = mock(TagAttribute.class);
        lenient().when(attr.getValue()).thenReturn(value);
        lenient().when(attr.getValue(any(FaceletContext.class))).thenReturn(value);
        mockedAttributes.put(name, attr);
    }

}
