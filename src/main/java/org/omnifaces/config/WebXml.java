/*
 * Copyright 2018 OmniFaces
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

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

/**
 * <p>
 * This configuration interface parses the <code>/WEB-INF/web.xml</code> and all <code>/META-INF/web-fragment</code> files
 * found in the classpath and offers methods to obtain information from them which is not available by the standard
 * Servlet API.
 *
 * <h3>Usage</h3>
 * <p>
 * Some examples:
 * <pre>
 * // Get the &lt;welcome-file-list&gt; (which are essentially path-relative filenames which needs to be served when a folder is requested).
 * List&lt;String&gt; welcomeFiles = WebXml.instance().getWelcomeFiles();
 * </pre>
 * <pre>
 * // Get a mapping of all error page locations by exception type (a key of null represents the default error page location, if any).
 * Map&lt;Class&lt;Throwable&gt;, String&gt; errorPageLocations = WebXml.instance().getErrorPageLocations();
 * </pre>
 * <pre>
 * // Get the &lt;form-login-page&gt; (which is a context-relative URL to the login page of FORM based authentication).
 * String formLoginPage = WebXml.instance().getFormLoginPage();
 * </pre>
 * <pre>
 * // Get a mapping of all &lt;security-constraint&gt; URL patterns and associated roles.
 * Map&lt;String, Set&lt;String&gt;&gt; securityConstraints = WebXml.instance().getSecurityConstraints();
 * </pre>
 * <pre>
 * // Check if access to certain (context-relative) URL is allowed for the given role based on &lt;security-constraint&gt;.
 * boolean accessAllowed = WebXml.instance().isAccessAllowed("/admin.xhtml", "admin");
 * </pre>
 * <pre>
 * // Get web.xml configured session timeout (in seconds).
 * int sessionTimeout = WebXml.instance().getSessionTimeout();
 * </pre>
 *
 * @author Bauke Scholtz
 * @since 1.2
 * @see WebXmlSingleton
 */
public interface WebXml {

	// Enum singleton -------------------------------------------------------------------------------------------------

	/**
	 * Returns the lazily loaded enum singleton instance.
	 * @deprecated Since 3.1; Use {@link #instance()} instead.
	 */
	@Deprecated
	public static final WebXml INSTANCE = new WebXml() {

		@Override
		public String findErrorPageLocation(Throwable exception) {
			return WebXmlSingleton.INSTANCE.findErrorPageLocation(exception);
		}

		@Override
		public boolean isAccessAllowed(String url, String role) {
			return WebXmlSingleton.INSTANCE.isAccessAllowed(url, role);
		}

		@Override
		public List<String> getWelcomeFiles() {
			return WebXmlSingleton.INSTANCE.getWelcomeFiles();
		}

		@Override
		public Map<Class<Throwable>, String> getErrorPageLocations() {
			return WebXmlSingleton.INSTANCE.getErrorPageLocations();
		}

		@Override
		public String getFormLoginPage() {
			return WebXmlSingleton.INSTANCE.getFormLoginPage();
		}

		@Override
		public String getFormErrorPage() {
			return WebXmlSingleton.INSTANCE.getFormLoginPage();
		}

		@Override
		public Map<String, Set<String>> getSecurityConstraints() {
			return WebXmlSingleton.INSTANCE.getSecurityConstraints();
		}

		@Override
		public int getSessionTimeout() {
			return WebXmlSingleton.INSTANCE.getSessionTimeout();
		}
	};

	/**
	 * Returns the lazily loaded enum singleton instance.
	 * @return The lazily loaded enum singleton instance.
	 * @since 3.1
	 */
	public static WebXml instance() {
		return WebXmlSingleton.INSTANCE;
	}

	// Init -----------------------------------------------------------------------------------------------------------

	/**
	 * Perform manual initialization with the given servlet context, if not null and not already initialized yet.
	 * @param servletContext The servlet context to obtain the web.xml from.
	 * @return The current {@link WebXml} instance, initialized and all.
	 * @deprecated Since 3.1; Use {@link #instance()} instead. It will fall back to CDI to obtain the servlet context.
	 */
	@Deprecated
	public default WebXml init(ServletContext servletContext) {
		return WebXmlSingleton.INSTANCE;
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Find for the given exception the right error page location. Exception types are matched as per Servlet 3.0
	 * specification 10.9.2 with the exception that the given exception is already unwrapped:
	 * <ul>
	 *   <li>Make a pass through all specific exception types. If a match is found in the exception class hierarchy,
	 *       use its location. The closest match in the class hierarchy wins.
	 *   <li>Else use the default error page location, which can be either the java.lang.Throwable or HTTP 500 or
	 *       default one.
	 * </ul>
	 * @param exception The exception to find the error page location for.
	 * @return The right error page location for the given exception.
	 */
	public String findErrorPageLocation(Throwable exception);

	/**
	 * Returns <code>true</code> if access to the given URL is allowed for the given role. URL patterns are matched as
	 * per Servlet 3.0 specification 12.1:
	 * <ul>
	 *   <li>Make a first pass through all URL patterns. If an exact match is found, then check the role on it.
	 *   <li>Else make a recursive pass through all prefix URL patterns, stepping down the URL one directory at a time,
	 *       trying to find the longest path match. If it is found, then check the role on it.
	 *   <li>Else if the last segment in the URL path contains an extension, then make a last pass through all suffix
	 *       URL patterns. If a match is found, then check the role on it.
	 *   <li>Else assume it as unprotected resource and return <code>true</code>.
	 * </ul>
	 * @param url URL to be checked for access by the given role. It must start with '/' and be context-relative.
	 * @param role Role to be checked for access to the given URL.
	 * @return <code>true</code> if access to the given URL is allowed for the given role, otherwise <code>false</code>.
	 * @throws NullPointerException If given URL is null.
	 * @throws IllegalArgumentException If given URL does not start with '/'.
	 * @since 1.4
	 */
	public boolean isAccessAllowed(String url, String role);

	// Getters --------------------------------------------------------------------------------------------------------

	/**
	 * Returns a list of all welcome files.
	 * @return A list of all welcome files.
	 * @since 1.4
	 */
	public List<String> getWelcomeFiles();

	/**
	 * Returns a mapping of all error page locations by exception type. The default location is identified by
	 * <code>null</code> key.
	 * @return A mapping of all error page locations by exception type.
	 */
	public Map<Class<Throwable>, String> getErrorPageLocations();

	/**
	 * Returns the location of the FORM authentication login page, or <code>null</code> if it is not defined.
	 * @return The location of the FORM authentication login page, or <code>null</code> if it is not defined.
	 */
	public String getFormLoginPage();

	/**
	 * Returns the location of the FORM authentication error page, or <code>null</code> if it is not defined.
	 * @return The location of the FORM authentication error page, or <code>null</code> if it is not defined.
	 * @since 1.8
	 */
	public String getFormErrorPage();

	/**
	 * Returns a mapping of all security constraint URL patterns and the associated roles in the declared order. If the
	 * roles is <code>null</code>, then it means that no auth constraint is been set (i.e. the resource is publicly
	 * accessible). If the roles is empty, then it means that an empty auth constraint is been set (i.e. the resource
	 * is in no way accessible).
	 * @return A mapping of all security constraint URL patterns and the associated roles in the declared order.
	 * @since 1.4
	 */
	public Map<String, Set<String>> getSecurityConstraints();

	/**
	 * Returns the configured session timeout in minutes, or <code>-1</code> if it is not defined.
	 * @return The configured session timeout in minutes, or <code>-1</code> if it is not defined.
	 * @since 1.7
	 */
	public int getSessionTimeout();

}