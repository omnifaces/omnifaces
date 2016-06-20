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

import static org.omnifaces.cdi.param.RequestParameterProducer.coerceValues;
import static org.omnifaces.cdi.param.RequestParameterProducer.getConvertedValues;
import static org.omnifaces.util.Faces.getContext;
import static org.omnifaces.util.Utils.isEmpty;
import static org.omnifaces.util.Utils.isSerializable;

import java.io.Serializable;
import java.lang.reflect.Type;

import org.omnifaces.cdi.Param;

/**
 * The type that's injected via the <code>&#64;</code>{@link Param} qualifier.
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

	private final String[] submittedValues;
	private final Param param;
	private final Type type;

	private transient V value;
	private transient boolean valueSet;

	private boolean valueIsSerializable;
	private V serializableValue;

	public ParamValue(String[] submittedValues, Param param, Type type, V value) {
		this.submittedValues = submittedValues;
		this.param = param;
		this.type = type;
		setValue(value);

		if (isSerializable(value)) {
			valueIsSerializable = true;
			serializableValue = value;
		}
	}

	private void setValue(V value) {
		this.value = value;
		valueSet = true;
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
			// If the value has not been set this instance has recently been de-serialized.

			if (valueIsSerializable) {
				// The original value was serializable and will thus have been been de-serialized too.
				setValue(serializableValue);
			}
			else {
				// The original value was NOT serializable so we need to generate it from the raw submitted value again.
				setValue((V) coerceValues(type, getConvertedValues(getContext(), param, "param", submittedValues, type)));
			}
		}

		return value;
	}

	public String getSubmittedValue() {
		return isEmpty(submittedValues) ? null : submittedValues[0];
	}

	public String[] getSubmittedValues() {
		return submittedValues;
	}

}