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
package org.omnifaces.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.faces.application.Application;
import javax.faces.application.NavigationHandler;
import javax.faces.application.ProjectStage;
import javax.faces.component.UIViewParameter;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.faces.context.PartialViewContext;
import javax.faces.event.PhaseId;
import javax.faces.view.ViewMetadata;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Collection of utility methods for the JSF API that are mainly shortcuts for obtaining stuff from the thread local
 * {@link FacesContext}. In effects, it 'flattens' the hierarchy of nested objects.
 * <p>
 * Do note that using the hierarchy is actually a better software design practice, but can lead to verbose code.
 * <p>
 * In addition, note that there's normally a small overhead in obtaining the thread local {@link FacesContext}. In case
 * client code needs to call methods in this class multiple times it's expected that performance will be slightly better
 * if instead the {@link FacesContext} is obtained once and the required methods called on that.
 *
 * @author Arjan Tijms
 * @author Bauke Scholtz
 */
public final class Faces {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String DEFAULT_MIME_TYPE = "application/octet-stream";
	private static final int DEFAULT_SENDFILE_BUFFER_SIZE = 10240;

	private static final String ERROR_UNSUPPORTED_ENCODING = "UTF-8 is apparently not supported on this machine.";

	// Constructors ---------------------------------------------------------------------------------------------------

	private Faces() {
		// Hide constructor.
	}

	// JSF general ----------------------------------------------------------------------------------------------------

	/**
	 * Returns the implementation information of currently loaded JSF implementation. E.g. "Mojarra 2.1.7-FCS".
	 * @return The implementation information of currently loaded JSF implementation.
	 * @see Package#getImplementationTitle()
	 * @see Package#getImplementationVersion()
	 */
	public static String getImplInfo() {
		Package jsfPackage = FacesContext.class.getPackage();
		return jsfPackage.getImplementationTitle() + " " + jsfPackage.getImplementationVersion();
	}

	/**
	 * Returns the server information of currently running application server implementation.
	 * @return The server information of currently running application server implementation.
	 * @see ServletContext#getServerInfo()
	 */
	public static String getServerInfo() {
		return getServletContext().getServerInfo();
	}

	/**
	 * Returns true if we're in development stage.
	 * @return True if we're in development stage.
	 * @see Application#getProjectStage()
	 */
	public static boolean isDevelopment() {
		return FacesContext.getCurrentInstance().getApplication().getProjectStage() == ProjectStage.Development;
	}

	/**
	 * Determines and returns the faces servlet mapping used in the current request. If the path info is
	 * <code>null</code>, then it is definitely a suffix (extension) mapping like <tt>*.xhtml</tt>, else it is
	 * definitely a prefix (path) mapping like <tt>/faces/*</tt> as available by the servlet path.
	 * @return The faces servlet mapping (without the wildcard).
	 */
	public static String getMapping() {
		ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();

		if (externalContext.getRequestPathInfo() == null) {
			String path = externalContext.getRequestServletPath();
			return path.substring(path.lastIndexOf('.'));
		}
		else {
			return externalContext.getRequestServletPath();
		}
	}

	/**
	 * Returns true if the faces servlet mapping used in the current request is a prefix mapping, otherwise false.
	 * @return True if the faces servlet mapping used in the current request is a prefix mapping, otherwise false.
	 */
	public static boolean isPrefixMapping() {
		return isPrefixMapping(getMapping());
	}

	/**
	 * Returns true if the given mapping is a prefix mapping, otherwise false. Use this method in preference to
	 * {@link #isPrefixMapping()} when you already have obtained the mapping from {@link #getMapping()} so that the
	 * mapping won't be calculated twice.
	 * @param mapping The mapping to be tested.
	 * @return True if the given mapping is a prefix mapping, otherwise false.
	 * @throws NullPointerException When mapping is <code>null</code>.
	 */
	public static boolean isPrefixMapping(String mapping) {
		return (mapping.charAt(0) == '/');
	}

	/**
	 * Returns the current phase ID.
	 * @return The current phase ID.
	 * @see FacesContext#getCurrentPhaseId()
	 */
	public static PhaseId getCurrentPhaseId() {
		return FacesContext.getCurrentInstance().getCurrentPhaseId();
	}

