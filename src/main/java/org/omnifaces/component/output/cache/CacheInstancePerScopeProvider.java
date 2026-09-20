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
 * Base class for Cache providers where for each scope a new instance of the cache is created if for that scope no instance
 * is present yet.
 * <p>
 * This kind of cache provider is suitable for simple in-memory cache implementations, where the cache is very cheap
 * to create. This is in contrast to caches where there is typically one expensive to create instance active per JVM,
 * and where scoped caches are better expressed as nodes in a tree structure.
 *
 * @since 1.1
 * @author Arjan Tijms
 *
 */
public abstract class CacheInstancePerScopeProvider implements CacheProvider {

	public static final String DEFAULT_CACHE_PARAM_NAME = "org.omnifaces.defaultcache";

	public static final String APP_TTL_PARAM_NAME = "APPLICATION_TTL";
	public static final String SESSION_TTL_PARAM_NAME = "SESSION_TTL";

	public static final String APP_MAX_CAP_PARAM_NAME = "APPLICATION_MAX_CAPACITY";
	public static final String SESSION_MAX_CAP_PARAM_NAME = "SESSION_MAX_CAPACITY";

	private Integer appDefaultTimeToLive;
	private Integer sessionDefaultTimeToLive;

	private Integer appMaxCapacity;
	private Integer sessionMaxCapacity;

	private Map<String, String> parameters;

	@Override
	public Cache getCache(FacesContext context, String scope) {

		if ("application".equals(scope)) {
			return getAppScopeCache(context);
		} else if ("session".equals(scope)) {
			return getSessionScopeCache(context);
		}

		throw new IllegalArgumentException("Scope " + scope + " not supported by provider" + DefaultCacheProvider.class.getName());
	}

	@Override
	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;

		if (parameters.containsKey(APP_TTL_PARAM_NAME)) {
			appDefaultTimeToLive = Integer.valueOf(parameters.get(APP_TTL_PARAM_NAME));
		}
		if (parameters.containsKey(SESSION_TTL_PARAM_NAME)) {
			sessionDefaultTimeToLive = Integer.valueOf(parameters.get(SESSION_TTL_PARAM_NAME));
		}
		if (parameters.containsKey(APP_MAX_CAP_PARAM_NAME)) {
			appMaxCapacity = Integer.valueOf(parameters.get(APP_MAX_CAP_PARAM_NAME));
		}
		if (parameters.containsKey(SESSION_MAX_CAP_PARAM_NAME)) {
			sessionMaxCapacity = Integer.valueOf(parameters.get(SESSION_MAX_CAP_PARAM_NAME));
		}
	}

	public Map<String, String> getParameters() {
		return parameters;
	}

	private Cache getAppScopeCache(FacesContext context) {

		Map<String, Object> applicationMap = context.getExternalContext().getApplicationMap();
		if (!applicationMap.containsKey(DEFAULT_CACHE_PARAM_NAME)) {
			synchronized (DefaultCacheProvider.class) {
				if (!applicationMap.containsKey(DEFAULT_CACHE_PARAM_NAME)) {
					applicationMap.put(DEFAULT_CACHE_PARAM_NAME, createCache(appDefaultTimeToLive, appMaxCapacity));
				}

			}
		}

		return (Cache) applicationMap.get(DEFAULT_CACHE_PARAM_NAME);
	}

	private Cache getSessionScopeCache(FacesContext context) {

		Map<String, Object> sessionMap = context.getExternalContext().getSessionMap();
		if (!sessionMap.containsKey(DEFAULT_CACHE_PARAM_NAME)) {
			Object session = context.getExternalContext().getSession(true);
			synchronized (session) {
				if (!sessionMap.containsKey(DEFAULT_CACHE_PARAM_NAME)) {
					sessionMap.put(DEFAULT_CACHE_PARAM_NAME, createCache(sessionDefaultTimeToLive, sessionMaxCapacity));
				}
			}
		}

		return (Cache) sessionMap.get(DEFAULT_CACHE_PARAM_NAME);
	}

	protected abstract Cache createCache(Integer timeToLive, Integer maxCapacity);

}