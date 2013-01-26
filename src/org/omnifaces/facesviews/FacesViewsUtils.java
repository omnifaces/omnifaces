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

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.regex.Pattern.quote;
import static javax.faces.FactoryFinder.APPLICATION_FACTORY;
import static org.omnifaces.util.Utils.csvToList;
import static org.omnifaces.util.Utils.isEmpty;
import static org.omnifaces.util.Utils.startsWithOneOf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.webapp.FacesServlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

/**
 * Collection of utilities for working with auto-scanned- and extensionless views.
 * 
 * @author Arjan Tijms
 * 
 */
final public class FacesViewsUtils {

	private FacesViewsUtils() {
	}

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
	 * The name of the application scope context parameter under which a Set version of the paths that are to be scanned
	 * by faces views are kept.
	 */
	public static final String SCAN_PATHS = "org.omnifaces.facesviews.scanpaths";

	/**
	 * The name of the init parameter (in web.xml) via which the user can set scanned views to be always rendered
	 * extensionless. Without this setting (or it being set to false), it depends on whether the request URI uses an
	 * extension or not. If it doesn't, links are also rendered without one, otherwise are rendered with an extension.
	 */
	public static final String FACES_VIEWS_SCANNED_VIEWS_EXTENSIONLESS_PARAM_NAME = "org.omnifaces.FACES_VIEWS_SCANNED_VIEWS_ALWAYS_EXTENSIONLESS";

	/**
	 * The name of the application scope context parameter under which a Boolean version of the scanned views always
	 * exensionless init parameter is kept.
	 */
	public static final String SCANNED_VIEWS_EXTENSIONLESS = "org.omnifaces.facesviews.scannedviewsextensionless";
	
	public static final String FACES_VIEWS_RESOURCES = "org.omnifaces.facesviews";
    public static final String FACES_VIEWS_RESOURCES_EXTENSIONS = "org.omnifaces.facesviews.extensions";
    

	/**
	 * Gets the JSF Application instance.
	 * 
	 * @return The JSF Application instance.
	 */
	public static Application getApplication() {
		return ((ApplicationFactory) FactoryFinder.getFactory(APPLICATION_FACTORY)).getApplication();
	}

	/**
	 * Checks if the given resource path obtained from {@link ServletContext#getResourcePaths(String)} represents a
	 * directory.
	 * 
	 * @param resourcePath
	 *            the resource path to check
	 * @return true if the resource path represents a directory, false otherwise
	 */
	public static boolean isDirectory(final String resourcePath) {
		return resourcePath.endsWith("/");
	}

	/**
	 * Strips the special 'faces-views' prefix path from the resource if any.
	 * 
	 * @param resource
	 * @return the resource without the special prefix path, or as-is if it didn't start with this prefix.
	 */
	public static String stripPrefixPath(final String resource) {
		return stripPrefixPath(WEB_INF_VIEWS, resource);
	}

	/**
	 * Strips the special 'faces-views' prefix path from the resource if any.
	 * 
	 * @param resource
	 * @return the resource without the special prefix path, or as-is if it didn't start with this prefix.
	 */
	public static String stripPrefixPath(final String prefix, final String resource) {
		String normalizedResource = resource;
		if (normalizedResource.startsWith(prefix)) {
			normalizedResource = normalizedResource.substring(prefix.length() - 1);
		}

		return normalizedResource;
	}

	/**
	 * Strips the extension from a resource if any. This extension is defined as everything after the last occurrence of
	 * a period, including the period itself. E.g. input "index.xhtml" will return "index".
	 * 
	 * @param resource
	 * @return the resource without its extension, of as-is if it doesn't have an extension.
	 */
	public static String stripExtension(final String resource) {
		String normalizedResource = resource;
		int lastPeriod = resource.lastIndexOf('.');
		if (lastPeriod != -1) {
			normalizedResource = resource.substring(0, lastPeriod);
		}

		return normalizedResource;
	}

