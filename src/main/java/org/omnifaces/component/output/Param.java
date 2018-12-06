/*
 * Copyright 2018 OmniFaces
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
package org.omnifaces.component.output;

import static org.omnifaces.util.FacesLocal.createConverter;

import java.io.IOException;
import java.io.StringWriter;

import javax.faces.FacesException;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIParameter;
import javax.faces.component.ValueHolder;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.convert.Converter;

import org.omnifaces.component.ParamHolder;

/**
 * <p>
 * The	<code>&lt;o:param&gt;</code> is a component that extends the standard {@link UIParameter} to implement {@link ValueHolder}
 * and thus support a {@link Converter} to convert the supplied value to string, if necessary.
 * <p>
 * You can use it the same way as <code>&lt;f:param&gt;</code>, you only need to change <code>f:</code> into
 * <code>o:</code> to get the extra support for a {@link Converter} by usual means via the <code>converter</code>
 * attribute of the tag, or the nested <code>&lt;f:converter&gt;</code> tag, or just automatically if a converter is
 * already registered for the target class via <code>@FacesConverter(forClass)</code>.
 * <p>
 * Also, if no value is specified, but children are present, then the encoded output of children will be returned as
 * param value. This is useful when you want to supply JSF components or HTML as parameter of an unescaped
 * <code>&lt;h:outputFormat&gt;</code>. For example,
 * <pre>
 * &lt;h:outputFormat value="#{bundle.paragraph}" escape="false"&gt;
 *     &lt;o:param&gt;&lt;h:link outcome="contact" value="#{bundle.contact}" /&gt;&lt;/o:param&gt;
 * &lt;/h:outputFormat&gt;
 * </pre>
 * <p>with this bundle
 * <pre>
 * paragraph = Please {0} for more information.
 * contact = contact us
 * </pre>
 * <p>will result in the link being actually encoded as output format parameter value.
 *
 * @author Bauke Scholtz
 * @param <T> The type of the value.
 * @since 1.4
 * @see ParamHolder
 */
@FacesComponent(Param.COMPONENT_TYPE)
public class Param<T> extends UIParameter implements ParamHolder<T> {

	// Public constants -----------------------------------------------------------------------------------------------

	public static final String COMPONENT_TYPE = "org.omnifaces.component.output.Param";

	// Private constants ----------------------------------------------------------------------------------------------

	private enum PropertyKeys {
		// Cannot be uppercased. They have to exactly match the attribute names.
		converter;
	}

	// Properties -----------------------------------------------------------------------------------------------------

	private Converter<T> localConverter;

	// Attribute getters/setters --------------------------------------------------------------------------------------

	@Override
	@SuppressWarnings("unchecked")
	public Converter<T> getConverter() {
		return localConverter != null ? localConverter : (Converter<T>) getStateHelper().eval(PropertyKeys.converter);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setConverter(Converter converter) {
		localConverter = converter;
	}

	/**
	 * @throws ClassCastException When actual value is not <code>T</code>.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public T getLocalValue() {
		return (T) super.getValue();
	}

	@Override
	@SuppressWarnings("unchecked")
	public String getValue() {
		FacesContext context = getFacesContext();
		Converter<T> converter = getConverter();
		Object value = getLocalValue();

		if (value == null && getChildCount() > 0) {
			ResponseWriter originalResponseWriter = context.getResponseWriter();
			StringWriter output = new StringWriter();
			context.setResponseWriter(originalResponseWriter.cloneWithWriter(output));

			try {
				super.encodeChildren(context);
			}
			catch (IOException e) {
				throw new FacesException(e);
			}
			finally {
				context.setResponseWriter(originalResponseWriter);
			}

			value = output.toString();
		}

		if (converter == null && value != null) {
			converter = createConverter(context, value.getClass());
		}

		if (converter != null) {
			return converter.getAsString(context, this, (T) value);
		}
		else {
			return value != null ? value.toString() : null;
		}
	}

	@Override
	public boolean getRendersChildren() {
		return true;
	}

	@Override
	public void encodeChildren(FacesContext context) throws IOException {
		// This override which does nothing effectively blocks the children from being encoded during JSF render.
	}

}