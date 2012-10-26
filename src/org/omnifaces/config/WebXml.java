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
package org.omnifaces.config;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.omnifaces.util.Faces;
import org.omnifaces.util.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This configuration enum parses the <tt>/WEB-INF/web.xml</tt> and all <tt>/META-INF/web-fragment</tt> files found in
 * the classpath and offers methods to obtain information from them which is not available by the standard servlet API
 * means.
 * <p>
 * So far there are only methods for obtaining a mapping of all error page locations by exception type and the location
 * of the FORM authentication login page.
 *
 * @author Bauke Scholtz
 * @since 1.2
 */
public enum WebXml {

	// Enum singleton -------------------------------------------------------------------------------------------------

	/**
	 * Returns the lazily loaded enum singleton instance.
	 */
	INSTANCE;

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String WEB_XML = "/WEB-INF/web.xml";
	private static final String WEB_FRAGMENT_XML = "META-INF/web-fragment.xml";

	private static final String XPATH_EXCEPTION_TYPE =
		"error-page/exception-type";
	private static final String XPATH_LOCATION =
		"location";
	private static final String XPATH_500_LOCATION =
		"error-page[error-code=500]/location";
	private static final String XPATH_DEFAULT_LOCATION =
		"error-page[not(error-code) and not(exception-type)]/location";
	private static final String XPATH_FORM_LOGIN_PAGE =
		"login-config[auth-method='FORM']/form-login-config/form-login-page";

	// Properties -----------------------------------------------------------------------------------------------------

	private Map<Class<Throwable>, String> errorPageLocations;
	private String formLoginPage;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Perform initialization.
	 */
	private WebXml() {
		try {
			Document allWebXml = loadAllWebXml();
			errorPageLocations = parseErrorPageLocations(allWebXml);
			formLoginPage = parseFormLoginPage(allWebXml);
		}
		catch (Exception e) {
			// If this occurs, web.xml is broken anyway and the app shouldn't have started/initialized this far at all.
			throw new RuntimeException(e);
		}
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Find for the given exception the right error page location as per Servlet specification 10.9.2:
	 * <ul>
	 *   <li>Make a first pass through all specific exception types. If an exact match is found, use its location.
	 *   <li>Else make a second pass through all specific exception types in the order as they are declared in
	 *       web.xml. If the current exception is an instance of it, then use its location.
	 *   <li>Else use the default error page location, which can be either the java.lang.Throwable or HTTP 500 or
	 *       default one.
	 * </ul>
	 * @param exception The exception to find the error page location for.
	 * @return The right error page location for the given exception.
	 */
	public String findErrorPageLocation(Throwable exception) {
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

	// Getters --------------------------------------------------------------------------------------------------------

	/**
	 * Returns a mapping of all error page locations by exception type. The default location is identified by
	 * <code>null</code> key.
	 * @return A mapping of all error page locations by exception type.
	 */
	public Map<Class<Throwable>, String> getErrorPageLocations() {
		return errorPageLocations;
	}

	/**
	 * Returns the location of the FORM authentication login page, or <code>null</code> if it is not defined.
	 * @return The location of the FORM authentication login page, or <code>null</code> if it is not defined.
	 */
	public String getFormLoginPage() {
		return formLoginPage;
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Load, merge and return all <code>web.xml</code> and <code>web-fragment.xml</code> files found in the classpath
	 * into a single {@link Document}.
	 */
	private static Document loadAllWebXml() throws Exception {
		DocumentBuilder builder = createDocumentBuilder();
		Document document = builder.newDocument();
		document.appendChild(document.createElement("web"));
		URL url = Faces.getResource(WEB_XML);

		if (url != null) { // Since Servlet 3.0, web.xml is optional.
			parseAndAppendChildren(url, builder, document);
		}

		if (Faces.getServletContext().getMajorVersion() >= 3) { // web-fragment.xml exist only since Servlet 3.0.
			Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources(WEB_FRAGMENT_XML);

			while (urls.hasMoreElements()) {
				parseAndAppendChildren(urls.nextElement(), builder, document);
			}
		}

		return document;
	}

	/**
	 * Returns an instance of {@link DocumentBuilder} which doesn't validate, nor is namespace aware nor expands entity
	 * references (to keep it as lenient as possible).
	 */
	private static DocumentBuilder createDocumentBuilder() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(false);
		factory.setExpandEntityReferences(false);
		return factory.newDocumentBuilder();
	}

	/**
	 * Parse the given URL as a document using the given builder and then append all its child nodes to the given
	 * document.
	 */
	private static void parseAndAppendChildren(URL url, DocumentBuilder builder, Document document) throws Exception {
		URLConnection connection = url.openConnection();
		connection.setUseCaches(false);
		InputStream input = null;

		try {
			input = connection.getInputStream();
			NodeList children = builder.parse(input).getDocumentElement().getChildNodes();

			for (int i = 0; i < children.getLength(); i++) {
				document.getDocumentElement().appendChild(document.importNode(children.item(i), true));
			}
		}
		finally {
			Utils.close(input);
		}
	}

	/**
	 * Create and return a mapping of all error page locations by exception type found in the given document.
	 */
	@SuppressWarnings("unchecked") // For the cast on Class<Throwable>.
	private static Map<Class<Throwable>, String> parseErrorPageLocations(Document document) throws Exception {
		Map<Class<Throwable>, String> errorPageLocations = new LinkedHashMap<Class<Throwable>, String>();
		Element documentElement = document.getDocumentElement();
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList exceptionTypes = (NodeList)
			xpath.compile(XPATH_EXCEPTION_TYPE).evaluate(documentElement, XPathConstants.NODESET);

		for (int i = 0; i < exceptionTypes.getLength(); i++) {
			Node node = exceptionTypes.item(i);
			Class<Throwable> exceptionClass = (Class<Throwable>) Class.forName(node.getTextContent().trim());
			String exceptionLocation = xpath.compile(XPATH_LOCATION).evaluate(node.getParentNode()).trim();
			Class<Throwable> key = (exceptionClass == Throwable.class) ? null : exceptionClass;

			if (!errorPageLocations.containsKey(key)) {
				errorPageLocations.put(key, exceptionLocation);
			}
		}

		if (!errorPageLocations.containsKey(null)) {
			String defaultLocation = xpath.compile(XPATH_500_LOCATION).evaluate(documentElement).trim();

			if (Utils.isEmpty(defaultLocation)) {
				defaultLocation = xpath.compile(XPATH_DEFAULT_LOCATION).evaluate(documentElement).trim();
			}

			if (!Utils.isEmpty(defaultLocation)) {
				errorPageLocations.put(null, defaultLocation);
			}
		}

		return errorPageLocations;
	}

	/**
	 * Return the location of the FORM authentication login page found in the given document.
	 */
	private static String parseFormLoginPage(Document document) throws Exception {
		XPath xpath = XPathFactory.newInstance().newXPath();
		String formLoginPage = xpath.compile(XPATH_FORM_LOGIN_PAGE).evaluate(document.getDocumentElement()).trim();
		return Utils.isEmpty(formLoginPage) ? null : formLoginPage;
	}

}