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
package org.omnifaces.converter;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

import org.omnifaces.util.selectitems.SelectItemsCollector;
import org.omnifaces.util.selectitems.SelectItemsUtils;

/**
 * <p>
 * The <code>omnifaces.SelectItemsConverter</code> allows you to populate a selection component with complex Java
 * model objects (entities) as value of <code>&lt;f:selectItems&gt;</code> and have Faces convert those
 * automatically back without the need to provide a custom converter which may need to do the job based on
 * possibly expensive service/DAO operations. This converter automatically converts based on the {@link #toString()}
 * of the selected item.
 *
 * <h2>Usage</h2>
 * <p>
 * This converter is available by converter ID <code>omnifaces.SelectItemsConverter</code>. Just specify it in the
 * <code>converter</code> attribute of the selection component holding <code>&lt;f:selectItems&gt;</code>.
 * <pre>
 * &lt;h:selectOneMenu value="#{bean.selectedItem}" converter="omnifaces.SelectItemsConverter"&gt;
 *     &lt;f:selectItems value="#{bean.availableItems}" /&gt;
 * &lt;/h:selectOneMenu&gt;
 * </pre>
 * <p>
 * Since OmniFaces 4.5 it's also available by <code>&lt;o:selectItemsConverter&gt;</code> tag.
 * <pre>
 * &lt;h:selectOneMenu value="#{bean.selectedItem}"&gt;
 *     &lt;f:selectItems value="#{bean.availableItems}" /&gt;
 *     &lt;o:selectItemsConverter /&gt;
 * &lt;/h:selectOneMenu&gt;
 * </pre>
 *
 * <h2>Make sure that your entity has a good <code>toString()</code> implementation</h2>
 * <p>
 * The base converter uses by default the <code>toString()</code> method of the entity to uniquely identify
 * the instance during the conversion. This is sufficient if your (abstract base) entity has a
 * <code>toString()</code> implementation which looks something like this:
 * <pre>
 * &#64;Override
 * public String toString() {
 *     return String.format("%s[id=%d]", getClass().getSimpleName(), getId());
 * }
 * </pre>
 * <p>
 * By the way, <a href="https://stackoverflow.com/a/17343582/157882">you should also make sure that your entity
 * has a good <code>equals()</code> and <code>hashCode()</code> implementation</a>, otherwise Faces won't be able
 * to set the right entity back in the model. Please note that this problem is in turn unrelated to the
 * <code>SelectItemsConverter</code>, you would have faced the same problem when using any other converter.
 *
 * <h2>If your entity can't have a good <code>toString()</code> implementation</h2>
 * <p>
 * However, if the entity doesn't have a <code>toString()</code> implementation (and thus relies on the default
 * <code>fqn@hashcode</code> implementation), or the existing implementation doesn't necessarily uniquely
 * identify the instance, and you can't implement/change it, then it is recommended to extend the
 * <code>SelectItemsConverter</code> class and override <b>only</b> the <code>getAsString</code> method wherein
 * the desired implementation is provided. For example:
 * <pre>
 * &#64;FacesConverter("exampleEntitySelectItemsConverter")
 * public class ExampleEntitySelectItemsConverter extends SelectItemsConverter {
 *
 *     &#64;Override
 *     public String getAsString(FacesContext context, UIComponent component, Object value) {
 *         Long id = (value instanceof ExampleEntity) ? ((ExampleEntity) value).getId() : null;
 *         return (id != null) ? String.valueOf(id) : null;
 *     }
 *
 * }
 * </pre>
 * <p>
 * Again, you do <strong>not</strong> need to override the <code>getAsObject()</code> method which would only
 * need to perform possibly expensive service/DAO operations. The <code>SelectItemsConverter</code> base
 * converter will already do it automatically based on the available items and the <code>getAsString()</code>
 * implementation.
 * <p>
 * An alternative is to switch to {@link SelectItemsIndexConverter}, which will convert based on the position (index)
 * of the selected item in the list instead of the {@link #toString()} of the selected item.
 *
 * @author Arjan Tijms
 * @see SelectItemsUtils
 * @see SelectItemsCollector
 */
@FacesConverter("omnifaces.SelectItemsConverter")
public class SelectItemsConverter implements Converter<Object> {

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