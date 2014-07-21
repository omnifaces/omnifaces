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

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.omnifaces.facesviews.FacesViews;
import org.omnifaces.util.Faces;

/**
 * <p>OmniFaces application initializer. So far, this performs the following tasks:
 * <ol>
 * <li>Log the OmniFaces version.
 * <li>Check if CDI is enabled, otherwise log and fail.
 * <li>Initialize FacesViews.
 * </ol>
 *
 * @author Bauke Scholtz
 * @since 2.0
 */
public class ApplicationInitializer implements ServletContainerInitializer {

	private static final Logger logger = Logger.getLogger(ApplicationInitializer.class.getName());

	@Override
	public void onStartup(Set<Class<?>> c, ServletContext servletContext) throws ServletException {
		logOmniFacesVersion();
		checkCDIEnabled(servletContext);
		FacesViews.initilaize(servletContext);
	}

	private static void logOmniFacesVersion() {
		logger.info("Using OmniFaces version " + Faces.class.getPackage().getSpecificationVersion());
	}

	private static void checkCDIEnabled(ServletContext servletContext) throws ServletException {
		try {
			try {
				Class.forName("javax.enterprise.inject.spi.BeanManager");
			}
			catch (ClassNotFoundException e) {
				throw new IllegalStateException("CDI BeanManager API is missing in classpath.");
			}
			if (servletContext.getResource("/WEB-INF/beans.xml") == null) {
				throw new IllegalStateException("CDI beans.xml file is missing in /WEB-INF.");
			}
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