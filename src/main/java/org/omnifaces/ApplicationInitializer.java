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

import java.util.Set;
import java.util.logging.Logger;

import javax.faces.webapp.FacesServlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;

import org.omnifaces.config.BeanManager;
import org.omnifaces.facesviews.FacesViews;

/**
 * <p>
 * OmniFaces application initializer. This runs when the servlet container starts up.
 * This performs the following tasks:
 * <ol>
 * <li>Log the OmniFaces version.
 * <li>Check if JSF 2.2 is available, otherwise log and fail.
 * <li>Check if CDI is available, otherwise log and fail.
 * <li>Register {@link FacesViews} filter.
 * </ol>
 *
 * @author Bauke Scholtz
 * @since 2.0
 */
public class ApplicationInitializer implements ServletContainerInitializer {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final Logger logger = Logger.getLogger(ApplicationInitializer.class.getName());

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public void onStartup(Set<Class<?>> c, ServletContext servletContext) throws ServletException {
		logOmniFacesVersion();
		checkJSF22Available(); // Already implies Servlet 3.0 (and EL 2.2).
		checkCDIAvailable();
		FacesViews.registerFilter(servletContext);
	}

	private void logOmniFacesVersion() {
		logger.info("Using OmniFaces version " + getClass().getPackage().getSpecificationVersion());
	}

	private void checkJSF22Available() throws ServletException {
		try {
			FacesServlet.class.getAnnotation(MultipartConfig.class).toString();
		}
		catch (Throwable e) {
			logger.severe(""
				+ "\n████████████████████████████████████████████████████████████████████████████████"
				+ "\n█░▀░░░░▀█▀░░░░░░▀█░░░░░░▀█▀░░░░░▀█                                             ▐"
				+ "\n█░░▐█▌░░█░░░██░░░█░░██░░░█░░░██░░█ OmniFaces failed to initialize!             ▐"
				+ "\n█░░▐█▌░░█░░░██░░░█░░██░░░█░░░██░░█                                             ▐"
				+ "\n█░░▐█▌░░█░░░██░░░█░░░░░░▄█░░▄▄▄▄▄█ This OmniFaces version requires JSF 2.2, but▐"
				+ "\n█░░▐█▌░░█░░░██░░░█░░░░████░░░░░░░█ none was found on this environment.         ▐"
				+ "\n█░░░█░░░█▄░░░░░░▄█░░░░████▄░░░░░▄█                                             ▐"
				+ "\n████████████████████████████████████████████████████████████████████████████████"
			);
			throw new ServletException(e);
		}
	}

	private void checkCDIAvailable() throws ServletException {
		try {
			BeanManager.INSTANCE.toString();
		}
		catch (Throwable e) {
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

			throw new ServletException(e);
		}
	}

}