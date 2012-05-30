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
package org.omnifaces.resourcehandler;

import java.io.IOException;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.context.FacesContext;

import org.omnifaces.util.Faces;

/**
 * This class is a wrapper which collects all combined resources and stores it in the cache. A builder has been provided
 * to create an instance of combined resource info and put it in the cache if absent.
 * @author Bauke Scholtz
 */
final class CombinedResourceInfo {

	// Constants ------------------------------------------------------------------------------------------------------

	// ConcurrentHashMap was considered, but duplicate inserts technically don't harm and a HashMap is faster on read.
	private static final Map<String, CombinedResourceInfo> CACHE = new HashMap<String, CombinedResourceInfo>();

	private static final String MOJARRA_DEFAULT_RESOURCE_MAX_AGE = "com.sun.faces.defaultResourceMaxAge";
	private static final String MYFACES_DEFAULT_RESOURCE_MAX_AGE = "org.apache.myfaces.RESOURCE_MAX_TIME_EXPIRES";
	private static final long DEFAULT_RESOURCE_MAX_AGE = 604800000L; // 1 week.
	private static final long MAX_AGE =
		initMaxAge(DEFAULT_RESOURCE_MAX_AGE, MOJARRA_DEFAULT_RESOURCE_MAX_AGE, MYFACES_DEFAULT_RESOURCE_MAX_AGE);

	private static final String ERROR_EMPTY_RESOURCE_NAMES =
		"There are no resource names been added. Use add() method to add them or use isEmpty() to check beforehand.";
	private static final String ERROR_CLASH_IN_CACHE =
		"There's a clash in the combined resource info cache!"
			+ "%n\tThe resource ID is: %s"
			+ "%n\tThe one which was already in cache is : %s"
			+ "%n\tThe one which generated the same ID is: %s";
	private static final String ERROR_CREATING_UNIQUE_ID =
		"Cannot create unique resource ID. This platform doesn't support MD5 algorithm and/or UTF-8 charset.";
	private static final String LOG_RESOURCE_NOT_FOUND =
		"CombinedResourceHandler: Resource with library '%s' and name '%s' is not found. Skipping.";

	// Properties -----------------------------------------------------------------------------------------------------

	private String id;
	private Map<String, Set<String>> resourceNames;
	private Set<Resource> resources;
	private int contentLength;
	private long lastModified;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Creates an instance of combined resource info based on the given ID and resource names.
	 * @param resourceNames All resource names, mapped by library name, which are to be combined in a single resource.
	 */
	private CombinedResourceInfo(String id, Map<String, Set<String>> resourceNames) {
		this.id = id;
		this.resourceNames = resourceNames;
	}

	/**
	 * Use this builder to create an instance of combined resource info and put it in the cache if absent.
	 * @author Bauke Scholtz
	 */
	public static final class Builder {

		// Properties -------------------------------------------------------------------------------------------------

		private Map<String, Set<String>> resourceNames = new LinkedHashMap<String, Set<String>>(3);

		// Actions ----------------------------------------------------------------------------------------------------

		/**
		 * Add the the resource represented by the given library name and resource name to the resource names mapping
		 * of this combined resource info. The insertion order is maintained and grouped by library name and duplicates
		 * are filtered.
		 * @param libraryName The library name of the resource to be added.
		 * @param resourceName The resource name of the resource to be added.
		 * @return This builder.
		 */
		public Builder add(String libraryName, String resourceName) {
			Set<String> names = resourceNames.get(libraryName);

			if (names == null) {
				names = new LinkedHashSet<String>(5);
				resourceNames.put(libraryName, names);
			}

			names.add(resourceName);
			return this;
		}

		/**
		 * Returns true if there are no resource names been added. Use this method before {@link #create()} if it's
		 * unknown if there are any resources been added.
		 * @return True if there are no resource names been added, otherwise false.
		 */
		public boolean isEmpty() {
			return resourceNames.isEmpty();
		}

		/**
		 * Creates the CombinedResourceInfo instance, puts it in the cache if absent and returns its ID.
		 * @return The ID of the CombinedResourceInfo instance.
		 * @throws IllegalStateException If there are no resource names been added. So, to prevent it beforehand, use
		 * the {@link #isEmpty()} method to check if there are any resource names been added.
		 */
		public String create() {
			if (resourceNames.isEmpty()) {
				throw new IllegalStateException(ERROR_EMPTY_RESOURCE_NAMES);
			}

			String id = createUniqueId(resourceNames);

			if (!CACHE.containsKey(id)) {
				CombinedResourceInfo info = new CombinedResourceInfo(id, Collections.unmodifiableMap(resourceNames));
				CombinedResourceInfo clash = CACHE.put(id, info);

				if (clash != null && !clash.equals(info)) {
					// We hope that MD5 is good enough that this never occurs. Needs more testing.
					throw new RuntimeException(String.format(ERROR_CLASH_IN_CACHE, id, clash, info));
				}
			}

			return id;
		}

		// Helpers ----------------------------------------------------------------------------------------------------

