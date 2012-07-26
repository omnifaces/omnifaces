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
import java.net.URL;
import java.net.URLConnection;
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
import org.omnifaces.util.Utils;

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

	private static final String RICHFACES_RESOURCE_OPTIMIZATION_ENABLED = "org.richfaces.resourceOptimization.enabled";

	/**
	 * RichFaces "resource optimization" do not support getURL() and getInputStream(). The combined resource handler
	 * has to manually create the URL based on getRequestPath() and the current request domain URL whenever RichFaces
	 * "resource optimization" is enabled. This field is package private because CombinedResourceInputStream also need
	 * to know about this.
	 */
	static final boolean ENABLE_RF_RES_HACK =
		Boolean.valueOf(Faces.getInitParameter(RICHFACES_RESOURCE_OPTIMIZATION_ENABLED));

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

		// Constants --------------------------------------------------------------------------------------------------

		private static final String ERROR_EMPTY_RESOURCE_NAMES =
			"There are no resource names been added. Use add() method to add them or use isEmpty() to check beforehand.";

		// Properties -------------------------------------------------------------------------------------------------

		private Map<String, Set<String>> resourceNames = new LinkedHashMap<String, Set<String>>(3);

		// Actions ----------------------------------------------------------------------------------------------------

		/**
		 * Add the resource represented by the given library name and resource name to the resource names mapping of
		 * this combined resource info. The insertion order is maintained and grouped by library name and duplicates
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
		 * Creates the CombinedResourceInfo instance in cache if absent and return its ID.
		 * @return The ID of the CombinedResourceInfo instance.
		 * @throws IllegalStateException If there are no resource names been added. So, to prevent it beforehand, use
		 * the {@link #isEmpty()} method to check if there are any resource names been added.
		 */
		public String create() {
			if (resourceNames.isEmpty()) {
				throw new IllegalStateException(ERROR_EMPTY_RESOURCE_NAMES);
			}

			return get(toUniqueId(resourceNames)).id;
		}

	}

	/**
	 * Returns the combined resource info identified by the given ID from the cache. A new one will be created based on
	 * the given ID if absent in cache.
	 * @param id The ID of the combined resource info to be returned from the cache.
	 * @return The combined resource info identified by the given ID from the cache.
	 */
	public static CombinedResourceInfo get(String id) {
		CombinedResourceInfo info = CACHE.get(id);

		if (info == null) {
			Map<String, Set<String>> resources = fromUniqueId(id);

			if (resources != null) {
				info = new CombinedResourceInfo(id, Collections.unmodifiableMap(resources));
				CACHE.put(id, info);
			}
		}

		return info;
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Load the combined resources so that the set of resources, the total content length and the last modified are
	 * been (re)initialized. If one of the resources cannot be resolved, then this leaves the resources empty.
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
					resources.clear();
					return;
				}

				resources.add(resource);

				try {
					URLConnection connection = !ENABLE_RF_RES_HACK
						? resource.getURL().openConnection()
						: new URL(Faces.getRequestDomainURL() + resource.getRequestPath()).openConnection();

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
	 * Returns true if the given object is also an instance of {@link CombinedResourceInfo} and its ID equals to the
	 * ID of the current combined resource info instance.
	 */
	@Override
	public boolean equals(Object other) {
		return (other instanceof CombinedResourceInfo)
			? ((CombinedResourceInfo) other).id.equals(id)
			: false;
	}

	/**
	 * Returns the sum of the hash code of this class and the ID.
	 */
	@Override
	public int hashCode() {
		return getClass().hashCode() + id.hashCode();
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

	// Helpers ----------------------------------------------------------------------------------------------------

	/**
	 * Create an unique ID based on the given mapping of resources. The current implementation converts the mapping to
	 * an delimited string which is serialized using {@link Utils#serialize(String)}.
	 * @param resources The mapping of resources to create an unique ID for.
	 * @return The unique ID of the given mapping of resources.
	 */
	private static String toUniqueId(Map<String, Set<String>> resources) {
		StringBuilder resourcesId = new StringBuilder();

		for (Entry<String, Set<String>> entry : resources.entrySet()) {
			if (resourcesId.length() > 0) {
				resourcesId.append('|');
			}

			String library = entry.getKey();

			if (library != null) {
				resourcesId.append(library);
			}

			for (String name : entry.getValue()) {
				resourcesId.append(':').append(name);
			}

			// Note: the characters | and : are chosen so because they're not allowed in Windows/Unix filenames anyway.
			// This saves us from the need to quote them individually which would only make the ID larger.
		}

		return Utils.serializeURLSafe(resourcesId.toString());
	}

	/**
	 * Create a mapping of resources based on the given unique ID. This does the reverse of {@link #toUniqueId(Map)}.
	 * @param id Te unique ID of the mapping of resources.
	 * @return The mapping of resources based on the given unique ID, or <code>null</code> if the ID is not valid.
	 */
	private static Map<String, Set<String>> fromUniqueId(String id) {
		String resourcesId;

		try {
			resourcesId = Utils.unserializeURLSafe(id);
		}
		catch (IllegalArgumentException e) {
			// This will occur when the ID has purposefully been manipulated for some reason.
			// Just return null then so that it will end up in a 404.
			return null;
		}

		Map<String, Set<String>> resources = new LinkedHashMap<String, Set<String>>();

		for (String libraries : resourcesId.split("\\|")) {
			String[] libraryAndNames = libraries.split(":");
			String library = libraryAndNames[0];
			Set<String> names = new LinkedHashSet<String>();

			for (int i = 1; i < libraryAndNames.length; i++) {
				names.add(libraryAndNames[i]);
			}

			resources.put(library.isEmpty() ? null : library, names);
		}

		return resources;
	}

}