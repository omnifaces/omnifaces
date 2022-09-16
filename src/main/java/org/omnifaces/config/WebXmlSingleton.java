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
package org.omnifaces.config;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static org.omnifaces.util.Servlets.getWebXmlURL;
import static org.omnifaces.util.Utils.isEmpty;
import static org.omnifaces.util.Utils.isNumber;
import static org.omnifaces.util.Xml.createDocument;
import static org.omnifaces.util.Xml.getNodeList;
import static org.omnifaces.util.Xml.getTextContent;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.omnifaces.util.Servlets;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Enum singleton implementation of {@link WebXml}.
 *
 * @author Bauke Scholtz
 * @since 3.1
 */
enum WebXmlSingleton implements WebXml {

	// Enum singleton -------------------------------------------------------------------------------------------------

	/**
	 * Returns the lazily loaded enum singleton instance.
	 */
	INSTANCE;

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String WEB_FRAGMENT_XML = "META-INF/web-fragment.xml";

	private static final String XPATH_WELCOME_FILE =
		"welcome-file-list/welcome-file";
	private static final String XPATH_EXCEPTION_TYPE =
		"error-page/exception-type";
	private static final String XPATH_LOCATION =
		"location";
	private static final String XPATH_ERROR_PAGE_500_LOCATION =
		"error-page[error-code=500]/location";
	private static final String XPATH_ERROR_PAGE_DEFAULT_LOCATION =
		"error-page[not(error-code) and not(exception-type)]/location";
	private static final String XPATH_FORM_LOGIN_PAGE =
		"login-config[auth-method='FORM']/form-login-config/form-login-page";
	private static final String XPATH_FORM_ERROR_PAGE =
		"login-config[auth-method='FORM']/form-login-config/form-error-page";
	private static final String XPATH_DEFAULT_FORM_LOGIN_PAGE =
		"login-config/form-login-config/form-login-page";
	private static final String XPATH_DEFAULT_FORM_ERROR_PAGE =
		"login-config/form-login-config/form-error-page";
	private static final String XPATH_SECURITY_CONSTRAINT =
		"security-constraint";
	private static final String XPATH_WEB_RESOURCE_URL_PATTERN =
		"web-resource-collection/url-pattern";
	private static final String XPATH_AUTH_CONSTRAINT =
		"auth-constraint";
	private static final String XPATH_AUTH_CONSTRAINT_ROLE_NAME =
		"auth-constraint/role-name";
	private static final String XPATH_SESSION_TIMEOUT =
		"session-config/session-timeout";
	private static final String XPATH_DISTRIBUTABLE =
		"boolean(distributable)";

	private static final String ERROR_URL_MUST_START_WITH_SLASH =
		"URL must start with '/': '%s'";
	private static final String ERROR_INITIALIZATION_FAIL =
		"WebXml failed to initialize. Perhaps your web.xml contains a typo?";

	// Properties -----------------------------------------------------------------------------------------------------

	private List<String> welcomeFiles;
	private Map<Class<Throwable>, String> errorPageLocations;
	private String formLoginPage;
	private String formErrorPage;
	private Map<String, Set<String>> securityConstraints;
	private int sessionTimeout;
	private boolean distributable;

	// Init -----------------------------------------------------------------------------------------------------------

	/**
	 * Perform automatic initialization whereby the servlet context is obtained from CDI.
	 */
	private WebXmlSingleton() {
		try {
			ServletContext servletContext = Servlets.getContext();
			Element allWebXmls = loadAllWebXmls(servletContext).getDocumentElement();
			XPath xpath = XPathFactory.newInstance().newXPath();
			welcomeFiles = parseWelcomeFiles(allWebXmls, xpath);
			errorPageLocations = parseErrorPageLocations(allWebXmls, xpath);
			formLoginPage = parseFormLoginPage(allWebXmls, xpath);
			formErrorPage = parseFormErrorPage(allWebXmls, xpath);
			securityConstraints = parseSecurityConstraints(allWebXmls, xpath);
			sessionTimeout = parseSessionTimeout(allWebXmls, xpath);

			Element rootWebXml = loadRootWebXml(servletContext).getDocumentElement();
			distributable = parseDistributable(rootWebXml, xpath);
		}
		catch (Exception e) {
			throw new IllegalStateException(ERROR_INITIALIZATION_FAIL, e);
		}
	}

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public String findErrorPageLocation(Throwable exception) {
		String location = null;

		for (Class<?> cls = exception.getClass(); cls != null && location == null; cls = cls.getSuperclass()) {
			location = errorPageLocations.get(cls);
		}

		return (location == null) ? errorPageLocations.get(null) : location;
	}

