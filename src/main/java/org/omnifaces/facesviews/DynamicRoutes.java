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

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static org.omnifaces.util.ResourcePaths.PATH_SEPARATOR;
import static org.omnifaces.util.Servlets.getApplicationAttribute;
import static org.omnifaces.util.Utils.encodeURI;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.servlet.ServletContext;

/**
 * Holds the dynamic route segments of {@link FacesViews} and resolves request paths against them.
 * <p>
 * A dynamic route segment is a directory whose name is wrapped in square brackets. It matches exactly one path segment and exposes it under the bracketed name.
 * A view of <code>/organizations/[id]/members.xhtml</code> thus answers to <code>/organizations/123/members</code> and makes <code>123</code> available as
 * <code>id</code>.
 * <p>
 * Unlike a MultiViews view, a dynamic route cannot be expressed as a servlet URL pattern, as the specification has only exact, prefix, extension and default
 * mappings. Dynamic routes are therefore not registered with the container at all and live in this tree instead, which {@link FacesViewsForwardingFilter} walks
 * when the exact match on the mapped resources missed.
 *
 * @author Bauke Scholtz
 * @since 5.5
 * @see FacesViews
 */
final class DynamicRoutes {

    private static final Logger logger = Logger.getLogger(DynamicRoutes.class.getName());

    private static final char PATH_SEPARATOR_CHAR = '/';

    private static final String ATTRIBUTE = "org.omnifaces.facesviews.dynamic_routes";

    private static final String WARNING_BRACKETED_FILE_NAME = "The file name of '%s' is wrapped in square brackets. Only a directory name is interpreted as a dynamic route segment, so this file is scanned"
        + " literally and answers to a URL containing the brackets. Move it into a directory named '[%s]' to make it a dynamic route.";
    private static final String ERROR_AMBIGUOUS_SEGMENT = "Dynamic route segment '[%s]' of view '%s' conflicts with sibling '[%s]'. A directory can have at most one dynamic route segment, as there is no"
        + " defensible way to choose between two of them.";
    private static final String ERROR_DUPLICATE_SEGMENT = "Dynamic route segment '[%s]' occurs more than once in view '%s'. Path parameters are addressed by name, so each name can occur at most once per view.";
    private static final String ERROR_MISSING_SEGMENT_VALUE = "Dynamic route segment '[%s]' of view '%s' has no value. Supply it with <o:pathParam name=\"%1$s\" value=\"...\" /> on the outcome target component,"
        + " else the rendered URL would contain square brackets, which are gen-delims per RFC 3986 that containers may refuse outright.";
    private static final String ERROR_MALFORMED_SEGMENT = "Path segment '%s' of view '%s' contains an unbalanced or empty square bracket pair. Use '[name]' to declare a dynamic route segment.";

    private final Node root = new Node();
    private final Set<String> filterPrefixes = new LinkedHashSet<>();

    /**
     * One path segment of the scanned view tree. A node prefers its literal children over its single dynamic child, which is what makes a literal sibling of a
     * dynamic route segment unreachable as a value of that segment.
     */
    private static final class Node {

        private final Map<String, Node> literals = new LinkedHashMap<>();
        private Node dynamic;
        private String dynamicName;
        private String viewId;
        private boolean multiViews;
        private String welcomeFileViewId;

    }

    /**
     * The outcome of a successful match: the physical view to forward to, the resolved segment values by name, and the remaining path info in case the matched
     * view is a MultiViews view.
     */
    record Match(String viewId, Map<String, String> params, String pathInfo) {
        //
    }

    // Scanning -------------------------------------------------------------------------------------------------------

    /**
     * Returns whether the given path has at least one dynamic route segment, in which case it belongs in this tree rather than in the flat mapped resources, as
     * it is not registrable as a servlet URL pattern. Only a directory name is interpreted as a dynamic route segment, so the last segment, being the file
     * name, is not considered.
     */
    static boolean isDynamicRoute(String path) {
        for (var i = path.indexOf('['); i > -1; i = path.indexOf('[', i + 1)) {
            if (i > 0 && path.charAt(i - 1) != PATH_SEPARATOR_CHAR) {
                continue; // Not at the start of a segment, so not a dynamic route segment.
            }

            var end = path.indexOf(PATH_SEPARATOR_CHAR, i);

            if (end > i + 2 && path.charAt(end - 1) == ']') {
                return true;
            }
        }

        return false;
    }

