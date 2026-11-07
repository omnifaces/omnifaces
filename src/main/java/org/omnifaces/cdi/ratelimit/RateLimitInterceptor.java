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

import static jakarta.interceptor.Interceptor.Priority.LIBRARY_BEFORE;
import static org.omnifaces.util.Beans.getReference;

import java.time.Duration;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.servlet.http.HttpServletRequest;

import org.omnifaces.cdi.RateLimit;
import org.omnifaces.util.Beans;
import org.omnifaces.util.Faces;

/**
 * <p>
 * CDI interceptor that enforces rate limiting on methods annotated with {@link RateLimit}.
 * <p>
 * This interceptor intercepts method invocations and checks if the rate limit has been exceeded. If the rate limit is exceeded, a
 * {@link RateLimitExceededException} is thrown. Otherwise, the method execution continues normally.
 * <p>
 * With an explicit client ID, the counter is shared by all methods with the same client ID and limits. Without it, the counter is per client IP address per
 * annotated method, or per class which declares a class-level annotation. The client IP address is resolved as per
 * {@link RateLimiter#checkRateLimit(HttpServletRequest, int, Duration, int)}.
 *
 * @author Bauke Scholtz
 * @since 5.0
 * @see RateLimit
 * @see RateLimiter
 * @see RateLimitExceededException
 */
@Dependent
@RateLimit
@Interceptor
@Priority(LIBRARY_BEFORE)
public class RateLimitInterceptor {

    private static final String ERROR_CLIENT_ID_REQUIRED = "Please specify clientId on @RateLimit;"
        + " the fallback of HttpServletRequest isn't available in the current context.";

    private final RateLimiter rateLimiter;

    @Inject
    public RateLimitInterceptor(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @AroundInvoke
    public Object limitRate(InvocationContext context) throws Exception {
        var method = context.getMethod();
        var rateLimit = method.getAnnotation(RateLimit.class);
        Object scope = method;

        if (rateLimit == null) {
            var annotatedClass = getRateLimitAnnotatedClass(context.getTarget().getClass());
            rateLimit = annotatedClass.getDeclaredAnnotation(RateLimit.class);
            scope = annotatedClass;
        }

        var clientId = rateLimit.clientId();
        var maxRequests = rateLimit.maxRequestsPerTimeWindow();
        var timeWindow = Duration.ofSeconds(rateLimit.timeWindowInSeconds());
        var maxRetries = rateLimit.maxRetries();

        if (clientId != null && !clientId.isBlank()) {
            rateLimiter.checkRateLimit(clientId, maxRequests, timeWindow, maxRetries);
        }
        else {
            rateLimiter.checkRateLimit(scope, getRequest(), maxRequests, timeWindow, maxRetries);
        }

        return context.proceed();
    }

    /**
     * Returns the class which declares the {@link RateLimit} annotation, so that subclasses and container generated proxy classes share its counter.
     */
    private static Class<?> getRateLimitAnnotatedClass(Class<?> beanClass) {
        var type = beanClass;

        while (type.getDeclaredAnnotation(RateLimit.class) == null) {
            type = type.getSuperclass();
        }

        return type;
    }

    private static HttpServletRequest getRequest() {
        if (Faces.hasContext()) {
            return Faces.getRequest();
        }

        if (Beans.isActive(RequestScoped.class)) {
            return getReference(HttpServletRequest.class);
        }

        throw new IllegalArgumentException(ERROR_CLIENT_ID_REQUIRED);
    }

}
