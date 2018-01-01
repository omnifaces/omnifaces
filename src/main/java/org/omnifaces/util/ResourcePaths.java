/*
 * Copyright 2018 OmniFaces
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

import static java.lang.String.join;
import static java.util.stream.Collectors.toSet;

import java.util.Set;

import javax.servlet.ServletContext;

/**
 * Utility class for working with (Servlet) resource paths, providing methods to handle their prefixes, slashes, extensions etc.
 *
 * @author Arjan Tijms
 * @since 1.4
 */
public final class ResourcePaths {

	/** The path separator, a forward slash <code>/</code>. */
	public static final String PATH_SEPARATOR = "/";

	/** The extension separator, a period <code>.</code>. */
	public static final String EXTENSION_SEPARATOR = ".";

	private ResourcePaths() {
		// Hide constructor
	}

	/**
	 * Checks if the given resource path obtained from {@link ServletContext#getResourcePaths(String)} represents a directory. That is,
	 * if it ends with {@value #PATH_SEPARATOR}.
	 * @param resourcePath The resource path to check.
	 * @return <code>true</code> if the resource path represents a directory, false otherwise.
	 */
	public static boolean isDirectory(String resourcePath) {
		return resourcePath.endsWith(PATH_SEPARATOR);
	}

	/**
	 * Strips the given prefix from the given resource path if any.
	 * @param prefix The prefix to be stripped.
	 * @param resourcePath The resource path to strip the prefix from.
	 * @return The resource without the prefix path, or as-is if it didn't start with this prefix.
	 */
	public static String stripPrefixPath(String prefix, String resourcePath) {
		return resourcePath.startsWith(prefix) ? resourcePath.substring(prefix.length() - 1) : resourcePath;
	}

	/**
	 * Strips the trailing slash(es) from the given resource path if any.
	 * @param resourcePath The resource path to strip the trailing slash from.
	 * @return The resource without the trailing slash, or as-is if it didn't have a trailing slash.
	 * @since 2.6
	 */
	public static String stripTrailingSlash(String resourcePath) {
		return resourcePath.endsWith(PATH_SEPARATOR) ? stripTrailingSlash(resourcePath.substring(0, resourcePath.length() - 1)) : resourcePath;
	}

	/**
	 * Strips the extension from the given resource path if any. This extension is defined as everything after the last occurrence of
	 * a period, including the period itself. E.g. input "index.xhtml" will return "index".
	 * @param resourcePath The resource path to strip the extension from.
	 * @return The resource path without its extension, of as-is if it doesn't have an extension.
	 */
	public static String stripExtension(String resourcePath) {
		int lastPeriod = resourcePath.lastIndexOf(EXTENSION_SEPARATOR);
		return (lastPeriod != -1) ? resourcePath.substring(0, lastPeriod) : resourcePath;
	}

	/**
	 * Gets the extension of given resource path if any. This extension is defined as everything after the last occurrence of a period,
	 * including the period itself. E.g. input "index.xhtml" will return ".xhtml'.
	 * @param resourcePath The resource path to get the extension from.
	 * @return the extension of the resource path, or null if it doesn't have an extension.
	 */
	public static String getExtension(String resourcePath) {
		int lastPeriod = resourcePath.lastIndexOf(EXTENSION_SEPARATOR);
		return (lastPeriod != -1) ? resourcePath.substring(lastPeriod) : null;
	}

	/**
	 * Checks if given resource path is extensionless.
	 * @param resourcePath The resource path to check.
	 * @return <code>true</code> if the resource path is extensionless, false otherwise.
	 */
	public static boolean isExtensionless(String resourcePath) {
		return resourcePath != null && !resourcePath.contains(EXTENSION_SEPARATOR);
	}

	/**
	 * Filters away every resource path in the given set that has an extension.
	 * @param resourcePaths A set of resource paths to be filtered
	 * @return A set where no resource path has an extension. May be empty, but never null.
	 */
	public static Set<String> filterExtension(Set<String> resourcePaths) {
		return resourcePaths.stream().filter(ResourcePaths::isExtensionless).collect(toSet());
	}

	/**
	 * Checks if the given resource path represents the root. That is, if it equals {@value #PATH_SEPARATOR}.
	 * @param resourcePath The resource path to check.
	 * @return <code>true</code> if the resource path represents the root, false otherwise.
	 * @since 3.0
	 */
	public static boolean isRoot(String resourcePath) {
		return resourcePath.equals(PATH_SEPARATOR);
	}

	/**
	 * Add leading slash to given resource path if necessary.
	 * @param resourcePath The resource paths to add leading slash to.
	 * @return Resource path with leading slash.
	 * @since 3.0
	 */
	public static String addLeadingSlashIfNecessary(String resourcePath) {
		return resourcePath.startsWith(PATH_SEPARATOR) ? resourcePath : PATH_SEPARATOR + resourcePath;
	}

	/**
	 * Add trailing slash to given resource path if necessary.
	 * @param resourcePath The resource paths to add trailing slash to.
	 * @return Resource path with trailing slash.
	 * @since 3.0
	 */
	public static String addTrailingSlashIfNecessary(String resourcePath) {
		return resourcePath.endsWith(PATH_SEPARATOR) ? resourcePath : resourcePath + PATH_SEPARATOR;
	}

	/**
	 * Concat given resource paths with the path separator.
	 * @param resourcePaths The resource paths to concat.
	 * @return Concatenated resource paths.
	 * @since 3.0
	 */
	public static String concat(String... resourcePaths) {
		return join(PATH_SEPARATOR, resourcePaths);
	}

}