	/**
	 * Programmatically evaluate the given EL expression and return the evaluated value.
	 * @param <T> The expected return type.
	 * @param expression The EL expression to be evaluated.
	 * @return The evaluated value of the given EL expression.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see Application#evaluateExpressionGet(FacesContext, String, Class)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T evaluateExpressionGet(String expression) {
		if (expression == null) {
			return null;
		}

		FacesContext context = FacesContext.getCurrentInstance();
		return (T) context.getApplication().evaluateExpressionGet(context, expression, Object.class);
	}

	// JSF views ------------------------------------------------------------------------------------------------------

	/**
	 * Returns the current view root.
	 * @return The current view root.
	 * @see FacesContext#getViewRoot()
	 */
	public static UIViewRoot getViewRoot() {
		return FacesContext.getCurrentInstance().getViewRoot();
	}

	/**
	 * Returns the ID of the current view root, or <code>null</code> if there is no view.
	 * @return The ID of the current view root, or <code>null</code> if there is no view.
	 * @see UIViewRoot#getViewId()
	 */
	public static String getViewId() {
		UIViewRoot viewRoot = getViewRoot();
		return (viewRoot != null) ? viewRoot.getViewId() : null;
	}

	/**
	 * Normalize the given path as a valid view ID based on the current mapping, if necessary.
	 * <ul>
	 * <li>If the current mapping is a prefix mapping and the given path starts with it, then remove it.
	 * <li>If the current mapping is a suffix mapping and the given path does not end with it, then replace it.
	 * </ul>
	 * @param path The path to be normalized as a valid view ID based on the current mapping.
	 * @return The path as a valid view ID.
	 */
	public static String normalizeViewId(String path) {
		String mapping = getMapping();

		if (isPrefixMapping(mapping)) {
			if (path.startsWith(mapping)) {
				return path.substring(mapping.length());
			}
		}
		else if (!path.endsWith(mapping)) {
			return path.substring(0, path.lastIndexOf('.')) + mapping;
		}

		return path;
	}

	/**
	 * Returns the current locale. If the locale set in the JSF view root is not null, then return it. Else if the
	 * client preferred locale is not null and is among supported locales, then return it. Else if the JSF default
	 * locale is not null, then return it. Else return the system default locale.
	 * @return The current locale.
	 * @see UIViewRoot#getLocale()
	 * @see ExternalContext#getRequestLocale()
	 * @see Application#getDefaultLocale()
	 * @see Locale#getDefault()
	 */
	public static Locale getLocale() {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		Locale locale = null;
		UIViewRoot viewRoot = facesContext.getViewRoot();

		// Prefer the locale set in the view.
		if (viewRoot != null) {
			locale = viewRoot.getLocale();
		}

		// Then the client preferred locale.
		if (locale == null) {
			Locale clientLocale = facesContext.getExternalContext().getRequestLocale();

			if (getSupportedLocales().contains(clientLocale)) {
				locale = clientLocale;
			}
		}

		// Then the JSF default locale.
		if (locale == null) {
			locale = facesContext.getApplication().getDefaultLocale();
		}

		// Finally the system default locale.
		if (locale == null) {
			locale = Locale.getDefault();
		}

		return locale;
	}

	/**
	 * Returns a list of all supported locales on this application, with the default locale as the first item, if any.
	 * This will return an empty list if there are no locales definied in <tt>faces-config.xml</tt>.
	 * @return A list of all supported locales on this application, with the default locale as the first item, if any.
	 * @see Application#getDefaultLocale()
	 * @see Application#getSupportedLocales()
	 */
	public static List<Locale> getSupportedLocales() {
		Application application = FacesContext.getCurrentInstance().getApplication();
		List<Locale> supportedLocales = new ArrayList<Locale>();
		Locale defaultLocale = application.getDefaultLocale();

		if (defaultLocale != null) {
			supportedLocales.add(defaultLocale);
		}

		for (Iterator<Locale> iter = application.getSupportedLocales(); iter.hasNext();) {
			Locale supportedLocale = iter.next();

			if (!supportedLocale.equals(defaultLocale)) {
				supportedLocales.add(supportedLocale);
			}
		}

		return supportedLocales;
	}

