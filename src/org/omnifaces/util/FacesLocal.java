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

import static javax.servlet.http.HttpServletResponse.SC_MOVED_PERMANENTLY;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.application.Application;
import javax.faces.application.ProjectStage;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewParameter;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.faces.event.PhaseId;
import javax.faces.view.ViewDeclarationLanguage;
import javax.faces.view.ViewMetadata;
import javax.faces.view.facelets.FaceletContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Collection of utility methods for the JSF API that are mainly shortcuts for obtaining stuff from the provided
 * {@link FacesContext} argument. In effect, it 'flattens' the hierarchy of nested objects.
 * <p>
 * The difference with {@link Faces} is that no one method of {@link FacesLocal} obtains the {@link FacesContext} from
 * the current thread by {@link FacesContext#getCurrentInstance()}. This job is up to the caller.
 * <p>
 * Note that methods which are <strong>directly</strong> available on {@link FacesContext} instance itself, such as
 * <code>getExternalContext()</code>, <code>getViewRoot()</code>, <code>isValidationFailed()</code>, etc are not
 * delegated by the current utility class, because it would design technically not make any sense to delegate a
 * single-depth method call like
 * <pre>ExternalContext externalContext = FacesLocal.getExternalContext(facesContext);</pre>
 * <p>instead of just calling it directly
 * <pre>ExternalContext externalContext = facesContext.getExternalContext();</pre>
 *
 * @author Arjan Tijms
 * @author Bauke Scholtz
 * @since 1.6
 */
public final class FacesLocal {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String DEFAULT_MIME_TYPE = "application/octet-stream";
	private static final int DEFAULT_SENDFILE_BUFFER_SIZE = 10240;
	private static final String ERROR_NO_VIEW = "There is no view.";

	// Constructors ---------------------------------------------------------------------------------------------------

	private FacesLocal() {
		// Hide constructor.
	}

	// JSF general ----------------------------------------------------------------------------------------------------

	/**
	 * @see Faces#getServerInfo()
	 */
	public static String getServerInfo(FacesContext context) {
		return getServletContext(context).getServerInfo();
	}

	/**
	 * @see Faces#isDevelopment()
	 */
	public static boolean isDevelopment(FacesContext context) {
		return context.getApplication().getProjectStage() == ProjectStage.Development;
	}

	/**
	 * @see Faces#getMapping()
	 */
	public static String getMapping(FacesContext context) {
		ExternalContext externalContext = context.getExternalContext();

		if (externalContext.getRequestPathInfo() == null) {
			String path = externalContext.getRequestServletPath();
			return path.substring(path.lastIndexOf('.'));
		}
		else {
			return externalContext.getRequestServletPath();
		}
	}

	/**
	 * @see Faces#isPrefixMapping()
	 */
	public static boolean isPrefixMapping(FacesContext context) {
		return Faces.isPrefixMapping(getMapping(context));
	}

	/**
	 * @see Faces#evaluateExpressionGet(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T evaluateExpressionGet(FacesContext context, String expression) {
		if (expression == null) {
			return null;
		}

		return (T) context.getApplication().evaluateExpressionGet(context, expression, Object.class);
	}

	/**
	 * @see Faces#evaluateExpressionSet(String, Object)
	 */
	public static void evaluateExpressionSet(FacesContext context, String expression, Object value) {
		ELContext elContext = context.getELContext();
		ValueExpression valueExpression = context.getApplication().getExpressionFactory()
		    .createValueExpression(elContext, expression, Object.class);
		valueExpression.setValue(elContext, value);
	}

	/**
	 * @see Faces#getContextAttribute(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getContextAttribute(FacesContext context, String name) {
		return (T) context.getAttributes().get(name);
	}

	/**
	 * @see Faces#setContextAttribute(String, Object)
	 */
	public static void setContextAttribute(FacesContext context, String name, Object value) {
		context.getAttributes().put(name, value);
	}

	// JSF views ------------------------------------------------------------------------------------------------------

	/**
	 * @see Faces#setViewRoot(String)
	 */
	public static void setViewRoot(FacesContext context, String viewId) {
		context.setViewRoot(context.getApplication().getViewHandler().createView(context, viewId));
	}

	/**
	 * @see Faces#getViewId()
	 */
	public static String getViewId(FacesContext context) {
		UIViewRoot viewRoot = context.getViewRoot();
		return (viewRoot != null) ? viewRoot.getViewId() : null;
	}

	/**
	 * @see Faces#normalizeViewId(String)
	 */
	public static String normalizeViewId(FacesContext context, String path) {
		String mapping = getMapping(context);

		if (Faces.isPrefixMapping(mapping)) {
			if (path.startsWith(mapping)) {
				return path.substring(mapping.length());
			}
		}
		else if (path.endsWith(mapping)) {
			return path.substring(0, path.lastIndexOf('.')) + Utils.coalesce(
				getInitParameter(context, ViewHandler.FACELETS_SUFFIX_PARAM_NAME), ViewHandler.DEFAULT_FACELETS_SUFFIX);
		}

		return path;
	}

	/**
	 * @see Faces#getViewParameters()
	 */
	public static Collection<UIViewParameter> getViewParameters(FacesContext context) {
		UIViewRoot viewRoot = context.getViewRoot();
		return (viewRoot != null) ? ViewMetadata.getViewParameters(viewRoot) : Collections.<UIViewParameter>emptyList();
	}

	/**
	 * @see Faces#getMetadataAttributes(String)
	 */
	public static Map<String, Object> getMetadataAttributes(FacesContext context, String viewId) {
		ViewHandler viewHandler = context.getApplication().getViewHandler();
		ViewDeclarationLanguage vdl = viewHandler.getViewDeclarationLanguage(context, viewId);
		ViewMetadata metadata = vdl.getViewMetadata(context, viewId);

		return (metadata != null)
			? metadata.createMetadataView(context).getAttributes()
			: Collections.<String, Object>emptyMap();
	}

	/**
	 * @see Faces#getMetadataAttribute(String, String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getMetadataAttribute(FacesContext context, String viewId, String name) {
		return (T) getMetadataAttributes(context, viewId).get(name);
	}

	/**
	 * @see Faces#getMetadataAttribute(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getMetadataAttribute(FacesContext context, String name) {
		return (T) context.getViewRoot().getAttributes().get(name);
	}

	/**
	 * @see Faces#getLocale()
	 */
	public static Locale getLocale(FacesContext context) {
		Locale locale = null;
		UIViewRoot viewRoot = context.getViewRoot();

		// Prefer the locale set in the view.
		if (viewRoot != null) {
			locale = viewRoot.getLocale();
		}

		// Then the client preferred locale.
		if (locale == null) {
			Locale clientLocale = context.getExternalContext().getRequestLocale();

			if (getSupportedLocales(context).contains(clientLocale)) {
				locale = clientLocale;
			}
		}

		// Then the JSF default locale.
		if (locale == null) {
			locale = context.getApplication().getDefaultLocale();
		}

		// Finally the system default locale.
		if (locale == null) {
			locale = Locale.getDefault();
		}

		return locale;
	}

	/**
	 * @see Faces#getDefaultLocale()
	 */
	public static Locale getDefaultLocale(FacesContext context) {
		return context.getApplication().getDefaultLocale();
	}

	/**
	 * @see Faces#getSupportedLocales()
	 */
	public static List<Locale> getSupportedLocales(FacesContext context) {
		Application application = context.getApplication();
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
	 * @see Faces#setLocale(Locale)
	 */
	public static void setLocale(FacesContext context, Locale locale) {
		UIViewRoot viewRoot = context.getViewRoot();

		if (viewRoot == null) {
			throw new IllegalStateException(ERROR_NO_VIEW);
		}

		viewRoot.setLocale(locale);
	}

	/**
	 * @see Faces#navigate(String)
	 */
	public static void navigate(FacesContext context, String outcome) {
		context.getApplication().getNavigationHandler().handleNavigation(context, null, outcome);
	}

	/**
	 * @see Faces#getBookmarkableURL(Map, boolean)
	 */
	public static String getBookmarkableURL
		(FacesContext context, Map<String, List<String>> params, boolean includeViewParams)
	{
		String viewId = getViewId(context);

		if (viewId == null) {
			throw new IllegalStateException(ERROR_NO_VIEW);
		}

		return getBookmarkableURL(context, viewId, params, includeViewParams);
	}

	/**
	 * @see Faces#getBookmarkableURL(String, Map, boolean)
	 */
	public static String getBookmarkableURL
		(FacesContext context, String viewId, Map<String, List<String>> params, boolean includeViewParams)
	{
		return context.getApplication().getViewHandler().getBookmarkableURL(context, viewId, params, includeViewParams);
	}

	// Facelets -------------------------------------------------------------------------------------------------------

	/**
	 * @see Faces#getFaceletContext()
	 */
	public static FaceletContext getFaceletContext(FacesContext context) {
	    return (FaceletContext) context.getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);
	}

	/**
	 * @see Faces#getFaceletAttribute(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getFaceletAttribute(FacesContext context, String name) {
		return (T) getFaceletContext(context).getAttribute(name);
	}

	/**
	 * @see Faces#setFaceletAttribute(String, Object)
	 */
	public static void setFaceletAttribute(FacesContext context, String name, Object value) {
		getFaceletContext(context).setAttribute(name, value);
	}

	// HTTP request ---------------------------------------------------------------------------------------------------

	/**
	 * @see Faces#getRequest()
	 */
	public static HttpServletRequest getRequest(FacesContext context) {
		return (HttpServletRequest) context.getExternalContext().getRequest();
	}

	/**
	 * @see Faces#isAjaxRequest()
	 */
	public static boolean isAjaxRequest(FacesContext context) {
		return context.getPartialViewContext().isAjaxRequest();
	}

	/**
	 * @see Faces#getRequestParameterMap()
	 */
	public static Map<String, String> getRequestParameterMap(FacesContext context) {
		return context.getExternalContext().getRequestParameterMap();
	}

	/**
	 * @see Faces#getRequestParameter(String)
	 */
	public static String getRequestParameter(FacesContext context, String name) {
		return getRequestParameterMap(context).get(name);
	}

	/**
	 * @see Faces#getRequestParameterValuesMap()
	 */
	public static Map<String, String[]> getRequestParameterValuesMap(FacesContext context) {
		return context.getExternalContext().getRequestParameterValuesMap();
	}

	/**
	 * @see Faces#getRequestParameterValues(String)
	 */
	public static String[] getRequestParameterValues(FacesContext context, String name) {
		return getRequestParameterValuesMap(context).get(name);
	}

	/**
	 * @see Faces#getRequestHeaderMap()
	 */
	public static Map<String, String> getRequestHeaderMap(FacesContext context) {
		return context.getExternalContext().getRequestHeaderMap();
	}

	/**
	 * @see Faces#getRequestHeader(String)
	 */
	public static String getRequestHeader(FacesContext context, String name) {
		return getRequestHeaderMap(context).get(name);
	}

	/**
	 * @see Faces#getRequestHeaderValuesMap()
	 */
	public static Map<String, String[]> getRequestHeaderValuesMap(FacesContext context) {
		return context.getExternalContext().getRequestHeaderValuesMap();
	}

	/**
	 * @see Faces#getRequestHeaderValues(String)
	 */
	public static String[] getRequestHeaderValues(FacesContext context, String name) {
		return getRequestHeaderValuesMap(context).get(name);
	}

	/**
	 * @see Faces#getRequestContextPath()
	 */
	public static String getRequestContextPath(FacesContext context) {
		return context.getExternalContext().getRequestContextPath();
	}

	/**
	 * @see Faces#getRequestServletPath()
	 */
	public static String getRequestServletPath(FacesContext context) {
		return context.getExternalContext().getRequestServletPath();
	}

	/**
	 * @see Faces#getRequestPathInfo()
	 */
	public static String getRequestPathInfo(FacesContext context) {
		return context.getExternalContext().getRequestPathInfo();
	}

	/**
	 * @see Faces#getRequestHostname()
	 */
	public static String getRequestHostname(FacesContext context) {
		return Servlets.getRequestHostname(getRequest(context));
	}

	/**
	 * @see Faces#getRequestBaseURL()
	 */
	public static String getRequestBaseURL(FacesContext context) {
		return Servlets.getRequestBaseURL(getRequest(context));
	}

	/**
	 * @see Faces#getRequestDomainURL()
	 */
	public static String getRequestDomainURL(FacesContext context) {
		return Servlets.getRequestDomainURL(getRequest(context));
	}

	/**
	 * @see Faces#getRequestURL()
	 */
	public static String getRequestURL(FacesContext context) {
		return getRequest(context).getRequestURL().toString();
	}

	/**
	 * @see Faces#getRequestURI()
	 */
	public static String getRequestURI(FacesContext context) {
		return getRequest(context).getRequestURI();
	}

	/**
	 * @see Faces#getRequestQueryString()
	 */
	public static String getRequestQueryString(FacesContext context) {
		return getRequest(context).getQueryString();
	}

	/**
	 * @see Faces#getRequestURLWithQueryString()
	 */
	public static String getRequestURLWithQueryString(FacesContext context) {
		return Servlets.getRequestURLWithQueryString(getRequest(context));
	}

	/**
	 * @see Faces#getRemoteAddr()
	 */
	public static String getRemoteAddr(FacesContext context) {
		String forwardedFor = getRequestHeader(context, "X-Forwarded-For");

		if (!Utils.isEmpty(forwardedFor)) {
			return forwardedFor.split("\\s*,\\s*", 2)[0]; // It's a comma separated string: client,proxy1,proxy2,...
		}

		return getRequest(context).getRemoteAddr();
	}

	// HTTP response --------------------------------------------------------------------------------------------------

	/**
	 * @see Faces#getResponse()
	 */
	public static HttpServletResponse getResponse(FacesContext context) {
		return (HttpServletResponse) context.getExternalContext().getResponse();
	}

	/**
	 * @see Faces#getResponseBufferSize()
	 */
	public static int getResponseBufferSize(FacesContext context) {
		return context.getExternalContext().getResponseBufferSize();
	}

	/**
	 * @see Faces#getResponseCharacterEncoding()
	 */
	public static String getResponseCharacterEncoding(FacesContext context) {
		return context.getExternalContext().getResponseCharacterEncoding();
	}

	/**
	 * @see Faces#setResponseStatus(FacesContext, int)
	 */
	public static void setResponseStatus(FacesContext context, int status) {
		context.getExternalContext().setResponseStatus(status);
	}

	/**
	 * @see Faces#redirect(String, String...)
	 */
	public static void redirect(FacesContext context, String url, String... paramValues) throws IOException {
		String normalizedURL = normalizeRedirectURL(context, url);
		Object[] params = encodeURLParams(paramValues);

		ExternalContext externalContext = context.getExternalContext();
		externalContext.getFlash().setRedirect(true);
		externalContext.redirect(String.format(normalizedURL, params));
	}

	/**
	 * @see Faces#redirectPermanent(String, String...)
	 */
	public static void redirectPermanent(FacesContext context, String url, String... paramValues) {
		String normalizedURL = normalizeRedirectURL(context, url);
		Object[] params = encodeURLParams(paramValues);

		ExternalContext externalContext = context.getExternalContext();
		externalContext.getFlash().setRedirect(true);
		externalContext.setResponseStatus(SC_MOVED_PERMANENTLY);
		externalContext.setResponseHeader("Location", String.format(normalizedURL, params));
		externalContext.setResponseHeader("Connection", "close");
		context.responseComplete();
	}

	/**
	 * Helper method to normalize the given URL for a redirect. If the given URL does not start with
	 * <code>http://</code>, <code>https://</code> or <code>/</code>, then the request context path will be prepended,
	 * otherwise it will be unmodified.
	 */
	private static String normalizeRedirectURL(FacesContext context, String url) {
		if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("/")) {
			url = getRequestContextPath(context) + "/" + url;
		}

		return url;
	}

	/**
	 * Helper method to encode the given URL parameters using UTF-8.
	 */
	private static Object[] encodeURLParams(String... params) {
		if (params == null) {
			return new Object[0];
		}
		else {
			Object[] encodedParams = new Object[params.length];

			for (int i = 0; i < params.length; i++) {
				encodedParams[i] = Utils.encodeURL(params[i]);
			}

			return encodedParams;
		}
	}

	/**
	 * @see Faces#responseSendError(int, String)
	 */
	public static void responseSendError(FacesContext context, int status, String message) throws IOException {
		context.getExternalContext().responseSendError(status, message);
		context.responseComplete();
	}

	/**
	 * @see Faces#addResponseHeader(String, String)
	 */
	public static void addResponseHeader(FacesContext context, String name, String value) {
		context.getExternalContext().addResponseHeader(name, value);
	}

	/**
	 * @see Faces#isResponseCommitted()
	 */
	public static boolean isResponseCommitted(FacesContext context) {
		return context.getExternalContext().isResponseCommitted();
	}

	/**
	 * @see Faces#responseReset()
	 */
	public static void responseReset(FacesContext context) {
		context.getExternalContext().responseReset();
	}

	/**
	 * @see Faces#isRenderResponse()
	 */
	public static boolean isRenderResponse(FacesContext context) {
		return context.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE;
	}

	// FORM based authentication --------------------------------------------------------------------------------------

	/**
	 * @see Faces#login(String, String)
	 */
	public static void login(FacesContext context, String username, String password) throws ServletException {
		getRequest(context).login(username, password);
	}

	/**
	 * @see Faces#authenticate()
	 */
	public static boolean authenticate(FacesContext context) throws ServletException, IOException {
		return getRequest(context).authenticate(getResponse(context));
	}

	/**
	 * @see Faces#logout()
	 */
	public static void logout(FacesContext context) throws ServletException {
		getRequest(context).logout();
	}

	/**
	 * @see Faces#getRemoteUser()
	 */
	public static String getRemoteUser(FacesContext context) {
		return context.getExternalContext().getRemoteUser();
	}

	/**
	 * @see Faces#isUserInRole(String)
	 */
	public static boolean isUserInRole(FacesContext context, String role) {
		return context.getExternalContext().isUserInRole(role);
	}

	// HTTP cookies ---------------------------------------------------------------------------------------------------

	/**
	 * @see Faces#getRequestCookie(String)
	 */
	public static String getRequestCookie(FacesContext context, String name) {
		Cookie cookie = (Cookie) context.getExternalContext().getRequestCookieMap().get(name);
		return (cookie != null) ? Utils.decodeURL(cookie.getValue()) : null;
	}

	/**
	 * @see Faces#addResponseCookie(String, String, String, int)
	 */
	public static void addResponseCookie(FacesContext context, String name, String value, String path, int maxAge) {
		if (value != null) {
			value = Utils.encodeURL(value);
		}

		ExternalContext externalContext = context.getExternalContext();
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("path", path);
		properties.put("maxAge", maxAge);
		properties.put("secure", ((HttpServletRequest) externalContext.getRequest()).isSecure());
		externalContext.addResponseCookie(name, value, properties);
	}

	/**
	 * @see Faces#removeResponseCookie(String, String)
	 */
	public static void removeResponseCookie(FacesContext context, String name, String path) {
		addResponseCookie(context, name, null, path, 0);
	}

	// HTTP session ---------------------------------------------------------------------------------------------------

	/**
	 * @see Faces#getSession()
	 */
	public static HttpSession getSession(FacesContext context) {
		return getSession(context, true);
	}

	/**
	 * @see Faces#getSession(boolean)
	 */
	public static HttpSession getSession(FacesContext context, boolean create) {
		return (HttpSession) context.getExternalContext().getSession(create);
	}

	/**
	 * @see Faces#getSessionId()
	 */
	public static String getSessionId(FacesContext context) {
		HttpSession session = getSession(context, false);
		return (session != null) ? session.getId() : null;
	}

	/**
	 * @see Faces#invalidateSession()
	 */
	public static void invalidateSession(FacesContext context) {
		context.getExternalContext().invalidateSession();
	}

	/**
	 * @see Faces#hasSession()
	 */
	public static boolean hasSession(FacesContext context) {
		return getSession(context, false) != null;
	}

	/**
	 * @see Faces#isSessionNew()
	 */
	public static boolean isSessionNew(FacesContext context) {
		HttpSession session = getSession(context, false);
		return (session != null && session.isNew());
	}

	/**
	 * @see Faces#getSessionCreationTime()
	 */
	public static long getSessionCreationTime(FacesContext context) {
		return getSession(context).getCreationTime();
	}

	/**
	 * @see Faces#getSessionLastAccessedTime()
	 */
	public static long getSessionLastAccessedTime(FacesContext context) {
		return getSession(context).getLastAccessedTime();
	}

	/**
	 * @see Faces#getSessionMaxInactiveInterval()
	 */
	public static int getSessionMaxInactiveInterval(FacesContext context) {
		// Note that JSF 2.1 has this method on ExternalContext. We don't use it in order to be JSF 2.0 compatible.
		return getSession(context).getMaxInactiveInterval();
	}

	/**
	 * @see Faces#setSessionMaxInactiveInterval(int)
	 */
	public static void setSessionMaxInactiveInterval(FacesContext context, int seconds) {
		// Note that JSF 2.1 has this method on ExternalContext. We don't use it in order to be JSF 2.0 compatible.
		getSession(context).setMaxInactiveInterval(seconds);
	}

	/**
	 * @see Faces#hasSessionTimedOut()
	 */
	public static boolean hasSessionTimedOut(FacesContext context) {
		HttpServletRequest request = getRequest(context);
		return request.getRequestedSessionId() != null && !request.isRequestedSessionIdValid();
	}

	// Servlet context ------------------------------------------------------------------------------------------------

	/**
	 * @see Faces#getServletContext()
	 */
	public static ServletContext getServletContext(FacesContext context) {
		return (ServletContext) context.getExternalContext().getContext();
	}

	/**
	 * @see Faces#getInitParameterMap()
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String> getInitParameterMap(FacesContext context) {
		return context.getExternalContext().getInitParameterMap();
	}

	/**
	 * @see Faces#getInitParameter(String)
	 */
	public static String getInitParameter(FacesContext context, String name) {
		return context.getExternalContext().getInitParameter(name);
	}

	/**
	 * @see Faces#getMimeType(String)
	 */
	public static String getMimeType(FacesContext context, String name) {
		String mimeType = context.getExternalContext().getMimeType(name);

		if (mimeType == null) {
			mimeType = DEFAULT_MIME_TYPE;
		}

		return mimeType;
	}

	/**
	 * @see Faces#getResource(String)
	 */
	public static URL getResource(FacesContext context, String path) throws MalformedURLException {
		return context.getExternalContext().getResource(path);
	}

	/**
	 * @see Faces#getResourceAsStream(String)
	 */
	public static InputStream getResourceAsStream(FacesContext context, String path) {
		return context.getExternalContext().getResourceAsStream(path);
	}

	/**
	 * @see Faces#getResourcePaths(String)
	 */
	public static Set<String> getResourcePaths(FacesContext context, String path) {
		return context.getExternalContext().getResourcePaths(path);
	}

	/**
	 * @see Faces#getRealPath(String)
	 */
	public static String getRealPath(FacesContext context, String webContentPath) {
		return context.getExternalContext().getRealPath(webContentPath);
	}

	// Request scope --------------------------------------------------------------------------------------------------

	/**
	 * @see Faces#getRequestMap()
	 */
	public static Map<String, Object> getRequestMap(FacesContext context) {
		return context.getExternalContext().getRequestMap();
	}

	/**
	 * @see Faces#getRequestAttribute(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getRequestAttribute(FacesContext context, String name) {
		return (T) getRequestMap(context).get(name);
	}

	/**
	 * @see Faces#setRequestAttribute(String, Object)
	 */
	public static void setRequestAttribute(FacesContext context, String name, Object value) {
		getRequestMap(context).put(name, value);
	}

	/**
	 * @see Faces#removeRequestAttribute(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T removeRequestAttribute(FacesContext context, String name) {
		return (T) getRequestMap(context).remove(name);
	}

	// Flash scope ----------------------------------------------------------------------------------------------------

	/**
	 * @see Faces#getFlash()
	 */
	public static Flash getFlash(FacesContext context) {
		return context.getExternalContext().getFlash();
	}

	/**
	 * @see Faces#getFlashAttribute(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getFlashAttribute(FacesContext context, String name) {
		return (T) getFlash(context).get(name);
	}

	/**
	 * @see Faces#setFlashAttribute(String, Object)
	 */
	public static void setFlashAttribute(FacesContext context, String name, Object value) {
		getFlash(context).put(name, value);
	}

	/**
	 * @see Faces#removeFlashAttribute(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T removeFlashAttribute(FacesContext context, String name) {
		return (T) getFlash(context).remove(name);
	}

	// View scope -----------------------------------------------------------------------------------------------------

	/**
	 * @see Faces#getViewMap()
	 */
	public static Map<String, Object> getViewMap(FacesContext context) {
		return context.getViewRoot().getViewMap();
	}

	/**
	 * @see Faces#getViewAttribute(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getViewAttribute(FacesContext context, String name) {
		return (T) getViewMap(context).get(name);
	}

	/**
	 * @see Faces#setViewAttribute(String, Object)
	 */
	public static void setViewAttribute(FacesContext context, String name, Object value) {
		getViewMap(context).put(name, value);
	}

	/**
	 * @see Faces#removeViewAttribute(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T removeViewAttribute(FacesContext context, String name) {
		return (T) getViewMap(context).remove(name);
	}

	// Session scope --------------------------------------------------------------------------------------------------

	/**
	 * @see Faces#getSessionMap()
	 */
	public static Map<String, Object> getSessionMap(FacesContext context) {
		return context.getExternalContext().getSessionMap();
	}

	/**
	 * @see Faces#getSessionAttribute(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getSessionAttribute(FacesContext context, String name) {
		return (T) getSessionMap(context).get(name);
	}

	/**
	 * @see Faces#setSessionAttribute(String, Object)
	 */
	public static void setSessionAttribute(FacesContext context, String name, Object value) {
		getSessionMap(context).put(name, value);
	}

	/**
	 * @see Faces#removeSessionAttribute(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T removeSessionAttribute(FacesContext context, String name) {
		return (T) getSessionMap(context).remove(name);
	}

	// Application scope ----------------------------------------------------------------------------------------------

	/**
	 * @see Faces#getApplicationMap()
	 */
	public static Map<String, Object> getApplicationMap(FacesContext context) {
		return context.getExternalContext().getApplicationMap();
	}

	/**
	 * @see Faces#getApplicationAttribute(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getApplicationAttribute(FacesContext context, String name) {
		return (T) getApplicationMap(context).get(name);
	}

	/**
	 * @see Faces#setApplicationAttribute(String, Object)
	 */
	public static void setApplicationAttribute(FacesContext context, String name, Object value) {
		getApplicationMap(context).put(name, value);
	}

	/**
	 * @see Faces#removeApplicationAttribute(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T removeApplicationAttribute(FacesContext context, String name) {
		return (T) getApplicationMap(context).remove(name);
	}

	// File download --------------------------------------------------------------------------------------------------

	/**
	 * @see Faces#sendFile(File, boolean)
	 */
	public static void sendFile(FacesContext context, File file, boolean attachment) throws IOException {
		sendFile(context, new FileInputStream(file), file.getName(), file.length(), attachment);
	}

	/**
	 * @see Faces#sendFile(byte[], String, boolean)
	 */
	public static void sendFile(FacesContext context, byte[] content, String filename, boolean attachment)
		throws IOException
	{
		sendFile(context, new ByteArrayInputStream(content), filename, content.length, attachment);
	}

	/**
	 * @see Faces#sendFile(InputStream, String, boolean)
	 */
	public static void sendFile(FacesContext context, InputStream content, String filename, boolean attachment)
		throws IOException
	{
		sendFile(context, content, filename, -1, attachment);
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
	private static void sendFile
		(FacesContext context, InputStream input, String filename, long contentLength, boolean attachment)
			throws IOException
	{
		ExternalContext externalContext = context.getExternalContext();

		// Prepare the response and set the necessary headers.
		externalContext.setResponseBufferSize(DEFAULT_SENDFILE_BUFFER_SIZE);
		externalContext.setResponseContentType(getMimeType(context, filename));
		externalContext.setResponseHeader("Content-Disposition", String.format("%s;filename=\"%s\"",
			(attachment ? "attachment" : "inline"), Utils.encodeURL(filename)));

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