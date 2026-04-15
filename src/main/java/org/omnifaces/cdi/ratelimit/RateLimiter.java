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

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.logging.Level.WARNING;
import static org.omnifaces.util.Servlets.getRemoteAddr;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.servlet.http.HttpServletRequest;

import org.omnifaces.cdi.RateLimit;
import org.omnifaces.util.JNDI;

/**
 * <p>
 * CDI managed bean for rate limiting requests per client identifier. This utility provides a thread-safe sliding window rate limiting mechanism with
 * configurable request limits and time windows.
 * <p>
 * The rate limiter supports both HTTP request-based rate limiting (using client IP addresses) and custom client identifier-based rate limiting (using any
 * string identifier such as user IDs, API keys, etc.). It uses a {@link ConcurrentHashMap} to track request counts per client identifier and automatically
 * cleans up expired entries to prevent memory leaks.
 * <p>
 * When the rate limit is exceeded, the rate limiter will by default immediately throw a {@link RateLimitExceededException}. Optionally, you can configure
 * automatic retries via the {@code maxRetries} parameter, which will retry the request after a calculated delay based on the remaining time window. If all
 * retries are exhausted, a {@link RateLimitExceededException} is thrown.
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

    private static final Logger logger = Logger.getLogger(RateLimiter.class.getName());

    private static final String THREAD_ID = "omnifaces.RateLimiter.executorService";

    private static final String WARNING_RETRY = "Rate limit exceeded for client ID '%s'; now retry attempt #%d ...";
    private static final String WARNING_INTERRUPTED = "Rate limit delay interrupted for client ID '%s'; continuing with retry ...";
    private static final String WARNING_UNEXPECTED_ERROR = "Unexpected error during rate limit delay for client ID '%s'.";

    // Variables ------------------------------------------------------------------------------------------------------

    private final Map<String, RequestCounter> requestCountsByClientId = new ConcurrentHashMap<>();
    private ScheduledExecutorService executorService;
    private boolean managedExecutorService;

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * First looks up the JEE default managed scheduled executor service in JNDI. If it isn't available, then create scheduled executor service ourselves with
     * help of {@link Executors#newSingleThreadExecutor()}.
     */
    @PostConstruct
    public void init() {
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
    }

    /**
     * Checks if the current request exceeds the configured rate limit for the client IP address associated with the given request.
     * <p>
     * This method implements a sliding window rate limiting algorithm. It tracks the number of requests per IP address within the specified time window and
     * throws an exception if the limit is exceeded. Expired tracking entries are automatically cleaned up to prevent memory leaks.
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
        checkRateLimit(getRemoteAddr(request), maxRequestsPerTimeWindow, timeWindow, maxRetries);
    }

    /**
     * Checks if the current request exceeds the configured rate limit for the given client identifier.
     * <p>
     * This method implements a sliding window rate limiting algorithm. It tracks the number of requests per client identifier within the specified time window
     * and throws an exception if the limit is exceeded. Expired tracking entries are automatically cleaned up to prevent memory leaks.
     *
     * @param clientId The client identifier to check, whether client IP, user ID, API key, etc.
     * @param maxRequestsPerTimeWindow The maximum number of requests allowed within the time window.
     * @param timeWindow The time window duration.
     * @param maxRetries The maximum number of retries.
     * @throws RateLimitExceededException When the rate limit is exceeded for the given client identifier.
     */
    public void checkRateLimit(String clientId, int maxRequestsPerTimeWindow, Duration timeWindow, int maxRetries) throws RateLimitExceededException {
        var attempt = new AtomicInteger(0);

        while (attempt.get() <= maxRetries) {
            try {
                checkRateLimit(clientId, maxRequestsPerTimeWindow, timeWindow);
                return;
            }
            catch (RateLimitExceededException e) {
                if (attempt.incrementAndGet() > maxRetries) {
                    throw e;
                }

                logger.log(WARNING, WARNING_RETRY.formatted(clientId, attempt.get()));
                executeDelay(clientId, e.getRecommendedDelayInMillis());
            }
        }
    }

    private void checkRateLimit(String clientId, int maxRequestsPerTimeWindow, Duration timeWindow) {
        var now = currentTimeMillis();
        var timeWindowInMillis = timeWindow.toMillis();
        requestCountsByClientId.entrySet().removeIf(entry -> now - entry.getValue().starttime >= timeWindowInMillis);
        var counter = requestCountsByClientId.compute(clientId, ($, rc) -> (rc == null) ? new RequestCounter() : rc.increment());

        if (counter.count > maxRequestsPerTimeWindow && now - counter.starttime < timeWindowInMillis) {
            var elapsedTimeInMillis = now - counter.starttime;

            if (elapsedTimeInMillis < timeWindowInMillis) {
                throw new RateLimitExceededException(clientId, timeWindowInMillis - elapsedTimeInMillis);
            }
        }
    }

    private void executeDelay(String clientId, long delayInMillis) {
        var delayFuture = new CompletableFuture<Void>();
        executorService.schedule(() -> delayFuture.complete(null), delayInMillis, MILLISECONDS);

        try {
            delayFuture.get();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.log(WARNING, WARNING_INTERRUPTED.formatted(clientId));
        }
        catch (ExecutionException e) {
            logger.log(WARNING, WARNING_UNEXPECTED_ERROR.formatted(clientId), e);
        }
    }

    /**
     * If the scheduled executor service was created with help of {@link Executors#newSingleThreadExecutor()}, then attempt to orderly shut down it. If it's
     * still not shut down after 5 seconds, then terminate it.
     */
    @PreDestroy
    public void destroy() {
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

    // Nested classes -------------------------------------------------------------------------------------------------

    /**
     * Convenience class for a request counter.
     */
    private static class RequestCounter {

        long starttime;
        int count;

        public RequestCounter() {
            this.starttime = currentTimeMillis();
            this.count = 1;
        }

        public RequestCounter increment() {
            count++;
            return this;
        }

    }

}
