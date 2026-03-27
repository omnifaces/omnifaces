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
package org.omnifaces.cdi;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;
import jakarta.servlet.http.HttpServletRequest;

import org.omnifaces.cdi.ratelimit.RateLimitExceededException;
import org.omnifaces.cdi.ratelimit.RateLimitInterceptor;
import org.omnifaces.cdi.ratelimit.RateLimiter;

/**
 * <p>
 * The CDI annotation <code>&#64;</code>{@link RateLimit} allows you to rate limit method invocations in CDI managed beans based on a configurable client
 * identifier, such as client IP address, user ID, API key, etc.
 * <p>
 * This annotation can be applied to both methods and classes. When applied to a class, all methods in that class will be rate limited unless they have their
 * own <code>&#64;</code>{@link RateLimit} annotation which overrides the class-level configuration.
 * <p>
 * The rate limiting is enforced by the {@link RateLimitInterceptor} which uses a sliding window algorithm to track request counts per client identifier. When
 * the rate limit is exceeded, the interceptor will by default immediately throw a {@link RateLimitExceededException}. Optionally, you can configure automatic
 * retries via the {@link #maxRetries()} attribute, which will retry the request after a calculated delay based on the remaining time window. If all retries are
 * exhausted, a {@link RateLimitExceededException} is thrown.
 *
 * <h2>Usage</h2>
 * <p>
 * Here's an example of rate limiting an API endpoint to 10 requests per minute per client IP:
 * 
 * <pre>
 * &#64;Named
 * &#64;RequestScoped
 * public class ApiController {
 *
 *     &#64;RateLimit(maxRequestsPerTimeWindow = 10, timeWindowInSeconds = 60, maxRetries = 0)
 *     public void processApiRequest() {
 *         // Process API request ...
 *     }
 * 
 * }
 * </pre>
 * <p>
 * Here's an example of rate limiting based on a custom client identifier:
 * 
 * <pre>
 * &#64;Named
 * &#64;RequestScoped
 * public class ApiController {
 *
 *     &#64;RateLimit(clientId = "FooAPI", maxRequestsPerTimeWindow = 5, timeWindowInSeconds = 30, maxRetries = 1)
 *     public void processFooAPIRequest() {
 *         // Process Foo API request ...
 *     }
 * 
 * }
 * </pre>
 * <p>
 * When no <code>clientId</code> is specified, the rate limiter will automatically use the client IP address from the current {@link HttpServletRequest}. If no
 * HTTP request is available in the current context, you must explicitly provide a <code>clientId</code>, otherwise an {@link IllegalArgumentException} will be
 * thrown.
 *
 * @author Bauke Scholtz
 * @since 5.0
 * @see RateLimiter
 * @see RateLimitInterceptor
 * @see RateLimitExceededException
 */
@Inherited
@InterceptorBinding
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
public @interface RateLimit {

    public static final int DEFAULT_MAX_REQUESTS_PER_TIME_WINDOW = 1;
    public static final int DEFAULT_TIME_WINDOW_IN_SECONDS = 1;
    public static final int DEFAULT_MAX_RETRIES = 0;

    /**
     * (Optional) The client identifier to check, whether client IP, user ID, API key, etc. Defaults to client IP address associated with the current
     * {@link HttpServletRequest}.
     * <p>
     * Note thus that when you cannot guarantee that a {@link HttpServletRequest} is available in the context of the annotated method, then you'll definitely
     * need to explicitly provide a client ID, otherwise the {@link RateLimitInterceptor} will throw an {@link IllegalArgumentException}.
     * 
     * @return The client identifier to check, whether client IP, user ID, API key, etc.
     */
    @Nonbinding
    String clientId() default "";

    /**
     * (Optional) The maximum number of requests allowed within the time window. Defaults to {@value #DEFAULT_MAX_REQUESTS_PER_TIME_WINDOW}.
     * 
     * @return The maximum number of requests allowed within the time window.
     */
    @Nonbinding
    int maxRequestsPerTimeWindow() default DEFAULT_MAX_REQUESTS_PER_TIME_WINDOW;

    /**
     * (Optional) The time window duration in seconds. Defaults to {@value #DEFAULT_TIME_WINDOW_IN_SECONDS}.
     * 
     * @return The time window duration in seconds.
     */
    @Nonbinding
    long timeWindowInSeconds() default DEFAULT_TIME_WINDOW_IN_SECONDS;

    /**
     * (Optional) The maximum number of retries when rate limit is exceeded. Defaults to {@value #DEFAULT_MAX_RETRIES} (no retries - fail immediately). Set to a
     * positive value to enable automatic retries with calculated delays.
     * 
     * @return The maximum number of retries.
     */
    @Nonbinding
    int maxRetries() default DEFAULT_MAX_RETRIES;

}
