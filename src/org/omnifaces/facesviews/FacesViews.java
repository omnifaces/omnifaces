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
import static java.util.Locale.US;
import static java.util.regex.Pattern.quote;
import static org.omnifaces.util.Faces.getApplicationAttribute;
import static org.omnifaces.util.Faces.getFacesServletRegistration;
import static org.omnifaces.util.ResourcePaths.getExtension;
import static org.omnifaces.util.ResourcePaths.isDirectory;
import static org.omnifaces.util.ResourcePaths.stripExtension;
import static org.omnifaces.util.ResourcePaths.stripPrefixPath;
import static org.omnifaces.util.Utils.csvToList;
import static org.omnifaces.util.Utils.isEmpty;
import static org.omnifaces.util.Utils.reverse;
import static org.omnifaces.util.Utils.startsWithOneOf;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServletRequest;

import org.omnifaces.util.Faces;

/**
 * This class contains the core methods that implement the Faces Views feature.
 * 
 * @author Arjan Tijms
 * 
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

	public static final String FACES_VIEWS_EXTENSION_ACTION_PARAM_NAME = "org.omnifaces.FACES_VIEWS_EXTENSION_ACTION";
	
	public static final String FACES_VIEWS_PATH_ACTION_PARAM_NAME = "org.omnifaces.FACES_VIEWS_PATH_ACTION";
	
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
	public static final String FACES_VIEWS_REVERSE_RESOURCES = "org.omnifaces.facesviews.reverse.resources";
    public static final String FACES_VIEWS_RESOURCES_EXTENSIONS = "org.omnifaces.facesviews.extensions";

    
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
	
	public static Set<String> getPublicRootPaths(ServletContext servletContext) {
		@SuppressWarnings("unchecked")
		Set<String> publicRootPaths = (Set<String>) servletContext.getAttribute(PUBLIC_SCAN_PATHS);
		if (publicRootPaths == null) {
			Set<String> rootPaths = getRootPaths(servletContext);
			publicRootPaths = new HashSet<String>();
			for (String rootPath : rootPaths) {
				if (!"/".equals(rootPath) && !startsWithOneOf(rootPath, "/WEB-INF/", "/META-INF/")) {
					publicRootPaths.add(rootPath);
				}
			}
			servletContext.setAttribute(PUBLIC_SCAN_PATHS, unmodifiableSet(publicRootPaths));
		}
		
		return publicRootPaths;
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
			return null;
		}
		
		try {
			return ExtensionAction.valueOf(extensionActionString.toUpperCase(US));
		} catch (Exception e) {
			throw new IllegalStateException(
				String.format(
					"Value '%s' is not valud for context parameter for '%s'",
					extensionActionString, FACES_VIEWS_EXTENSION_ACTION_PARAM_NAME
				)
			);
		}
	}
	
	public static PathAction getPathAction(ServletContext servletContext) {
		String pathActionString = servletContext.getInitParameter(FACES_VIEWS_PATH_ACTION_PARAM_NAME);
		if (isEmpty(pathActionString)) {
			return null;
		}
		
		try {
			return PathAction.valueOf(pathActionString.toUpperCase(US));
		} catch (Exception e) {
			throw new IllegalStateException(
				String.format(
					"Value '%s' is not valud for context parameter for '%s'",
					pathActionString, FACES_VIEWS_PATH_ACTION_PARAM_NAME
				)
			);
		}
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
			context.setAttribute(FACES_VIEWS_REVERSE_RESOURCES, unmodifiableMap(reverse(views)));
		}
		return views;
	}

	/**
	 * Strips the special 'faces-views' prefix path from the resource if any.
	 * 
	 * @param resource
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
		
		String baseURL = Faces.getRequestBaseURL(request);
		if (baseURL.endsWith("/")) {
			baseURL = baseURL.substring(0, baseURL.length()-1);
		}
		
		return baseURL + stripExtension(resource) + queryString;
	}

}