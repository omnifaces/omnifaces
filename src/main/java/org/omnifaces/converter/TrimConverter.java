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

import static org.omnifaces.util.Utils.coalesce;
import static org.omnifaces.util.Utils.isEmpty;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 * <p>
 * The <code>omnifaces.TrimConverter</code> is intented to trim any whitespace from submitted {@link String} values.
 * This keeps the data store free of whitespace pollution.
 * <p>
 * This converter does by design no conversion in <code>getAsString()</code>.
 *
 * <h3>Usage</h3>
 * <p>
 * This converter is available by converter ID <code>omnifaces.TrimConverter</code>. Just specify it in the
 * <code>converter</code> attribute of the component referring the <code>String</code> property. For example:
 * <pre>
 * &lt;h:inputText value="#{bean.username}" converter="omnifaces.TrimConverter" /&gt;
 * </pre>
 * <p>
 * You can also configure it application wide via below entry in <code>faces-config.xml</code> without the need to
 * specify it in every single input component:
 * <pre>
 * &lt;converter&gt;
 *     &lt;converter-for-class&gt;java.lang.String&lt;/converter-for-class&gt;
 *     &lt;converter-class&gt;org.omnifaces.converter.TrimConverter&lt;/converter-class&gt;
 * &lt;/converter&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 * @since 2.6
 */
@FacesConverter("omnifaces.TrimConverter")
public class TrimConverter implements Converter<String> {

	@Override
	public String getAsObject(FacesContext context, UIComponent component, String submittedValue) {
		if (isEmpty(submittedValue)) {
			return null;
		}

		String trimmed = submittedValue.trim();
		return isEmpty(trimmed) ? null : trimmed;
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, String modelValue) {
		return coalesce(modelValue, "");
	}

}