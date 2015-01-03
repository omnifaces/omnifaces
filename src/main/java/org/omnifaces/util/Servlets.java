/*
 * Copyright 2013 OmniFaces.
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
package org.omnifaces.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.regex.Pattern.quote;
import static javax.faces.application.ProjectStage.Development;
import static javax.faces.application.ProjectStage.PROJECT_STAGE_JNDI_NAME;
import static javax.faces.application.ProjectStage.PROJECT_STAGE_PARAM_NAME;
import static org.omnifaces.util.JNDI.getEnvEntry;
import static org.omnifaces.util.Utils.decodeURL;
import static org.omnifaces.util.Utils.encodeURL;
import static org.omnifaces.util.Utils.isEmpty;
import static org.omnifaces.util.Utils.startsWithOneOf;
import static org.omnifaces.util.Utils.unmodifiableSet;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.application.Application;
import javax.faces.application.ResourceHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.omnifaces.filter.CacheControlFilter;

/**
 * <p>
 * Collection of utility methods for the Servlet API in general. Most of them are internally used by {@link Faces}
 * and {@link FacesLocal}, however they may also be useful in a "plain vanilla" servlet or servlet filter.
 * <p>
 * There are as of now also three special methods related to JSF without needing a {@link FacesContext}:
 * <ul>
 * <li>The {@link #isFacesAjaxRequest(HttpServletRequest)} which is capable of checking if the current request is a JSF
 * ajax request.
 * <li>The {@link #isFacesResourceRequest(HttpServletRequest)} which is capable of checking if the current request is a
 * JSF resource request.
 * <li>The {@link #facesRedirect(HttpServletRequest, HttpServletResponse, String, String...)} which is capable
 * of distinguishing JSF ajax requests from regular requests and altering the redirect logic on it, exactly like as
 * {@link ExternalContext#redirect(String)} does. In other words, this method behaves exactly the same as
 * {@link Faces#redirect(String, String...)}.
 * </ul>
 * <p>
 * Those methods can be used in for example a servlet filter.
 *
 * @author Arjan Tijms
 * @author Bauke Scholtz
 * @since 1.6
 */
public final class Servlets {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final Set<String> FACES_AJAX_HEADERS = unmodifiableSet("partial/ajax", "partial/process");
	private static final String FACES_AJAX_REDIRECT_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
		+ "<partial-response><redirect url=\"%s\"></redirect></partial-response>";

	// Variables ------------------------------------------------------------------------------------------------------

	private static Boolean facesDevelopment;

	// Constructors ---------------------------------------------------------------------------------------------------

	private Servlets() {
		// Hide constructor.
	}

	// HttpServletRequest ---------------------------------------------------------------------------------------------

