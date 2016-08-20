/*
 * Copyright 2016 OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnifaces.resourcehandler;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.faces.application.Resource;
import javax.faces.application.ResourceWrapper;

/**
 * This {@link Resource} implementation remaps the given wrapped resource to the given request path.
 *
 * @author Bauke Scholtz
 * @since 2.1
 */
public class RemappedResource extends ResourceWrapper implements Externalizable {

	private Resource wrapped;
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
		this.wrapped = wrapped;
		this.requestPath = requestPath;
	}

	@Override
	public String getRequestPath() {
		return requestPath;
	}

	@Override // Necessary because this is missing in ResourceWrapper (will be fixed in JSF 2.2).
	public String getResourceName() {
		return getWrapped().getResourceName();
	}

	@Override // Necessary because this is missing in ResourceWrapper (will be fixed in JSF 2.2).
	public String getLibraryName() {
		return getWrapped().getLibraryName();
	}

	@Override // Necessary because this is missing in ResourceWrapper (will be fixed in JSF 2.2).
	public String getContentType() {
		return getWrapped().getContentType();
	}

	@Override
	public Resource getWrapped() {
		return wrapped;
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
		return wrapped.equals(other.wrapped) && requestPath.equals(other.requestPath);
	}

	@Override
	public int hashCode() {
		return wrapped.hashCode() + requestPath.hashCode();
	}

	@Override
	public void readExternal(ObjectInput input) throws IOException, ClassNotFoundException {
		wrapped = (Resource) input.readObject();
		requestPath = (String) input.readObject();
	}

	@Override
	public void writeExternal(ObjectOutput output) throws IOException {
		output.writeObject(wrapped);
		output.writeObject(requestPath);
	}

}