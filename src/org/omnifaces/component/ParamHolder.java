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
package org.omnifaces.component;

import javax.faces.application.Application;
import javax.faces.component.ValueHolder;
import javax.faces.convert.Converter;

import org.omnifaces.util.Faces;

/**
 * This interface represents a (request) parameter holder which extends {@link ValueHolder} with {@link #getName()}
 * method to obtain the parameter name associated with the parameter value and changes the {@link #getLocalValue()}
 * method to return the original, unconverted value and changes the {@link #getValue()} method to return the converted
 * value. This is used in among others the {@link Faces#getBookmarkableURL(String, java.util.Collection, boolean)}.
 *
 * @author Bauke Scholtz
 * @since 1.7
 */
public interface ParamHolder extends ValueHolder {

	/**
	 * Returns the name of the parameter.
	 * @return The name of the parameter.
	 */
	public String getName();

	/**
	 * Returns the original, unconverted value of the parameter.
	 * @return The original, unconverted value of the parameter.
	 */
	@Override
	public Object getLocalValue();

	/**
	 * Returns the converted value of the parameter. If the converter is set, or if any converter is available by
	 * {@link Application#createConverter(Class)}, passing the value's class, then return the result of
	 * {@link Converter#getAsString(javax.faces.context.FacesContext, javax.faces.component.UIComponent, Object)},
	 * otherwise if the value is not <code>null</code>, then return the value's {@link Object#toString()}, else return
	 * an empty string.
	 * @return The converted value of the parameter, which should never be <code>null</code>.
	 * @see Converter#getAsString(javax.faces.context.FacesContext, javax.faces.component.UIComponent, Object)
	 */
	@Override
	public String getValue();

}