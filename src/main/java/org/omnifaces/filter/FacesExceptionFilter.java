/*
 * Copyright 2012 OmniFaces.
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
package org.omnifaces.filter;

import static org.omnifaces.util.Exceptions.unwrap;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.webapp.FacesServlet;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.omnifaces.exceptionhandler.FullAjaxExceptionHandler;

/**
 * <p>
 * The {@link FacesExceptionFilter} will solve 2 problems with exceptions thrown in JSF methods.
 * <ol>
 * <li>Mojarra's <code>FacesFileNotFoundException</code> needs to be interpreted as 404.
 * <li>Root cause needs to be unwrapped from {@link FacesException} and {@link ELException} to utilize standard
 * Servlet API error page handling.
 * </ol>
 * <p>
 * Noted should be that this filter won't run on exceptions thrown during ajax requests. To handle them using
 * <code>web.xml</code> configured error pages, use {@link FullAjaxExceptionHandler}.
 *
 * <h3>Installation</h3>
 * <p>
 * To get it to run, map this filter on the <code>&lt;servlet-name&gt;</code> of the {@link FacesServlet} in the same
 * <code>web.xml</code>.
 * <pre>
 * &lt;filter&gt;
 *     &lt;filter-name&gt;facesExceptionFilter&lt;/filter-name&gt;
 *     &lt;filter-class&gt;org.omnifaces.filter.FacesExceptionFilter&lt;/filter-class&gt;
 * &lt;/filter&gt;
 * &lt;filter-mapping&gt;
 *     &lt;filter-name&gt;facesExceptionFilter&lt;/filter-name&gt;
 *     &lt;servlet-name&gt;facesServlet&lt;/servlet-name&gt;
 * &lt;/filter-mapping&gt;
 * </pre>
 *
 * <h3>Configuration</h3>
 * <p>
 * By default only {@link FacesException} and {@link ELException} are unwrapped. You can supply a context parameter
 * {@value org.omnifaces.exceptionhandler.FullAjaxExceptionHandler#PARAM_NAME_EXCEPTION_TYPES_TO_UNWRAP} to specify
 * additional exception types to unwrap. The context parameter value must be a commaseparated string of fully qualified
 * names of additional exception types.
 * <pre>
 * &lt;context-param&gt;
 *     &lt;param-name&gt;org.omnifaces.EXCEPTION_TYPES_TO_UNWRAP&lt;/param-name&gt;
 *     &lt;param-value&gt;javax.ejb.EJBException,javax.persistence.RollbackException&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * </pre>
 * <p>
 * This context parameter will also be read and used by {@link FullAjaxExceptionHandler}.
 *
 *
 * @author Bauke Scholtz
 * @see FullAjaxExceptionHandler
 * @see HttpFilter
 */
public class FacesExceptionFilter extends HttpFilter {

	private Class<? extends Throwable>[] exceptionTypesToUnwrap;

	@Override
	public void init() throws ServletException {
		exceptionTypesToUnwrap = FullAjaxExceptionHandler.getExceptionTypesToUnwrap(getServletContext());
	}

	@Override
	public void doFilter
		(HttpServletRequest request, HttpServletResponse response, HttpSession session, FilterChain chain)
			throws ServletException, IOException
	{
		try {
			chain.doFilter(request, response);
		}
		catch (FileNotFoundException e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, request.getRequestURI());
		}
		catch (ServletException e) {
			throw new ServletException(unwrap(e.getRootCause(), exceptionTypesToUnwrap));
		}
	}

}