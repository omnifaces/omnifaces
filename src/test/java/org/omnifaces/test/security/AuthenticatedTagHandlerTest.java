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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.omnifaces.security.AuthenticatedTagHandler;

@ExtendWith(MockitoExtension.class)
class AuthenticatedTagHandlerTest extends BaseSecurityTagHandlerTest {

    @Mock
    private Principal principal;

    @Test
    void testUserIsAuthenticated_contentRendered() throws Throwable {
        when(securityContext.getCallerPrincipal()).thenReturn(principal);
        var handler = new AuthenticatedTagHandler(tagConfig);

        withMockedCDI(() -> {
            assertDoesNotThrow(() -> handler.apply(faceletContext, parent));
            verify(nextHandler).apply(faceletContext, parent);
        });
    }

    @Test
    void testUserIsAnonymous_contentNotRendered() throws Throwable {
        when(securityContext.getCallerPrincipal()).thenReturn(null);
        var handler = new AuthenticatedTagHandler(tagConfig);

        withMockedCDI(() -> {
            assertDoesNotThrow(() -> handler.apply(faceletContext, parent));
            verify(nextHandler, never()).apply(any(), any());
        });
    }

    @Test
    void testNullSecurityContext_noException() throws Throwable {
        when(securityContextInstance.get()).thenReturn(null);
        var handler = new AuthenticatedTagHandler(tagConfig);

        withMockedCDI(() -> {
            assertDoesNotThrow(() -> handler.apply(faceletContext, parent));
            verify(nextHandler, never()).apply(any(), any());
        });
    }

    @Test
    void testMultipleInvocations_consistentBehavior() throws Throwable {
        when(securityContext.getCallerPrincipal()).thenReturn(principal);
        var handler = new AuthenticatedTagHandler(tagConfig);

        withMockedCDI(() -> {
            assertDoesNotThrow(() -> handler.apply(faceletContext, parent));
            assertDoesNotThrow(() -> handler.apply(faceletContext, parent));
            verify(nextHandler, times(2)).apply(faceletContext, parent);
        });
    }

    @Test
    void testDifferentPrincipals_contentAlwaysRendered() throws Throwable {
        var principal1 = mock(Principal.class);
        var principal2 = mock(Principal.class);
        var handler = new AuthenticatedTagHandler(tagConfig);

        withMockedCDI(() -> {
            when(securityContext.getCallerPrincipal()).thenReturn(principal1);
            assertDoesNotThrow(() -> handler.apply(faceletContext, parent));

            when(securityContext.getCallerPrincipal()).thenReturn(principal2);
            assertDoesNotThrow(() -> handler.apply(faceletContext, parent));

            verify(nextHandler, times(2)).apply(faceletContext, parent);
        });
    }

}
