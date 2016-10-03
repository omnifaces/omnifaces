package org.omnifaces.converter;

import static org.omnifaces.util.FacesLocal.getLocale;

import java.util.Locale;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;

/**
 * <p>
 * The <code>omnifaces.ToUpperCaseConverter</code> is intented to convert submitted {@link String} values to upper case
 * based on current {@link Locale}. Additionally, it trims any whitespace from the submitted value. This is useful for
 * among others zip code inputs.
 *
 * <h3>Usage</h3>
 * <p>
 * This converter is available by converter ID <code>omnifaces.ToUpperCaseConverter</code>. Just specify it in the
 * <code>converter</code> attribute of the component referring the <code>String</code> property. For example:
 * <pre>
 * &lt;h:inputText value="#{bean.zipCode}" converter="omnifaces.ToUpperCaseConverter" /&gt;
 * </pre>
 *
 * @author Bauke Scholtz
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