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

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.application.Application;
import javax.faces.application.NavigationHandler;
import javax.faces.application.ProjectStage;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewParameter;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.faces.context.PartialViewContext;
import javax.faces.event.PhaseId;
import javax.faces.view.ViewMetadata;
import javax.faces.view.facelets.FaceletContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
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
 * In addition, note that there's normally a minor overhead in obtaining the thread local {@link FacesContext}. In case
 * client code needs to call methods in this class multiple times it's expected that performance will be slightly better
 * if instead the {@link FacesContext} is obtained once and the required methods are called on that, although the
 * difference is practically negligible when used in modern server hardware.
 *
 * @author Arjan Tijms
 * @author Bauke Scholtz
 */
public final class Faces {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String DEFAULT_MIME_TYPE = "application/octet-stream";
	private static final int DEFAULT_SENDFILE_BUFFER_SIZE = 10240;

	private static final String ERROR_UNSUPPORTED_ENCODING = "UTF-8 is apparently not supported on this machine.";
	private static final String ERROR_NO_VIEW = "There is no UIViewRoot.";

	// Constructors ---------------------------------------------------------------------------------------------------

	private Faces() {
		// Hide constructor.
	}

	// JSF general ----------------------------------------------------------------------------------------------------

	/**
	 * Returns the current faces context.
	 * <p>
	 * <i>Note that whenever you absolutely need this method to perform a general task, you might want to consider to
	 * submit a feature request to OmniFaces in order to add a new utility method which performs exactly this general
	 * task.</i>
	 * @return The current faces context.
	 * @see FacesContext#getCurrentInstance()
	 */
	public static FacesContext getContext() {
		return FacesContext.getCurrentInstance();
	}

	/**
	 * Returns the faces context that's stored in an ELContext.
	 * <p>
	 * Note that this only works for an ELContext that is created in the context of JSF.
	 *
	 * @param elContext the EL context to obtain the faces context from.
	 * @return the faces context that's stored in the given ELContext.
	 * @since 1.2
	 */
	public static FacesContext getContext(ELContext elContext) {
		return (FacesContext) elContext.getContext(FacesContext.class);
	}

	/**
	 * Returns the current external context.
	 * <p>
	 * <i>Note that whenever you absolutely need this method to perform a general task, you might want to consider to
	 * submit a feature request to OmniFaces in order to add a new utility method which performs exactly this general
	 * task.</i>
	 * @return The current external context.
	 * @see FacesContext#getExternalContext()
	 */
	public static ExternalContext getExternalContext() {
		return getContext().getExternalContext();
	}

	/**
	 * Returns the current partial view context (the ajax context).
	 * <p>
	 * <i>Note that whenever you absolutely need this method to perform a general task, you might want to consider to
	 * submit a feature request to OmniFaces in order to add a new utility method which performs exactly this general
	 * task.</i>
	 * @return The current partial view context.
	 * @see FacesContext#getPartialViewContext()
	 */
	public static PartialViewContext getPartialViewContext() {
		return getContext().getPartialViewContext();
	}

	/**
	 * Returns the application singleton.
	 * <p>
	 * <i>Note that whenever you absolutely need this method to perform a general task, you might want to consider to
	 * submit a feature request to OmniFaces in order to add a new utility method which performs exactly this general
	 * task.</i>
	 * @return The faces application singleton.
	 * @see FacesContext#getApplication()
	 */
	public static Application getApplication() {
		return getContext().getApplication();
	}

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
	 * Returns whether we're in development stage. This will be the case when the <tt>javax.faces.PROJECT_STAGE</tt>
	 * context parameter in <tt>web.xml</tt> is set to <tt>Development</tt>.
	 * @return <code>true</code> if we're in development stage, otherwise <code>false</code>.
	 * @see Application#getProjectStage()
	 */
	public static boolean isDevelopment() {
		return getApplication().getProjectStage() == ProjectStage.Development;
	}

	/**
	 * Determines and returns the faces servlet mapping used in the current request. If JSF is prefix mapped (e.g.
	 * <tt>/faces/*</tt>), then this returns the whole path, with a leading slash (e.g. <tt>/faces</tt>). If JSF is
	 * suffix mapped (e.g. <tt>*.xhtml</tt>), then this returns the whole extension (e.g. <tt>.xhtml</tt>).
	 * @return The faces servlet mapping (without the wildcard).
	 * @see #getRequestPathInfo()
	 * @see #getRequestServletPath()
	 */
	public static String getMapping() {
		ExternalContext externalContext = getExternalContext();

		if (externalContext.getRequestPathInfo() == null) {
			String path = externalContext.getRequestServletPath();
			return path.substring(path.lastIndexOf('.'));
		}
		else {
			return externalContext.getRequestServletPath();
		}
	}

