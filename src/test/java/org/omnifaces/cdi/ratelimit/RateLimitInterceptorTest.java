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
package org.omnifaces.cdi.ratelimit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import jakarta.enterprise.context.RequestScoped;
import jakarta.interceptor.InvocationContext;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omnifaces.cdi.RateLimit;
import org.omnifaces.util.Beans;
import org.omnifaces.util.Faces;

/**
 * When {@link RateLimit#clientId()} is blank, the rate limit key is the client IP address as obtained by {@link HttpServletRequest#getRemoteAddr()}, never from
 * proxy headers, because the client controls them.
 */
class RateLimitInterceptorTest {

    private static final int MAX_REQUESTS = 2;
    private static final String REMOTE_IP = "192.0.2.1";
    private static final String SPOOFED_IP_PREFIX = "198.51.100.";
    private static final String[] PROXY_HEADERS = { "Forwarded", "X-Forwarded-For", "X-Real-IP" };

    private RateLimiter rateLimiter;
    private RateLimitInterceptor interceptor;
    private InvocationContext context;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() throws Exception {
        rateLimiter = new RateLimiter();
        rateLimiter.init();
        interceptor = new RateLimitInterceptor(rateLimiter);
        context = mock(InvocationContext.class);
        when(context.getMethod()).thenReturn(RateLimitedByRemoteAddr.class.getMethod("invoke"));
        request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(REMOTE_IP);
    }

    @AfterEach
    void tearDown() {
        rateLimiter.destroy();
    }

    @Test
    void testProxyHeadersDoNotDetermineClientIPInFacesRequest() {
        try (var faces = mockStatic(Faces.class)) {
            faces.when(Faces::hasContext).thenReturn(true);
            faces.when(Faces::getRequest).thenReturn(request);
            assertRateLimitedDespiteSpoofedProxyHeaders();
        }
    }

    @Test
    void testProxyHeadersDoNotDetermineClientIPInServletRequest() {
        try (var beans = mockStatic(Beans.class)) {
            beans.when(() -> Beans.isActive(RequestScoped.class)).thenReturn(true);
            beans.when(() -> Beans.getReference(HttpServletRequest.class)).thenReturn(request);
            assertRateLimitedDespiteSpoofedProxyHeaders();
        }
    }

    private void assertRateLimitedDespiteSpoofedProxyHeaders() {
        for (var i = 0; i < MAX_REQUESTS; i++) {
            stubProxyHeaders(SPOOFED_IP_PREFIX + i);
            assertDoesNotThrow(() -> interceptor.limitRate(context));
        }

        stubProxyHeaders(SPOOFED_IP_PREFIX + MAX_REQUESTS);
        assertThrows(RateLimitExceededException.class, () -> interceptor.limitRate(context));
    }

    private void stubProxyHeaders(String ip) {
        for (var header : PROXY_HEADERS) {
            when(request.getHeader(header)).thenReturn(ip);
        }
    }

    public static class RateLimitedByRemoteAddr {

        @RateLimit(maxRequestsPerTimeWindow = MAX_REQUESTS, timeWindowInSeconds = 60)
        public void invoke() {
            // NOOP
        }

    }

}
