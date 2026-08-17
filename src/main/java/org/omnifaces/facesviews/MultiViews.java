/*
 * Copyright OmniFaces
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
package org.omnifaces.facesviews;

import static java.util.Collections.unmodifiableSet;
import static java.util.Locale.ENGLISH;
import static java.util.stream.Collectors.toSet;
import static org.omnifaces.facesviews.FacesViews.getExcludedPaths;
import static org.omnifaces.facesviews.FacesViews.getMappedResources;
import static org.omnifaces.facesviews.FacesViews.getMappedWelcomeFiles;
import static org.omnifaces.util.ResourcePaths.PATH_SEPARATOR;
import static org.omnifaces.util.ResourcePaths.getParent;
import static org.omnifaces.util.ResourcePaths.stripExtension;
import static org.omnifaces.util.ResourcePaths.stripTrailingSlash;
import static org.omnifaces.util.Servlets.getApplicationAttribute;
import static org.omnifaces.util.Utils.isEmpty;

import java.text.Collator;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Holds the MultiViews state of {@link FacesViews} and resolves requests against it.
 * <p>
 * A MultiViews view is a scanned view which additionally swallows any trailing path segments and exposes them as path parameters. It is identified by the
 * {@value #SUFFIX} suffix, both in the <code>org.omnifaces.FACES_VIEWS_SCAN_PATHS</code> context parameter which enables it and in the mapped resources under
 * which the scanned views are stored. That suffix is also a valid servlet URL pattern, which is what allows a MultiViews view to be registered with the
 * container as a prefix mapping and therefore to live in the same flat map as every other scanned view.
 * <p>
 * The public entry points are {@link FacesViews#isMultiViewsEnabled(ServletContext)}, {@link FacesViews#isMultiViewsEnabled(HttpServletRequest)} and
 * {@link FacesViews#isMultiViewsEnabled(ServletContext, String)}, which all delegate here.
 *
 * @author Arjan Tijms
 * @see FacesViews
 */
final class MultiViews {

    /** The suffix identifying a MultiViews scan path respectively a MultiViews mapped resource. */
    static final String SUFFIX = "/*";

    private static final String ENABLED = "org.omnifaces.facesviews.multiviews_enabled";
    private static final String PATHS = "org.omnifaces.facesviews.multiviews_paths";
    private static final String RESOURCES = "org.omnifaces.facesviews.multiviews_resources";
    private static final String WELCOME_FILE = "org.omnifaces.facesviews.multiviews_welcome_file";

    private MultiViews() {
        //
    }

    // Scanning -------------------------------------------------------------------------------------------------------

    static boolean isScanPath(String rootPath) {
        return rootPath.endsWith(SUFFIX);
    }

    static String stripScanPathSuffix(String rootPath) {
        return rootPath.substring(0, rootPath.lastIndexOf(SUFFIX));
    }

    static void storePaths(ServletContext servletContext, Collection<String> paths) {
        Set<String> multiViewsPaths = new TreeSet<>(Collator.getInstance(ENGLISH)); // Makes sure ! is sorted before /.
        multiViewsPaths.addAll(paths);
        servletContext.setAttribute(PATHS, unmodifiableSet(multiViewsPaths));
    }

    static void storeResources(ServletContext servletContext, Map<String, String> collectedViews) {
        servletContext.setAttribute(
            RESOURCES, unmodifiableSet(
                collectedViews.keySet().stream()
                    .filter(key -> key.endsWith(SUFFIX)).map(key -> key.substring(0, key.length() - SUFFIX.length())).collect(toSet())
            )
        );
    }

    static void storeWelcomeFile(ServletContext servletContext, Map<String, String> collectedViews) {
        for (var welcomeFile : getMappedWelcomeFiles(servletContext)) {
            if (isEnabled(servletContext) && collectedViews.containsKey(welcomeFile + SUFFIX)) {
                servletContext.setAttribute(WELCOME_FILE, welcomeFile);
            }
        }
    }

    static boolean isResource(ServletContext servletContext, String resource) {
        if (isEnabled(servletContext)) {
            var path = resource + PATH_SEPARATOR;

            for (var multiViewsPath : getPaths(servletContext)) {
                if (path.startsWith(multiViewsPath)) {
                    return true;
                }
            }
        }

        return false;
    }

    static boolean hasWelcomeFile(ServletContext servletContext) {
        return isEnabled(servletContext) && !getMappedWelcomeFiles(servletContext).isEmpty();
    }

    // Resolving ------------------------------------------------------------------------------------------------------

    static boolean isEnabled(ServletContext servletContext) {
        Boolean enabled = getApplicationAttribute(servletContext, ENABLED);

        if (enabled == null) {
            enabled = !isEmpty(getPaths(servletContext));
            servletContext.setAttribute(ENABLED, enabled);
        }

        return enabled;
    }

    static boolean isEnabled(HttpServletRequest request) {
        String resource = request.getServletPath();

        if (isEmpty(resource) && request.getPathInfo() != null) {
            resource = request.getPathInfo();
        }

        return isEnabled(request.getServletContext(), resource);
    }

    static boolean isEnabled(ServletContext servletContext, String resource) {
        if (!isEnabled(servletContext)) {
            return false;
        }

        String extensionlessResource = stripExtension(resource);
        Set<String> excludedPaths = getExcludedPaths(servletContext);

        if (!isEmpty(excludedPaths)) {
            var path = extensionlessResource + PATH_SEPARATOR;

            if (excludedPaths.stream().anyMatch(path::startsWith)) {
                return false;
            }
        }

        Set<String> multiViewsResources = getResources(servletContext);

        if (multiViewsResources != null && multiViewsResources.contains(extensionlessResource)) {
            return true;
        }

        Map<String, String> mappedResources = getMappedResources(servletContext);

        if (mappedResources != null && mappedResources.containsKey(extensionlessResource)) {
            return false;
        }

        return getWelcomeFile(servletContext) != null;
    }

    static String getWelcomeFile(ServletContext servletContext, Map<String, String> resources, String servletPath) {
        var mappedWelcomeFiles = getMappedWelcomeFiles(servletContext);

        for (var path = stripTrailingSlash(servletPath); !path.isEmpty(); path = getParent(path)) {
            for (var mappedWelcomeFile : mappedWelcomeFiles) {
                var subfolderWelcomeFile = path + mappedWelcomeFile;

                if (resources.containsKey(subfolderWelcomeFile + SUFFIX)) {
                    return subfolderWelcomeFile;
                }
            }
        }

        return getWelcomeFile(servletContext);
    }

    static Set<String> getPaths(ServletContext servletContext) {
        return getApplicationAttribute(servletContext, PATHS);
    }

    static Set<String> getResources(ServletContext servletContext) {
        return getApplicationAttribute(servletContext, RESOURCES);
    }

    static String getWelcomeFile(ServletContext servletContext) {
        return getApplicationAttribute(servletContext, WELCOME_FILE);
    }

}
