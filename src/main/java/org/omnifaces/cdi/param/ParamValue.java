/*
 * Copyright OmniFaces
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
package org.omnifaces.cdi.param;

import static org.omnifaces.cdi.param.ParamProducer.coerceValues;
import static org.omnifaces.cdi.param.ParamProducer.getConvertedValues;
import static org.omnifaces.util.Faces.getContext;
import static org.omnifaces.util.Utils.isEmpty;
import static org.omnifaces.util.Utils.isSerializable;

import java.io.Serializable;

import org.omnifaces.cdi.Param;

/**
 * The type that's injected via the <code>&#64;</code>{@link Param} qualifier.
 * <p>
 * This acts as a wrapper for the actual value that is retrieved from the request and optionally converted.
 *
 * @author Arjan Tijms
 *
 * @param <V> The type of the actual value this class is wrapping.
 */
public class ParamValue<V> implements Serializable {

	private static final long serialVersionUID = 2L;

	final Param param;
	final String name;
	final String label;
	final Class<?> sourceType;
	final String[] submittedValues;
	final Class<V> targetType;

	private transient V value;
	private transient boolean valueSet;

	private boolean valueIsSerializable;
	private V serializableValue;

	/**
	 * Internal only. This is exclusively used by {@link ParamProducer} for injection.
	 */
	ParamValue(Param param, String name, String label, Class<?> sourceType, String[] submittedValues, Class<V> targetType) {
		this.param = param;
		this.name = name;
		this.label = label;
		this.sourceType = sourceType;
		this.submittedValues = submittedValues;
		this.targetType = targetType;
	}

	/**
	 * Internal only. This is exclusively used by {@link ParamProducer} for bean validation.
	 */
	ParamValue(V value) {
		this(null, null, null, null, null, null);
		setValue(value);
	}

	/**
	 * Internal only. This sets the param value.
	 */
	void setValue(V value) {
		this.value = value;
		valueSet = true;
		valueIsSerializable = value == null || isSerializable(value);
		serializableValue = valueIsSerializable ? value : null;
	}

	/**
	 * Gets the converted version of the value that was retrieved from the request.
	 * <p>
	 * <b>Note</b>: if this instance was injected into a passivating scope and passivation has
	 * indeed taken place and the converted value was <em>not</em> serializable, this will attempt to reconvert
	 * the submitted value again. Conversion can only be done when in a JSF context!
	 *
	 * @return The converted value.
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
				setValue((V) coerceValues(sourceType, getConvertedValues(getContext(), this)));
			}
		}

		return value;
	}

	/**
	 * Returns the submitted value. If this is a multi-valued parameter, then this returns only the first one.
	 * @return The submitted value.
	 */
	public String getSubmittedValue() {
		return isEmpty(submittedValues) ? null : submittedValues[0];
	}

	/**
	 * Returns the submitted values. If this is a multi-valued parameter, then this returns all of them.
	 * Since 3.8, any modifications to the array do not anymore affect the original array.
	 * @return The submitted values.
	 */
	public String[] getSubmittedValues() {
		return submittedValues == null ? null : submittedValues.clone();
	}

}