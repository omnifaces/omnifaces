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

import static org.omnifaces.util.Servlets.getRequestRelativeURIWithoutPathParameters;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.omnifaces.filter.HttpFilter;

/**
 * A servlet Filter that can be used as alternative for {@link EagerBeansRequestListener}.
 * <p>
 * This instantiates eager request scoped beans during request processing at the point where this filter
 * is inserted in the chain.
 * <p>
 * This is needed for those situations where CDI is NOT available in a {@link ServletRequestListener},
 * such as is the case for GlassFish 3 (note that this is not spec compliant, CDI should be available).
 * <p>
 * If this Filter is installed {@link EagerBeansRequestListener} will be automatically disabled.
 *
 * @since 1.8
 * @author Arjan Tijms
 *
 */
public class EagerBeansFilter extends HttpFilter {
	
	@Inject
	private BeansInstantiator eagerBeansRepository;

	@Override
	public void init() throws ServletException {
		EagerBeansRequestListener.setEnabled(false);
	}

	@Override
	public void doFilter(HttpServletRequest request, HttpServletResponse response, HttpSession session,	FilterChain chain) throws ServletException, IOException {
		eagerBeansRepository.instantiateByRequestURI(getRequestRelativeURIWithoutPathParameters(request));
		chain.doFilter(request, response);
	}

}
