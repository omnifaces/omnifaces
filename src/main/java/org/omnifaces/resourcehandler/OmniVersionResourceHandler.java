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

import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;

import org.omnifaces.config.OmniFaces;

/**
 * Appends OmniFaces version to resources with library name <code>omnifaces</code>.
 *
 * @author Bauke Scholtz
 * @since 2.2
 */
public class OmniVersionResourceHandler extends DefaultResourceHandler {

	private static final String LIBRARY_NAME = "omnifaces";
	private final String version;

	public OmniVersionResourceHandler(ResourceHandler wrapped) {
		super(wrapped);
		version = "&v=" + (OmniFaces.isSnapshot() ? OmniFaces.getStartupTime() : OmniFaces.getVersion());
	}

	@Override
	public Resource decorateResource(Resource resource) {
		if (resource == null || !LIBRARY_NAME.equals(resource.getLibraryName())) {
			return resource;
		}

		return new RemappedResource(resource, resource.getRequestPath() + version);
	}

}