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
package org.omnifaces.converter;

import static java.lang.String.format;
import static org.omnifaces.util.Faces.getContextAttribute;
import static org.omnifaces.util.Faces.setContextAttribute;
import static org.omnifaces.util.Messages.createError;
import static org.omnifaces.util.Utils.isEmpty;

import java.util.List;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.convert.FacesConverter;

import org.omnifaces.util.selectitems.SelectItemsCollector;
import org.omnifaces.util.selectitems.SelectItemsUtils;

/**
 * <p>
 * The <code>omnifaces.SelectItemsIndexConverter</code> is a variant of the {@link SelectItemsConverter} which
 * automatically converts based on the position (index) of the selected item in the list instead of the
 * {@link #toString()} of the selected item.
 *
 * <h2>Usage</h2>
 * <p>
 * This converter is available by converter ID <code>omnifaces.SelectItemsIndexConverter</code>. Just specify it in the
 * <code>converter</code> attribute of the selection component holding <code>&lt;f:selectItems&gt;</code>.
 * <pre>
 * &lt;h:selectOneMenu value="#{bean.selectedItem}" converter="omnifaces.SelectItemsIndexConverter"&gt;
 *     &lt;f:selectItems value="#{bean.availableItems}" /&gt;
 * &lt;/h:selectOneMenu&gt;
 * </pre>
 *
 * <h2>Pros and cons as compared to {@link SelectItemsConverter}</h2>
 * <p>
 * This converter has the following advantages over {@link SelectItemsConverter}:
 * <ul>
 * <li>No need to rely on {@link #toString()} method of the object.</li>
 * <li>No need to extend the {@link SelectItemsConverter} when {@link #toString()} method of the object cannot be
 * used.</li>
 * <li>No need to expose the object's unique key in its {@link #toString()},if that's a problem.</li>
 * </ul>
 * <p>
 * This converter has the following disadvantage over {@link SelectItemsConverter}:
 * <ul>
 * <li>The "Validation Error: value is not valid" will never occur anymore for the case that the available select items
 * has incompatibly changed during the postback due to a developer's mistake. The developer should make absolutely sure
 * that exactly the same list is preserved on postback (e.g. by making it a property of a view scoped or broader scoped
 * bean).</li>
 * </ul>
 *
 * @author Patrick Dobler
 * @author Bauke Scholtz
 * @since 1.3
 * @see SelectItemsUtils
 * @see SelectItemsCollector
 */
@FacesConverter("omnifaces.SelectItemsIndexConverter")
public class SelectItemsIndexConverter implements Converter<Object> {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String ATTRIBUTE_SELECT_ITEMS = "SelectItemsIndexConverter.%s";

	private static final String ERROR_SELECT_ITEMS_LIST_INDEX =
		"Could not determine index for value ''{0}'' in component {1}.";
	private static final String ERROR_GET_AS_OBJECT =
		"Could not convert value ''{0}'' for component {1}.";

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String submittedValue) {
		if (isEmpty(submittedValue)) {
			return null; // Work around for MyFaces 2.0.x bug.
		}

		List<Object> selectItemValues = SelectItemsUtils.collectAllValuesFromSelectItems(context, component);

		try {
			return selectItemValues.get(Integer.parseInt(submittedValue));
		}
		catch (NumberFormatException e) {
			throw new ConverterException(
				createError(ERROR_SELECT_ITEMS_LIST_INDEX, submittedValue, component.getClientId(context)), e);
		}
		catch (Exception e) {
			throw new ConverterException(
				createError(ERROR_GET_AS_OBJECT, submittedValue, component.getClientId(context)), e);
		}
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object modelValue) {
		String key = format(ATTRIBUTE_SELECT_ITEMS, component.getClientId(context));
		List<Object> selectItemValues = getContextAttribute(key);

		if (selectItemValues == null) {
			selectItemValues = SelectItemsUtils.collectAllValuesFromSelectItems(context, component);
			setContextAttribute(key, selectItemValues); // Cache it as it's a rather expensive job.
		}

		for (int i = 0; i < selectItemValues.size(); i++) {
			Object selectItemValue = selectItemValues.get(i);

			if (isEmpty(modelValue) ? isEmpty(selectItemValue) : modelValue.equals(selectItemValue)) {
				return Integer.toString(i);
			}
		}

		return "";
	}

}