/*
 * Copyright 2014 OmniFaces.
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
	
	private Callback.ReturningWithArgument<Object, ELContext> callbackWithArgument;
	private Callback.Returning<Object> callbackReturning;
	
	private Class<?> expectedType;
	
	public ReadOnlyValueExpression(Class<?> expectedType, Callback.Returning<Object> callbackReturning) {
		this(expectedType);
		this.callbackReturning = callbackReturning;
	}
	
	public ReadOnlyValueExpression(Class<?> expectedType, Callback.ReturningWithArgument<Object, ELContext> callbackWithArgument) {
		this(expectedType);
		this.callbackWithArgument = callbackWithArgument;
	}
	
	public ReadOnlyValueExpression(Class<?> expectedType) {
		this.expectedType = expectedType;
	}
	
	public ReadOnlyValueExpression() {}

	@Override
	public Object getValue(ELContext context) {
		if (callbackReturning != null) {
			return callbackReturning.invoke();
		}
		
		if (callbackWithArgument != null) {
			return callbackWithArgument.invoke(context);
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
	public boolean equals(Object obj) {
		return (obj instanceof ReadOnlyValueExpression && equals((ReadOnlyValueExpression) obj));
	}
	
	public boolean equals(ReadOnlyValueExpression other) {
		Object value = getValue(null);
		Object otherValue = other == null ? null : other.getValue(null);
		
		return other != null && (value != null && otherValue != null && (value == otherValue || value.equals(otherValue)));
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
	
}