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

import static org.omnifaces.util.Faces.getMapping;
import static org.omnifaces.util.Faces.isPrefixMapping;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.faces.application.Resource;
import javax.faces.application.ResourceWrapper;

/**
 * This {@link Resource} implementation is extracted from {@link UnmappedResourceHandler} in order to overcome
 * state saving issues.
 *
 * @author Bauke Scholtz
 * @since 1.8
 */
public class UnmappedResource extends ResourceWrapper implements Externalizable {

	private Resource wrapped;

	/**
	 * Constructs a new unmapped resource wrapping the given resource.
	 * @param wrapped The resource to be wrapped.
	 */
	public UnmappedResource(Resource wrapped) {
		this.wrapped = wrapped;
	}

	/**
	 * Obtain the request path of the wrapped resource. If JSF is mapped on a prefix pattern, then get rid of the
	 * prefix pattern on the request path and return it, else if JSF is mapped on a suffix  pattern, then get rid of the
	 * suffix pattern on the request path and return it.
	 */
	@Override
	public String getRequestPath() {
		String path = getWrapped().getRequestPath();
		String mapping = getMapping();

		if (isPrefixMapping(mapping)) {
			return path.replaceFirst(mapping, "");
		}
		else if (path.contains("?")) {
			return path.replace(mapping + "?", "?");
		}
		else {
			return path.substring(0, path.length() - mapping.length());
		}
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