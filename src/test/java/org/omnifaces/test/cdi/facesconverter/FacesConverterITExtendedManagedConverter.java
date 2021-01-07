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
package org.omnifaces.test.cdi.facesconverter;

import jakarta.faces.application.ResourceDependency;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;

@FacesConverter(value = "facesConverterITExtendedManagedConverter", managed = true) // JSF 2.3-managed
@ResourceDependency(library = "omnifaces.test", name = "facesConverterITExtendedManagedConverterResourceDependency.js", target = "head")
public class FacesConverterITExtendedManagedConverter extends FacesConverterITBaseConverter {

	@Inject
	private FacesConverterITSomeEJB ejb;

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
		return ejb == null ? "null" : ejb.getClass().getSimpleName();
	}
}
