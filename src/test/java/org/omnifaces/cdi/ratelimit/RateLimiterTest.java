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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.omnifaces.cdi.ratelimit.RateLimiter.PARAM_NAME_MAX_TRACKED_COUNTERS;
import static org.omnifaces.cdi.ratelimit.RateLimiter.getClientId;
import static org.omnifaces.cdi.ratelimit.RateLimiter.isIPv6LiteralCandidate;

import java.time.Duration;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RateLimiterTest {

    private static final String FIRST_IP = "192.0.2.1";
    private static final String SECOND_IP = "192.0.2.2";
    private static final String THIRD_IP = "192.0.2.3";
    private static final String SPOOFED_IP_PREFIX = "198.51.100.";
    private static final String IPV6_IP = "2001:db8:1:2:3:4:5:6";
    private static final String IPV6_IP_IN_SAME_NETWORK = "2001:db8:1:2:ffff::9";
    private static final String IPV6_IP_IN_OTHER_NETWORK = "2001:db8:1:3::1";
    private static final String IPV6_CLIENT_ID = "2001:db8:1:2:0:0:0:0/64";
    private static final String IPV4_MAPPED_IPV6_IP = "::ffff:" + FIRST_IP;
    private static final String NAT64_IPV6_IP = "64:ff9b::" + FIRST_IP;
    private static final String OTHER_NAT64_IPV6_IP = "64:ff9b::" + SECOND_IP;
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
     * Callers with the same client identifier but a different maximum or time window have separate counters, so that one limit cannot exhaust another.
     */
    @Test
    void testSameClientIdWithDifferentLimitsHasSeparateCounters() {
        var longTimeWindow = Duration.ofSeconds(60);
        var shortTimeWindow = Duration.ofSeconds(1);

        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(FIRST_IP, 1, longTimeWindow, 0));
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(FIRST_IP, 1, shortTimeWindow, 0));
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(FIRST_IP, 2, longTimeWindow, 0));
        assertThrows(RateLimitExceededException.class, () -> rateLimiter.checkRateLimit(FIRST_IP, 1, longTimeWindow, 0));
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
        assertEquals(2, rateLimiter.getTrackedCounterCount());

        advanceTicker(timeWindow);
        rateLimiter.removeExpiredCounters();
        assertEquals(0, rateLimiter.getTrackedCounterCount());
    }

    /**
     * A single client usually controls a whole IPv6 <code>/64</code> network, so all addresses in it share one counter.
     */
    @Test
    void testIPv6AddressesInSameNetworkShareCounter() {
        var timeWindow = Duration.ofSeconds(10);
        var maxRequests = 1;

        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(requestFrom(IPV6_IP), maxRequests, timeWindow, 0));
        assertThrows(RateLimitExceededException.class, () -> rateLimiter.checkRateLimit(requestFrom(IPV6_IP_IN_SAME_NETWORK), maxRequests, timeWindow, 0));
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(requestFrom(IPV6_IP_IN_OTHER_NETWORK), maxRequests, timeWindow, 0));
    }

    @Test
    void testIPv4MappedIPv6AddressSharesCounterWithIPv4Address() {
        var timeWindow = Duration.ofSeconds(10);
        var maxRequests = 1;

        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(requestFrom(FIRST_IP), maxRequests, timeWindow, 0));
        assertThrows(RateLimitExceededException.class, () -> rateLimiter.checkRateLimit(requestFrom(IPV4_MAPPED_IPV6_IP), maxRequests, timeWindow, 0));
    }

    /**
     * Behind NAT64, every IPv4 client shares the same <code>/64</code> network prefix, so each embedded IPv4 address must get its own counter.
     */
    @Test
    void testNAT64AddressesAreTrackedByEmbeddedIPv4Address() {
        var timeWindow = Duration.ofSeconds(10);
        var maxRequests = 1;

        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(requestFrom(NAT64_IPV6_IP), maxRequests, timeWindow, 0));
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(requestFrom(OTHER_NAT64_IPV6_IP), maxRequests, timeWindow, 0));
        assertThrows(RateLimitExceededException.class, () -> rateLimiter.checkRateLimit(requestFrom(FIRST_IP), maxRequests, timeWindow, 0));
    }

    @Test
    void testClientIdOfRemoteAddr() {
        assertEquals(FIRST_IP, getClientId(FIRST_IP));
        assertEquals(FIRST_IP, getClientId(IPV4_MAPPED_IPV6_IP));
        assertEquals(FIRST_IP, getClientId(NAT64_IPV6_IP));
        assertEquals(IPV6_CLIENT_ID, getClientId(IPV6_IP));
        assertEquals(IPV6_CLIENT_ID, getClientId(IPV6_IP + "%eth0"));
        assertEquals(IPV6_CLIENT_ID, getClientId("[" + IPV6_IP + "%eth0]"));
        assertEquals("64:ff9b:1:0:0:0:0:0/64", getClientId("64:ff9b:1::" + FIRST_IP));
        assertEquals("ffff:zz", getClientId("ffff:zz"));
        assertEquals("not:an:ip", getClientId("not:an:ip"));
        assertEquals("\u0663:foo", getClientId("\u0663:foo"));
    }

    /**
     * <code>InetAddress#getByName()</code> queries the name service for a host which does not start with an ASCII hex digit, ':' or '[', so such a remote
     * address must never be parsed.
     */
    @Test
    void testIPv6LiteralCandidateNeverRequiresNameService() {
        assertTrue(isIPv6LiteralCandidate("::1"));
        assertTrue(isIPv6LiteralCandidate("[::1]"));
        assertTrue(isIPv6LiteralCandidate(IPV6_IP));
        assertFalse(isIPv6LiteralCandidate(FIRST_IP));
        assertFalse(isIPv6LiteralCandidate("g:1"));
        assertFalse(isIPv6LiteralCandidate("\u0663:foo"));
        assertFalse(isIPv6LiteralCandidate("\uff21:foo"));
        assertFalse(isIPv6LiteralCandidate(null));
    }

    /**
     * When the tracked request counters are at the cap, a request which needs a new counter is rejected, so that the map cannot grow unbounded.
     */
    @Test
    void testNewCounterIsRejectedWhenMaxTrackedCountersIsReached() {
        var timeWindow = Duration.ofSeconds(10);
        var maxRequests = 2;
        rateLimiter.setMaxTrackedCounters(2);

        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(FIRST_IP, maxRequests, timeWindow, 0));
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(SECOND_IP, maxRequests, timeWindow, 0));
        assertThrows(RateLimitExceededException.class, () -> rateLimiter.checkRateLimit(THIRD_IP, maxRequests, timeWindow, 0));
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(FIRST_IP, maxRequests, timeWindow, 0));
        assertEquals(2, rateLimiter.getTrackedCounterCount());
    }

    @Test
    void testExpiredCountersAreRemovedBeforeRejectingNewCounter() {
        var timeWindow = Duration.ofSeconds(10);
        var maxRequests = 1;
        rateLimiter.setMaxTrackedCounters(1);
        freezeTicker();

        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(FIRST_IP, maxRequests, timeWindow, 0));
        advanceTicker(timeWindow);

        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(SECOND_IP, maxRequests, timeWindow, 0));
        assertEquals(1, rateLimiter.getTrackedCounterCount());
    }

    /**
     * A request which needs a new counter is rejected immediately when the cap is reached, because retrying would hold the request thread for no benefit.
     */
    @Test
    void testNewCounterIsNotRetriedWhenMaxTrackedCountersIsReached() {
        var timeWindow = Duration.ofSeconds(10);
        rateLimiter.setMaxTrackedCounters(1);
        freezeTicker();

        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(FIRST_IP, 1, timeWindow, 0));
        var start = System.nanoTime();
        var exception = assertThrows(RateLimitExceededException.class, () -> rateLimiter.checkRateLimit(SECOND_IP, 1, timeWindow, 1));
        var elapsed = Duration.ofNanos(System.nanoTime() - start);
        assertTrue(elapsed.compareTo(Duration.ofMillis(exception.getRecommendedDelayInMillis())) < 0, "new client was retried");
    }

    /**
     * Expired request counters are removed at most once per second when the cap is reached, so that a flood of new counters does not scan the whole map on
     * every request.
     */
    @Test
    void testExpiredCountersRemovalOnFullIsThrottled() {
        var timeWindow = Duration.ofMillis(500);
        var maxRequests = 1;
        rateLimiter.setMaxTrackedCounters(1);
        freezeTicker();

        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(FIRST_IP, maxRequests, timeWindow, 0));
        assertThrows(RateLimitExceededException.class, () -> rateLimiter.checkRateLimit(SECOND_IP, maxRequests, timeWindow, 0));

        advanceTicker(timeWindow);
        assertThrows(RateLimitExceededException.class, () -> rateLimiter.checkRateLimit(THIRD_IP, maxRequests, timeWindow, 0));

        advanceTicker(timeWindow);
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(THIRD_IP, maxRequests, timeWindow, 0));
    }

    @Test
    void testMaxTrackedCountersIsReadFromContextParam() {
        var limiter = createRateLimiterWithContextParam(" 1 ");
        limiter.init();

        try {
            assertDoesNotThrow(() -> limiter.checkRateLimit(FIRST_IP, 1, Duration.ofSeconds(10), 0));
            assertThrows(RateLimitExceededException.class, () -> limiter.checkRateLimit(SECOND_IP, 1, Duration.ofSeconds(10), 0));
        }
        finally {
            limiter.destroy();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "0", "-1", "many" })
    void testInvalidMaxTrackedCountersContextParamIsRejected(String value) {
        var limiter = createRateLimiterWithContextParam(value);

        try {
            assertThrows(IllegalArgumentException.class, limiter::init);
        }
        finally {
            limiter.destroy();
        }
    }

    @Test
    void testMaxRetriesAllowsAdditionalAttempts() {
        var timeWindow = Duration.ofMillis(200); // Short window for testing
        var maxRequests = 1;

        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(request, maxRequests, timeWindow, 0));
        assertThrows(RateLimitExceededException.class, () -> rateLimiter.checkRateLimit(request, maxRequests, timeWindow, 0));
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(request, maxRequests, timeWindow, 1));
    }

    private static HttpServletRequest requestFrom(String remoteAddr) {
        var request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(remoteAddr);
        return request;
    }

    private static RateLimiter createRateLimiterWithContextParam(String value) {
        var servletContext = mock(ServletContext.class);
        when(servletContext.getInitParameter(PARAM_NAME_MAX_TRACKED_COUNTERS)).thenReturn(value);
        var limiter = new RateLimiter();
        limiter.setServletContext(servletContext);
        return limiter;
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
