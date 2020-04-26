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
package org.omnifaces.converter;

import java.util.Objects;

import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

/**
 * <p>
 * By default, JSF converters run on every request, regardless of whether the submitted value has changed or not. In
 * case of conversion against the DB on complex objects which are already stored in the model in a broader scope, such
 * as the view scope, this may result in unnecessarily expensive service/DAO calls. In such case, you'd like to perform
 * the expensive service/DAO call only when the submitted value is really changed as compared to the model value.
 * <p>
 * This converter offers you a template to do it transparently. To use it, just change your converters from:
 * <pre>
 * public class YourConverter implements Converter&lt;YourEntity&gt; {
 *
 *     public YourEntity getAsObject(FacesContext context, UIComponent component, String submittedValue) {
 *         // ...
 *     }
 *
 *     // ...
 * }
 * </pre>
 * <p>to
 * <pre>
 * public class YourConverter extends ValueChangeConverter&lt;YourEntity&gt; {
 *
 *     public YourEntity getAsChangedObject(FacesContext context, UIComponent component, String submittedValue) {
 *         // ...
 *     }
 *
 *     // ...
 * }
 * </pre>
 * <p>
 * So, essentially, just replace <code>implements Converter</code> by <code>extends ValueChangeConverter</code> and
 * rename the method from <code>getAsObject</code> to <code>getAsChangedObject</code>.
 * Note: the <code>getAsString</code> method of your converter doesn't need to be changed.
 *
 * @author Bauke Scholtz
 * @since 1.6
 */
public abstract class ValueChangeConverter<T> implements Converter<T> {

	/**
	 * If the component is an instance of {@link EditableValueHolder} and the string representation of its old object
	 * value is equal to the submitted value, then immediately return its old object value unchanged. Otherwise, invoke
	 * {@link #getAsChangedObject(FacesContext, UIComponent, String)} which may in turn do the necessary possibly
	 * expensive DAO operations.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public T getAsObject(FacesContext context, UIComponent component, String submittedValue) {
		if (component instanceof EditableValueHolder) {
			String newStringValue = submittedValue;
			T oldObjectValue = (T) ((EditableValueHolder) component).getValue();
			String oldStringValue = getAsString(context, component, oldObjectValue);

			if (Objects.equals(newStringValue, oldStringValue)) {
				return oldObjectValue;
			}
		}

		return getAsChangedObject(context, component, submittedValue);
	}

	/**
	 * Use this method instead of {@link #getAsObject(FacesContext, UIComponent, String)} if you intend to perform the
	 * conversion only when the submitted value is really changed as compared to the model value.
	 * @param context The involved faces context.
	 * @param component The involved UI component.
	 * @param submittedValue The submitted value.
	 * @return The converted value, exactly like as when you use {@link #getAsObject(FacesContext, UIComponent, String)}
	 * the usual way.
	 */
	public abstract T getAsChangedObject(FacesContext context, UIComponent component, String submittedValue);

}