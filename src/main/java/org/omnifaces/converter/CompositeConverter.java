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

import static org.omnifaces.util.Utils.csvToList;
import static org.omnifaces.util.Utils.isEmpty;

import java.util.Collections;
import java.util.List;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

import org.omnifaces.config.OmniFaces;
import org.omnifaces.util.FacesLocal;
import org.omnifaces.vdl.FacesAttribute;
import org.omnifaces.vdl.FacesConverterTag;

/**
 * <p>
 * The <code>o:compositeConverter</code> allows multiple converters to be chained together.
 *
 * <h2>Usage</h2>
 * <p>
 * This converter is available by <code>&lt;o:compositeConverter&gt;</code> tag.
 * <pre>
 * &lt;h:inputText value="#{bean.value}"&gt;
 *     &lt;o:compositeConverter converterIds="trimConverter, sanitizeConverter, entityConverter" /&gt;
 * &lt;/h:inputText&gt;
 * </pre>
 *
 * <h2>Execution Order</h2>
 * <p>
 * The converters are executed in the order they are defined in the <code>converterIds</code> attribute during the <strong>getAsObject</strong> and in reverse order during the <strong>getAsString</strong>.
 * <ul>
 * <li><strong>getAsObject</strong>: Executed 1st &rarr; 2nd &rarr; 3rd. The result of the first converter is passed as the input to the next converter.</li>
 * <li><strong>getAsString</strong>: Executed 3rd &rarr; 2nd &rarr; 1st. The result of the last converter is passed as the input to the previous converter to ensure symmetry.</li>
 * </ul>
 *
 * @author Bauke Scholtz
 * @since 5.1
 */
@FacesConverter("omnifaces.CompositeConverter")
@FacesConverterTag(namespace = OmniFaces.OMNIFACES_NAMESPACE)
public class CompositeConverter implements Converter<Object> {

    private String converterIds;

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String submittedValue) {
        Object result = submittedValue;

        for (var converterId : getConverterIds(false)) {
            result = createConverter(context, converterId).getAsObject(context, component, (result == null) ? null : result.toString());
        }

        return result;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object modelValue) {
        Object result = modelValue;

        for (var converterId : getConverterIds(true)) {
            result = createConverter(context, converterId).getAsString(context, component, result);
        }

        return (result == null) ? "" : result.toString();
    }

    private List<String> getConverterIds(boolean reversed) {
        if (isEmpty(converterIds)) {
            throw new IllegalArgumentException("o:compositeConverter converterIds attribute is required");
        }

        var ids = csvToList(converterIds);

        if (reversed) {
            Collections.reverse(ids);
        }

        return ids;
    }

    private static Converter<Object> createConverter(FacesContext context, String converterId) {
        var converter = FacesLocal.createConverter(context, converterId);

        if (converter == null) {
            throw new IllegalArgumentException("o:compositeConverter unknown converter ID: " + converterId);
        }

        return converter;
    }

    /**
     * Sets the comma-separated string of converter IDs.
     * @param converterIds The comma-separated string of converter IDs.
     */
    @FacesAttribute(required = true)
    public void setConverterIds(String converterIds) {
        this.converterIds = converterIds;
    }
}
