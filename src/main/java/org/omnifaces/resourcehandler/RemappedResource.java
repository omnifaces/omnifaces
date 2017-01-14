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

import static org.omnifaces.util.Faces.getApplication;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Objects;

import javax.faces.application.Resource;
import javax.faces.application.ResourceWrapper;

/**
 * This {@link Resource} implementation remaps the given wrapped resource to the given request path.
 *
 * @author Bauke Scholtz
 * @since 2.1
 */
public class RemappedResource extends ResourceWrapper implements Externalizable {

	private Serializable serializableWrappedResource;
	private transient Resource transientWrappedResource;
	private String resourceName;
	private String libraryName;
	private String requestPath;

	/**
	 * Do not use this constructor.
	 */
	public RemappedResource() {
		// Keep default c'tor alive for Externalizable.
	}

	/**
	 * Constructs a new resource which remaps the given wrapped resource to the given request path.
	 * @param wrapped The resource to be wrapped.
	 * @param requestPath The remapped request path.
	 */
	public RemappedResource(Resource wrapped, String requestPath) {
		if (wrapped instanceof Serializable) {
			serializableWrappedResource = (Serializable) wrapped;
		}
		else if (wrapped != null) {
			transientWrappedResource = wrapped;
			resourceName = wrapped.getResourceName();
			libraryName = wrapped.getLibraryName();
		}

		this.requestPath = requestPath;
	}

	@Override
	public String getRequestPath() {
		return requestPath;
	}

	@Override
	public Resource getWrapped() {
		return getResource();
	}

	private Resource getResource() {
		if (serializableWrappedResource != null) {
			return (Resource) serializableWrappedResource;
		}
		else if (transientWrappedResource == null) {
			transientWrappedResource = getApplication().getResourceHandler().createResource(resourceName, libraryName);
		}

		return transientWrappedResource;
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
		return Objects.equals(getResource(), other.getResource()) && requestPath.equals(other.requestPath);
	}

	@Override
	public int hashCode() {
		return getResource().hashCode() + requestPath.hashCode();
	}

	@Override
	public void readExternal(ObjectInput input) throws IOException, ClassNotFoundException {
		serializableWrappedResource = (Serializable) input.readObject();
		resourceName = (String) input.readObject();
		libraryName = (String) input.readObject();
		requestPath = (String) input.readObject();
	}

	@Override
	public void writeExternal(ObjectOutput output) throws IOException {
		output.writeObject(serializableWrappedResource);
		output.writeObject(resourceName);
		output.writeObject(libraryName);
		output.writeObject(requestPath);
	}

}