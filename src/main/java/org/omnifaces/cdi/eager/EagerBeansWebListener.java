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
package org.omnifaces.cdi.eager;

import static java.lang.String.format;
import static java.util.logging.Level.WARNING;
import static org.omnifaces.util.Servlets.getRequestRelativeURIWithoutPathParameters;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * <p>
 * A web listener that instantiates eager session beans and request/view beans by request URI. This is auto-registered
 * by {@link EagerBeansRepository#instantiateApplicationScopedAndRegisterListenerIfNecessary(javax.servlet.ServletContext)}
 * when any eager session beans or request/view beans by request URI are available.
 *
 * @since 2.0
 * @author Arjan Tijms
 *
 */
public class EagerBeansWebListener implements HttpSessionListener, ServletRequestListener {

	// Constants ------------------------------------------------------------------------------------------------------

	public static final String SESSION_CREATED = "org.omnifaces.eager.SESSION_CREATED";

	private static final Logger logger = Logger.getLogger(EagerBeansWebListener.class.getName());

	private static final String POSSIBLY_REQUEST_SCOPE_NOT_ACTIVE =
		"Could not instantiate eager request scoped beans for request %s. Possibly the CDI request scope is not active."
			+ " If this is indeed the case, see JavaDoc on org.omnifaces.cdi.Eager on how to remedy this.";

	private static volatile boolean sessionListenerDisabled;
	private static volatile boolean requestListenerDisabled;

	// Actions --------------------------------------------------------------------------------------------------------

	static void disable() {
		sessionListenerDisabled = true;
		requestListenerDisabled = true;
	}

	@Override
	public void sessionCreated(HttpSessionEvent event) {
		if (!sessionListenerDisabled) {
			if (!EagerBeansRepository.getInstance().instantiateSessionScoped()) {
				sessionListenerDisabled = true;
			}
		}
		else {
			// Record a "session created" marker manually. HttpSession#isNew() not entirely accurate for our purpose.
			event.getSession().setAttribute(SESSION_CREATED, new AtomicBoolean(true));
		}
	}

	@Override
	public void requestInitialized(ServletRequestEvent event) {
		if (!requestListenerDisabled) {
			String uri = getRequestRelativeURIWithoutPathParameters((HttpServletRequest) event.getServletRequest());

			try {
				if (!EagerBeansRepository.getInstance().instantiateByRequestURI(uri)) {
					requestListenerDisabled = true;
				}
			}
			catch (Exception e) {
				logger.log(WARNING, format(POSSIBLY_REQUEST_SCOPE_NOT_ACTIVE, uri), e);
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