	/**
	 * Gets the extension of a resource if any. This extension is defined as everything after the last occurrence of a
	 * period, including the period itself. E.g. input "index.xhtml" will return ".xhtml'.
	 * 
	 * 
	 * @param resource
	 * @return the extension of the resource, or null if it doesn't have an extension.
	 */
	public static String getExtension(final String resource) {
		String extension = null;
		int lastPeriod = resource.lastIndexOf('.');
		if (lastPeriod != -1) {
			extension = resource.substring(lastPeriod);
		}

		return extension;
	}

	public static boolean isExtensionless(final String viewId) {
		return viewId != null && !viewId.contains(".");
	}

	@SuppressWarnings("unchecked")
	public static <T> T getApplicationAttribute(final FacesContext context, final String name) {
		return (T) context.getExternalContext().getApplicationMap().get(name);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getApplicationAttribute(final ServletContext context, final String name) {
		return (T) context.getAttribute(name);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getRequestAttribute(final FacesContext context, final String name) {
		return (T) context.getExternalContext().getRequestMap().get(name);
	}

	/**
	 * Gets the ServletRegistration associated with the FacesServlet.
	 * 
	 * @param servletContext
	 *            the context to get the ServletRegistration from.
	 * @return ServletRegistration for FacesServlet, or null if the FacesServlet is not installed.
	 */
	public static ServletRegistration getFacesServletRegistration(final ServletContext servletContext) {
		ServletRegistration facesServletRegistration = null;
		for (ServletRegistration registration : servletContext.getServletRegistrations().values()) {
			if (registration.getClassName().equals(FacesServlet.class.getName())) {
				facesServletRegistration = registration;
				break;
			}
		}

		return facesServletRegistration;
	}

	public static void scanViewsFromRootPaths(ServletContext servletContext, Map<String, String> collectedViews, Set<String> collectedExtensions) {
		for (String rootPath : getRootPaths(servletContext)) {
			
			String extensionToScan = null;
			if (rootPath.contains("*")) {
				String[] pathAndExtension = rootPath.split(quote("*"));
				rootPath = pathAndExtension[0];
				extensionToScan = pathAndExtension[1];
				
			}
			
			scanViews(servletContext, rootPath, servletContext.getResourcePaths(rootPath), collectedViews, extensionToScan, collectedExtensions);
		}
	}

	public static Set<String> getRootPaths(ServletContext servletContext) {
		@SuppressWarnings("unchecked")
		Set<String> rootPaths = (Set<String>) servletContext.getAttribute(SCAN_PATHS);
		if (rootPaths == null) {
			rootPaths = new HashSet<String>(csvToList(servletContext.getInitParameter(FACES_VIEWS_SCAN_PATHS_PARAM_NAME)));
			rootPaths.add(WEB_INF_VIEWS);
			servletContext.setAttribute(SCAN_PATHS, unmodifiableSet(rootPaths));
		}

		return rootPaths;
	}

	public static Boolean isScannedViewsAlwaysExtensionless(final FacesContext context) {

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
	 * @param servletContext
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
					if (!"/".equals(rootPath)) {
						// For the root path "/", there is no need to store the resource with extension, as
						// that particular 'mapping' is already what servers load by default.
						collectedViews.put(resource, resourcePath);
					}
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
		
		// For the special directory /, don't scan WEB-INF and META-INF
		return !startsWithOneOf(directory, "/WEB-INF/", "/META-INF/");
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
	 * @param servletContext
	 * @return views
	 */
	public static Map<String, String> scanViews(ServletContext servletContext) {
		Map<String, String> collectedViews = new HashMap<String, String>();
		scanViewsFromRootPaths(servletContext, collectedViews, null);
		return collectedViews;
	}

	/**
	 * Checks if resources haven't been scanned yet, and if not does scanning and stores the result at the designated
	 * location "org.omnifaces.facesviews" in the ServletContext.
	 * 
	 * @param context
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
	 * @param context
	 * @return the view found during scanning, or the empty map if no views encountered
	 */
	public static Map<String, String> scanAndStoreViews(ServletContext context) {
		Map<String, String> views = scanViews(context);
		if (!views.isEmpty()) {
			context.setAttribute(FACES_VIEWS_RESOURCES, unmodifiableMap(views));
		}
		return views;
	}

}