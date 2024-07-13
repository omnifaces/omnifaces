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
package org.omnifaces.filter;

import static java.lang.String.format;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.SEVERE;
import static org.omnifaces.exceptionhandler.FullAjaxExceptionHandler.EXCEPTION_UUID;
import static org.omnifaces.exceptionhandler.FullAjaxExceptionHandler.getExceptionTypesToIgnoreInLogging;
import static org.omnifaces.exceptionhandler.FullAjaxExceptionHandler.getExceptionTypesToUnwrap;
import static org.omnifaces.util.Exceptions.unwrap;
import static org.omnifaces.util.Servlets.getRemoteAddr;
import static org.omnifaces.util.Servlets.resetResponse;
import static org.omnifaces.util.Utils.isOneInstanceOf;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Formatter;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

import jakarta.el.ELException;
import jakarta.faces.FacesException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.omnifaces.config.WebXml;
import org.omnifaces.exceptionhandler.FullAjaxExceptionHandler;

/**
 * <p>
 * The {@link FacesExceptionFilter} will solve 2 problems with exceptions thrown in Faces methods.
 * <ol>
 * <li>Mojarra's <code>FacesFileNotFoundException</code> needs to be interpreted as 404.
 * <li>Root cause needs to be unwrapped from {@link FacesException} and {@link ELException} to utilize standard
 * Servlet API error page handling.
 * </ol>
 * <p>
 * Noted should be that this filter won't run on exceptions thrown during ajax requests. To handle them using
 * <code>web.xml</code> configured error pages, use {@link FullAjaxExceptionHandler}.
 * <p>
 * Since version 3.2, the {@link FacesExceptionFilter} also logs exceptions with an UUID and IP via the
 * {@link #logException(HttpServletRequest, Throwable, String, String, Object...)} method. The UUID is in turn available
 * in EL by <code>#{requestScope['org.omnifaces.exception_uuid']}</code>.
 *
 * <h2>Installation</h2>
 * <p>
 * To get it to run, map this filter on an <code>&lt;url-pattern&gt;</code> of <code>/*</code> in <code>web.xml</code>.
 * <pre>
 * &lt;filter&gt;
 *     &lt;filter-name&gt;facesExceptionFilter&lt;/filter-name&gt;
 *     &lt;filter-class&gt;org.omnifaces.filter.FacesExceptionFilter&lt;/filter-class&gt;
 * &lt;/filter&gt;
 * &lt;filter-mapping&gt;
 *     &lt;filter-name&gt;facesExceptionFilter&lt;/filter-name&gt;
 *     &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
 * &lt;/filter-mapping&gt;
 * </pre>
 *
 * <h2>Error pages</h2>
 * <p>
 * Please refer the "Error pages" section of the {@link FullAjaxExceptionHandler} javadoc for recommended error page
 * configuration.
 *
 * <h2>Configuration</h2>
 * <p>
 * Please refer the "Configuration" section of the {@link FullAjaxExceptionHandler} javadoc for available context
 * parameters.
 *
 * <h2>Customizing <code>FacesExceptionFilter</code></h2>
 * <p>
 * If more fine grained control is desired for logging the exception, then the developer can opt to extend this
 * {@link FacesExceptionFilter} and override one or more of the following protected methods:
 * <ul>
 * <li>{@link #logException(HttpServletRequest, Throwable, String, String, Object...)}
 * </ul>
 *
 * @author Bauke Scholtz
 * @see FullAjaxExceptionHandler
 * @see HttpFilter
 */
public class FacesExceptionFilter extends HttpFilter {

    private static final Logger logger = Logger.getLogger(FacesExceptionFilter.class.getName());

    private static final String LOG_EXCEPTION_HANDLED =
            "FacesExceptionFilter: An exception occurred during processing servlet request."
                + " Error page '%s' will be shown.";
    private static final String LOG_EXCEPTION_UNHANDLED =
            "FacesExceptionFilter: An exception occurred during processing servlet request."
                + " Error page '%s' CANNOT be shown as response is already committed.";

    private Class<? extends Throwable>[] exceptionTypesToUnwrap;
    private Class<? extends Throwable>[] exceptionTypesToIgnoreInLogging;

    @Override
    public void init() throws ServletException {
        exceptionTypesToUnwrap = getExceptionTypesToUnwrap(getServletContext());
        exceptionTypesToIgnoreInLogging = getExceptionTypesToIgnoreInLogging(getServletContext());
    }

    @Override
    public void doFilter
        (HttpServletRequest request, HttpServletResponse response, HttpSession session, FilterChain chain)
            throws ServletException, IOException
    {
        try {
            chain.doFilter(request, response);
        }
        catch (FileNotFoundException ignore) {
            logger.log(FINEST, "Ignoring thrown exception; this is a Faces quirk and it should be interpreted as 404.", ignore);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, request.getRequestURI());
        }
        catch (Throwable exception) {
            request.setAttribute(EXCEPTION_UUID, UUID.randomUUID().toString());
            var cause = unwrap(exception instanceof ServletException ? exception.getCause() : exception, exceptionTypesToUnwrap);
            var location = WebXml.instance().findErrorPageLocation(cause);

            if (!response.isCommitted()) {
                logException(request, cause, location, LOG_EXCEPTION_HANDLED, location);
                resetResponse(response);
            }
            else {
                logException(request, cause, location, LOG_EXCEPTION_UNHANDLED, location);
            }

            if (Objects.equals(exception, cause)) {
                throw exception;
            }

            logger.log(FINEST, "Ignoring thrown exception; this is a wrapper exception and only its root cause is of interest.", exception);

            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            else {
                throw new ServletException(cause);
            }
        }
    }

    /**
     * Log the thrown exception and determined error page location with the given message, optionally parameterized
     * with the given parameters.
     * The default implementation logs through <code>java.util.logging</code> as SEVERE when the thrown exception is
     * not an instance of any type specified in context parameter
     * {@value org.omnifaces.exceptionhandler.FullAjaxExceptionHandler#PARAM_NAME_EXCEPTION_TYPES_TO_IGNORE_IN_LOGGING}.
     * The log message will be prepended with the UUID and IP address.
     * The UUID is available in EL by <code>#{requestScope['org.omnifaces.exception_uuid']}</code>.
     * @param request The involved servlet request.
     * @param exception The exception to log.
     * @param location The error page location.
     * @param message The log message.
     * @param parameters The log message parameters, if any. They are formatted using {@link Formatter}.
     * @since 3.2
     */
    protected void logException
        (HttpServletRequest request, Throwable exception, String location, String message, Object... parameters)
    {
        if (!isOneInstanceOf(exception.getClass(), exceptionTypesToIgnoreInLogging)) {
            logger.log(SEVERE, format("[%s][%s] %s", request.getAttribute(EXCEPTION_UUID), getRemoteAddr(request), format(message, parameters)), exception);
        }
    }

}