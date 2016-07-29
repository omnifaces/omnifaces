/*
 * Copyright 2014 OmniFaces.
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
package org.omnifaces;

import static java.util.logging.Level.SEVERE;
import static org.omnifaces.ApplicationInitializer.ERROR_OMNIFACES_INITIALIZATION_FAIL;

import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.WebListener;

import org.omnifaces.cdi.Eager;
import org.omnifaces.cdi.GraphicImageScoped;
import org.omnifaces.cdi.eager.EagerBeansPhaseListener;
import org.omnifaces.cdi.eager.EagerBeansRepository;
import org.omnifaces.cdi.eager.EagerBeansWebListener;
import org.omnifaces.cdi.push.Socket;
import org.omnifaces.component.output.Cache;
import org.omnifaces.component.output.cache.CacheInitializer;
import org.omnifaces.eventlistener.DefaultServletContextListener;
import org.omnifaces.facesviews.FacesViews;
import org.omnifaces.resourcehandler.GraphicResource;

/**
 * <p>
 * OmniFaces application listener. This runs when the servlet context is created and thus after the
 * {@link ApplicationInitializer}.
 * This performs the following tasks:
 * <ol>
 * <li>Instantiate {@link Eager} application scoped beans and register its {@link EagerBeansWebListener} and/or
 * {@link EagerBeansPhaseListener} if necessary.
 * <li>Add {@link FacesViews} mappings to FacesServlet if necessary.
 * <li>Load {@link Cache} provider and register its filter if necessary.
 * <li>Register {@link Socket} endpoint if necessary.
 * <li>Register {@link GraphicImageScoped} beans in {@link GraphicResource}.
 * </ol>
 *
 * @author Bauke Scholtz
 * @since 2.0
 */
@WebListener
public class ApplicationListener extends DefaultServletContextListener {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final Logger logger = Logger.getLogger(ApplicationListener.class.getName());

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public void contextInitialized(ServletContextEvent event) {
		ServletContext servletContext = event.getServletContext();

		try {
			EagerBeansRepository.instantiateApplicationScopedAndRegisterListeners(servletContext);
			FacesViews.addMappings(servletContext);
			CacheInitializer.loadProviderAndRegisterFilter(servletContext);
			Socket.registerEndpointIfNecessary(servletContext);
			GraphicResource.registerGraphicImageScopedBeans();
		}
		catch (Throwable e) {
			logger.log(SEVERE, ERROR_OMNIFACES_INITIALIZATION_FAIL, e);
			throw e;
		}
	}

}