	/**
	 * Returns the view parameters of the current view, or an empty collection if there is no view.
	 * @return The view parameters of the current view, or an empty collection if there is no view.
	 * @see ViewMetadata#getViewParameters(UIViewRoot)
	 */
	public static Collection<UIViewParameter> getViewParams() {
		UIViewRoot viewRoot = getViewRoot();
		return (viewRoot != null) ? ViewMetadata.getViewParameters(viewRoot) : Collections.<UIViewParameter>emptyList();
	}

	/**
	 * Perform the JSF navigation to the given outcome.
	 * @param outcome The navigation outcome.
	 * @see Application#getNavigationHandler()
	 * @see NavigationHandler#handleNavigation(FacesContext, String, String)
	 */
	public static void navigate(String outcome) {
		FacesContext context = FacesContext.getCurrentInstance();
		context.getApplication().getNavigationHandler().handleNavigation(context, null, outcome);
	}

	// HTTP request/response ------------------------------------------------------------------------------------------

	/**
	 * Returns the HTTP servlet request.
	 * @return The HTTP servlet request.
	 * @see ExternalContext#getRequest()
	 */
	public static HttpServletRequest getRequest() {
		return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
	}

	/**
	 * Returns whether the current request is an ajax request.
	 * @return true for a postback, false if the request is a non-faces (non-postback) request.
	 * @see PartialViewContext#isAjaxRequest()
	 */
	public static boolean isAjaxRequest() {
		return FacesContext.getCurrentInstance().getPartialViewContext().isAjaxRequest();
	}

	/**
	 * Returns whether the current request is a postback.
	 * @return true for a postback, false if the request is a non-faces (non-postback) request.
	 * @see FacesContext#isPostback()
	 */
	public static boolean isPostback() {
		return FacesContext.getCurrentInstance().isPostback();
	}

	/**
	 * Returns the HTTP request parameter map.
	 * @return The HTTP request parameter map.
	 * @see ExternalContext#getRequestParameterMap()
	 */
	public static Map<String, String> getRequestParameterMap() {
		return FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
	}

	/**
	 * Returns the HTTP request parameter value associated with the given name.
	 * @param name The HTTP request parameter name.
	 * @return The HTTP request parameter value associated with the given name.
	 * @see ExternalContext#getRequestParameterMap()
	 */
	public static String getRequestParameter(String name) {
		return FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(name);
	}

	/**
	 * Returns the HTTP request parameter values map.
	 * @return The HTTP request parameter values map.
	 * @see ExternalContext#getRequestParameterValuesMap()
	 */
	public static Map<String, String[]> getRequestParameterValuesMap() {
		return FacesContext.getCurrentInstance().getExternalContext().getRequestParameterValuesMap();
	}

	/**
	 * Returns the HTTP request parameter values associated with the given name.
	 * @param name The HTTP request parameter name.
	 * @return The HTTP request parameter values associated with the given name.
	 * @see ExternalContext#getRequestParameterValuesMap()
	 */
	public static String[] getRequestParameterValues(String name) {
		return FacesContext.getCurrentInstance().getExternalContext().getRequestParameterValuesMap().get(name);
	}

	/**
	 * Returns the HTTP request header map.
	 * @return The HTTP request header map.
	 * @see ExternalContext#getRequestHeaderMap()
	 */
	public static Map<String, String> getRequestHeaderMap() {
		return FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap();
	}

	/**
	 * Returns the HTTP request header value associated with the given name.
	 * @param name The HTTP request header name.
	 * @return The HTTP request header value associated with the given name.
	 * @see ExternalContext#getRequestHeaderMap()
	 */
	public static String getRequestHeader(String name) {
		return FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap().get(name);
	}

	/**
	 * Returns the HTTP request header values map.
	 * @return The HTTP request header values map.
	 * @see ExternalContext#getRequestHeaderValuesMap()
	 */
	public static Map<String, String[]> getRequestHeaderValuesMap() {
		return FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderValuesMap();
	}

