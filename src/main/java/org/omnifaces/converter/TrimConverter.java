package org.omnifaces.converter;

import static org.omnifaces.util.Utils.isEmpty;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 * <p>
 * The <code>omnifaces.TrimConverter</code> is intented to trim any whitespace from submitted {@link String} values.
 * This keeps the data store free of whitespace pollution.
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
public class TrimConverter implements Converter {

    @Override
    public String getAsObject(FacesContext context, UIComponent component, String submittedValue) {
        if (isEmpty(submittedValue)) {
        	return null;
        }

        String trimmed = submittedValue.trim();
        return isEmpty(trimmed) ? null : trimmed;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object modelValue) {
        return (modelValue == null) ? "" : modelValue.toString();
    }

}