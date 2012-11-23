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

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

import org.omnifaces.util.Faces;
import org.omnifaces.util.Messages;
import org.omnifaces.util.selectitems.SelectItemsUtils;

/**
 * <p>
 * The <strong><code>SelectItemsIndexConverter</code></strong> automatically converts between the index of the select
 * item value and the select item value itself based on its position in the associated select items for the component
 * for which conversion is taking place.
 * <p>
 * This converter has the following advantages over {@link SelectItemsConverter}:
 * <ul>
 * <li>No need to rely on <code>toString()</code> method of the object.</li>
 * <li>No need to extend the {@link SelectItemsConverter} when <code>toString()</code> method of the object cannot be
 * used.</li>
 * <li>No need to expose the object's unique key if that's a problem.</li>
 * </ul>
 * <p>
 * This converter has the following disadvantage over {@link SelectItemsConverter}:
 * <ul>
 * <li>The "Validation Error: value is not valid" will never occur anymore for the case that the available select items
 * has incompatibly changed during the postback due to a developer's mistake. The developer should make absolutely sure
 * that exactly the same list is preserved on postback (e.g. by making it a property of a view scoped or broader scoped
 * bean).</li>
 * </ul>
 * <p>
 * This converter is available by converter ID <code>omnifaces.SelectItemsIndexConverter</code>. Basic usage example:
 * <pre>
 * &lt;h:selectOneMenu value="#{bean.selectedEntity}" converter="omnifaces.SelectItemsIndexConverter"&gt;
 *   &lt;f:selectItems value="#{bean.availableEntities}" var="entity"
 *     itemValue="#{entity}" itemLabel="#{entity.someProperty}" /&gt;
 * &lt;/h:selectOneMenu&gt;
 * </pre>
 *
 * @author Patrick Dobler
 * @author Bauke Scholtz
 * @since 1.3
 */
@FacesConverter("omnifaces.SelectItemsIndexConverter")
public class SelectItemsIndexConverter implements Converter {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String ATTRIBUTE_SELECT_ITEMS = "SelectItemsIndexConverter.%s";

	private static final String ERROR_SELECT_ITEMS_LIST_NULL =
		"Could not determine select items list for component {0}";
	private static final String ERROR_SELECT_ITEMS_LIST_INDEX =
		"Could not determine index for value {0} in component {1}";
	private static final String ERROR_NO_SELECT_ITEM_VALUE_FOUND =
		"No matching select item entry was found for object {0} in component {1}";
	private static final String ERROR_GET_AS_OBJECT =
		"Could not convert value {0} for component {1}";

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String submittedValue) {
		final List<Object> selectItemValues = getSelectItemValues(context, component);

		try {
			return selectItemValues.get(Integer.parseInt(submittedValue));
		} catch (NumberFormatException e) {
			throw new ConverterException(
				Messages.createError(ERROR_SELECT_ITEMS_LIST_INDEX, submittedValue, component.getClientId(context)), e);
		} catch (Exception e) {
			throw new ConverterException(
				Messages.createError(ERROR_GET_AS_OBJECT, submittedValue, component.getClientId(context)), e);
		}
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object modelValue) {
		final List<Object> selectItemValues = getSelectItemValues(context, component);

		for (int i = 0; i < selectItemValues.size(); i++) {
			Object selectItemValue = selectItemValues.get(i);

			if (modelValue == null ? selectItemValue == null : modelValue.equals(selectItemValue)) {
				return Integer.toString(i);
			}
		}

		throw new ConverterException(
			Messages.createError(ERROR_NO_SELECT_ITEM_VALUE_FOUND, modelValue, component.getClientId(context)));
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	private static List<Object> getSelectItemValues(FacesContext context, UIComponent component) {
		String key = String.format(ATTRIBUTE_SELECT_ITEMS, component.getClientId(context));
		List<Object> selectItemValues = Faces.getContextAttribute(key);

		if (selectItemValues == null) {
			selectItemValues = SelectItemsUtils.collectAllValuesFromSelectItems(context, component);
			Faces.setContextAttribute(key, selectItemValues);
		}

		if (selectItemValues == null) {
			throw new ConverterException(
				Messages.createError(ERROR_SELECT_ITEMS_LIST_NULL, component.getClientId(context)));
		}

		return selectItemValues;
	}

}