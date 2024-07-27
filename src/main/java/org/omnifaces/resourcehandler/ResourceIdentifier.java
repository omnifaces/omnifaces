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
import static java.util.logging.Level.WARNING;
import static org.omnifaces.util.FacesLocal.createResource;
import static org.omnifaces.util.Utils.toByteArray;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.logging.Logger;

import javax.faces.application.Resource;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

/**
 * Convenience class to represent a resource identifier.
 *
 * @author Bauke Scholtz
 * @since 1.3
 */
public class ResourceIdentifier {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final Logger logger = Logger.getLogger(ResourceIdentifier.class.getName());

	private static final Map<String, String> INTEGRITIES = new ConcurrentHashMap<>();

	private static final String WARNING_CANNOT_COMPUTE_INTEGRITY =
		"Cannot compute integrity for %s; defaulting to empty string";

	// Properties -----------------------------------------------------------------------------------------------------

	private String library;
	private String name;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Create a new instance based on given standard JSF resource identifier string format <code>library:name</code>.
	 * @param resourceIdentifier The standard JSF resource identifier.
	 */
	public ResourceIdentifier(String resourceIdentifier) {
		String[] parts = resourceIdentifier.split(":");
		setLibraryAndName((parts.length > 1) ? parts[0] : null, parts[parts.length - 1]);
	}

	/**
	 * Create a new instance based on library and name attributes of the given component resource.
	 * @param componentResource The component resource.
	 */
	public ResourceIdentifier(UIComponent componentResource) {
		Map<String, Object> attributes = componentResource.getAttributes();
		setLibraryAndName((String) attributes.get("library"), (String) attributes.get("name"));
	}

	/**
	 * Create a new instance based on given resource library and name.
	 * @param library The resource lirbary.
	 * @param name The resource name.
	 */
	public ResourceIdentifier(String library, String name) {
		setLibraryAndName(library, name);
	}

	/**
	 * Create a new instance based on given resource.
	 * @param resource The resource.
	 * @since 3.13
	 */
	public ResourceIdentifier(Resource resource) {
		setLibraryAndName(resource.getLibraryName(), resource.getResourceName());
	}

	private void setLibraryAndName(String library, String name) {
		this.library = library;
		this.name = (name != null) ? name.split("[?#;]", 2)[0] : null; // Split gets rid of query string and path fragment.
	}

	// Getters --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the resource library.
	 * @return The resource library.
	 */
	public String getLibrary() {
		return library;
	}

	/**
	 * Returns the resource name.
	 * @return The resource name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the resource integrity as base64 encoded sha384 hash.
	 * This is lazily computed and will return an empty string when the integrity could not be computed.
	 * The reason for the compute failure will be logged as WARNING.
	 * @return The resource integrity as base64 encoded sha384 hash.
	 * @since 3.13
	 */
	public String getIntegrity(FacesContext context) {
		return INTEGRITIES.computeIfAbsent(toString(), k -> computeIntegrity(context, this));
	}

	// Object overrides -----------------------------------------------------------------------------------------------

	@Override
	public boolean equals(Object object) {
		// Basic checks.
		if (object == this) {
			return true;
		}
		if (object == null || object.getClass() != getClass()) {
			return false;
		}

		// Property checks.
		ResourceIdentifier other = (ResourceIdentifier) object;
		if (library == null ? other.library != null : !library.equals(other.library)) {
			return false;
		}
		if (name == null ? other.name != null : !name.equals(other.name)) {
			return false;
		}

		// All passed.
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((library == null) ? 0 : library.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/**
	 * Returns the resource identifier as string in standard JSF resource identifier format <code>library:name</code>.
	 * If there is no library, then only the name is returned without the colon separator like so <code>name</code>.
	 */
	@Override
	public String toString() {
		return (library != null ? (library + ":") : "") + name;
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	private static String computeIntegrity(FacesContext context, ResourceIdentifier id) {
		try {
			byte[] content = toByteArray(createResource(context, id).getInputStream());
			byte[] sha384 = MessageDigest.getInstance("SHA-384").digest(content);
			return "sha384-" + Base64.getEncoder().encodeToString(sha384);
		}
		catch (Exception e) {
			logger.log(WARNING, format(WARNING_CANNOT_COMPUTE_INTEGRITY, id), e);
			return "";
		}
	}

	static void clearIntegrity(Predicate<String> keyPredicate) {
		INTEGRITIES.keySet().removeIf(keyPredicate::test);
	}
}