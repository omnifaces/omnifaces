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

import static org.omnifaces.util.Faces.getInitParameterOrDefault;
import static org.omnifaces.util.Utils.endsWithOneOf;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.application.ResourceWrapper;

/**
 * <p>
 * This {@link ResourceHandler} implementation will set the <code>SourceMap</code> response header with the correctly
 * mapped request path to any discovered source map of any CSS and JS resource.
 * <p>
 * By default, CSS and JS minifiers will embed the path to the source map in a comment like as the below one in our own
 * <code>omnifaces.js</code>.
 * <pre>
 * //# sourceMappingURL=omnifaces.js.map
 * </pre>
 * <p>
 * The web browser will then attempt to resolve this against the current request URL, but this would fail with a 404
 * error because the JSF mapping such as <code>*.xhtml</code> is missing.
 * <p>
 * In order to sovle that, first configure your minifier to disable writing the <code># sourceMappingURL</code> comment,
 * otherwise that would still take precedence over the <code>SourceMap</code> response header, and register the
 * {@link SourceMapResourceHandler} in <code>faces-config.xml</code> as below.
 * <pre>
 * &lt;application&gt;
 *     &lt;resource-handler&gt;org.omnifaces.resourcehandler.SourceMapResourceHandler&lt;/resource-handler&gt;
 * &lt;/application&gt;
 * </pre>
 * <p>
 * By default, the {@link SourceMapResourceHandler} will use <code>*.map</code> pattern to create the source map URL.
 * In other words, it's expected that the source map file is located in exactly the same folder and has the <code>.map</code>
 * extension. In case you need a different pattern, e.g. <code>sourcemaps/*.map</code>, then you can set that via the
 * {@value org.omnifaces.resourcehandler.SourceMapResourceHandler#PARAM_NAME_SOURCE_MAP_PATTERN} context parameter.
 * <pre>
 * &lt;context-param&gt;
 *     &lt;param-name&gt;org.omnifaces.SOURCE_MAP_RESOURCE_HANDLER_PATTERN&lt;/param-name&gt;
 *     &lt;param-value&gt;sourcemaps/*.map&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * </pre>
 * <p>
 * Note that the <code>SourceMap</code> response header will only be set when the target source map file actually exists.
 *
 * @author Bauke Scholtz
 * @since 3.1
 */
public class SourceMapResourceHandler extends DefaultResourceHandler {

	/** The context parameter name to configure the source map pattern. */
	public static final String PARAM_NAME_SOURCE_MAP_PATTERN =
		"org.omnifaces.SOURCE_MAP_RESOURCE_HANDLER_PATTERN";

	private static final Map<ResourceIdentifier, String> SOURCE_MAPS = new ConcurrentHashMap<>();

	private static final String DEFAULT_SOURCE_MAP_PATTERN = "*.map";
	private static final String EXTENSION_JS = ".js";
	private static final String EXTENSION_CSS = ".css";
	private static final String HEADER_SOURCE_MAP = "SourceMap";

	private String sourceMapPattern;

	public SourceMapResourceHandler(ResourceHandler wrapped) {
		super(wrapped);
		sourceMapPattern = getInitParameterOrDefault(PARAM_NAME_SOURCE_MAP_PATTERN, DEFAULT_SOURCE_MAP_PATTERN);
	}

	@Override
	public Resource decorateResource(Resource resource, String resourceName, String libraryName) {
		if (resource == null) {
			return null;
		}

		String sourceMap = SOURCE_MAPS.computeIfAbsent(new ResourceIdentifier(libraryName, resourceName), this::computeSourceMap);

		return super.decorateResource(sourceMap.isEmpty() ? resource : new ResourceWrapper(resource) {
			@Override
			public Map<String, String> getResponseHeaders() {
				Map<String, String> responseHeaders = super.getResponseHeaders();
				responseHeaders.put(HEADER_SOURCE_MAP, sourceMap);
				return responseHeaders;
			}
		}, resourceName, libraryName);
	}

	private String computeSourceMap(ResourceIdentifier resourceIdentifier) {
		if (endsWithOneOf(resourceIdentifier.getName(), EXTENSION_JS, EXTENSION_CSS)) {
			Resource sourceMapResource = createResource(sourceMapPattern.replace("*", resourceIdentifier.getName()), resourceIdentifier.getLibrary());

			if (sourceMapResource != null) {
				return sourceMapResource.getRequestPath();
			}
		}

		return "";
	}

}