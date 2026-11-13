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

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.logging.Level.WARNING;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

import org.omnifaces.cdi.RateLimit;
import org.omnifaces.util.JNDI;

/**
 * <p>
 * CDI managed bean for rate limiting requests per client identifier. This utility provides a thread-safe fixed window rate limiting mechanism with configurable
 * request limits and time windows. Callers with the same client identifier, maximum number of requests and time window share one counter.
 * <p>
 * Both public <code>checkRateLimit</code> methods share one counter namespace: {@link #checkRateLimit(String, int, Duration, int)} with a client IP address as
 * client identifier shares its counter with {@link #checkRateLimit(HttpServletRequest, int, Duration, int)} for a request from that client IP address, given
 * the same limits. The {@link RateLimit} annotation without <code>clientId</code> does not share counters with either.
 * <p>
 * The rate limiter supports both HTTP request-based rate limiting (using client IP addresses) and custom client identifier-based rate limiting (using any
 * string identifier such as user IDs, API keys, etc.). It uses a {@link ConcurrentHashMap} to track request counts per client identifier and automatically
 * cleans up expired entries to prevent memory leaks.
 * <p>
 * When the rate limit is exceeded, the rate limiter will by default immediately throw a {@link RateLimitExceededException}. Optionally, you can configure
 * automatic retries via the {@code maxRetries} parameter, which will retry the request after a calculated delay based on the remaining time window. If all
 * retries are exhausted, a {@link RateLimitExceededException} is thrown.
 * <p>
 * The number of tracked request counters is capped by the {@value #PARAM_NAME_MAX_TRACKED_COUNTERS} context parameter, which defaults to
 * {@value #DEFAULT_MAX_TRACKED_COUNTERS}. There is one counter per client identifier per limit, and, for the {@link RateLimit} annotation without
 * <code>clientId</code>, per annotated method or class. When the cap is reached and expired counters cannot be removed, which is attempted at most once per
 * second, a request which needs a new counter is rejected with a {@link RateLimitExceededException}, while existing counters continue to be checked as usual.
 * <p>
 * A request which needs a new counter is rejected rather than let through, so that filling the cap cannot be used to bypass the rate limit. The trade-off is
 * that a client which can choose many client identifiers can fill the cap and lock out new clients for the duration of the time window. When the client
 * identifier is derived from user input, such as a user name submitted to a login form, then validate it before passing it to
 * {@link #checkRateLimit(String, int, Duration, int)}.
 *
 * <h2>Usage</h2>
 * <p>
 * The recommended usage is with the {@link RateLimit} annotation.
 *
 * <pre>
 * &#64;Named
 * &#64;RequestScoped
 * public class ApiController {
 *
 *     &#64;RateLimit(clientId = "FooAPI", maxRequestsPerTimeWindow = 10, maxRetries = 3)
 *     public void processFooApiRequest() {
 *         // Process Foo API request ...
 *     }
 *
 * }
 * </pre>
 *
 * <p>
 * For more fine grained usage, or when you have a variable rate limit parameter which therefore cannot be set as an annotation attribute, then you can inject
 * this CDI bean in any CDI managed artifact and explicitly invoke either {@link #checkRateLimit(HttpServletRequest, int, Duration, int)} or
 * {@link #checkRateLimit(String, int, Duration, int)}. Below is an example of a servlet filter that applies rate limiting to all requests depending on client
 * IP address:
 *
 * <pre>
 * &#64;WebFilter("/*")
 * public class RateLimitFilter extends HttpFilter {
 *
 *     &#64;Inject
 *     private RateLimiter rateLimiter;
 *
 *     &#64;Override
 *     public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
 *         var maxRequestsPerTimeWindow = determineMaxRequestsBasedOn(request);
 *         var timeWindowInSeconds = determineTimeWindowBasedOn(request);
 *         var maxRetries = 0;
 *
 *         try {
 *             rateLimiter.checkRateLimit(request, maxRequestsPerTimeWindow, Duration.ofSeconds(timeWindowInSeconds), maxRetries);
 *             chain.doFilter(request, response);
 *         }
 *         catch (RateLimitExceededException e) {
 *             response.sendError(429); // Too Many Requests
 *         }
 *     }
 *
 * }
 * </pre>
 *
 * @author Bauke Scholtz
 * @since 5.0
 * @see RateLimit
 * @see RateLimitInterceptor
 * @see RateLimitExceededException
 */
@ApplicationScoped
public class RateLimiter {

    // Constants ------------------------------------------------------------------------------------------------------

