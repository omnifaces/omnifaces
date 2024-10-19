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
package org.omnifaces.exceptionhandler;

import static jakarta.servlet.DispatcherType.REQUEST;
import static jakarta.servlet.RequestDispatcher.ERROR_EXCEPTION;
import static jakarta.servlet.RequestDispatcher.ERROR_EXCEPTION_TYPE;
import static jakarta.servlet.RequestDispatcher.ERROR_MESSAGE;
import static jakarta.servlet.RequestDispatcher.ERROR_REQUEST_URI;
import static jakarta.servlet.RequestDispatcher.ERROR_STATUS_CODE;
import static java.lang.String.format;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static org.omnifaces.util.Exceptions.unwrap;
import static org.omnifaces.util.Faces.getContext;
import static org.omnifaces.util.Faces.getServletContext;
import static org.omnifaces.util.FacesLocal.getRemoteAddr;
import static org.omnifaces.util.FacesLocal.getRequest;
import static org.omnifaces.util.FacesLocal.getRequestAttribute;
import static org.omnifaces.util.FacesLocal.normalizeViewId;
import static org.omnifaces.util.FacesLocal.resetResponse;
import static org.omnifaces.util.FacesLocal.setRequestAttribute;
import static org.omnifaces.util.Utils.isEmpty;
import static org.omnifaces.util.Utils.isOneInstanceOf;
import static org.omnifaces.util.Utils.splitAndTrim;
import static org.omnifaces.util.Utils.unmodifiableSet;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import jakarta.el.ELException;
import jakarta.faces.FacesException;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerFactory;
import jakarta.faces.context.ExceptionHandlerWrapper;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.AbortProcessingException;
import jakarta.faces.event.PhaseId;
import jakarta.faces.event.PreRenderViewEvent;
import jakarta.faces.webapp.FacesServlet;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.omnifaces.ApplicationListener;
import org.omnifaces.config.FacesConfigXml;
import org.omnifaces.config.WebXml;
import org.omnifaces.context.OmniPartialViewContext;
import org.omnifaces.context.OmniPartialViewContextFactory;
import org.omnifaces.filter.FacesExceptionFilter;
import org.omnifaces.util.Exceptions;
import org.omnifaces.util.Hacks;
import org.omnifaces.util.Reflection;