	/**
	 * Returns the HTTP request header values associated with the given name.
	 * @param name The HTTP request header name.
	 * @return The HTTP request header values associated with the given name.
	 * @see ExternalContext#getRequestHeaderValuesMap()
	 */
	public static String[] getRequestHeaderValues(String name) {
		return FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderValuesMap().get(name);
	}

	/**
	 * Returns the HTTP request context path. It's the webapp context name, with a leading slash. If the webapp runs
	 * on context root, then it returns an empty string.
	 * @return The HTTP request context path.
	 * @see ExternalContext#getRequestContextPath()
	 */
	public static String getRequestContextPath() {
		return FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
	}

	/**
	 * Returns the HTTP request servlet path. If JSF is prefix mapped (e.g. <tt>/faces/*</tt>), then this returns the
	 * whole prefix mapping (e.g. <tt>/faces</tt>). If JSF is suffix mapped (e.g. <tt>*.xhtml</tt>), then this returns
	 * the whole part after the context path.
	 * @return The HTTP request servlet path.
	 * @see ExternalContext#getRequestServletPath()
	 */
	public static String getRequestServletPath() {
		return FacesContext.getCurrentInstance().getExternalContext().getRequestServletPath();
	}

	/**
	 * Returns the HTTP request path info. If JSF is prefix mapped (e.g. <tt>/faces/*</tt>), then this returns the
	 * whole part after the prefix mapping. If JSF is suffix mapped (e.g. <tt>*.xhtml</tt>), then this returns an empty
	 * string.
	 * @return The HTTP request path info.
	 * @see ExternalContext#getRequestPathInfo()
	 */
	public static String getRequestPathInfo() {
		return FacesContext.getCurrentInstance().getExternalContext().getRequestPathInfo();
	}

	/**
	 * Returns the HTTP request base URL, this is the full URL from the scheme, domain until with context path,
	 * including the trailing slash. This is the value you could use in HTML <code>&lt;base&gt;</code> tag.
	 * @return The HTTP request base URL.
	 * @see HttpServletRequest#getRequestURL()
	 * @see HttpServletRequest#getRequestURI()
	 * @see HttpServletRequest#getContextPath()
	 */
	public static String getRequestBaseURL() {
		HttpServletRequest request = getRequest();
		String url = request.getRequestURL().toString();
		return url.substring(0, url.length() - request.getRequestURI().length()) + request.getContextPath() + "/";
	}

	/**
	 * Returns the HTTP servlet response.
	 * @return The HTTP servlet response.
	 * @see ExternalContext#getResponse()
	 */
	public static HttpServletResponse getResponse() {
		return (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
	}

	/**
	 * Sends a temporary (302) redirect to the given URL. If the given URL does not start with <tt>http://</tt>,
	 * <tt>https://</tt> or <tt>/</tt>, then the request context path will be prepended, otherwise it will be the
	 * unmodified redirect URL.
	 * @param url The URL to redirect the current response to.
	 * @throws IOException Whenever something fails at I/O level. The caller should preferably not catch it, but just
	 * redeclare it in the action method. The servletcontainer will handle it.
	 * @see ExternalContext#redirect(String)
	 */
	public static void redirect(String url) throws IOException {
		FacesContext.getCurrentInstance().getExternalContext().redirect(normalizeRedirectURL(url));
	}

	/**
	 * Sends a permanent (301) redirect to the given URL. If the given URL does not start with <tt>http://</tt>,
	 * <tt>https://</tt> or <tt>/</tt>, then the request context path will be prepended, otherwise it will be the
	 * unmodified redirect URL.
	 * @param url The URL to redirect the current response to.
	 * @throws IOException Whenever something fails at I/O level. The caller should preferably not catch it, but just
	 * redeclare it in the action method. The servletcontainer will handle it.
	 * @see ExternalContext#setResponseStatus(int)
	 * @see ExternalContext#setResponseHeader(String, String)
	 */
	public static void redirectPermanent(String url) throws IOException {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		ExternalContext externalContext = facesContext.getExternalContext();
		externalContext.setResponseStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
		externalContext.setResponseHeader("Location", normalizeRedirectURL(url));
		facesContext.responseComplete();
	}

	/**
	 * Helper method to normalize the given URL for a redirect. If the given URL does not start with <tt>http://</tt>,
	 * <tt>https://</tt> or <tt>/</tt>, then the request context path will be prepended, otherwise it will be
	 * unmodified.
	 */
	private static String normalizeRedirectURL(String url) {
		if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("/")) {
			url = getRequestContextPath() + "/" + url;
		}

		return url;
	}