	/**
	 * Returns whether the faces servlet mapping used in the current request is a prefix mapping.
	 * @return <code>true</code> if the faces servlet mapping used in the current request is a prefix mapping, otherwise
	 * <code>false</code>.
	 * @see #getMapping()
	 * @see #isPrefixMapping(String)
	 */
	public static boolean isPrefixMapping() {
		return isPrefixMapping(getMapping());
	}

	/**
	 * Returns whether the faces servlet mapping used in the current request is a prefix mapping. Use this method in
	 * preference to {@link #isPrefixMapping()} when you already have obtained the mapping from {@link #getMapping()}
	 * so that the mapping won't be calculated twice.
	 * @param mapping The mapping to be tested.
	 * @return <code>true</code> if the faces servlet mapping used in the current request is a prefix mapping, otherwise
	 * <code>false</code>.
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
		return getContext().getCurrentPhaseId();
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

		FacesContext context = getContext();
		return (T) context.getApplication().evaluateExpressionGet(context, expression, Object.class);
	}

	/**
	 * Programmatically evaluate the given EL expression and set the given value.
	 * @param expression The EL expression to be evaluated.
	 * @param value The value to be set in the property behind the EL expression.
	 * @see Application#getExpressionFactory()
	 * @see ExpressionFactory#createValueExpression(ELContext, String, Class)
	 * @see ValueExpression#setValue(ELContext, Object)
	 * @since 1.1
	 */
	public static void evaluateExpressionSet(String expression, Object value) {
		FacesContext context = getContext();
		ELContext elContext = context.getELContext();
		ValueExpression valueExpression = context.getApplication().getExpressionFactory()
		    .createValueExpression(elContext, expression, Object.class);
		valueExpression.setValue(elContext, value);
	}

	// JSF views ------------------------------------------------------------------------------------------------------

	/**
	 * Returns the current view root.
	 * @return The current view root.
	 * @see FacesContext#getViewRoot()
	 */
	public static UIViewRoot getViewRoot() {
		return getContext().getViewRoot();
	}