/**
 * <p>
 * The {@link FullAjaxExceptionHandler} will transparently handle exceptions during ajax requests exactly the same way
 * as exceptions during synchronous (non-ajax) requests.
 * <p>
 * By default, when an exception occurs during a Faces ajax request, the enduser would not get any form of feedback if the
 * action was successfully performed or not. In Mojarra, only when the project stage is set to <code>Development</code>,
 * the enduser would see a bare JavaScript alert with only the exception type and message. It would make sense if
 * exceptions during ajax requests are handled the same way as exceptions during synchronous requests, which is
 * utilizing the standard Servlet API <code>&lt;error-page&gt;</code> mechanisms in <code>web.xml</code>.
 *
 * <h2>Installation</h2>
 * <p>
 * This handler must be registered by a factory as follows in <code>faces-config.xml</code> in order to get it to run:
 * <pre>
 * &lt;factory&gt;
 *     &lt;exception-handler-factory&gt;org.omnifaces.exceptionhandler.FullAjaxExceptionHandlerFactory&lt;/exception-handler-factory&gt;
 * &lt;/factory&gt;
 * </pre>
 *
 * <h2>Error pages</h2>
 * <p>
 * This exception handler will parse the <code>web.xml</code> and <code>web-fragment.xml</code> files to find the error
 * page locations of the HTTP error code <code>500</code> and all declared specific exception types. Those locations
 * need to point to Facelets files (JSP is not supported) and the URL must match the {@link FacesServlet} mapping (just
 * mapping it on <code>*.xhtml</code> should eliminate confusion about virtual URLs). E.g.
 * <pre>
 * &lt;error-page&gt;
 *     &lt;exception-type&gt;jakarta.faces.application.ViewExpiredException&lt;/exception-type&gt;
 *     &lt;location&gt;/WEB-INF/errorpages/expired.xhtml&lt;/location&gt;
 * &lt;/error-page&gt;
 * </pre>
 * <p>
 * The location of the HTTP error code <code>500</code> or the exception type <code>java.lang.Throwable</code> is
 * <b>required</b> in order to get the {@link FullAjaxExceptionHandler} to work, because there's then at least a fall
 * back error page when there's no match with any of the declared specific exceptions types. You can have both, but the
 * <code>java.lang.Throwable</code> one will always get precedence over all others. When you have error pages for
 * specific exception types, then you'd better use the <code>500</code> one as fallback error page.
 * <pre>
 * &lt;error-page&gt;
 *     &lt;error-code&gt;500&lt;/error-code&gt;
 *     &lt;location&gt;/WEB-INF/errorpages/500.xhtml&lt;/location&gt;
 * &lt;/error-page&gt;
 * </pre>
 * <p>
 * The exception detail is available in the request scope by the standard Servlet error request attributes like as in a
 * normal synchronous error page response. You could for example show them in the error page as follows:
 * <pre>
 * &lt;ul&gt;
 *     &lt;li&gt;Date/time: #{of:formatDate(now, 'yyyy-MM-dd HH:mm:ss')}&lt;/li&gt;
 *     &lt;li&gt;User agent: #{header['user-agent']}&lt;/li&gt;
 *     &lt;li&gt;User IP: #{request.remoteAddr}&lt;/li&gt;
 *     &lt;li&gt;Request URI: #{requestScope['jakarta.servlet.error.request_uri']}&lt;/li&gt;
 *     &lt;li&gt;Ajax request: #{facesContext.partialViewContext.ajaxRequest ? 'Yes' : 'No'}&lt;/li&gt;
 *     &lt;li&gt;Status code: #{requestScope['jakarta.servlet.error.status_code']}&lt;/li&gt;
 *     &lt;li&gt;Exception type: #{requestScope['jakarta.servlet.error.exception_type']}&lt;/li&gt;
 *     &lt;li&gt;Exception message: #{requestScope['jakarta.servlet.error.message']}&lt;/li&gt;
 *     &lt;li&gt;Exception UUID: #{requestScope['org.omnifaces.exception_uuid']}&lt;/li&gt;
 *     &lt;li&gt;Stack trace:
 *         &lt;pre&gt;#{of:printStackTrace(requestScope['jakarta.servlet.error.exception'])}&lt;/pre&gt;
 *     &lt;/li&gt;
 * &lt;/ul&gt;
 * </pre>
 * <p>
 * Exceptions during render response can only be handled when the <code>jakarta.faces.FACELETS_BUFFER_SIZE</code> is
 * large enough so that the so far rendered response until the occurrence of the exception fits in there and can
 * therefore safely be resetted.
 *
 * <h2>Error in error page itself</h2>
 * <p>
 * When the rendering of the error page failed due to a bug in the error page itself, and the response can still be
 * resetted, then the {@link FullAjaxExceptionHandler} will display a hardcoded error message in "plain text" informing
 * the developer about the double mistake.
 *
 * <h2>Normal requests</h2>
 * <p>
 * Note that the {@link FullAjaxExceptionHandler} does not deal with normal (non-ajax) requests at all. To properly
 * handle Faces and EL exceptions on normal requests as well, you need an additional {@link FacesExceptionFilter}. This
 * will extract the root cause from a wrapped {@link FacesException} and {@link ELException} before delegating the
 * {@link ServletException} further to the container (the container will namely use the first root cause of
 * {@link ServletException} to match an error page by exception in web.xml).
 * <p>
 * Before OmniFaces 4.5, you needed to explicitly register the {@link FacesExceptionFilter} in {@code web.xml}. Since
 * OmniFaces 4.5, the {@link FullAjaxExceptionHandler} will automatically register the {@link FacesExceptionFilter} on
 * its default URL pattern of {@code /*} when it is absent in {@code web.xml}. In case you wish to map it on a different
 * URL pattern for some reason, then you'll still need to explicitly register it in {@code web.xml}.
 *
 * <h2>Configuration</h2>
 * <p>
 * By default only {@link FacesException} and {@link ELException} are unwrapped. You can supply a context parameter
 * {@value org.omnifaces.exceptionhandler.FullAjaxExceptionHandler#PARAM_NAME_EXCEPTION_TYPES_TO_UNWRAP} to specify
 * additional exception types to unwrap. The context parameter value must be a commaseparated string of fully qualified
 * names of additional exception types. Note that this also covers subclasses of specified exception types.
 * <pre>
 * &lt;context-param&gt;
 *     &lt;param-name&gt;org.omnifaces.EXCEPTION_TYPES_TO_UNWRAP&lt;/param-name&gt;
 *     &lt;param-value&gt;jakarta.ejb.EJBException,jakarta.persistence.RollbackException&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * </pre>
 * <p>
 * This context parameter will also be read and used by {@link FacesExceptionFilter}.
 * <p>
 * By default all exceptions are logged. You can supply a context parameter
 * {@value org.omnifaces.exceptionhandler.FullAjaxExceptionHandler#PARAM_NAME_EXCEPTION_TYPES_TO_IGNORE_IN_LOGGING} to
 * specify exception types to ignore from logging. The context parameter value must be a commaseparated string of fully
 * qualified names of exception types. Note that this also covers subclasses of specified exception types.
 * <pre>
 * &lt;context-param&gt;
 *     &lt;param-name&gt;org.omnifaces.EXCEPTION_TYPES_TO_IGNORE_IN_LOGGING&lt;/param-name&gt;
 *     &lt;param-value&gt;jakarta.faces.application.ViewExpiredException&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * </pre>
 * <p>
 * This context parameter will also be read and used by {@link FacesExceptionFilter}.
 * <p>
 * This context parameter will <strong>not</strong> suppress standard Faces and/or container builtin logging. This will
 * only suppress <code>org.omnifaces.exceptionhandler.FullAjaxExceptionHandler</code> logging. So chances are that
 * standard Faces and/or container will still log it. This may need to be configured separately.
 *
 * <h2>Customizing <code>FullAjaxExceptionHandler</code></h2>
 * <p>
 * If more fine grained control is desired for determining the root cause of the caught exception, or whether it should
 * be handled, or determining the error page, or logging the exception, then the developer can opt to extend this
 * {@link FullAjaxExceptionHandler} and override one or more of the following protected methods:
 * <ul>
 * <li>{@link #findExceptionRootCause(FacesContext, Throwable)}
 * <li>{@link #shouldHandleExceptionRootCause(FacesContext, Throwable)}
 * <li>{@link #findErrorPageLocation(FacesContext, Throwable)}
 * <li>{@link #logException(FacesContext, Throwable, String, LogReason)}
 * <li>{@link #logException(FacesContext, Throwable, String, String, Object...)}
 * </ul>
 * <p>
 * Don't forget to create a custom {@link ExceptionHandlerFactory} for it as well, so that it could be registered
 * in <code>faces-config.xml</code>. This does not necessarily need to extend from
 * {@link FullAjaxExceptionHandlerFactory}.
 *
 * @author Bauke Scholtz
 * @see FullAjaxExceptionHandlerFactory
 * @see OmniPartialViewContext
 * @see OmniPartialViewContextFactory
 * @see WebXml
 * @see FacesExceptionFilter
 */
