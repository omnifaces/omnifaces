/*
 * Copyright 2017 OmniFaces
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

import java.util.Map;

import javax.faces.component.UIComponent;

/**
 * Convenience class to represent a resource identifier.
 *
 * @author Bauke Scholtz
 * @since 1.3
 */
public class ResourceIdentifier {

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

}