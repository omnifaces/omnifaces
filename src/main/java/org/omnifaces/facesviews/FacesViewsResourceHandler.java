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
package org.omnifaces.facesviews;

import static org.omnifaces.facesviews.FacesViews.getMappedPath;
import static org.omnifaces.facesviews.FacesViews.scanAndStoreViews;
import static org.omnifaces.util.Faces.getServletContext;
import static org.omnifaces.util.Faces.isDevelopment;

import jakarta.faces.application.ResourceHandler;
import jakarta.faces.application.ResourceHandlerWrapper;
import jakarta.faces.application.ViewResource;
import jakarta.faces.context.FacesContext;

import org.omnifaces.ApplicationProcessor;

/**
 * Facelets resource handler that resolves mapped resources (views) to the folders from which
 * those views were scanned (like the the special auto-scanned faces-views folder).
 * <p>
 * For a guide on FacesViews, please see the <a href="package-summary.html">package summary</a>.
 *
 * @author Arjan Tijms
 * @see FacesViews
 * @see ApplicationProcessor
 */
public class FacesViewsResourceHandler extends ResourceHandlerWrapper {

	/**
	 * Construct faces views resource handler.
	 * @param wrapped The resource handler to be wrapped.
	 */
	public FacesViewsResourceHandler(ResourceHandler wrapped) {
		super(wrapped);
	}

	@Override
	public ViewResource createViewResource(FacesContext context, String path) {
		ViewResource resource = super.createViewResource(context, getMappedPath(path));

		if (resource == null && isDevelopment()) {
			// If resource is null it means it wasn't found.
			// Check if the resource was dynamically added by scanning the faces-views location(s) again.
			scanAndStoreViews(getServletContext(), false);
			resource = super.createViewResource(context, getMappedPath(path));
		}

		return resource;
	}

}