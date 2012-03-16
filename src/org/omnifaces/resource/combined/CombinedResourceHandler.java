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
package org.omnifaces.resource.combined;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;

import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.application.ResourceHandlerWrapper;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.omnifaces.util.Utils;

/**
 * This {@link ResourceHandler} implementation recognizes combined resources based on the unique library name as
 * represented by <tt>{@value #LIBRARY_NAME}</tt> and takes care of streaming of them. The
 * webapp developer should make sure that this library name is never used for other libraries.
 * <p>
 * This handler must be registered as follows in <tt>faces-config.xml</tt>:
 * <pre>
 * &lt;application&gt;
 *   &lt;resource-handler&gt;org.omnifaces.resource.combined.CombinedResourceHandler&lt;/resource-handler&gt;
 * &lt;/application&gt;
 * </pre>
 * Don't forget to register the {@link CombinedResourceListener} in <code>&lt;application&gt;</code> as well.
 *
 * @author Bauke Scholtz
 */
public class CombinedResourceHandler extends ResourceHandlerWrapper {

	// Constants ------------------------------------------------------------------------------------------------------

	/** The default library name of a combined resource. Make sure that this is never used for other libraries. */
	public static final String LIBRARY_NAME = "omnifaces.combined";

	// Properties -----------------------------------------------------------------------------------------------------

	private ResourceHandler wrapped;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Creates a new instance of this combined resource handler which wraps the given resource handler.
	 * @param wrapped The resource handler to be wrapped.
	 */
	public CombinedResourceHandler(ResourceHandler wrapped) {
		this.wrapped = wrapped;
	}

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public Resource createResource(String resourceName, String libraryName) {
		if (LIBRARY_NAME.equals(libraryName)) {
			return new CombinedResource(resourceName);
		}
		else {
			return super.createResource(resourceName, libraryName);
		}
	}

	@Override
	public void handleResourceRequest(FacesContext context) throws IOException {
		if (LIBRARY_NAME.equals(context.getExternalContext().getRequestParameterMap().get("ln"))) {
			streamResource(context, new CombinedResource(context));
		}
		else {
			super.handleResourceRequest(context);
		}
	}

	@Override
	public ResourceHandler getWrapped() {
		return wrapped;
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Stream the given resource to the response associated with the given faces context.
	 * @param context The involved faces context.
	 * @param resource The resource to be streamed.
	 * @throws IOException If something fails at I/O level.
	 */
	private static void streamResource(FacesContext context, Resource resource) throws IOException {
		ExternalContext externalContext = context.getExternalContext();

		if (!resource.userAgentNeedsUpdate(context)) {
			externalContext.setResponseStatus(HttpServletResponse.SC_NOT_MODIFIED);
			return;
		}

		InputStream input = resource.getInputStream();

		if (input == null) {
			externalContext.setResponseStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		if (resource.getContentType() != null) {
			externalContext.setResponseContentType(resource.getContentType());
		}

		for (Entry<String, String> header : resource.getResponseHeaders().entrySet()) {
			externalContext.setResponseHeader(header.getKey(), header.getValue());
		}

		Utils.stream(input, externalContext.getResponseOutputStream());
	}

}