public class FullAjaxExceptionHandler extends ExceptionHandlerWrapper {

    // Public constants -----------------------------------------------------------------------------------------------

    /**
     * The context parameter name to specify additional exception types to unwrap by both {@link FullAjaxExceptionHandler}
     * and {@link FacesExceptionFilter}. Those will be added to exception types {@link FacesException} and {@link ELException}.
     * @since 2.3
     */
    public static final String PARAM_NAME_EXCEPTION_TYPES_TO_UNWRAP =
        "org.omnifaces.EXCEPTION_TYPES_TO_UNWRAP";

    /**
     * The context parameter name to specify exception types to ignore in logging by both {@link FullAjaxExceptionHandler}
     * and {@link FacesExceptionFilter}.
     * @since 2.5
     */
    public static final String PARAM_NAME_EXCEPTION_TYPES_TO_IGNORE_IN_LOGGING =
        "org.omnifaces.EXCEPTION_TYPES_TO_IGNORE_IN_LOGGING";

    /**
     * The request attribute name of the UUID of the thrown exception which is logged by both {@link FullAjaxExceptionHandler}
     * and {@link FacesExceptionFilter}.
     * @since 3.2
     */
    public static final String EXCEPTION_UUID =
        "org.omnifaces.exception_uuid";

    /**
     * This is used in {@link FullAjaxExceptionHandler#logException(FacesContext, Throwable, String, LogReason)}.
     *
     * @author Bauke Scholtz
     * @since 2.4
     */
    protected enum LogReason {
        /** An exception occurred during processing Faces ajax request. Error page will be shown. */
        EXCEPTION_HANDLED(LOG_EXCEPTION_HANDLED),