	@Override
	public boolean isAccessAllowed(String url, String role) {
		if (url.charAt(0) != ('/')) {
			throw new IllegalArgumentException(format(ERROR_URL_MUST_START_WITH_SLASH, url));
		}

		String uri = url;

		if (url.length() > 1 && url.charAt(url.length() - 1) == '/') {
			uri = url.substring(0, url.length() - 1); // Trim trailing slash.
		}

		Set<String> roles = findExactMatchRoles(uri);

		if (roles.isEmpty()) {
			roles = findPrefixMatchRoles(uri);
		}

		if (roles.isEmpty()) {
			roles = findSuffixMatchRoles(uri);
		}

		return isRoleMatch(roles, role);
	}

	private Set<String> findExactMatchRoles(String url) {
		for (Entry<String, Set<String>> entry : securityConstraints.entrySet()) {
			if (isExactMatch(entry.getKey(), url)) {
				return entry.getValue();
			}
		}

		return emptySet();
	}

	private Set<String> findPrefixMatchRoles(String url) {
		String urlMatch = "";

		for (String path = url; !path.isEmpty(); path = path.substring(0, path.lastIndexOf('/'))) {
			Set<String> roles = null;

			for (Entry<String, Set<String>> entry : securityConstraints.entrySet()) {
				if (urlMatch.length() < entry.getKey().length() && isPrefixMatch(entry.getKey(), path)) {
					urlMatch = entry.getKey();
					roles = entry.getValue();
				}
			}

			if (roles != null) {
				return roles;
			}
		}

		return emptySet();
	}

	private Set<String> findSuffixMatchRoles(String url) {
		if (url.contains(".")) {
			for (Entry<String, Set<String>> entry : securityConstraints.entrySet()) {
				if (isSuffixMatch(url, entry.getKey())) {
					return entry.getValue();
				}
			}
		}

		return emptySet();
	}

	private static boolean isExactMatch(String urlPattern, String url) {
		return url.equals(urlPattern.endsWith("/*") ? urlPattern.substring(0, urlPattern.length() - 2) : urlPattern);
	}

	private static boolean isPrefixMatch(String urlPattern, String url) {
		return urlPattern.endsWith("/*") && (url + "/").startsWith(urlPattern.substring(0, urlPattern.length() - 1));
	}

	private static boolean isSuffixMatch(String urlPattern, String url) {
		return urlPattern.startsWith("*.") && url.endsWith(urlPattern.substring(1));
	}

	private static boolean isRoleMatch(Set<String> roles, String role) {
		return roles.isEmpty() || roles.contains(role) || (role != null && roles.contains("*"));
	}

	// Getters --------------------------------------------------------------------------------------------------------

	@Override
	public List<String> getWelcomeFiles() {
		return welcomeFiles;
	}

	@Override
	public Map<Class<Throwable>, String> getErrorPageLocations() {
		return errorPageLocations;
	}

	@Override
	public String getFormLoginPage() {
		return formLoginPage;
	}

	@Override
	public String getFormErrorPage() {
		return formErrorPage;
	}

	@Override
	public Map<String, Set<String>> getSecurityConstraints() {
		return securityConstraints;
	}

	@Override
	public int getSessionTimeout() {
		return sessionTimeout;
	}

