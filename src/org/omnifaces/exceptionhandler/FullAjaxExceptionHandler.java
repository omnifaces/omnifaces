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
package org.omnifaces.exceptionhandler;

import java.io.IOException;
import java.util.Iterator;

import javax.faces.FacesException;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.view.ViewDeclarationLanguage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.omnifaces.config.WebXml;
import org.omnifaces.context.OmniPartialViewContext;
import org.omnifaces.util.Exceptions;
import org.omnifaces.util.Faces;

/**
 * This exception handler enables you to show the full error page in its entirety to the enduser in case of exceptions
 * during ajax requests. Refer the documentation of {@link FullAjaxExceptionHandlerFactory} how to setup it.
 * <p>
 * This exception handler will parse the <tt>web.xml</tt> and <tt>web-fragment.xml</tt> files to find the error page
 * locations of the HTTP error code <tt>500</tt> and all declared specific exception types. Those locations need to
 * point to Facelets files. The location of the HTTP error code <tt>500</tt> or the exception type
 * <code>java.lang.Throwable</code> is required in order to get the full ajax exception handler to work, because
 * there's then at least a fall back error page whenever there's no match with any of the declared specific exceptions.
 * So, you must at least have either
 * <pre>
 * &lt;error-page&gt;
 *   &lt;error-code&gt;500&lt;/error-code&gt;
 *   &lt;location&gt;/errors/500.xhtml&lt;/location&gt;
 * &lt;/error-page&gt;
 * </pre>
 * <p>or
 * <pre>
 * &lt;error-page&gt;
 *   &lt;exception-type&gt;java.lang.Throwable&lt;/exception-type&gt;
 *   &lt;location&gt;/errors/500.xhtml&lt;/location&gt;
 * &lt;/error-page&gt;
 * </pre>
 * <p>
 * Both can also, only the <code>java.lang.Throwable</code> one will always get precedence over the <tt>500</tt>
 * one, as per the Servlet API specification, so the <tt>500</tt> one would be basically superfluous.
 * <p>
 * The exception detail is available in the request scope by the standard servlet error request attributes like as in a
 * normal synchronous error page response. You could for example show them in the error page as follows:
 * <pre>
 * &lt;ul&gt;
 *   &lt;li&gt;Date/time: #{of:formatDate(now, 'yyyy-MM-dd HH:mm:ss')}&lt;/li&gt;
 *   &lt;li&gt;User agent: #{header['user-agent']}&lt;/li&gt;
 *   &lt;li&gt;User IP: #{request.remoteAddr}&lt;/li&gt;
 *   &lt;li&gt;Request URI: #{requestScope['javax.servlet.error.request_uri']}&lt;/li&gt;
 *   &lt;li&gt;Ajax request: #{facesContext.partialViewContext.ajaxRequest ? 'Yes' : 'No'}&lt;/li&gt;
 *   &lt;li&gt;Status code: #{requestScope['javax.servlet.error.status_code']}&lt;/li&gt;
 *   &lt;li&gt;Exception type: #{requestScope['javax.servlet.error.exception_type']}&lt;/li&gt;
 *   &lt;li&gt;Exception message: #{requestScope['javax.servlet.error.message']}&lt;/li&gt;
 *   &lt;li&gt;Stack trace:
 *     &lt;pre&gt;#{of:printStackTrace(requestScope['javax.servlet.error.exception'])}&lt;/pre&gt;
 *   &lt;/li&gt;
 * &lt;/ul&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 * @see FullAjaxExceptionHandlerFactory
 */
public class FullAjaxExceptionHandler extends ExceptionHandlerWrapper {

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String ERROR_DEFAULT_LOCATION_MISSING =
		"Either HTTP 500 or java.lang.Throwable error page is required in web.xml or web-fragment.xml."
			+ " Neither was found.";
	private static final String LOG_EXCEPTION_OCCURRED =
		"An exception occurred during JSF ajax request. Showing error page location '%s'.";

	// Yes, those are copies of Servlet 3.0 RequestDispatcher constant field values.
	// They are hardcoded to maintain Servlet 2.5 compatibility.
	private static final String ATTRIBUTE_ERROR_EXCEPTION = "javax.servlet.error.exception";
	private static final String ATTRIBUTE_ERROR_EXCEPTION_TYPE = "javax.servlet.error.exception_type";
	private static final String ATTRIBUTE_ERROR_MESSAGE = "javax.servlet.error.message";
	private static final String ATTRIBUTE_ERROR_REQUEST_URI = "javax.servlet.error.request_uri";
	private static final String ATTRIBUTE_ERROR_STATUS_CODE = "javax.servlet.error.status_code";