        /** An exception occurred during rendering Faces ajax request. Error page will be shown. */
        RENDER_EXCEPTION_HANDLED(LOG_RENDER_EXCEPTION_HANDLED),

        /** An exception occurred during rendering Faces ajax request. Error page CANNOT be shown as response is already committed. */
        RENDER_EXCEPTION_UNHANDLED(LOG_RENDER_EXCEPTION_UNHANDLED),

        /** Another exception occurred during rendering error page. A hardcoded error page will be shown. */
        ERROR_PAGE_ERROR(LOG_ERROR_PAGE_ERROR);

        private final String message;

        LogReason(String message) {
            this.message = message;
        }

        /**
         * Returns the default message associated with the log reason.
         * @return The default message associated with the log reason.
         */
        public String getMessage() {
            return message;
        }
    }

    // Private constants ----------------------------------------------------------------------------------------------

    private static final Logger logger = Logger.getLogger(FullAjaxExceptionHandler.class.getName());

    private static final Set<Class<? extends Throwable>> STANDARD_TYPES_TO_UNWRAP =
        unmodifiableSet(FacesException.class, ELException.class);

    private static final String FACES_EXCEPTION_FILTER_AUTO_INSTALLED =
        "FullAjaxExceptionHandler: the FacesExceptionFilter has been automatically installed at URL pattern of /*";
    private static final String ERROR_INVALID_EXCEPTION_TYPES_PARAM_CLASS =
        "Context parameter '%s' references a class which cannot be found in runtime classpath: '%s'";
    private static final String ERROR_DEFAULT_LOCATION_MISSING =
        "Either HTTP 500 or java.lang.Throwable error page is required in web.xml or web-fragment.xml."
            + " Neither was found.";
    private static final String LOG_EXCEPTION_HANDLED =
        "FullAjaxExceptionHandler: An exception occurred during processing Faces ajax request."
            + " Error page '%s' will be shown.";
    private static final String LOG_RENDER_EXCEPTION_HANDLED =
        "FullAjaxExceptionHandler: An exception occurred during rendering Faces ajax response."
            + " Error page '%s' will be shown.";
    private static final String LOG_RENDER_EXCEPTION_UNHANDLED =
        "FullAjaxExceptionHandler: An exception occurred during rendering Faces ajax response."
            + " Error page '%s' CANNOT be shown as response is already committed."
            + " Consider increasing 'jakarta.faces.FACELETS_BUFFER_SIZE' if it really needs to be handled.";
    private static final String LOG_ERROR_PAGE_ERROR =
        "FullAjaxExceptionHandler: Well, another exception occurred during rendering error page '%s'."
            + " Trying to render a hardcoded error page now.";
    private static final String ERROR_PAGE_ERROR =
        "<?xml version='1.0' encoding='UTF-8'?><partial-response id='error'><changes><update id='jakarta.faces.ViewRoot'>"
            + "<![CDATA[<html lang='en'><head><title>Error in error</title></head><body><section><h2>Oops!</h2>"
            + "<p>A problem occurred during processing the ajax request. Subsequently, another problem occurred during"
            + " processing the error page which should inform you about that problem.</p><p>If you are the responsible"
            + " web developer, it's time to read the server logs about the bug in the error page itself.</p></section>"
            + "</body></html>]]></update></changes></partial-response>";

    // Variables ------------------------------------------------------------------------------------------------------

    private final Class<? extends Throwable>[] exceptionTypesToUnwrap;
    private final Class<? extends Throwable>[] exceptionTypesToIgnoreInLogging;

    // Initialization -------------------------------------------------------------------------------------------------

