/*
 * Copyright OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces;

import static java.util.logging.Level.WARNING;
import static org.omnifaces.ApplicationInitializer.ERROR_OMNIFACES_INITIALIZATION_FAIL;
import static org.omnifaces.ApplicationInitializer.WARNING_OMNIFACES_INITIALIZATION_FAIL;
import static org.omnifaces.util.Reflection.toClass;

import java.util.logging.Logger;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.ExternalContextWrapper;
import jakarta.faces.context.FacesContextWrapper;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.annotation.WebListener;

import org.omnifaces.cdi.Eager;
import org.omnifaces.cdi.GraphicImageBean;
import org.omnifaces.cdi.eager.EagerBeansRepository;
import org.omnifaces.cdi.eager.EagerBeansWebListener;
import org.omnifaces.cdi.push.Socket;
import org.omnifaces.cdi.push.Sse;
import org.omnifaces.component.output.Cache;
import org.omnifaces.component.script.ScriptErrorHandler;
import org.omnifaces.config.OmniFaces;
import org.omnifaces.eventlistener.DefaultServletContextListener;
import org.omnifaces.exceptionhandler.FullAjaxExceptionHandler;
import org.omnifaces.facesviews.FacesViews;
import org.omnifaces.filter.FacesExceptionFilter;
import org.omnifaces.resourcehandler.GraphicResource;
import org.omnifaces.resourcehandler.ViewResourceHandler;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Servlets;
import org.omnifaces.util.cache.CacheInitializer;

/**
 * <p>
 * OmniFaces application listener. This runs when the servlet context is created. This performs the following tasks:
 * <ol>
 * <li>Check if Faces 3.0 is available, otherwise log and fail.
 * <li>Check if CDI 3.0 is available, otherwise log and fail.
 * <li>Load {@link Cache} provider and register its filter if necessary.
 * <li>Add {@link FacesViews} mappings to FacesServlet if necessary.
 * <li>Add {@link ViewResourceHandler} mapping to FacesServlet if necessary.
 * <li>Register {@link FacesExceptionFilter} via {@link FullAjaxExceptionHandler} if necessary.
 * <li>Instantiate {@link Eager} application scoped beans and register {@link EagerBeansWebListener} if necessary.
 * <li>Register {@link GraphicImageBean} beans in {@link GraphicResource}.
 * <li>Register {@link Socket} endpoint if necessary.
 * <li>Register {@link Sse} endpoint if necessary.
 * <li>Register {@link ScriptErrorHandler} servlet if necessary.
 * </ol>
 * <p>
 * This is invoked <strong>after</strong> {@link ApplicationInitializer} and <strong>before</strong> {@link ApplicationProcessor}. If any exception is thrown,
 * then the deployment will fail, unless the {@value OmniFaces#PARAM_NAME_SKIP_DEPLOYMENT_EXCEPTION} context parameter is set to <code>true</code>, it will then
 * merely log a WARNING line.
 *
 * @author Bauke Scholtz
 * @since 2.0
 */
@WebListener
public class ApplicationListener extends DefaultServletContextListener {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final Logger logger = Logger.getLogger(ApplicationListener.class.getName());

    private static final String ERROR_FACES_API_UNAVAILABLE = "Faces API is not available in this environment.";
    private static final String ERROR_FACES_API_INCOMPATIBLE = "Faces API of this environment is not Faces 4.1 compatible.";
    private static final String ERROR_CDI_API_UNAVAILABLE = "CDI API is not available in this environment.";
    private static final String ERROR_CDI_IMPL_UNAVAILABLE = "CDI BeanManager instance is not available in this environment.";

    // Actions --------------------------------------------------------------------------------------------------------

