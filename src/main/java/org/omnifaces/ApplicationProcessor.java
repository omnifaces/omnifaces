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

import static java.lang.String.format;
import static java.util.logging.Level.WARNING;
import static org.omnifaces.ApplicationInitializer.ERROR_OMNIFACES_INITIALIZATION_FAIL;
import static org.omnifaces.ApplicationInitializer.WARNING_OMNIFACES_INITIALIZATION_FAIL;
import static org.omnifaces.util.Faces.getServletContext;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.faces.application.Application;
import jakarta.faces.application.ResourceHandler;
import jakarta.faces.event.SystemEvent;
import jakarta.faces.event.SystemEventListener;
import jakarta.servlet.ServletContext;

import org.omnifaces.component.search.MessagesKeywordResolver;
import org.omnifaces.config.FacesConfigXml;
import org.omnifaces.config.OmniFaces;
import org.omnifaces.facesviews.FacesViews;

/**
 * <p>
 * OmniFaces application processor. This runs when the faces application is created.
 * This performs the following tasks:
 * <ol>
 * <li>Check if {@link Application#getResourceHandler()} chain is unique, otherwise log and fail.
 * <li>Register the {@link FacesViews} view handler.
 * <li>Register the {@link MessagesKeywordResolver}.
 * </ol>
 * <p>
 * This is invoked <strong>after</strong> {@link ApplicationInitializer} and {@link ApplicationListener}.
 * If any exception is thrown, then the deployment will fail, unless the {@value OmniFaces#PARAM_NAME_SKIP_DEPLOYMENT_EXCEPTION}
 * context parameter is set to <code>true</code>, it will then merely log a WARNING line.
 *
 * @author Bauke Scholtz
 * @since 3.1
 */
public class ApplicationProcessor implements SystemEventListener {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final Logger logger = Logger.getLogger(ApplicationProcessor.class.getName());

    private static final String ERROR_DUPLICATE_RESOURCE_HANDLER =
        "Resource handler %s is duplicated."
            + " This will result in erratic resource handling behavior."
            + " Please check if your build is clean and does not contain duplicate libraries having same resource handler."
            + " Also check if the same resource handler is not declared multiple times in all your faces-config.xml files combined.";

    // Actions --------------------------------------------------------------------------------------------------------

    @Override
    public boolean isListenerForSource(Object source) {
        return source instanceof Application;
    }

    @Override
    public void processEvent(SystemEvent event) {
        ServletContext servletContext = getServletContext();

        try {
            Application application = (Application) event.getSource();
            checkDuplicateResourceHandler();
            FacesViews.registerViewHandler(servletContext, application);
            MessagesKeywordResolver.register(application);
        }
        catch (Exception | LinkageError e) {
            if (OmniFaces.skipDeploymentException(servletContext)) {
                logger.log(WARNING, format(WARNING_OMNIFACES_INITIALIZATION_FAIL, e));
            }
            else {
                throw new IllegalStateException(ERROR_OMNIFACES_INITIALIZATION_FAIL, e);
            }
        }
    }

    private void checkDuplicateResourceHandler() {
        Set<Class<? extends ResourceHandler>> allResourceHandlers = new HashSet<>();

        for (Class<? extends ResourceHandler> resourceHandler : FacesConfigXml.instance().getResourceHandlers()) {
            if (!allResourceHandlers.add(resourceHandler)) {
                throw new IllegalStateException(format(ERROR_DUPLICATE_RESOURCE_HANDLER, resourceHandler));
            }
        }
    }

}