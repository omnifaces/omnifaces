/*
 * Copyright 2014 OmniFaces.
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
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.faces.application.Resource;
import javax.faces.application.ResourceWrapper;

/**
 * A default {@link Resource} implementation, fixing broken {@link ResourceWrapper} and implementing
 * {@link Externalizable} to avoid JSF state saving trouble.
 *
 * @author Bauke Scholtz
 * @since 2.0
 */
public abstract class DefaultResource extends ResourceWrapper implements Externalizable {

	public static final String RES_NOT_FOUND = "RES_NOT_FOUND";

	private Resource wrapped;

	/**
	 * Constructs a new default resource.
	 */
	public DefaultResource() {
		// Keep default c'tor alive for Externalizable.
	}

	/**
	 * Constructs a new default resource wrapping the given resource.
	 * @param wrapped The resource to be wrapped.
	 */
	public DefaultResource(Resource wrapped) {
		this.wrapped = wrapped;
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
	public void readExternal(ObjectInput input) throws IOException, ClassNotFoundException {
		wrapped = (Resource) input.readObject();
	}

	@Override
	public void writeExternal(ObjectOutput output) throws IOException {
		output.writeObject(wrapped);
	}

}