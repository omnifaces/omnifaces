/*
 * Copyright 2016 OmniFaces
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
import static org.omnifaces.util.Reflection.toClass;

import java.lang.annotation.Annotation;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.WebListener;

import org.omnifaces.cdi.Eager;
import org.omnifaces.cdi.GraphicImageBean;
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
 * <li>Check if JSF 2.2 is available, otherwise log and fail.
 * <li>Check if CDI 1.1 is available, otherwise log and fail.
 * <li>Load {@link Cache} provider and register its filter if necessary.
 * <li>Instantiate {@link Eager} application scoed beans and register {@link EagerBeansWebListener} if necessary.
 * <li>Add {@link FacesViews} mappings to FacesServlet if necessary.
 * <li>Register {@link GraphicImageBean} beans in {@link GraphicResource}.
 * <li>Register {@link Socket} endpoint if necessary.
 * </ol>
 *
 * @author Bauke Scholtz
 * @since 2.0
 */
@WebListener
public class ApplicationListener extends DefaultServletContextListener {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final Logger logger = Logger.getLogger(ApplicationListener.class.getName());

	private static final String ERROR_JSF_API_UNAVAILABLE =
		"JSF API is not available in this environment.";
	private static final String ERROR_JSF_API_INCOMPATIBLE =
		"JSF API of this environment is not JSF 2.2 compatible.";
	private static final String ERROR_CDI_API_UNAVAILABLE =
		"CDI API is not available in this environment.";
	private static final String ERROR_CDI_API_INCOMPATIBLE =
		"CDI API of this environment is not CDI 1.1 compatible.";
	private static final String ERROR_CDI_IMPL_UNAVAILABLE =
		"CDI BeanManager instance is not available in this environment.";

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public void contextInitialized(ServletContextEvent event) {
		checkJSF22Available();
		checkCDI11Available();

		try {
			ServletContext servletContext = event.getServletContext();
			CacheInitializer.loadProviderAndRegisterFilter(servletContext);
			EagerBeansRepository.instantiateApplicationScopedAndRegisterListenerIfNecessary(servletContext);
			FacesViews.addFacesServletMappings(servletContext);
			GraphicResource.registerGraphicImageBeans();
			Socket.registerEndpointIfNecessary(servletContext);
		}
		catch (Throwable e) {
			logger.log(SEVERE, ERROR_OMNIFACES_INITIALIZATION_FAIL, e);
			throw e;
		}
	}

	private void checkJSF22Available() {
		try {
			checkJSFAPIAvailable();
			checkJSF22Compatible();
		}
		catch (Throwable e) {
			logger.severe(""
				+ "\n████████████████████████████████████████████████████████████████████████████████"
				+ "\n█░▀░░░░▀█▀░░░░░░▀█░░░░░░▀█▀░░░░░▀█                                             ▐"
				+ "\n█░░▐█▌░░█░░░██░░░█░░██░░░█░░░██░░█ OmniFaces failed to initialize!             ▐"
				+ "\n█░░▐█▌░░█░░░██░░░█░░██░░░█░░░██░░█                                             ▐"
				+ "\n█░░▐█▌░░█░░░██░░░█░░░░░░▄█░░▄▄▄▄▄█ This OmniFaces version requires JSF 2.2,    ▐"
				+ "\n█░░▐█▌░░█░░░██░░░█░░░░████░░░░░░░█ but none was found on this environment.     ▐"
				+ "\n█░░░█░░░█▄░░░░░░▄█░░░░████▄░░░░░▄█                                             ▐"
				+ "\n████████████████████████████████████████████████████████████████████████████████"
			);
			throw e;
		}
 	}

	private void checkCDI11Available() {
		try {
			checkCDIAPIAvailable();
			checkCDI11Compatible();
			checkCDIImplAvailable();
		}
		catch (Throwable e) {
			logger.severe(""
				+ "\n████████████████████████████████████████████████████████████████████████████████"
				+ "\n▌                         ▐█     ▐                                             ▐"
				+ "\n▌    ▄                  ▄█▓█▌    ▐ OmniFaces failed to initialize!             ▐"
				+ "\n▌   ▐██▄               ▄▓░░▓▓    ▐                                             ▐"
				+ "\n▌   ▐█░██▓            ▓▓░░░▓▌    ▐ This OmniFaces version requires CDI 1.1,    ▐"
				+ "\n▌   ▐█▌░▓██          █▓░░░░▓     ▐ but none was found on this environment.     ▐"
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

	// Helpers --------------------------------------------------------------------------------------------------------

	private static void checkJSFAPIAvailable() {
		try {
			toClass("javax.faces.webapp.FacesServlet");
		}
		catch (Exception | LinkageError e) {
			throw new IllegalStateException(ERROR_JSF_API_UNAVAILABLE, e);
		}
	}

	private static void checkJSF22Compatible() {
		try {
			Class<Annotation> servlet30Annotation = toClass("javax.servlet.annotation.MultipartConfig");
			toClass("javax.faces.webapp.FacesServlet").getAnnotation(servlet30Annotation).toString();
		}
		catch (Exception | LinkageError e) {
			throw new IllegalStateException(ERROR_JSF_API_INCOMPATIBLE, e);
		}
	}

	private static void checkCDIAPIAvailable() {
		try {
			toClass("javax.enterprise.inject.spi.BeanManager");
		}
		catch (Exception | LinkageError e) {
			throw new IllegalStateException(ERROR_CDI_API_UNAVAILABLE, e);
		}
	}

	private static void checkCDI11Compatible() {
		try {
			toClass("javax.enterprise.inject.spi.CDI");
		}
		catch (Exception | LinkageError e) {
			throw new IllegalStateException(ERROR_CDI_API_INCOMPATIBLE, e);
		}
	}

	private static void checkCDIImplAvailable() {
		try {
			Class<Object> cdi = toClass("javax.enterprise.inject.spi.CDI");
			Object current = cdi.getMethod("current").invoke(null);
			cdi.getMethod("getBeanManager").invoke(current).toString();
		}
		catch (Exception | LinkageError e) {
			throw new IllegalStateException(ERROR_CDI_IMPL_UNAVAILABLE, e);
		}
	}

}