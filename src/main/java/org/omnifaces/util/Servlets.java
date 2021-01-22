/*
 * Copyright 2021 OmniFaces
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
package org.omnifaces.util;

import static jakarta.faces.application.ProjectStage.Development;
import static jakarta.faces.application.ProjectStage.PROJECT_STAGE_JNDI_NAME;
import static jakarta.faces.application.ProjectStage.PROJECT_STAGE_PARAM_NAME;
import static jakarta.servlet.RequestDispatcher.ERROR_REQUEST_URI;
import static jakarta.servlet.RequestDispatcher.FORWARD_QUERY_STRING;
import static jakarta.servlet.RequestDispatcher.FORWARD_REQUEST_URI;
import static jakarta.servlet.http.HttpServletResponse.SC_MOVED_PERMANENTLY;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.logging.Level.FINEST;
import static java.util.regex.Pattern.quote;
import static org.omnifaces.util.JNDI.lookup;
import static org.omnifaces.util.Utils.coalesce;
import static org.omnifaces.util.Utils.decodeURL;
import static org.omnifaces.util.Utils.encodeURI;
import static org.omnifaces.util.Utils.encodeURL;
import static org.omnifaces.util.Utils.isAnyEmpty;
import static org.omnifaces.util.Utils.isEmpty;
import static org.omnifaces.util.Utils.isOneOf;
import static org.omnifaces.util.Utils.startsWithOneOf;
import static org.omnifaces.util.Utils.unmodifiableSet;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.faces.FactoryFinder;
import jakarta.faces.application.Application;
import jakarta.faces.application.ResourceHandler;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.lifecycle.Lifecycle;
import jakarta.faces.lifecycle.LifecycleFactory;
import jakarta.faces.webapp.FacesServlet;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import org.omnifaces.component.ParamHolder;
import org.omnifaces.facesviews.FacesViews;

/**
 * <p>
 * Collection of utility methods for the Servlet API in general. Most of them are internally used by {@link Faces}
 * and {@link FacesLocal}, however they may also be useful in a "plain vanilla" servlet or servlet filter.
 * <p>
 * There are as of now also five special methods related to JSF without needing a {@link FacesContext}:
 * <ul>
 * <li>The {@link #getFacesLifecycle(ServletContext)} which returns the JSF lifecycle, allowing you a.o. to
 * programmatically register JSF application's phase listeners.
 * <li>The {@link #isFacesAjaxRequest(HttpServletRequest)} which is capable of checking if the current request is a JSF
 * ajax request.
 * <li>The {@link #isFacesResourceRequest(HttpServletRequest)} which is capable of checking if the current request is a
 * JSF resource request.
 * <li>The {@link #facesRedirect(HttpServletRequest, HttpServletResponse, String, Object...)} which is capable
 * of distinguishing JSF ajax requests from regular requests and altering the redirect logic on it, exactly like as
 * {@link ExternalContext#redirect(String)} does. In other words, this method behaves exactly the same as
 * {@link Faces#redirect(String, Object...)}.
 * <li>The {@link #isFacesDevelopment(ServletContext)} which is capable of checking if the current JSF application
 * configuration is set to development project stage.
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

	private static final Logger logger = Logger.getLogger(Servlets.class.getName());

	private static final String CONTENT_DISPOSITION_HEADER = "%s;filename=\"%2$s\"; filename*=UTF-8''%2$s";
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
	 * @throws IllegalArgumentException When the URL is malformed. This is however unexpected as the request would
	 * otherwise not have hit the server at all.
	 * @see HttpServletRequest#getRequestURL()
	 */
	public static String getRequestDomainURL(HttpServletRequest request) {
		try {
			URL url = new URL(request.getRequestURL().toString());
			String fullURL = url.toString();
			return fullURL.substring(0, fullURL.length() - url.getPath().length());
		}
		catch (MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
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
	 * Returns the HTTP request URI, regardless of any forward or error dispatch. This is the part after the domain in
	 * the request URL, including the leading slash.
	 * @param request The involved HTTP servlet request.
	 * @return The HTTP request URI, regardless of any forward or error dispatch.
	 * @since 2.4
	 * @see HttpServletRequest#getRequestURI()
	 * @see RequestDispatcher#FORWARD_REQUEST_URI
	 * @see RequestDispatcher#ERROR_REQUEST_URI
	 */
	public static String getRequestURI(HttpServletRequest request) {
		return coalesce((String) request.getAttribute(ERROR_REQUEST_URI), (String) request.getAttribute(FORWARD_REQUEST_URI), request.getRequestURI());
	}

	/**
	 * Returns the HTTP request path info, taking into account whether FacesViews is used with MultiViews enabled.
	 * If the resource is prefix mapped (e.g. <code>/faces/*</code>), then this returns the whole part after the prefix
	 * mapping, with a leading slash. If the resource is suffix mapped (e.g. <code>*.xhtml</code>), then this returns
	 * <code>null</code>.
	 * @param request The involved HTTP servlet request.
	 * @return The HTTP request path info.
	 * @since 2.5
	 * @see HttpServletRequest#getPathInfo()
	 * @see FacesViews#FACES_VIEWS_ORIGINAL_PATH_INFO
	 */
	public static String getRequestPathInfo(HttpServletRequest request) {
		return coalesce((String) request.getAttribute(FacesViews.FACES_VIEWS_ORIGINAL_PATH_INFO), request.getPathInfo());
	}

	/**
	 * Returns the HTTP request query string, regardless of any forward.
	 * @param request The involved HTTP servlet request.
	 * @return The HTTP request query string, regardless of any forward.
	 * @since 2.4
	 * @see HttpServletRequest#getRequestURI()
	 * @see RequestDispatcher#FORWARD_QUERY_STRING
	 */
	public static String getRequestQueryString(HttpServletRequest request) {
		return coalesce((String) request.getAttribute(FORWARD_QUERY_STRING), request.getQueryString());
	}

	/**
	 * Returns the HTTP request query string as parameter values map. Note this method returns <strong>only</strong>
	 * the request URL (GET) parameters, as opposed to {@link HttpServletRequest#getParameterMap()}, which contains both
	 * the request URL (GET) parameters and and the request body (POST) parameters.
	 * The map entries are in the same order as they appear in the query string.
	 * @param request The involved HTTP servlet request.
	 * @return The HTTP request query string as parameter values map.
	 */
	public static Map<String, List<String>> getRequestQueryStringMap(HttpServletRequest request) {
		String queryString = getRequestQueryString(request);

		if (isEmpty(queryString)) {
			return new LinkedHashMap<>(0);
		}

		return toParameterMap(queryString);
	}

	/**
	 * Returns the HTTP request parameter map. Note this method returns the values as a <code>List&lt;String&gt;</code>,
	 * as opposed to {@link HttpServletRequest#getParameterMap()}, which returns the values as <code>String[]</code>.
	 * The map entries are not per definition ordered, but the values are.
	 * @param request The involved HTTP servlet request.
	 * @return The HTTP request parameter map.
	 * @since 2.6
	 */
	public static Map<String, List<String>> getRequestParameterMap(HttpServletRequest request) {
		Map<String, List<String>> parameterMap = new HashMap<>(request.getParameterMap().size());

		for (Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
			parameterMap.put(entry.getKey(), asList(entry.getValue()));
		}

		return parameterMap;
	}

	/**
	 * Returns the HTTP request URI with query string, regardless of any forward. This is the part after the domain in
	 * the request URL, including the leading slash and the request query string.
	 * @param request The involved HTTP servlet request.
	 * @return The HTTP request URI with query string.
	 * @see #getRequestURI(HttpServletRequest)
	 * @see #getRequestQueryString(HttpServletRequest)
	 */
	public static String getRequestURIWithQueryString(HttpServletRequest request) {
		String requestURI = getRequestURI(request);
		String queryString = getRequestQueryString(request);
		return (queryString == null) ? requestURI : (requestURI + "?" + queryString);
	}

	/**
	 * Returns the HTTP request URI relative to the context root, regardless of any forward. This is the request URI
	 * minus the context path. Note that this includes path parameters.
	 * @param request The involved HTTP servlet request.
	 * @return The HTTP request URI relative to the context root.
	 * @since 1.8
	 */
	public static String getRequestRelativeURI(HttpServletRequest request) {
		return getRequestURI(request).substring(request.getContextPath().length());
	}

	/**
	 * Returns the HTTP request URI relative to the context root without path parameters, regardless of any forward.
	 * This is the request URI minus the context path and path parameters.
	 * @param request The involved HTTP servlet request.
	 * @return The HTTP request URI relative to the context root without path parameters.
	 * @since 1.8
	 */
	public static String getRequestRelativeURIWithoutPathParameters(HttpServletRequest request) {
		return getRequestRelativeURI(request).split(";", 2)[0];
	}

	/**
	 * Returns the HTTP request URL with query string, regardless of any forward. This is the full request URL without
	 * query string as the enduser sees in browser address bar.
	 * @param request The involved HTTP servlet request.
	 * @return The HTTP request URL without query string, regardless of any forward.
	 * @since 2.4
	 * @see HttpServletRequest#getRequestURL()
	 */
	public static String getRequestURL(HttpServletRequest request) {
		return getRequestDomainURL(request) + getRequestURI(request);
	}

	/**
	 * Returns the HTTP request URL with query string. This is the full request URL with query string as the enduser
	 * sees in browser address bar.
	 * @param request The involved HTTP servlet request.
	 * @return The HTTP request URL with query string, regardless of any forward.
	 * @see HttpServletRequest#getRequestURL()
	 * @see HttpServletRequest#getQueryString()
	 */
	public static String getRequestURLWithQueryString(HttpServletRequest request) {
		return getRequestDomainURL(request) + getRequestURIWithQueryString(request);
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
				addParamToMapIfNecessary(parameterMap, key, value);
			}
		}

		return parameterMap;
	}

	/**
	 * Converts the given request parameter values map to request query string.
	 * Empty names and null values will be skipped.
	 * @param parameterMap The request parameter values map.
	 * @return The request parameter values map as request query string.
	 * @since 2.0
	 */
	public static String toQueryString(Map<String, List<String>> parameterMap) {
		StringBuilder queryString = new StringBuilder();

		for (Entry<String, List<String>> entry : parameterMap.entrySet()) {
			if (isEmpty(entry.getKey())) {
				continue;
			}

			String name = encodeURL(entry.getKey());

			for (String value : entry.getValue()) {
				if (value == null) {
					continue;
				}

				if (queryString.length() > 0) {
					queryString.append("&");
				}

				queryString.append(name).append("=").append(encodeURL(value));
			}
		}

		return queryString.toString();
	}

	/**
	 * Converts the given parameter values list to request query string.
	 * Empty names and null values will be skipped.
	 * @param params The parameter values list.
	 * @return The parameter values list as request query string.
	 * @since 2.2
	 */
	public static String toQueryString(List<? extends ParamHolder<?>> params) {
		StringBuilder queryString = new StringBuilder();

		for (ParamHolder<?> param : params) {
			if (isEmpty(param.getName())) {
				continue;
			}

			String value = param.getValue();

			if (value != null) {
				if (queryString.length() > 0) {
					queryString.append("&");
				}

				queryString.append(encodeURL(param.getName())).append("=").append(encodeURL(value));
			}
		}

		return queryString.toString();
	}

	/**
	 * Returns the Internet Protocol (IP) address of the client that sent the request. This will first check the
	 * <code>Forwarded</code> and <code>X-Forwarded-For</code> request headers and if any is present, then return its
	 * first IP address, else just return {@link HttpServletRequest#getRemoteAddr()} unmodified.
	 * @param request The involved HTTP servlet request.
	 * @return The IP address of the client.
	 * @see HttpServletRequest#getRemoteAddr()
	 * @since 2.3
	 */
	public static String getRemoteAddr(HttpServletRequest request) {
		String forwardedFor = coalesce(request.getHeader("Forwarded"), request.getHeader("X-Forwarded-For"));
		return isEmpty(forwardedFor) ? request.getRemoteAddr() : forwardedFor.split("\\s*,\\s*", 2)[0]; // It's a comma separated string: client,proxy1,proxy2,...
	}

	/**
	 * Returns <code>true</code> if request is proxied, <code>false</code> otherwise. In other words, returns
	 * <code>true</code> when either <code>Forwarded</code> or <code>X-Forwarded-For</code> request headers is present.
	 * @param request The involved HTTP servlet request.
	 * @return <code>true</code> if request is proxied, <code>false</code> otherwise.
	 * @see HttpServletRequest#getHeader(String)
	 * @since 3.6
	 */
	public static boolean isProxied(HttpServletRequest request) {
		return !isEmpty(coalesce(request.getHeader("Forwarded"), request.getHeader("X-Forwarded-For")));
	}

	/**
	 * Returns the User-Agent string of the client.
	 * @param request The involved HTTP servlet request.
	 * @return The User-Agent string of the client.
	 * @see HttpServletRequest#getHeader(String)
	 * @since 3.2
	 */
	public static String getUserAgent(HttpServletRequest request) {
		return request.getHeader("User-Agent");
	}

	/**
	 * Returns the referrer of the request.
	 * @param request The involved HTTP servlet request.
	 * @return The referrer of the request.
	 * @see HttpServletRequest#getHeader(String)
	 * @since 3.10
	 */
	public static String getReferrer(HttpServletRequest request) {
		return request.getHeader("Referer"); // Yes, typo is set in stone, see https://en.wikipedia.org/wiki/HTTP_referer#Etymology
	}

	/**
	 * Returns <code>true</code> if connection is secure, <code>false</code> otherwise. This method will first check if
	 * {@link HttpServletRequest#isSecure()} returns <code>true</code>, and if not <code>true</code>, check if the
	 * <code>X-Forwarded-Proto</code> is present and equals to <code>https</code>.
	 * @param request The involved HTTP servlet request.
	 * @return <code>true</code> if connection is secure, <code>false</code> otherwise.
	 * @see HttpServletRequest#isSecure()
	 * @since 3.0
	 */
	public static boolean isSecure(HttpServletRequest request) {
		return request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
	}

	/**
	 * Returns the submitted file name of the given part, making sure that any path is stripped off. Some browsers
	 * are known to incorrectly include the client side path along with it. Since version 2.6.7, RFC-2231/5987 encoded
	 * file names are also supported.
	 * @param part The part of a multipart/form-data request.
	 * @return The submitted file name of the given part, or null if there is none.
	 * @since 2.5
	 */
	public static String getSubmittedFileName(Part part) {
		Map<String, String> entries = headerToMap(part.getHeader("Content-Disposition"));
		String encodedFileName = entries.get("filename*");
		String fileName = null;

		if (encodedFileName != null) {
			String[] parts = encodedFileName.split("'", 3);

			if (parts.length == 3 && !isEmpty(parts[0])) {
				try {
					fileName = URLDecoder.decode(parts[2], Charset.forName(parts[0]).name());
				}
				catch (IllegalArgumentException | UnsupportedEncodingException ignore) {
					logger.log(Level.FINEST, "Ignoring thrown exception, falling back to default filename", ignore);
				}
			}
		}

		if (fileName == null) {
			fileName = entries.get("filename");
		}

		if (fileName != null) {
			if (fileName.matches("^[A-Za-z]:\\\\.*")) {
				fileName = fileName.substring(fileName.lastIndexOf('\\') + 1); // Fakepath fix.
			}

			return fileName.substring(fileName.lastIndexOf('/') + 1).replace("\\", ""); // MSIE fix.
		}

		return null;

	}

	/**
	 * Returns a mapping of given semicolon-separated request header. The returned map is unordered and unmodifiable.
	 * @param header Any semicolon-separated request header, e.g. <code>Content-Disposition</code>.
	 * @return A mapping of given semicolon-separated request header.
	 * @since 3.0
	 */
	public static Map<String, String> headerToMap(String header) {
		if (isEmpty(header)) {
			return emptyMap();
		}

		Map<String, String> map = new HashMap<>();
		StringBuilder builder = new StringBuilder();
		boolean quoted = false;

		for (int i = 0; i < header.length(); i++) {
			char c = header.charAt(i);
			builder.append(c);

			if (c == '"' && i > 0 && (header.charAt(i - 1) != '\\' || (i > 1 && header.charAt(i - 2) == '\\'))) {
				quoted = !quoted;
			}

			if ((!quoted && c == ';') || i + 1 == header.length()) {
				String[] entry = builder.toString().replaceAll(";$", "").trim().split("\\s*=\\s*", 2);
				String name = entry[0].toLowerCase();
				String value = entry.length == 1 ? ""
					: entry[1].replaceAll("^\"|\"$", "") // Trim leading and trailing quotes.
						.replace("\\\"", "\"") // Unescape quotes.
						.replaceAll("%\\\\([0-9]{2})", "%$1") // Unescape %xx.
						.trim();
				map.put(name, value);
				builder = new StringBuilder();
			}
		}

		return unmodifiableMap(map);
	}

	// HttpServletResponse --------------------------------------------------------------------------------------------

	/**
	 * <p>Set the cache headers. If the <code>expires</code> argument is larger than 0 seconds, then the following headers
	 * will be set:
	 * <ul>
	 * <li><code>Cache-Control: public,max-age=[expiration time in seconds],must-revalidate</code></li>
	 * <li><code>Expires: [expiration date of now plus expiration time in seconds]</code></li>
	 * </ul>
	 * <p>Else the method will delegate to {@link #setNoCacheHeaders(HttpServletResponse)}.
	 * @param response The HTTP servlet response to set the headers on.
	 * @param expires The expire time in seconds (not milliseconds!).
	 * @since 2.2
	 */
	public static void setCacheHeaders(HttpServletResponse response, long expires) {
		if (expires > 0) {
			response.setHeader("Cache-Control", "public,max-age=" + expires + ",must-revalidate");
			response.setDateHeader("Expires", System.currentTimeMillis() + SECONDS.toMillis(expires));
			response.setHeader("Pragma", ""); // Explicitly set pragma to prevent container from overriding it.
		}
		else {
			setNoCacheHeaders(response);
		}
	}

	/**
	 * <p>Set the no-cache headers. The following headers will be set:
	 * <ul>
	 * <li><code>Cache-Control: no-cache,no-store,must-revalidate</code></li>
	 * <li><code>Expires: [expiration date of 0]</code></li>
	 * <li><code>Pragma: no-cache</code></li>
	 * </ul>
	 * Set the no-cache headers.
	 * @param response The HTTP servlet response to set the headers on.
	 * @since 2.2
	 */
	public static void setNoCacheHeaders(HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-cache,no-store,must-revalidate");
		response.setDateHeader("Expires", 0);
		response.setHeader("Pragma", "no-cache"); // Backwards compatibility for HTTP 1.0.
	}

	/**
	 * <p>Format an UTF-8 compatible content disposition header for the given filename and whether it's an attachment.
	 * @param filename The filename to appear in "Save As" dialogue.
	 * @param attachment Whether the content should be provided as an attachment or inline.
	 * @return An UTF-8 compatible content disposition header.
	 * @since 2.6
	 */
	public static String formatContentDispositionHeader(String filename, boolean attachment) {
		return format(CONTENT_DISPOSITION_HEADER, (attachment ? "attachment" : "inline"), encodeURI(filename));
	}

	/**
	 * Sends a permanent (301) redirect to the given URL.
	 * @param response The involved HTTP servlet response.
     * @param url The URL to permanently redirect the current response to.
     * @see HttpServletResponse#setStatus(int)
     * @see HttpServletResponse#setHeader(String, String)
     * @since 3.6
     */
	public static void redirectPermanent(HttpServletResponse response, String url) {
		response.setStatus(SC_MOVED_PERMANENTLY);
		response.setHeader("Location", url);
		response.setHeader("Connection", "close");
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
	 * Add a cookie with given name, value and maxage to the HTTP response.
	 * The cookie value will implicitly be URL-encoded with UTF-8 so that any special characters can be stored.
	 * The cookie will implicitly be set in the domain and path of the current request URL.
	 * The cookie will implicitly be set to HttpOnly as JavaScript is not supposed to manipulate server-created cookies.
	 * The cookie will implicitly be set to secure when the current request is a HTTPS request.
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
	 * Add a cookie with given name, value, path and maxage to the HTTP response.
	 * The cookie value will implicitly be URL-encoded with UTF-8 so that any special characters can be stored.
	 * The cookie will implicitly be set in the domain of the current request URL.
	 * The cookie will implicitly be set to HttpOnly as JavaScript is not supposed to manipulate server-created cookies.
	 * The cookie will implicitly be set to secure when the current request is a HTTPS request.
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
	 * Add a cookie with given name, value, domain, path and maxage to the HTTP response.
	 * The cookie value will implicitly be URL-encoded with UTF-8 so that any special characters can be stored.
	 * The cookie will implicitly be set to HttpOnly as JavaScript is not supposed to manipulate server-created cookies.
	 * The cookie will implicitly be set to secure when the current request is a HTTPS request.
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
		Cookie cookie = new Cookie(name, encodeURL(value));

		if (!isOneOf(domain, null, "localhost")) { // Chrome doesn't like domain:"localhost" on cookies.
			cookie.setDomain(domain);
		}

		if (path != null) {
			cookie.setPath(path);
		}

		cookie.setMaxAge(maxAge);
		cookie.setHttpOnly(true);
		cookie.setSecure(isSecure(request));
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
	 * Returns the servlet context.
	 * If the JSF context is available, then return it from there.
	 * Else if the CDI bean manager is available, then return it from there.
	 * @return The servlet context.
	 * @since 3.10
	 * @see Faces#getServletContext()
	 * @see Beans#getInstance(Bean, boolean)
	 */
	public static ServletContext getContext() {
		if (Faces.hasContext()) {
			return Faces.getServletContext();
		}

		BeanManager beanManager = Beans.getManager();

		if (BeansLocal.isActive(beanManager, RequestScoped.class)) {
			return BeansLocal.getInstance(beanManager, ServletContext.class);
		}
		else {
			// #522 For some reason Weld by default searches for the ServletContext in the request scope.
			// But this won't work during e.g. startup. So we need to explicitly search in application scope.
			Bean<ServletContext> bean = BeansLocal.resolve(beanManager, ServletContext.class);
			Context context = beanManager.getContext(ApplicationScoped.class);
			return context.get(bean, beanManager.createCreationalContext(bean));
		}
	}

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
	 * Returns The {@link Lifecycle} associated with current Faces application.
	 * @param context The involved servlet context.
	 * @return The {@link Lifecycle} associated with current Faces application.
	 * @see LifecycleFactory#getLifecycle(String)
	 * @since 2.5
	 */
	public static Lifecycle getFacesLifecycle(ServletContext context) {
		String lifecycleId = coalesce(context.getInitParameter(FacesServlet.LIFECYCLE_ID_ATTR), LifecycleFactory.DEFAULT_LIFECYCLE);
		return ((LifecycleFactory) FactoryFinder.getFactory(FactoryFinder.LIFECYCLE_FACTORY)).getLifecycle(lifecycleId);
	}

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
		return getRequestURI(request).startsWith(request.getContextPath() + ResourceHandler.RESOURCE_IDENTIFIER + "/");
	}

	/**
	 * Returns <code>true</code> if we're in JSF development stage. This will be the case when the
	 * <code>jakarta.faces.PROJECT_STAGE</code> context parameter in <code>web.xml</code> is set to
	 * <code>Development</code>.
	 * @param context The involved servlet context.
	 * @return <code>true</code> if we're in development stage, otherwise <code>false</code>.
	 * @since 2.1
	 * @see Application#getProjectStage()
	 */
	public static boolean isFacesDevelopment(ServletContext context) {
		if (facesDevelopment == null) {
			String projectStage = null;

			try {
				projectStage = lookup(PROJECT_STAGE_JNDI_NAME);
			}
			catch (IllegalStateException ignore) {
				logger.log(FINEST, "Ignoring thrown exception; will only happen in buggy containers.", ignore);
				return false; // May happen in a.o. GlassFish 4.1 during startup.
			}

			if (projectStage == null) {
				projectStage = context.getInitParameter(PROJECT_STAGE_PARAM_NAME);
			}

			facesDevelopment = Development.name().equals(projectStage);
		}

		return facesDevelopment;
	}

	/**
	 * Sends a temporary (302) JSF redirect to the given URL, supporting JSF ajax requests. This does exactly the same
	 * as {@link Faces#redirect(String, Object...)}, but without the need for a {@link FacesContext}. The major
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
	 * @throws UncheckedIOException Whenever something fails at I/O level.
	 * @since 2.0
	 */
	public static void facesRedirect(HttpServletRequest request, HttpServletResponse response, String url, Object... paramValues) {
		String redirectURL = prepareRedirectURL(request, url, paramValues);

		try {
			if (isFacesAjaxRequest(request)) {
				setNoCacheHeaders(response);
				response.setContentType("text/xml");
				response.setCharacterEncoding(UTF_8.name());
				response.getWriter().printf(FACES_AJAX_REDIRECT_XML, redirectURL);
			}
			else {
				response.sendRedirect(redirectURL);
			}
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Helper method to prepare redirect URL. Package-private so that {@link FacesLocal} can also use it.
	 */
	static String prepareRedirectURL(HttpServletRequest request, String url, Object... paramValues) {
		String redirectURL = url;

		if (!startsWithOneOf(url, "http://", "https://", "/")) {
			redirectURL = request.getContextPath() + "/" + url;
		}

		if (isEmpty(paramValues)) {
			return redirectURL;
		}

		Object[] encodedParams = new Object[paramValues.length];

		for (int i = 0; i < paramValues.length; i++) {
			Object paramValue = paramValues[i];
			encodedParams[i] = (paramValue instanceof String) ? encodeURL((String) paramValue) : paramValue;
		}

		return format(redirectURL, encodedParams);
	}

	/**
	 * Helper method to add param to map if necessary. Package-private so that {@link FacesLocal} can also use it.
	 */
	static void addParamToMapIfNecessary(Map<String, List<String>> map, String name, Object value) {
		if (isAnyEmpty(name, value)) {
			return;
		}

		map.computeIfAbsent(name, k -> new ArrayList<>(1)).add(value.toString());
	}

}