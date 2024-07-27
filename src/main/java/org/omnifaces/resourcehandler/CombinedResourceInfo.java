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
package org.omnifaces.resourcehandler;

import static java.lang.String.format;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.WARNING;
import static org.omnifaces.resourcehandler.CombinedResourceHandler.LIBRARY_NAME;
import static org.omnifaces.util.FacesLocal.createResource;
import static org.omnifaces.util.FacesLocal.isDevelopment;
import static org.omnifaces.util.Utils.isEmpty;
import static org.omnifaces.util.Utils.openConnection;
import static org.omnifaces.util.Utils.serializeURLSafe;
import static org.omnifaces.util.Utils.unserializeURLSafe;

import java.net.URLConnection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.faces.application.Resource;
import javax.faces.context.FacesContext;

import org.omnifaces.el.functions.Converters;
import org.omnifaces.util.Utils;

/**
 * <p>
 * This class is a wrapper which collects all combined resources and stores it in the cache. A builder has been provided
 * to create an instance of combined resource info and put it in the cache if absent.
 *
 * @author Bauke Scholtz
 */
public final class CombinedResourceInfo {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final Logger logger = Logger.getLogger(CombinedResourceInfo.class.getName());

	private static final Map<String, CombinedResourceInfo> CACHE = new ConcurrentHashMap<>();

	private static final String LOG_RESOURCE_NOT_FOUND = "CombinedResourceHandler: The resource %s cannot be found"
			+ " and therefore a 404 will be returned for the combined resource ID %s";

	// Properties -----------------------------------------------------------------------------------------------------

	private String id;
	private Set<ResourceIdentifier> resourceIdentifiers;
	private Set<Resource> resources;
	private int contentLength;
	private long lastModified;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Creates an instance of combined resource info based on the given ID and ordered set of resource identifiers.
	 * @param resourceIdentifiers Ordered set of resource identifiers, which are to be combined in a single resource.
	 */
	private CombinedResourceInfo(String id, Set<ResourceIdentifier> resourceIdentifiers) {
		this.id = id;
		this.resourceIdentifiers = resourceIdentifiers;
	}

	/**
	 * Use this builder to create an instance of combined resource info and put it in the cache if absent.
	 * @author Bauke Scholtz
	 */
	public static final class Builder {

		// Constants --------------------------------------------------------------------------------------------------

		private static final String ERROR_EMPTY_RESOURCES =
			"There are no resources been added. Use add() method to add them or use isEmpty() to check beforehand.";

		// Properties -------------------------------------------------------------------------------------------------

		private Set<ResourceIdentifier> resourceIdentifiers = new LinkedHashSet<>();

		// Actions ----------------------------------------------------------------------------------------------------

		/**
		 * Add the resource represented by the given resource identifier resources of this combined resource info. The
		 * insertion order is maintained and duplicates are filtered.
		 * @param resourceIdentifier The resource identifier of the resource to be added.
		 * @return This builder.
		 */
		public Builder add(ResourceIdentifier resourceIdentifier) {
			resourceIdentifiers.add(resourceIdentifier);
			return this;
		}

		/**
		 * Returns true if there are no resources been added. Use this method before {@link #create()} if it's unknown
		 * if there are any resources been added.
		 * @return True if there are no resources been added, otherwise false.
		 */
		public boolean isEmpty() {
			return resourceIdentifiers.isEmpty();
		}

		/**
		 * Creates the CombinedResourceInfo instance in cache if absent and return its ID.
		 * @return The ID of the CombinedResourceInfo instance.
		 * @throws IllegalStateException If there are no resources been added. So, to prevent it beforehand, use
		 * the {@link #isEmpty()} method to check if there are any resources been added.
		 */
		public String create() {
			if (resourceIdentifiers.isEmpty()) {
				throw new IllegalStateException(ERROR_EMPTY_RESOURCES);
			}

			String id = toUniqueId(resourceIdentifiers);

			if (!CACHE.containsKey(id)) {
				CombinedResourceInfo.create(id, resourceIdentifiers);
			}

			return id;
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
			Set<ResourceIdentifier> resourceIdentifiers = fromUniqueId(id);

			if (resourceIdentifiers != null) {
				info = create(id, resourceIdentifiers);
			}
		}

		return info;
	}

