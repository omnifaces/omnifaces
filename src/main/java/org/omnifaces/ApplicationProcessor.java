/*
 * Copyright 2018 OmniFaces
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
import static org.omnifaces.util.Faces.getServletContext;

import java.util.logging.Logger;

import javax.faces.application.Application;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;
import javax.servlet.ServletContext;

import org.omnifaces.component.search.MessagesKeywordResolver;
import org.omnifaces.facesviews.FacesViews;

/**
 * <p>
 * OmniFaces application processor. This runs when the faces application is created.
 * This performs the following tasks:
 * <ol>
 * <li>Register the {@link FacesViews} view handler.
 * <li>Register the {@link MessagesKeywordResolver}.
 * </ol>
 * <p>
 * This is invoked <strong>after</strong> {@link ApplicationInitializer} and {@link ApplicationListener}.
 *
 * @author Bauke Scholtz
 * @since 3.1
 */
public class ApplicationProcessor implements SystemEventListener {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final Logger logger = Logger.getLogger(ApplicationProcessor.class.getName());

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public boolean isListenerForSource(Object source) {
		return source instanceof Application;
	}

	@Override
	public void processEvent(SystemEvent event) {
		try {
			ServletContext servletContext = getServletContext();
			Application application = (Application) event.getSource();
			FacesViews.registerViewHander(servletContext, application);
			MessagesKeywordResolver.register(application);
		}
		catch (Exception | LinkageError e) {
			logger.log(SEVERE, ERROR_OMNIFACES_INITIALIZATION_FAIL, e);
			throw e;
		}
	}

}