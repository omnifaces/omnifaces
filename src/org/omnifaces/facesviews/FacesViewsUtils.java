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
import static javax.faces.FactoryFinder.APPLICATION_FACTORY;
import static org.omnifaces.facesviews.FacesViewsResolver.FACES_VIEWS_RESOURCES_PARAM_NAME;
import static org.omnifaces.util.Utils.isEmpty;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;
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

    private FacesViewsUtils() {}

    public static final String WEB_INF_VIEWS = "/WEB-INF/faces-views/";

    /**
     * Gets the JSF Application instance.
     *
     * @return The JSF Application instance.
     */
    public static Application getApplication() {
        return ((ApplicationFactory) FactoryFinder.getFactory(APPLICATION_FACTORY)).getApplication();
    }

    /**
     * Checks if the given resource path obtained from {@link ServletContext#getResourcePaths(String)} represents a directory.
     *
     * @param resourcePath the resource path to check
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
        String normalizedResource = resource;
        if (normalizedResource.startsWith(WEB_INF_VIEWS)) {
            normalizedResource = normalizedResource.substring(WEB_INF_VIEWS.length() - 1);
        }

        return normalizedResource;
    }

    /**
     * Strips the extension from a resource if any. This extension is defined as
     * everything after the last occurrence of a period, including the period itself.
     * E.g. input "index.xhtml" will return "index".
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
     * Gets the extension of a resource if any. This extension is defined as
     * everything after the last occurrence of a period, including the period itself.
     * E.g. input "index.xhtml" will return ".xhtml'.
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
     * @param servletContext the context to get the ServletRegistration from.
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

    /**
     * Scans resources (views) recursively starting with the given resource paths, and collects those and all unique extensions
     * encountered in a flat map respectively set.
     *
     * @param servletContext
     * @param resourcePaths
     * @param collectedViews
     * @param collectedExtentions
     */
    public static void scanViews(ServletContext servletContext, Set<String> resourcePaths, Map<String, String> collectedViews, Set<String> collectedExtentions) {
        if (!isEmpty(resourcePaths)) {
            for (String resourcePath : resourcePaths) {
                if (isDirectory(resourcePath)) {
                    scanViews(servletContext, servletContext.getResourcePaths(resourcePath), collectedViews, collectedExtentions);
                } else {
                    String resource = stripPrefixPath(resourcePath);
                    collectedViews.put(resource, resourcePath);
                    collectedViews.put(stripExtension(resource), resourcePath);
                    collectedExtentions.add("*" + getExtension(resourcePath));
                }
            }
        }
    }

    /**
     * Scans resources (views) recursively starting with the given resource paths and returns a flat map containing all resources
     * encountered.
     *
     * @param servletContext
     * @param resourcePaths
     * @return views
     */
    public static Map<String, String> scanViews(ServletContext servletContext, Set<String> resourcePaths) {
        Map<String, String> collectedViews = new HashMap<String, String>();
        scanViews(servletContext, resourcePaths, collectedViews);
        return collectedViews;
    }

    /**
     * Scans resources (views) recursively starting with the given resource paths, and collects those in a flat map.
     *
     * @param servletContext
     * @param resourcePaths
     * @param collectedViews
     */
    public static void scanViews(ServletContext servletContext, Set<String> resourcePaths, Map<String, String> collectedViews) {
        if (!isEmpty(resourcePaths)) {
            for (String resourcePath : resourcePaths) {
                if (isDirectory(resourcePath)) {
                    scanViews(servletContext, servletContext.getResourcePaths(resourcePath), collectedViews);
                } else {
                    String resource = stripPrefixPath(resourcePath);
                    collectedViews.put(resource, resourcePath);
                    collectedViews.put(stripExtension(resource), resourcePath);
                }
            }
        }
    }
    
    /**
     * Checks if resources haven't been scanned yet, and if not does scanning and stores the
     * result at the designated location "org.omnifaces.facesviews" in the ServletContext.
     * 
     * @param context
     */
    public static void tryScanAndStoreViews(ServletContext context) {
        if (getApplicationAttribute(context, FACES_VIEWS_RESOURCES_PARAM_NAME) == null) {
        	scanAndStoreViews(context);
        }
    }
    
    /**
     * Scans for faces-views resources and stores the result at the designated location "org.omnifaces.facesviews"
     * in the ServletContext.
     * 
     * @param context
     * @return the view found during scanning, or the empty map if no views encountered
     */
    public static Map<String, String> scanAndStoreViews(ServletContext context) {
        Map<String, String> views = scanViews(context, context.getResourcePaths(WEB_INF_VIEWS));
        if (!views.isEmpty()) {
            context.setAttribute(FACES_VIEWS_RESOURCES_PARAM_NAME, unmodifiableMap(views));
        }
        return views;
    }

}