	// Variables ------------------------------------------------------------------------------------------------------

	private ExceptionHandler wrapped;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new ajax exception handler around the given wrapped exception handler.
	 * @param wrapped The wrapped exception handler.
	 */
	public FullAjaxExceptionHandler(ExceptionHandler wrapped) {
		this.wrapped = wrapped;
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Handle the ajax exception as follows, only and only if the current request is an ajax request and there is at
	 * least one unhandled exception:
	 * <ul>
	 *   <li>If the exception is an instance of {@link FacesException}, then unwrap its root cause as long as it is not
	 *       an instance of {@link FacesException}.
	 *   <li>Find the error page location by {@link WebXml#findErrorPageLocation(Throwable)}.
	 *   <li>Set the standard servlet error request attributes.
	 *   <li>Force JSF to render the full error page in its entirety.
	 * </ul>
	 * Any remaining unhandled exceptions will be swallowed. Only the first one is relevant.
	 */
	@Override
	public void handle() throws FacesException {
		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();

		if (context.getPartialViewContext().isAjaxRequest() && !externalContext.isResponseCommitted()) {
			Iterator<ExceptionQueuedEvent> unhandledExceptionQueuedEvents = getUnhandledExceptionQueuedEvents().iterator();

			if (unhandledExceptionQueuedEvents.hasNext()) {
				Throwable exception = unhandledExceptionQueuedEvents.next().getContext().getException();
				unhandledExceptionQueuedEvents.remove();

				// Unwrap root cause of FacesException and find error page location for the unwrapped root cause.
				exception = Exceptions.unwrap(exception, FacesException.class);
				String errorPageLocation = WebXml.INSTANCE.findErrorPageLocation(exception);

				// If there's no default error page location, well, it's then end of story.
				if (errorPageLocation == null) {
					throw new IllegalArgumentException(ERROR_DEFAULT_LOCATION_MISSING);
				}

				// Log the exception to server log like as in a normal synchronous HTTP 500 error page response.
				externalContext.log(String.format(LOG_EXCEPTION_OCCURRED, errorPageLocation), exception);

				// Set the necessary servlet request attributes which a bit decent error page may expect.
				HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
				request.setAttribute(ATTRIBUTE_ERROR_EXCEPTION, exception);
				request.setAttribute(ATTRIBUTE_ERROR_EXCEPTION_TYPE, exception.getClass());
				request.setAttribute(ATTRIBUTE_ERROR_MESSAGE, exception.getMessage());
				request.setAttribute(ATTRIBUTE_ERROR_REQUEST_URI, request.getRequestURI());
				request.setAttribute(ATTRIBUTE_ERROR_STATUS_CODE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

				// If the exception was thrown in midst of rendering the JSF response, then reset (partial) response.
				if (context.getRenderResponse()) {
					externalContext.responseReset();
					OmniPartialViewContext.getCurrentInstance().resetPartialResponse();
				}

				// Force JSF to render the error page in its entirety to the ajax response.
				// Note that we cannot set response status code to 500, the JSF ajax response won't be processed then.
				String viewId = Faces.normalizeViewId(errorPageLocation);
				ViewHandler viewHandler = context.getApplication().getViewHandler();
				UIViewRoot viewRoot = viewHandler.createView(context, viewId);
				context.setViewRoot(viewRoot);
				context.getPartialViewContext().setRenderAll(true);
            	ViewDeclarationLanguage vdl = viewHandler.getViewDeclarationLanguage(context, viewId);

	            try {
	            	vdl.buildView(context, viewRoot);
	            	vdl.renderView(context, viewRoot);
	            }
	            catch (IOException e) {
	            	throw new FacesException(e);
	            }
	            finally {
					// Prevent some servlet containers from handling error page itself afterwards. So far Tomcat/JBoss
					// are known to do that. It would only result in IllegalStateException "response already committed".
					request.removeAttribute(ATTRIBUTE_ERROR_EXCEPTION);
	            }

	            // Inform JSF that we've completed the response ourselves.
				context.responseComplete();
			}

			while (unhandledExceptionQueuedEvents.hasNext()) {
				// Any remaining unhandled exceptions are not interesting. First fix the first.
				unhandledExceptionQueuedEvents.next();
				unhandledExceptionQueuedEvents.remove();
			}

		}

		wrapped.handle();
	}

	@Override
	public ExceptionHandler getWrapped() {
		return wrapped;
	}

}