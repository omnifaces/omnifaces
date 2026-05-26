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

import static java.lang.Boolean.TRUE;
import static org.omnifaces.util.Utils.coalesce;
import static org.omnifaces.util.Utils.forEach;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

import jakarta.el.ValueExpression;
import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UISelectItem;
import jakarta.faces.component.UISelectItems;
import jakarta.faces.model.SelectItem;
import jakarta.faces.model.SelectItemGroup;

import org.omnifaces.config.OmniFaces;
import org.omnifaces.el.ScopedRunner;
import org.omnifaces.util.Faces;
import org.omnifaces.vdl.FacesAttribute;

/**
 * <p>
 * The <code>o:selectItemGroups</code> is an extension of {@link UISelectItems} which allows you to iterate over a nested collection representing groups of
 * select items. This is basically the {@link UIComponent} counterpart of <code>jakarta.faces.model.SelectItemGroup</code>.
 *
 * <h2>Usage</h2>
 * <p>
 * Below example assumes a <code>List&lt;Category&gt;</code> as value wherein <code>Category</code> in turn has a <code>List&lt;Product&gt;</code>.
 *
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
 * @deprecated Since OmniFaces 5.4. Use the standard <code>&lt;f:selectItemGroups&gt;</code> instead, which was introduced in Faces 4.0 and is functionally
 * equivalent.
 */
@Deprecated(since = "5.4", forRemoval = true)
@FacesComponent(value = SelectItemGroups.COMPONENT_TYPE, namespace = OmniFaces.OMNIFACES_NAMESPACE)
public class SelectItemGroups extends UISelectItems {

    // Public constants -----------------------------------------------------------------------------------------------

    /** The component type, which is {@value org.omnifaces.component.model.SelectItemGroups#COMPONENT_TYPE}. */
    public static final String COMPONENT_TYPE = "org.omnifaces.component.model.SelectItemGroups";

    // Private constants ----------------------------------------------------------------------------------------------

    private static final Logger logger = Logger.getLogger(SelectItemGroups.class.getName());

    private static final String ERROR_EXPRESSION_DISALLOWED = "A value expression is disallowed on 'var' attribute of SelectItemGroups.";
    private static final String DEPRECATION_WARNING_LOGGED = SelectItemGroups.class.getName() + ".DEPRECATION_WARNING_LOGGED";
    private static final String DEPRECATION_WARNING = "o:selectItemGroups is deprecated and will be removed in a future version."
        + " Please migrate to the standard f:selectItemGroups, which was introduced in Faces 4.0 and is functionally equivalent."
        + " First encountered in view: %s";

    private enum PropertyKeys {

        // Cannot be uppercased. They have to exactly match the attribute names.
        VAR,
        itemLabel,
        itemValue;

        @Override
        public String toString() {
            return this == VAR ? name().toLowerCase() : name();
        }

    }

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * An override which checks if this isn't been invoked on <code>var</code> attribute. Finally it delegates to the super method.
     *
     * @throws IllegalArgumentException When this value expression is been set on <code>var</code> attribute.
     */
    @Override
    public void setValueExpression(String name, ValueExpression binding) {
        if (PropertyKeys.VAR.toString().equals(name)) {
            throw new IllegalArgumentException(ERROR_EXPRESSION_DISALLOWED);
        }

        super.setValueExpression(name, binding);
    }

    /**
     * An override which wraps each UISelectItem(s) child in a {@link SelectItemGroup}.
     */
    @Override
    public Object getValue() {
        logDeprecationWarningOnce();
        List<SelectItemGroup> groups = new ArrayList<>();

        createSelectItems(this, super.getValue(), SelectItemGroup::new, selectItemGroup -> {
            List<SelectItem> items = new ArrayList<>();

            for (var child : getChildren()) {
                if (child instanceof UISelectItems selectItems) {
                    createSelectItems(child, selectItems.getValue(), SelectItem::new, items::add);
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

    private static void logDeprecationWarningOnce() {
        if (Faces.getApplicationMap().putIfAbsent(DEPRECATION_WARNING_LOGGED, TRUE) == null) {
            logger.warning(String.format(DEPRECATION_WARNING, Faces.getViewId()));
        }
    }

    private <S extends SelectItem> void createSelectItems(UIComponent component, Object values, Supplier<S> supplier, Consumer<S> callback) {
        Map<String, Object> attributes = component.getAttributes();
        var varName = coalesce((String) attributes.get("var"), "item");
        forEach(
            values, value -> ScopedRunner.forEach(
                getFacesContext(), varName, value,
                () -> callback.accept(createSelectItem(component, getItemValue(attributes, value), supplier))
            )
        );
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

    // Attribute getters/setters --------------------------------------------------------------------------------------

    /**
     * Returns the name of the EL variable which exposes the currently iterated select item group. Defaults to {@code item}.
     *
     * @return The name of the EL variable.
     */
    public String getVar() {
        return (String) getStateHelper().eval(PropertyKeys.VAR);
    }

    /**
     * Sets the name of the EL variable which exposes the currently iterated select item group. Defaults to {@code item}.
     *
     * @param varName The name of the EL variable.
     */
    public void setVar(String varName) {
        getStateHelper().put(PropertyKeys.VAR, varName);
    }

    /**
     * Returns the label of the select item group. Defaults to the String representation of {@code itemValue}.
     *
     * @return The label of the select item group.
     */
    public String getItemLabel() {
        return (String) getStateHelper().eval(PropertyKeys.itemLabel);
    }

    /**
     * Sets the label of the select item group. Defaults to the String representation of {@code itemValue}.
     *
     * @param itemLabel The label of the select item group.
     */
    public void setItemLabel(String itemLabel) {
        getStateHelper().put(PropertyKeys.itemLabel, itemLabel);
    }

    /**
     * Returns the value of the select item group. This will be exposed to any nested UISelectItem(s) children. Defaults to the currently iterated select item
     * group.
     *
     * @return The value of the select item group.
     */
    public Object getItemValue() {
        return getStateHelper().eval(PropertyKeys.itemValue);
    }

    /**
     * Sets the value of the select item group. This will be exposed to any nested UISelectItem(s) children. Defaults to the currently iterated select item
     * group.
     *
     * @param itemValue The value of the select item group.
     */
    public void setItemValue(Object itemValue) {
        getStateHelper().put(PropertyKeys.itemValue, itemValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @FacesAttribute(required = true)
    public void setValue(Object value) {
        super.setValue(value);
    }

}