	/**
	 * Sends a HTTP response error with the given status and message. This will end up in either a custom
	 * <code>&lt;error-page&gt;</code> whose <code>&lt;error-code&gt;</code> matches the given status, or in a servlet
	 * container specific default error page if there is none. The message will be available in the error page as a
	 * request attribute with name <tt>javax.servlet.error.message</tt>. The {@link FacesContext#responseComplete()}
	 * will implicitly be called after sending the error.
	 * @param status The HTTP response status which is supposed to be in the range 4nn-5nn. You can use the constant
	 * field values of {@link HttpServletResponse} for this.
	 * @param message The message which is supposed to be available in the error page.
	 * @throws IOException Whenever something fails at I/O level. The caller should preferably not catch it, but just
	 * redeclare it in the action method. The servletcontainer will handle it.
	 * @see ExternalContext#responseSendError(int, String)
	 */
	public static void responseSendError(int status, String message) throws IOException {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		facesContext.getExternalContext().responseSendError(status, message);
		facesContext.responseComplete();
	}

	/**
	 * Add a header with given name and value to the HTTP response.
	 * @param name The header name.
	 * @param value The header value.
	 * @see ExternalContext#addResponseHeader(String, String)
	 */
	public static void addResponseHeader(String name, String value) {
		FacesContext.getCurrentInstance().getExternalContext().addResponseHeader(name, value);
	}

	// HTTP cookies ---------------------------------------------------------------------------------------------------

	/**
	 * Returns the value of the HTTP request cookie associated with the given name. The value is implicitly URL-decoded
	 * with a charset of UTF-8.
	 * @param name The HTTP request cookie name.
	 * @return The value of the HTTP request cookie associated with the given name.
	 * @throws UnsupportedOperationException If UTF-8 is not supported on this machine.
	 * @see ExternalContext#getRequestCookieMap()
	 */
	public static String getRequestCookie(String name) {
		Cookie cookie = (Cookie) FacesContext.getCurrentInstance().getExternalContext().getRequestCookieMap().get(name);

		try {
			return (cookie != null) ? URLDecoder.decode(cookie.getValue(), "UTF-8") : null;
		}
		catch (UnsupportedEncodingException e) {
			throw new UnsupportedOperationException(ERROR_UNSUPPORTED_ENCODING, e);
		}
	}

	/**
	 * Add a cookie with given name, value, path and maxage to the HTTP response. The cookie value will implicitly be
	 * URL-encoded with UTF-8 so that any special characters can be stored in the cookie. The cookie will implicitly
	 * be set to secure when the current request is secure (i.e. when the current request is a HTTPS request).
	 * @param name The cookie name.
	 * @param value The cookie value.
	 * @param path The cookie path.
	 * @param maxAge The maximum age of the cookie, in seconds. If this is <tt>0</tt> then the cookie will be removed.
	 * Note that the name and path must be exactly the same as it was when the cookie was created. If this is
	 * <tt>-1</tt> then the cookie will become a session cookie and thus live as long as the established HTTP session.
	 * @throws UnsupportedOperationException If UTF-8 is not supported on this machine.
	 * @see ExternalContext#addResponseCookie(String, String, Map)
	 */
	public static void addResponseCookie(String name, String value, String path, int maxAge) {
		if (value != null) {
			try {
				value = URLEncoder.encode(value, "UTF-8");
			}
			catch (UnsupportedEncodingException e) {
				throw new UnsupportedOperationException(ERROR_UNSUPPORTED_ENCODING, e);
			}
		}

		ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("path", path);
		properties.put("maxAge", maxAge);
		properties.put("secure", ((HttpServletRequest) externalContext.getRequest()).isSecure());
		externalContext.addResponseCookie(name, value, properties);
	}

