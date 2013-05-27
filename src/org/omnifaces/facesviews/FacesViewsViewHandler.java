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
package org.omnifaces.facesviews;

import static org.omnifaces.facesviews.FacesViews.FACES_VIEWS_ORIGINAL_SERVLET_PATH;
import static org.omnifaces.facesviews.FacesViews.FACES_VIEWS_RESOURCES;
import static org.omnifaces.facesviews.FacesViews.getFacesServletExtensions;
import static org.omnifaces.facesviews.FacesViews.isScannedViewsAlwaysExtensionless;
import static org.omnifaces.util.Faces.getApplicationAttribute;
import static org.omnifaces.util.Faces.getRequestAttribute;
import static org.omnifaces.util.ResourcePaths.getExtension;
import static org.omnifaces.util.ResourcePaths.isExtensionless;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.faces.application.ViewHandler;
import javax.faces.application.ViewHandlerWrapper;
import javax.faces.context.FacesContext;

/**
 * View handler that renders action URL extensionless if the current request is extensionless and the requested resource
 * is a mapped one, otherwise as-is.
 * <p>
 * For a guide on FacesViews, please see the <a href="package-summary.html">package summary</a>.
 *
 * @author Arjan Tijms
 *
 */
public class FacesViewsViewHandler extends ViewHandlerWrapper {

	private final ViewHandler wrapped;

	public FacesViewsViewHandler(ViewHandler viewHandler) {
		wrapped = viewHandler;
	}

	@Override
	public String getActionURL(FacesContext context, String viewId) {
		
		String actionURL = super.getActionURL(context, viewId);

		Map<String, String> mappedResources = getApplicationAttribute(context, FACES_VIEWS_RESOURCES);
		if (mappedResources.containsKey(viewId)) {
			
			if (isScannedViewsAlwaysExtensionless(context) || isOriginalViewExtensionless(context)) {
				// User has requested to always render extensionless, or the requested viewId was mapped and the current
				// request is extensionless, render the action URL extensionless as well.
				return removeExtension(context, actionURL, viewId);
			}
		}

		// Not a resource we mapped or not a forwarded one, let the original view handler take care of it.
		return actionURL;
	}
	
	private boolean isOriginalViewExtensionless(FacesContext context) {
		String originalViewId = getRequestAttribute(context, "javax.servlet.forward.servlet_path");
		if (originalViewId == null) {
			originalViewId = getRequestAttribute(context, FACES_VIEWS_ORIGINAL_SERVLET_PATH);
		}
		
		return isExtensionless(originalViewId);
	}
	
	public String removeExtension(FacesContext context, String resource, String viewId) {
		
		Set<String> extensions = getFacesServletExtensions(context);
	    
	    if (!isExtensionless(viewId)) {
	    	String viewIdExtension = getExtension(viewId);
	    	if (!extensions.contains(viewIdExtension)) {
	    		extensions = new HashSet<String>(extensions);
	    		extensions.add(viewIdExtension);
	    	}
	    }
	    
	    int lastSlashPos = resource.lastIndexOf('/');
	    int lastQuestionMarkPos = resource.lastIndexOf('?'); // so we don't remove "extension" from parameter value
	    for (String extension : extensions) {
	    	
	    	int extensionPos = resource.lastIndexOf(extension);
	    	if (extensionPos > lastSlashPos && (lastQuestionMarkPos == -1 || extensionPos < lastQuestionMarkPos)) {
	    		return resource.substring(0, extensionPos) + resource.substring(extensionPos + extension.length());
	    	}
	    	
	    }
	    
		return resource;
	}

	@Override
	public ViewHandler getWrapped() {
		return wrapped;
	}

}