	/**
	 * Returns the HTTP request hostname. This is the entire domain, without any scheme and slashes. Noted should be
	 * that this value is extracted from the request URL, not from {@link HttpServletRequest#getServerName()} as its
	 * outcome can be influenced by proxies.
	 * @param request The involved HTTP servlet request.
	 * @return The HTTP request hostname.
	 * @throws IllegalArgumentException When the URL is malformed. This is however unexpected as the request would
	 * otherwise not have hit the server at all.
	 * @see HttpServletRequest#getRequestURL()
	 */
	public static String getRequestHostname(HttpServletRequest request) {
		try {
			return new URL(request.getRequestURL().toString()).getHost();
		}
		catch (MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Returns the HTTP request domain URL. This is the URL with the scheme and domain, without any trailing slash.
	 * @param request The involved HTTP servlet request.
	 * @return The HTTP request domain URL.
	 * @see HttpServletRequest#getRequestURL()
	 * @see HttpServletRequest#getRequestURI()
	 */
	public static String getRequestDomainURL(HttpServletRequest request) {
		String url = request.getRequestURL().toString();
		return url.substring(0, url.length() - request.getRequestURI().length());
	}

	/**
	 * Returns the HTTP request base URL. This is the URL from the scheme, domain until with context path, including
	 * the trailing slash. This is the value you could use in HTML <code>&lt;base&gt;</code> tag.
	 * @param request The involved HTTP servlet request.
	 * @return The HTTP request base URL.
	 * @see HttpServletRequest#getRequestURL()
	 * @see HttpServletRequest#getRequestURI()
	 * @see HttpServletRequest#getContextPath()
	 */
	public static String getRequestBaseURL(HttpServletRequest request) {
		return getRequestDomainURL(request) + request.getContextPath() + "/";
	}

	/**
	 * Returns the HTTP request URI relative to the context root of a web application. This is the request URI
	 * minus the context path. Note that this includes path parameters.
	 *
	 * @param request The involved HTTP servlet request.
	 * @return the request URI relative to the context root
	 * @since 1.8
	 */
	public static String getRequestRelativeURI(HttpServletRequest request) {
		return request.getRequestURI().substring(request.getContextPath().length());
	}

	/**
	 * Returns the HTTP request URI relative to the context root of a web application. This is the servlet path
	 * plus the path info (if any).
	 *
	 * @param request The involved HTTP servlet request.
	 * @return the request URI relative to the context root
	 * @since 1.8
	 */
	public static String getRequestRelativeURIWithoutPathParameters(HttpServletRequest request) {
		return request.getPathInfo() == null? request.getServletPath() : request.getServletPath() + request.getPathInfo();
	}

	/**
	 * Returns the HTTP request URL with query string. This is the full request URL with query string as the enduser
	 * sees in browser address bar.
	 * @param request The involved HTTP servlet request.
	 * @return The HTTP request URL with query string.
	 * @see HttpServletRequest#getRequestURL()
	 * @see HttpServletRequest#getQueryString()
	 */
	public static String getRequestURLWithQueryString(HttpServletRequest request) {
		StringBuffer requestURL = request.getRequestURL();
		String queryString = request.getQueryString();
		return (queryString == null) ? requestURL.toString() : requestURL.append('?').append(queryString).toString();
	}

	/**
	 * Returns the HTTP request URI with query string. This is the part after the domain in the request URL, including
	 * the leading slash and the request query string.
	 * @param request The involved HTTP servlet request.
	 * @return The HTTP request URI with query string.
	 * @see HttpServletRequest#getRequestURI()
	 * @see HttpServletRequest#getQueryString()
	 */
	public static String getRequestURIWithQueryString(HttpServletRequest request) {
		String requestURI = request.getRequestURI();
		String queryString = request.getQueryString();
		return (queryString == null) ? requestURI : (requestURI + "?" + queryString);
	}

	/**
	 * Returns the HTTP request query string as parameter values map. Note this method returns <strong>only</strong>
	 * the request URL (GET) parameters, as opposed to {@link HttpServletRequest#getParameterMap()}, which contains both
	 * the request URL (GET) parameters and and the request body (POST) parameters.
	 * The map entries are in the same order as they appear in the query string.
	 * @param request The request for which the base URL is computed.
	 * @return The HTTP request query string as parameter values map.
	 */
	public static Map<String, List<String>> getRequestQueryStringMap(HttpServletRequest request) {
		String queryString = request.getQueryString();

		if (isEmpty(queryString)) {
			return Collections.<String, List<String>>emptyMap();
		}

		return toParameterMap(queryString);
	}

	/**
	 * Returns the original HTTP request URI behind this forwarded request, if any.
	 * This does not include the request query string.
	 * @param request The involved HTTP servlet request.
	 * @return The original HTTP request URI behind this forwarded request, if any.
	 * @since 1.8
	 */
	public static String getForwardRequestURI(HttpServletRequest request) {
		return (String) request.getAttribute("javax.servlet.forward.request_uri");
	}

	/**
	 * Returns the original HTTP request query string behind this forwarded request, if any.
	 * @param request The involved HTTP servlet request.
	 * @return The original HTTP request query string behind this forwarded request, if any.
	 * @since 1.8
	 */
	public static String getForwardRequestQueryString(HttpServletRequest request) {
		return (String) request.getAttribute("javax.servlet.forward.query_string");
	}

	/**
	 * Returns the original HTTP request URI with query string behind this forwarded request, if any.
	 * @param request The involved HTTP servlet request.
	 * @return The original HTTP request URI with query string behind this forwarded request, if any.
	 * @since 1.8
	 */
	public static String getForwardRequestURIWithQueryString(HttpServletRequest request) {
		String requestURI = getForwardRequestURI(request);
		String queryString = getForwardRequestQueryString(request);
		return (queryString == null) ? requestURI : (requestURI + "?" + queryString);
	}

	/**
	 * Converts the given request query string to request parameter values map.
	 * @param queryString The request query string.
	 * @return The request query string as request parameter values map.
	 * @since 1.7
	 */
	public static Map<String, List<String>> toParameterMap(String queryString) {
		String[] parameters = queryString.split(quote("&"));
		Map<String, List<String>> parameterMap = new LinkedHashMap<>(parameters.length);

		for (String parameter : parameters) {
			if (parameter.contains("=")) {
				String[] pair = parameter.split(quote("="));
				String key = decodeURL(pair[0]);
				String value = (pair.length > 1 && !isEmpty(pair[1])) ? decodeURL(pair[1]) : "";
				List<String> values = parameterMap.get(key);

				if (values == null) {
					values = new ArrayList<>(1);
					parameterMap.put(key, values);
				}

				values.add(value);
			}
		}

		return parameterMap;
	}

	/**
	 * Converts the given request parameter values map to request query string.
	 * @param parameterMap The request parameter values map.
	 * @return The request parameter values map as request query string.
	 * @since 2.0
	 */
	public static String toQueryString(Map<String, List<String>> parameterMap) {
		StringBuilder queryString = new StringBuilder();

		for (Entry<String, List<String>> entry : parameterMap.entrySet()) {
			String name = encodeURL(entry.getKey());

			for (String value : entry.getValue()) {
				if (queryString.length() > 0) {
					queryString.append("&");
				}

				queryString.append(name).append("=").append(encodeURL(value));
			}
		}

		return queryString.toString();
	}

	// Cookies --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the value of the HTTP request cookie associated with the given name. The value is implicitly URL-decoded
	 * with a charset of UTF-8.
	 * @param request The involved HTTP servlet request.
	 * @param name The HTTP request cookie name.
	 * @return The value of the HTTP request cookie associated with the given name.
	 * @throws UnsupportedOperationException When this platform does not support UTF-8.
	 * @see HttpServletRequest#getCookies()
	 * @since 2.0
	 */
	public static String getRequestCookie(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();

		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals(name)) {
					return decodeURL(cookie.getValue());
				}
			}
		}