    /**
     * This will register the {@link FacesExceptionFilter} when {@link FullAjaxExceptionHandler} is explicitly registered.
     * This is invoked by {@link ApplicationListener}.
     * @param servletContext The involved servlet context.
     * @since 4.5
     */
    public static void registerFacesExceptionFilterIfNecessary(ServletContext servletContext) {
        if (FacesConfigXml.instance().getExceptionHandlerFactories().stream()
                .noneMatch(FullAjaxExceptionHandlerFactory.class::equals) || servletContext.getFilterRegistrations().values().stream()
                .map(FilterRegistration::getClassName)
                .map(Reflection::toClassOrNull).filter(Objects::nonNull)
                .anyMatch(FacesExceptionFilter.class::equals))
        {
            return; // FacesExceptionFilter is already explicitly registered.
        }

        servletContext
            .addFilter(FacesExceptionFilter.class.getName(), FacesExceptionFilter.class)
            .addMappingForUrlPatterns(EnumSet.of(REQUEST), false, "/*");
        logger.log(INFO, FACES_EXCEPTION_FILTER_AUTO_INSTALLED);
    }

    // Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Construct a new ajax exception handler around the given wrapped exception handler.
     * @param wrapped The wrapped exception handler.
     */
    public FullAjaxExceptionHandler(ExceptionHandler wrapped) {
        super(wrapped);
        exceptionTypesToUnwrap = getExceptionTypesToUnwrap(getServletContext());
        exceptionTypesToIgnoreInLogging = getExceptionTypesToIgnoreInLogging(getServletContext());
    }

    /**
     * Get the exception types to unwrap. This contains at least the standard types to unwrap {@link FacesException} and
     * {@link ELException}. Additional types can be specified via context parameter
     * {@value org.omnifaces.exceptionhandler.FullAjaxExceptionHandler#PARAM_NAME_EXCEPTION_TYPES_TO_UNWRAP}, if any.
     * @param context The involved servlet context.
     * @return Exception types to unwrap.
     * @since 2.3
     */
    public static Class<? extends Throwable>[] getExceptionTypesToUnwrap(ServletContext context) {
        return parseExceptionTypesParam(context, PARAM_NAME_EXCEPTION_TYPES_TO_UNWRAP, STANDARD_TYPES_TO_UNWRAP);
    }

    /**
     * Get the exception types to ignore in logging. This can be specified via context parameter
     * {@value org.omnifaces.exceptionhandler.FullAjaxExceptionHandler#PARAM_NAME_EXCEPTION_TYPES_TO_IGNORE_IN_LOGGING}.
     * @param context The involved servlet context.
     * @return Exception types to ignore in logging.
     * @since 2.5
     */
    public static Class<? extends Throwable>[] getExceptionTypesToIgnoreInLogging(ServletContext context) {
        return parseExceptionTypesParam(context, PARAM_NAME_EXCEPTION_TYPES_TO_IGNORE_IN_LOGGING, null);
    }

    @SuppressWarnings("unchecked")
    static Class<? extends Throwable>[] parseExceptionTypesParam(ServletContext context, String paramName, Set<Class<? extends Throwable>> defaults) {
        var types = new HashSet<Class<? extends Throwable>>();

        if (defaults != null) {
            types.addAll(defaults);
        }

        var typesParam = context.getInitParameter(paramName);

        if (!isEmpty(typesParam)) {
            splitAndTrim(typesParam, ",").forEach(typeParam -> {
                try {
                    types.add((Class<? extends Throwable>) Class.forName(typeParam));
                }
                catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException(
                        format(ERROR_INVALID_EXCEPTION_TYPES_PARAM_CLASS, paramName, typeParam), e);
                }
            });
        }

