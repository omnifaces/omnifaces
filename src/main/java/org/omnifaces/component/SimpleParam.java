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
package org.omnifaces.component;

import javax.faces.component.UIParameter;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

/**
 * This class provides a basic and default implementation of the {@link ParamHolder} interface. Ultimately, this class
 * can be used as a simple key-value pair holder (parameter name-value) which uses an explicit/implicit JSF converter
 * to convert the object value to string.
 *
 * @author Bauke Scholtz
 * @since 1.7
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class SimpleParam implements ParamHolder {

	// Properties -----------------------------------------------------------------------------------------------------

	private String name;
	private Object value;
	private Converter converter;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Default constructor.
	 */
	public SimpleParam() {
		// NOOP.
	}

	/**
	 * Construct a simple param with name and value.
	 * @param name The parameter name.
	 * @param value The parameter value.
	 */
	public SimpleParam(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * Construct a simple param with name, value and converter.
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * @param converter The converter.
	 */
	public SimpleParam(String name, Object value, Converter converter) {
		this(name, value);
		this.converter = converter;
	}

	/**
	 * Construct a simple param with name and value of given {@link UIParameter} component.
	 * @param param The {@link UIParameter} to copy.
	 * @since 2.2
	 */
	public SimpleParam(UIParameter param) {
		name = param.getName();

		if (param instanceof ParamHolder) {
			ParamHolder holder = (ParamHolder) param;
			value = holder.getLocalValue();
			converter = holder.getConverter();
		}
		else {
			value = param.getValue();
		}
	}

	/**
	 * Construct a simple param with name, value and converter of given {@link ParamHolder} instance.
	 * @param param The {@link ParamHolder} to copy.
	 * @since 2.2
	 */
	public SimpleParam(ParamHolder param) {
		this(param.getName(), param.getLocalValue(), param.getConverter());
	}

	// Getters/setters ------------------------------------------------------------------------------------------------

	@Override
	public String getName() {
		return name;
	}

	/**
	 * Sets the parameter name.
	 * @param name The parameter name.
	 */
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public Object getLocalValue() {
		return value;
	}

	@Override
	public Object getValue() {
		FacesContext context = FacesContext.getCurrentInstance();

		if (converter == null && value != null) {
			converter = context.getApplication().createConverter(value.getClass());
		}

		if (converter != null) {
			UIParameter component = new UIParameter();
			component.setName(name);
			component.setValue(value);
			return converter.getAsString(context, component, value);
		}
		else {
			return value;
		}
	}

	@Override
	public void setValue(Object value) {
		this.value = value;
	}

	@Override
	public Converter getConverter() {
		return converter;
	}

	@Override
	public void setConverter(Converter converter) {
		this.converter = converter;
	}

}