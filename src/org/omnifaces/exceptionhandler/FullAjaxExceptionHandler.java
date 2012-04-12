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
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.FacesException;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.context.FacesContext;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.PhaseId;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.omnifaces.util.Events;
import org.omnifaces.util.Exceptions;
import org.omnifaces.util.Faces;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This exception handler enables you to show the full error page in its entirety to the enduser in case of exceptions
 * during ajax requests. Refer the documentation of {@link FullAjaxExceptionHandlerFactory} how to setup it.
 * <p>
 * This exception handler will parse the <tt>web.xml</tt> to find the error page locations of the HTTP error
 * code <tt>500</tt> and all declared specific exception types. Those locations need to point to Facelets files. The
 * location of the HTTP error code <tt>500</tt> or the exception type <code>java.lang.Throwable</code> is required in
 * order to get the full ajax exception handler to work, because there's then at least a fall back error page whenever
 * there's no match with any of the declared specific exceptions. So, you must at least have either
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

	private static final String WEB_XML =
		"/WEB-INF/web.xml";
	private static final String ERROR_LOCATION_INVALID =
		"Error page location '%s' in web.xml is invalid. Resource resolved to null.";
	private static final String ERROR_DEFAULT_LOCATION_MISSING =
		"Either HTTP 500 or java.lang.Throwable error page is required in web.xml. Neither was found.";
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
	private Map<Class<Throwable>, String> errorPageLocations;

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
	 *   <li>Find the error page location as per Servlet specification 10.9.2:
	 *     <ul>
	 *       <li>Make a first pass through all specific exception types. If an exact match is found, use its location.
	 *       <li>Else make a second pass through all specific exception types in the order as they are declared in
	 *           web.xml. If the current exception is an instance of it, then use its location.
	 *       <li>Else use the default error page location, which can be either the HTTP 500 or java.lang.Throwable one.
	 *     </ul>
	 *   <li>Set the standard servlet error request attributes.
	 *   <li>Force JSF to render the full error page in its entirety.
	 * </ul>
	 * Any remaining unhandled exceptions will be swallowed. Only the first one is relevant.
	 */
	@Override
	public void handle() throws FacesException {
		FacesContext context = FacesContext.getCurrentInstance();

		if (context.getPartialViewContext().isAjaxRequest()) {
			Iterator<ExceptionQueuedEvent> unhandledExceptionQueuedEvents = getUnhandledExceptionQueuedEvents().iterator();

			if (unhandledExceptionQueuedEvents.hasNext()) {
				Throwable exception = unhandledExceptionQueuedEvents.next().getContext().getException();
				unhandledExceptionQueuedEvents.remove();

				// If the exception is wrapped in a FacesException, unwrap the root cause.
				exception = Exceptions.unwrap(exception, FacesException.class);

				// Find the error page location for the given exception as per Servlet specification 10.9.2.
				String errorPageLocation = findErrorPageLocation(exception);

				// Log the exception to server log like as in a normal synchronous HTTP 500 error page response.
				context.getExternalContext().log(String.format(LOG_EXCEPTION_OCCURRED, errorPageLocation), exception);

				// Set the necessary servlet request attributes which a bit decent error page may expect.
				final HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
				request.setAttribute(ATTRIBUTE_ERROR_EXCEPTION, exception);
				request.setAttribute(ATTRIBUTE_ERROR_EXCEPTION_TYPE, exception.getClass());
				request.setAttribute(ATTRIBUTE_ERROR_MESSAGE, exception.getMessage());
				request.setAttribute(ATTRIBUTE_ERROR_REQUEST_URI, request.getRequestURI());
				request.setAttribute(ATTRIBUTE_ERROR_STATUS_CODE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

				// Force JSF to render the error page in its entirety to the ajax response.
				context.setViewRoot(context.getApplication().getViewHandler().createView(context, errorPageLocation));
				context.getPartialViewContext().setRenderAll(true);
				context.renderResponse();

				// Prevent some servlet containers from handling the error page itself afterwards. So far Tomcat/JBoss
				// are known to do that. It would only result in IllegalStateException "response already committed".
				Events.addAfterPhaseListener(PhaseId.RENDER_RESPONSE, new Runnable() {
					@Override
					public void run() {
						request.removeAttribute(ATTRIBUTE_ERROR_EXCEPTION);
					}
				});

				// Note that we cannot set response status code to 500, the JSF ajax response won't be processed then.
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

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Find for the given exception the right error page location as per Servlet specification 10.9.2:
	 * <ul>
	 *   <li>Make a first pass through all specific exception types. If an exact match is found, use its location.
	 *   <li>Else make a second pass through all specific exception types in the order as they are declared in
	 *       web.xml. If the current exception is an instance of it, then use its location.
	 *   <li>Else use the default error page location, which can be either the HTTP 500 or java.lang.Throwable one.
	 * </ul>
	 * @param throwable The exception to find the error page location for.
	 * @return The right error page location for the given exception.
	 */
	private String findErrorPageLocation(Throwable exception) {
		if (errorPageLocations == null) {
			// #6: It isn't possible to perform this on webapp's startup which would be more ideal.
			errorPageLocations = findErrorPageLocations();
		}

		for (Entry<Class<Throwable>, String> entry : errorPageLocations.entrySet()) {
			if (entry.getKey() == exception.getClass()) {
				return entry.getValue();
			}
		}

		for (Entry<Class<Throwable>, String> entry : errorPageLocations.entrySet()) {
			if (entry.getKey() != null && entry.getKey().isInstance(exception)) {
				return entry.getValue();
			}
		}

		return errorPageLocations.get(null);
	}

	/**
	 * Parse <tt>web.xml</tt> and find all error page locations.
	 * @return An ordered map of all error page locations. The key <code>null</code> represents the default location.
	 * @throws IllegalArgumentException When an error page location in <tt>web.xml</tt> is missing or invalid.
	 */
	@SuppressWarnings("unchecked") // For the cast on Class<Throwable>.
	private static Map<Class<Throwable>, String> findErrorPageLocations() {
		Map<Class<Throwable>, String> errorPageLocations = new LinkedHashMap<Class<Throwable>, String>();
		String defaultLocation = null;
		InputStream input = null;

		try {
			input = Faces.getResourceAsStream(WEB_XML);

			if (input != null) { // Since Servlet 3.0, web.xml is optional.
				Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
				XPath xpath = XPathFactory.newInstance().newXPath();
				defaultLocation =
					xpath.compile("web-app/error-page[error-code=500]/location").evaluate(document).trim();
				NodeList exceptionTypes = (NodeList)
					xpath.compile("web-app/error-page/exception-type").evaluate(document, XPathConstants.NODESET);

				for (int i = 0; i < exceptionTypes.getLength(); i++) {
					Node node = exceptionTypes.item(i);
					Class<Throwable> exceptionClass = (Class<Throwable>) Class.forName(node.getTextContent().trim());
					String exceptionLocation = xpath.compile("location").evaluate(node.getParentNode()).trim();

					if (exceptionClass == Throwable.class) {
						defaultLocation = exceptionLocation;
					}
					else {
						errorPageLocations.put(exceptionClass, exceptionLocation);
					}
				}
			}
		}
		catch (Exception e) {
			// This exception should never occur. If it occurs, then web.xml is broken anyway.
			throw new RuntimeException(e);
		}
		finally {
			if (input != null) try { input.close(); } catch (IOException ignore) { /**/ }
		}

		if (defaultLocation == null || defaultLocation.isEmpty()) {
			throw new IllegalArgumentException(ERROR_DEFAULT_LOCATION_MISSING);
		}
		else {
			errorPageLocations.put(null, defaultLocation);
		}

		for (String exceptionLocation : errorPageLocations.values()) {
			if (Faces.getResourceAsStream(exceptionLocation) == null) {
				throw new IllegalArgumentException(String.format(ERROR_LOCATION_INVALID, exceptionLocation));
			}
		}

		return errorPageLocations;
	}

}