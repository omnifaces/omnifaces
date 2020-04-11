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
package org.omnifaces.component;

import jakarta.faces.application.Application;
import jakarta.faces.component.ValueHolder;
import jakarta.faces.convert.Converter;

import org.omnifaces.util.Faces;

/**
 * This interface represents a (request) parameter holder which extends {@link ValueHolder} with {@link #getName()}
 * method to obtain the parameter name associated with the parameter value and changes the {@link #getLocalValue()}
 * method to return the original, unconverted value and changes the {@link #getValue()} method to return the value
 * converted to {@link String}. This is used in among others the {@link Faces#getBookmarkableURL(String, java.util.Collection, boolean)}.
 *
 * @author Bauke Scholtz
 * @param <T> The type of the value.
 * @since 1.7
 */
public interface ParamHolder<T> extends ValueHolder {

	/**
	 * Returns the name of the parameter.
	 * @return The name of the parameter.
	 */
	String getName();

	/**
	 * Returns the original, unconverted value of the parameter.
	 * @return The original, unconverted value of the parameter.
	 */
	@Override
	T getLocalValue();

	/**
	 * Returns the converter, if any.
	 * @return The converter, if any.
	 */
	@Override
	Converter<T> getConverter();

	/**
	 * Returns the value of the parameter as {@link String}. If the converter is set, or if any converter is available by
	 * {@link Application#createConverter(Class)}, passing the value's class, then return the result of
	 * {@link Converter#getAsString(jakarta.faces.context.FacesContext, jakarta.faces.component.UIComponent, Object)},
	 * otherwise return the {@link Object#toString()} of the value.
	 * @return The value of the parameter as {@link String}.
	 * @see Converter#getAsString(jakarta.faces.context.FacesContext, jakarta.faces.component.UIComponent, Object)
	 */
	@Override
	String getValue();

}