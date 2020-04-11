/*
 * Copyright 2020 OmniFaces
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

import static org.omnifaces.util.FacesLocal.getLocale;

import java.util.Locale;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.FacesConverter;

/**
 * <p>
 * The <code>omnifaces.ToUpperCaseConverter</code> is intented to convert submitted {@link String} values to upper case
 * based on current {@link Locale}. Additionally, it trims any whitespace from the submitted value. This is useful for
 * among others zip code inputs.
 * <p>
 * This converter does by design no conversion in <code>getAsString()</code>.
 *
 * <h2>Usage</h2>
 * <p>
 * This converter is available by converter ID <code>omnifaces.ToUpperCaseConverter</code>. Just specify it in the
 * <code>converter</code> attribute of the component referring the <code>String</code> property. For example:
 * <pre>
 * &lt;h:inputText value="#{bean.zipCode}" converter="omnifaces.ToUpperCaseConverter" /&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 * @see TrimConverter
 * @since 2.6
 */
@FacesConverter("omnifaces.ToUpperCaseConverter")
public class ToUpperCaseConverter extends TrimConverter {

	@Override
	public String getAsObject(FacesContext context, UIComponent component, String submittedValue) {
		String trimmed = super.getAsObject(context, component, submittedValue);
		return (trimmed == null) ? null : trimmed.toUpperCase(getLocale(context));
	}

}