	/**
	 * Sets the current view root to the given view ID. The view ID must start with a leading slash. If an invalid view
	 * ID is given, then the response will simply result in a 404.
	 * @param viewId The ID of the view which needs to be set as the current view root.
	 * @see ViewHandler#createView(FacesContext, String)
	 * @see FacesContext#setViewRoot(UIViewRoot)
	 * @since 1.1
	 */
	public static void setViewRoot(String viewId) {
		FacesContext context = getContext();
		context.setViewRoot(context.getApplication().getViewHandler().createView(context, viewId));
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
	 * @see #getMapping()
	 * @see #isPrefixMapping(String)
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
	 * Returns the view parameters of the current view, or an empty collection if there is no view.
	 * @return The view parameters of the current view, or an empty collection if there is no view.
	 * @see ViewMetadata#getViewParameters(UIViewRoot)
	 */
	public static Collection<UIViewParameter> getViewParameters() {
		UIViewRoot viewRoot = getViewRoot();
		return (viewRoot != null) ? ViewMetadata.getViewParameters(viewRoot) : Collections.<UIViewParameter>emptyList();
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
		FacesContext context = getContext();
		Locale locale = null;
		UIViewRoot viewRoot = context.getViewRoot();

		// Prefer the locale set in the view.
		if (viewRoot != null) {
			locale = viewRoot.getLocale();
		}

		// Then the client preferred locale.
		if (locale == null) {
			Locale clientLocale = context.getExternalContext().getRequestLocale();

			if (getSupportedLocales().contains(clientLocale)) {
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
	 * Returns the default locale, or <code>null</code> if there is none.
	 * @return The default locale, or <code>null</code> if there is none.
	 * @see Application#getDefaultLocale()
	 */
	public static Locale getDefaultLocale() {
		return getApplication().getDefaultLocale();
	}

	/**
	 * Returns a list of all supported locales on this application, with the default locale as the first item, if any.
	 * This will return an empty list if there are no locales definied in <tt>faces-config.xml</tt>.
	 * @return A list of all supported locales on this application, with the default locale as the first item, if any.
	 * @see Application#getDefaultLocale()
	 * @see Application#getSupportedLocales()
	 */
	public static List<Locale> getSupportedLocales() {
		Application application = getApplication();
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
	 * Set the locale of the current view, which is to be used in localizing of the response.
	 * @param locale The locale of the current view.
	 * @throws IllegalStateException When there is no view (i.e. when it is <code>null</code>). This can happen if the
	 * method is called at the wrong moment in the JSF lifecycle, e.g. before the view has been restored/created.
	 * @see UIViewRoot#setLocale(Locale)
	 * @since 1.2
	 */
	public static void setLocale(Locale locale) {
		UIViewRoot viewRoot = getViewRoot();

		if (viewRoot == null) {
			throw new IllegalStateException(ERROR_NO_VIEW);
		}

		viewRoot.setLocale(locale);
	}

	/**
	 * Perform the JSF navigation to the given outcome.
	 * @param outcome The navigation outcome.
	 * @see Application#getNavigationHandler()
	 * @see NavigationHandler#handleNavigation(FacesContext, String, String)
	 */
	public static void navigate(String outcome) {
		FacesContext context = getContext();
		context.getApplication().getNavigationHandler().handleNavigation(context, null, outcome);
	}

	/**
	 * Add the given client IDs to the collection of render IDs of the current partial view context. This will force
	 * JSF to ajax-render/update the components with the given client IDs on render response. Note that those client IDs
	 * should not start with the naming container separator character like <code>:</code>.
	 * @param clientIds The client IDs to be added to the collection of render IDs of the current partial view context.
	 * @since 1.1
	 */
	public static void addRenderIds(String... clientIds) {
		Collection<String> renderIds = getContext().getPartialViewContext().getRenderIds();

		for (String clientId : clientIds) {
			renderIds.add(clientId);
		}
	}

	// Facelets -------------------------------------------------------------------------------------------------------

	/**
	 * Returns the Facelet context.
	 * <p>
	 * <i>Note that whenever you absolutely need this method to perform a general task, you might want to consider to
	 * submit a feature request to OmniFaces in order to add a new utility method which performs exactly this general
	 * task.</i>
	 * @return The Facelet context.
	 * @see FaceletContext
	 * @since 1.1
	 */
	public static FaceletContext getFaceletContext() {
	    return (FaceletContext) getContext().getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);
	}

	/**
	 * Include the Facelet file at the given (relative) path as child of the given UI component. This has the same
	 * effect as using <code>&lt;ui:include&gt;</code>. The path is relative to the current view ID and absolute to the
	 * webcontent root.
	 * @param component The component to include the Facelet file in.
	 * @param path The (relative) path to the Facelet file.
	 * @throws IOException Whenever something fails at I/O level. The caller should preferably not catch it, but just
	 * redeclare it in the action method. The servletcontainer will handle it.
	 * @see FaceletContext#includeFacelet(UIComponent, String)
	 * @since 1.1
	 */
	public static void includeFacelet(UIComponent component, String path) throws IOException {
		getFaceletContext().includeFacelet(component, path);
	}

	/**
	 * Returns the Facelet attribute value associated with the given name. This basically returns the value of the
	 * <code>&lt;ui:param&gt;</code> which is been declared inside the Facelet file, or is been passed into the Facelet
	 * file by e.g. an <code>&lt;ui:include&gt;</code>.
	 * @param name The Facelet attribute name.
	 * @return The Facelet attribute value associated with the given name.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see FaceletContext#getAttribute(String)
	 * @since 1.1
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getFaceletAttribute(String name) {
		return (T) getFaceletContext().getAttribute(name);
	}

	/**
	 * Sets the Facelet attribute value associated with the given name. This basically does the same as an
	 * <code>&lt;ui:param&gt;</code> which is been declared inside the Facelet file, or is been passed into the Facelet
	 * file by e.g. an <code>&lt;ui:include&gt;</code>.
	 * @param name The Facelet attribute name.
	 * @param value The Facelet attribute value.
	 * @see FaceletContext#setAttribute(String, Object)
	 * @since 1.1
	 */
	public static void setFaceletAttribute(String name, Object value) {
		getFaceletContext().setAttribute(name, value);
	}

	// HTTP request ---------------------------------------------------------------------------------------------------

	/**
	 * Returns the HTTP servlet request.
	 * <p>
	 * <i>Note that whenever you absolutely need this method to perform a general task, you might want to consider to
	 * submit a feature request to OmniFaces in order to add a new utility method which performs exactly this general
	 * task.</i>
	 * @return The HTTP servlet request.
	 * @see ExternalContext#getRequest()
	 */
	public static HttpServletRequest getRequest() {
		return (HttpServletRequest) getExternalContext().getRequest();
	}

	/**
	 * Returns whether the current request is an ajax request.
	 * @return <code>true</code> for an ajax request, <code>false</code> for a non-ajax (synchronous) request.
	 * @see PartialViewContext#isAjaxRequest()
	 */
	public static boolean isAjaxRequest() {
		return getContext().getPartialViewContext().isAjaxRequest();
	}

	/**
	 * Returns whether the current request is a postback.
	 * @return <code>true</code> for a postback, <code>false</code> for a non-postback (GET) request.
	 * @see FacesContext#isPostback()
	 */
	public static boolean isPostback() {
		return getContext().isPostback();
	}

	/**
	 * Returns whether the validations phase of the current request has failed.
	 * @return <code>true</code> if the validations phase of the current request has failed, otherwise
	 * <code>false</code>.
	 * @see FacesContext#isValidationFailed()
	 */
	public static boolean isValidationFailed() {
		return getContext().isValidationFailed();
	}

	/**
	 * Returns the HTTP request parameter map.
	 * @return The HTTP request parameter map.
	 * @see ExternalContext#getRequestParameterMap()
	 */
	public static Map<String, String> getRequestParameterMap() {
		return getExternalContext().getRequestParameterMap();
	}

	/**
	 * Returns the HTTP request parameter value associated with the given name.
	 * @param name The HTTP request parameter name.
	 * @return The HTTP request parameter value associated with the given name.
	 * @see ExternalContext#getRequestParameterMap()
	 */
	public static String getRequestParameter(String name) {
		return getRequestParameterMap().get(name);
	}

	/**
	 * Returns the HTTP request parameter values map.
	 * @return The HTTP request parameter values map.
	 * @see ExternalContext#getRequestParameterValuesMap()
	 */
	public static Map<String, String[]> getRequestParameterValuesMap() {
		return getExternalContext().getRequestParameterValuesMap();
	}

	/**
	 * Returns the HTTP request parameter values associated with the given name.
	 * @param name The HTTP request parameter name.
	 * @return The HTTP request parameter values associated with the given name.
	 * @see ExternalContext#getRequestParameterValuesMap()
	 */
	public static String[] getRequestParameterValues(String name) {
		return getRequestParameterValuesMap().get(name);
	}

	/**
	 * Returns the HTTP request header map.
	 * @return The HTTP request header map.
	 * @see ExternalContext#getRequestHeaderMap()
	 */
	public static Map<String, String> getRequestHeaderMap() {
		return getExternalContext().getRequestHeaderMap();
	}

	/**
	 * Returns the HTTP request header value associated with the given name.
	 * @param name The HTTP request header name.
	 * @return The HTTP request header value associated with the given name.
	 * @see ExternalContext#getRequestHeaderMap()
	 */
	public static String getRequestHeader(String name) {
		return getRequestHeaderMap().get(name);
	}

	/**
	 * Returns the HTTP request header values map.
	 * @return The HTTP request header values map.
	 * @see ExternalContext#getRequestHeaderValuesMap()
	 */
	public static Map<String, String[]> getRequestHeaderValuesMap() {
		return getExternalContext().getRequestHeaderValuesMap();
	}

	/**
	 * Returns the HTTP request header values associated with the given name.
	 * @param name The HTTP request header name.
	 * @return The HTTP request header values associated with the given name.
	 * @see ExternalContext#getRequestHeaderValuesMap()
	 */
	public static String[] getRequestHeaderValues(String name) {
		return getRequestHeaderValuesMap().get(name);
	}

	/**
	 * Returns the HTTP request context path. It's the webapp context name, with a leading slash. If the webapp runs
	 * on context root, then it returns an empty string.
	 * @return The HTTP request context path.
	 * @see ExternalContext#getRequestContextPath()
	 */
	public static String getRequestContextPath() {
		return getExternalContext().getRequestContextPath();
	}

	/**
	 * Returns the HTTP request servlet path. If JSF is prefix mapped (e.g. <tt>/faces/*</tt>), then this returns the
	 * whole prefix mapping (e.g. <tt>/faces</tt>). If JSF is suffix mapped (e.g. <tt>*.xhtml</tt>), then this returns
	 * the whole part after the context path, with a leading slash.
	 * @return The HTTP request servlet path.
	 * @see ExternalContext#getRequestServletPath()
	 */
	public static String getRequestServletPath() {
		return getExternalContext().getRequestServletPath();
	}

	/**
	 * Returns the HTTP request path info. If JSF is prefix mapped (e.g. <tt>/faces/*</tt>), then this returns the
	 * whole part after the prefix mapping, with a leading slash. If JSF is suffix mapped (e.g. <tt>*.xhtml</tt>), then
	 * this returns <code>null</code>.
	 * @return The HTTP request path info.
	 * @see ExternalContext#getRequestPathInfo()
	 */
	public static String getRequestPathInfo() {
		return getExternalContext().getRequestPathInfo();
	}

	/**
	 * Returns the HTTP request base URL. This is the URL from the scheme, domain until with context path, including
	 * the trailing slash. This is the value you could use in HTML <code>&lt;base&gt;</code> tag.
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
	 * Returns the HTTP request domain URL. This is the URL with the scheme and domain, without any trailing slash.
	 * @return The HTTP request domain URL.
	 * @see HttpServletRequest#getRequestURL()
	 * @see HttpServletRequest#getRequestURI()
	 * @since 1.1
	 */
	public static String getRequestDomainURL() {
		HttpServletRequest request = getRequest();
		String url = request.getRequestURL().toString();
		return url.substring(0, url.length() - request.getRequestURI().length());
	}

	/**
	 * Returns the HTTP request URL. This is the full request URL as the enduser sees in browser address bar. This does
	 * not include the request query string.
	 * @return The HTTP request URL.
	 * @see HttpServletRequest#getRequestURL()
	 * @since 1.1
	 */
	public static String getRequestURL() {
		return getRequest().getRequestURL().toString();
	}

	/**
	 * Returns the HTTP request URI. This is the part after the domain in the request URL, including the leading slash.
	 * This does not include the request query string.
	 * @return The HTTP request URI.
	 * @see HttpServletRequest#getRequestURI()
	 * @since 1.1
	 */
	public static String getRequestURI() {
		return getRequest().getRequestURI();
	}

	/**
	 * Returns the HTTP request query string. This is the part after the <tt>?</tt> in the request URL as the enduser
	 * sees in browser address bar.
	 * @return The HTTP request query string.
	 * @see HttpServletRequest#getQueryString()
	 * @since 1.1
	 */
	public static String getRequestQueryString() {
		return getRequest().getQueryString();
	}

	// HTTP request ---------------------------------------------------------------------------------------------------

	/**
	 * Returns the HTTP servlet response.
	 * <p>
	 * <i>Note that whenever you absolutely need this method to perform a general task, you might want to consider to
	 * submit a feature request to OmniFaces in order to add a new utility method which performs exactly this general
	 * task.</i>
	 * @return The HTTP servlet response.
	 * @see ExternalContext#getResponse()
	 */
	public static HttpServletResponse getResponse() {
		return (HttpServletResponse) getExternalContext().getResponse();
	}

	/**
	 * Sends a temporary (302) redirect to the given URL. If the given URL does not start with <tt>http://</tt>,
	 * <tt>https://</tt> or <tt>/</tt>, then the request context path will be prepended, otherwise it will be the
	 * unmodified redirect URL.
	 * @param url The URL to redirect the current response to.
	 * @throws IOException Whenever something fails at I/O level. The caller should preferably not catch it, but just
	 * redeclare it in the action method. The servletcontainer will handle it.
	 * @throws NullPointerException When url is <code>null</code>.
	 * @see ExternalContext#redirect(String)
	 */
	public static void redirect(String url) throws IOException {
		getExternalContext().redirect(normalizeRedirectURL(url));
	}

	/**
	 * Sends a permanent (301) redirect to the given URL. If the given URL does not start with <tt>http://</tt>,
	 * <tt>https://</tt> or <tt>/</tt>, then the request context path will be prepended, otherwise it will be the
	 * unmodified redirect URL.
	 * @param url The URL to redirect the current response to.
	 * @throws IOException Whenever something fails at I/O level. The caller should preferably not catch it, but just
	 * redeclare it in the action method. The servletcontainer will handle it.
	 * @throws NullPointerException When url is <code>null</code>.
	 * @see ExternalContext#setResponseStatus(int)
	 * @see ExternalContext#setResponseHeader(String, String)
	 */
	public static void redirectPermanent(String url) throws IOException {
		FacesContext context = getContext();
		ExternalContext externalContext = context.getExternalContext();
		externalContext.setResponseStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
		externalContext.setResponseHeader("Location", normalizeRedirectURL(url));
		externalContext.setResponseHeader("Connection", "close");
		context.responseComplete();
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
		FacesContext context = getContext();
		context.getExternalContext().responseSendError(status, message);
		context.responseComplete();
	}

	/**
	 * Add a header with given name and value to the HTTP response.
	 * @param name The header name.
	 * @param value The header value.
	 * @see ExternalContext#addResponseHeader(String, String)
	 */
	public static void addResponseHeader(String name, String value) {
		getExternalContext().addResponseHeader(name, value);
	}

	/**
	 * Returns whether the response is already committed. That is, when the response headers and a part of the response
	 * body has already been sent to the client. This is usually a point of no return and you can't change the response
	 * anymore.
	 * @return <code>true</code> if the response is already committed, otherwise <code>false</code>.
	 * @see ExternalContext#isResponseCommitted()
	 * @since 1.1
	 */
	public static boolean isResponseCommitted() {
		return getExternalContext().isResponseCommitted();
	}

	/**
	 * Resets the current response. This will clear any headers which are been set and any data which is written to
	 * the response buffer which isn't committed yet.
	 * @throws IllegalStateException When the response is already committed.
	 * @see ExternalContext#responseReset()
	 * @since 1.1
	 */
	public static void responseReset() {
		getExternalContext().responseReset();
	}

	// FORM based authentication --------------------------------------------------------------------------------------

	/**
	 * Perform programmatic login for container managed FORM based authentication. Note that configuration is container
	 * specific and unrelated to JSF. Refer the documentation of the servletcontainer using the keyword "realm".
	 * @param username The login username.
	 * @param password The login password.
	 * @throws ServletException When the login is invalid, or when container managed FORM based authentication is not
	 * enabled.
	 * @see HttpServletRequest#login(String, String)
	 */
	public static void login(String username, String password) throws ServletException {
		getRequest().login(username, password);
	}

	/**
	 * Perform programmatic logout for container managed FORM based authentication. Note that this basically removes
	 * the user principal from the session. It's however better practice to just invalidate the session altogether,
	 * which will implicitly also remove the user principal. Just invoke {@link #invalidateSession()} instead. Note
	 * that the user principal is still present in the response of the current request, it's therefore recommend to
	 * send a redirect after {@link #logout()} or {@link #invalidateSession()}. You can use {@link #redirect(String)}
	 * for this.
	 * @throws ServletException When the logout has failed.
	 * @see HttpServletRequest#logout()
	 */
	public static void logout() throws ServletException {
		getRequest().logout();
	}

	/**
	 * Returns the name of the logged-in user for container managed FORM based authentication, if any.
	 * @return The name of the logged-in user for container managed FORM based authentication, if any.
	 * @see ExternalContext#getRemoteUser()
	 */
	public static String getRemoteUser() {
		return getExternalContext().getRemoteUser();
	}

	/**
	 * Returns whether the currently logged-in user has the given role.
	 * @param role The role to be checked on the currently logged-in user.
	 * @return <code>true</code> if the currently logged-in user has the given role, otherwise <code>false</code>.
	 * @see ExternalContext#isUserInRole(String)
	 */
	public static boolean isUserInRole(String role) {
		return getExternalContext().isUserInRole(role);
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
		Cookie cookie = (Cookie) getExternalContext().getRequestCookieMap().get(name);

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
	 * @param path The cookie path. If this is <tt>/</tt>, then the cookie is available in all pages of the webapp.
	 * If this is <tt>/somespecificpath</tt>, then the cookie is only available in pages under the specified path.
	 * @param maxAge The maximum age of the cookie, in seconds. If this is <tt>0</tt>, then the cookie will be removed.
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

		ExternalContext externalContext = getExternalContext();
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
	 * <p>
	 * <i>Note that whenever you absolutely need this method to perform a general task, you might want to consider to
	 * submit a feature request to OmniFaces in order to add a new utility method which performs exactly this general
	 * task.</i>
	 * @return The HTTP session.
	 * @see ExternalContext#getSession(boolean)
	 */
	public static HttpSession getSession() {
		return getSession(true);
	}

	/**
	 * Returns the HTTP session and creates one if one doesn't exist and <code>create</code> argument is
	 * <code>true</code>, otherwise don't create one and return <code>null</code>.
	 * <p>
	 * <i>Note that whenever you absolutely need this method to perform a general task, you might want to consider to
	 * submit a feature request to OmniFaces in order to add a new utility method which performs exactly this general
	 * task.</i>
	 * @return The HTTP session.
	 * @see ExternalContext#getSession(boolean)
	 */
	public static HttpSession getSession(boolean create) {
		return (HttpSession) getExternalContext().getSession(create);
	}

	/**
	 * Invalidates the current HTTP session. So, any subsequent HTTP request will get a new one when necessary.
	 * @see ExternalContext#invalidateSession()
	 */
	public static void invalidateSession() {
		getExternalContext().invalidateSession();
	}

	/**
	 * Returns whether the HTTP session has already been created.
	 * @return <code>true</code> if the HTTP session has already been created, otherwise <code>false</code>.
	 * @see ExternalContext#getSession(boolean)
	 * @since 1.1
	 */
	public static boolean hasSession() {
		return getSession(false) != null;
	}

	/**
	 * Returns whether the HTTP session has been created for the first time in the current request. This returns also
	 * <code>false</code> when there is no means of a HTTP session.
	 * @return <code>true</code> if the HTTP session has been created for the first time in the current request,
	 * otherwise <code>false</code>.
	 * @see ExternalContext#getSession(boolean)
	 * @see HttpSession#isNew()
	 * @since 1.1
	 */
	public static boolean isSessionNew() {
		HttpSession session = getSession(false);
		return (session != null && session.isNew());
	}

	/**
	 * Returns the time when the HTTP session was created, measured in epoch time. This implicitly creates the session
	 * if one doesn't exist.
	 * @return The time when the HTTP session was created.
	 * @see HttpSession#getCreationTime()
	 * @since 1.1
	 */
	public static long getSessionCreationTime() {
		return getSession().getCreationTime();
	}

	/**
	 * Returns the time of the previous request associated with the current HTTP session, measured in epoch time. This
	 * implicitly creates the session if one doesn't exist.
	 * @return The time of the previous request associated with the current HTTP session.
	 * @see HttpSession#getLastAccessedTime()
	 * @since 1.1
	 */
	public static long getSessionLastAccessedTime() {
		return getSession().getLastAccessedTime();
	}

	/**
	 * Returns the HTTP session timeout in seconds. This implicitly creates the session if one doesn't exist.
	 * @return The HTTP session timeout in seconds.
	 * @see HttpSession#getMaxInactiveInterval()
	 * @since 1.1
	 */
	public static int getSessionMaxInactiveInterval() {
		// Note that JSF 2.1 has this method on ExternalContext. We don't use it in order to be JSF 2.0 compatible.
		return getSession().getMaxInactiveInterval();
	}

	/**
	 * Sets the HTTP session timeout in seconds. A value of 0 or less means that the session should never timeout.
	 * This implicitly creates the session if one doesn't exist.
	 * @param seconds The HTTP session timeout in seconds.
	 * @see HttpSession#setMaxInactiveInterval(int)
	 * @since 1.1
	 */
	public static void setSessionMaxInactiveInterval(int seconds) {
		// Note that JSF 2.1 has this method on ExternalContext. We don't use it in order to be JSF 2.0 compatible.
		getSession().setMaxInactiveInterval(seconds);
	}

	/**
	 * Returns whether the HTTP session has been timed out for the current request. This is helpful if you need to
	 * distinguish between a first-time request on a fresh session and a first-time request on a timed out session, for
	 * example to display "Oops, you have been logged out because your session has been timed out!".
	 * @return <code>true</code> if the HTTP session has been timed out for the current request, otherwise
	 * <code>false</code>.
	 * @see HttpServletRequest#getRequestedSessionId()
	 * @see HttpServletRequest#isRequestedSessionIdValid()
	 * @since 1.1
	 */
	public static boolean hasSessionTimedOut() {
		HttpServletRequest request = getRequest();
		return request.getRequestedSessionId() != null && !request.isRequestedSessionIdValid();
	}

	// Servlet context ------------------------------------------------------------------------------------------------

	/**
	 * Returns the servlet context.
	 * <p>
	 * <i>Note that whenever you absolutely need this method to perform a general task, you might want to consider to
	 * submit a feature request to OmniFaces in order to add a new utility method which performs exactly this general
	 * task.</i>
	 * @return the servlet context.
	 * @see ExternalContext#getContext()
	 */
	public static ServletContext getServletContext() {
		return (ServletContext) getExternalContext().getContext();
	}

	/**
	 * Returns the application initialization parameter map. This returns the parameter name-value pairs of all
	 * <code>&lt;context-param&gt;</code> entries in in <code>web.xml</code>.
	 * @return The application initialization parameter map.
	 * @see ExternalContext#getInitParameterMap()
	 * @since 1.1
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String> getInitParameterMap() {
		return getExternalContext().getInitParameterMap();
	}

	/**
	 * Returns the application initialization parameter. This returns the <code>&lt;param-value&gt;</code> of a
	 * <code>&lt;context-param&gt;</code> in <code>web.xml</code> associated with the given
	 * <code>&lt;param-name&gt;</code>.
	 * @param name The application initialization parameter name.
	 * @return The application initialization parameter value associated with the given name.
	 * @see ExternalContext#getInitParameter(String)
	 * @since 1.1
	 */
	public static String getInitParameter(String name) {
		return getExternalContext().getInitParameter(name);
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
		String mimeType = getExternalContext().getMimeType(name);

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
		return getExternalContext().getResourceAsStream(path);
	}

	/**
	 * Returns a set of available application resource paths matching the specified path.
	 * @param path The partial application resource path used to return matching resource paths.
	 * @return A set of available application resource paths matching the specified path.
	 * @see ExternalContext#getResourcePaths(String)
	 */
	public static Set<String> getResourcePaths(String path) {
		return getExternalContext().getResourcePaths(path);
	}

	/**
	 * Returns the absolute disk file system path representation of the given web content path. This thus converts the
	 * given path of a web content resource (e.g. <code>/index.xhtml</code>) to an absolute disk file system path (e.g.
	 * <code>/path/to/server/work/folder/some.war/index.xhtml</code>) which can then be used in {@link File},
	 * {@link FileInputStream}, etc.
	 * <p>
	 * Note that this will return <code>null</code> when the WAR is not expanded into the disk file system, but instead
	 * into memory. If all you want is just an {@link InputStream} of the web content resource, then better use
	 * {@link #getResourceAsStream(String)} instead.
	 * <p>
	 * Also note that it wouldn't make sense to modify or create files in this location, as those changes would get lost
	 * anyway when the WAR is redeployed or even when the server is restarted. This is thus absolutely not a good
	 * location to store for example uploaded files.
	 * @param webContentPath The web content path to be converted to an absolute disk file system path.
	 * @return The absolute disk file system path representation of the given web content path.
	 * @since 1.2
	 */
	public static String getRealPath(String webContentPath) {
		return getExternalContext().getRealPath(webContentPath);
	}

	// Request scope --------------------------------------------------------------------------------------------------

	/**
	 * Returns the request scope map.
	 * @return The request scope map.
	 * @see ExternalContext#getRequestMap()
	 */
	public static Map<String, Object> getRequestMap() {
		return getExternalContext().getRequestMap();
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
		return (T) getRequestMap().get(name);
	}

	/**
	 * Sets the request scope attribute value associated with the given name.
	 * @param name The request scope attribute name.
	 * @param value The request scope attribute value.
	 * @see ExternalContext#getRequestMap()
	 */
	public static void setRequestAttribute(String name, Object value) {
		getRequestMap().put(name, value);
	}

	/**
	 * Removes the request scope attribute value associated with the given name.
	 * @param name The request scope attribute name.
	 * @return The request scope attribute value previously associated with the given name, or <code>null</code> if
	 * there is no such attribute.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see ExternalContext#getRequestMap()
	 * @since 1.1
	 */
	@SuppressWarnings("unchecked")
	public static <T> T removeRequestAttribute(String name) {
		return (T) getRequestMap().remove(name);
	}

	// Flash scope ----------------------------------------------------------------------------------------------------

	/**
	 * Returns the flash scope. Note that <code>Flash</code> implements <code>Map&lt;String, Object&gt;</code>, so you
	 * can just treat it like a <code>Map&lt;String, Object&gt;</code>.
	 * @return The flash scope.
	 * @see ExternalContext#getFlash()
	 */
	public static Flash getFlash() {
		return getExternalContext().getFlash();
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
		return (T) getFlash().get(name);
	}

	/**
	 * Sets the flash scope attribute value associated with the given name.
	 * @param name The flash scope attribute name.
	 * @param value The flash scope attribute value.
	 * @see ExternalContext#getFlash()
	 */
	public static void setFlashAttribute(String name, Object value) {
		getFlash().put(name, value);
	}

	/**
	 * Removes the flash scope attribute value associated with the given name.
	 * @param name The flash scope attribute name.
	 * @return The flash scope attribute value previously associated with the given name, or <code>null</code> if
	 * there is no such attribute.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see ExternalContext#getFlash()
	 * @since 1.1
	 */
	@SuppressWarnings("unchecked")
	public static <T> T removeFlashAttribute(String name) {
		return (T) getFlash().remove(name);
	}

	// View scope -----------------------------------------------------------------------------------------------------

	/**
	 * Returns the view scope map.
	 * @return The view scope map.
	 * @see UIViewRoot#getViewMap()
	 */
	public static Map<String, Object> getViewMap() {
		return getViewRoot().getViewMap();
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
		return (T) getViewMap().get(name);
	}

	/**
	 * Sets the view scope attribute value associated with the given name.
	 * @param name The view scope attribute name.
	 * @param value The view scope attribute value.
	 * @see UIViewRoot#getViewMap()
	 */
	public static void setViewAttribute(String name, Object value) {
		getViewMap().put(name, value);
	}

	/**
	 * Removes the view scope attribute value associated with the given name.
	 * @param name The view scope attribute name.
	 * @return The view scope attribute value previously associated with the given name, or <code>null</code> if
	 * there is no such attribute.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see UIViewRoot#getViewMap()
	 * @since 1.1
	 */
	@SuppressWarnings("unchecked")
	public static <T> T removeViewAttribute(String name) {
		return (T) getViewMap().remove(name);
	}

	// Session scope --------------------------------------------------------------------------------------------------

	/**
	 * Returns the session scope map.
	 * @return The session scope map.
	 * @see ExternalContext#getSessionMap()
	 */
	public static Map<String, Object> getSessionMap() {
		return getExternalContext().getSessionMap();
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
		return (T) getSessionMap().get(name);
	}

	/**
	 * Sets the session scope attribute value associated with the given name.
	 * @param name The session scope attribute name.
	 * @param value The session scope attribute value.
	 * @see ExternalContext#getSessionMap()
	 */
	public static void setSessionAttribute(String name, Object value) {
		getSessionMap().put(name, value);
	}

	/**
	 * Removes the session scope attribute value associated with the given name.
	 * @param name The session scope attribute name.
	 * @return The session scope attribute value previously associated with the given name, or <code>null</code> if
	 * there is no such attribute.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see ExternalContext#getSessionMap()
	 * @since 1.1
	 */
	@SuppressWarnings("unchecked")
	public static <T> T removeSessionAttribute(String name) {
		return (T) getSessionMap().remove(name);
	}

	// Application scope ----------------------------------------------------------------------------------------------

	/**
	 * Returns the application scope map.
	 * @return The application scope map.
	 * @see ExternalContext#getApplicationMap()
	 */
	public static Map<String, Object> getApplicationMap() {
		return getExternalContext().getApplicationMap();
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
		return (T) getApplicationMap().get(name);
	}

	/**
	 * Sets the application scope attribute value associated with the given name.
	 * @param name The application scope attribute name.
	 * @param value The application scope attribute value.
	 * @see ExternalContext#getApplicationMap()
	 */
	public static void setApplicationAttribute(String name, Object value) {
		getApplicationMap().put(name, value);
	}

	/**
	 * Removes the application scope attribute value associated with the given name.
	 * @param name The application scope attribute name.
	 * @return The application scope attribute value previously associated with the given name, or <code>null</code> if
	 * there is no such attribute.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see ExternalContext#getApplicationMap()
	 * @since 1.1
	 */
	@SuppressWarnings("unchecked")
	public static <T> T removeApplicationAttribute(String name) {
		return (T) getApplicationMap().remove(name);
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
		FacesContext context = getContext();
		ExternalContext externalContext = context.getExternalContext();

		// Prepare the response and set the necessary headers.
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