	@Override
	public boolean isDistributable() {
		return distributable;
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Load, merge and return all <code>web.xml</code> and <code>web-fragment.xml</code> files found in the classpath
	 * into a single {@link Document}.
	 */
	private static Document loadAllWebXmls(ServletContext context) throws IOException, SAXException {
		List<URL> webXmlURLs = new ArrayList<>();
		webXmlURLs.add(getWebXmlURL(context));
		webXmlURLs.addAll(Collections.list(Thread.currentThread().getContextClassLoader().getResources(WEB_FRAGMENT_XML)));
		return createDocument(webXmlURLs);
	}

	/**
	 * Load root <code>web.xml</code> file into a single {@link Document}.
	 */
	private static Document loadRootWebXml(ServletContext context) throws IOException, SAXException {
		return createDocument(asList(getWebXmlURL(context)));
	}

	/**
	 * Create and return a list of all welcome files.
	 */
	private static List<String> parseWelcomeFiles(Element webXml, XPath xpath) throws XPathExpressionException {
		NodeList welcomeFileList = getNodeList(webXml, xpath, XPATH_WELCOME_FILE);
		List<String> welcomeFiles = new ArrayList<>(welcomeFileList.getLength());

		for (int i = 0; i < welcomeFileList.getLength(); i++) {
			welcomeFiles.add(getTextContent(welcomeFileList.item(i)));
		}

		return Collections.unmodifiableList(welcomeFiles);
	}

	/**
	 * Create and return a mapping of all error page locations by exception type found in the given document.
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("unchecked") // For the cast on Class<Throwable>.
	private static Map<Class<Throwable>, String> parseErrorPageLocations(Element webXml, XPath xpath) throws XPathExpressionException, ClassNotFoundException {
		Map<Class<Throwable>, String> errorPageLocations = new HashMap<>();
		NodeList exceptionTypes = getNodeList(webXml, xpath, XPATH_EXCEPTION_TYPE);

		for (int i = 0; i < exceptionTypes.getLength(); i++) {
			Node node = exceptionTypes.item(i);
			Class<Throwable> exceptionClass = (Class<Throwable>) Class.forName(getTextContent(node));
			String exceptionLocation = xpath.compile(XPATH_LOCATION).evaluate(node.getParentNode()).trim();
			Class<Throwable> key = (exceptionClass == Throwable.class) ? null : exceptionClass;
			errorPageLocations.computeIfAbsent(key, k -> exceptionLocation);
		}

		if (!errorPageLocations.containsKey(null)) {
			String defaultLocation = xpath.compile(XPATH_ERROR_PAGE_500_LOCATION).evaluate(webXml).trim();

			if (isEmpty(defaultLocation)) {
				defaultLocation = xpath.compile(XPATH_ERROR_PAGE_DEFAULT_LOCATION).evaluate(webXml).trim();
			}

			if (!isEmpty(defaultLocation)) {
				errorPageLocations.put(null, defaultLocation);
			}
		}

		return Collections.unmodifiableMap(errorPageLocations);
	}

	/**
	 * Return the location of the FORM authentication login page.
	 */
	private static String parseFormLoginPage(Element webXml, XPath xpath) throws XPathExpressionException {
		String formLoginPage = xpath.compile(XPATH_FORM_LOGIN_PAGE).evaluate(webXml).trim();

		if (isEmpty(formLoginPage)) {
			formLoginPage = xpath.compile(XPATH_DEFAULT_FORM_LOGIN_PAGE).evaluate(webXml).trim();
		}

		return isEmpty(formLoginPage) ? null : formLoginPage;
	}

	/**
	 * Return the location of the FORM authentication error page.
	 */
	private static String parseFormErrorPage(Element webXml, XPath xpath) throws XPathExpressionException {
		String formErrorPage = xpath.compile(XPATH_FORM_ERROR_PAGE).evaluate(webXml).trim();

		if (isEmpty(formErrorPage)) {
			formErrorPage = xpath.compile(XPATH_DEFAULT_FORM_ERROR_PAGE).evaluate(webXml).trim();
		}

		return isEmpty(formErrorPage) ? null : formErrorPage;
	}

	/**
	 * Create and return a mapping of all security constraint URL patterns and the associated roles.
	 */
	private static Map<String, Set<String>> parseSecurityConstraints(Element webXml, XPath xpath) throws XPathExpressionException {
		Map<String, Set<String>> securityConstraints = new LinkedHashMap<>();
		NodeList constraints = getNodeList(webXml, xpath, XPATH_SECURITY_CONSTRAINT);

		for (int i = 0; i < constraints.getLength(); i++) {
			Node constraint = constraints.item(i);
			Set<String> roles = emptySet();
			NodeList auth = getNodeList(constraint, xpath, XPATH_AUTH_CONSTRAINT);

			if (auth.getLength() > 0) {
				NodeList authRoles = getNodeList(constraint, xpath, XPATH_AUTH_CONSTRAINT_ROLE_NAME);
				roles = new HashSet<>(authRoles.getLength());

				for (int j = 0; j < authRoles.getLength(); j++) {
					roles.add(getTextContent(authRoles.item(j)));
				}
			}

			NodeList urlPatterns = getNodeList(constraint, xpath, XPATH_WEB_RESOURCE_URL_PATTERN);

			for (int j = 0; j < urlPatterns.getLength(); j++) {
				String urlPattern = getTextContent(urlPatterns.item(j));
				Set<String> allRoles = securityConstraints.get(urlPattern);

				if (allRoles != null) {
					allRoles = new HashSet<>(allRoles);
					allRoles.addAll(roles);
				}
				else {
					allRoles = roles;
				}

				securityConstraints.put(urlPattern, unmodifiableSet(allRoles));
			}
		}

		return Collections.unmodifiableMap(securityConstraints);
	}

	/**
	 * Return the configured session timeout in minutes, or <code>-1</code> if it is not defined.
	 */
	private static int parseSessionTimeout(Element webXml, XPath xpath) throws XPathExpressionException {
		String sessionTimeout = xpath.compile(XPATH_SESSION_TIMEOUT).evaluate(webXml).trim();
		return isNumber(sessionTimeout) ? Integer.parseInt(sessionTimeout) : -1;
	}

	/**
	 * Return the configured distributable flag.
	 */
	private static boolean parseDistributable(Element webXml, XPath xpath) throws XPathExpressionException {
		String distributable = xpath.compile(XPATH_DISTRIBUTABLE).evaluate(webXml).trim();
		return Boolean.parseBoolean(distributable);
	}

}