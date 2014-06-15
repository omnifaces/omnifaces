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
package org.omnifaces.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

import org.omnifaces.util.selectitems.SelectItemsUtils;

/**
 * Base class for a converter that automatically converts a string representation of an Object back
 * to its original object based on the associated select items for the component for which conversion is
 * taking place.
 * <p>
 * This class can be used as-is provided the Object has a <code>toString</code> method that uniquely identifies it and which
 * makes sense as an identifier. Since this is not the typical case for <code>toString</code>, users are assumed to override
 * the <code>getAsString</code> method and provide an implementation that converts an object to its string representation.
 * This <code>getAsObject</code> method will then be used to automatically do the reverse mapping.
 * <p>
 * This converter is available by converter ID <code>omnifaces.SelectItemsConverter</code>. Basic usage example:
 * <pre>
 * &lt;h:selectOneMenu value="#{bean.selectedEntity}" converter="omnifaces.SelectItemsConverter"&gt;
 *   &lt;f:selectItems value="#{bean.availableEntities}" var="entity"
 *     itemValue="#{entity}" itemLabel="#{entity.someProperty}" /&gt;
 * &lt;/h:selectOneMenu&gt;
 * </pre>
 * <p>
 * When extending this converter to override <code>getAsString()</code> to return something simpler, such as the entity
 * ID by <code>return String.valueOf(((Entity) value).getId());</code>, you only have to change the <code>converter</code>
 * attribute in the above example to refer the extended converter instead.
 *
 * @author Arjan Tijms
 *
 */
@FacesConverter("omnifaces.SelectItemsConverter")
public class SelectItemsConverter implements Converter {

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
		return SelectItemsUtils.findValueByStringConversion(context, component, value, this);
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
		if (value == null) {
			return "";
		}

		return value.toString();
	}

}