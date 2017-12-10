/*
 * Copyright 2017 OmniFaces
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

import static org.omnifaces.util.Utils.isEmpty;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.regex.Pattern;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;
import javax.faces.convert.NumberConverter;

/**
 * <p>
 * This converter won't output the percent or currency symbols, that's up to the UI. This converter will implicitly
 * infer percent or currency symbols on submitted value when absent, just to prevent an unnecessary conversion error.
 *
 * <h3>Usage</h3>
 * <p>
 * This converter is available by converter ID <code>omnifaces.ImplicitNumberConverter</code>. Just specify it as
 * <code>&lt;o:converter&gt;</code> nested in the component referring the <code>Number</code> property. For example:
 * <pre>
 * &lt;span class="currency"&gt;
 *     &lt;span class="symbol"&gt;$&lt;/span&gt;
 *     &lt;h:inputText value="#{bean.price}"&gt;
 *         &lt;o:converter converterId="omnifaces.ImplicitNumberConverter" type="currency" currencySymbol="$" /&gt;
 *     &lt;/h:inputText&gt;
 * &lt;/span&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 * @since 3.0
 */
@FacesConverter("omnifaces.ImplicitNumberConverter")
public class ImplicitNumberConverter extends NumberConverter {

	private static final Pattern PATTERN_NUMBER = Pattern.compile("[\\d,.]+");

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object modelValue) {
		String string = super.getAsString(context, component, modelValue);

		if (string != null) {
			DecimalFormat formatter = getFormatter();
			String symbol = getSymbol(formatter);

			if (symbol != null) {
				string = string.replace(symbol, "").trim();
			}
		}

		return string;
	}

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String submittedValue) {
		String string = submittedValue;

		if (!isEmpty(string)) {
			DecimalFormat formatter = getFormatter();
			String symbol = getSymbol(formatter);

			if (!string.contains(symbol)) {
				string = PATTERN_NUMBER.matcher(formatter.format(0)).replaceAll(submittedValue);
			}
		}

		return super.getAsObject(context, component, string);
	}

	private boolean isPercent() {
		return "percent".equals(getType());
	}

	private boolean isCurrency() {
		return "currency".equals(getType());
	}

	private DecimalFormat getFormatter() {
		if (isPercent()) {
			return (DecimalFormat) NumberFormat.getPercentInstance(getLocale());
		}
		else if (isCurrency()) {
			DecimalFormat formatter = (DecimalFormat) NumberFormat.getCurrencyInstance(getLocale());
			String currencyCode = getCurrencyCode();
			String currencySymbol = getCurrencySymbol();

			if (currencyCode != null) {
				formatter.setCurrency(Currency.getInstance(currencyCode));
			}
			else if (currencySymbol != null) {
				DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
				symbols.setCurrencySymbol(currencySymbol);
				formatter.setDecimalFormatSymbols(symbols);
			}

			return formatter;
		}
		else {
			return null;
		}
	}

	private String getSymbol(DecimalFormat formatter) {
		if (isPercent()) {
			return String.valueOf(formatter.getDecimalFormatSymbols().getPercent());
		}
		else if (isCurrency()) {
			return formatter.getDecimalFormatSymbols().getCurrencySymbol();
		}
		else {
			return null;
		}
	}

}
