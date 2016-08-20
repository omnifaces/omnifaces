/*
 * Copyright 2016 OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnifaces.cdi.eager;

import static org.omnifaces.cdi.eager.EagerBeansWebListener.SESSION_CREATED;
import static org.omnifaces.util.Servlets.getRequestRelativeURIWithoutPathParameters;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionListener;

import org.omnifaces.filter.HttpFilter;

/**
 * <p>
 * A servlet Filter that can be used as alternative for {@link EagerBeansWebListener}.
 * <p>
 * This instantiates eager request scoped beans during request processing at the point where this filter
 * is inserted in the chain.
 * <p>
 * This is needed for those situations where the CDI request scope is NOT available in a {@link ServletRequestListener},
 * such as is the case for GlassFish 3 (note that this is not spec compliant, CDI request scope should be available) and where
 * session scoped beans cannot be instantiated from an {@link HttpSessionListener} such as is the case for GlassFish 3 again.
 * <p>
 * If this Filter is installed {@link EagerBeansWebListener} main function (instantiating request and session scoped
 * beans) will be automatically disabled.
 * <p>
 * Naturally this filter should not be enabled for environments where CDI is not available at all.
 *
 * @since 1.8
 * @author Arjan Tijms
 *
 */
public class EagerBeansFilter extends HttpFilter { // TODO: remove in OmniFaces 3.0

	@Override
	public void init() throws ServletException {
		EagerBeansWebListener.disable();
	}

	@Override
	public void doFilter(HttpServletRequest request, HttpServletResponse response, HttpSession session,	FilterChain chain) throws ServletException, IOException {
		EagerBeansRepository.getInstance().instantiateByRequestURI(getRequestRelativeURIWithoutPathParameters(request));

		chain.doFilter(request, response);

		HttpSession newSession = request.getSession(false);

		if (newSession != null) {

			// Get the session created marker inserted by EagerBeansSessionListener. Note that HttpSession#isNew()
			// would be tons easier but doesn't seem to work reliably on all servers.
			AtomicBoolean sessionCreated = (AtomicBoolean) newSession.getAttribute(SESSION_CREATED);
			if (sessionCreated != null) {
				// First remove so session created marker to minimize other threads/request picking it up at all.
				newSession.removeAttribute(SESSION_CREATED);

				// Even if we remove it immediately there's still a chance for a race, so test the boolean atomically
				// and make sure only one thread sees the initial value of "true" returned.
				if (sessionCreated.getAndSet(false)) {
					EagerBeansRepository.getInstance().instantiateSessionScoped();
				}
			}
		}
	}

}
