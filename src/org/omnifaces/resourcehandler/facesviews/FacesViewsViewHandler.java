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

package org.omnifaces.resourcehandler.facesviews;

import static org.omnifaces.resourcehandler.facesviews.FacesViewsResolver.FACES_VIEWS_RESOURCES_PARAM_NAME;
import static org.omnifaces.resourcehandler.facesviews.FacesViewsUtils.getApplicationAttribute;
import static org.omnifaces.resourcehandler.facesviews.FacesViewsUtils.getRequestAttribute;
import static org.omnifaces.resourcehandler.facesviews.FacesViewsUtils.isExtensionless;
import static org.omnifaces.resourcehandler.facesviews.FacesViewsUtils.stripExtension;

import java.util.Map;

import javax.faces.application.ViewHandler;
import javax.faces.application.ViewHandlerWrapper;
import javax.faces.context.FacesContext;

/**
 * View handler that renders action URL extensionless if the current request is extensionless and the
 * requested resource is a mapped one, otherwise as-is.
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
        
        Map<String, String> mappedResources = getApplicationAttribute(context, FACES_VIEWS_RESOURCES_PARAM_NAME);
        if (mappedResources.containsKey(viewId)) {
            
            String originalViewId = getRequestAttribute(context, "javax.servlet.forward.servlet_path");
            if (isExtensionless(originalViewId)) {
                // Requested viewId was mapped and the current request is extensionless, render the
                // action URL extensionless as well.
                return context.getExternalContext().getRequestContextPath() + stripExtension(viewId); 
            }
        }
        
        // Not a resource we mapped or not a forwarded one, let the original view handler take care of it.
        return super.getActionURL(context, viewId);
    }
    
    @Override
    public ViewHandler getWrapped() {
       return wrapped;
    }
    
}
