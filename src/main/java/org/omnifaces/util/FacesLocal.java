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

import static java.util.Arrays.asList;
import static javax.servlet.http.HttpServletResponse.SC_MOVED_PERMANENTLY;
import static org.omnifaces.util.Servlets.prepareRedirectURL;
import static org.omnifaces.util.Utils.encodeURI;
import static org.omnifaces.util.Utils.encodeURL;
import static org.omnifaces.util.Utils.isAnyEmpty;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.ValueExpression;
import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.ProjectStage;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewParameter;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.faces.context.PartialViewContext;
import javax.faces.event.PhaseId;
import javax.faces.render.RenderKit;
import javax.faces.render.RenderKitFactory;
import javax.faces.view.ViewDeclarationLanguage;
import javax.faces.view.ViewMetadata;
import javax.faces.view.facelets.FaceletContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.omnifaces.component.ParamHolder;
import org.omnifaces.config.FacesConfigXml;

/**
 * <p>
 * Collection of utility methods for the JSF API that are mainly shortcuts for obtaining stuff from the provided
 * {@link FacesContext} argument. In effect, it 'flattens' the hierarchy of nested objects.
 * <p>
 * The difference with {@link Faces} is that no one method of {@link FacesLocal} obtains the {@link FacesContext} from
 * the current thread by {@link FacesContext#getCurrentInstance()}. This job is up to the caller. This is more efficient
 * in situations where multiple utility methods needs to be called at the same time. Invoking
 * {@link FacesContext#getCurrentInstance()} is at its own an extremely cheap operation, however as it's to be obtained
 * as a {@link ThreadLocal} variable, it's during the call still blocking all other running threads for some nanoseconds
 * or so.
 * <p>
 * Note that methods which are <strong>directly</strong> available on {@link FacesContext} instance itself, such as
 * {@link FacesContext#getExternalContext()}, {@link FacesContext#getViewRoot()},
 * {@link FacesContext#isValidationFailed()}, etc are not delegated by the this utility class, because it would design
 * technically not make any sense to delegate a single-depth method call like follows:
 * <pre>
 * ExternalContext externalContext = FacesLocal.getExternalContext(facesContext);
 * </pre>
 * <p>
 * instead of just calling it directly like follows:
 * <pre>
 * ExternalContext externalContext = facesContext.getExternalContext();
 * </pre>
 *
 * <h3>Usage</h3>
 * <p>
 * Some examples (for the full list, check the API documentation):
 * <pre>
 * FacesContext context = Faces.getContext();
 * User user = FacesLocal.getSessionAttribute(context, "user");
 * Item item = FacesLocal.evaluateExpressionGet(context, "#{item}");
 * String cookieValue = FacesLocal.getRequestCookie(context, "cookieName");
 * List&lt;Locale&gt; supportedLocales = FacesLocal.getSupportedLocales(context);
 * FacesLocal.invalidateSession(context);
 * FacesLocal.redirect(context, "login.xhtml");
 * </pre>
 *
 * @author Arjan Tijms
 * @author Bauke Scholtz
 * @since 1.6
 * @see Servlets
 */
public final class FacesLocal {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String DEFAULT_MIME_TYPE = "application/octet-stream";
	private static final int DEFAULT_SENDFILE_BUFFER_SIZE = 10240;
	private static final String SENDFILE_HEADER = "%s;filename=\"%2$s\"; filename*=UTF-8''%2$s";
	private static final String ERROR_NO_VIEW = "There is no view.";
	private static final String[] FACELET_CONTEXT_KEYS = {
		FaceletContext.FACELET_CONTEXT_KEY, // Compiletime constant, may fail when compiled against EE6 and run on EE7.
		"com.sun.faces.facelets.FACELET_CONTEXT", // JSF 2.0/2.1.
		"javax.faces.FACELET_CONTEXT" // JSF 2.2.
	};

	// Constructors ---------------------------------------------------------------------------------------------------

	private FacesLocal() {
		// Hide constructor.
	}

