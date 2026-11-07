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
import org.mockito.MockedStatic;
import org.omnifaces.cdi.RateLimit;
import org.omnifaces.util.Beans;
import org.omnifaces.util.Faces;

/**
 * When {@link RateLimit#clientId()} is blank, the rate limit key is the client IP address as obtained by {@link HttpServletRequest#getRemoteAddr()}, never from
 * proxy headers, because the client controls them.
 */
class RateLimitInterceptorTest {

    private static final int MAX_REQUESTS = 2;
    private static final long TIME_WINDOW_IN_SECONDS = 60;
    private static final String API_CLIENT_ID = "api";
    private static final String REMOTE_IP = "192.0.2.1";
    private static final String SPOOFED_IP_PREFIX = "198.51.100.";
    private static final String[] PROXY_HEADERS = { "Forwarded", "X-Forwarded-For", "X-Real-IP" };

    private RateLimiter rateLimiter;
    private RateLimitInterceptor interceptor;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter();
        rateLimiter.init();
        interceptor = new RateLimitInterceptor(rateLimiter);
        request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(REMOTE_IP);
    }

    @AfterEach
    void tearDown() {
        rateLimiter.destroy();
    }

    @Test
    void testProxyHeadersDoNotDetermineClientIPInFacesRequest() {
        try (var faces = mockFacesRequest()) {
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

    /**
     * Without <code>clientId</code>, each annotated method has its own counter per client IP address, so that one endpoint cannot exhaust another.
     */
    @Test
    void testMethodsWithoutClientIdHaveSeparateCounters() {
        try (var faces = mockFacesRequest()) {
            assertAllowedUpToMaxRequests(new MethodLevelRateLimits(), "first");
            assertDoesNotThrow(() -> invoke(new MethodLevelRateLimits(), "second"));
            assertThrows(RateLimitExceededException.class, () -> invoke(new MethodLevelRateLimits(), "first"));
        }
    }

    /**
     * With <code>clientId</code>, all methods with the same configuration share one counter, so that a <code>clientId</code> can represent a whole API.
     */
    @Test
    void testExplicitClientIdIsSharedAcrossMethods() {
        assertAllowedUpToMaxRequests(new MethodLevelRateLimits(), "firstApi");
        assertThrows(RateLimitExceededException.class, () -> invoke(new MethodLevelRateLimits(), "secondApi"));
    }

    @Test
    void testClassLevelAnnotationWithoutClientIdSharesOneCounterPerClass() {
        try (var faces = mockFacesRequest()) {
            assertAllowedUpToMaxRequests(new ClassLevelRateLimit(), "first");
            assertThrows(RateLimitExceededException.class, () -> invoke(new ClassLevelRateLimit(), "second"));
        }
    }

    @Test
    void testMethodLevelAnnotationInClassLevelAnnotatedClassHasSeparateCounter() {
        try (var faces = mockFacesRequest()) {
            assertAllowedUpToMaxRequests(new ClassLevelRateLimit(), "first");
            assertDoesNotThrow(() -> invoke(new ClassLevelRateLimit(), "own"));
            assertThrows(RateLimitExceededException.class, () -> invoke(new ClassLevelRateLimit(), "second"));
        }
    }

    /**
     * The class-level counter belongs to the class which declares the annotation, so that subclasses, including container generated proxy classes, share it.
     */
    @Test
    void testClassLevelAnnotationIsSharedAcrossSubclasses() {
        try (var faces = mockFacesRequest()) {
            assertAllowedUpToMaxRequests(new FirstClassLevelRateLimitSubclass(), "first");
            assertThrows(RateLimitExceededException.class, () -> invoke(new SecondClassLevelRateLimitSubclass(), "first"));
        }
    }

    private MockedStatic<Faces> mockFacesRequest() {
        var faces = mockStatic(Faces.class);
        faces.when(Faces::hasContext).thenReturn(true);
        faces.when(Faces::getRequest).thenReturn(request);
        return faces;
    }

    private void assertAllowedUpToMaxRequests(Object target, String methodName) {
        for (var i = 0; i < MAX_REQUESTS; i++) {
            assertDoesNotThrow(() -> invoke(target, methodName));
        }
    }

    private void invoke(Object target, String methodName) throws Exception {
        var invocation = mock(InvocationContext.class);
        when(invocation.getMethod()).thenReturn(target.getClass().getMethod(methodName));
        when(invocation.getTarget()).thenReturn(target);
        interceptor.limitRate(invocation);
    }

    private void assertRateLimitedDespiteSpoofedProxyHeaders() {
        for (var i = 0; i < MAX_REQUESTS; i++) {
            stubProxyHeaders(SPOOFED_IP_PREFIX + i);
            assertDoesNotThrow(() -> invoke(new MethodLevelRateLimits(), "first"));
        }

        stubProxyHeaders(SPOOFED_IP_PREFIX + MAX_REQUESTS);
        assertThrows(RateLimitExceededException.class, () -> invoke(new MethodLevelRateLimits(), "first"));
    }

    private void stubProxyHeaders(String ip) {
        for (var header : PROXY_HEADERS) {
            when(request.getHeader(header)).thenReturn(ip);
        }
    }

    public static class MethodLevelRateLimits {

        @RateLimit(maxRequestsPerTimeWindow = MAX_REQUESTS, timeWindowInSeconds = TIME_WINDOW_IN_SECONDS)
        public void first() {
            // NOOP
        }

        @RateLimit(maxRequestsPerTimeWindow = MAX_REQUESTS, timeWindowInSeconds = TIME_WINDOW_IN_SECONDS)
        public void second() {
            // NOOP
        }

        @RateLimit(clientId = API_CLIENT_ID, maxRequestsPerTimeWindow = MAX_REQUESTS, timeWindowInSeconds = TIME_WINDOW_IN_SECONDS)
        public void firstApi() {
            // NOOP
        }

        @RateLimit(clientId = API_CLIENT_ID, maxRequestsPerTimeWindow = MAX_REQUESTS, timeWindowInSeconds = TIME_WINDOW_IN_SECONDS)
        public void secondApi() {
            // NOOP
        }

    }

    @RateLimit(maxRequestsPerTimeWindow = MAX_REQUESTS, timeWindowInSeconds = TIME_WINDOW_IN_SECONDS)
    public static class ClassLevelRateLimit {

        public void first() {
            // NOOP
        }

        public void second() {
            // NOOP
        }

        @RateLimit(maxRequestsPerTimeWindow = MAX_REQUESTS, timeWindowInSeconds = TIME_WINDOW_IN_SECONDS)
        public void own() {
            // NOOP
        }

    }

    public static class FirstClassLevelRateLimitSubclass extends ClassLevelRateLimit {
    }

    public static class SecondClassLevelRateLimitSubclass extends ClassLevelRateLimit {
    }

}