		return null;
	}

	/**
	 * Add a cookie with given name, value and maxage to the HTTP response. The cookie value will implicitly be
	 * URL-encoded with UTF-8 so that any special characters can be stored in the cookie. The cookie will implicitly
	 * be set to secure when the current request is secure (i.e. when the current request is a HTTPS request). The
	 * cookie will implicitly be set in the domain and path of the current request URL.
	 * @param request The involved HTTP servlet request.
	 * @param response The involved HTTP servlet response.
	 * @param name The cookie name.
	 * @param value The cookie value.
	 * @param maxAge The maximum age of the cookie, in seconds. If this is <code>0</code>, then the cookie will be
	 * removed. Note that the name and path must be exactly the same as it was when the cookie was created. If this is
	 * <code>-1</code> then the cookie will become a session cookie and thus live as long as the established HTTP
	 * session.
	 * @throws UnsupportedOperationException When this platform does not support UTF-8.
	 * @see HttpServletResponse#addCookie(Cookie)
	 * @since 2.0
	 */
	public static void addResponseCookie(HttpServletRequest request, HttpServletResponse response,
		String name, String value, int maxAge)
	{
		addResponseCookie(request, response, name, value, getRequestHostname(request), null, maxAge);
	}

	/**
	 * Add a cookie with given name, value, path and maxage to the HTTP response. The cookie value will implicitly be
	 * URL-encoded with UTF-8 so that any special characters can be stored in the cookie. The cookie will implicitly
	 * be set to secure when the current request is secure (i.e. when the current request is a HTTPS request). The
	 * cookie will implicitly be set in the domain of the current request URL.
	 * @param request The involved HTTP servlet request.
	 * @param response The involved HTTP servlet response.
	 * @param name The cookie name.
	 * @param value The cookie value.
	 * @param path The cookie path. If this is <code>/</code>, then the cookie is available in all pages of the webapp.
	 * If this is <code>/somespecificpath</code>, then the cookie is only available in pages under the specified path.
	 * @param maxAge The maximum age of the cookie, in seconds. If this is <code>0</code>, then the cookie will be
	 * removed. Note that the name and path must be exactly the same as it was when the cookie was created. If this is
	 * <code>-1</code> then the cookie will become a session cookie and thus live as long as the established HTTP
	 * session.
	 * @throws UnsupportedOperationException When this platform does not support UTF-8.
	 * @see HttpServletResponse#addCookie(Cookie)
	 * @since 2.0
	 */
	public static void addResponseCookie(HttpServletRequest request, HttpServletResponse response,
		String name, String value, String path, int maxAge)
	{
		addResponseCookie(request, response, name, value, getRequestHostname(request), path, maxAge);
	}

	/**
	 * Add a cookie with given name, value, domain, path and maxage to the HTTP response. The cookie value will
	 * implicitly be URL-encoded with UTF-8 so that any special characters can be stored in the cookie. The cookie will
	 * implicitly be set to secure when the current request is secure (i.e. when the current request is a HTTPS request).
	 * @param request The involved HTTP servlet request.
	 * @param response The involved HTTP servlet response.
	 * @param name The cookie name.
	 * @param value The cookie value.
	 * @param domain The cookie domain. You can use <code>.example.com</code> (with a leading period) if you'd like the
	 * cookie to be available to all subdomains of the domain. Note that you cannot set it to a different domain.
	 * @param path The cookie path. If this is <code>/</code>, then the cookie is available in all pages of the webapp.
	 * If this is <code>/somespecificpath</code>, then the cookie is only available in pages under the specified path.
	 * @param maxAge The maximum age of the cookie, in seconds. If this is <code>0</code>, then the cookie will be
	 * removed. Note that the name and path must be exactly the same as it was when the cookie was created. If this is
	 * <code>-1</code> then the cookie will become a session cookie and thus live as long as the established HTTP
	 * session.
	 * @throws UnsupportedOperationException When this platform does not support UTF-8.
	 * @see HttpServletResponse#addCookie(Cookie)
	 * @since 2.0
	 */
	public static void addResponseCookie(HttpServletRequest request, HttpServletResponse response,
		String name, String value, String domain, String path, int maxAge)
	{
		if (value != null) {
			value = encodeURL(value);
		}

		Cookie cookie = new Cookie(name, value);

		if (domain != null) {
			cookie.setDomain(domain);
		}

		if (path != null) {
			cookie.setPath(path);
		}

		cookie.setMaxAge(maxAge);
		cookie.setSecure(request.isSecure());
		response.addCookie(cookie);
	}

	/**
	 * Remove the cookie with given name and path from the HTTP response. Note that the name and path must be exactly
	 * the same as it was when the cookie was created.
	 * @param request The involved HTTP servlet request.
	 * @param response The involved HTTP servlet response.
	 * @param name The cookie name.
	 * @param path The cookie path.
	 * @see HttpServletResponse#addCookie(Cookie)
	 * @since 2.0
	 */
	public static void removeResponseCookie(HttpServletRequest request, HttpServletResponse response,
		String name, String path)
	{
		addResponseCookie(request, response, name, null, path, 0);
	}

	// ServletContext -------------------------------------------------------------------------------------------------

	/**
	 * Returns the application scope attribute value associated with the given name.
	 * @param <T> The expected return type.
	 * @param context The servlet context used for looking up the attribute.
	 * @param name The application scope attribute name.
	 * @return The application scope attribute value associated with the given name.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see ServletContext#getAttribute(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getApplicationAttribute(ServletContext context, String name) {
		return (T) context.getAttribute(name);
	}

	// JSF ------------------------------------------------------------------------------------------------------------

	/**
	 * Returns <code>true</code> if the given HTTP servlet request is a JSF ajax request. This does exactly the same as
	 * {@link Faces#isAjaxRequest()}, but then without the need for a {@link FacesContext}. The major advantage is that
	 * you can perform the job inside a servlet filter, where the {@link FacesContext} is normally not available.
	 * @param request The involved HTTP servlet request.
	 * @return <code>true</code> if the given HTTP servlet request is a JSF ajax request.
	 * @since 2.0
	 */
	public static boolean isFacesAjaxRequest(HttpServletRequest request) {
		return FACES_AJAX_HEADERS.contains(request.getHeader("Faces-Request"));
	}

	/**
	 * Returns <code>true</code> if the given HTTP servlet request is a JSF resource request. I.e. this request will
	 * trigger the JSF {@link ResourceHandler} for among others CSS/JS/image resources.
	 * @param request The involved HTTP servlet request.
	 * @return <code>true</code> if the given HTTP servlet request is a JSF resource request.
	 * @since 2.0
	 * @see ResourceHandler#RESOURCE_IDENTIFIER
	 */
	public static boolean isFacesResourceRequest(HttpServletRequest request) {
		return request.getRequestURI().startsWith(request.getContextPath() + ResourceHandler.RESOURCE_IDENTIFIER + "/");
	}

	/**
	 * Returns <code>true</code> if we're in JSF development stage. This will be the case when the
	 * <code>javax.faces.PROJECT_STAGE</code> context parameter in <code>web.xml</code> is set to
	 * <code>Development</code>.
	 * @param context The involved servlet context.
	 * @return <code>true</code> if we're in development stage, otherwise <code>false</code>.
	 * @since 2.1
	 * @see Application#getProjectStage()
	 */
	public static boolean isFacesDevelopment(ServletContext context) {
		if (facesDevelopment != null) {
			return facesDevelopment;
		}

		String projectStage = getEnvEntry(PROJECT_STAGE_JNDI_NAME);

		if (projectStage == null) {
			projectStage = context.getInitParameter(PROJECT_STAGE_PARAM_NAME);
		}

		return (facesDevelopment = Development.name().equals(projectStage));
	}

	/**
	 * Sends a temporary (302) JSF redirect to the given URL, supporting JSF ajax requests. This does exactly the same
	 * as {@link Faces#redirect(String, String...)}, but without the need for a {@link FacesContext}. The major
	 * advantage is that you can perform the job inside a servlet filter or even a plain vanilla servlet, where the
	 * {@link FacesContext} is normally not available. This method also recognizes JSF ajax requests which requires a
	 * special XML response in order to successfully perform the redirect.
	 * <p>
	 * If the given URL does <b>not</b> start with <code>http://</code>, <code>https://</code> or <code>/</code>, then
	 * the request context path will be prepended, otherwise it will be the unmodified redirect URL. So, when
	 * redirecting to another page in the same web application, always specify the full path from the context root on
	 * (which in turn does not need to start with <code>/</code>).
	 * <pre>
	 * Servlets.facesRedirect(request, response, "some.xhtml");
	 * </pre>
	 * <p>
	 * You can use {@link String#format(String, Object...)} placeholder <code>%s</code> in the redirect URL to represent
	 * placeholders for any request parameter values which needs to be URL-encoded. Here's a concrete example:
	 * <pre>
	 * Servlets.facesRedirect(request, response, "some.xhtml?foo=%s&amp;bar=%s", foo, bar);
	 * </pre>
	 * @param request The involved HTTP servlet request.
	 * @param response The involved HTTP servlet response.
	 * @param url The URL to redirect the current response to.
	 * @param paramValues The request parameter values which you'd like to put URL-encoded in the given URL.
	 * @throws IOException Whenever something fails at I/O level. The caller should preferably not catch it, but just
	 * redeclare it in the action method. The servletcontainer will handle it.
	 * @since 2.0
	 */
	public static void facesRedirect
		(HttpServletRequest request, HttpServletResponse response, String url, String ... paramValues)
			throws IOException
	{
		String redirectURL = prepareRedirectURL(request, url, paramValues);

		if (isFacesAjaxRequest(request)) {
			CacheControlFilter.setNoCacheHeaders(response);
			response.setContentType("text/xml");
			response.setCharacterEncoding(UTF_8.name());
			response.getWriter().printf(FACES_AJAX_REDIRECT_XML, redirectURL);
		}
		else {
			response.sendRedirect(redirectURL);
		}
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Helper method to prepare redirect URL. Package-private so that {@link FacesLocal} can also use it.
	 */
	static String prepareRedirectURL(HttpServletRequest request, String url, String... paramValues) {
		if (!startsWithOneOf(url, "http://", "https://", "/")) {
			url = request.getContextPath() + "/" + url;
		}

		if (isEmpty(paramValues)) {
			return url;
		}

		Object[] encodedParams = new Object[paramValues.length];

		for (int i = 0; i < paramValues.length; i++) {
			encodedParams[i] = encodeURL(paramValues[i]);
		}

		return String.format(url, encodedParams);
	}

}