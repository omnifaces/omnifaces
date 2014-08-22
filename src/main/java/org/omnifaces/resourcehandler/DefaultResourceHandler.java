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

import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.application.ResourceHandlerWrapper;

/**
 * A default {@link ResourceHandler} implementation, delegating both {@link #createResource(String)} and
 * {@link #createResource(String, String)} to {@link #createResource(String, String, String)}. Implementors should
 * only need to override {@link #createResource(String, String, String)}.
 *
 * @author Bauke Scholtz
 * @since 2.0
 */
public class DefaultResourceHandler extends ResourceHandlerWrapper {

	// Properties -----------------------------------------------------------------------------------------------------

	private ResourceHandler wrapped;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Creates a new instance of this unmapped resource handler which wraps the given resource handler.
	 * @param wrapped The resource handler to be wrapped.
	 */
	public DefaultResourceHandler(ResourceHandler wrapped) {
		this.wrapped = wrapped;
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Delegate to {@link #createResource(String, String, String)} with <code>null</code> as library name and content
	 * type.
	 */
	@Override
	public Resource createResource(String resourceName) {
		return createResource(resourceName, null, null);
	}

	/**
	 * Delegate to {@link #createResource(String, String, String)} with <code>null</code> as content type.
	 */
	@Override
	public Resource createResource(String resourceName, String libraryName) {
		return createResource(resourceName, libraryName, null);
	}

	@Override
	public ResourceHandler getWrapped() {
		return wrapped;
	}

}