		/**
		 * Create an unique ID based on the given mapping with a string key and a set of string value. The current
		 * implementation uses a MD5 hash of the {@link Map#toString()} and returns the MD5 bytes as a fixed length
		 * 32-character hexadecimal string.
		 * @param map The map to create an unique ID for.
		 * @return The unique ID of the given map.
		 */
		private static String createUniqueId(Map<String, Set<String>> map) {
			// TODO: I personally don't trust MD5 to be fail-safe. There's still *a* chance on a clash even though it's
			// less than 0.01%. This needs more testing or a different unique ID approach has to be invented.
			// Note that UUID is NOT suitable as the ID needs to be the same every time based on map's content!

			byte[] hash;

			try {
				hash = MessageDigest.getInstance("MD5").digest(map.toString().getBytes("UTF-8"));
			}
			catch (Exception e) {
				// So, MD5 and/or UTF-8 isn't supported. Does such a server platform even exist nowadays?
				throw new RuntimeException(ERROR_CREATING_UNIQUE_ID, e);
			}

			StringBuilder hex = new StringBuilder(hash.length * 2);

			for (byte b : hash) {
				if ((b & 0xff) < 0x10) {
					hex.append("0");
				}

				hex.append(Integer.toHexString(b & 0xff));
			}

			return hex.toString();
		}

	}

	/**
	 * Returns the combined resource info identified by the given ID from the cache.
	 * @param id The ID of the combined resource info to be returned from the cache.
	 * @return The combined resource info identified by the given ID from the cache.
	 */
	public static CombinedResourceInfo get(String id) {
		return CACHE.get(id);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Load the combined resources so that the set of resources, the total content length and the last modified are
	 * been (re)initialized.
	 * @param forceReload Set to true if you want to force reloading of the combined resources, even though the
	 * resources are already been initialized.
	 */
	private synchronized void loadResources(boolean forceReload) {
		if (!forceReload && resources != null) {
			return;
		}

		FacesContext context = FacesContext.getCurrentInstance();
		ResourceHandler handler = context.getApplication().getResourceHandler();
		resources = new LinkedHashSet<Resource>();
		contentLength = 0;
		lastModified = 0;

		for (Entry<String, Set<String>> entry : resourceNames.entrySet()) {
			String libraryName = entry.getKey();

			for (String resourceName : entry.getValue()) {
				Resource resource = handler.createResource(resourceName, libraryName);

				if (resource == null) {
					context.getExternalContext().log(String.format(LOG_RESOURCE_NOT_FOUND, libraryName, resourceName));
					continue;
				}

				resources.add(resource);

				try {
					URLConnection connection = resource.getURL().openConnection();
					contentLength += connection.getContentLength();
					long lastModified = connection.getLastModified();

					if (lastModified > this.lastModified) {
						this.lastModified = lastModified;
					}
				}
				catch (IOException e) {
					// Can't and shouldn't handle it at this point.
					// It would be thrown during resource streaming anyway which is a better moment.
				}
			}
		}
	}

	/**
	 * Returns true if the given object is also an instance of {@link CombinedResourceInfo} and its resource names
	 * mapping equals to the one of the current combined resource info instance.
	 */
	@Override
	public boolean equals(Object other) {
		// Do NOT compare by ID yet!
		return (other instanceof CombinedResourceInfo)
			? ((CombinedResourceInfo) other).resourceNames.equals(resourceNames)
			: false;
	}

	/**
	 * Returns the sum of the hash code of this class and the resource names mapping.
	 */
	@Override
	public int hashCode() {
		// Do NOT calculate hashcode by ID yet!
		return getClass().hashCode() + resourceNames.hashCode();
	}

	/**
	 * Returns the string representation of this combined resource info in the format of
	 * <pre>CombinedResourceInfo[id,resourceNames]</pre>
	 * Where <tt>id</tt> is the unique ID and <tt>resourceNames</tt> is the mapping of all resource names as is been
	 * created with the builder.
	 */
	@Override
	public String toString() {
		return String.format("CombinedResourceInfo[%s,%s]", id, resourceNames);
	}

	// Getters --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the ordered set of resources of this combined resource info.
	 * @return The ordered set of resources of this combined resource info.
	 */
	public Set<Resource> getResources() {
		loadResources(false);
		return resources;
	}

	/**
	 * Returns the content length in bytes of this combined resource info.
	 * @return The content length in bytes of this combined resource info.
	 */
	public int getContentLength() {
		loadResources(false);
		return contentLength;
	}

	/**
	 * Returns the last modified timestamp in milliseconds of this combined resource info. If we're in development
	 * stage, this will forcibly reload the resources.
	 * @return The last modified timestamp in milliseconds of this combined resource info.
	 */
	public long getLastModified() {
		loadResources(Faces.isDevelopment());
		return lastModified;
	}

	/**
	 * Returns the maximum age in milliseconds of this combined resource info.
	 * @return The maximum age in milliseconds of this combined resource info.
	 */
	public long getMaxAge() {
		return MAX_AGE;
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Initializes the maximum age in milliseconds. The first non-null value associated with the given context parameter
	 * names will be returned, else the given default value will be returned.
	 * @param defaultValue The default value.
	 * @param initParameterNames The context parameter names to look for any predefinied maximum age.
	 * @return The maximum age in milliseconds.
	 */
	@SuppressWarnings("unchecked")
	private static long initMaxAge(long defaultValue, String... initParameterNames) {
		Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getInitParameterMap();
		String param = null;

		for (String name : initParameterNames) {
			param = params.get(name);

			if (param != null) {
				break;
			}
		}

		if (param == null || !param.matches("\\d+")) {
			return defaultValue;
		}
		else {
			return Long.valueOf(param);
		}
	}

}