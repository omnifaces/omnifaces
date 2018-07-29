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
package org.omnifaces.resourcehandler;

import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import javax.faces.FacesException;
import javax.faces.application.Resource;
import javax.faces.application.ResourceWrapper;
import javax.faces.context.FacesContext;

/**
 * This {@link Resource} implementation remaps the given resource to the given request path.
 *
 * @author Bauke Scholtz
 * @since 2.1
 */
public class RemappedResource extends ResourceWrapper implements Externalizable {

	private Serializable serializableResource;
	private transient Resource resource;
	private String resourceName;
	private String libraryName;
	private String requestPath;

	/**
	 * Do not use this constructor. It's merely there for {@link Externalizable}.
	 */
	@SuppressWarnings("deprecation")
	public RemappedResource() {
		// Keep default c'tor alive for Externalizable.
	}

	/**
	 * Constructs a new resource which remaps the given wrapped resource to the given request path.
	 * @param resource The resource to be remapped.
	 * @param requestPath The remapped request path.
	 */
	public RemappedResource(Resource resource, String requestPath) {
		super(resource);

		if (resource instanceof Serializable) {
			serializableResource = (Serializable) resource;
		}

		this.resource = resource;
		this.requestPath = requestPath;
	}

	/**
	 * Constructs a new resource which remaps the given requested resource and library name to the given request path.
	 * @param resourceName The requested resource name.
	 * @param libraryName The requested library name.
	 * @param requestPath The remapped request path.
	 */
	public RemappedResource(String resourceName, String libraryName, String requestPath) {
		super(null);
		this.resourceName = resourceName;
		this.libraryName = libraryName;
		this.requestPath = requestPath;
	}

	@Override
	public Resource getWrapped() {
		return resource;
	}

	@Override
	public String getResourceName() {
		Resource wrapped = getWrapped();
		return (wrapped != null) ? wrapped.getResourceName() : resourceName;
	}

	@Override
	public String getLibraryName() {
		Resource wrapped = getWrapped();
		return (wrapped != null) ? wrapped.getLibraryName() : libraryName;
	}

	@Override
	public String getContentType() {
		Resource wrapped = getWrapped();
		return (wrapped != null) ? wrapped.getContentType() : null;
	}

	@Override
	public String getRequestPath() {
		return requestPath;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		Resource wrapped = getWrapped();
		return (wrapped != null) ? wrapped.getInputStream() : getURL().openStream();
	}

	@Override
	public Map<String, String> getResponseHeaders() {
		Resource wrapped = getWrapped();
		return (wrapped != null) ? wrapped.getResponseHeaders() : Collections.<String, String>emptyMap();
	}

	@Override
	public URL getURL() {
		try {
			Resource wrapped = getWrapped();
			return (wrapped != null) ? wrapped.getURL() : new URL(requestPath);
		}
		catch (MalformedURLException e) {
			throw new FacesException(e);
		}
	}

	@Override
	public boolean userAgentNeedsUpdate(FacesContext context) {
		Resource wrapped = getWrapped();
		return (wrapped != null) ? wrapped.userAgentNeedsUpdate(context) : false;
	}

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}

		if (object == null || getClass() != object.getClass()) {
			return false;
		}

		RemappedResource other = (RemappedResource) object;
		Resource wrapped = getWrapped();
		return Objects.equals(wrapped, other.getWrapped()) && requestPath.equals(other.requestPath);
	}

	@Override
	public int hashCode() {
		Resource wrapped = getWrapped();
		return Objects.hash(wrapped, requestPath);
	}

	@Override
	public void readExternal(ObjectInput input) throws IOException, ClassNotFoundException {
		serializableResource = (Serializable) input.readObject();
		resource = (Resource) serializableResource;
		resourceName = (String) input.readObject();
		libraryName = (String) input.readObject();
		requestPath = (String) input.readObject();
	}

	@Override
	public void writeExternal(ObjectOutput output) throws IOException {
		output.writeObject(serializableResource);
		output.writeObject(resourceName);
		output.writeObject(libraryName);
		output.writeObject(requestPath);
	}

}