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
package org.omnifaces.test.cdi.facesconverter;

import jakarta.enterprise.context.Dependent;
import jakarta.faces.application.ResourceDependency;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;

@Dependent // OmniFaces-managed
@FacesConverter(value = "facesConverterITExtendedConverter")
@ResourceDependency(library = "omnifaces.test", name = "facesConverterITExtendedConverterResourceDependency.js", target = "head")
public class FacesConverterITExtendedConverter extends FacesConverterITBaseConverter {

    @Inject
    private FacesConverterITSomeService service;

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        return service == null ? "null" : service.getClass().getSimpleName();
    }
}
