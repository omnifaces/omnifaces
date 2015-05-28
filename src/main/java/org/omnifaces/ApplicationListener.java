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

import java.util.logging.Logger;

import javax.faces.webapp.FacesServlet;
import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.WebListener;

import org.omnifaces.cdi.Eager;
import org.omnifaces.cdi.eager.EagerBeansRepository;
import org.omnifaces.component.output.Cache;
import org.omnifaces.component.output.cache.CacheInitializer;
import org.omnifaces.config.BeanManager;
import org.omnifaces.eventlistener.DefaultServletContextListener;
import org.omnifaces.facesviews.FacesViews;

/**
 * <p>
 * OmniFaces application listener. This runs when the servlet context is created and thus after the
 * {@link ApplicationInitializer} and the {@link FacesServlet}.
 * This performs the following tasks:
 * <ol>
 * <li>Check if CDI is available, otherwise log and fail.
 * <li>Add {@link FacesViews} mappings to FacesServlet.
 * <li>Load {@link Cache} provider and register its filter.
 * <li>Instantiate {@link Eager} application scoped beans.
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
		checkCDIAvailable();
		EagerBeansRepository.getInstance().instantiateApplicationScoped();
		FacesViews.addMappings(event.getServletContext());
		CacheInitializer.loadProviderAndRegisterFilter(event.getServletContext());
	}

	private void checkCDIAvailable() {
		try {
			BeanManager.INSTANCE.toString();
		}
		catch (IllegalStateException e) {
			logger.severe(""
				+ "\n████████████████████████████████████████████████████████████████████████████████"
				+ "\n▌                         ▐█     ▐                                             ▐"
				+ "\n▌    ▄                  ▄█▓█▌    ▐ OmniFaces failed to initialize!             ▐"
				+ "\n▌   ▐██▄               ▄▓░░▓▓    ▐                                             ▐"
				+ "\n▌   ▐█░██▓            ▓▓░░░▓▌    ▐ This OmniFaces version requires CDI, but    ▐"
				+ "\n▌   ▐█▌░▓██          █▓░░░░▓     ▐ none was found on this environment.         ▐"
				+ "\n▌    ▓█▌░░▓█▄███████▄███▓░▓█     ▐                                             ▐"
				+ "\n▌    ▓██▌░▓██░░░░░░░░░░▓█░▓▌     ▐ OmniFaces 2.x requires a minimum of JSF 2.2.▐"
				+ "\n▌     ▓█████░░░░░░░░░░░░▓██      ▐ Since this JSF version, the JSF managed bean▐"
				+ "\n▌     ▓██▓░░░░░░░░░░░░░░░▓█      ▐ facility @ManagedBean is semi-official      ▐"
				+ "\n▌     ▐█▓░░░░░░█▓░░▓█░░░░▓█▌     ▐ deprecated in favour of CDI. JSF 2.2 users  ▐"
				+ "\n▌     ▓█▌░▓█▓▓██▓░█▓▓▓▓▓░▓█▌     ▐ are strongly encouraged to move to CDI.     ▐"
				+ "\n▌     ▓▓░▓██████▓░▓███▓▓▌░█▓     ▐                                             ▐"
				+ "\n▌    ▐▓▓░█▄▐▓▌█▓░░▓█▐▓▌▄▓░██     ▐ OmniFaces goes a step further by making CDI ▐"
				+ "\n▌    ▓█▓░▓█▄▄▄█▓░░▓█▄▄▄█▓░██▌    ▐ a REQUIRED dependency next to JSF 2.2. This ▐"
				+ "\n▌    ▓█▌░▓█████▓░░░▓███▓▀░▓█▓    ▐ not only ensures that your web application  ▐"
				+ "\n▌   ▐▓█░░░▀▓██▀░░░░░ ▀▓▀░░▓█▓    ▐ represents the state of art, but this also  ▐"
				+ "\n▌   ▓██░░░░░░░░▀▄▄▄▄▀░░░░░░▓▓    ▐ makes for us easier to develop OmniFaces,   ▐"
				+ "\n▌   ▓█▌░░░░░░░░░░▐▌░░░░░░░░▓▓▌   ▐ without the need for all sorts of hacks in  ▐"
				+ "\n▌   ▓█░░░░░░░░░▄▀▀▀▀▄░░░░░░░█▓   ▐ in order to get OmniFaces to deploy on      ▐"
				+ "\n▌  ▐█▌░░░░░░░░▀░░░░░░▀░░░░░░█▓▌  ▐ environments without CDI.                   ▐"
				+ "\n▌  ▓█░░░░░░░░░░░░░░░░░░░░░░░██▓  ▐                                             ▐"
				+ "\n▌  ▓█░░░░░░░░░░░░░░░░░░░░░░░▓█▓  ▐ You have 3 options:                         ▐"
				+ "\n██████████████████████████████████ 1. Downgrade to OmniFaces 1.x.              ▐"
				+ "\n█░▀░░░░▀█▀░░░░░░▀█░░░░░░▀█▀░░░░░▀█ 2. Install CDI in this environment.         ▐"
				+ "\n█░░▐█▌░░█░░░██░░░█░░██░░░█░░░██░░█ 3. Switch to a CDI capable environment.     ▐"
				+ "\n█░░▐█▌░░█░░░██░░░█░░██░░░█░░░██░░█                                             ▐"
				+ "\n█░░▐█▌░░█░░░██░░░█░░░░░░▄█░░▄▄▄▄▄█ For additional instructions, check          ▐"
				+ "\n█░░▐█▌░░█░░░██░░░█░░░░████░░░░░░░█ http://omnifaces.org/cdi                    ▐"
				+ "\n█░░░█░░░█▄░░░░░░▄█░░░░████▄░░░░░▄█                                             ▐"
				+ "\n████████████████████████████████████████████████████████████████████████████████"
			);

			throw e;
		}
	}

}