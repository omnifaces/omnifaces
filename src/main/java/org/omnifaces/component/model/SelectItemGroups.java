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
package org.omnifaces.component.model;

import static org.omnifaces.util.Utils.coalesce;
import static org.omnifaces.util.Utils.forEach;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.el.ValueExpression;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItem;
import javax.faces.component.UISelectItems;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;

import org.omnifaces.el.ScopedRunner;

/**
 * <p>
 * The <code>o:selectItemGroups</code> is an extension of {@link UISelectItems} which allows you to iterate over a
 * nested collection representing groups of select items. This is basically the {@link UIComponent} counterpart of
 * <code>javax.faces.model.SelectItemGroup</code>. There is no equivalent (yet) in the standard JSF API. Currently the
 * only way to represent {@link SelectItemGroup} in UI is to manually create and populate them in a backing bean which
 * can end up to be quite verbose.
 *
 * <h3>Usage</h3>
 * <p>
 * Below example assumes a <code>List&lt;Category&gt;</code> as value wherein <code>Category</code> in turn has a
 * <code>List&lt;Product&gt;</code>.
 * <pre>
 * &lt;h:selectOneMenu value="#{bean.selectedProduct}" converter="omnifaces.SelectItemsConverter"&gt;
 *     &lt;f:selectItem itemValue="#{null}" /&gt;
 *     &lt;o:selectItemGroups value="#{bean.categories}" var="category" itemLabel="#{category.name}"&gt;
 *         &lt;f:selectItems value="#{category.products}" var="product" itemLabel="#{product.name}" /&gt;
 *     &lt;/o:selectItemGroups&gt;
 * &lt;/h:selectOneMenu&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 * @since 3.0
 */
@FacesComponent(SelectItemGroups.COMPONENT_TYPE)
public class SelectItemGroups extends UISelectItems {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The standard component type. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.model.SelectItemGroups";

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String ERROR_EXPRESSION_DISALLOWED =
		"A value expression is disallowed on 'var' attribute of SelectItemGroups.";

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * An override which checks if this isn't been invoked on <code>var</code> attribute.
	 * Finally it delegates to the super method.
	 * @throws IllegalArgumentException When this value expression is been set on <code>var</code> attribute.
	 */
	@Override
	public void setValueExpression(String name, ValueExpression binding) {
		if ("var".equals(name)) {
			throw new IllegalArgumentException(ERROR_EXPRESSION_DISALLOWED);
		}

		super.setValueExpression(name, binding);
	}

	/**
	 * An override which wraps each UISelectItem(s) child in a {@link SelectItemGroup}.
	 */
	@Override
	public Object getValue() {
		List<SelectItemGroup> groups = new ArrayList<>();

		createSelectItems(this, super.getValue(), SelectItemGroup::new, selectItemGroup -> {
			List<SelectItem> items = new ArrayList<>();

			for (UIComponent child : getChildren()) {
				if (child instanceof UISelectItems) {
					createSelectItems(child, ((UISelectItems) child).getValue(), SelectItem::new, items::add);
				}
				else if (child instanceof UISelectItem) {
					items.add(createSelectItem(child, null, SelectItem::new));
				}
			}

			selectItemGroup.setSelectItems(items.toArray(new SelectItem[items.size()]));
			groups.add(selectItemGroup);
		});

		return groups;
	}

	private <S extends SelectItem> void createSelectItems(UIComponent component, Object values, Supplier<S> supplier, Consumer<S> callback) {
		Map<String, Object> attributes = component.getAttributes();
		String var = coalesce((String) attributes.get("var"), "item");
		forEach(values, value -> ScopedRunner.forEach(getFacesContext(), var, value, () -> callback.accept(createSelectItem(component, getItemValue(attributes, value), supplier))));
	}

	private static <S extends SelectItem> S createSelectItem(UIComponent component, Object value, Supplier<S> supplier) {
		Map<String, Object> attributes = component.getAttributes();
		Object itemValue = getItemValue(attributes, value);
		Object itemLabel = attributes.get("itemLabel");
		Object itemLabelEscaped = coalesce(attributes.get("itemEscaped"), attributes.get("itemLabelEscaped")); // f:selectItem || f:selectItems
		Object itemDisabled = attributes.get("itemDisabled");

		S selectItem = supplier.get();
		selectItem.setValue(itemValue);
		selectItem.setLabel(String.valueOf(itemLabel != null ? itemLabel : selectItem.getValue()));
		selectItem.setEscape(itemLabelEscaped == null || Boolean.parseBoolean(itemLabelEscaped.toString()));
		selectItem.setDisabled(itemDisabled != null && Boolean.parseBoolean(itemDisabled.toString()));
		return selectItem;
	}

	/**
	 * Returns item value attribute, taking into account any value expression which actually evaluates to null.
	 */
	private static Object getItemValue(Map<String, Object> attributes, Object defaultValue) {
		Object itemValue = attributes.get("itemValue");
		return itemValue != null || attributes.containsKey("itemValue") ? itemValue : defaultValue;
	}

}