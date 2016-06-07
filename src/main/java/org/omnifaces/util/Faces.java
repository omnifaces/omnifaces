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

import static javax.faces.FactoryFinder.APPLICATION_FACTORY;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;
import javax.faces.application.NavigationHandler;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewParameter;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextWrapper;
import javax.faces.context.Flash;
import javax.faces.context.PartialViewContext;
import javax.faces.event.PhaseId;
import javax.faces.render.RenderKit;
import javax.faces.view.ViewDeclarationLanguage;
import javax.faces.view.ViewMetadata;
import javax.faces.view.facelets.FaceletContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.omnifaces.component.ParamHolder;
import org.omnifaces.config.FacesConfigXml;

/**
 * <p>
 * Collection of utility methods for the JSF API that are mainly shortcuts for obtaining stuff from the thread local
 * {@link FacesContext}. In effects, it 'flattens' the hierarchy of nested objects. Do note that using the hierarchy is
 * actually a better software design practice, but can lead to verbose code.
 * <p>
 * Next to those oneliner delegate calls, there are also some helpful methods which eliminates multiline boilerplate
 * code, such as {@link #getLocale()} which returns sane fallback values, a more convenient
 * {@link #redirect(String, String...)} which automatically prepends the context path when the path does not start with
 * <code>/</code> and offers support for URL encoding of request parameters supplied by varargs argument, and several
 * useful {@link #sendFile(File, boolean)} methods which allows you to provide a {@link File}, <code>byte[]</code> or
 * {@link InputStream} as a download to the client.
 *
 * <h3>Usage</h3>
 * <p>
 * Some examples:
 * <pre>
 * // Get a session attribute (no explicit cast necessary!).
 * User user = Faces.getSessionAttribute("user");
 * </pre>
 * <pre>
 * // Evaluate EL programmatically (no explicit cast necessary!).
 * Item item = Faces.evaluateExpressionGet("#{item}");
 * </pre>
 * <pre>
 * // Get a cookie value.
 * String cookieValue = Faces.getRequestCookie("cookieName");
 * </pre>
 * <pre>
 * // Get all supported locales with default locale as first item.
 * List&lt;Locale&gt; supportedLocales = Faces.getSupportedLocales();
 * </pre>
 * <pre>
 * // Check in e.g. preRenderView if session has been timed out.
 * if (Faces.hasSessionTimedOut()) {
 *     Messages.addGlobalWarn("Oops, you have been logged out because your session was been timed out!");
 * }
 * </pre>
 * <pre>
 * // Get value of &lt;f:metadata&gt;&lt;f:attribute name="foo"&gt; of different view without building it.
 * String foo = Faces.getMetadataAttribute("/other.xhtml", "foo");
 * </pre>
 * <pre>
 * // Send a redirect with parameters UTF-8 encoded in query string.
 * Faces.redirect("product.xhtml?id=%d&amp;name=%s", product.getId(), product.getName());
 * </pre>
 * <pre>
 * // Invalidate the session and send a redirect.
 * public void logout() throws IOException {
 *     Faces.invalidateSession();
 *     Faces.redirect("login.xhtml"); // Can by the way also be done by return "login?faces-redirect=true" if in action method.
 * }
 * </pre>
 * <pre>
 * // Provide a file as attachment.
 * public void download() throws IOException {
 *     Faces.sendFile(new File("/path/to/file.ext"), true);
 * }
 * </pre>
 * <pre>
 * // Provide a file as attachment via output stream callback.
 * public void download() throws IOException {
 *     Faces.sendFile("file.ext", true, o -&gt; Files.copy(Paths.get("/path/to/file.ext"), o));
 * }
 * </pre>
 *
 * <h3>FacesLocal</h3>
 * <p>
 * Note that there's normally a minor overhead in obtaining the thread local {@link FacesContext}. In case client code
 * needs to call methods in this class multiple times it's expected that performance will be slightly better if instead
 * the {@link FacesContext} is obtained once and the required methods are called on that, although the difference is
 * practically negligible when used in modern server hardware.
 * <p>
 * In such case, consider using {@link FacesLocal} instead. The difference with {@link Faces} is that no one method of
 * {@link FacesLocal} obtains the {@link FacesContext} from the current thread by
 * {@link FacesContext#getCurrentInstance()}. This job is up to the caller.
 *
 * @author Arjan Tijms
 * @author Bauke Scholtz
 * @see FacesLocal
 * @see Servlets
 */
public final class Faces {

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
	 * Sets the given faces context as current instance. Use this if you have a custom {@link FacesContextWrapper}
	 * which you'd like to (temporarily) use as the current instance of the faces context.
	 * @param context The faces context to be set as the current instance.
	 * @since 1.3
	 */
	public static void setContext(FacesContext context) {
		FacesContextSetter.setCurrentInstance(context);
	}

	/**
	 * Inner class so that the protected {@link FacesContext#setCurrentInstance(FacesContext)} method can be invoked.
	 * @author Bauke Scholtz
	 */
	private abstract static class FacesContextSetter extends FacesContext {
		protected static void setCurrentInstance(FacesContext context) {
			FacesContext.setCurrentInstance(context);
		}
	}

