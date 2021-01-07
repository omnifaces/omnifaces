/*
 * Copyright 2021 OmniFaces
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

import static org.omnifaces.util.Faces.getRequestParameter;
import static org.omnifaces.util.Faces.getRequestParameterValues;

import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;

/**
 * <p>
 * This {@link ResourceHandler} implementation deals with {@link GraphicResource} requests.
 *
 * @author Bauke Scholtz
 * @since 2.0
 */
public class GraphicResourceHandler extends DefaultResourceHandler {

	// Constants ------------------------------------------------------------------------------------------------------

	/** The default library name of a graphic resource. Make sure that this is never used for other libraries. */
	public static final String LIBRARY_NAME = "omnifaces.graphic";

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Creates a new instance of this graphic resource handler which wraps the given resource handler.
	 * @param wrapped The resource handler to be wrapped.
	 */
	public GraphicResourceHandler(ResourceHandler wrapped) {
		super(wrapped);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns {@link #LIBRARY_NAME}.
	 */
	@Override
	public String getLibraryName() {
		return LIBRARY_NAME;
	}

	/**
	 * Returns a new {@link GraphicResource}.
	 */
	@Override
	public Resource createResourceFromLibrary(String resourceName, String contentType) {
		return new GraphicResource(resourceName, getRequestParameterValues("p"), getRequestParameter("v"));
	}

}