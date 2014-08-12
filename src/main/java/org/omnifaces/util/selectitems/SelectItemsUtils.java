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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;

/**
 * Collection of utility methods for working with {@link SelectItem} or data represented by them.
 *
 * @author Arjan Tijms
 *
 */
public final class SelectItemsUtils {

	private SelectItemsUtils() {}

	/**
	 * Finds an object value in the {@link SelectItem} instances associated with the given component by means of matching its converted value with
	 * the given string value.
	 *
	 * @param context The involved faces context.
	 * @param component the component with which {@link SelectItem}s should be associated that are used to search in.
	 * @param value a string that should match the string representation of one of the values held by the {@link SelectItem}s.
	 * @param converter the faces {@link Converter} used to generate String representations for the values held by the {@link SelectItem}s.
	 *
	 * @return the Object representation of the value where its string representation matches the input value.
	 */
	public static Object findValueByStringConversion(FacesContext context, UIComponent component, String value, Converter converter) {
		return findValueByStringConversion(context, component, SelectItemsCollector.collectFromParent(context, component).iterator(), value, converter);
	}

	private static Object findValueByStringConversion(FacesContext context, UIComponent component, Iterator<SelectItem> items, String value, Converter converter) {
		while (items.hasNext()) {
			SelectItem item = items.next();
			if (item instanceof SelectItemGroup) {
				SelectItem subitems[] = ((SelectItemGroup) item).getSelectItems();
				if (!isEmpty(subitems)) {
					Object object = findValueByStringConversion(context, component, new ArrayIterator(subitems), value, converter);
					if (object != null) {
						return object;
					}
				}
			} else if (!item.isNoSelectionOption()) {
				Object itemValue = item.getValue();
				String convertedItemValue = converter.getAsString(context, component, itemValue);
				if (value == null ? convertedItemValue == null : value.equals(convertedItemValue)) {
					return itemValue;
				}
			}
		}

		return null;
	}

	/**
	 * Collects all values associated with all {@link SelectItem} instances associated with the given component.
	 * <p>
	 * Note that values from recursively scanned {@link SelectItemGroup} instances are included.
	 *
	 * @param context The involved faces context.
	 * @param component the component with which {@link SelectItem} instances should be associated
	 * @return List of all values hold by {@link SelectItem} instances
	 */
	public static List<Object> collectAllValuesFromSelectItems(FacesContext context, UIComponent component) {
		List<Object> values = new ArrayList<Object>();
		collect(SelectItemsCollector.collectFromParent(context, component).iterator(), values);

		return values;
	}

	private static void collect(Iterator<SelectItem> items, List<Object> values) {
		while (items.hasNext()) {
			SelectItem item = items.next();
			if (item instanceof SelectItemGroup) {
				SelectItem subitems[] = ((SelectItemGroup) item).getSelectItems();
				if (!isEmpty(subitems)) {
					collect(new ArrayIterator(subitems), values);
				}
			} else if (!item.isNoSelectionOption()) {
				values.add(item.getValue());
			}
		}
	}

	/**
	 * Exposes an Array via an <code>Iterator</code>
	 */
	static class ArrayIterator implements Iterator<SelectItem> {

		public ArrayIterator(SelectItem items[]) {
			this.items = items;
		}

		private SelectItem items[];
		private int index = 0;

		@Override
		public boolean hasNext() {
			return (index < items.length);
		}

		@Override
		public SelectItem next() {
			try {
				return (items[index++]);
			}
			catch (IndexOutOfBoundsException e) {
				throw new NoSuchElementException();
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

}
