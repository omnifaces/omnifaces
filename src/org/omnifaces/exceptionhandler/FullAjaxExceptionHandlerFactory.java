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

import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

import org.omnifaces.util.Faces;
import org.w3c.dom.Document;

/**
 * This exception handler factory needs to be registered as follows in <tt>faces-config.xml</tt> to get the
 * {@link FullAjaxExceptionHandler} to run:
 * <pre>
 * &lt;factory&gt;
 *   &lt;exception-handler-factory&gt;org.omnifaces.exceptionhandler.FullAjaxExceptionHandlerFactory&lt;/exception-handler-factory&gt;
 * &lt;/factory&gt;
 * </pre>
 * <p>
 * This exception handler factory will parse the <tt>web.xml</tt> to find the HTTP 500 error page location. You need to
 * make sure that you have a Facelets file as HTTP 500 error page configured in <tt>web.xml</tt>:
 * <pre>
 * &lt;error-page&gt;
 *   &lt;error-code&gt;500&lt;/error-code&gt;
 *   &lt;location&gt;/errors/500.xhtml&lt;/location&gt;
 * &lt;/error-page&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 */
public class FullAjaxExceptionHandlerFactory extends ExceptionHandlerFactory {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String WEB_XML =
		"/WEB-INF/web.xml";
	private static final String XPATH_500_LOCATION =
		"/web-app/error-page[error-code=500]/location";
	private static final String ERROR_500_MISSING =
		"HTTP 500 error page is missing in web.xml.";
	private static final String ERROR_500_LOCATION_INVALID =
		"HTTP 500 error page location '%s' in web.xml is invalid. Resource resolved to null.";

	// Variables ------------------------------------------------------------------------------------------------------

	private String errorPageLocation;
	private ExceptionHandlerFactory wrapped;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new full ajax exception handler factory around the given wrapped factory. This constructor will
	 * parse the <tt>web.xml</tt> to find the HTTP 500 error page location.
	 * @param wrapped The wrapped factory.
     * @throws IllegalArgumentException When the HTTP 500 error page location in <tt>web.xml</tt> is missing or invalid.
	 */
	public FullAjaxExceptionHandlerFactory(ExceptionHandlerFactory wrapped) {
		this.wrapped = wrapped;
		this.errorPageLocation = findErrorPageLocation();
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns a new instance {@link FullAjaxExceptionHandler}.
	 */
	@Override
	public ExceptionHandler getExceptionHandler() {
		return new FullAjaxExceptionHandler(wrapped.getExceptionHandler(), errorPageLocation);
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
     * Parse <tt>web.xml</tt> and find the location of the HTTP 500 error page.
     * @return The location of the HTTP 500 error page.
     * @throws IllegalArgumentException When the HTTP 500 error page is not definied in <tt>web.xml</tt> at all, or
     * when the declared location is not resolveable.
     */
    private static String findErrorPageLocation() {
    	InputStream input = null;
    	String location = null;

        try {
    		input = Faces.getResourceAsStream(WEB_XML);

    		if (input != null) { // Since Servlet 3.0, web.xml is optional.
        		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
        		location = XPathFactory.newInstance().newXPath().compile(XPATH_500_LOCATION).evaluate(document);
    		}
		}
		catch (Exception e) {
			// This exception should never occur. If web.xml was unparseable, the webapp wouldn't even have started.
			throw new RuntimeException(e);
		}
        finally {
			if (input != null) try { input.close(); } catch (IOException ignore) { /**/ }
		}

		if (location == null || location.trim().isEmpty()) {
			throw new IllegalArgumentException(ERROR_500_MISSING);
		}

		if (Faces.getResourceAsStream(location) == null) {
			throw new IllegalArgumentException(String.format(ERROR_500_LOCATION_INVALID, location));
		}

		return location;
    }

}