    /**
     * The context parameter name to specify the maximum number of request counters to track: {@value}.
     *
     * @since 5.5.7
     */
    public static final String PARAM_NAME_MAX_TRACKED_COUNTERS = "org.omnifaces.RATE_LIMITER_MAX_TRACKED_COUNTERS";

    /**
     * The default maximum number of request counters to track: {@value}.
     *
     * @since 5.5.7
     */
    public static final int DEFAULT_MAX_TRACKED_COUNTERS = 100_000;

    private static final Logger logger = Logger.getLogger(RateLimiter.class.getName());

    private static final String THREAD_ID = "omnifaces.RateLimiter.executorService";
    private static final long EXPIRED_COUNTERS_REMOVAL_INTERVAL_IN_SECONDS = 10;
    private static final long EXPIRED_COUNTERS_REMOVAL_ON_FULL_INTERVAL_IN_NANOS = SECONDS.toNanos(1);
    private static final long NEVER = Long.MIN_VALUE;
    private static final long NANOS_PER_MILLI = MILLISECONDS.toNanos(1);
    private static final int IPV6_NETWORK_PREFIX_LENGTH_IN_BYTES = 8;
    private static final String IPV6_NETWORK_PREFIX_SUFFIX = "/" + IPV6_NETWORK_PREFIX_LENGTH_IN_BYTES * Byte.SIZE;
    private static final byte[] NAT64_WELL_KNOWN_PREFIX = { 0, 0x64, (byte) 0xff, (byte) 0x9b, 0, 0, 0, 0, 0, 0, 0, 0 }; // RFC 6052: 64:ff9b::/96

    private static final String ERROR_MAX_TRACKED_COUNTERS = "The '%s' context param must be a positive number. Encountered an invalid value of '%s'.";

    private static final String WARNING_RETRY = "Rate limit exceeded for client ID '%s'; now retry attempt #%d ...";
    private static final String WARNING_INTERRUPTED = "Rate limit delay interrupted for client ID '%s'; continuing with retry ...";
    private static final String WARNING_UNEXPECTED_ERROR = "Unexpected error during rate limit delay for client ID '%s'.";
    private static final String WARNING_MAX_TRACKED_COUNTERS = "Rate limiter tracks the maximum of %d request counters; requests which need a new counter"
        + " are rejected until tracked ones expire. Consider raising the '%s' context param.";

    // Variables ------------------------------------------------------------------------------------------------------

    private final Map<RateLimitKey, RequestCounter> requestCountsByKey = new ConcurrentHashMap<>();
    private final AtomicLong lastExpiredCountersRemovalOnFull = new AtomicLong(NEVER);

    @Inject
    private ServletContext servletContext;

