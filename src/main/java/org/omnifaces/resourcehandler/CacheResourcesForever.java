/*
 * Copyright 2020 OmniFaces
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
package org.omnifaces.resourcehandler;

import org.omnifaces.util.Lazy;
import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.application.ResourceWrapper;
import org.omnifaces.util.Faces;
import static org.omnifaces.util.Utils.isBlank;

/**
 * Automatically adds version parameter to all resource URLs so, in production mode,
 * they will be cached forever (or as configured in web.xml),
 * but will not be stale when a new version of the app is deployed.
 * <p>
 * Example:
 * <pre>
 * faces-config.xml:
 * {@code
 *   <application>
 *       <resource-handler>org.omnifaces.resourcehandler.CacheResourcesForever</resource-handler>
 *   </application>
 * }
 *
 * web.xml:
 * {@code
 *   <context-param>
 *       <!-- Mojarra: 1 year cache, effects production mode only -->
 *       <param-name>com.sun.faces.defaultResourceMaxAge</param-name>
 *       <param-value>31536000000</param-value>
 *   </context-param>
 *   <context-param>
 *       <param-name>org.omnifaces.VERSION_STRING</param-name>
 *       <!-- Version string could be any string here, or taken from @Named bean -->
 *       <param-value>#{environmentInfo.version}</param-value>
 *   </context-param>
 * }
 * <a href="https://github.com/flowlogix/flowlogix/blob/master/jakarta-ee/jee-examples/src/main/java/com/flowlogix/examples/ui/EnvironmentInfo.java"
 * target="_blank">Example Code (GitHub)</a>
 * </pre>
 * @author lprimak
 */
public class CacheResourcesForever extends DefaultResourceHandler {
    private static final String VERSION_SUFFIX = "v=";
    private final Lazy<String> versionString;


    public CacheResourcesForever(ResourceHandler wrapped) {
        super(wrapped);
        versionString = new Lazy<>(() -> Faces.evaluateExpressionGet(
                Faces.getExternalContext().getInitParameter("org.omnifaces.VERSION_STRING")));
    }

    @Override
    public Resource decorateResource(Resource resource) {
        if (resource == null || isBlank(versionString.get())) {
            return resource;
        }
        String requestPath = resource.getRequestPath();
        if (requestPath.contains('&' + VERSION_SUFFIX) || requestPath.contains('?' + VERSION_SUFFIX)) {
            // ignore already-versioned resources
            return resource;
        } else {
            return new CachingWrapper(resource);
        }
    }

    private class CachingWrapper extends ResourceWrapper {
        public CachingWrapper(Resource wrapped) {
            super(wrapped);
        }

        @Override
        public String getRequestPath() {
            String requestPath = getWrapped().getRequestPath();
            if (!requestPath.contains(ResourceHandler.RESOURCE_IDENTIFIER)) {
                // do not touch CDN resources
                return requestPath;
            }

            if (requestPath.contains("?")) {
                requestPath = requestPath + '&' + VERSION_SUFFIX + versionString.get();
            } else {
                requestPath = requestPath + '?' + VERSION_SUFFIX + versionString.get();
            }

            return requestPath;
        }
    }
}
