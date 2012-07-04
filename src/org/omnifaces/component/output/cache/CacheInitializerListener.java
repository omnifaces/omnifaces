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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Optional initializer for the {@link Cache} used by the Omnifaces Cache component.
 * <p>
 * It takes a configuration from web.xml context parameters and used that to set a cache provider and/or configure the
 * caching provider. If no initialization is done defaults are used.
 * 
 * @author Arjan Tijms
 * 
 */
@WebListener
public class CacheInitializerListener implements ServletContextListener {

	// Web context parameter to set the cache provider implementation
	public static final String CACHE_PROVIDER_INIT_PARAM_NAME = "org.omnifaces.CACHE_PROVIDER";

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		String cacheProviderName = sce.getServletContext().getInitParameter(CACHE_PROVIDER_INIT_PARAM_NAME);
		if (cacheProviderName != null) {
			CacheFactory.setCacheProvider(createInstance(cacheProviderName), sce.getServletContext());
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		// NOOP for now, give cache destroy command later
	}

	private CacheProvider createInstance(String cacheProviderName) {
		try {
			return (CacheProvider) Class.forName(cacheProviderName).newInstance();
		} catch (InstantiationException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(e);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException(e);
		}
	}
}