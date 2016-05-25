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
package org.omnifaces.facesviews;

import static java.lang.Boolean.parseBoolean;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Locale.US;
import static java.util.regex.Pattern.quote;
import static javax.faces.view.facelets.ResourceResolver.FACELETS_RESOURCE_RESOLVER_PARAM_NAME;
import static org.omnifaces.facesviews.FacesServletDispatchMethod.DO_FILTER;
import static org.omnifaces.util.Faces.getApplicationAttribute;
import static org.omnifaces.util.Faces.getApplicationFromFactory;
import static org.omnifaces.util.Platform.getFacesServletRegistration;
import static org.omnifaces.util.ResourcePaths.filterExtension;
import static org.omnifaces.util.ResourcePaths.getExtension;
import static org.omnifaces.util.ResourcePaths.isDirectory;
import static org.omnifaces.util.ResourcePaths.isExtensionless;
import static org.omnifaces.util.ResourcePaths.stripExtension;
import static org.omnifaces.util.ResourcePaths.stripPrefixPath;
import static org.omnifaces.util.Servlets.getApplicationAttribute;
import static org.omnifaces.util.Servlets.getRequestBaseURL;
import static org.omnifaces.util.Servlets.isFacesDevelopment;
import static org.omnifaces.util.Utils.csvToList;
import static org.omnifaces.util.Utils.isEmpty;
import static org.omnifaces.util.Utils.reverse;
import static org.omnifaces.util.Utils.startsWithOneOf;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.faces.application.Application;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.webapp.FacesServlet;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServletRequest;

import org.omnifaces.ApplicationInitializer;
import org.omnifaces.ApplicationListener;
import org.omnifaces.config.WebXml;

/**
 * <p>
 * FacesViews is a mechanism to use SEO-friendly extensionless URLs in a JSF application without the need to enlist
 * individual Facelet source files in some configuration file.
 * <p>
 * Instead, Facelets source files can be put into either the special <code>/WEB-INF/faces-views</code>
 * directory, from where they will be automatically scanned (no configuration whatsoever required) or an
 * explicit directory can be configured to be scanned. The web app root is supported as well.
 *
 * <h3>Installation</h3>
 * <p>
 * Example of configuring FacesViews to make all Facelets found in the root and its sub-directories (excluding /WEB-INF,
 * /META-INF and /resources) available as extensionless URLs in <code>web.xml</code>:
 * <pre>
 * &lt;context-param&gt;
 *     &lt;param-name&gt;org.omnifaces.FACES_VIEWS_SCAN_PATHS&lt;/param-name&gt;
 *     &lt;param-value&gt;/*.xhtml&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * </pre>
 * <p>
 * See <a href="package-summary.html">package documentation</a> for additional details.
 *
 * <h3>PrettyFaces</h3>
 * <p>
 * Note that there is some overlap between this feature and <a href="http://ocpsoft.org/prettyfaces">PrettyFaces</a>.
 * The difference is that FacesViews has a focus on zero- or very minimal config, where PrettyFaces has a focus on very
 * powerful mapping mechanisms, which of course need some level of configuration. As such FacesViews will only focus on
 * auto discovering views and mapping them to both <code>.xhtml</code> and to no-extension without needing to explicitly
 * declare the <code>FacesServlet</code> in <code>web.xml</code>.
 * <p>
 * Specifically, FacesViews will thus <em>not</em> become a general URL rewriting tool (e.g. one that maps path segments
 * to parameters, or that totally changes the name of the URL). For this the user is advised to look at the
 * aforementioned <a href="http://ocpsoft.org/prettyfaces">PrettyFaces</a>.
 *
 * @author Arjan Tijms
 * @see FacesViewsForwardingFilter
 * @see FacesViewsViewHandler
 * @see FacesViewsViewHandlerInstaller
 * @see FacesViewsResolver
 */
public final class FacesViews {

	private FacesViews() {
	}

	/**
	 * A special dedicated "well-known" directory where facelets implementing views can be placed.
	 * This directory is scanned by convention so that no explicit configuration is needed.
	 */
	public static final String WEB_INF_VIEWS = "/WEB-INF/faces-views/";

