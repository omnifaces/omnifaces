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

import java.util.logging.Logger;

import javax.inject.Inject;
import javax.servlet.ServletRequestEvent;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;

import org.omnifaces.eventlistener.DefaultServletRequestListener;

/**
 * A WebListener that instantiates eager request scoped beans by request URI.
 * <p>
 * This instantiates beans at one of the earliest possible moment during request
 * processing.
 *
 * @since 1.8
 * @author Arjan Tijms
 *
 */
@WebListener
public class EagerBeansRequestListener extends DefaultServletRequestListener {
	
	private static final Logger logger = Logger.getLogger(EagerBeansRequestListener.class.getName());
	
	// Private constants ----------------------------------------------------------------------------------------------

	private static final String POSSIBLY_REQUEST_SCOPE_NOT_ACTIVE =
		"Could not instantiate eager request scoped beans for request %s. Possibly the CDI request scope is not active."
			+ " If this is indeed the case, see JavaDoc on org.omnifaces.cdi.Eager on how to remedy this.";

	
	// Variables ------------------------------------------------------------------------------------------------------
	
	private static boolean enabled = true;
	
	@Inject
	private BeansInstantiator eagerBeansRepository;
	
	
	// Methods --------------------------------------------------------------------------------------------------------

	public static void setEnabled(boolean enabled) {
		EagerBeansRequestListener.enabled = enabled;
	}

	@Override
	public void requestInitialized(ServletRequestEvent sre) {
		if (eagerBeansRepository != null && enabled) {
			try {
				eagerBeansRepository.instantiateByRequestURI(getRequestRelativeURIWithoutPathParameters((HttpServletRequest)sre.getServletRequest()));
			} catch (Exception e) {
				logger.log(
					SEVERE, 
					format(POSSIBLY_REQUEST_SCOPE_NOT_ACTIVE, getRequestRelativeURIWithoutPathParameters((HttpServletRequest)sre.getServletRequest())), 
					e
				);
			}
		}
	}

}