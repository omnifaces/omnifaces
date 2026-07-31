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
package org.omnifaces.test.facesviews.dynamicroutes;

import java.io.Serializable;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.omnifaces.cdi.Param;

@Named
@RequestScoped
public class DynamicRoutesITBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    @Param(pathName = "id")
    private String id;

    @Inject
    @Param(pathName = "locale")
    private String locale;

    @Inject
    @Param(pathName = "sku")
    private String sku;

    @Inject
    @Param(pathIndex = 0)
    private String pathIndex0;

    /**
     * Proves that a named path parameter goes through the same conversion pipeline as any other, here via the converter registered for {@link Product}.
     */
    @Inject
    @Param(pathName = "sku")
    private Product product;

    public String getId() {
        return id;
    }

    public String getLocale() {
        return locale;
    }

    public String getSku() {
        return sku;
    }

    public String getPathIndex0() {
        return pathIndex0;
    }

    public Product getProduct() {
        return product;
    }

}
