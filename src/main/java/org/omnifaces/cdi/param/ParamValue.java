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
package org.omnifaces.cdi.param;

import java.io.Serializable;

import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.omnifaces.cdi.Param;

/**
 * The type that's injected via the {@link Param} qualifier.
 * <p>
 * This acts as a wrapper for the actual value that is retrieved from the request and optionally converted.
 *
 * @author Arjan Tijms
 *
 * @param <V>
 *            The type of the actual value this class is wrapping.
 */
public class ParamValue<V> implements Serializable {

	private static final long serialVersionUID = 1l;

	private final String submittedValue;
	private final Param requestParameter;
	private final Class<?> targetType;

	private transient V value;
	private boolean valueSet;

	private V serializableValue;
	private boolean valueIsSerializable;

	public ParamValue(String submittedValue, Param requestParameter, Class<?> targetType, V value) {
		this.submittedValue = submittedValue;
		this.requestParameter = requestParameter;
		this.targetType = targetType;
		this.value = value;
		valueSet = true;

		if (value instanceof Serializable) {
			serializableValue = value;
			valueIsSerializable = true;
		}
	}

	/**
	 * Gets the converted version of the value that was retrieved from the request.
	 * <p>
	 * <b>Note</b>: if this instance was injected into a passivating scope and passivation has
	 * indeed taken place and the converted value was <em>not</em> serializable, this will attempt to reconvert
	 * the submitted value again. Conversion can only be done when in a JSF context!
	 *
	 * @return the converter value
	 */
	@SuppressWarnings("unchecked")
	public V getValue() {

		if (!valueSet) {
			// If the value has not been set this instance has recently been de-serialized

			if (valueIsSerializable) {
				// The original value was serializable and will thus have been been de-serialized too
				value = serializableValue;
			} else {

				// The original value was NOT serializable so we need to generate it from the raw
				// submitted value again.

				// A converter may not be serializable either, so we obtain a new instance as well.
				// TODO: Maybe test if converter is serializable and if so keep a reference
				Converter converter = RequestParameterProducer.getConverter(requestParameter, targetType);

				Object convertedValue;
				if (converter != null) {
					FacesContext context = FacesContext.getCurrentInstance();
					convertedValue = converter.getAsObject(context, context.getViewRoot(), submittedValue);
				} else {
					convertedValue = submittedValue;
				}

				value = (V) convertedValue;
			}

			valueSet = true;
		}

		return value;
	}

	public String getSubmittedValue() {
		return submittedValue;
	}

}
