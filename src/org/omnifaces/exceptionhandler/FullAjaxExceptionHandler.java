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

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
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

import org.omnifaces.util.Callback;
import org.omnifaces.util.Events;
import org.omnifaces.util.Exceptions;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This exception handler enables you to show the full error page in its entirety to the enduser in case of exceptions
 * during ajax requests. Refer the documentation of {@link FullAjaxExceptionHandlerFactory} how to setup it.
 * <p>
 * This exception handler will parse the <tt>web.xml</tt> and <tt>web-fragment.xml<tt> files to find the error page
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

	private static final String WEB_XML = "/WEB-INF/web.xml";
	private static final String WEB_FRAGMENT_XML = "META-INF/web-fragment.xml";

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
				String viewId = Faces.normalizeViewId(errorPageLocation);
				context.setViewRoot(context.getApplication().getViewHandler().createView(context, viewId));
				context.getPartialViewContext().setRenderAll(true);
				context.renderResponse();

				// Prevent some servlet containers from handling the error page itself afterwards. So far Tomcat/JBoss
				// are known to do that. It would only result in IllegalStateException "response already committed".
				Events.addAfterPhaseListener(PhaseId.RENDER_RESPONSE, new Callback.Void() {
					@Override
					public void invoke() {
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
			// #6: Due to a MyFaces issue, it isn't possible to perform this task in FullAjaxExceptionHandlerFactory on
			// webapp's startup which would be more ideal.
			// http://code.google.com/p/omnifaces/source/detail?r=c7899e317a95a92325bdac4ab707bfca22958d07
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

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Parse <tt>web.xml</tt> and <tt>web-fragment.xml</tt> files and find all error page locations.
	 * @return An ordered map of all error page locations. The key <code>null</code> represents the default location.
	 * @throws IllegalArgumentException When the default location is missing.
	 */
	private static Map<Class<Throwable>, String> findErrorPageLocations() {
		Map<Class<Throwable>, String> errorPageLocations = new LinkedHashMap<Class<Throwable>, String>();

		parseWebXmlAndFillErrorPageLocations(errorPageLocations);

		if (Faces.getServletContext().getMajorVersion() >= 3) { // web-fragment.xml exist only since Servlet 3.0.
			parseWebFragmentXmlAndFillErrorPageLocations(errorPageLocations);
		}

		if (!errorPageLocations.containsKey(null)) {
			throw new IllegalArgumentException(ERROR_DEFAULT_LOCATION_MISSING);
		}

		return errorPageLocations;
	}

	/**
	 * Parse the <tt>web.xml</tt> file found in the webapp and fill all error page locations in the given map.
	 * @param errorPageLocations The error page locations map to be filled.
	 */
	private static void parseWebXmlAndFillErrorPageLocations(Map<Class<Throwable>, String> errorPageLocations) {
		InputStream input = null;

		try {
			input = Faces.getResourceAsStream(WEB_XML);

			if (input == null) {
				return; // Since Servlet 3.0, web.xml is optional.
			}

			fillErrorPageLocations("web-app", input, errorPageLocations);
		}
		catch (Exception e) {
			// This exception should never occur. If it occurs, then web.xml is broken anyway.
			throw new RuntimeException(e);
		}
		finally {
			Utils.close(input);
		}
	}

	/**
	 * Parse all <tt>web-fragment.xml</tt> files found in the runtime classpath and fill all error page locations in
	 * the given map.
	 * @param errorPageLocations The error page locations map to be filled.
	 */
	private static void parseWebFragmentXmlAndFillErrorPageLocations(Map<Class<Throwable>, String> errorPageLocations) {
		Enumeration<URL> urls = null;

		try {
			urls = Thread.currentThread().getContextClassLoader().getResources(WEB_FRAGMENT_XML);
		}
		catch (Exception e) {
			// This exception should never occur. If it occurs, then classpath is broken anyway.
			throw new RuntimeException(e);
		}

		while (urls.hasMoreElements()) {
			InputStream input = null;

			try {
				URLConnection connection = urls.nextElement().openConnection();
				connection.setUseCaches(false);
				input = connection.getInputStream();
				fillErrorPageLocations("web-fragment", input, errorPageLocations);
			}
			catch (Exception e) {
				// This exception should never occur. If it occurs, then web.xml is broken anyway.
				throw new RuntimeException(e);
			}
			finally {
				Utils.close(input);
			}
		}
	}

	/**
	 * Perform the actual XML parsing starting with the given root element name on the given input stream. If an error
	 * page location is found and it does not exist in the given map yet, then it will be added.
	 * @param root The XML root element name.
	 * @param input The XML input stream.
	 * @param errorPageLocations The error page locations map to be filled.
	 */
	@SuppressWarnings("unchecked") // For the cast on Class<Throwable>.
	private static void fillErrorPageLocations
		(String root, InputStream input, Map<Class<Throwable>, String> errorPageLocations)
			throws Exception
	{
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
		XPath xpath = XPathFactory.newInstance().newXPath();
		String defaultLocation =
			xpath.compile(root + "/error-page[error-code=500]/location").evaluate(document).trim();
		NodeList exceptionTypes =
			(NodeList) xpath.compile(root + "/error-page/exception-type").evaluate(document, XPathConstants.NODESET);

		for (int i = 0; i < exceptionTypes.getLength(); i++) {
			Node node = exceptionTypes.item(i);
			Class<Throwable> exceptionClass = (Class<Throwable>) Class.forName(node.getTextContent().trim());
			String exceptionLocation = xpath.compile("location").evaluate(node.getParentNode()).trim();

			if (exceptionClass == Throwable.class) {
				defaultLocation = exceptionLocation;
			}
			else if (!errorPageLocations.containsKey(exceptionClass)) {
				errorPageLocations.put(exceptionClass, exceptionLocation);
			}
		}

		if (defaultLocation != null && !defaultLocation.isEmpty() && !errorPageLocations.containsKey(null)) {
			errorPageLocations.put(null, defaultLocation);
		}
	}

}