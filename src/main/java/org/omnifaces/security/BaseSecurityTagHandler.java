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
package org.omnifaces.security;

import static java.util.Optional.ofNullable;
import static java.util.logging.Logger.getLogger;

import java.util.Optional;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.faces.view.facelets.TagConfig;
import jakarta.faces.view.facelets.TagHandler;
import jakarta.security.enterprise.SecurityContext;

/**
 * Base class for security-related tag handlers. Provides common functionality for retrieving and validating SecurityContext.
 *
 * @author Bauke Scholtz
 * @since 5.0
 */
abstract class BaseSecurityTagHandler extends TagHandler {

    protected BaseSecurityTagHandler(TagConfig config) {
        super(config);
    }

    /**
     * Retrieves the current SecurityContext from CDI. If the SecurityContext is not available, logs a warning and returns empty Optional.
     *
     * @return Optional containing the SecurityContext, or empty if not available
     */
    protected Optional<SecurityContext> getSecurityContext() {
        var identity = CDI.current().select(SecurityContext.class).get();

        if (identity == null) {
            getLogger(getClass().getName()).warning("SecurityContext is not available. Make sure jakarta.security.enterprise is properly configured.");
        }

        return ofNullable(identity);
    }

}
