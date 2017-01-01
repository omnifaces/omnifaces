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
package org.omnifaces.el;

import javax.el.ELContext;
import javax.el.PropertyNotWritableException;
import javax.el.ValueExpression;

import org.omnifaces.util.Callback;

/**
 * Implementation of a read only value expression that can be used when the value is not yet available at construction time, or when the
 * value would be too expensive to create and it's not yet clear if the expression will actually be evaluated.
 *
 * <p>
 * A callback (lambda in Java 8) that obtains the value can be provided, or the getValue() method can be overridden.
 *
 * @since 2.0
 * @author Arjan Tijms
 */
public class ReadOnlyValueExpression extends ValueExpression {

	private static final long serialVersionUID = 1L;

	private Callback.SerializableReturning<Object> callback;
	private Class<?> expectedType;

	public ReadOnlyValueExpression(Class<?> expectedType, Callback.SerializableReturning<Object> callback) {
		this(expectedType);
		this.callback = callback;
	}

	public ReadOnlyValueExpression(Class<?> expectedType) {
		this.expectedType = expectedType;
	}

	public ReadOnlyValueExpression() {
		//
	}

	@Override
	public Object getValue(ELContext context) {
		if (callback != null) {
			return callback.invoke();
		}

		return null;
	}

	@Override
	public void setValue(ELContext context, Object value) {
		throw new PropertyNotWritableException();
	}

	@Override
	public boolean isReadOnly(ELContext context) {
		return true;
	}

	@Override
	public Class<?> getType(ELContext context) {
		Object value = getValue(context);
		return value == null ? null : value.getClass();
	}

	@Override
	public Class<?> getExpectedType() {
		return expectedType;
	}

	@Override
	public String getExpressionString() {
		return null;
	}

	@Override
	public boolean equals(Object object) {
		// Basic checks.
		if (object == this) {
			return true;
		}
		if (object == null || object.getClass() != getClass()) {
			return false;
		}

		// Property checks.
		ReadOnlyValueExpression other = (ReadOnlyValueExpression) object;
		Object value = getValue(null);
		Object otherValue = other.getValue(null);
		if (value == null ? otherValue != null : !value.equals(otherValue)) {
			return false;
		}

		// All passed.
		return true;
	}

	@Override
	public int hashCode() {
		Object value = getValue(null);
		return value == null ? 0 : value.hashCode();
	}

	@Override
	public boolean isLiteralText() {
		return true;
	}

	/**
	 * Returns the functional interface that will be called when the value expression is resolved
	 *
	 * @return the functional interface that will be called when the value expression is resolved
	 * @since 2.1
	 */
	public Callback.SerializableReturning<Object> getCallbackReturning() {
		return callback;
	}

	/**
	 * Sets the functional interface that will be called when the value expression is resolved
	 *
	 * @param callbackReturning functional interface returning what the value expression will return
	 * @since 2.1
	 */
	public void setCallbackReturning(Callback.SerializableReturning<Object> callbackReturning) {
		this.callback = callbackReturning;
	}

}