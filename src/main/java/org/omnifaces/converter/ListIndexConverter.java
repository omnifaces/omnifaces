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

import static org.omnifaces.util.Messages.createError;

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;
import javax.faces.model.SelectItem;

/**
 * <p>
 * The <code>omnifaces.ListIndexConverter</code> is a variant of the {@link ListConverter} which automatically converts
 * based on the position (index) of the selected item in the list instead of the {@link #toString()} of the selected
 * item.
 *
 * <h3>Usage</h3>
 * <p>
 * This converter is available by converter ID <code>omnifaces.ListIndexConverter</code> and should be used in
 * combination with <code>&lt;o:converter&gt;</code> in order to be able to pass the {@link List} source to it, which it
 * can use for conversion. Here's a basic usage example with PrimeFaces <code>&lt;p:pickList&gt;</code>, which is one of
 * the few select components which doesn't use {@link SelectItem}s as the source, but work directly via a {@link List}.
 * <pre>
 * &lt;p:pickList value="#{bean.dualListModel}" var="entity" itemValue="#{entity}" itemLabel="#{entity.someProperty}"&gt;
 *     &lt;o:converter converterId="omnifaces.ListIndexConverter" list="#{bean.dualListModel.source}" /&gt;
 * &lt;/p:pickList&gt;
 * </pre>
 *
 * <h3>Pros and cons as compared to {@link ListConverter}</h3>
 * <p>
 * For detail, refer the javadoc of {@link SelectItemsIndexConverter} and substitute
 * "<code>SelectItemsIndexConverter</code>" by "<code>ListIndexConverter</code>" and "<code>SelectItemsConverter</code>"
 * by "<code>ListConverter</code>".
 *
 * @author Arjan Tijms
 */
@FacesConverter("omnifaces.ListIndexConverter")
public class ListIndexConverter implements Converter {

	private static final String ERROR_LIST_INDEX =
			"Could not determine index for value ''{0}'' in component {1}.";

	private static final String ERROR_LIST_INDEX_BOUNDS =
			"Index {0} for value {1} in component {2} is out of bounds.";

	private static final String ERROR_VALUE_NOT_IN_LIST =
			"Object {0} in component {1} does not appear to be present in the given list.";

	private List<?> list;

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
		int index;

		try {
			index = Integer.valueOf(value);
		}
		catch (NumberFormatException e) {
			throw new ConverterException(
				createError(ERROR_LIST_INDEX, value, component.getClientId(context)), e);
		}

		if (index < 0 || index >= list.size()) {
			throw new ConverterException(
				createError(ERROR_LIST_INDEX_BOUNDS, index, value, component.getClientId(context))
			);
		}

		return list.get(index);
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).equals(value)) {
				return i + "";
			}
		}

		throw new ConverterException(
			createError(ERROR_VALUE_NOT_IN_LIST, value == null ? "null" : value.toString(), component.getClientId(context))
		);
	}

	public void setList(List<?> list) {
		this.list = list;
	}

}