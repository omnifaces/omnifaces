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
package org.omnifaces.util.selectitems;

import static org.omnifaces.util.Utils.isEmpty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItem;
import javax.faces.component.UISelectItems;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;

import org.omnifaces.el.ScopedRunner;
import org.omnifaces.model.ExtendedSelectItem;
import org.omnifaces.util.Callback;

/**
 * Collection of utility methods for collecting {@link SelectItem} instances from various sources.
 *
 * @author Arjan Tijms
 *
 */
public final class SelectItemsCollector {

	private SelectItemsCollector() {}

	private static final String ERROR_UNKNOWN_SELECT_TYPE	 =
			"A value expression of type '%s' is disallowed for a select item";

	/**
	 * This method gets all select items that are expressed via {@link UISelectItem} or {@link UISelectItems}
	 * children of the given parent component.
	 * <p>
	 * Note that if {@link SelectItemGroup} instances are present then those will be inserted directly in the returned list
	 * and the using code still has to iterate over its children recursively to obtain all separate {@link SelectItem} instances.
	 *
	 * @param parent the parent whose children are scanned
	 * @param context The involved faces context.
	 * @return list of select items obtained from parent's children.
	 */
	public static List<SelectItem> collectFromParent(FacesContext context, UIComponent parent) {

		List<SelectItem> selectItems = new ArrayList<>();

		// Iterate over all children of the parent component. Non-UISelectItem/s children are automatically skipped.
		for (UIComponent child : parent.getChildren()) {

			if (child instanceof UISelectItem) {

				UISelectItem uiSelectItem = (UISelectItem) child;
				Object value = uiSelectItem.getValue();

				if (value instanceof SelectItem) {

					// A single SelectItem can be added directly without any further processing
					selectItems.add((SelectItem)value);
				} else if (uiSelectItem.getValue() == null) {

					// No value binding specified, create a select item out of the properties
					// of the UI component.
					selectItems.add(new ExtendedSelectItem(uiSelectItem));
				} else {

					// A value binding was specified, but of a type we don't support.
					throw new IllegalArgumentException(String.format(ERROR_UNKNOWN_SELECT_TYPE, value.getClass().toString()));
				}

			} else if (child instanceof UISelectItems) {

				UISelectItems uiSelectItems = (UISelectItems) child;
				Object value = uiSelectItems.getValue();

				if (value instanceof SelectItem) {

					// A single SelectItem can be added directly without any further processing
					selectItems.add((SelectItem) value);
				} else if (value instanceof Object[]) {

					// An array of objects is supposed to be transformed by the SelectItems iteration construct
					selectItems.addAll(collectFromUISelectItemsIterator(context, uiSelectItems, Arrays.asList((Object[]) value)));
				} else if (value instanceof Iterable) {

					// An iterable (Collection, List, etc) is also supposed to be transformed by the SelectItems iteration construct
					selectItems.addAll(collectFromUISelectItemsIterator(context, uiSelectItems, (Iterable<?>) value));
				} else if (value instanceof Map) {

					// A map has its own algorithm for how it should be turned into a list of SelectItems
					selectItems.addAll(SelectItemsBuilder.fromMap((Map<?, ?>)value));

				} else {

					// A value binding was specified, but of a type we don't support.
					throw new IllegalArgumentException(String.format(ERROR_UNKNOWN_SELECT_TYPE, value.getClass().toString()));
				}

			}

		}

		return selectItems;
	}