    @Override
    public void contextInitialized(ServletContextEvent event) {
        var servletContext = event.getServletContext();
        var skipDeploymentException = OmniFaces.skipDeploymentException(servletContext);

        if (!skipDeploymentException) {
            checkFacesAvailable();
            checkCDIAvailable();
        }

        try {
            Faces.setContext(new ServletContextFacesContext(servletContext));

            CacheInitializer.loadProviderAndRegisterFilter(servletContext);
            FacesViews.addFacesServletMappings(servletContext);
            ViewResourceHandler.addFacesServletMappingsIfNecessary(servletContext);
            FullAjaxExceptionHandler.registerFacesExceptionFilterIfNecessary(servletContext);

            if (skipDeploymentException) {
                checkCDIImplAvailable(); // Because below initializations require CDI impl being available, see #703
            }

            EagerBeansRepository.instantiateApplicationScopedAndRegisterListenerIfNecessary(servletContext);
            GraphicResource.registerGraphicImageBeans();
            Socket.registerEndpointIfNecessary(servletContext);
            Sse.registerEndpointIfNecessary(servletContext);
            ScriptErrorHandler.registerServletIfNecessary(servletContext);
        }
        catch (Exception | LinkageError e) {
            if (skipDeploymentException) {
                logger.log(WARNING, WARNING_OMNIFACES_INITIALIZATION_FAIL.formatted(e));
            }
            else {
                throw new IllegalStateException(ERROR_OMNIFACES_INITIALIZATION_FAIL, e);
            }
        }
        finally {
            Faces.setContext(null);
        }
    }

    private static void checkFacesAvailable() {
        try {
            checkFacesAPIAvailable();
            checkFaces41Compatible();
        }
        catch (Exception | LinkageError e) {
            logger.severe("""

                ████████████████████████████████████████████████████████████████████████████████
                █░▀░░░░▀█▀░░░░░░▀█░░░░░░▀█▀░░░░░▀█                                             ▐
                █░░▐█▌░░█░░░██░░░█░░██░░░█░░░██░░█ OmniFaces failed to initialize!             ▐
                █░░▐█▌░░█░░░██░░░█░░██░░░█░░░██░░█                                             ▐
                █░░▐█▌░░█░░░██░░░█░░░░░░▄█░░▄▄▄▄▄█ OmniFaces 5.x requires minimally Faces 4.1, ▐
                █░░▐█▌░░█░░░██░░░█░░░░████░░░░░░░█ but none was found on this environment.     ▐
                █░░░█░░░█▄░░░░░░▄█░░░░████▄░░░░░▄█ Downgrade to OmniFaces 4.x, 3.x, 2.x or 1.x.▐
                ████████████████████████████████████████████████████████████████████████████████""");
            throw e;
        }
    }

