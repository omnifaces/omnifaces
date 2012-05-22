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

import java.net.URL;
import java.util.Map;

import javax.faces.view.facelets.ResourceResolver;

import org.omnifaces.util.Faces;

/**
 * Facelets resource resolver that resolves mapped resources (views) to the special auto-scanned
 * faces-views folder.
 * 
 * @author Arjan Tijms
 *
 */
public class FacesViewsResolver extends ResourceResolver {

    public static final String FACES_VIEWS_RESOURCES_PARAM_NAME = "org.omnifaces.faces-views";

    private final ResourceResolver resourceResolver;

    public FacesViewsResolver(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    @Override
    public URL resolveUrl(String path) {
        
        String facesViewsPath = path;
        
        Map<String, String> mappedResources = Faces.getApplicationAttribute(FacesViewsResolver.FACES_VIEWS_RESOURCES_PARAM_NAME);
        if (mappedResources != null && mappedResources.containsKey(path)) {
            facesViewsPath = mappedResources.get(path);
        }
        
        return resourceResolver.resolveUrl(facesViewsPath);
    }

}
