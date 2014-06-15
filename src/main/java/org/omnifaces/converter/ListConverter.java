/*
 * Copyright 2013 OmniFaces.
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

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 * Base class for a converter that automatically converts a string representation of an Object back
 * to its original object based on objects in a given list for which conversion is taking place.
 * <p>
 * This is a variant of the {@link SelectItemsConverter}. See its Javadoc for further documentation.
 * </p>
 * 
 * @since 1.5
 * @author Arjan Tijms
 */
@FacesConverter("omnifaces.ListConverter")
public class ListConverter implements Converter {

	private List<?> list;

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
		for (Object listValue : list) {
			String convertedListValue = getAsString(context, component, listValue);
			if (value == null ? convertedListValue == null : value.equals(convertedListValue)) {
				return listValue;
			}
		}

		return null;
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
		if (value == null) {
			return null;
		}

		return value.toString();
	}

	public void setList(List<?> list) {
		this.list = list;
	}

}