	/**
	 * Returns <code>true</code> when the current faces context is available (i.e. it is not <code>null</code>).
	 * @return <code>true</code> when the current faces context is available.
	 * @since 2.0
	 */
	public static boolean hasContext() {
		return getContext() != null;
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
	 * Gets the JSF Application singleton from the FactoryFinder.
	 * <p>
	 * This method is an alternative for {@link Faces#getApplication()} for those situations where the
	 * {@link FacesContext} isn't available.
	 *
	 * @return The faces application singleton.
	 */
	public static Application getApplicationFromFactory() {
		return ((ApplicationFactory) FactoryFinder.getFactory(APPLICATION_FACTORY)).getApplication();
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
		return FacesLocal.getServerInfo(getContext());
	}

	/**
	 * Returns whether we're in development stage. This will be the case when the <code>javax.faces.PROJECT_STAGE</code>
	 * context parameter in <code>web.xml</code> is set to <code>Development</code>.
	 * @return <code>true</code> if we're in development stage, otherwise <code>false</code>.
	 * @see Application#getProjectStage()
	 */
	public static boolean isDevelopment() {
		return FacesLocal.isDevelopment(getContext());
	}

	/**
	 * Determines and returns the faces servlet mapping used in the current request. If JSF is prefix mapped (e.g.
	 * <code>/faces/*</code>), then this returns the whole path, with a leading slash (e.g. <code>/faces</code>). If JSF
	 * is suffix mapped (e.g. <code>*.xhtml</code>), then this returns the whole extension (e.g. <code>.xhtml</code>).
	 * @return The faces servlet mapping (without the wildcard).
	 * @see #getRequestPathInfo()
	 * @see #getRequestServletPath()
	 */
	public static String getMapping() {
		return FacesLocal.getMapping(getContext());
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
	 * Returns whether the given faces servlet mapping is a prefix mapping. Use this method in preference to
	 * {@link #isPrefixMapping()} when you already have obtained the mapping from {@link #getMapping()} so that the
	 * mapping won't be calculated twice.
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
	 * Signals JSF that the validations phase of the current request has failed. This can be invoked in any other
	 * phase than the validations phase. The value can be read by {@link #isValidationFailed()} in Java and by
	 * <code>#{facesContext.validationFailed}</code> in EL.
	 * @see FacesContext#validationFailed()
	 */
	public static void validationFailed() {
		getContext().validationFailed();
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
	 * Returns the current EL context.
	 * @return The current EL context.
	 * @see FacesContext#getELContext()
	 * @since 2.0
	 */
	public static ELContext getELContext() {
		return getContext().getELContext();
	}

	/**
	 * Programmatically evaluate the given EL expression and return the evaluated value.
	 * @param <T> The expected return type.
	 * @param expression The EL expression to be evaluated.
	 * @return The evaluated value of the given EL expression.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see Application#evaluateExpressionGet(FacesContext, String, Class)
	 */
	public static <T> T evaluateExpressionGet(String expression) {
		return FacesLocal.evaluateExpressionGet(getContext(), expression);
	}

	/**
	 * Programmatically evaluate the given EL expression and set the given value.
	 * @param expression The EL expression to be evaluated.
	 * @param value The value to be set in the property behind the given EL expression.
	 * @see Application#getExpressionFactory()
	 * @see ExpressionFactory#createValueExpression(ELContext, String, Class)
	 * @see ValueExpression#setValue(ELContext, Object)
	 * @since 1.1
	 */
	public static void evaluateExpressionSet(String expression, Object value) {
		FacesLocal.evaluateExpressionSet(getContext(), expression, value);
	}

	/**
	 * Programmatically EL-resolve the given property on the given base object and return the resolved value.
	 * @param <T> The expected return type.
	 * @param base The base object whose property value is to be returned, or null to resolve a top-level variable.
	 * @param property The property or variable to be resolved on the given base.
	 * @return The resolved value of the given property on the given base object.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see Application#getELResolver()
	 * @see ELResolver#getValue(ELContext, Object, Object)
	 * @since 2.1
	 */
	public static <T> T resolveExpressionGet(Object base, String property) {
		return FacesLocal.resolveExpressionGet(getContext(), base, property);
	}

	/**
	 * Programmatically EL-resolve the given property on the given base object and set the given value.
	 * @param base The base object whose property value is to be set, or null to set a top-level variable.
	 * @param property The property or variable to be set on the given base.
	 * @param value The value to be set in the property on the given base.
	 * @see Application#getELResolver()
	 * @see ELResolver#setValue(ELContext, Object, Object, Object)
	 * @since 2.1
	 */
	public static void resolveExpressionSet(Object base, String property, Object value) {
		FacesLocal.resolveExpressionSet(getContext(), base, property, value);
	}

	/**
	 * Returns the Faces context attribute value associated with the given name.
	 * @param <T> The expected return type.
	 * @param name The Faces context attribute name.
	 * @return The Faces context attribute value associated with the given name.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see FacesContext#getAttributes()
	 * @since 1.3
	 */
	public static <T> T getContextAttribute(String name) {
		return FacesLocal.getContextAttribute(getContext(), name);
	}

	/**
	 * Sets the Faces context attribute value associated with the given name.
	 * @param name The Faces context attribute name.
	 * @param value The Faces context attribute value.
	 * @see FacesContext#getAttributes()
	 * @since 1.3
	 */
	public static void setContextAttribute(String name, Object value) {
		FacesLocal.setContextAttribute(getContext(), name, value);
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
		FacesLocal.setViewRoot(getContext(), viewId);
	}

	/**
	 * Returns the ID of the current view root, or <code>null</code> if there is no view.
	 * @return The ID of the current view root, or <code>null</code> if there is no view.
	 * @see UIViewRoot#getViewId()
	 */
	public static String getViewId() {
		return FacesLocal.getViewId(getContext());
	}

	/**
	 * Returns the base name of the current view, without extension, or <code>null</code> if there is no view.
	 * E.g. if the view ID is <code>/path/to/some.xhtml</code>, then this will return <code>some</code>.
	 * @return The base name of the current view, without extension, or <code>null</code> if there is no view.
	 * @see UIViewRoot#getViewId()
	 * @since 2.3
	 */
	public static String getViewName() {
		return FacesLocal.getViewName(getContext());
	}

	/**
	 * Returns the {@link ViewDeclarationLanguage} associated with the "current" view ID.
	 * <p>
	 * The current view ID is the view ID that's set for the view root that's associated with
	 * the current faces context.
	 *
	 * @return The {@link ViewDeclarationLanguage} associated with the "current" view ID.
	 * @since 1.8
	 */
	public static ViewDeclarationLanguage getViewDeclarationLanguage() {
		return FacesLocal.getViewDeclarationLanguage(getContext());
	}

	/**
	 * Returns the {@link RenderKit} associated with the "current" view ID or view handler.
	 * <p>
	 * The current view ID is the view ID that's set for the view root that's associated with the current faces context.
	 * Or if there is none, then the current view handler will be assumed, which is the view handler that's associated
	 * with the requested view.
	 *
	 * @return The {@link RenderKit} associated with the "current" view ID or view handler.
	 * @since 2.2
	 * @see UIViewRoot#getRenderKitId()
	 * @see ViewHandler#calculateRenderKitId(FacesContext)
	 */
	public static RenderKit getRenderKit() {
		return FacesLocal.getRenderKit(getContext());
	}

	/**
	 * Normalize the given path as a valid view ID based on the current mapping, if necessary.
	 * <ul>
	 * <li>If the current mapping is a prefix mapping and the given path starts with it, then remove it.
	 * <li>If the current mapping is a suffix mapping and the given path ends with it, then replace it with the default
	 * Facelets suffix.
	 * </ul>
	 * @param path The path to be normalized as a valid view ID based on the current mapping.
	 * @return The path as a valid view ID.
	 * @see #getMapping()
	 * @see #isPrefixMapping(String)
	 */
	public static String normalizeViewId(String path) {
		return FacesLocal.normalizeViewId(getContext(), path);
	}

	/**
	 * Returns the view parameters of the current view, or an empty collection if there is no view.
	 * @return The view parameters of the current view, or an empty collection if there is no view.
	 * @see ViewMetadata#getViewParameters(UIViewRoot)
	 */
	public static Collection<UIViewParameter> getViewParameters() {
		return FacesLocal.getViewParameters(getContext());
	}

	/**
	 * Returns the view parameters of the current view as a parameter map, or an empty map if there is no view. This is
	 * ready for usage in among others {@link ViewHandler#getBookmarkableURL(FacesContext, String, Map, boolean)}.
	 * @return The view parameters of the current view as a parameter map, or an empty map if there is no view.
	 * @see ViewMetadata#getViewParameters(UIViewRoot)
	 */
	public static Map<String, List<String>> getViewParameterMap() {
		return FacesLocal.getViewParameterMap(getContext());
	}

	/**
	 * Returns the metadata attribute map of the given view ID, or an empty map if there is no view metadata.
	 * @param viewId The view ID to return the metadata attribute map for.
	 * @return The metadata attribute map of the given view ID, or an empty map if there is no view metadata.
	 * @see ViewDeclarationLanguage#getViewMetadata(FacesContext, String)
	 * @since 1.4
	 */
	public static Map<String, Object> getMetadataAttributes(String viewId) {
		return FacesLocal.getMetadataAttributes(getContext(), viewId);
	}

	/**
	 * Returns the metadata attribute map of the current view, or an empty map if there is no view metadata.
	 * @return The metadata attribute map of the current view, or an empty map if there is no view metadata.
	 * @see UIViewRoot#getAttributes()
	 * @since 2.0
	 */
	public static Map<String, Object> getMetadataAttributes() {
		return FacesLocal.getMetadataAttributes(getContext());
	}

	/**
	 * Returns the metadata attribute of the given view ID associated with the given name.
	 * Note: this is not the same as the view scope, for that use {@link #getViewAttribute(String)}.
	 * @param <T> The expected return type.
	 * @param viewId The view ID to return the metadata attribute for.
	 * @param name The metadata attribute name.
	 * @return The metadata attribute of the given view ID associated with the given name.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see ViewDeclarationLanguage#getViewMetadata(FacesContext, String)
	 * @since 1.4
	 */
	public static <T> T getMetadataAttribute(String viewId, String name) {
		return FacesLocal.getMetadataAttribute(getContext(), viewId, name);
	}

	/**
	 * Returns the metadata attribute of the current view associated with the given name.
	 * Note: this is not the same as the view scope, for that use {@link #getViewAttribute(String)}.
	 * @param <T> The expected return type.
	 * @param name The metadata attribute name.
	 * @return The metadata attribute of the current view associated with the given name.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see UIViewRoot#getAttributes()
	 * @since 1.4
	 */
	public static <T> T getMetadataAttribute(String name) {
		return FacesLocal.getMetadataAttribute(getContext(), name);
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
		return FacesLocal.getLocale(getContext());
	}

	/**
	 * Returns the default locale, or <code>null</code> if there is none.
	 * @return The default locale, or <code>null</code> if there is none.
	 * @see Application#getDefaultLocale()
	 */
	public static Locale getDefaultLocale() {
		return FacesLocal.getDefaultLocale(getContext());
	}

	/**
	 * Returns an unordered list of all supported locales on this application, with the default locale as the first
	 * item, if any. This will return an empty list if there are no locales definied in <code>faces-config.xml</code>.
	 * @return An unordered list of all supported locales on this application, with the default locale as the first
	 * item, if any. If you need an ordered list, use {@link FacesConfigXml#getSupportedLocales()} instead.
	 * @see Application#getDefaultLocale()
	 * @see Application#getSupportedLocales()
	 */
	public static List<Locale> getSupportedLocales() {
		return FacesLocal.getSupportedLocales(getContext());
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
		FacesLocal.setLocale(getContext(), locale);
	}

	/**
	 * Returns the application message bundle as identified by <code>&lt;message-bundle&gt;</code> in
	 * <code>faces-config.xml</code>. The instance is already localized via {@link Faces#getLocale()}. If there is no
	 * <code>&lt;message-bundle&gt;</code>, then this method just returns <code>null</code>.
	 * @return The message bundle as identified by <code>&lt;message-bundle&gt;</code> in <code>faces-config.xml</code>.
	 * @throws MissingResourceException When the <code>&lt;message-bundle&gt;</code> in <code>faces-config.xml</code>
	 * does not refer an existing resource in the classpath.
	 * @see Application#getMessageBundle()
	 * @since 2.0
	 */
	public static ResourceBundle getMessageBundle() {
		return FacesLocal.getMessageBundle(getContext());
	}

	/**
	 * Returns the application resource bundle as identified by the given <code>&lt;var&gt;</code> of the
	 * <code>&lt;resource-bundle&gt;</code> in <code>faces-config.xml</code>. If there is no
	 * <code>&lt;resource-bundle&gt;</code> with the given <code>&lt;var&gt;</code>, then this method just returns
	 * <code>null</code>.
	 * @param var The value of the <code>&lt;var&gt;</code> which identifies the <code>&lt;resource-bundle&gt;</code> in
	 * <code>faces-config.xml</code>.
	 * @return The application resource bundle as identified by the given <code>&lt;var&gt;</code> of the
	 * <code>&lt;resource-bundle&gt;</code> in <code>faces-config.xml</code>.
	 * @throws MissingResourceException When the <code>&lt;resource-bundle&gt;</code> as identified by the given
	 * <code>&lt;var&gt;</code> in <code>faces-config.xml</code> does not refer an existing resource in the classpath.
	 * @see Application#getResourceBundle(FacesContext, String)
	 * @since 2.0
	 */
	public static ResourceBundle getResourceBundle(String var) {
		return FacesLocal.getResourceBundle(getContext(), var);
	}

	/**
	 * Returns all application resource bundles registered as <code>&lt;resource-bundle&gt;</code> in
	 * <code>faces-config.xml</code>. If there are no resource bundles registered, then this method just returns an
	 * empty map.
	 * @return all application resource bundles registered as <code>&lt;resource-bundle&gt;</code> in
	 * <code>faces-config.xml</code>.
	 * <code>&lt;base-name&gt;</code> of the <code>&lt;resource-bundle&gt;</code> in <code>faces-config.xml</code>.
	 * @throws MissingResourceException When the <code>&lt;resource-bundle&gt;</code> in <code>faces-config.xml</code>
	 * does not refer an existing resource in the classpath.
	 * @see Application#getResourceBundle(FacesContext, String)
	 * @since 2.1
	 */
	public static Map<String, ResourceBundle> getResourceBundles() {
		return FacesLocal.getResourceBundles(getContext());
	}

	/**
	 * Gets a string for the given key searching declared resource bundles, order by declaration in
	 * <code>faces-config.xml</code>.
	 * If the string is missing, then this method returns <code>???key???</code>.
	 * @param key The bundle key.
	 * @return a string for the given key searching declared resource bundles, order by declaration in
	 * <code>faces-config.xml</code>.
	 * @since 2.1
	 */
	public static String getBundleString(String key) {
		return FacesLocal.getBundleString(getContext(), key);
	}

	/**
	 * Perform the JSF navigation to the given outcome.
	 * @param outcome The navigation outcome.
	 * @see Application#getNavigationHandler()
	 * @see NavigationHandler#handleNavigation(FacesContext, String, String)
	 */
	public static void navigate(String outcome) {
		FacesLocal.navigate(getContext(), outcome);
	}

	/**
	 * Returns the concrete domain-relative URL to the current view with the given params URL-encoded in the query
	 * string and optionally include view parameters as well. This URL can ultimately be used as redirect URL, or in
	 * <code>&lt;form action&gt;</code>, or in <code>&lt;a href&gt;</code>. Any parameter with an empty name or value
	 * will be skipped. To skip empty view parameters as well, use <code>&lt;o:viewParam&gt;</code> instead.
	 * @param params The parameters to be URL-encoded in the query string. Can be <code>null</code>.
	 * @param includeViewParams Whether the view parameters of the current view should be included as well.
	 * @return The concrete domain-relative URL to the current view.
	 * @throws IllegalStateException When there is no view (i.e. when it is <code>null</code>). This can happen if the
	 * method is called at the wrong moment in the JSF lifecycle, e.g. before the view has been restored/created.
	 * @see ViewHandler#getBookmarkableURL(FacesContext, String, Map, boolean)
	 * @since 1.6
	 */
	public static String getBookmarkableURL(Map<String, List<String>> params, boolean includeViewParams) {
		return FacesLocal.getBookmarkableURL(getContext(), params, includeViewParams);
	}

	/**
	 * Returns the concrete domain-relative URL to the given view with the given params URL-encoded in the query
	 * string and optionally include view parameters as well. This URL can ultimately be used as redirect URL, or in
	 * <code>&lt;form action&gt;</code>, or in <code>&lt;a href&gt;</code>. Any parameter with an empty name or value
	 * will be skipped. To skip empty view parameters as well, use <code>&lt;o:viewParam&gt;</code> instead.
	 * @param viewId The view ID to create the bookmarkable URL for.
	 * @param params The parameters to be URL-encoded in the query string. Can be <code>null</code>.
	 * @param includeViewParams Whether the view parameters of the current view which are also declared in the target
	 * view should be included as well. Note thus that this does not include the view parameters which are not declared
	 * in the target view!
	 * @return The concrete domain-relative URL to the target view.
	 * @see ViewHandler#getBookmarkableURL(FacesContext, String, Map, boolean)
	 * @since 1.6
	 */
	public static String getBookmarkableURL(String viewId, Map<String, List<String>> params, boolean includeViewParams) {
		return FacesLocal.getBookmarkableURL(getContext(), viewId, params, includeViewParams);
	}

	/**
	 * Returns the concrete domain-relative URL to the current view with the given params URL-encoded in the query
	 * string and optionally include view parameters as well. This URL can ultimately be used as redirect URL, or in
	 * <code>&lt;form action&gt;</code>, or in <code>&lt;a href&gt;</code>. Any parameter with an empty name or value
	 * will be skipped. To skip empty view parameters as well, use <code>&lt;o:viewParam&gt;</code> instead.
	 * @param params The parameters to be URL-encoded in the query string. Can be <code>null</code>.
	 * @param includeViewParams Whether the view parameters of the current view should be included as well.
	 * @return The concrete domain-relative URL to the current view.
	 * @throws IllegalStateException When there is no view (i.e. when it is <code>null</code>). This can happen if the
	 * method is called at the wrong moment in the JSF lifecycle, e.g. before the view has been restored/created.
	 * @see ViewHandler#getBookmarkableURL(FacesContext, String, Map, boolean)
	 * @since 1.7
	 */
	public static String getBookmarkableURL(Collection<? extends ParamHolder> params, boolean includeViewParams) {
		return FacesLocal.getBookmarkableURL(getContext(), params, includeViewParams);
	}

	/**
	 * Returns the concrete domain-relative URL to the given view with the given params URL-encoded in the query
	 * string and optionally include view parameters as well. This URL can ultimately be used as redirect URL, or in
	 * <code>&lt;form action&gt;</code>, or in <code>&lt;a href&gt;</code>. Any parameter with an empty name or value
	 * will be skipped. To skip empty view parameters as well, use <code>&lt;o:viewParam&gt;</code> instead.
	 * @param viewId The view ID to create the bookmarkable URL for.
	 * @param params The parameters to be URL-encoded in the query string. Can be <code>null</code>.
	 * @param includeViewParams Whether the view parameters of the current view which are also declared in the target
	 * view should be included as well. Note thus that this does not include the view parameters which are not declared
	 * in the target view!
	 * @return The concrete domain-relative URL to the target view.
	 * @see ViewHandler#getBookmarkableURL(FacesContext, String, Map, boolean)
	 * @since 1.7
	 */
	public static String getBookmarkableURL(String viewId, Collection<? extends ParamHolder> params, boolean includeViewParams) {
		return FacesLocal.getBookmarkableURL(getContext(), viewId, params, includeViewParams);
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
		return FacesLocal.getFaceletContext(getContext());
	}

	/**
	 * Returns the Facelet attribute value associated with the given name. This basically returns the value of the
	 * <code>&lt;ui:param&gt;</code> which is been declared inside the Facelet file, or is been passed into the Facelet
	 * file by e.g. an <code>&lt;ui:include&gt;</code>.
	 * @param <T> The expected return type.
	 * @param name The Facelet attribute name.
	 * @return The Facelet attribute value associated with the given name.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see FaceletContext#getAttribute(String)
	 * @since 1.1
	 */
	public static <T> T getFaceletAttribute(String name) {
		return FacesLocal.getFaceletAttribute(getContext(), name);
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
		FacesLocal.setFaceletAttribute(getContext(), name, value);
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
		return FacesLocal.getRequest(getContext());
	}

	/**
	 * Returns whether the current request is an ajax request.
	 * @return <code>true</code> for an ajax request, <code>false</code> for a non-ajax (synchronous) request.
	 * @see PartialViewContext#isAjaxRequest()
	 */
	public static boolean isAjaxRequest() {
		return FacesLocal.isAjaxRequest(getContext());
	}

	/**
	 * Returns whether the current request is an ajax request with partial rendering. That is, when it's an ajax request
	 * without <code>render="@all"</code>.
	 * @return <code>true</code> for an ajax request with partial rendering, <code>false</code> an ajax request with
	 * <code>render="@all"</code> or a non-ajax (synchronous) request.
	 * @see PartialViewContext#isAjaxRequest()
	 * @see PartialViewContext#isRenderAll()
	 * @since 2.3
	 */
	public static boolean isAjaxRequestWithPartialRendering() {
		return FacesLocal.isAjaxRequestWithPartialRendering(getContext());
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
	 * Returns the HTTP request parameter map.
	 * @return The HTTP request parameter map.
	 * @see ExternalContext#getRequestParameterMap()
	 */
	public static Map<String, String> getRequestParameterMap() {
		return FacesLocal.getRequestParameterMap(getContext());
	}

	/**
	 * Returns the HTTP request parameter value associated with the given name.
	 * @param name The HTTP request parameter name.
	 * @return The HTTP request parameter value associated with the given name.
	 * @see ExternalContext#getRequestParameterMap()
	 */
	public static String getRequestParameter(String name) {
		return FacesLocal.getRequestParameterMap(getContext()).get(name);
	}

	/**
	 * Returns the HTTP request parameter values map.
	 * @return The HTTP request parameter values map.
	 * @see ExternalContext#getRequestParameterValuesMap()
	 */
	public static Map<String, String[]> getRequestParameterValuesMap() {
		return FacesLocal.getRequestParameterValuesMap(getContext());
	}

	/**
	 * Returns the HTTP request parameter values associated with the given name.
	 * @param name The HTTP request parameter name.
	 * @return The HTTP request parameter values associated with the given name.
	 * @see ExternalContext#getRequestParameterValuesMap()
	 */
	public static String[] getRequestParameterValues(String name) {
		return FacesLocal.getRequestParameterValuesMap(getContext()).get(name);
	}

	/**
	 * Returns the HTTP request header map.
	 * @return The HTTP request header map.
	 * @see ExternalContext#getRequestHeaderMap()
	 */
	public static Map<String, String> getRequestHeaderMap() {
		return FacesLocal.getRequestHeaderMap(getContext());
	}

	/**
	 * Returns the HTTP request header value associated with the given name.
	 * @param name The HTTP request header name.
	 * @return The HTTP request header value associated with the given name.
	 * @see ExternalContext#getRequestHeaderMap()
	 */
	public static String getRequestHeader(String name) {
		return FacesLocal.getRequestHeaderMap(getContext()).get(name);
	}

	/**
	 * Returns the HTTP request header values map.
	 * @return The HTTP request header values map.
	 * @see ExternalContext#getRequestHeaderValuesMap()
	 */
	public static Map<String, String[]> getRequestHeaderValuesMap() {
		return FacesLocal.getRequestHeaderValuesMap(getContext());
	}

	/**
	 * Returns the HTTP request header values associated with the given name.
	 * @param name The HTTP request header name.
	 * @return The HTTP request header values associated with the given name.
	 * @see ExternalContext#getRequestHeaderValuesMap()
	 */
	public static String[] getRequestHeaderValues(String name) {
		return FacesLocal.getRequestHeaderValuesMap(getContext()).get(name);
	}

	/**
	 * Returns the HTTP request context path. It's the webapp context name, with a leading slash. If the webapp runs
	 * on context root, then it returns an empty string.
	 * @return The HTTP request context path.
	 * @see ExternalContext#getRequestContextPath()
	 */
	public static String getRequestContextPath() {
		return FacesLocal.getRequestContextPath(getContext());
	}

	/**
	 * Returns the HTTP request servlet path. If JSF is prefix mapped (e.g. <code>/faces/*</code>), then this returns
	 * the whole prefix mapping (e.g. <code>/faces</code>). If JSF is suffix mapped (e.g. <code>*.xhtml</code>), then
	 * this returns the whole part after the context path, with a leading slash.
	 * @return The HTTP request servlet path.
	 * @see ExternalContext#getRequestServletPath()
	 */
	public static String getRequestServletPath() {
		return FacesLocal.getRequestServletPath(getContext());
	}

	/**
	 * Returns the HTTP request path info. If JSF is prefix mapped (e.g. <code>/faces/*</code>), then this returns the
	 * whole part after the prefix mapping, with a leading slash. If JSF is suffix mapped (e.g. <code>*.xhtml</code>),
	 * then this returns <code>null</code>.
	 * @return The HTTP request path info.
	 * @see ExternalContext#getRequestPathInfo()
	 */
	public static String getRequestPathInfo() {
		return FacesLocal.getRequestPathInfo(getContext());
	}

	/**
	 * Returns the HTTP request hostname. This is the entire domain, without any scheme and slashes. Noted should be
	 * that this value is extracted from the request URL, not from {@link HttpServletRequest#getServerName()} as its
	 * outcome can be influenced by proxies.
	 * @return The HTTP request hostname.
	 * @throws IllegalArgumentException When the URL is malformed. This is however unexpected as the request would
	 * otherwise not have hit the server at all.
	 * @see HttpServletRequest#getRequestURL()
	 * @since 1.6
	 */
	public static String getRequestHostname() {
		return FacesLocal.getRequestHostname(getContext());
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
		return FacesLocal.getRequestBaseURL(getContext());
	}

	/**
	 * Returns the HTTP request domain URL. This is the URL with the scheme and domain, without any trailing slash.
	 * @return The HTTP request domain URL.
	 * @see HttpServletRequest#getRequestURL()
	 * @see HttpServletRequest#getRequestURI()
	 * @since 1.1
	 */
	public static String getRequestDomainURL() {
		return FacesLocal.getRequestDomainURL(getContext());
	}

	/**
	 * Returns the HTTP request URL. This is the full request URL as the enduser sees in browser address bar. This does
	 * not include the request query string.
	 * @return The HTTP request URL.
	 * @see HttpServletRequest#getRequestURL()
	 * @since 1.1
	 */
	public static String getRequestURL() {
		return FacesLocal.getRequestURL(getContext());
	}

	/**
	 * Returns the HTTP request URI. This is the part after the domain in the request URL, including the leading slash.
	 * This does not include the request query string.
	 * @return The HTTP request URI.
	 * @see HttpServletRequest#getRequestURI()
	 * @since 1.1
	 */
	public static String getRequestURI() {
		return FacesLocal.getRequestURI(getContext());
	}

	/**
	 * Returns the HTTP request query string. This is the part after the <code>?</code> in the request URL as the
	 * enduser sees in browser address bar.
	 * @return The HTTP request query string.
	 * @see HttpServletRequest#getQueryString()
	 * @since 1.1
	 */
	public static String getRequestQueryString() {
		return FacesLocal.getRequestQueryString(getContext());
	}

	/**
	 * Returns the HTTP request query string as parameter values map. Note this method returns <strong>only</strong>
	 * the request URL (GET) parameters, as opposed to {@link #getRequestParameterValuesMap()}, which contains both
	 * the request URL (GET) parameters and and the request body (POST) parameters. This is ready for usage in among
	 * others {@link ViewHandler#getBookmarkableURL(FacesContext, String, Map, boolean)}.
	 * @return The HTTP request query string as parameter values map.
	 * @see HttpServletRequest#getQueryString()
	 * @since 1.6
	 */
	public static Map<String, List<String>> getRequestQueryStringMap() {
		return FacesLocal.getRequestQueryStringMap(getContext());
	}

	/**
	 * Returns the HTTP request URL with query string. This is the full request URL with query string as the enduser
	 * sees in browser address bar.
	 * @return The HTTP request URL with query string.
	 * @see HttpServletRequest#getRequestURL()
	 * @see HttpServletRequest#getQueryString()
	 * @since 1.5
	 */
	public static String getRequestURLWithQueryString() {
		return FacesLocal.getRequestURLWithQueryString(getContext());
	}

	/**
	 * Returns the HTTP request URI with query string. This is the part after the domain in the request URL, including
	 * the leading slash and the request query string.
	 * @return The HTTP request URI with query string.
	 * @see HttpServletRequest#getRequestURI()
	 * @see HttpServletRequest#getQueryString()
	 * @since 1.6
	 */
	public static String getRequestURIWithQueryString() {
		return FacesLocal.getRequestURIWithQueryString(getContext());
	}

	/**
	 * Returns the original HTTP request URI behind this forwarded request, if any.
	 * This does not include the request query string.
	 * @return The original HTTP request URI behind this forwarded request, if any.
	 * @since 1.8
	 * @deprecated Since 2.4. This is abstracted away by {@link #getRequestURI()}. Use it instead.
	 * JSF has as to retrieving request URI no business of knowing if the request is forwarded/rewritten or not.
	 */
	@Deprecated // TODO: Remove in OmniFaces 3.0.
	public static String getForwardRequestURI() {
		return FacesLocal.getForwardRequestURI(getContext());
	}

	/**
	 * Returns the original HTTP request query string behind this forwarded request, if any.
	 * @return The original HTTP request query string behind this forwarded request, if any.
	 * @since 1.8
	 * @deprecated Since 2.4. This is abstracted away by {@link #getRequestQueryString()}. Use it instead.
	 * JSF has as to retrieving request URI no business of knowing if the request is forwarded/rewritten or not.
	 */
	@Deprecated // TODO: Remove in OmniFaces 3.0.
	public static String getForwardRequestQueryString() {
		return FacesLocal.getForwardRequestQueryString(getContext());
	}

	/**
	 * Returns the original HTTP request URI with query string behind this forwarded request, if any.
	 * @return The original HTTP request URI with query string behind this forwarded request, if any.
	 * @since 1.8
	 * @deprecated Since 2.4. This is abstracted away by {@link #getRequestURIWithQueryString()}. Use it instead.
	 * JSF has as to retrieving request URI no business of knowing if the request is forwarded/rewritten or not.
	 */
	@Deprecated // TODO: Remove in OmniFaces 3.0.
	public static String getForwardRequestURIWithQueryString() {
		return FacesLocal.getForwardRequestURIWithQueryString(getContext());
	}

	/**
	 * Returns the Internet Protocol (IP) address of the client that sent the request. This will first check the
	 * <code>X-Forwarded-For</code> request header and if it's present, then return its first IP address, else just
	 * return {@link HttpServletRequest#getRemoteAddr()} unmodified.
	 * @return The IP address of the client.
	 * @see HttpServletRequest#getRemoteAddr()
	 * @since 1.2
	 */
	public static String getRemoteAddr() {
		return FacesLocal.getRemoteAddr(getContext());
	}

	// HTTP response --------------------------------------------------------------------------------------------------

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
		return FacesLocal.getResponse(getContext());
	}

	/**
	 * Returns the HTTP response buffer size. If Facelets is used and the <code>javax.faces.FACELETS_BUFFER_SIZE</code>
	 * context parameter is been set, then it's the context parameter value which will be returned. Otherwise it
	 * returns the implementation independent default value, which is 1024 in Mojarra.
	 * @return The HTTP response buffer size.
	 * @see ExternalContext#getResponseBufferSize()
	 * @since 1.2
	 */
	public static int getResponseBufferSize() {
		return FacesLocal.getResponseBufferSize(getContext());
	}

	/**
	 * Returns the HTTP response character encoding.
	 * @return The HTTP response character encoding.
	 * @see ExternalContext#getResponseCharacterEncoding()
	 * @since 1.2
	 */
	public static String getResponseCharacterEncoding() {
		return FacesLocal.getResponseCharacterEncoding(getContext());
	}

	/**
	 * Sets the HTTP response status code. You can use the constant field values of {@link HttpServletResponse} for
	 * this. For example, <code>Faces.setResponseStatus(HttpServletResponse.SC_BAD_REQUEST)</code>.
	 * @param status The HTTP status code to be set on the current response.
	 * @since 1.6
	 */
	public static void setResponseStatus(int status) {
		FacesLocal.setResponseStatus(getContext(), status);
	}

	/**
	 * Sends a temporary (302) redirect to the given URL. If the given URL does <b>not</b> start with <code>http://</code>,
	 * <code>https://</code> or <code>/</code>, then the request context path will be prepended, otherwise it will be
	 * the unmodified redirect URL. So, when redirecting to another page in the same web application, always specify the
	 * full path from the context root on (which in turn does not need to start with <code>/</code>).
	 * <pre>
	 * Faces.redirect("other.xhtml");
	 * </pre>
	 * <p>
	 * You can use {@link String#format(String, Object...)} placeholder <code>%s</code> in the redirect URL to represent
	 * placeholders for any request parameter values which needs to be URL-encoded. Here's a concrete example:
	 * <pre>
	 * Faces.redirect("other.xhtml?foo=%s&amp;bar=%s", foo, bar);
	 * </pre>
	 * @param url The URL to redirect the current response to.
	 * @param paramValues The request parameter values which you'd like to put URL-encoded in the given URL.
	 * @throws IOException Whenever something fails at I/O level. The caller should preferably not catch it, but just
	 * redeclare it in the action method. The servletcontainer will handle it.
	 * @throws NullPointerException When url is <code>null</code>.
	 * @see ExternalContext#redirect(String)
	 */
	public static void redirect(String url, String... paramValues) throws IOException {
		FacesLocal.redirect(getContext(), url, paramValues);
	}

	/**
	 * Sends a permanent (301) redirect to the given URL. If the given URL does <b>not</b> start with <code>http://</code>,
	 * <code>https://</code> or <code>/</code>, then the request context path will be prepended, otherwise it will be
	 * the unmodified redirect URL. So, when redirecting to another page in the same web application, always specify the
	 * full path from the context root on (which in turn does not need to start with <code>/</code>).
	 * <pre>
	 * Faces.redirectPermanent("other.xhtml");
	 * </pre>
	 * <p>
	 * You can use {@link String#format(String, Object...)} placeholder <code>%s</code> in the redirect URL to represent
	 * placeholders for any request parameter values which needs to be URL-encoded. Here's a concrete example:
	 * <pre>
	 * Faces.redirectPermanent("other.xhtml?foo=%s&amp;bar=%s", foo, bar);
	 * </pre>
	 * <p>
	 * This method does by design not work on ajax requests. It is not possible to return a "permanent redirect" via
	 * JSF ajax XML response.
	 * @param url The URL to redirect the current response to.
	 * @param paramValues The request parameter values which you'd like to put URL-encoded in the given URL.
	 * @throws NullPointerException When url is <code>null</code>.
	 * @see ExternalContext#setResponseStatus(int)
	 * @see ExternalContext#setResponseHeader(String, String)
	 */
	public static void redirectPermanent(String url, String... paramValues) {
		FacesLocal.redirectPermanent(getContext(), url, paramValues);
	}

	/**
	 * Refresh the current page by a GET request. This basically sends a temporary (302) redirect to current request
	 * URI, without query string.
	 * @throws IOException Whenever something fails at I/O level. The caller should preferably not catch it, but just
	 * redeclare it in the action method. The servletcontainer will handle it.
	 * @see ExternalContext#redirect(String)
	 * @see HttpServletRequest#getRequestURI()
	 * @since 2.2
	 */
	public static void refresh() throws IOException {
		FacesLocal.refresh(getContext());
	}

	/**
	 * Refresh the current page by a GET request, maintaining the query string. This basically sends a temporary (302)
	 * redirect to current request URI, with the current query string.
	 * @throws IOException Whenever something fails at I/O level. The caller should preferably not catch it, but just
	 * redeclare it in the action method. The servletcontainer will handle it.
	 * @see ExternalContext#redirect(String)
	 * @see HttpServletRequest#getRequestURI()
	 * @see HttpServletRequest#getQueryString()
	 * @since 2.2
	 */
	public static void refreshWithQueryString() throws IOException {
		FacesLocal.refreshWithQueryString(getContext());
	}

	/**
	 * Sends a HTTP response error with the given status and message. This will end up in either a custom
	 * <code>&lt;error-page&gt;</code> whose <code>&lt;error-code&gt;</code> matches the given status, or in a servlet
	 * container specific default error page if there is none. The message will be available in the error page as a
	 * request attribute with name <code>javax.servlet.error.message</code>. The {@link FacesContext#responseComplete()}
	 * will implicitly be called after sending the error.
	 * @param status The HTTP response status which is supposed to be in the range 4nn-5nn. You can use the constant
	 * field values of {@link HttpServletResponse} for this.
	 * @param message The message which is supposed to be available in the error page.
	 * @throws IOException Whenever something fails at I/O level. The caller should preferably not catch it, but just
	 * redeclare it in the action method. The servletcontainer will handle it.
	 * @see ExternalContext#responseSendError(int, String)
	 */
	public static void responseSendError(int status, String message) throws IOException {
		FacesLocal.responseSendError(getContext(), status, message);
	}

	/**
	 * Add a header with given name and value to the HTTP response.
	 * @param name The header name.
	 * @param value The header value.
	 * @see ExternalContext#addResponseHeader(String, String)
	 */
	public static void addResponseHeader(String name, String value) {
		FacesLocal.addResponseHeader(getContext(), name, value);
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
		return FacesLocal.isResponseCommitted(getContext());
	}

	/**
	 * Resets the current response. This will clear any headers which are been set and any data which is written to
	 * the response buffer which isn't committed yet.
	 * @throws IllegalStateException When the response is already committed.
	 * @see ExternalContext#responseReset()
	 * @since 1.1
	 */
	public static void responseReset() {
		FacesLocal.responseReset(getContext());
	}

	/**
	 * Signals JSF that, as soon as the current phase of the lifecycle has been completed, control should be passed to
	 * the Render Response phase, bypassing any phases that have not been executed yet.
	 * @see FacesContext#renderResponse()
	 * @since 1.4
	 */
	public static void renderResponse() {
		getContext().renderResponse();
	}

	/**
	 * Returns <code>true</code> if we're currently in the render response phase. This explicitly checks the current
	 * phase ID instead of {@link FacesContext#getRenderResponse()} as the latter may unexpectedly return false during
	 * a GET request when <code>&lt;f:viewParam&gt;</code> is been used.
	 * @return <code>true</code> if we're currently in the render response phase.
	 * @see FacesContext#getCurrentPhaseId()
	 * @since 1.4
	 */
	public static boolean isRenderResponse() {
		return FacesLocal.isRenderResponse(getContext());
	}

	/**
	 * Signals JSF that the response for this request has already been generated (such as providing a file download),
	 * and that the lifecycle should be terminated as soon as the current phase is completed.
	 * @see FacesContext#responseComplete()
	 * @since 1.4
	 */
	public static void responseComplete() {
		getContext().responseComplete();
	}

	/**
	 * Returns <code>true</code> if the {@link FacesContext#responseComplete()} has been called.
	 * @return <code>true</code> if the {@link FacesContext#responseComplete()} has been called.
	 * @see FacesContext#responseComplete()
	 * @since 1.4
	 */
	public static boolean isResponseComplete() {
		return getContext().getResponseComplete();
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
		FacesLocal.login(getContext(), username, password);
	}

	/**
	 * Trigger the default container managed authentication mechanism on the current request. It expects the username
	 * and password being available as predefinied request parameters on the current request and/or a custom JASPIC
	 * implementation.
	 * @return <code>true</code> if the authentication was successful, otherwise <code>false</code>.
	 * @throws ServletException When the authentication has failed. The caller is responsible for handling it.
	 * @throws IOException Whenever something fails at I/O level. The caller should preferably not catch it, but just
	 * redeclare it in the action method. The servletcontainer will handle it.
	 * @see HttpServletRequest#authenticate(HttpServletResponse)
	 * @since 1.4
	 */
	public static boolean authenticate() throws ServletException, IOException {
		return FacesLocal.authenticate(getContext());
	}

	/**
	 * Perform programmatic logout for container managed FORM based authentication. Note that this basically removes
	 * the user principal from the session. It's however better practice to just invalidate the session altogether,
	 * which will implicitly also remove the user principal. Just invoke {@link #invalidateSession()} instead. Note
	 * that the user principal is still present in the response of the current request, it's therefore recommend to
	 * send a redirect after {@link #logout()} or {@link #invalidateSession()}. You can use
	 * {@link #redirect(String, String...)} for this.
	 * @throws ServletException When the logout has failed.
	 * @see HttpServletRequest#logout()
	 */
	public static void logout() throws ServletException {
		FacesLocal.logout(getContext());
	}

	/**
	 * Returns the name of the logged-in user for container managed FORM based authentication, if any.
	 * @return The name of the logged-in user for container managed FORM based authentication, if any.
	 * @see ExternalContext#getRemoteUser()
	 */
	public static String getRemoteUser() {
		return FacesLocal.getRemoteUser(getContext());
	}

	/**
	 * Returns whether the currently logged-in user has the given role.
	 * @param role The role to be checked on the currently logged-in user.
	 * @return <code>true</code> if the currently logged-in user has the given role, otherwise <code>false</code>.
	 * @see ExternalContext#isUserInRole(String)
	 */
	public static boolean isUserInRole(String role) {
		return FacesLocal.isUserInRole(getContext(), role);
	}

	// HTTP cookies ---------------------------------------------------------------------------------------------------

	/**
	 * Returns the value of the HTTP request cookie associated with the given name. The value is implicitly URL-decoded
	 * with a charset of UTF-8.
	 * @param name The HTTP request cookie name.
	 * @return The value of the HTTP request cookie associated with the given name.
	 * @throws UnsupportedOperationException When this platform does not support UTF-8.
	 * @see ExternalContext#getRequestCookieMap()
	 */
	public static String getRequestCookie(String name) {
		return FacesLocal.getRequestCookie(getContext(), name);
	}

	/**
	 * Add a cookie with given name, value and maxage to the HTTP response. The cookie value will implicitly be
	 * URL-encoded with UTF-8 so that any special characters can be stored in the cookie. The cookie will implicitly
	 * be set to secure when the current request is secure (i.e. when the current request is a HTTPS request). The
	 * cookie will implicitly be set in the domain and path of the current request URL.
	 * @param name The cookie name.
	 * @param value The cookie value.
	 * @param maxAge The maximum age of the cookie, in seconds. If this is <code>0</code>, then the cookie will be
	 * removed. Note that the name and path must be exactly the same as it was when the cookie was created. If this is
	 * <code>-1</code> then the cookie will become a session cookie and thus live as long as the established HTTP
	 * session.
	 * @throws UnsupportedOperationException When this platform does not support UTF-8.
	 * @see ExternalContext#addResponseCookie(String, String, Map)
	 * @since 1.8
	 */
	public static void addResponseCookie(String name, String value, int maxAge) {
		FacesLocal.addResponseCookie(getContext(), name, value, maxAge);
	}

	/**
	 * Add a cookie with given name, value, path and maxage to the HTTP response. The cookie value will implicitly be
	 * URL-encoded with UTF-8 so that any special characters can be stored in the cookie. The cookie will implicitly
	 * be set to secure when the current request is secure (i.e. when the current request is a HTTPS request). The
	 * cookie will implicitly be set in the domain of the current request URL.
	 * @param name The cookie name.
	 * @param value The cookie value.
	 * @param path The cookie path. If this is <code>/</code>, then the cookie is available in all pages of the webapp.
	 * If this is <code>/somespecificpath</code>, then the cookie is only available in pages under the specified path.
	 * @param maxAge The maximum age of the cookie, in seconds. If this is <code>0</code>, then the cookie will be
	 * removed. Note that the name and path must be exactly the same as it was when the cookie was created. If this is
	 * <code>-1</code> then the cookie will become a session cookie and thus live as long as the established HTTP
	 * session.
	 * @throws UnsupportedOperationException When this platform does not support UTF-8.
	 * @see ExternalContext#addResponseCookie(String, String, Map)
	 */
	public static void addResponseCookie(String name, String value, String path, int maxAge) {
		FacesLocal.addResponseCookie(getContext(), name, value, path, maxAge);
	}

	/**
	 * Add a cookie with given name, value, domain, path and maxage to the HTTP response. The cookie value will
	 * implicitly be URL-encoded with UTF-8 so that any special characters can be stored in the cookie. The cookie will
	 * implicitly be set to secure when the current request is secure (i.e. when the current request is a HTTPS request).
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
	 * @see ExternalContext#addResponseCookie(String, String, Map)
	 * @since 1.8
	 */
	public static void addResponseCookie(String name, String value, String domain, String path, int maxAge) {
		FacesLocal.addResponseCookie(getContext(), name, value, domain, path, maxAge);
	}

	/**
	 * Remove the cookie with given name and path from the HTTP response. Note that the name and path must be exactly
	 * the same as it was when the cookie was created.
	 * @param name The cookie name.
	 * @param path The cookie path.
	 * @see ExternalContext#addResponseCookie(String, String, Map)
	 */
	public static void removeResponseCookie(String name, String path) {
		FacesLocal.removeResponseCookie(getContext(), name, path);
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
		return FacesLocal.getSession(getContext());
	}

	/**
	 * Returns the HTTP session and creates one if one doesn't exist and <code>create</code> argument is
	 * <code>true</code>, otherwise don't create one and return <code>null</code>.
	 * <p>
	 * <i>Note that whenever you absolutely need this method to perform a general task, you might want to consider to
	 * submit a feature request to OmniFaces in order to add a new utility method which performs exactly this general
	 * task.</i>
	 * @param create Whether or not to create a new session if one doesn't exist.
	 * @return The HTTP session.
	 * @see ExternalContext#getSession(boolean)
	 */
	public static HttpSession getSession(boolean create) {
		return FacesLocal.getSession(getContext(), create);
	}

	/**
	 * Returns a string containing the unique identifier assigned to this session. The identifier is assigned by the
	 * servlet container and is implementation dependent.
	 * @return The HTTP session ID.
	 * @see HttpSession#getId()
	 * @since 1.2
	 */
	public static String getSessionId() {
		return FacesLocal. getSessionId(getContext());
	}

	/**
	 * Invalidates the current HTTP session. So, any subsequent HTTP request will get a new one when necessary.
	 * @see ExternalContext#invalidateSession()
	 */
	public static void invalidateSession() {
		FacesLocal.invalidateSession(getContext());
	}

	/**
	 * Returns whether the HTTP session has already been created.
	 * @return <code>true</code> if the HTTP session has already been created, otherwise <code>false</code>.
	 * @see ExternalContext#getSession(boolean)
	 * @since 1.1
	 */
	public static boolean hasSession() {
		return FacesLocal.hasSession(getContext());
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
		return FacesLocal.isSessionNew(getContext());
	}

	/**
	 * Returns the time when the HTTP session was created, measured in epoch time. This implicitly creates the session
	 * if one doesn't exist.
	 * @return The time when the HTTP session was created.
	 * @see HttpSession#getCreationTime()
	 * @since 1.1
	 */
	public static long getSessionCreationTime() {
		return FacesLocal.getSessionCreationTime(getContext());
	}

	/**
	 * Returns the time of the previous request associated with the current HTTP session, measured in epoch time. This
	 * implicitly creates the session if one doesn't exist.
	 * @return The time of the previous request associated with the current HTTP session.
	 * @see HttpSession#getLastAccessedTime()
	 * @since 1.1
	 */
	public static long getSessionLastAccessedTime() {
		return FacesLocal.getSessionLastAccessedTime(getContext());
	}

	/**
	 * Returns the HTTP session timeout in seconds. This implicitly creates the session if one doesn't exist.
	 * @return The HTTP session timeout in seconds.
	 * @see HttpSession#getMaxInactiveInterval()
	 * @since 1.1
	 */
	public static int getSessionMaxInactiveInterval() {
		// Note that JSF 2.1 has this method on ExternalContext. We don't use it in order to be JSF 2.0 compatible.
		return FacesLocal.getSessionMaxInactiveInterval(getContext());
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
		FacesLocal.setSessionMaxInactiveInterval(getContext(), seconds);
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
		return FacesLocal.hasSessionTimedOut(getContext());
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
		return FacesLocal.getServletContext(getContext());
	}

	/**
	 * Returns the application initialization parameter map. This returns the parameter name-value pairs of all
	 * <code>&lt;context-param&gt;</code> entries in in <code>web.xml</code>.
	 * @return The application initialization parameter map.
	 * @see ExternalContext#getInitParameterMap()
	 * @since 1.1
	 */
	public static Map<String, String> getInitParameterMap() {
		return FacesLocal.getInitParameterMap(getContext());
	}

	/**
	 * Returns the application initialization parameter. This returns the <code>&lt;param-value&gt;</code> of a
	 * <code>&lt;context-param&gt;</code> in <code>web.xml</code> associated with the given
	 * <code>&lt;param-name&gt;</code>.
	 * @param name The application initialization parameter name.
	 * @return The application initialization parameter value associated with the given name, or <code>null</code> if
	 * there is none.
	 * @see ExternalContext#getInitParameter(String)
	 * @since 1.1
	 */
	public static String getInitParameter(String name) {
		return FacesLocal.getInitParameter(getContext(), name);
	}

	/**
	 * Returns the mime type for the given file name. The mime type is determined based on file extension and
	 * configureable by <code>&lt;mime-mapping&gt;</code> entries in <code>web.xml</code>. When the mime type is
	 * unknown, then a default of <code>application/octet-stream</code> will be returned.
	 * @param name The file name to return the mime type for.
	 * @return The mime type for the given file name.
	 * @see ExternalContext#getMimeType(String)
	 */
	public static String getMimeType(String name) {
		return FacesLocal.getMimeType(getContext(), name);
	}

	/**
	 * Returns a URL for an application resource mapped to the specified path, if it exists; otherwise, return
	 * <code>null</code>.
	 * @param path The application resource path to return an input stream for.
	 * @return An input stream for an application resource mapped to the specified path.
	 * @throws MalformedURLException When the specified path is not in correct URL form.
	 * @see ExternalContext#getResource(String)
	 * @since 1.2
	 */
	public static URL getResource(String path) throws MalformedURLException {
		return FacesLocal.getResource(getContext(), path);
	}

	/**
	 * Returns an input stream for an application resource mapped to the specified path, if it exists; otherwise,
	 * return <code>null</code>.
	 * @param path The application resource path to return an input stream for.
	 * @return An input stream for an application resource mapped to the specified path.
	 * @see ExternalContext#getResourceAsStream(String)
	 */
	public static InputStream getResourceAsStream(String path) {
		return FacesLocal.getResourceAsStream(getContext(), path);
	}

	/**
	 * Returns a set of available application resource paths matching the specified path.
	 * @param path The partial application resource path used to return matching resource paths.
	 * @return A set of available application resource paths matching the specified path.
	 * @see ExternalContext#getResourcePaths(String)
	 */
	public static Set<String> getResourcePaths(String path) {
		return FacesLocal.getResourcePaths(getContext(), path);
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
		return FacesLocal.getRealPath(getContext(), webContentPath);
	}

	// Request scope --------------------------------------------------------------------------------------------------

	/**
	 * Returns the request scope map.
	 * @return The request scope map.
	 * @see ExternalContext#getRequestMap()
	 */
	public static Map<String, Object> getRequestMap() {
		return FacesLocal.getRequestMap(getContext());
	}

	/**
	 * Returns the request scope attribute value associated with the given name.
	 * @param <T> The expected return type.
	 * @param name The request scope attribute name.
	 * @return The request scope attribute value associated with the given name.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see ExternalContext#getRequestMap()
	 */
	public static <T> T getRequestAttribute(String name) {
		return FacesLocal.getRequestAttribute(getContext(), name);
	}

	/**
	 * Sets the request scope attribute value associated with the given name.
	 * @param name The request scope attribute name.
	 * @param value The request scope attribute value.
	 * @see ExternalContext#getRequestMap()
	 */
	public static void setRequestAttribute(String name, Object value) {
		FacesLocal.setRequestAttribute(getContext(), name, value);
	}

	/**
	 * Removes the request scope attribute value associated with the given name.
	 * @param <T> The expected return type.
	 * @param name The request scope attribute name.
	 * @return The request scope attribute value previously associated with the given name, or <code>null</code> if
	 * there is no such attribute.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see ExternalContext#getRequestMap()
	 * @since 1.1
	 */
	public static <T> T removeRequestAttribute(String name) {
		return FacesLocal.removeRequestAttribute(getContext(), name);
	}

	// Flash scope ----------------------------------------------------------------------------------------------------

	/**
	 * Returns the flash scope. Note that <code>Flash</code> implements <code>Map&lt;String, Object&gt;</code>, so you
	 * can just treat it like a <code>Map&lt;String, Object&gt;</code>.
	 * @return The flash scope.
	 * @see ExternalContext#getFlash()
	 */
	public static Flash getFlash() {
		return FacesLocal.getFlash(getContext());
	}

	/**
	 * Returns the flash scope attribute value associated with the given name.
	 * @param <T> The expected return type.
	 * @param name The flash scope attribute name.
	 * @return The flash scope attribute value associated with the given name.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see ExternalContext#getFlash()
	 */
	public static <T> T getFlashAttribute(String name) {
		return FacesLocal.getFlashAttribute(getContext(), name);
	}

	/**
	 * Sets the flash scope attribute value associated with the given name.
	 * @param name The flash scope attribute name.
	 * @param value The flash scope attribute value.
	 * @see ExternalContext#getFlash()
	 */
	public static void setFlashAttribute(String name, Object value) {
		FacesLocal.setFlashAttribute(getContext(), name, value);
	}

	/**
	 * Removes the flash scope attribute value associated with the given name.
	 * @param <T> The expected return type.
	 * @param name The flash scope attribute name.
	 * @return The flash scope attribute value previously associated with the given name, or <code>null</code> if
	 * there is no such attribute.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see ExternalContext#getFlash()
	 * @since 1.1
	 */
	public static <T> T removeFlashAttribute(String name) {
		return FacesLocal.removeFlashAttribute(getContext(), name);
	}

	// View scope -----------------------------------------------------------------------------------------------------

	/**
	 * Returns the view scope map.
	 * @return The view scope map.
	 * @see UIViewRoot#getViewMap()
	 */
	public static Map<String, Object> getViewMap() {
		return FacesLocal.getViewMap(getContext());
	}

	/**
	 * Returns the view scope attribute value associated with the given name.
	 * @param <T> The expected return type.
	 * @param name The view scope attribute name.
	 * @return The view scope attribute value associated with the given name.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see UIViewRoot#getViewMap()
	 */
	public static <T> T getViewAttribute(String name) {
		return FacesLocal.getViewAttribute(getContext(), name);
	}

	/**
	 * Sets the view scope attribute value associated with the given name.
	 * @param name The view scope attribute name.
	 * @param value The view scope attribute value.
	 * @see UIViewRoot#getViewMap()
	 */
	public static void setViewAttribute(String name, Object value) {
		FacesLocal.setViewAttribute(getContext(), name, value);
	}

	/**
	 * Removes the view scope attribute value associated with the given name.
	 * @param <T> The expected return type.
	 * @param name The view scope attribute name.
	 * @return The view scope attribute value previously associated with the given name, or <code>null</code> if
	 * there is no such attribute.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see UIViewRoot#getViewMap()
	 * @since 1.1
	 */
	public static <T> T removeViewAttribute(String name) {
		return FacesLocal.removeViewAttribute(getContext(), name);
	}

	// Session scope --------------------------------------------------------------------------------------------------

	/**
	 * Returns the session scope map.
	 * @return The session scope map.
	 * @see ExternalContext#getSessionMap()
	 */
	public static Map<String, Object> getSessionMap() {
		return FacesLocal.getSessionMap(getContext());
	}

	/**
	 * Returns the session scope attribute value associated with the given name.
	 * @param <T> The expected return type.
	 * @param name The session scope attribute name.
	 * @return The session scope attribute value associated with the given name.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see ExternalContext#getSessionMap()
	 */
	public static <T> T getSessionAttribute(String name) {
		return FacesLocal.getSessionAttribute(getContext(), name);
	}

	/**
	 * Sets the session scope attribute value associated with the given name.
	 * @param name The session scope attribute name.
	 * @param value The session scope attribute value.
	 * @see ExternalContext#getSessionMap()
	 */
	public static void setSessionAttribute(String name, Object value) {
		FacesLocal.setSessionAttribute(getContext(), name, value);
	}

	/**
	 * Removes the session scope attribute value associated with the given name.
	 * @param <T> The expected return type.
	 * @param name The session scope attribute name.
	 * @return The session scope attribute value previously associated with the given name, or <code>null</code> if
	 * there is no such attribute.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see ExternalContext#getSessionMap()
	 * @since 1.1
	 */
	public static <T> T removeSessionAttribute(String name) {
		return FacesLocal.removeSessionAttribute(getContext(), name);
	}

	// Application scope ----------------------------------------------------------------------------------------------

	/**
	 * Returns the application scope map.
	 * @return The application scope map.
	 * @see ExternalContext#getApplicationMap()
	 */
	public static Map<String, Object> getApplicationMap() {
		return FacesLocal.getApplicationMap(getContext());
	}

	/**
	 * Returns the application scope attribute value associated with the given name.
	 * @param <T> The expected return type.
	 * @param name The application scope attribute name.
	 * @return The application scope attribute value associated with the given name.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see ExternalContext#getApplicationMap()
	 */
	public static <T> T getApplicationAttribute(String name) {
		return FacesLocal.getApplicationAttribute(getContext(), name);
	}

	/**
	 * Sets the application scope attribute value associated with the given name.
	 * @param name The application scope attribute name.
	 * @param value The application scope attribute value.
	 * @see ExternalContext#getApplicationMap()
	 */
	public static void setApplicationAttribute(String name, Object value) {
		FacesLocal.setApplicationAttribute(getContext(), name, value);
	}

	/**
	 * Removes the application scope attribute value associated with the given name.
	 * @param <T> The expected return type.
	 * @param name The application scope attribute name.
	 * @return The application scope attribute value previously associated with the given name, or <code>null</code> if
	 * there is no such attribute.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see ExternalContext#getApplicationMap()
	 * @since 1.1
	 */
	public static <T> T removeApplicationAttribute(String name) {
		return FacesLocal.removeApplicationAttribute(getContext(), name);
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
		FacesLocal.sendFile(getContext(), file, attachment);
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
		FacesLocal.sendFile(getContext(), content, filename, attachment);
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
		FacesLocal.sendFile(getContext(), content, filename, attachment);
	}

	/**
	 * Send a file to the response whose content is provided via given output stream callback. The content type will be
	 * determined based on file name. The content length will not be set because that would require buffering the entire
	 * file in memory or temp disk. The client may receive a download of an unknown length and thus the download
	 * progress may be unknown to the client. If this is undesirable, write to a temp file instead and then use
	 * {@link Faces#sendFile(File, boolean)}. The {@link FacesContext#responseComplete()} will implicitly be called
	 * after successful streaming.
	 * @param filename The file name which should appear in content disposition header.
	 * @param attachment Whether the file should be provided as attachment, or just inline.
	 * @param outputCallback The output stream callback to write the file content to.
	 * @throws IOException Whenever something fails at I/O level. The caller should preferably not catch it, but just
	 * redeclare it in the action method. The servletcontainer will handle it.
	 * @since 2.3
	 */
	public static void sendFile(String filename, boolean attachment, Callback.Output outputCallback) throws IOException {
		FacesLocal.sendFile(getContext(), filename, attachment, outputCallback);
	}

}