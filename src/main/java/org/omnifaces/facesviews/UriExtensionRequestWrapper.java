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
package org.omnifaces.facesviews;

import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.MappingMatch;

/**
 * This wraps a request to an extensionless Faces view and provides an extension for
 * all methods that reveal the servlet path. Additional the path info is set to null.
 * <p>
 * This is needed since Faces implementations inspect the request to determine if a
 * prefix (path) or suffix (extension) mapping was used. If the request is neither
 * (in effect, an "exact and extensionless mapping), Faces will get confused and not
 * be able to derive view IDs etc correctly.
 * <p>
 * For a guide on FacesViews, please see the <a href="package-summary.html">package summary</a>.
 *
 * @author Arjan Tijms
 * @since 1.4
 * @see FacesViews
 * @see FacesViewsForwardingFilter
 */
public class UriExtensionRequestWrapper extends HttpServletRequestWrapper {

    private final String servletPath;
    private final HttpServletMapping mapping;

    /**
     * Construct the URI extension request wrapper.
     * @param request The request to be wrapped.
     * @param servletPath The involved servlet path.
     */
    public UriExtensionRequestWrapper(HttpServletRequest request, String servletPath) {
        super(request);
        this.servletPath = servletPath;

        String[] parts = servletPath.split("\\.", 2);
        final String pattern = "*." + parts[1];
        final String matchValue = parts[0];
        final String servletName = request.getHttpServletMapping().getServletName();

        this.mapping = new HttpServletMapping() {

            @Override
            public String getServletName() {
                return servletName;
            }

            @Override
            public String getPattern() {
                return pattern;
            }

            @Override
            public String getMatchValue() {
                return matchValue;
            }

            @Override
            public MappingMatch getMappingMatch() {
                return MappingMatch.EXTENSION;
            }
        };
    }

    @Override
    public String getServletPath() {
        return servletPath;
    }

    @Override
    public String getPathInfo() {
        // Since we simulate that the request is mapped to an extension and not to a prefix path, there can be no path info.
        return null;
    }

    @Override
    public HttpServletMapping getHttpServletMapping() {
        return mapping;
    }

}