    private static boolean isDynamicSegment(String segment) {
        return segment.length() > 2 && segment.charAt(0) == '[' && segment.charAt(segment.length() - 1) == ']';
    }

    /**
     * Adds the given extensionless resource to the tree.
     *
     * @param resource The extensionless resource, e.g. <code>/organizations/[id]/members</code>.
     * @param resourcePath The physical path of the view, e.g. <code>/organizations/[id]/members.xhtml</code>.
     * @param multiViews Whether the view is additionally a MultiViews view.
     * @param welcomeFile Whether the resource is a welcome file, in which case it additionally answers for its own directory.
     * @throws IllegalStateException When the resource declares an ambiguous, duplicate or malformed dynamic route segment.
     */
    void add(String resource, String resourcePath, boolean multiViews, boolean welcomeFile) {
        var segments = split(resource);

        if (segments.length == 0) {
            return;
        }

        var node = root;
        Set<String> names = new LinkedHashSet<>();

        for (var i = 0; i < segments.length - 1; i++) {
            var segment = segments[i];
            validate(segment, resource);
            node = isDynamicSegment(segment) ? addDynamic(node, segments, i, names, resource) : node.literals.computeIfAbsent(segment, key -> new Node());
        }

        if (welcomeFile) {
            node.welcomeFileViewId = resourcePath;
        }

        var fileNode = node.literals.computeIfAbsent(segments[segments.length - 1], key -> new Node());
        fileNode.viewId = resourcePath;
        fileNode.multiViews = multiViews;
    }

    /**
     * Adds the dynamic route segment at the given index of the given segments to the given node and returns the node it leads to, collecting the segment name
     * in the given names and registering the filter prefix when it is the first one of the resource.
     *
     * @throws IllegalStateException When the segment is ambiguous against a sibling or duplicate within the same resource.
     */
    private Node addDynamic(Node node, String[] segments, int index, Set<String> names, String resource) {
        var segment = segments[index];
        var name = segment.substring(1, segment.length() - 1);

        if (names.isEmpty()) {
            filterPrefixes.add(PATH_SEPARATOR + String.join(PATH_SEPARATOR, Arrays.copyOfRange(segments, 0, index)) + (index == 0 ? "*" : "/*"));
        }

        if (node.dynamic == null) {
            node.dynamic = new Node();
            node.dynamicName = name;
        }
        else if (!node.dynamicName.equals(name)) {
            throw new IllegalStateException(ERROR_AMBIGUOUS_SEGMENT.formatted(name, resource, node.dynamicName));
        }

        if (!names.add(name)) {
            throw new IllegalStateException(ERROR_DUPLICATE_SEGMENT.formatted(name, resource));
        }

        return node.dynamic;
    }

    /**
     * Returns the URL patterns the forwarding filter must be mapped on to see the requests which may resolve to a dynamic route. A dynamic route itself is not
     * registrable, so this is the nearest static ancestor of each one, e.g. <code>/organizations/*</code> for <code>/organizations/[id]/members</code>.
     */
    Set<String> getFilterPrefixes() {
        return unmodifiableSet(filterPrefixes);
    }

    private static void validate(String segment, String resource) {
        if (isDynamicSegment(segment)) {
            return;
        }

        if (segment.indexOf('[') > -1 || segment.indexOf(']') > -1) {
            throw new IllegalStateException(ERROR_MALFORMED_SEGMENT.formatted(segment, resource));
        }
    }

    /**
     * Logs a warning when the given resource has a bracketed file name, which is scanned literally and would otherwise silently answer to a URL containing
     * square brackets, which are gen-delims that a container may refuse outright.
     */
    static void warnIfBracketedFileName(String resource) {
        var fileName = resource.substring(resource.lastIndexOf(PATH_SEPARATOR) + 1);

        if (isDynamicSegment(fileName)) {
            logger.warning(() -> WARNING_BRACKETED_FILE_NAME.formatted(resource, fileName.substring(1, fileName.length() - 1)));
        }
    }