        return types.toArray(new Class[types.size()]);
    }

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * Handle the ajax exception as follows, only and only if the current request is an ajax request with an uncommitted
     * response and there is at least one unhandled exception:
     * <ul>
     *   <li>Find the root cause of the exception by {@link #findExceptionRootCause(FacesContext, Throwable)}.
     *   <li>Find the error page location based on root cause by {@link #findErrorPageLocation(FacesContext, Throwable)}.
     *   <li>Set the standard servlet error request attributes.
     *   <li>Force Faces to render the full error page in its entirety.
     * </ul>
     * Any remaining unhandled exceptions will be swallowed. Only the first one is relevant.
     */
    @Override
    public void handle() {
        handleAjaxException(getContext());
        getWrapped().handle();
    }

    private void handleAjaxException(FacesContext context) {
        if (context == null) {
            return; // Unexpected, most likely buggy Faces implementation or parent exception handler.
        }

        var unhandledExceptionQueuedEvents = getUnhandledExceptionQueuedEvents().iterator();

        if (!unhandledExceptionQueuedEvents.hasNext()) {
            return; // There's no unhandled exception.
        }

        var exception = unhandledExceptionQueuedEvents.next().getContext().getException();

        if (exception instanceof AbortProcessingException) {
            return; // Let Faces handle it itself.
        }

        exception = findExceptionRootCause(context, exception);
        unhandledExceptionQueuedEvents.remove();

        if (!shouldHandleExceptionRootCause(context, exception)) {
            return; // A subclass apparently want to do it differently.
        }

        var errorPageLocation = findErrorPageLocation(context, exception);

        if (errorPageLocation == null) {
            throw new IllegalArgumentException(ERROR_DEFAULT_LOCATION_MISSING);
        }

        if (!context.getPartialViewContext().isAjaxRequest()) {
            throw new FacesException(exception); // Not an ajax request, let default web.xml error page mechanism or FacesExceptionFilter do its job.
        }

        if (!canRenderErrorPageView(context, exception, errorPageLocation)) {
            return; // If error page cannot be rendered, then it's end of story.
        }

        // Set the necessary servlet request attributes which a bit decent error page may expect.
        var request = getRequest(context);
        request.setAttribute(ERROR_EXCEPTION, exception);
        request.setAttribute(ERROR_EXCEPTION_TYPE, exception.getClass());
        request.setAttribute(ERROR_MESSAGE, exception.getMessage());
        request.setAttribute(ERROR_REQUEST_URI, request.getRequestURI());
        request.setAttribute(ERROR_STATUS_CODE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        try {
            renderErrorPageView(context, request, errorPageLocation);
        }
        catch (IOException e) {
            throw new FacesException(e);
        }

        while (unhandledExceptionQueuedEvents.hasNext()) {
            // Any remaining unhandled exceptions are not interesting. First fix the first.
            unhandledExceptionQueuedEvents.next();
            unhandledExceptionQueuedEvents.remove();
        }
    }

    /**
     * Determine the root cause based on the caught exception, which will then be used to find the error page location.
     * The default implementation delegates to {@link Exceptions#unwrap(Throwable, Class...)} with {@link FacesException},
     * {@link ELException} and the types specified in context parameter
     * {@value org.omnifaces.exceptionhandler.FullAjaxExceptionHandler#PARAM_NAME_EXCEPTION_TYPES_TO_UNWRAP}, if any.
     * @param context The involved faces context.
     * @param exception The caught exception to determine the root cause for.
     * @return The root cause of the caught exception.
     * @since 1.5
     */
    protected Throwable findExceptionRootCause(FacesContext context, Throwable exception) {
        return unwrap(exception, exceptionTypesToUnwrap);
    }

    /**
     * Returns <code>true</code> if the {@link FullAjaxExceptionHandler} should handle this exception root cause. If
     * this returns <code>false</code>, then the {@link FullAjaxExceptionHandler} will skip handling this exception and
     * delegate it further to the wrapped exception handler. The default implementation just returns <code>true</code>.
     * @param context The involved faces context.
     * @param exception The caught exception to determine the root cause for.
     * @return <code>true</code> if the given exception should be handled by the {@link FullAjaxExceptionHandler}.
     * @since 1.8
     */
    protected boolean shouldHandleExceptionRootCause(FacesContext context, Throwable exception) {
        return true;
    }

    /**
     * Determine the error page location based on the given exception.
     * The default implementation delegates to {@link WebXml#findErrorPageLocation(Throwable)}.
     * @param context The involved faces context.
     * @param exception The exception to determine the error page for.
     * @return The location of the error page. It must start with <code>/</code> and be relative to the context path.
     * @since 1.5
     */
    protected String findErrorPageLocation(FacesContext context, Throwable exception) {
        return WebXml.instance().findErrorPageLocation(exception);
    }

    /**
     * Log the thrown exception and determined error page location for the given log reason.
     * The default implementation delegates to {@link #logException(FacesContext, Throwable, String, String, Object...)}
     * with the default message associated with the log reason.
     * @param context The involved faces context.
     * @param exception The exception to log.
     * @param location The error page location.
     * @param reason The log reason.
     * @since 2.4
     */
    protected void logException(FacesContext context, Throwable exception, String location, LogReason reason) {
        logException(context, exception, location, reason.getMessage(), location);
    }

    /**
     * Log the thrown exception and determined error page location with the given message, optionally parameterized
     * with the given parameters.
     * The default implementation logs through <code>java.util.logging</code> as SEVERE when the thrown exception is
     * not an instance of any type specified in context parameter
     * {@value org.omnifaces.exceptionhandler.FullAjaxExceptionHandler#PARAM_NAME_EXCEPTION_TYPES_TO_IGNORE_IN_LOGGING}.
     * Since version 3.2, the log message will be prepended with the UUID and IP address.
     * The UUID is available in EL by <code>#{requestScope['org.omnifaces.exception_uuid']}</code>.
     * @param context The involved faces context.
     * @param exception The exception to log.
     * @param location The error page location.
     * @param message The log message.
     * @param parameters The log message parameters, if any. They are formatted using {@link Formatter}.
     * @since 1.6
     */
    protected void logException(FacesContext context, Throwable exception, String location, String message, Object... parameters) {
        if (!isOneInstanceOf(exception.getClass(), exceptionTypesToIgnoreInLogging)) {
            logger.log(SEVERE, format("[%s][%s] %s", getRequestAttribute(context, EXCEPTION_UUID), getRemoteAddr(context), format(message, parameters)), exception);
        }
    }

    private boolean canRenderErrorPageView(FacesContext context, Throwable exception, String errorPageLocation) {
        setRequestAttribute(context, EXCEPTION_UUID, UUID.randomUUID().toString());

        if (context.getCurrentPhaseId() != PhaseId.RENDER_RESPONSE) {
            logException(context, exception, errorPageLocation, LogReason.EXCEPTION_HANDLED);
            return true;
        }
        else if (!context.getExternalContext().isResponseCommitted()) {
            logException(context, exception, errorPageLocation, LogReason.RENDER_EXCEPTION_HANDLED);
            resetResponse(context); // If the exception was thrown in midst of rendering the Faces response, then reset (partial) response.
            return true;
        }
        else {
            logException(context, exception, errorPageLocation, LogReason.RENDER_EXCEPTION_UNHANDLED);

            // Mojarra doesn't close the partial response during render exception. Let do it ourselves.
            OmniPartialViewContext.getCurrentInstance(context).closePartialResponse();
            return false;
        }
    }

    private void renderErrorPageView(FacesContext context, HttpServletRequest request, String errorPageLocation)
        throws IOException
    {
        var viewId = getViewIdAndPrepareParamsIfNecessary(context, errorPageLocation);
        var viewHandler = context.getApplication().getViewHandler();
        var viewRoot = viewHandler.createView(context, viewId);
        Hacks.removeResourceDependencyState(context);
        context.setViewRoot(viewRoot);
        context.getPartialViewContext().setRenderAll(true);

        try {
            var vdl = viewHandler.getViewDeclarationLanguage(context, viewId);
            vdl.buildView(context, viewRoot);
            context.getApplication().publishEvent(context, PreRenderViewEvent.class, viewRoot);
            vdl.renderView(context, viewRoot);
            context.responseComplete();
        }
        catch (Exception e) {
            // Apparently, the error page itself contained an error.
            logException(context, e, errorPageLocation, LogReason.ERROR_PAGE_ERROR);
            var externalContext = context.getExternalContext();

            if (!externalContext.isResponseCommitted()) {
                // Okay, reset the response and tell that the error page itself contained an error.
                resetResponse(context);
                externalContext.setResponseContentType("text/xml");
                externalContext.getResponseOutputWriter().write(ERROR_PAGE_ERROR);
                context.responseComplete();
            }
            else {
                // Well, it's too late to handle. Just let it go.
                throw new FacesException(e);
            }
        }
        finally {
            // Prevent some servlet containers from handling error page itself afterwards. So far Tomcat/JBoss
            // are known to do that. It would only result in IllegalStateException "response already committed"
            // or "getOutputStream() has already been called for this response".
            request.removeAttribute(ERROR_EXCEPTION);
        }
    }

    private static String getViewIdAndPrepareParamsIfNecessary(FacesContext context, String errorPageLocation) {
        var parts = errorPageLocation.split("\\?", 2);

        // TODO: #287: make params available via #{param(Values)}. Request wrapper needed :|

        return normalizeViewId(context, parts[0]);
    }

}