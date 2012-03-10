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
import java.util.LinkedHashMap;
import java.util.Map;

import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.omnifaces.util.Faces;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This exception handler factory needs to be registered as follows in <tt>faces-config.xml</tt> to get the
 * {@link FullAjaxExceptionHandler} to run:
 * <pre>
 * &lt;factory&gt;
 *   &lt;exception-handler-factory&gt;
 *     org.omnifaces.exceptionhandler.FullAjaxExceptionHandlerFactory
 *   &lt;/exception-handler-factory&gt;
 * &lt;/factory&gt;
 * </pre>
 * <p>
 * This exception handler factory will parse the <tt>web.xml</tt> to find the error page locations of the HTTP error
 * code <tt>500</tt> all declared exception types. Those locations need to point to Facelets files. The location of the
 * HTTP error code <tt>500</tt> or the exception type <code>java.lang.Throwable</code> is required in order to get the
 * full ajax exception handler to work, because there's then at least a fall back error page whenever there's no match
 * with any of the declared specific exceptions. So, you must at least have either
 * <pre>
 * &lt;error-page&gt;
 *   &lt;error-code&gt;500&lt;/error-code&gt;
 *   &lt;location&gt;/errors/500.xhtml&lt;/location&gt;
 * &lt;/error-page&gt;
 * </pre>
 * or
 * <pre>
 * &lt;error-page&gt;
 *   &lt;exception-type&gt;java.lang.Throwable&lt;/error-code&gt;
 *   &lt;location&gt;/errors/500.xhtml&lt;/location&gt;
 * &lt;/error-page&gt;
 * </pre>
 * <p>
 * Both can also, only the <code>java.lang.Throwable</code> one will always get precedence over the <tt>500</tt> one,
 * as per the Servlet API specification, so the <tt>500</tt> one would be basically superfluous.
 *
 * @author Bauke Scholtz
 */
public class FullAjaxExceptionHandlerFactory extends ExceptionHandlerFactory {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String WEB_XML =
		"/WEB-INF/web.xml";
	private static final String ERROR_LOCATION_INVALID =
		"Error page location '%s' in web.xml is invalid. Resource resolved to null.";
	private static final String ERROR_DEFAULT_LOCATION_MISSING =
		"Either HTTP 500 or java.lang.Throwable error page is required in web.xml. Neither was found.";

	// Variables ------------------------------------------------------------------------------------------------------

	private ExceptionHandlerFactory wrapped;
	private Map<Class<Throwable>, String> errorPageLocations;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new full ajax exception handler factory around the given wrapped factory. This constructor will
	 * parse the <tt>web.xml</tt> to find the error page locations.
	 * @param wrapped The wrapped factory.
	 * @throws IllegalArgumentException When an error page location in <tt>web.xml</tt> is missing or invalid.
	 */
	public FullAjaxExceptionHandlerFactory(ExceptionHandlerFactory wrapped) {
		this.wrapped = wrapped;
		this.errorPageLocations = findErrorPageLocations();
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns a new instance {@link FullAjaxExceptionHandler}.
	 */
	@Override
	public ExceptionHandler getExceptionHandler() {
		return new FullAjaxExceptionHandler(wrapped.getExceptionHandler(), errorPageLocations);
	}

	/**
	 * Returns the wrapped factory.
	 */
	@Override
	public ExceptionHandlerFactory getWrapped() {
		return wrapped;
	}

	// Helpers --------------------------------------------------------------------------------------------------------

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