	/**
	 * Web context parameter to switch auto-scanning completely off for Servlet 3.0 containers.
	 */
	public static final String FACES_VIEWS_ENABLED_PARAM_NAME = "org.omnifaces.FACES_VIEWS_ENABLED";

	/**
	 * The name of the init parameter (in web.xml) where the value holds a comma separated list of paths that are to be
	 * scanned by faces views.
	 */
	public static final String FACES_VIEWS_SCAN_PATHS_PARAM_NAME = "org.omnifaces.FACES_VIEWS_SCAN_PATHS";

	/**
	 * The name of the init parameter (in web.xml) via which the user can set scanned views to be always rendered
	 * extensionless. Without this setting (or it being set to false), it depends on whether the request URI uses an
	 * extension or not. If it doesn't, links are also rendered without one, otherwise are rendered with an extension.
	 */
	public static final String FACES_VIEWS_SCANNED_VIEWS_EXTENSIONLESS_PARAM_NAME = "org.omnifaces.FACES_VIEWS_SCANNED_VIEWS_ALWAYS_EXTENSIONLESS";

	/**
	 * The name of the init parameter (in web.xml) that determines the action that is performed whenever a resource
	 * is requested WITH extension that's also available without an extension. See {@link ExtensionAction}
	 */
	public static final String FACES_VIEWS_EXTENSION_ACTION_PARAM_NAME = "org.omnifaces.FACES_VIEWS_EXTENSION_ACTION";

	/**
	 * The name of the init parameter (in web.xml) that determines the action that is performed whenever a resource
	 * is requested in a public path that has been used for scanning views by faces views. See {@link PathAction}
	 */
	public static final String FACES_VIEWS_PATH_ACTION_PARAM_NAME = "org.omnifaces.FACES_VIEWS_PATH_ACTION";

	/**
	 * The name of the init parameter (in web.xml) that determines the method used by FacesViews to invoke the FacesServlet.
	 * See {@link FacesServletDispatchMethod}.
	 */
	public static final String FACES_VIEWS_DISPATCH_METHOD_PARAM_NAME = "org.omnifaces.FACES_VIEWS_DISPATCH_METHOD";

	/**
	 * The name of the boolean init parameter (in web.xml) via which the user can set whether the {@link FacesViewsForwardingFilter}
	 * should match before declared filters (false) or after declared filters (true);
	 */
	public static final String FACES_VIEWS_FILTER_AFTER_DECLARED_FILTERS_PARAM_NAME = "org.omnifaces.FACES_VIEWS_FILTER_AFTER_DECLARED_FILTERS";

	/**
	 * The name of the boolean init parameter (in web.xml) via which the user can set whether the {@link FacesViewsViewHandler}
	 * should strip the extension from the parent view handler's outcome or construct the URL itself and only take the query
	 * parameters (if any) from the parent.
	 */
	public static final String FACES_VIEWS_VIEW_HANDLER_MODE_PARAM_NAME = "org.omnifaces.FACES_VIEWS_VIEW_HANDLER_MODE";

	/**
	 * The name of the application scope context parameter under which a Set version of the paths that are to be scanned
	 * by faces views are kept.
	 */
	public static final String SCAN_PATHS = "org.omnifaces.facesviews.scanpaths";

	/**
	 * The name of the application scope context parameter under which a Set version of the public paths that are to be scanned
	 * by faces views are kept. A public path is a path that is also directly accessible, e.g. is world readable. This excludes
	 * the special path /, which is by definition world readable but not included in this set.
	 */
	public static final String PUBLIC_SCAN_PATHS = "org.omnifaces.facesviews.public.scanpaths";

	/**
	 * The name of the application scope context parameter under which a Boolean version of the scanned views always
	 * exensionless init parameter is kept.
	 */
	public static final String SCANNED_VIEWS_EXTENSIONLESS = "org.omnifaces.facesviews.scannedviewsextensionless";

	/**
	 * The name of the application scope context parameter under which a Set that stores the extensions to which
	 * the FacesServlet is mapped.
	 */
	public static final String FACES_SERVLET_EXTENSIONS = "org.omnifaces.facesviews.facesservletextensions";

	public static final String FACES_VIEWS_RESOURCES = "org.omnifaces.facesviews";
	public static final String FACES_VIEWS_REVERSE_RESOURCES = "org.omnifaces.facesviews.reverse.resources";
	public static final String FACES_VIEWS_RESOURCES_EXTENSIONS = "org.omnifaces.facesviews.extensions";