    private int maxTrackedCounters = DEFAULT_MAX_TRACKED_COUNTERS;
    private LongSupplier ticker = System::nanoTime;
    private ScheduledExecutorService executorService;
    private boolean managedExecutorService;
    private ScheduledFuture<?> expiredCountersRemoval;

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * First reads the {@value #PARAM_NAME_MAX_TRACKED_COUNTERS} context parameter, if any. Then looks up the JEE default managed scheduled executor service in
     * JNDI. If it isn't available, then create scheduled executor service ourselves with help of
     * {@link Executors#newSingleThreadScheduledExecutor(ThreadFactory)}. Finally schedule the periodic removal of expired request counters on it.
     *
     * @throws IllegalArgumentException When the {@value #PARAM_NAME_MAX_TRACKED_COUNTERS} context parameter is not a positive number.
     */
    @PostConstruct
    public void init() {
        if (servletContext != null) {
            initMaxTrackedCounters(servletContext.getInitParameter(PARAM_NAME_MAX_TRACKED_COUNTERS));
        }

        try {
            executorService = JNDI.lookup("java:comp/DefaultManagedScheduledExecutorService");
        }
        catch (Exception e) {
            // JNDI lookup failed, will create our own executor service below
        }

        if (executorService != null) {
            managedExecutorService = true;
        }
        else { // Can happen when running on non-JEE-server (e.g. Tomcat) or when it is disabled in server config for some reason.
            executorService = newSingleThreadScheduledExecutor(runnable -> {
                var thread = new Thread(runnable, THREAD_ID);
                thread.setDaemon(true);
                return thread;
            });
        }

        expiredCountersRemoval = executorService.scheduleWithFixedDelay(
            this::removeExpiredCounters, EXPIRED_COUNTERS_REMOVAL_INTERVAL_IN_SECONDS,
            EXPIRED_COUNTERS_REMOVAL_INTERVAL_IN_SECONDS, SECONDS
        );
    }

    private void initMaxTrackedCounters(String value) {
        if (value == null) {
            return;
        }

        try {
            setMaxTrackedCounters(Integer.parseInt(value.strip()));
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(ERROR_MAX_TRACKED_COUNTERS.formatted(PARAM_NAME_MAX_TRACKED_COUNTERS, value), e);
        }
    }

    /**
     * Checks if the current request exceeds the configured rate limit for the client IP address associated with the given request.
     * <p>
     * The client IP address is obtained by {@link HttpServletRequest#getRemoteAddr()}. Proxy headers such as <code>X-Forwarded-For</code> are not consulted,
     * because the client controls them. When the application runs behind a reverse proxy, configure the servlet container to resolve the client IP address from
     * the proxy headers of trusted proxies only, such as Tomcat's <code>RemoteIpValve</code>. Otherwise all clients share the IP address of the proxy.
     * <p>
     * An IPv6 address is tracked by its <code>/64</code> network prefix, because a single client usually controls a whole <code>/64</code> network. An
     * IPv4-mapped IPv6 address, or one in the NAT64 well-known prefix <code>64:ff9b::/96</code>, is tracked as its embedded IPv4 address.
     * <p>
     * This method implements a fixed window rate limiting algorithm: the time window of a counter starts at its first request, and the counter resets when the
     * time window has expired. It tracks the number of requests per IP address within the specified time window and throws an exception if the limit is
     * exceeded. Expired tracking entries are automatically cleaned up to prevent memory leaks.
     *
     * @param request The HTTP servlet request to check.
     * @param maxRequestsPerTimeWindow The maximum number of requests allowed within the time window.
     * @param timeWindow The time window duration.
     * @param maxRetries The maximum number of retries.
     * @throws RateLimitExceededException When the rate limit is exceeded for the client IP address associated with the given request.
     */
    public void checkRateLimit(HttpServletRequest request, int maxRequestsPerTimeWindow, Duration timeWindow, int maxRetries)
        throws RateLimitExceededException
    {
        checkRateLimit(null, request, maxRequestsPerTimeWindow, timeWindow, maxRetries);
    }

    /**
     * Checks if the current request exceeds the configured rate limit for the client IP address associated with the given request, within the given scope.
     * Different scopes have separate counters for the same client IP address.
     *
     * @param scope The scope of the counter, such as the rate limited method or the class which declares the annotation, or <code>null</code> for no scope.
     * @param request The HTTP servlet request to check.
     * @param maxRequestsPerTimeWindow The maximum number of requests allowed within the time window.
     * @param timeWindow The time window duration.
     * @param maxRetries The maximum number of retries.
     * @throws RateLimitExceededException When the rate limit is exceeded for the client IP address associated with the given request.
     */
    void checkRateLimit(Object scope, HttpServletRequest request, int maxRequestsPerTimeWindow, Duration timeWindow, int maxRetries)
        throws RateLimitExceededException
    {
        checkRateLimit(new RateLimitKey(scope, getClientId(request.getRemoteAddr()), maxRequestsPerTimeWindow, timeWindow.toNanos()), maxRetries);
    }

    /**
     * Returns the client identifier of the given remote address: an IPv6 address, without its zone ID, is reduced to its <code>/64</code> network prefix, an
     * IPv4-mapped IPv6 address or an IPv6 address in the NAT64 well-known prefix <code>64:ff9b::/96</code> to its embedded IPv4 address, and anything that is
     * not an IP address literal is returned unmodified.
     *
     * @param remoteAddr The remote address as obtained by {@link HttpServletRequest#getRemoteAddr()}.
     * @return The client identifier of the given remote address.
     */
    static String getClientId(String remoteAddr) {
        if (!isIPv6LiteralCandidate(remoteAddr)) {
            return remoteAddr;
        }

        try {
            var address = InetAddress.getByName(stripZoneId(remoteAddr));

            return (address instanceof Inet6Address) ? getIPv6ClientId(address.getAddress()) : address.getHostAddress();
        }
        catch (UnknownHostException e) {
            return remoteAddr;
        }
    }

    /**
     * Returns whether the given remote address can be parsed as an IPv6 address literal without querying the name service.
     *
     * @param remoteAddr The remote address as obtained by {@link HttpServletRequest#getRemoteAddr()}.
     * @return Whether the given remote address can be parsed as an IPv6 address literal without querying the name service.
     */
    static boolean isIPv6LiteralCandidate(String remoteAddr) {
        if (remoteAddr == null || remoteAddr.indexOf(':') < 0) {
            return false;
        }

        var firstChar = remoteAddr.charAt(0);
        return HexFormat.isHexDigit(firstChar) || firstChar == ':' || firstChar == '['; // InetAddress#getByName() queries the name service otherwise.
    }

    private static String stripZoneId(String remoteAddr) {
        var zoneIndex = remoteAddr.indexOf('%');

        if (zoneIndex < 0) {
            return remoteAddr;
        }

        return remoteAddr.substring(0, zoneIndex) + (remoteAddr.endsWith("]") ? "]" : "");
    }

    private static String getIPv6ClientId(byte[] bytes) throws UnknownHostException {
        var prefixLength = NAT64_WELL_KNOWN_PREFIX.length;

        if (Arrays.equals(bytes, 0, prefixLength, NAT64_WELL_KNOWN_PREFIX, 0, prefixLength)) {
            return InetAddress.getByAddress(Arrays.copyOfRange(bytes, prefixLength, bytes.length)).getHostAddress();
        }

        Arrays.fill(bytes, IPV6_NETWORK_PREFIX_LENGTH_IN_BYTES, bytes.length, (byte) 0);
        return InetAddress.getByAddress(bytes).getHostAddress() + IPV6_NETWORK_PREFIX_SUFFIX;
    }

    /**
     * Checks if the current request exceeds the configured rate limit for the given client identifier.
     * <p>
     * This method implements a fixed window rate limiting algorithm: the time window of a counter starts at its first request, and the counter resets when the
     * time window has expired. It tracks the number of requests per client identifier within the specified time window and throws an exception if the limit is
     * exceeded. Expired tracking entries are automatically cleaned up to prevent memory leaks.
     * <p>
     * When the number of tracked request counters has reached the cap set by the {@value #PARAM_NAME_MAX_TRACKED_COUNTERS} context parameter, a request which
     * needs a new counter is rejected immediately, without retries.
     *
     * @param clientId The client identifier to check, whether client IP, user ID, API key, etc.
     * @param maxRequestsPerTimeWindow The maximum number of requests allowed within the time window.
     * @param timeWindow The time window duration.
     * @param maxRetries The maximum number of retries.
     * @throws RateLimitExceededException When the rate limit is exceeded for the given client identifier.
     */
    public void checkRateLimit(String clientId, int maxRequestsPerTimeWindow, Duration timeWindow, int maxRetries) throws RateLimitExceededException {
        checkRateLimit(new RateLimitKey(null, clientId, maxRequestsPerTimeWindow, timeWindow.toNanos()), maxRetries);
    }

    private void checkRateLimit(RateLimitKey key, int maxRetries) {
        rejectNewCounterIfFull(key);
        var attempt = new AtomicInteger(0);

        while (attempt.get() <= maxRetries) {
            try {
                checkRateLimit(key);
                return;
            }
            catch (RateLimitExceededException e) {
                if (attempt.incrementAndGet() > maxRetries) {
                    throw e;
                }

                logger.log(WARNING, () -> WARNING_RETRY.formatted(key.clientId(), attempt.get()));
                executeDelay(key.clientId(), e.getRecommendedDelayInMillis());
            }
        }
    }

    private void checkRateLimit(RateLimitKey key) {
        var now = ticker.getAsLong();
        var counter = requestCountsByKey.compute(
            key,
            ($, rc) -> (rc == null || rc.isExpired(now)) ? new RequestCounter(now, key.timeWindowInNanos(), 1) : rc.increment()
        );

        if (counter.count() > key.maxRequestsPerTimeWindow()) {
            throw new RateLimitExceededException(key.clientId(), toDelayInMillis(counter.getRemainingTimeInNanos(now)));
        }
    }

    /**
     * When the given key is new and the tracked keys are at the cap, then remove expired request counters, at most once per second, and reject the given key
     * when still at the cap. Concurrent new keys may be rejected while the removal runs. Concurrent requests may exceed the cap by at most their own number.
     */
    private void rejectNewCounterIfFull(RateLimitKey key) {
        if (requestCountsByKey.containsKey(key) || requestCountsByKey.size() < maxTrackedCounters) {
            return;
        }

        var now = ticker.getAsLong();
        var lastRemoval = lastExpiredCountersRemovalOnFull.get();
        var removalDue = (lastRemoval == NEVER || now - lastRemoval >= EXPIRED_COUNTERS_REMOVAL_ON_FULL_INTERVAL_IN_NANOS)
            && lastExpiredCountersRemovalOnFull.compareAndSet(lastRemoval, now);

        if (removalDue) {
            removeExpiredCounters();
        }

        if (requestCountsByKey.size() >= maxTrackedCounters) {
            if (removalDue) {
                logger.log(WARNING, () -> WARNING_MAX_TRACKED_COUNTERS.formatted(maxTrackedCounters, PARAM_NAME_MAX_TRACKED_COUNTERS));
            }

            throw new RateLimitExceededException(
                key.clientId(), toDelayInMillis(Math.min(key.timeWindowInNanos(), SECONDS.toNanos(EXPIRED_COUNTERS_REMOVAL_INTERVAL_IN_SECONDS)))
            );
        }
    }

    private static long toDelayInMillis(long delayInNanos) {
        return Math.max(1, delayInNanos / NANOS_PER_MILLI + (delayInNanos % NANOS_PER_MILLI > 0 ? 1 : 0));
    }

    /**
     * Removes all request counters whose time window has expired.
     */
    void removeExpiredCounters() {
        var now = ticker.getAsLong();
        requestCountsByKey.values().removeIf(counter -> counter.isExpired(now));
    }

    /**
     * Returns the number of request counters currently being tracked.
     *
     * @return The number of request counters currently being tracked.
     */
    int getTrackedCounterCount() {
        return requestCountsByKey.size();
    }

    private void executeDelay(String clientId, long delayInMillis) {
        var delayFuture = new CompletableFuture<Void>();
        executorService.schedule(() -> delayFuture.complete(null), delayInMillis, MILLISECONDS);

        try {
            delayFuture.get();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.log(WARNING, () -> WARNING_INTERRUPTED.formatted(clientId));
        }
        catch (ExecutionException e) {
            logger.log(WARNING, e, () -> WARNING_UNEXPECTED_ERROR.formatted(clientId));
        }
    }

    /**
     * Cancels the periodic removal of expired request counters. If the scheduled executor service was created with help of
     * {@link Executors#newSingleThreadScheduledExecutor(ThreadFactory)}, then attempt to orderly shut down it. If it's still not shut down after 5 seconds,
     * then terminate it.
     */
    @PreDestroy
    public void destroy() {
        if (expiredCountersRemoval != null) {
            expiredCountersRemoval.cancel(false);
        }

        if (!managedExecutorService && executorService != null) {
            executorService.shutdown();

            try {
                if (!executorService.awaitTermination(5, SECONDS)) {
                    executorService.shutdownNow();
                }
            }
            catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Sets the ticker which is used to measure the time window, so that a test can advance it without waiting. It must be monotonic, as the time window is
     * measured as an elapsed duration rather than as a moment in time.
     *
     * @param ticker The ticker which is used to measure the time window, in nanoseconds.
     */
    void setTicker(LongSupplier ticker) {
        this.ticker = ticker;
    }

    /**
     * Sets the servlet context to read the context parameters from during {@link #init()}, so that a test can provide one without CDI.
     *
     * @param servletContext The servlet context to read the context parameters from.
     */
    void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    /**
     * Sets the maximum number of request counters to track.
     *
     * @param maxTrackedCounters The maximum number of request counters to track.
     * @throws IllegalArgumentException When the given maximum is not a positive number.
     */
    void setMaxTrackedCounters(int maxTrackedCounters) {
        if (maxTrackedCounters < 1) {
            throw new IllegalArgumentException(ERROR_MAX_TRACKED_COUNTERS.formatted(PARAM_NAME_MAX_TRACKED_COUNTERS, maxTrackedCounters));
        }

        this.maxTrackedCounters = maxTrackedCounters;
    }

    // Nested classes -------------------------------------------------------------------------------------------------

    /**
     * Key of a request counter. Callers with the same scope, client identifier, maximum number of requests and time window share one counter.
     */
    private record RateLimitKey(Object scope, String clientId, int maxRequestsPerTimeWindow, long timeWindowInNanos) {
    }

    /**
     * Immutable request counter, so that each caller of {@link Map#compute(Object, java.util.function.BiFunction)} gets its own snapshot.
     */
    private record RequestCounter(long starttime, long timeWindowInNanos, int count) {

        public RequestCounter increment() {
            return new RequestCounter(starttime, timeWindowInNanos, count + 1);
        }

        public boolean isExpired(long now) {
            return getRemainingTimeInNanos(now) <= 0;
        }

        public long getRemainingTimeInNanos(long now) {
            return timeWindowInNanos - (now - starttime);
        }

    }

}