	/**
	 * Create new combined resource info identified by given ID in the cache.
	 * @param id The ID of the combined resource info to be created in the cache.
	 * @param resourceIdentifiers The set of resource identifiers to create combined resource info for.
	 * @return New combined resource info identified by given ID.
	 */
	private static CombinedResourceInfo create(String id, Set<ResourceIdentifier> resourceIdentifiers) {
		CombinedResourceInfo info = new CombinedResourceInfo(id, Collections.unmodifiableSet(resourceIdentifiers));
		CACHE.put(id, info);
		return info;
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Lazily load the combined resources so that the set of resources, the total content length and the last modified
	 * are been initialized. If one of the resources cannot be resolved, then this will log a WARNING and leave the
	 * resources empty.
	 */
	private synchronized void loadResources() {
		FacesContext context = FacesContext.getCurrentInstance();

		if (!isEmpty(resources) && !isDevelopment(context)) {
			return;
		}

		long previousLastModified = lastModified;
		resources = new LinkedHashSet<>();
		contentLength = 0;
		lastModified = 0;

		for (ResourceIdentifier resourceIdentifier : resourceIdentifiers) {
			Resource resource = createResource(context, resourceIdentifier.getLibrary(), resourceIdentifier.getName());

			if (resource == null) {
				if (logger.isLoggable(WARNING)) {
					logger.log(WARNING, format(LOG_RESOURCE_NOT_FOUND, resourceIdentifier, id));
				}

				resources.clear();
				return;
			}

			resources.add(resource);
			URLConnection connection = openConnection(context, resource);

			if (connection == null) {
				return;
			}

			contentLength += connection.getContentLength();
			long resourceLastModified = connection.getLastModified();

			if (resourceLastModified > lastModified) {
				lastModified = resourceLastModified;
			}
		}

		if (previousLastModified != 0 && lastModified != previousLastModified) {
			String keyPrefix = LIBRARY_NAME + ":" + id + ".";
			ResourceIdentifier.clearIntegrity(key -> key.startsWith(keyPrefix));
		}
	}

	/**
	 * Returns true if the given object is also an instance of {@link CombinedResourceInfo} and its ID equals to the
	 * ID of the current combined resource info instance.
	 */
	@Override
	public boolean equals(Object other) {
		return (other instanceof CombinedResourceInfo) && ((CombinedResourceInfo) other).id.equals(id);
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
	 * <pre>CombinedResourceInfo[id,resourceIdentifiers]</pre>
	 * Where <code>id</code> is the unique ID and <code>resourceIdentifiers</code> is the ordered set of all resource
	 * identifiers as is been created with the builder.
	 */
	@Override
	public String toString() {
		return format("CombinedResourceInfo[%s,%s]", id, resourceIdentifiers);
	}

	// Getters --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the ordered set of resource identifiers of this combined resource info.
	 * @return the ordered set of resource identifiers of this combined resource info.
	 */
	public Set<ResourceIdentifier> getResourceIdentifiers() {
		return resourceIdentifiers;
	}

	/**
	 * Returns the ordered set of resources of this combined resource info.
	 * @return The ordered set of resources of this combined resource info.
	 */
	public Set<Resource> getResources() {
		loadResources();
		return resources;
	}

	/**
	 * Returns the content length in bytes of this combined resource info.
	 * @return The content length in bytes of this combined resource info.
	 */
	public int getContentLength() {
		loadResources();
		return contentLength;
	}

	/**
	 * Returns the last modified timestamp in milliseconds of this combined resource info.
	 * @return The last modified timestamp in milliseconds of this combined resource info.
	 */
	public long getLastModified() {
		loadResources();
		return lastModified;
	}

	// Helpers ----------------------------------------------------------------------------------------------------

	/**
	 * Create an unique ID based on the given set of resource identifiers. The current implementation converts the
	 * set to a <code>|</code>-delimited string which is serialized using {@link Utils#serialize(String)}.
	 * @param resourceIdentifiers The set of resource identifiers to create an unique ID for.
	 * @return The unique ID of the given set of resource identifiers.
	 */
	private static String toUniqueId(Set<ResourceIdentifier> resourceIdentifiers) {
		return serializeURLSafe(Converters.joinCollection(resourceIdentifiers, "|"));
	}

	/**
	 * Create an ordered set of resource identifiers based on the given unique ID. This does the reverse of
	 * {@link #toUniqueId(Map)}.
	 * @param id The unique ID of the set of resource identifiers.
	 * @return The set of resource identifiers based on the given unique ID, or <code>null</code> if the ID is not
	 * valid.
	 */
	private static Set<ResourceIdentifier> fromUniqueId(String id) {
		String resourcesId;

		try {
			resourcesId = unserializeURLSafe(id);
		}
		catch (IllegalArgumentException ignore) {
			logger.log(FINEST, "Ignoring thrown exception; this can only be a hacker attempt, just return null to indicate 404.", ignore);
			return null;
		}

		Set<ResourceIdentifier> resourceIdentifiers = new LinkedHashSet<>();

		for (String resourceIdentifier : resourcesId.split("\\|")) {
			resourceIdentifiers.add(new ResourceIdentifier(resourceIdentifier));
		}

		return resourceIdentifiers;
	}

}