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

import org.omnifaces.cdi.RateLimit;

/**
 * Thrown when rate limit has exceeded according to {@link RateLimiter}.
 *
 * @author Bauke Scholtz
 * @since 5.0
 * @see RateLimit
 * @see RateLimiter
 * @see RateLimitExceededException
 */
public class RateLimitExceededException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private static final String ERROR_RATE_LIMIT_EXCEEDED = "Rate limit exceeded for client ID '%s'";

    private final long recommendedDelayInMillis;

    public RateLimitExceededException(String clientId, long recommendedDelayInMillis) {
        super(ERROR_RATE_LIMIT_EXCEEDED.formatted(clientId));
        this.recommendedDelayInMillis = recommendedDelayInMillis;
    }

    public long getRecommendedDelayInMillis() {
        return recommendedDelayInMillis;
    }

}