    private static void checkCDIAvailable() {
        try {
            checkCDIAPIAvailable();
            // No need to explicitly check version here because the jakarta.* one is already guaranteed to be minimally 3.0.
            checkCDIImplAvailable();
        }
        catch (Exception | LinkageError e) {
            logger.severe("""

                ████████████████████████████████████████████████████████████████████████████████
                ▌                         ▐█     ▐                                             ▐
                ▌    ▄                  ▄█▓█▌    ▐ OmniFaces failed to initialize!             ▐
                ▌   ▐██▄               ▄▓░░▓▓    ▐                                             ▐
                ▌   ▐█░██▓            ▓▓░░░▓▌    ▐ This OmniFaces version requires CDI,        ▐
                ▌   ▐█▌░▓██          █▓░░░░▓     ▐ but none was found on this environment.     ▐
                ▌    ▓█▌░░▓█▄███████▄███▓░▓█     ▐                                             ▐
                ▌    ▓██▌░▓██░░░░░░░░░░▓█░▓▌     ▐                                             ▐
                ▌     ▓█████░░░░░░░░░░░░▓██      ▐                                             ▐
                ▌     ▓██▓░░░░░░░░░░░░░░░▓█      ▐                                             ▐
                ▌     ▐█▓░░░░░░█▓░░▓█░░░░▓█▌     ▐                                             ▐
                ▌     ▓█▌░▓█▓▓██▓░█▓▓▓▓▓░▓█▌     ▐                                             ▐
                ▌     ▓▓░▓██████▓░▓███▓▓▌░█▓     ▐                                             ▐
                ▌    ▐▓▓░█▄▐▓▌█▓░░▓█▐▓▌▄▓░██     ▐                                             ▐
                ▌    ▓█▓░▓█▄▄▄█▓░░▓█▄▄▄█▓░██▌    ▐                                             ▐
                ▌    ▓█▌░▓█████▓░░░▓███▓▀░▓█▓    ▐                                             ▐
                ▌   ▐▓█░░░▀▓██▀░░░░░ ▀▓▀░░▓█▓    ▐                                             ▐
                ▌   ▓██░░░░░░░░▀▄▄▄▄▀░░░░░░▓▓    ▐                                             ▐
                ▌   ▓█▌░░░░░░░░░░▐▌░░░░░░░░▓▓▌   ▐                                             ▐
                ▌   ▓█░░░░░░░░░▄▀▀▀▀▄░░░░░░░█▓   ▐                                             ▐
                ▌  ▐█▌░░░░░░░░▀░░░░░░▀░░░░░░█▓▌  ▐                                             ▐
                ▌  ▓█░░░░░░░░░░░░░░░░░░░░░░░██▓  ▐                                             ▐
                ▌  ▓█░░░░░░░░░░░░░░░░░░░░░░░▓█▓  ▐ You have 3 options:                         ▐
                ██████████████████████████████████ 1. Downgrade to CDI-less OmniFaces 1.x.     ▐
                █░▀░░░░▀█▀░░░░░░▀█░░░░░░▀█▀░░░░░▀█ 2. Install CDI in this environment.         ▐
                █░░▐█▌░░█░░░██░░░█░░██░░░█░░░██░░█ 3. Switch to a CDI capable environment.     ▐
                █░░▐█▌░░█░░░██░░░█░░██░░░█░░░██░░█                                             ▐
                █░░▐█▌░░█░░░██░░░█░░░░░░▄█░░▄▄▄▄▄█ For additional instructions, check          ▐
                █░░▐█▌░░█░░░██░░░█░░░░████░░░░░░░█ https://omnifaces.org/cdi                   ▐
                █░░░█░░░█▄░░░░░░▄█░░░░████▄░░░░░▄█                                             ▐
                ████████████████████████████████████████████████████████████████████████████████""");
            throw e;
        }
    }

    // Helpers --------------------------------------------------------------------------------------------------------

    private static void checkFacesAPIAvailable() {
        try {
            toClass("jakarta.faces.webapp.FacesServlet");
        }
        catch (Exception | LinkageError e) {
            throw new IllegalStateException(ERROR_FACES_API_UNAVAILABLE, e);
        }
    }

    private static void checkFaces41Compatible() {
        try {
            toClass("jakarta.faces.view.ActionSourceAttachedObjectTarget");
        }
        catch (Exception | LinkageError e) {
            throw new IllegalStateException(ERROR_FACES_API_INCOMPATIBLE, e);
        }
    }

    private static void checkCDIAPIAvailable() {
        try {
            toClass("jakarta.enterprise.inject.spi.BeanManager");
        }
        catch (Exception | LinkageError e) {
            throw new IllegalStateException(ERROR_CDI_API_UNAVAILABLE, e);
        }
    }

    private static void checkCDIImplAvailable() {
        try {
            toClass("org.omnifaces.util.Beans").getMethod("getManager").invoke(null).toString();
        }
        catch (Exception | LinkageError e) {
            throw new IllegalStateException(ERROR_CDI_IMPL_UNAVAILABLE, e);
        }
    }

    // Inner classes --------------------------------------------------------------------------------------------------

    /**
     * This faces context wrapper basically makes ServletContext available by {@link Servlets#getContext()} further down in the chain and ignores all the other
     * things Faces. It's currently only used by {@link FullAjaxExceptionHandler#registerFacesExceptionFilterIfNecessary(ServletContext)}. See also #831.
     */
    private static class ServletContextFacesContext extends FacesContextWrapper {

        private ExternalContext externalContext;

        public ServletContextFacesContext(ServletContext servletContext) {
            super(null);
            this.externalContext = new ServletContextExternalContext(servletContext);
        }

        @Override
        public ExternalContext getExternalContext() {
            return externalContext;
        }

        private static class ServletContextExternalContext extends ExternalContextWrapper {

            private ServletContext servletContext;

            public ServletContextExternalContext(ServletContext servletContext) {
                super(null);
                this.servletContext = servletContext;
            }

            @Override
            public Object getContext() {
                return servletContext;
            }

        }

    }

}
