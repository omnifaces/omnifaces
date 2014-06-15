/*
 * Copyright 2012 OmniFaces.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.component.output.cache;

import java.util.Map;

import javax.faces.context.FacesContext;

/**
 * A provider for a specific {@link Cache} implementation. Via this plug-in construct, the OmniFaces Cache component can
 * make use of different kinds of cache implementations.
 *
 * @since 1.1
 * @author Arjan Tijms
 *
 */
public interface CacheProvider {

	/**
	 * Gets an instance of a Cache using the configured cache provider.
	 *
	 * @param context
	 *            faces context used for resolving the given scope.
	 * @param scope
	 *            scope for which the cache should be obtained. Supported scopes are dependent on the specific caching
	 *            provider, but generally at least "session" and "application" should be supported.
	 *
	 * @return Cache instance encapsulating the cache represented by this CacheProvider
	 */
	Cache getCache(FacesContext context, String scope);

	/**
	 * Passes parameters to the cache provider implementation. This is mainly intended for configuration of things
	 * like LRU and global TTL. Settings are mainly implementation specific.
	 *
	 * @param parameters map of parameters used to configure the cache.
	 */
	void setParameters(Map<String, String> parameters);

}
