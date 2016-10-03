package org.omnifaces.converter;

import static org.omnifaces.util.FacesLocal.getLocale;

import java.util.Locale;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;

/**
 * <p>
 * The <code>omnifaces.ToLowerCaseConverter</code> is intented to convert submitted {@link String} values to lower case
 * based on current {@link Locale}. Additionally, it trims any whitespace from the submitted value. This is useful for
 * among others email address inputs.
 *
 * <h3>Usage</h3>
 * <p>
 * This converter is available by converter ID <code>omnifaces.ToLowerCaseConverter</code>. Just specify it in the
 * <code>converter</code> attribute of the component referring the <code>String</code> property. For example:
 * <pre>
 * &lt;h:inputText value="#{bean.email}" converter="omnifaces.ToLowerCaseConverter" /&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 * @since 2.6
 */
@FacesConverter("omnifaces.ToLowerCaseConverter")
public class ToLowerCaseConverter extends TrimConverter {

	@Override
	public String getAsObject(FacesContext context, UIComponent component, String submittedValue) {
        String trimmed = super.getAsObject(context, component, submittedValue);
		return (trimmed == null) ? null : trimmed.toLowerCase(getLocale(context));
	}

}