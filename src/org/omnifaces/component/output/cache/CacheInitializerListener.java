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

import static java.util.Collections.list;
import static org.omnifaces.facesviews.FacesViewsUtils.*;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;

import org.omnifaces.filter.OnDemandResponseBufferFilter;

/**
 * Optional initializer for the {@link Cache} used by the Omnifaces Cache component.
 * <p>
 * It takes a configuration from web.xml context parameters and used that to set a cache provider and/or configure the
 * caching provider. If no initialization is done defaults are used.
 * 
 * @since 1.1
 * @author Arjan Tijms
 * 
 */
@WebListener
public class CacheInitializerListener implements ServletContextListener {

	// Web context parameter to set the cache provider implementation
	public static final String CACHE_PROVIDER_INIT_PARAM_NAME = "org.omnifaces.CACHE_PROVIDER";
	public static final String CACHE_INSTALL_BUFFER_FILTER = "org.omnifaces.CACHE_INSTALL_BUFFER_FILTER";
	
	public static final String CACHE_PROVIDER_SETTING_INIT_PARAM_PREFIX = "org.omnifaces.CACHE_SETTING_";

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		
		ServletContext context = sce.getServletContext();
		
		// Check for a user configured custom cache provider, or get default one
		CacheProvider cacheProvider = getCacheProvider(context);
		
		// Build a map of settings for either the custom- or the default cache provider and set them.
		cacheProvider.setParameters(getCacheSetting(context));
		
		// Installs a filter that on demands buffers the response from the Faces Servlet, in order to grab child content
		// from the buffer.
		if (Boolean.TRUE.equals(Boolean.valueOf(context.getInitParameter(CACHE_INSTALL_BUFFER_FILTER)))) {
			ServletRegistration facesServletRegistration = getFacesServletRegistration(context);
			FilterRegistration bufferFilterRegistration = context.addFilter(OnDemandResponseBufferFilter.class.getName(), OnDemandResponseBufferFilter.class);
			bufferFilterRegistration.addMappingForServletNames(null, true, facesServletRegistration.getName());
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		// NOOP for now, give cache destroy command later
	}
	
	private CacheProvider getCacheProvider(ServletContext context) {
		CacheProvider cacheProvider = null;
		
		String cacheProviderName = context.getInitParameter(CACHE_PROVIDER_INIT_PARAM_NAME);
		if (cacheProviderName != null) {
			cacheProvider = createInstance(cacheProviderName);
			CacheFactory.setCacheProvider(cacheProvider, context);
		} else {
			cacheProvider = CacheFactory.getDefaultCacheProvider();
		}
		
		return cacheProvider;
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
	
	private Map<String, String> getCacheSetting(ServletContext context) {
		Map<String, String> settings = new HashMap<String, String>();

		for (String initParameterName : list(context.getInitParameterNames())) {
			if (initParameterName.startsWith(CACHE_PROVIDER_SETTING_INIT_PARAM_PREFIX)) {
				settings.put(
					initParameterName.substring(CACHE_PROVIDER_SETTING_INIT_PARAM_PREFIX.length()),
					context.getInitParameter(initParameterName)
				);
			}
		}
		
		return settings;
	}
}