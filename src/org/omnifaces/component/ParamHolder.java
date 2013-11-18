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

import javax.faces.component.ValueHolder;
import javax.faces.context.FacesContext;

import org.omnifaces.util.Faces;

/**
 * This interface represents a (request) parameter holder which extends {@link ValueHolder} with methods to obtain the
 * parameter name associated with the parameter value and to explicitly obtain the converted value. This is used in
 * among others the {@link Faces#getBookmarkableURL(String, java.util.List, boolean)}.
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
	 * Returns the converted value of the parameter.
	 * @param context The involved faces context.
	 * @return The converted value of the parameter.
	 */
	public String getConvertedValue(FacesContext context);

}