	// JSF general ----------------------------------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * @see Faces#getServerInfo()
	 */
	public static String getServerInfo(FacesContext context) {
		return getServletContext(context).getServerInfo();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#isDevelopment()
	 */
	public static boolean isDevelopment(FacesContext context) {
		return context.getApplication().getProjectStage() == ProjectStage.Development;
	}

	/**
	 * {@inheritDoc}
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
	 * {@inheritDoc}
	 * @see Faces#isPrefixMapping()
	 */
	public static boolean isPrefixMapping(FacesContext context) {
		return Faces.isPrefixMapping(getMapping(context));
	}

	/**
	 * {@inheritDoc}
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
	 * {@inheritDoc}
	 * @see Faces#evaluateExpressionSet(String, Object)
	 */
	public static void evaluateExpressionSet(FacesContext context, String expression, Object value) {
		ELContext elContext = context.getELContext();
		ValueExpression valueExpression = context.getApplication().getExpressionFactory()
			.createValueExpression(elContext, expression, Object.class);
		valueExpression.setValue(elContext, value);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#resolveExpressionGet(Object, String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T resolveExpressionGet(FacesContext context, Object base, String property) {
		ELResolver elResolver = context.getApplication().getELResolver();
		return (T) elResolver.getValue(context.getELContext(), base, property);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#resolveExpressionSet(Object, String, Object)
	 */
	public static void resolveExpressionSet(FacesContext context, Object base, String property, Object value) {
		ELResolver elResolver = context.getApplication().getELResolver();
		elResolver.setValue(context.getELContext(), base, property, value);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getContextAttribute(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getContextAttribute(FacesContext context, String name) {
		return (T) context.getAttributes().get(name);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#setContextAttribute(String, Object)
	 */
	public static void setContextAttribute(FacesContext context, String name, Object value) {
		context.getAttributes().put(name, value);
	}

	// JSF views ------------------------------------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * @see Faces#setViewRoot(String)
	 */
	public static void setViewRoot(FacesContext context, String viewId) {
		context.setViewRoot(context.getApplication().getViewHandler().createView(context, viewId));
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getViewId()
	 */
	public static String getViewId(FacesContext context) {
		UIViewRoot viewRoot = context.getViewRoot();
		return (viewRoot != null) ? viewRoot.getViewId() : null;
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getViewName()
	 */
	public static String getViewName(FacesContext context) {
		String viewId = getViewId(context);
		return (viewId != null) ? viewId.substring(viewId.lastIndexOf('/') + 1).split("\\.")[0] : null;
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getViewDeclarationLanguage()
	 */
	public static ViewDeclarationLanguage getViewDeclarationLanguage(FacesContext context) {
		return context.getApplication()
					  .getViewHandler()
					  .getViewDeclarationLanguage(context, context.getViewRoot().getViewId());
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getRenderKit()
	 */
	public static RenderKit getRenderKit(FacesContext context) {
		UIViewRoot view = context.getViewRoot();
		String renderKitId = (view != null) ? view.getRenderKitId() : context.getApplication().getViewHandler().calculateRenderKitId(context);
		return ((RenderKitFactory) FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY)).getRenderKit(context, renderKitId);
	}

	/**
	 * {@inheritDoc}
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
	 * {@inheritDoc}
	 * @see Faces#getViewParameters()
	 */
	public static Collection<UIViewParameter> getViewParameters(FacesContext context) {
		UIViewRoot viewRoot = context.getViewRoot();
		return (viewRoot != null) ? ViewMetadata.getViewParameters(viewRoot) : Collections.<UIViewParameter>emptyList();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getViewParameterMap()
	 */
	public static Map<String, List<String>> getViewParameterMap(FacesContext context) {
		Collection<UIViewParameter> viewParameters = getViewParameters(context);

		if (viewParameters.isEmpty()) {
			return new LinkedHashMap<>(0);
		}

		Map<String, List<String>> parameterMap = new LinkedHashMap<>(viewParameters.size());

		for (UIViewParameter viewParameter : viewParameters) {
			String value = viewParameter.getStringValue(context);

			if (value != null) {
				// <f:viewParam> doesn't support multiple values anyway, so having multiple <f:viewParam> on the
				// same request parameter shouldn't end up in repeated parameters in action URL.
				parameterMap.put(viewParameter.getName(), asList(value));
			}
		}

		return parameterMap;
	}

	/**
	 * {@inheritDoc}
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
	 * {@inheritDoc}
	 * @see Faces#getMetadataAttributes()
	 */
	public static Map<String, Object> getMetadataAttributes(FacesContext context) {
		return context.getViewRoot().getAttributes();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getMetadataAttribute(String, String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getMetadataAttribute(FacesContext context, String viewId, String name) {
		return (T) getMetadataAttributes(context, viewId).get(name);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getMetadataAttribute(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getMetadataAttribute(FacesContext context, String name) {
		return (T) getMetadataAttributes(context).get(name);
	}

	/**
	 * {@inheritDoc}
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
	 * {@inheritDoc}
	 * @see Faces#getDefaultLocale()
	 */
	public static Locale getDefaultLocale(FacesContext context) {
		return context.getApplication().getDefaultLocale();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getSupportedLocales()
	 */
	public static List<Locale> getSupportedLocales(FacesContext context) {
		Application application = context.getApplication();
		List<Locale> supportedLocales = new ArrayList<>();
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
	 * {@inheritDoc}
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
	 * {@inheritDoc}
	 * @see Faces#getMessageBundle()
	 */
	public static ResourceBundle getMessageBundle(FacesContext context) {
		String messageBundle = context.getApplication().getMessageBundle();

		if (messageBundle == null) {
			return null;
		}

		return ResourceBundle.getBundle(messageBundle, getLocale(context));
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getResourceBundle(String)
	 */
	public static ResourceBundle getResourceBundle(FacesContext context, String var) {
		return context.getApplication().getResourceBundle(context, var);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getResourceBundles()
	 */
	public static Map<String, ResourceBundle> getResourceBundles(FacesContext context) {
		Map<String, String> resourceBundles = FacesConfigXml.INSTANCE.getResourceBundles();
		Map<String, ResourceBundle> map = new HashMap<>(resourceBundles.size());

		for (String var : resourceBundles.keySet()) {
			map.put(var, getResourceBundle(context, var));
		}

		return map;
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getBundleString(String)
	 */
	public static String getBundleString(FacesContext context, String key) {
		for (ResourceBundle bundle : getResourceBundles(context).values()) {
			try {
				return bundle.getString(key);
			}
			catch (MissingResourceException ignore) {
				continue;
			}
		}

		return "???" + key + "???";
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#navigate(String)
	 */
	public static void navigate(FacesContext context, String outcome) {
		context.getApplication().getNavigationHandler().handleNavigation(context, null, outcome);
	}

	/**
	 * {@inheritDoc}
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
	 * {@inheritDoc}
	 * @see Faces#getBookmarkableURL(String, Map, boolean)
	 */
	public static String getBookmarkableURL
		(FacesContext context, String viewId, Map<String, List<String>> params, boolean includeViewParams)
	{
		Map<String, List<String>> map = new HashMap<>();

		if (params != null) {
			for (Entry<String, List<String>> param : params.entrySet()) {
				for (String value : param.getValue()) {
					addParamToMapIfNecessary(map, param.getKey(), value);
				}
			}
		}

		return context.getApplication().getViewHandler().getBookmarkableURL(context, viewId, map, includeViewParams);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getBookmarkableURL(Collection, boolean)
	 */
	public static String getBookmarkableURL
		(FacesContext context, Collection<? extends ParamHolder> params, boolean includeViewParams)
	{
		String viewId = getViewId(context);

		if (viewId == null) {
			throw new IllegalStateException(ERROR_NO_VIEW);
		}

		return getBookmarkableURL(context, viewId, params, includeViewParams);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getBookmarkableURL(String, Collection, boolean)
	 */
	public static String getBookmarkableURL
		(FacesContext context, String viewId, Collection<? extends ParamHolder> params, boolean includeViewParams)
	{
		Map<String, List<String>> map = new HashMap<>();

		if (params != null) {
			for (ParamHolder param : params) {
				addParamToMapIfNecessary(map, param.getName(), param.getValue());
			}
		}

		return context.getApplication().getViewHandler().getBookmarkableURL(context, viewId, map, includeViewParams);
	}

	private static void addParamToMapIfNecessary(Map<String, List<String>> map, String name, Object value) {
		if (isAnyEmpty(name, value)) {
			return;
		}

		List<String> values = map.get(name);

		if (values == null) {
			values = new ArrayList<>(1);
			map.put(name, values);
		}

		values.add(value.toString());
	}

	// Facelets -------------------------------------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * @see Faces#getFaceletContext()
	 */
	public static FaceletContext getFaceletContext(FacesContext context) {
		Map<Object, Object> contextAttributes = context.getAttributes();

		for (String key : FACELET_CONTEXT_KEYS) {
			FaceletContext faceletContext = (FaceletContext) contextAttributes.get(key);

			if (faceletContext != null) {
				return faceletContext;
			}
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getFaceletAttribute(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getFaceletAttribute(FacesContext context, String name) {
		return (T) getFaceletContext(context).getAttribute(name);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#setFaceletAttribute(String, Object)
	 */
	public static void setFaceletAttribute(FacesContext context, String name, Object value) {
		getFaceletContext(context).setAttribute(name, value);
	}

	// HTTP request ---------------------------------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * @see Faces#getRequest()
	 */
	public static HttpServletRequest getRequest(FacesContext context) {
		return (HttpServletRequest) context.getExternalContext().getRequest();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#isAjaxRequest()
	 */
	public static boolean isAjaxRequest(FacesContext context) {
		return context.getPartialViewContext().isAjaxRequest();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#isAjaxRequestWithPartialRendering()
	 */
	public static boolean isAjaxRequestWithPartialRendering(FacesContext context) {
		PartialViewContext pvc = context.getPartialViewContext();
		return pvc.isAjaxRequest() && !pvc.isRenderAll();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getRequestParameterMap()
	 */
	public static Map<String, String> getRequestParameterMap(FacesContext context) {
		return context.getExternalContext().getRequestParameterMap();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getRequestParameter(String)
	 */
	public static String getRequestParameter(FacesContext context, String name) {
		return getRequestParameterMap(context).get(name);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getRequestParameterValuesMap()
	 */
	public static Map<String, String[]> getRequestParameterValuesMap(FacesContext context) {
		return context.getExternalContext().getRequestParameterValuesMap();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getRequestParameterValues(String)
	 */
	public static String[] getRequestParameterValues(FacesContext context, String name) {
		return getRequestParameterValuesMap(context).get(name);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getRequestHeaderMap()
	 */
	public static Map<String, String> getRequestHeaderMap(FacesContext context) {
		return context.getExternalContext().getRequestHeaderMap();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getRequestHeader(String)
	 */
	public static String getRequestHeader(FacesContext context, String name) {
		return getRequestHeaderMap(context).get(name);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getRequestHeaderValuesMap()
	 */
	public static Map<String, String[]> getRequestHeaderValuesMap(FacesContext context) {
		return context.getExternalContext().getRequestHeaderValuesMap();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getRequestHeaderValues(String)
	 */
	public static String[] getRequestHeaderValues(FacesContext context, String name) {
		return getRequestHeaderValuesMap(context).get(name);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getRequestContextPath()
	 */
	public static String getRequestContextPath(FacesContext context) {
		return context.getExternalContext().getRequestContextPath();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getRequestServletPath()
	 */
	public static String getRequestServletPath(FacesContext context) {
		return context.getExternalContext().getRequestServletPath();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getRequestPathInfo()
	 */
	public static String getRequestPathInfo(FacesContext context) {
		return context.getExternalContext().getRequestPathInfo();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getRequestHostname()
	 */
	public static String getRequestHostname(FacesContext context) {
		return Servlets.getRequestHostname(getRequest(context));
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getRequestBaseURL()
	 */
	public static String getRequestBaseURL(FacesContext context) {
		return Servlets.getRequestBaseURL(getRequest(context));
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getRequestDomainURL()
	 */
	public static String getRequestDomainURL(FacesContext context) {
		return Servlets.getRequestDomainURL(getRequest(context));
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getRequestURL()
	 */
	public static String getRequestURL(FacesContext context) {
		return Servlets.getRequestURL(getRequest(context));
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getRequestURI()
	 */
	public static String getRequestURI(FacesContext context) {
		return Servlets.getRequestURI(getRequest(context));
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getRequestQueryString()
	 */
	public static String getRequestQueryString(FacesContext context) {
		return Servlets.getRequestQueryString(getRequest(context));
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getRequestQueryStringMap()
	 */
	public static Map<String, List<String>> getRequestQueryStringMap(FacesContext context) {
		return Servlets.getRequestQueryStringMap(getRequest(context));
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getRequestURLWithQueryString()
	 */
	public static String getRequestURLWithQueryString(FacesContext context) {
		return Servlets.getRequestURLWithQueryString(getRequest(context));
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getRequestURIWithQueryString()
	 */
	public static String getRequestURIWithQueryString(FacesContext context) {
		return Servlets.getRequestURIWithQueryString(getRequest(context));
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getForwardRequestURI()
	 * @deprecated Since 2.4. This is abstracted away by {@link #getRequestURI(FacesContext)}. Use it instead.
	 * JSF has as to retrieving request URI no business of knowing if the request is forwarded/rewritten or not.
	 */
	@Deprecated // TODO: Remove in OmniFaces 3.0.
	public static String getForwardRequestURI(FacesContext context) {
		return Servlets.getForwardRequestURI(getRequest(context));
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getForwardRequestQueryString()
	 * @deprecated Since 2.4. This is abstracted away by {@link #getRequestQueryString(FacesContext)}. Use it instead.
	 * JSF has as to retrieving request URI no business of knowing if the request is forwarded/rewritten or not.
	 */
	@Deprecated // TODO: Remove in OmniFaces 3.0.
	public static String getForwardRequestQueryString(FacesContext context) {
		return Servlets.getForwardRequestQueryString(getRequest(context));
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getForwardRequestURIWithQueryString()
	 * @deprecated Since 2.4. This is abstracted away by {@link #getRequestURIWithQueryString(FacesContext)}. Use it instead.
	 * JSF has as to retrieving request URI no business of knowing if the request is forwarded/rewritten or not.
	 */
	@Deprecated // TODO: Remove in OmniFaces 3.0.
	public static String getForwardRequestURIWithQueryString(FacesContext context) {
		return Servlets.getForwardRequestURIWithQueryString(getRequest(context));
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getRemoteAddr()
	 */
	public static String getRemoteAddr(FacesContext context) {
		return Servlets.getRemoteAddr(getRequest(context));
	}

	// HTTP response --------------------------------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * @see Faces#getResponse()
	 */
	public static HttpServletResponse getResponse(FacesContext context) {
		return (HttpServletResponse) context.getExternalContext().getResponse();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getResponseBufferSize()
	 */
	public static int getResponseBufferSize(FacesContext context) {
		return context.getExternalContext().getResponseBufferSize();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getResponseCharacterEncoding()
	 */
	public static String getResponseCharacterEncoding(FacesContext context) {
		return context.getExternalContext().getResponseCharacterEncoding();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#setResponseStatus(int)
	 */
	public static void setResponseStatus(FacesContext context, int status) {
		context.getExternalContext().setResponseStatus(status);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#redirect(String, String...)
	 */
	public static void redirect(FacesContext context, String url, String... paramValues) throws IOException {
		ExternalContext externalContext = context.getExternalContext();
		externalContext.getFlash().setRedirect(true); // MyFaces also requires this for a redirect in current request (which is incorrect).
		externalContext.redirect(prepareRedirectURL(getRequest(context), url, paramValues));
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#redirectPermanent(String, String...)
	 */
	public static void redirectPermanent(FacesContext context, String url, String... paramValues) {
		ExternalContext externalContext = context.getExternalContext();
		externalContext.getFlash().setRedirect(true); // MyFaces also requires this for a redirect in current request (which is incorrect).
		externalContext.setResponseStatus(SC_MOVED_PERMANENTLY);
		externalContext.setResponseHeader("Location", prepareRedirectURL(getRequest(context), url, paramValues));
		externalContext.setResponseHeader("Connection", "close");
		context.responseComplete();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#refresh()
	 */
	public static void refresh(FacesContext context) throws IOException {
		redirect(context, getRequestURI(context));
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#refreshWithQueryString()
	 */
	public static void refreshWithQueryString(FacesContext context) throws IOException {
		redirect(context, getRequestURIWithQueryString(context));
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#responseSendError(int, String)
	 */
	public static void responseSendError(FacesContext context, int status, String message) throws IOException {
		context.getExternalContext().responseSendError(status, message);
		context.responseComplete();

		// Below is a workaround for disappearing FacesContext in WildFly/Undertow. It disappears because Undertow
		// immediately performs a forward to error page instead of waiting until servlet's service is finished. When
		// the error page is a JSF page as well, then it implicitly invokes FacesServlet once again, which in turn
		// creates another FacesContext which overrides the current FacesContext in the same thread! So, when the
		// FacesContext which is created during the forward is released, it leaves the current FacesContext as null,
		// causing NPE over all place which is relying on FacesContext#getCurrentInstance().
		// This is already fixed in WildFly 8.2 / Undertow 1.1.0 as per https://issues.jboss.org/browse/UNDERTOW-322.
		if (!Faces.hasContext()) {
			Faces.setContext(context);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#addResponseHeader(String, String)
	 */
	public static void addResponseHeader(FacesContext context, String name, String value) {
		context.getExternalContext().addResponseHeader(name, value);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#isResponseCommitted()
	 */
	public static boolean isResponseCommitted(FacesContext context) {
		return context.getExternalContext().isResponseCommitted();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#responseReset()
	 */
	public static void responseReset(FacesContext context) {
		context.getExternalContext().responseReset();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#isRenderResponse()
	 */
	public static boolean isRenderResponse(FacesContext context) {
		return context.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE;
	}

	// FORM based authentication --------------------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * @see Faces#login(String, String)
	 */
	public static void login(FacesContext context, String username, String password) throws ServletException {
		getRequest(context).login(username, password);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#authenticate()
	 */
	public static boolean authenticate(FacesContext context) throws ServletException, IOException {
		return getRequest(context).authenticate(getResponse(context));
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#logout()
	 */
	public static void logout(FacesContext context) throws ServletException {
		getRequest(context).logout();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getRemoteUser()
	 */
	public static String getRemoteUser(FacesContext context) {
		return context.getExternalContext().getRemoteUser();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#isUserInRole(String)
	 */
	public static boolean isUserInRole(FacesContext context, String role) {
		return context.getExternalContext().isUserInRole(role);
	}

	// HTTP cookies ---------------------------------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * @see Faces#getRequestCookie(String)
	 */
	public static String getRequestCookie(FacesContext context, String name) {
		Cookie cookie = (Cookie) context.getExternalContext().getRequestCookieMap().get(name);
		return (cookie != null) ? Utils.decodeURL(cookie.getValue()) : null;
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#addResponseCookie(String, String, int)
	 */
	public static void addResponseCookie(FacesContext context, String name, String value, int maxAge) {
		addResponseCookie(context, name, value, getRequestHostname(context), null, maxAge);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#addResponseCookie(String, String, String, int)
	 */
	public static void addResponseCookie(FacesContext context, String name, String value, String path, int maxAge) {
		addResponseCookie(context, name, value, getRequestHostname(context), path, maxAge);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#addResponseCookie(String, String, String, String, int)
	 */
	public static void addResponseCookie(FacesContext context, String name, String value, String domain, String path, int maxAge) {
		ExternalContext externalContext = context.getExternalContext();
		Map<String, Object> properties = new HashMap<>();

		if (domain != null && !domain.equals("localhost")) { // Chrome doesn't like domain:"localhost" on cookies.
			properties.put("domain", domain);
		}

		if (path != null) {
			properties.put("path", path);
		}

		properties.put("maxAge", maxAge);
		properties.put("secure", ((HttpServletRequest) externalContext.getRequest()).isSecure());
		externalContext.addResponseCookie(name, encodeURL(value), properties);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#removeResponseCookie(String, String)
	 */
	public static void removeResponseCookie(FacesContext context, String name, String path) {
		addResponseCookie(context, name, null, path, 0);
	}

	// HTTP session ---------------------------------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * @see Faces#getSession()
	 */
	public static HttpSession getSession(FacesContext context) {
		return getSession(context, true);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getSession(boolean)
	 */
	public static HttpSession getSession(FacesContext context, boolean create) {
		return (HttpSession) context.getExternalContext().getSession(create);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getSessionId()
	 */
	public static String getSessionId(FacesContext context) {
		HttpSession session = getSession(context, false);
		return (session != null) ? session.getId() : null;
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#invalidateSession()
	 */
	public static void invalidateSession(FacesContext context) {
		context.getExternalContext().invalidateSession();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#hasSession()
	 */
	public static boolean hasSession(FacesContext context) {
		return getSession(context, false) != null;
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#isSessionNew()
	 */
	public static boolean isSessionNew(FacesContext context) {
		HttpSession session = getSession(context, false);
		return (session != null && session.isNew());
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getSessionCreationTime()
	 */
	public static long getSessionCreationTime(FacesContext context) {
		return getSession(context).getCreationTime();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getSessionLastAccessedTime()
	 */
	public static long getSessionLastAccessedTime(FacesContext context) {
		return getSession(context).getLastAccessedTime();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getSessionMaxInactiveInterval()
	 */
	public static int getSessionMaxInactiveInterval(FacesContext context) {
		// Note that JSF 2.1 has this method on ExternalContext. We don't use it in order to be JSF 2.0 compatible.
		return getSession(context).getMaxInactiveInterval();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#setSessionMaxInactiveInterval(int)
	 */
	public static void setSessionMaxInactiveInterval(FacesContext context, int seconds) {
		// Note that JSF 2.1 has this method on ExternalContext. We don't use it in order to be JSF 2.0 compatible.
		getSession(context).setMaxInactiveInterval(seconds);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#hasSessionTimedOut()
	 */
	public static boolean hasSessionTimedOut(FacesContext context) {
		HttpServletRequest request = getRequest(context);
		return request.getRequestedSessionId() != null && !request.isRequestedSessionIdValid();
	}

	// Servlet context ------------------------------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * @see Faces#getServletContext()
	 */
	public static ServletContext getServletContext(FacesContext context) {
		return (ServletContext) context.getExternalContext().getContext();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getInitParameterMap()
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String> getInitParameterMap(FacesContext context) {
		return context.getExternalContext().getInitParameterMap();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getInitParameter(String)
	 */
	public static String getInitParameter(FacesContext context, String name) {
		return context.getExternalContext().getInitParameter(name);
	}

	/**
	 * {@inheritDoc}
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
	 * {@inheritDoc}
	 * @see Faces#getResource(String)
	 */
	public static URL getResource(FacesContext context, String path) throws MalformedURLException {
		return context.getExternalContext().getResource(path);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getResourceAsStream(String)
	 */
	public static InputStream getResourceAsStream(FacesContext context, String path) {
		return context.getExternalContext().getResourceAsStream(path);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getResourcePaths(String)
	 */
	public static Set<String> getResourcePaths(FacesContext context, String path) {
		return context.getExternalContext().getResourcePaths(path);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getRealPath(String)
	 */
	public static String getRealPath(FacesContext context, String webContentPath) {
		return context.getExternalContext().getRealPath(webContentPath);
	}

	// Request scope --------------------------------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * @see Faces#getRequestMap()
	 */
	public static Map<String, Object> getRequestMap(FacesContext context) {
		return context.getExternalContext().getRequestMap();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getRequestAttribute(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getRequestAttribute(FacesContext context, String name) {
		return (T) getRequestMap(context).get(name);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#setRequestAttribute(String, Object)
	 */
	public static void setRequestAttribute(FacesContext context, String name, Object value) {
		getRequestMap(context).put(name, value);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#removeRequestAttribute(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T removeRequestAttribute(FacesContext context, String name) {
		return (T) getRequestMap(context).remove(name);
	}

	// Flash scope ----------------------------------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * @see Faces#getFlash()
	 */
	public static Flash getFlash(FacesContext context) {
		return context.getExternalContext().getFlash();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getFlashAttribute(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getFlashAttribute(FacesContext context, String name) {
		return (T) getFlash(context).get(name);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#setFlashAttribute(String, Object)
	 */
	public static void setFlashAttribute(FacesContext context, String name, Object value) {
		getFlash(context).put(name, value);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#removeFlashAttribute(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T removeFlashAttribute(FacesContext context, String name) {
		return (T) getFlash(context).remove(name);
	}

	// View scope -----------------------------------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * @see Faces#getViewMap()
	 */
	public static Map<String, Object> getViewMap(FacesContext context) {
		return context.getViewRoot().getViewMap();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getViewAttribute(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getViewAttribute(FacesContext context, String name) {
		return (T) getViewMap(context).get(name);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#setViewAttribute(String, Object)
	 */
	public static void setViewAttribute(FacesContext context, String name, Object value) {
		getViewMap(context).put(name, value);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#removeViewAttribute(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T removeViewAttribute(FacesContext context, String name) {
		return (T) getViewMap(context).remove(name);
	}

	// Session scope --------------------------------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * @see Faces#getSessionMap()
	 */
	public static Map<String, Object> getSessionMap(FacesContext context) {
		return context.getExternalContext().getSessionMap();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getSessionAttribute(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getSessionAttribute(FacesContext context, String name) {
		return (T) getSessionMap(context).get(name);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#setSessionAttribute(String, Object)
	 */
	public static void setSessionAttribute(FacesContext context, String name, Object value) {
		getSessionMap(context).put(name, value);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#removeSessionAttribute(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T removeSessionAttribute(FacesContext context, String name) {
		return (T) getSessionMap(context).remove(name);
	}

	// Application scope ----------------------------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * @see Faces#getApplicationMap()
	 */
	public static Map<String, Object> getApplicationMap(FacesContext context) {
		return context.getExternalContext().getApplicationMap();
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#getApplicationAttribute(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getApplicationAttribute(FacesContext context, String name) {
		return (T) getApplicationMap(context).get(name);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#setApplicationAttribute(String, Object)
	 */
	public static void setApplicationAttribute(FacesContext context, String name, Object value) {
		getApplicationMap(context).put(name, value);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#removeApplicationAttribute(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T removeApplicationAttribute(FacesContext context, String name) {
		return (T) getApplicationMap(context).remove(name);
	}

	// File download --------------------------------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 * @see Faces#sendFile(File, boolean)
	 */
	public static void sendFile(FacesContext context, File file, boolean attachment) throws IOException {
		sendFile(context, new FileInputStream(file), file.getName(), file.length(), attachment);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#sendFile(byte[], String, boolean)
	 */
	public static void sendFile(FacesContext context, byte[] content, String filename, boolean attachment)
		throws IOException
	{
		sendFile(context, new ByteArrayInputStream(content), filename, content.length, attachment);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#sendFile(InputStream, String, boolean)
	 */
	public static void sendFile(FacesContext context, InputStream content, String filename, boolean attachment)
		throws IOException
	{
		sendFile(context, content, filename, -1, attachment);
	}

	/**
	 * {@inheritDoc}
	 * @see Faces#sendFile(String, boolean, org.omnifaces.util.Callback.Output)
	 */
	public static void sendFile(FacesContext context, String filename, boolean attachment, Callback.Output outputCallback)
		throws IOException
	{
		ExternalContext externalContext = context.getExternalContext();

		// Prepare the response and set the necessary headers.
		externalContext.setResponseBufferSize(DEFAULT_SENDFILE_BUFFER_SIZE);
		externalContext.setResponseContentType(getMimeType(context, filename));
		externalContext.setResponseHeader("Content-Disposition", String.format(SENDFILE_HEADER,
			(attachment ? "attachment" : "inline"), encodeURI(filename)));

		// Not exactly mandatory, but this fixes at least a MSIE quirk: http://support.microsoft.com/kb/316431
		if (((HttpServletRequest) externalContext.getRequest()).isSecure()) {
			externalContext.setResponseHeader("Cache-Control", "public");
			externalContext.setResponseHeader("Pragma", "public");
		}

		try (OutputStream output = externalContext.getResponseOutputStream()) {
			outputCallback.writeTo(output);
		}

		context.responseComplete();
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
		(final FacesContext context, final InputStream input, String filename, final long contentLength, boolean attachment)
			throws IOException
	{
		sendFile(context, filename, attachment, new Callback.Output() {
			@Override
			public void writeTo(OutputStream output) throws IOException {
				ExternalContext externalContext = context.getExternalContext();

				// If content length is known, set it. Note that setResponseContentLength() cannot be used as it takes only int.
				if (contentLength != -1) {
					externalContext.setResponseHeader("Content-Length", String.valueOf(contentLength));
				}

				long size = Utils.stream(input, output);

				// This may be on time for files smaller than the default buffer size, but is otherwise ignored anyway.
				if (contentLength == -1 && !externalContext.isResponseCommitted()) {
					externalContext.setResponseHeader("Content-Length", String.valueOf(size));
				}
			}
		});
	}

}
