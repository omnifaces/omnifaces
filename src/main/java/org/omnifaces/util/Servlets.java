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

import static java.util.regex.Pattern.quote;
import static org.omnifaces.util.Utils.decodeURL;
import static org.omnifaces.util.Utils.isEmpty;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/**
 * Collection of utility methods for the Servlet API in general.
 *
 * @author Arjan Tijms
 * @author Bauke Scholtz
 * @since 1.6
 */
public final class Servlets {

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
	 * Converts the given query string to request parameter values map.
	 * @param queryString The query string.
	 * @return The query string as parameter values map.
	 * @since 1.7
	 */
	public static Map<String, List<String>> toParameterMap(String queryString) {
		String[] parameters = queryString.split(quote("&"));
		Map<String, List<String>> parameterMap = new LinkedHashMap<String, List<String>>(parameters.length);

		for (String parameter : parameters) {
			if (parameter.contains("=")) {
				String[] pair = parameter.split(quote("="));
				String key = decodeURL(pair[0]);
				String value = (pair.length > 1 && !isEmpty(pair[1])) ? decodeURL(pair[1]) : "";
				List<String> values = parameterMap.get(key);

				if (values == null) {
					values = new ArrayList<String>(1);
					parameterMap.put(key, values);
				}

				values.add(value);
			}
		}

		return parameterMap;
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

}
