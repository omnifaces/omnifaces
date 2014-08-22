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

import static org.omnifaces.util.Faces.getMimeType;

import java.io.IOException;
import java.io.InputStream;

import javax.faces.application.Resource;

/**
 * This {@link Resource} implementation holds all the necessary information about combined resources in order to
 * properly serve combined resources on a single HTTP request.
 * @author Bauke Scholtz
 */
final class CombinedResource extends DynamicResource {

	// Properties -----------------------------------------------------------------------------------------------------

	private CombinedResourceInfo info;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Constructs a new combined resource based on the given resource name. This constructor is only used by
	 * {@link CombinedResourceHandler#createResource(String, String)}.
	 * @param resourceName The resource name of the combined resource.
	 */
	public CombinedResource(String resourceName) {
		super(resourceName, CombinedResourceHandler.LIBRARY_NAME, getMimeType(resourceName));
		String[] resourcePathParts = resourceName.split("\\.", 2)[0].split("/");
		String resourceId = resourcePathParts[resourcePathParts.length - 1];
		info = CombinedResourceInfo.get(resourceId);
		setLastModified(info.getLastModified());
	}

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public InputStream getInputStream() throws IOException {
		if (!info.getResources().isEmpty()) {
			return new CombinedResourceInputStream(info.getResources());
		}
		else {
			return null;
		}
	}

}