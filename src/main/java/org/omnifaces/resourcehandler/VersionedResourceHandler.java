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
package org.omnifaces.resourcehandler;

import static java.util.Optional.ofNullable;
import static org.omnifaces.util.Faces.evaluateExpressionGet;
import static org.omnifaces.util.Faces.getInitParameter;
import static org.omnifaces.util.Utils.isBlank;

import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;

import org.omnifaces.util.Lazy;
import org.omnifaces.util.Utils;

/**
 * Automatically adds version parameter with query string name <code>v</code> to all resource URLs so that the browser
 * cache will be busted whenever the version parameter changes.
 * <p>
 * NOTE: if resource URL already has <code>v</code> query string parameter, or when it is URL-rewritten to not include
 * <code>{@value javax.faces.application.ResourceHandler#RESOURCE_IDENTIFIER}</code> path anymore, then these will be ignored.
 * <h3>Installation</h3>
 * <p>
 * To get it to run, this handler needs be registered as follows in <code>faces-config.xml</code>:
 * <pre>
 * {@code
 * <application>
 *     <resource-handler>org.omnifaces.resourcehandler.VersionedResourceHandler</resource-handler>
 * </application>
 * }
 * </pre>
 * <p>
 * And the version parameter needs to be configured as follows in <code>web.xml</code>:
 * <pre>
 * {@code
 * <context-param>
 *     <param-name>org.omnifaces.VERSIONED_RESOURCE_HANDLER_VERSION</param-name>
 *     <!-- Version parameter value could be any hardcoded string here, or any object property from managed bean -->
 *     <param-value>#{environmentInfo.version}</param-value>
 * </context-param>
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

	private static final String XHTML_EXTENSION = ".xhtml";
	private static final String VERSION_SUFFIX = "v=";
	private final Lazy<String> versionString;

	public VersionedResourceHandler(ResourceHandler wrapped) {
		super(wrapped);
		versionString = new Lazy<>(() -> ofNullable(evaluateExpressionGet(getInitParameter(PARAM_NAME_VERSION))).map(String::valueOf).map(Utils::encodeURL).orElse(null));
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
		}
		else if (!requestPath.contains(ResourceHandler.RESOURCE_IDENTIFIER)) {
			// do not touch CDN resources
			return resource;
		}
		else if (resource.getResourceName().endsWith(XHTML_EXTENSION)) {
			// do not touch XHTML resources
			return resource;
		}
		else {
			requestPath += (requestPath.contains("?") ? '&' : '?') + VERSION_SUFFIX + versionString.get();
			return new RemappedResource(resource, requestPath);
		}
	}

}
