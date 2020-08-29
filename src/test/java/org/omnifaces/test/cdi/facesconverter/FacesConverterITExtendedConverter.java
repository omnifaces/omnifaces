/*
 * Copyright 2020 OmniFaces
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

import javax.enterprise.context.Dependent;
import javax.faces.application.ResourceDependency;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;

@Dependent // OmniFaces-managed
@FacesConverter(value = "facesConverterITExtendedConverter")
@ResourceDependency(library = "omnifaces.test", name = "facesConverterITExtendedConverterResourceDependency.js", target = "head")
public class FacesConverterITExtendedConverter extends FacesConverterITBaseConverter {

	@Inject
	private FacesConverterITSomeEJB ejb;

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
		return ejb == null ? "null" : ejb.getClass().getSimpleName();
	}
}
