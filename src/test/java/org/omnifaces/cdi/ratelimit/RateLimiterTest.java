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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

    private static final String FIRST_IP = "192.0.2.1";
    private static final String SECOND_IP = "192.0.2.2";
    private static final String SPOOFED_IP_PREFIX = "198.51.100.";
    private static final String[] PROXY_HEADERS = { "Forwarded", "X-Forwarded-For", "X-Real-IP" };

    private RateLimiter rateLimiter;
    private HttpServletRequest request;
    private long nanos;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter();
        rateLimiter.init();
        request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(FIRST_IP);
    }

    @AfterEach
    void tearDown() {
        rateLimiter.destroy();
    }

    @Test
    void testRequestsWithinLimitShouldPass() {
        var timeWindow = Duration.ofSeconds(10);
        var maxRequests = 5;

        for (var i = 0; i < maxRequests; i++) {
            assertDoesNotThrow(() -> rateLimiter.checkRateLimit(request, maxRequests, timeWindow, 0));
        }
    }

    @Test
    void testRequestsExceedingLimitShouldThrowException() {
        var timeWindow = Duration.ofSeconds(10);
        var maxRequests = 3;

        for (var i = 0; i < maxRequests; i++) {
            assertDoesNotThrow(() -> rateLimiter.checkRateLimit(request, maxRequests, timeWindow, 0));
        }

        assertThrows(RateLimitExceededException.class, () -> rateLimiter.checkRateLimit(request, maxRequests, timeWindow, 0));
    }

    @Test
    void testDifferentIPAddressesAreTrackedSeparately() {
        var timeWindow = Duration.ofSeconds(10);
        var maxRequests = 2;

        var request1 = mock(HttpServletRequest.class);
        when(request1.getRemoteAddr()).thenReturn(FIRST_IP);

        var request2 = mock(HttpServletRequest.class);
        when(request2.getRemoteAddr()).thenReturn(SECOND_IP);

        for (var i = 0; i < maxRequests; i++) {
            assertDoesNotThrow(() -> rateLimiter.checkRateLimit(request1, maxRequests, timeWindow, 0));
            assertDoesNotThrow(() -> rateLimiter.checkRateLimit(request2, maxRequests, timeWindow, 0));
        }

        assertThrows(RateLimitExceededException.class, () -> rateLimiter.checkRateLimit(request1, maxRequests, timeWindow, 0));
        assertThrows(RateLimitExceededException.class, () -> rateLimiter.checkRateLimit(request2, maxRequests, timeWindow, 0));
    }

    @Test
    void testRateLimitResetAfterTimeWindow() {
        var timeWindow = Duration.ofMillis(100);
        var maxRequests = 2;
        freezeTicker();

        for (var i = 0; i < maxRequests; i++) {
            assertDoesNotThrow(() -> rateLimiter.checkRateLimit(request, maxRequests, timeWindow, 0));
        }

        assertThrows(RateLimitExceededException.class, () -> rateLimiter.checkRateLimit(request, maxRequests, timeWindow, 0));

        advanceTicker(timeWindow);

        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(request, maxRequests, timeWindow, 0));
    }

    @Test
    void testExpiredCounterIsReplacedOnNextCheck() {
        var timeWindow = Duration.ofMillis(50);
        var maxRequests = 1;
        freezeTicker();

        var request1 = mock(HttpServletRequest.class);
        when(request1.getRemoteAddr()).thenReturn(FIRST_IP);

        var request2 = mock(HttpServletRequest.class);
        when(request2.getRemoteAddr()).thenReturn(SECOND_IP);

        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(request1, maxRequests, timeWindow, 0));
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(request2, maxRequests, timeWindow, 0));

        advanceTicker(timeWindow);

        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(request1, maxRequests, timeWindow, 0));
    }

    /**
     * The client controls proxy headers such as <code>X-Forwarded-For</code>, so they must not determine the rate limit key.
     */
    @Test
    void testProxyHeadersDoNotDetermineClientIP() {
        var timeWindow = Duration.ofSeconds(10);
        var maxRequests = 2;

        for (var i = 0; i < maxRequests; i++) {
            stubProxyHeaders(SPOOFED_IP_PREFIX + i);
            assertDoesNotThrow(() -> rateLimiter.checkRateLimit(request, maxRequests, timeWindow, 0));
        }

        stubProxyHeaders(SPOOFED_IP_PREFIX + maxRequests);
        assertThrows(RateLimitExceededException.class, () -> rateLimiter.checkRateLimit(request, maxRequests, timeWindow, 0));
    }

    /**
     * Each request counter expires by its own time window, so that a check with a shorter time window does not reset one with a longer time window.
     */
    @Test
    void testShorterTimeWindowDoesNotExpireLongerTimeWindow() {
        var longTimeWindow = Duration.ofSeconds(60);
        var shortTimeWindow = Duration.ofSeconds(1);
        var maxRequests = 1;
        freezeTicker();

        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(FIRST_IP, maxRequests, longTimeWindow, 0));
        advanceTicker(shortTimeWindow);
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(SECOND_IP, maxRequests, shortTimeWindow, 0));
        rateLimiter.removeExpiredCounters();

        assertThrows(RateLimitExceededException.class, () -> rateLimiter.checkRateLimit(FIRST_IP, maxRequests, longTimeWindow, 0));
    }

    /**
     * Expired request counters are removed by the periodic task, so that client identifiers which never return do not stay in the map.
     */
    @Test
    void testExpiredCountersAreRemoved() {
        var timeWindow = Duration.ofSeconds(10);
        var maxRequests = 1;
        freezeTicker();

        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(FIRST_IP, maxRequests, timeWindow, 0));
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(SECOND_IP, maxRequests, timeWindow, 0));
        rateLimiter.removeExpiredCounters();
        assertEquals(2, rateLimiter.getTrackedClientCount());

        advanceTicker(timeWindow);
        rateLimiter.removeExpiredCounters();
        assertEquals(0, rateLimiter.getTrackedClientCount());
    }

    @Test
    void testMaxRetriesAllowsAdditionalAttempts() {
        var timeWindow = Duration.ofMillis(200); // Short window for testing
        var maxRequests = 1;

        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(request, maxRequests, timeWindow, 0));
        assertThrows(RateLimitExceededException.class, () -> rateLimiter.checkRateLimit(request, maxRequests, timeWindow, 0));
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(request, maxRequests, timeWindow, 1));
    }

    private void stubProxyHeaders(String ip) {
        for (var header : PROXY_HEADERS) {
            when(request.getHeader(header)).thenReturn(ip);
        }
    }

    private void freezeTicker() {
        nanos = 0;
        rateLimiter.setTicker(() -> nanos);
    }

    private void advanceTicker(Duration duration) {
        nanos += duration.toNanos();
    }

}