	/**
	 * Remove the cookie with given name and path from the HTTP response. Note that the name and path must be exactly
	 * the same as it was when the cookie was created.
	 * @param name The cookie name.
	 * @param path The cookie path.
	 * @see ExternalContext#addResponseCookie(String, String, Map)
	 */
	public static void removeResponseCookie(String name, String path) {
		addResponseCookie(name, null, path, 0);
	}

	// HTTP session ---------------------------------------------------------------------------------------------------

	/**
	 * Returns the HTTP session and creates one if one doesn't exist.
	 * @return The HTTP session.
	 * @see ExternalContext#getSession(boolean)
	 */
	public static HttpSession getSession() {
		return (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(true);
	}

	/**
	 * Returns the HTTP session and creates one if one doesn't exist and <code>create</code> argument is
	 * <code>true</code>, otherwise don't create one and return <code>null</code>.
	 * @return The HTTP session.
	 * @see ExternalContext#getSession(boolean)
	 */
	public static HttpSession getSession(boolean create) {
		return (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(create);
	}

	/**
	 * Invalidates the current HTTP session. So, any subsequent HTTP request will get a new one when necessary.
	 * @see ExternalContext#invalidateSession()
	 */
	public static void invalidateSession() {
		FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
	}

	// Servlet context ------------------------------------------------------------------------------------------------

	/**
	 * Returns the servlet context.
	 * @return the servlet context.
	 * @see ExternalContext#getContext()
	 */
	public static ServletContext getServletContext() {
		return (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
	}

	/**
	 * Returns the mime type for the given file name. The mime type is determined based on file extension and
	 * configureable by <code>&lt;mime-mapping&gt;</code> entries in <tt>web.xml</tt>. When the mime type is unknown,
	 * then a default of <tt>application/octet-stream</tt> will be returned.
	 * @param name The file name to return the mime type for.
	 * @return The mime type for the given file name.
	 * @see ExternalContext#getMimeType(String)
	 */
	public static String getMimeType(String name) {
		String mimeType = FacesContext.getCurrentInstance().getExternalContext().getMimeType(name);

		if (mimeType == null) {
			mimeType = DEFAULT_MIME_TYPE;
		}

		return mimeType;
	}

	/**
	 * Returns an input stream for an application resource mapped to the specified path, if it exists; otherwise,
	 * return <code>null</code>.
	 * @param path The application resource path to return an input stream for.
	 * @return An input stream for an application resource mapped to the specified path.
	 * @see ExternalContext#getResourceAsStream(String)
	 */
	public static InputStream getResourceAsStream(String path) {
		return FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream(path);
	}

	/**
	 * Returns a set of available application resource paths matching the specified path.
	 * @param path The partial application resource path used to return matching resource paths.
	 * @return A set of available application resource paths matching the specified path.
	 * @see ServletContext#getResourcePaths(String)
	 */
	public static Set<String> getResourcePaths(String path) {
		return getServletContext().getResourcePaths(path);
	}

	// Request scope --------------------------------------------------------------------------------------------------

	/**
	 * Returns the request scope map.
	 * @return The request scope map.
	 * @see ExternalContext#getRequestMap()
	 */
	public static Map<String, Object> getRequestMap() {
		return FacesContext.getCurrentInstance().getExternalContext().getRequestMap();
	}

	/**
	 * Returns the request scope attribute value associated with the given name.
	 * @param name The request scope attribute name.
	 * @return The request scope attribute value associated with the given name.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see ExternalContext#getRequestMap()
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getRequestAttribute(String name) {
		return (T) FacesContext.getCurrentInstance().getExternalContext().getRequestMap().get(name);
	}

	/**
	 * Sets the request scope attribute value associated with the given name.
	 * @param name The request scope attribute name.
	 * @param value The request scope attribute value.
	 * @see ExternalContext#getRequestMap()
	 */
	public static void setRequestAttribute(String name, Object value) {
		FacesContext.getCurrentInstance().getExternalContext().getRequestMap().put(name, value);
	}

	// Flash scope ----------------------------------------------------------------------------------------------------

	/**
	 * Returns the flash scope.
	 * @return The flash scope.
	 * @see ExternalContext#getFlash()
	 */
	public static Flash getFlash() {
		return FacesContext.getCurrentInstance().getExternalContext().getFlash();
	}

	/**
	 * Returns the flash scope attribute value associated with the given name.
	 * @param name The flash scope attribute name.
	 * @return The flash scope attribute value associated with the given name.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see ExternalContext#getFlash()
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getFlashAttribute(String name) {
		return (T) FacesContext.getCurrentInstance().getExternalContext().getFlash().get(name);
	}

	/**
	 * Sets the flash scope attribute value associated with the given name.
	 * @param name The flash scope attribute name.
	 * @param value The flash scope attribute value.
	 * @see ExternalContext#getFlash()
	 */
	public static void setFlashAttribute(String name, Object value) {
		FacesContext.getCurrentInstance().getExternalContext().getFlash().put(name, value);
	}

	// View scope -----------------------------------------------------------------------------------------------------

	/**
	 * Returns the view scope map.
	 * @return The view scope map.
	 * @see UIViewRoot#getViewMap()
	 */
	public static Map<String, Object> getViewMap() {
		return FacesContext.getCurrentInstance().getViewRoot().getViewMap();
	}

	/**
	 * Returns the view scope attribute value associated with the given name.
	 * @param name The view scope attribute name.
	 * @return The view scope attribute value associated with the given name.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see UIViewRoot#getViewMap()
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getViewAttribute(String name) {
		return (T) FacesContext.getCurrentInstance().getViewRoot().getViewMap().get(name);
	}

	/**
	 * Sets the view scope attribute value associated with the given name.
	 * @param name The view scope attribute name.
	 * @param value The view scope attribute value.
	 * @see UIViewRoot#getViewMap()
	 */
	public static void setViewAttribute(String name, Object value) {
		FacesContext.getCurrentInstance().getViewRoot().getViewMap().put(name, value);
	}

	// Session scope --------------------------------------------------------------------------------------------------

	/**
	 * Returns the session scope map.
	 * @return The session scope map.
	 * @see ExternalContext#getSessionMap()
	 */
	public static Map<String, Object> getSessionMap() {
		return FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
	}

	/**
	 * Returns the session scope attribute value associated with the given name.
	 * @param name The session scope attribute name.
	 * @return The session scope attribute value associated with the given name.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see ExternalContext#getSessionMap()
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getSessionAttribute(String name) {
		return (T) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get(name);
	}

	/**
	 * Sets the session scope attribute value associated with the given name.
	 * @param name The session scope attribute name.
	 * @param value The session scope attribute value.
	 * @see ExternalContext#getSessionMap()
	 */
	public static void setSessionAttribute(String name, Object value) {
		FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put(name, value);
	}

	// Application scope ----------------------------------------------------------------------------------------------

	/**
	 * Returns the application scope map.
	 * @return The application scope map.
	 * @see ExternalContext#getApplicationMap()
	 */
	public static Map<String, Object> getApplicationMap() {
		return FacesContext.getCurrentInstance().getExternalContext().getApplicationMap();
	}

	/**
	 * Returns the application scope attribute value associated with the given name.
	 * @param name The application scope attribute name.
	 * @return The application scope attribute value associated with the given name.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see ExternalContext#getApplicationMap()
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getApplicationAttribute(String name) {
		return (T) FacesContext.getCurrentInstance().getExternalContext().getApplicationMap().get(name);
	}

	/**
	 * Sets the application scope attribute value associated with the given name.
	 * @param name The application scope attribute name.
	 * @param value The application scope attribute value.
	 * @see ExternalContext#getApplicationMap()
	 */
	public static void setApplicationAttribute(String name, Object value) {
		FacesContext.getCurrentInstance().getExternalContext().getApplicationMap().put(name, value);
	}

	// File download --------------------------------------------------------------------------------------------------

	/**
	 * Send the given file to the response. The content type will be determined based on file name. The content length
	 * will be set to the length of the file. The {@link FacesContext#responseComplete()} will implicitly be called
	 * after successful streaming.
	 * @param file The file to be sent to the response.
	 * @param attachment Whether the file should be provided as attachment, or just inline.
	 * @throws IOException Whenever something fails at I/O level. The caller should preferably not catch it, but just
	 * redeclare it in the action method. The servletcontainer will handle it.
	 */
	public static void sendFile(File file, boolean attachment) throws IOException {
		sendFile(new FileInputStream(file), file.getName(), file.length(), attachment);
	}

	/**
	 * Send the given byte array as a file to the response. The content type will be determined based on file name. The
	 * content length will be set to the length of the byte array. The {@link FacesContext#responseComplete()} will
	 * implicitly be called after successful streaming.
	 * @param content The file content as byte array.
	 * @param filename The file name which should appear in content disposition header.
	 * @param attachment Whether the file should be provided as attachment, or just inline.
	 * @throws IOException Whenever something fails at I/O level. The caller should preferably not catch it, but just
	 * redeclare it in the action method. The servletcontainer will handle it.
	 */
	public static void sendFile(byte[] content, String filename, boolean attachment) throws IOException {
		sendFile(new ByteArrayInputStream(content), filename, content.length, attachment);
	}

	/**
	 * Send the given input stream as a file to the response. The content type will be determined based on file name.
	 * The content length may not be set because that's not predictable based on input stream. The client may receive a
	 * download of an unknown length and thus the download progress may be unknown to the client. Only if the input
	 * stream is smaller than the default buffer size, then the content length will be set. The
	 * {@link InputStream#close()} will implicitly be called after streaming, regardless of whether an exception is
	 * been thrown or not. The {@link FacesContext#responseComplete()} will implicitly be called after successful
	 * streaming.
	 * @param content The file content as input stream.
	 * @param filename The file name which should appear in content disposition header.
	 * @param attachment Whether the file should be provided as attachment, or just inline.
	 * @throws IOException Whenever something fails at I/O level. The caller should preferably not catch it, but just
	 * redeclare it in the action method. The servletcontainer will handle it.
	 */
	public static void sendFile(InputStream content, String filename, boolean attachment) throws IOException {
		sendFile(content, filename, -1, attachment);
	}

	/**
	 * Internal global method to send the given input stream to the response.
	 * @param input The file content as input stream.
	 * @param filename The file name which should appear in content disposition header.
	 * @param contentLength The content length, or -1 if it is unknown.
	 * @param attachment Whether the file should be provided as attachment, or just inline.
	 * @throws IOException Whenever something fails at I/O level. The caller should preferably not catch it, but just
	 * redeclare it in the action method. The servletcontainer will handle it.
	 */
	private static void sendFile(InputStream input, String filename, long contentLength, boolean attachment)
		throws IOException
	{
		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();

		// Prepare the response and set the necessary headers.
		externalContext.responseReset();
		externalContext.setResponseBufferSize(DEFAULT_SENDFILE_BUFFER_SIZE);
		externalContext.setResponseContentType(getMimeType(filename));
		externalContext.setResponseHeader("Content-Disposition", String.format("%s;filename=\"%s\"",
			(attachment ? "attachment" : "inline"), URLEncoder.encode(filename, "UTF-8")));

		// Not exactly mandatory, but this fixes at least a MSIE quirk: http://support.microsoft.com/kb/316431
		if (((HttpServletRequest) externalContext.getRequest()).isSecure()) {
			externalContext.setResponseHeader("Cache-Control", "public");
			externalContext.setResponseHeader("Pragma", "public");
		}

		// If content length is known, set it. Note that setResponseContentLength() cannot be used as it takes only int.
		if (contentLength != -1) {
			externalContext.setResponseHeader("Content-Length", String.valueOf(contentLength));
		}

		long size = Utils.stream(input, externalContext.getResponseOutputStream());

		// This may be on time for files smaller than the default buffer size, but is otherwise ignored anyway.
		if (contentLength == -1) {
			externalContext.setResponseHeader("Content-Length", String.valueOf(size));
		}

		context.responseComplete();
	}

}