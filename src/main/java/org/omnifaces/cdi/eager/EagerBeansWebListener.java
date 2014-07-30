/*
 * Copyright 2014 OmniFaces.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnifaces.cdi.eager;

import static java.lang.String.format;
import static java.util.logging.Level.SEVERE;
import static org.omnifaces.util.Servlets.getRequestRelativeURIWithoutPathParameters;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * <p>
 * A web listener that instantiates eager session and request scoped beans.
 *
 * @since 2.0
 * @author Arjan Tijms
 *
 */
@WebListener
public class EagerBeansWebListener implements HttpSessionListener, ServletRequestListener {

	// Constants ------------------------------------------------------------------------------------------------------

	public static final String SESSION_CREATED = "org.omnifaces.eager.SESSION_CREATED";

	private static final Logger logger = Logger.getLogger(EagerBeansWebListener.class.getName());

	private static final String POSSIBLY_REQUEST_SCOPE_NOT_ACTIVE =
		"Could not instantiate eager request scoped beans for request %s. Possibly the CDI request scope is not active."
			+ " If this is indeed the case, see JavaDoc on org.omnifaces.cdi.Eager on how to remedy this.";

	private static boolean disabled;

	@Inject
	private EagerBeansRepository eagerBeansRepository;

	// Actions --------------------------------------------------------------------------------------------------------

	public static void disable() {
		EagerBeansWebListener.disabled = true;
	}

	@Override
	public void sessionCreated(HttpSessionEvent event) {
		if (!disabled) {
			eagerBeansRepository.instantiateSessionScoped();
		}
		else {
			// Record a "session created" marker manually. HttpSession#isNew() not entirely accurate for our purpose.
			event.getSession().setAttribute(SESSION_CREATED, new AtomicBoolean(true));
		}
	}

	@Override
	public void requestInitialized(ServletRequestEvent event) {
		if (!disabled) {
			String uri = getRequestRelativeURIWithoutPathParameters((HttpServletRequest) event.getServletRequest());

			try {
				eagerBeansRepository.instantiateByRequestURI(uri);
			}
			catch (Exception e) {
				logger.log(SEVERE, format(POSSIBLY_REQUEST_SCOPE_NOT_ACTIVE, uri), e);
			}
		}
	}

	@Override
	public void requestDestroyed(ServletRequestEvent event) {
		// NOOP.
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent event) {
		// NOOP.
	}

}