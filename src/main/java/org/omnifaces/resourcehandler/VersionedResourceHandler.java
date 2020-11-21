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

import static org.omnifaces.util.Faces.evaluateExpressionGet;
import static org.omnifaces.util.Faces.getInitParameter;
import static org.omnifaces.util.Utils.encodeURL;
import static org.omnifaces.util.Utils.isBlank;

import org.omnifaces.util.Lazy;

import jakarta.faces.application.Resource;
import jakarta.faces.application.ResourceHandler;
import jakarta.faces.application.ResourceWrapper;

/**
 * Automatically adds version parameter with query string name <code>v</code> to all resource URLs so, in production mode
 * they will be cached forever (or as configured in web.xml),
 * but will not be stale when a new version of the app is deployed.
 * <p>
 * NOTE: if resource URL already has <code>v</code> query string parameter, or when it is URL-rewritten to not include
 * <code>{@value jakarta.faces.application.ResourceHandler#RESOURCE_IDENTIFIER}</code> path anymore, then these will be ignored.
 * <p>
 * Example:
 * <pre>
 * faces-config.xml:
 * {@code
 *   <application>
 *       <resource-handler>org.omnifaces.resourcehandler.VersionedResourceHandler</resource-handler>
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
 *       <param-name>org.omnifaces.VERSIONED_RESOURCE_HANDLER_VERSION</param-name>
 *       <!-- Version string could be any string here, or taken from @Named bean -->
 *       <param-value>#{environmentInfo.version}</param-value>
 *   </context-param>
 * }
 * </pre>
 * <p>
 * <a href="https://github.com/flowlogix/flowlogix/blob/master/jakarta-ee/jee-examples/src/main/java/com/flowlogix/examples/ui/EnvironmentInfo.java"
 * target="_blank">Example Code (GitHub)</a>
 *
 * @author Lenny Primak
 * @since 3.9
 */
public class VersionedResourceHandler extends DefaultResourceHandler {

	/** The context parameter name to specify value of the version to be appended to the resource URL. */
	public static final String PARAM_NAME_VERSION = "org.omnifaces.VERSIONED_RESOURCE_HANDLER_VERSION";

	private static final String VERSION_SUFFIX = "v=";
	private final Lazy<String> versionString;

	public VersionedResourceHandler(ResourceHandler wrapped) {
		super(wrapped);
		versionString = new Lazy<>(() -> encodeURL(evaluateExpressionGet(getInitParameter(PARAM_NAME_VERSION))));
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
			return new VersionedResource(resource);
		}
	}

	private class VersionedResource extends ResourceWrapper {
		public VersionedResource(Resource wrapped) {
			super(wrapped);
		}

		@Override
		public String getRequestPath() {
			String requestPath = getWrapped().getRequestPath();

			if (!requestPath.contains(ResourceHandler.RESOURCE_IDENTIFIER)) {
				// do not touch CDN resources
				return requestPath;
			}

			return requestPath + (requestPath.contains("?") ? '&' : '?') + VERSION_SUFFIX + versionString.get();
		}
	}
}