	/**
	 * This method runs the algorithm expressed by a <code>UISelectItems</code> component that uses the <code>var</code> iterator construct to generate
	 * a list of <code>SelectItem</code>s.
	 *
	 * @param uiSelectItems The involved select items component.
	 * @param items The available select items.
	 * @param facesContext The involved faces context.
	 * @return list of <code>SelectItem</code> obtained from the given parameters
	 */
	public static List<SelectItem> collectFromUISelectItemsIterator(FacesContext facesContext, UISelectItems uiSelectItems, Iterable<?> items) {

		final List<SelectItem> selectItems = new ArrayList<>();

		final Map<String, Object> attributes = uiSelectItems.getAttributes();
		String var = (String) attributes.get("var");

		// Helper class that's used to set the item value in (EL) scope using the name set by "var" during the iteration.
		// If during each iteration the value of this is changed, any value expressions in the attribute
		// map referring it will resolve to that particular instance.
		ScopedRunner scopedRunner = new ScopedRunner(facesContext);

		for (final Object item : items) {

			// If the item is already a SelectItem, take it directly.
			// NOTE: I'm not 100% sure if this is right, since it now allows a collection to consist
			// out of a mix of SelectItems and non-SelectItems. Should we maybe always process the iterator
			// if there's a "var", "itemLabel" or "itemValue" present, or should we process the entire collection
			// as SelectItems if the first element is a SelectItem and throw an exception as soon as we encounter
			// a non-SelectItem?
			if (item instanceof SelectItem) {
				selectItems.add((SelectItem)item);
				continue;
			}

			if (!isEmpty(var)) {
				scopedRunner.with(var, item);
			}

			// During each iteration, just resolve all attributes again.
			scopedRunner.invoke(new Callback.Void() {
				@Override
				public void invoke() {
					Object itemValue = getItemValue(attributes, item);
					Object noSelectionValue = attributes.get("noSelectionValue");
					boolean itemValueIsNoSelectionValue = noSelectionValue != null && noSelectionValue.equals(itemValue);

					selectItems.add(new SelectItem(
						itemValue,
						getItemLabel(attributes, itemValue),
						getItemDescription(attributes),
						getBooleanAttribute(attributes, "itemDisabled", false),
						getBooleanAttribute(attributes, "itemLabelEscaped", true),
						getBooleanAttribute(attributes, "noSelectionOption", false) || itemValueIsNoSelectionValue
					));
				}
			});
		}

		return selectItems;
	}

	/**
	 * Gets the optional value. It defaults to the item itself if not specified.
	 *
	 * @param attributes the attributes from which the label is fetched.
	 * @param item default value if no item value present
	 * @return the value, or the item if none is present
	 */
	private static Object getItemValue(Map<String, Object> attributes, Object item) {
		Object itemValue = attributes.get("itemValue");
		if (itemValue == null) {
			itemValue = item;
		}

		return itemValue;
	}

	/**
	 * Gets the optional label. It defaults to the item value if not specified.
	 *
	 * @param attributes the attributes from which the label is fetched.
	 * @param itemValue default value if no item value present
	 * @return the label, or the item value if none present
	 */
	private static String getItemLabel(Map<String, Object> attributes, Object itemValue) {
		Object itemLabelObj = attributes.get("itemLabel");
		String itemLabel = null;
		if (itemLabelObj != null) {
			itemLabel = itemLabelObj.toString();
		} else {
			itemLabel = itemValue.toString();
		}

		return itemLabel;
	}

	/**
	 * Gets the optional description.
	 *
	 * @param attributes the attributes from which the description is fetched.
	 * @return the description, or null if none present.
	 */
	private static String getItemDescription(Map<String, Object> attributes) {
		Object itemDescriptionObj = attributes.get("itemDescription");
		String itemDescription = null;
		if (itemDescriptionObj != null) {
			itemDescription = itemDescriptionObj.toString();
		}

		return itemDescription;
	}

	/**
	 * Gets the name boolean attribute. It defaults to <code>false</code> if not specified.
	 * @param attributes the attributes from which the attribute is fetched.
	 * @param key name of the attribute
	 * @return the boolean represented by the attribute or false if there's no such attribute
	 */
	private static boolean getBooleanAttribute(Map<String, Object> attributes, String key, boolean defaultValue) {
		Object valueObj = attributes.get(key);
		boolean value = defaultValue;
		if (valueObj != null) {
			value = Boolean.parseBoolean(valueObj.toString());
		}

		return value;
	}

}