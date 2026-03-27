/*
 * Copyright OmniFaces
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
package org.omnifaces.test;

import static java.lang.Boolean.parseBoolean;
import static org.omnifaces.util.Faces.getRequestParameter;

import jakarta.faces.application.Resource;
import jakarta.faces.application.ResourceHandler;

import org.omnifaces.resourcehandler.CDNResource;
import org.omnifaces.resourcehandler.DefaultResourceHandler;

public class CustomCDNResourceHandler extends DefaultResourceHandler {

    public CustomCDNResourceHandler(ResourceHandler wrapped) {
        super(wrapped);
    }

    @Override
    public Resource decorateResource(Resource resource, String resourceName, String libraryName) {
        var skipCDN = parseBoolean(getRequestParameter("skipCDN")); // Just for testing! In real world you'd not supply this as a request parameter.

        if (skipCDN || resource == null) {
            return resource;
        }

        return new CDNResource(resource, resource.getRequestPath()); // Just a dummy CDN resource for testing! In real world you'd prepend CDN host to the
                                                                     // request path.
    }

}