    boolean isEmpty() {
        return root.literals.isEmpty() && root.dynamic == null;
    }

    // Resolving ------------------------------------------------------------------------------------------------------

    /**
     * Resolves the given request path against the scanned dynamic routes, preferring a literal segment over a dynamic one at every level and backtracking on
     * failure.
     *
     * @param path The extensionless request path, e.g. <code>/organizations/123/members</code>.
     * @return The match, or <code>null</code> when the path does not resolve to any dynamic route.
     */
    Match match(String path) {
        return match(root, split(path), 0, new LinkedHashMap<>());
    }

    private static Match match(Node node, String[] segments, int index, Map<String, String> params) {
        if (index == segments.length) {
            if (node.viewId != null) {
                return new Match(node.viewId, unmodifiableMap(params), null);
            }

            if (node.welcomeFileViewId != null) {
                return new Match(node.welcomeFileViewId, unmodifiableMap(params), null);
            }

            return null;
        }

        var segment = segments[index];
        var literal = node.literals.get(segment);

        if (literal != null) {
            var match = match(literal, segments, index + 1, params);

            if (match != null) {
                return match;
            }
        }

        if (node.dynamic != null) {
            params.put(node.dynamicName, segment);
            var match = match(node.dynamic, segments, index + 1, params);

            if (match != null) {
                return match;
            }

            params.remove(node.dynamicName);
        }

        // The path is not consumed by any deeper view, so a MultiViews view at this level swallows the remainder as path info.
        if (node.viewId != null && node.multiViews) {
            var remainder = Arrays.copyOfRange(segments, index, segments.length);
            return new Match(node.viewId, unmodifiableMap(params), PATH_SEPARATOR + String.join(PATH_SEPARATOR, remainder));
        }

        return null;
    }

    /**
     * Replaces every bracketed segment of the given URI by the value of the same name, so that a dynamic route renders as the URL it actually answers to. Every
     * bracketed segment is substituted, including a trailing one, as a URI which still contains brackets is never correct to render.
     *
     * @param uri The URI or view id, e.g. <code>/organizations/[id]/members</code>.
     * @param values The segment values by name.
     * @return The interpolated URI, e.g. <code>/organizations/123/members</code>.
     * @throws IllegalArgumentException When a segment has no value, as square brackets are gen-delims per RFC 3986 and must never reach a rendered URL.
     */
    static String interpolate(String uri, Map<String, String> values) {
        if (uri.indexOf('[') < 0) {
            return uri;
        }

        var interpolated = new StringBuilder(uri.length());
        var start = 0;

        while (start <= uri.length()) {
            var separator = uri.indexOf(PATH_SEPARATOR_CHAR, start);
            interpolated.append(interpolateSegment(uri.substring(start, separator < 0 ? uri.length() : separator), values, uri));

            if (separator < 0) {
                break;
            }

            interpolated.append(PATH_SEPARATOR_CHAR);
            start = separator + 1;
        }

        return interpolated.toString();
    }

    /**
     * Returns the given segment as-is when it is not a dynamic route segment, else the URI encoded value of the same name.
     *
     * @throws IllegalArgumentException When the segment has no value.
     */
    private static String interpolateSegment(String segment, Map<String, String> values, String uri) {
        if (!isDynamicSegment(segment)) {
            return segment;
        }

        var name = segment.substring(1, segment.length() - 1);
        var value = values == null ? null : values.get(name);

        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(ERROR_MISSING_SEGMENT_VALUE.formatted(name, uri));
        }

        return encodeURI(value);
    }

    private static String[] split(String path) {
        var stripped = path.startsWith(PATH_SEPARATOR) ? path.substring(1) : path;
        return stripped.isEmpty() ? new String[0] : stripped.split(PATH_SEPARATOR);
    }

    // Storage --------------------------------------------------------------------------------------------------------

    void store(ServletContext servletContext) {
        servletContext.setAttribute(ATTRIBUTE, this);
    }

    static DynamicRoutes get(ServletContext servletContext) {
        return getApplicationAttribute(servletContext, ATTRIBUTE);
    }

}
