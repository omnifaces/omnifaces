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

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

/**
 * Factory used to obtain {@link Cache} instance and to register the {@link CacheProvider} that is used to obtain that.
 *
 * @since 1.1
 * @author Arjan Tijms
 *
 */
public final class CacheFactory {

	public static final String CACHE_PROVIDER_PARAM_NAME = "org.omnifaces.cacheprovider";
	private static final CacheProvider DEFAULT_PROVIDER = new DefaultCacheProvider();

	private CacheFactory() {
	}

	/**
	 * Gets an instance of a Cache using the configured cache provider.
	 *
	 * @param context
	 *            faces context used for retrieving the cache provider and for resolving the given scope.
	 * @param scope
	 *            scope for which the cache should be obtained. Supported scopes are dependent on the specific caching
	 *            provider, but generally at least "session" and "application" should be supported.
	 *
	 * @return a cache provider specific Cache instance
	 */
	public static Cache getCache(FacesContext context, String scope) {
		return getCacheProvider(context).getCache(context, scope);
	}

	/**
	 * Gets the cache provider as it has been set in the ServletContext. Does NOT return the default cache provider if
	 * none is present.
	 *
	 * @param servletContext
	 *            the servlet context where the cache provider is retrieved from
	 * @return the previously set provider if one is set, null otherwise
	 */
	public static CacheProvider getCacheProvider(ServletContext servletContext) {
		return (CacheProvider) servletContext.getAttribute(CACHE_PROVIDER_PARAM_NAME);
	}

	public static void setCacheProvider(CacheProvider cacheProvider, ServletContext servletContext) {
		servletContext.setAttribute(CACHE_PROVIDER_PARAM_NAME, cacheProvider);
	}

	/**
	 * Gets the cache provider that has been set, or the default provider if none is present.
	 *
	 * @param context
	 *            the faces context where the cache provider is retrieved from
	 * @return the previously set provider if one is set, otherwise the default provider
	 */
	public static CacheProvider getCacheProvider(FacesContext context) {
		CacheProvider provider = (CacheProvider) context.getExternalContext().getApplicationMap().get(CACHE_PROVIDER_PARAM_NAME);
		return provider != null ? provider : DEFAULT_PROVIDER;
	}

	/**
	 * Returns an instance of the default cache provider. This is the provider that is used in
	 * {@link CacheFactory#getCache(FacesContext, String)} and {@link CacheFactory#getCacheProvider(FacesContext)} if no
	 * explicit provider has been set.
	 *
	 * @return the default cache provider
	 */
	public static CacheProvider getDefaultCacheProvider() {
		return DEFAULT_PROVIDER;
	}

}