	public static final String FACES_VIEWS_ORIGINAL_SERVLET_PATH = "org.omnifaces.facesviews.original.servlet_path";

	/**
	 * This will register the {@link FacesViewsForwardingFilter}.
	 * @param servletContext The involved servlet context.
	 */
	public static void registerFilter(ServletContext servletContext) {

		if (!"false".equals(servletContext.getInitParameter(FACES_VIEWS_ENABLED_PARAM_NAME))) {

			// Scan our dedicated directory for Faces resources that need to be mapped
			Map<String, String> collectedViews = new HashMap<>();
			Set<String> collectedExtensions = new HashSet<>();
			scanViewsFromRootPaths(servletContext, collectedViews, collectedExtensions);

			if (!collectedViews.isEmpty()) {

				// Store the resources and extensions that were found in application scope, where others can find it.
				servletContext.setAttribute(FACES_VIEWS_RESOURCES, unmodifiableMap(collectedViews));
				servletContext.setAttribute(FACES_VIEWS_REVERSE_RESOURCES, unmodifiableMap(reverse(collectedViews)));
				servletContext.setAttribute(FACES_VIEWS_RESOURCES_EXTENSIONS, unmodifiableSet(collectedExtensions));

				// Register 3 artifacts with the Servlet container and JSF that help implement this feature:

				// 1. A Filter that forwards extensionless requests to an extension mapped request, e.g. /index to
				// /index.xhtml
				// (The FacesServlet doesn't work well with the exact mapping that we use for extensionless URLs).
				FilterRegistration facesViewsRegistration = servletContext.addFilter(FacesViewsForwardingFilter.class.getName(),
						FacesViewsForwardingFilter.class);

				// 2. A Facelets resource resolver that resolves requests like /index.xhtml to
				// /WEB-INF/faces-views/index.xhtml
				servletContext.setInitParameter(FACELETS_RESOURCE_RESOLVER_PARAM_NAME, FacesViewsResolver.class.getName());

				// 3. A ViewHandler that transforms the forwarded extension based URL back to an extensionless one, e.g.
				// /index.xhtml to /index
				// See FacesViewsForwardingFilter#init


				if (isFacesDevelopment(servletContext) && getFacesServletDispatchMethod(servletContext) != DO_FILTER) {

					// In development mode map this Filter to "*", so we can catch requests to extensionless resources that
					// have been dynamically added. Note that resources with mapped extensions are already handled by the FacesViewsResolver.
					// Adding resources with new extensions still requires a restart.

					// Development mode only works when the dispatch mode is not DO_FILTER, since DO_FILTER mode depends
					// on the Faces Servlet being "exact"-mapped on the view resources.

					facesViewsRegistration.addMappingForUrlPatterns(null, isFilterAfterDeclaredFilters(servletContext), "/*");
				} else {

					// In non-development mode, only map this Filter to specific resources

					// Map the forwarding filter to all the resources we found.
					for (String resource : collectedViews.keySet()) {
						facesViewsRegistration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD), isFilterAfterDeclaredFilters(servletContext), resource);
					}

					// Additionally map the filter to all paths that were scanned and which are also directly
					// accessible. This is to give the filter an opportunity to block these.
					for (String path : getPublicRootPaths(servletContext)) {
						facesViewsRegistration.addMappingForUrlPatterns(null, false, path + "*");
					}
				}

				// We now need to map the Faces Servlet to the extensions we found, but at this point in time
                // this Faces Servlet might not be created yet, so we do this part in the
                // org.omnifaces.facesviews.FacesViews.addMappings(ServletContext) method below, which is called from
                // org.omnifaces.ApplicationListener.contextInitialized(ServletContextEvent) later
            }
		}
	}

	/**
	 * This will map the {@link FacesServlet} to extensions found during scanning in {@link ApplicationInitializer}.
	 * This part of the initialization is executed via {@link ApplicationListener}, because the {@link FacesServlet}
	 * has to be available.
	 * @param servletContext The involved servlet context.
	 */
	public static void addMappings(ServletContext servletContext) {

		if (!"false".equals(servletContext.getInitParameter(FACES_VIEWS_ENABLED_PARAM_NAME))) {

			Set<String> extensions = getApplicationAttribute(servletContext, FACES_VIEWS_RESOURCES_EXTENSIONS);

			if (!isEmpty(extensions)) {

				Set<String> mappings = new HashSet<>(extensions);
				for (String welcomeFile : WebXml.INSTANCE.init(servletContext).getWelcomeFiles()) {
					if (isExtensionless(welcomeFile)) {
						if (!welcomeFile.startsWith("/")) {
							welcomeFile = "/" + welcomeFile;
						}
						mappings.add(welcomeFile);
					}
				}

				if (getFacesServletDispatchMethod(servletContext) == DO_FILTER) {
					// In order for the DO_FILTER method to work the FacesServlet, in addition the forward filter, has
					// to be mapped on all extensionless resources.
					Map<String, String> collectedViews = getApplicationAttribute(servletContext, FACES_VIEWS_RESOURCES);
					mappings.addAll(filterExtension(collectedViews.keySet()));
				}

				mapFacesServlet(servletContext, mappings);
			}
		}
	}

	/**
	 * Register a view handler that transforms a view id with extension back to an extensionless one.
	 * @param servletContext The involved servlet context.
	 */
	public static void setViewHander(ServletContext servletContext) {
		if (isFacesViewsActive(servletContext)) {
			Application application = getApplicationFromFactory();
			application.setViewHandler(new FacesViewsViewHandler(application.getViewHandler()));
		}
	}

	public static boolean isFacesViewsActive(ServletContext servletContext) {
		if (!"false".equals(servletContext.getInitParameter(FACES_VIEWS_ENABLED_PARAM_NAME))) {
			return !isEmpty(getApplicationAttribute(servletContext, FACES_VIEWS_RESOURCES_EXTENSIONS));
		}

		return false;
	}

	public static void scanViewsFromRootPaths(ServletContext servletContext, Map<String, String> collectedViews, Set<String> collectedExtensions) {
		for (String rootPath : getRootPaths(servletContext)) {

			String extensionToScan = null;
			if (rootPath.contains("*")) {
				String[] pathAndExtension = rootPath.split(quote("*"));
				rootPath = pathAndExtension[0];
				extensionToScan = pathAndExtension[1];
			}

			rootPath = normalizeRootPath(rootPath);

			scanViews(servletContext, rootPath, servletContext.getResourcePaths(rootPath), collectedViews, extensionToScan, collectedExtensions);
		}
	}

	public static Set<String> getRootPaths(ServletContext servletContext) {
		@SuppressWarnings("unchecked")
		Set<String> rootPaths = (Set<String>) servletContext.getAttribute(SCAN_PATHS);

		if (rootPaths == null) {
			rootPaths = new HashSet<>(csvToList(servletContext.getInitParameter(FACES_VIEWS_SCAN_PATHS_PARAM_NAME)));
			rootPaths.add(WEB_INF_VIEWS);
			servletContext.setAttribute(SCAN_PATHS, unmodifiableSet(rootPaths));
		}

		return rootPaths;
	}

	public static Set<String> getPublicRootPaths(ServletContext servletContext) {
		@SuppressWarnings("unchecked")
		Set<String> publicRootPaths = (Set<String>) servletContext.getAttribute(PUBLIC_SCAN_PATHS);

		if (publicRootPaths == null) {
			Set<String> rootPaths = getRootPaths(servletContext);
			publicRootPaths = new HashSet<>();
			for (String rootPath : rootPaths) {

				if (rootPath.contains("*")) {
					String[] pathAndExtension = rootPath.split(quote("*"));
					rootPath = pathAndExtension[0];
				}

				rootPath = normalizeRootPath(rootPath);

				if (!"/".equals(rootPath) && !startsWithOneOf(rootPath, "/WEB-INF/", "/META-INF/")) {
					publicRootPaths.add(rootPath);
				}
			}
			servletContext.setAttribute(PUBLIC_SCAN_PATHS, unmodifiableSet(publicRootPaths));
		}

		return publicRootPaths;
	}

	public static String normalizeRootPath(String rootPath) {
		String normalizedPath = rootPath;
		if (!normalizedPath.startsWith("/")) {
			normalizedPath = "/" + normalizedPath;
		}
		if (!normalizedPath.endsWith("/")) {
			normalizedPath = normalizedPath + "/";
		}
		return normalizedPath;
	}

	public static boolean isResourceInPublicPath(ServletContext servletContext, String resource) {
		Set<String> publicPaths = getPublicRootPaths(servletContext);
		for (String path : publicPaths) {
			if (resource.startsWith(path)) {
				return true;
			}
		}
		return false;
	}

	public static ExtensionAction getExtensionAction(ServletContext servletContext) {
		String extensionActionString = servletContext.getInitParameter(FACES_VIEWS_EXTENSION_ACTION_PARAM_NAME);
		if (isEmpty(extensionActionString)) {
			return ExtensionAction.REDIRECT_TO_EXTENSIONLESS;
		}

		try {
			return ExtensionAction.valueOf(extensionActionString.toUpperCase(US));
		} catch (Exception e) {
			throw new IllegalStateException(
				String.format(
					"Value '%s' is not valid for context parameter for '%s'",
					extensionActionString, FACES_VIEWS_EXTENSION_ACTION_PARAM_NAME
				), e
			);
		}
	}

	public static PathAction getPathAction(ServletContext servletContext) {
		String pathActionString = servletContext.getInitParameter(FACES_VIEWS_PATH_ACTION_PARAM_NAME);
		if (isEmpty(pathActionString)) {
			return PathAction.SEND_404;
		}

		try {
			return PathAction.valueOf(pathActionString.toUpperCase(US));
		} catch (Exception e) {
			throw new IllegalStateException(
				String.format(
					"Value '%s' is not valid for context parameter for '%s'",
					pathActionString, FACES_VIEWS_PATH_ACTION_PARAM_NAME
				), e
			);
		}
	}

	public static FacesServletDispatchMethod getFacesServletDispatchMethod(ServletContext servletContext) {
		String dispatchMethodString = servletContext.getInitParameter(FACES_VIEWS_DISPATCH_METHOD_PARAM_NAME);
		if (isEmpty(dispatchMethodString)) {
			return FacesServletDispatchMethod.DO_FILTER;
		}

		try {
			return FacesServletDispatchMethod.valueOf(dispatchMethodString.toUpperCase(US));
		} catch (Exception e) {
			throw new IllegalStateException(
				String.format(
					"Value '%s' is not valid for context parameter for '%s'",
					dispatchMethodString, FACES_VIEWS_DISPATCH_METHOD_PARAM_NAME
				), e
			);
		}
	}

	public static ViewHandlerMode getViewHandlerMode(FacesContext context) {
		return getViewHandlerMode((ServletContext) context.getExternalContext().getContext());
	}

	public static ViewHandlerMode getViewHandlerMode(ServletContext servletContext) {
		String viewHandlerModeString = servletContext.getInitParameter(FACES_VIEWS_VIEW_HANDLER_MODE_PARAM_NAME);
		if (isEmpty(viewHandlerModeString)) {
			return ViewHandlerMode.STRIP_EXTENSION_FROM_PARENT;
		}

		try {
			return ViewHandlerMode.valueOf(viewHandlerModeString.toUpperCase(US));
		} catch (Exception e) {
			throw new IllegalStateException(
				String.format(
					"Value '%s' is not valid for context parameter for '%s'",
					viewHandlerModeString, FACES_VIEWS_VIEW_HANDLER_MODE_PARAM_NAME
				), e
			);
		}
	}

	public static boolean isFilterAfterDeclaredFilters(ServletContext servletContext) {
		String filterAfterDeclaredFilters = servletContext.getInitParameter(FACES_VIEWS_FILTER_AFTER_DECLARED_FILTERS_PARAM_NAME);
		return filterAfterDeclaredFilters == null || parseBoolean(filterAfterDeclaredFilters);
	}

	public static boolean isScannedViewsAlwaysExtensionless(final FacesContext context) {

		ExternalContext externalContext = context.getExternalContext();
		Map<String, Object> applicationMap = externalContext.getApplicationMap();

		Boolean scannedViewsExtensionless = (Boolean) applicationMap.get(SCANNED_VIEWS_EXTENSIONLESS);
		if (scannedViewsExtensionless == null) {
			scannedViewsExtensionless = Boolean.valueOf(externalContext.getInitParameter(FACES_VIEWS_SCANNED_VIEWS_EXTENSIONLESS_PARAM_NAME));
			applicationMap.put(SCANNED_VIEWS_EXTENSIONLESS, scannedViewsExtensionless);
		}

		return scannedViewsExtensionless;
	}

	/**
	 * Scans resources (views) recursively starting with the given resource paths for a specific root path, and collects
	 * those and all unique extensions encountered in a flat map respectively set.
	 *
	 * @param servletContext The involved servlet context.
	 * @param rootPath
	 *            one of the paths from which views are scanned. By default this is typically /WEB-INF/faces-view/
	 * @param resourcePaths
	 *            collection of paths to be considered for scanning, can be either files or directories.
	 * @param collectedViews
	 *            a mapping of all views encountered during scanning. Mapping will be from the simplified form to the
	 *            actual location relatively to the web root. E.g key "foo", value "/WEB-INF/faces-view/foo.xhtml"
	 * @param extensionToScan
	 *            a specific extension to scan for. Should start with a ., e.g. ".xhtml". If this is given, only
	 *            resources with that extension will be scanned. If null, all resources will be scanned.
	 * @param collectedExtensions
	 *            set in which all unique extensions will be collected. May be null, in which case no extensions will be
	 *            collected
	 */
	public static void scanViews(ServletContext servletContext, String rootPath, Set<String> resourcePaths, Map<String, String> collectedViews,
			String extensionToScan, Set<String> collectedExtensions) {

		if (!isEmpty(resourcePaths)) {
			for (String resourcePath : resourcePaths) {
				if (isDirectory(resourcePath)) {
					if (canScanDirectory(rootPath, resourcePath)) {
						scanViews(servletContext, rootPath, servletContext.getResourcePaths(resourcePath), collectedViews, extensionToScan, collectedExtensions);
					}
				} else if (canScanResource(resourcePath, extensionToScan)) {

					// Strip the root path from the current path. E.g.
					// /WEB-INF/faces-views/foo.xhtml will become foo.xhtml if the root path = /WEB-INF/faces-view/
					String resource = stripPrefixPath(rootPath, resourcePath);

					// Store the resource with and without an extension, e.g. store both foo.xhtml and foo
					collectedViews.put(resource, resourcePath);
					collectedViews.put(stripExtension(resource), resourcePath);

					// Optionally, collect all unique extensions that we have encountered
					if (collectedExtensions != null) {
						collectedExtensions.add("*" + getExtension(resourcePath));
					}
				}
			}
		}
	}

	public static boolean canScanDirectory(String rootPath, String directory) {

		if (!"/".equals(rootPath)) {
			// If a user has explicitly asked for scanning anything other than /, every sub directory of it can be scanned.
			return true;
		}

		// For the special directory /, don't scan WEB-INF, META-INF and resources
		return !startsWithOneOf(directory, "/WEB-INF/", "/META-INF/", "/resources/");
	}

	public static boolean canScanResource(String resource, String extensionToScan) {

		if (extensionToScan == null) {
			// If no extension has been explicitly defined, we scan all extensions encountered
			return true;
		}

		return resource.endsWith(extensionToScan);
	}

	/**
	 * Scans resources (views) recursively starting with the given resource paths and returns a flat map containing all
	 * resources encountered.
	 *
	 * @param servletContext The involved servlet context.
	 * @return views
	 */
	public static Map<String, String> scanViews(ServletContext servletContext) {
		Map<String, String> collectedViews = new HashMap<>();
		scanViewsFromRootPaths(servletContext, collectedViews, null);
		return collectedViews;
	}

	/**
	 * Checks if resources haven't been scanned yet, and if not does scanning and stores the result at the designated
	 * location "org.omnifaces.facesviews" in the ServletContext.
	 *
	 * @param context The involved servlet context.
	 */
	public static void tryScanAndStoreViews(ServletContext context) {
		if (getApplicationAttribute(context, FACES_VIEWS_RESOURCES) == null) {
			scanAndStoreViews(context);
		}
	}

	/**
	 * Scans for faces-views resources and stores the result at the designated location "org.omnifaces.facesviews" in
	 * the ServletContext.
	 *
	 * @param context The involved servlet context.
	 * @return the view found during scanning, or the empty map if no views encountered
	 */
	public static Map<String, String> scanAndStoreViews(ServletContext context) {
		Map<String, String> views = scanViews(context);
		if (!views.isEmpty()) {
			context.setAttribute(FACES_VIEWS_RESOURCES, unmodifiableMap(views));
			context.setAttribute(FACES_VIEWS_REVERSE_RESOURCES, unmodifiableMap(reverse(views)));
		}
		return views;
	}

	/**
	 * Strips the special 'faces-views' prefix path from the resource if any.
	 *
	 * @param resource The resource.
	 * @return the resource without the special prefix path, or as-is if it didn't start with this prefix.
	 */
	public static String stripFacesViewsPrefix(final String resource) {
		return stripPrefixPath(WEB_INF_VIEWS, resource);
	}

	public static String getMappedPath(String path) {
		String facesViewsPath = path;
		Map<String, String> mappedResources = getApplicationAttribute(FACES_VIEWS_RESOURCES);
		if (mappedResources != null && mappedResources.containsKey(path)) {
			facesViewsPath = mappedResources.get(path);
		}

		return facesViewsPath;
	}

	/**
	 * Map the Facelets Servlet to the given extensions
	 *
	 * @param servletContext The involved servlet context.
	 * @param extensions collections of extensions (typically those as encountered during scanning)
	 */
	public static void mapFacesServlet(ServletContext servletContext, Set<String> extensions) {

		ServletRegistration facesServletRegistration = getFacesServletRegistration(servletContext);
		if (facesServletRegistration != null) {
			Collection<String> mappings = facesServletRegistration.getMappings();
			for (String extension : extensions) {
				if (!mappings.contains(extension)) {
					facesServletRegistration.addMapping(extension);
				}
			}
		}
	}

	public static Set<String> getFacesServletExtensions(FacesContext context) {
		return getFacesServletExtensions((ServletContext) context.getExternalContext().getContext());
	}

	public static Set<String> getFacesServletExtensions(ServletContext servletContext) {

		@SuppressWarnings("unchecked")
		Set<String> extensions = (Set<String>) servletContext.getAttribute(FACES_SERVLET_EXTENSIONS);

		if (extensions == null) {
			extensions = new HashSet<>();
			ServletRegistration facesServletRegistration = getFacesServletRegistration(servletContext);
			if (facesServletRegistration != null) {
				Collection<String> mappings = facesServletRegistration.getMappings();
				for (String mapping : mappings) {
					if (mapping.startsWith("*")) {
						extensions.add(mapping.substring(1));
					}
				}
			}
			servletContext.setAttribute(FACES_SERVLET_EXTENSIONS, unmodifiableSet(extensions));
		}

		return extensions;
	}

	/**
	 * Obtains the full request URL from the given request complete with the query string, but with the
	 * extension (if any) cut out.
	 * <p>
	 * E.g. <code>http://localhost/foo/bar.xhtml?kaz=1</code> becomes <code>http://localhost/foo/bar?kaz=1</code>
	 *
	 * @param request the request from the URL is obtained.
	 * @return request URL with query parameters but without file extension
	 */
	public static String getExtensionlessURLWithQuery(HttpServletRequest request) {
		return getExtensionlessURLWithQuery(request, request.getServletPath());
	}

	/**
	 * Obtains the full request URL from the given request and the given resource complete with the query string, but with the
	 * extension (if any) cut out.
	 * <p>
	 * E.g. <code>http://localhost/foo/bar.xhtml?kaz=1</code> becomes <code>http://localhost/foo/bar?kaz=1</code>
	 *
	 * @param request the request from which the base URL is obtained.
	 * @param resource the resource relative to the base URL
	 * @return request URL with query parameters but without file extension
	 */
	public static String getExtensionlessURLWithQuery(HttpServletRequest request, String resource) {
		String queryString = !isEmpty(request.getQueryString()) ? "?" + request.getQueryString() : "";

		String baseURL = getRequestBaseURL(request);
		if (baseURL.endsWith("/")) {
			baseURL = baseURL.substring(0, baseURL.length()-1);
		}

		return baseURL + stripExtension(resource) + queryString;
	}

}