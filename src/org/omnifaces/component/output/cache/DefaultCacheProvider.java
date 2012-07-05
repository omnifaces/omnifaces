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
 * A default cache provider that will be used by the OmniFaces Cache component if no explicit provider has been
 * configured.
 * 
 * @since 1.1
 * @author Arjan Tijms
 * 
 */
public class DefaultCacheProvider implements CacheProvider {

	public static final String DEFAULT_CACHE_PARAM_NAME = "org.omnifaces.defaultcache";

	@Override
	public Cache getCache(FacesContext context, String scope) {

		if ("application".equals(scope)) {
			return getAppScopeCache(context);
		} else if ("session".equals(scope)) {
			return getSessionScopeCache(context);
		}

		throw new IllegalArgumentException("Scope " + scope + " not supported by provider" + DefaultCacheProvider.class.getName());
	}

	private Cache getAppScopeCache(FacesContext context) {

		Map<String, Object> applicationMap = context.getExternalContext().getApplicationMap();
		if (!applicationMap.containsKey("DEFAULT_CACHE_PARAM_NAME")) {
			synchronized (DefaultCacheProvider.class) {
				if (!applicationMap.containsKey("DEFAULT_CACHE_PARAM_NAME")) {
					applicationMap.put("DEFAULT_CACHE_PARAM_NAME", new DefaultCache());
				}

			}
		}

		return (Cache) applicationMap.get("DEFAULT_CACHE_PARAM_NAME");
	}

	private Cache getSessionScopeCache(FacesContext context) {

		Map<String, Object> sessionMap = context.getExternalContext().getSessionMap();
		if (!sessionMap.containsKey("DEFAULT_CACHE_PARAM_NAME")) {
			Object session = context.getExternalContext().getSession(true);
			synchronized (session) {
				if (!sessionMap.containsKey("DEFAULT_CACHE_PARAM_NAME")) {
					sessionMap.put("DEFAULT_CACHE_PARAM_NAME", new DefaultCache());
				}
			}
		}

		return (Cache) sessionMap.get("DEFAULT_CACHE_PARAM_NAME");
	}

}
