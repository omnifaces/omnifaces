/*
 * Copyright 2013 OmniFaces.
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

import javax.faces.component.FacesComponent;
import javax.faces.component.UIParameter;
import javax.faces.component.ValueHolder;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

/**
 * <strong>Param</strong> is a component that extends the standard {@link UIParameter} to implement {@link ValueHolder}
 * and thus support a {@link Converter} to convert the supplied value to string, if necessary.
 * <p>
 * You can use it the same way as <code>&lt;f:param&gt;</code>, you only need to change <code>f:</code> into
 * <code>o:</code> to get the extra support for a {@link Converter} by usual means via the <code>converter</code>
 * attribute of the tag, or the nested <code>&lt;f:converter&gt;</code> tag, or just automatically if a converter is
 * already registered for the target class.
 *
 * @author Bauke Scholtz
 * @since 1.4
 */
@FacesComponent(Param.COMPONENT_TYPE)
public class Param extends UIParameter implements ValueHolder {

	// Public constants -----------------------------------------------------------------------------------------------

	public static final String COMPONENT_TYPE = "org.omnifaces.component.output.Param";

	// Private constants ----------------------------------------------------------------------------------------------

	private enum PropertyKeys {
		// Cannot be uppercased. They have to exactly match the attribute names.
		converter;
	}

	// Properties -----------------------------------------------------------------------------------------------------

	private Converter converter;

	// Attribute getters/setters --------------------------------------------------------------------------------------

	@Override
	public Converter getConverter() {
		return this.converter != null ? this.converter : (Converter) getStateHelper().eval(PropertyKeys.converter);
	}

	@Override
	public void setConverter(Converter converter) {
		this.converter = converter;
	}

	@Override
	public Object getLocalValue() {
		return super.getValue();
	}

	@Override
	public Object getValue() {
		FacesContext context = getFacesContext();
		Converter converter = getConverter();
		Object value = getLocalValue();

		if (converter == null && value != null) {
			converter = context.getApplication().createConverter(value.getClass());
		}

		if (converter != null) {
			return converter.getAsString(context, this, value);
		}
		else {
			return value;
		}
	}

}