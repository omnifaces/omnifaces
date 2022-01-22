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
package org.omnifaces.el;

import static org.omnifaces.util.Utils.coalesce;

import java.lang.reflect.Method;

import jakarta.el.MethodInfo;

/**
 * This encapsulates information about an EL method expression.
 *
 * @since 1.4
 * @since 2.5 also extends MethodInfo
 */
public class MethodReference extends MethodInfo {

	/** An object array representing "no method parameters". */
	public static final Object[] NO_PARAMS = new Object[0];

	private Object base;
	private Method method;
	private Object[] actualParameters;
	private boolean fromMethod;

	/**
	 * Construct a method reference.
	 * @param base The base of the EL method expression.
	 * @param method The concrete {@link Method} instance of the EL method expression.
	 */
	public MethodReference(Object base, Method method) {
		super(method.getName(), method.getReturnType(), method.getParameterTypes());
		this.base = base;
		this.method = method;
	}

	/**
	 * Construct a method reference.
	 * @param base The base of the EL method expression.
	 * @param method The concrete {@link Method} instance of the EL method expression.
	 * @param actualParameters The actual (evaluated) parameters of the method call.
	 * @param fromMethod Whether this method reference is from an actual method call and not from a getter of a property.
	 */
	public MethodReference(Object base, Method method, Object[] actualParameters, boolean fromMethod) {
		this(base, method);
		this.actualParameters = coalesce(actualParameters, NO_PARAMS);
		this.fromMethod = fromMethod;
	}

	/**
	 * Returns the base of the EL method expression. Usually, this is the backing bean on which the method behind
	 * {@link #getMethod()} should be invoked.
	 * @return The base of the EL method expression.
	 */
	public Object getBase() {
		return base;
	}

	/**
	 * Returns the concrete {@link Method} instance of the EL method expression. Usually, this is a method of the
	 * class behind {@link #getBase()}.
	 * @return The concrete {@link Method} instance of the EL method expression.
	 */
	public Method getMethod() {
		return method;
	}

	/**
	 * Returns the actual (evaluated) parameters of the method call. If there are no params, then this returns an empty
	 * array, never <code>null</code>. Those should be passed to {@link Method#invoke(Object, Object...)}.
	 * @return The actual (evaluated) parameters of the method call.
	 */
	public Object[] getActualParameters() {
		return actualParameters;
	}

	/**
	 * Returns <code>true</code> if this method reference is from an actual method call and not from a getter of a property.
	 * @return <code>true</code> if this method reference is from an actual method call and not from a getter of a property.
	 */
	public boolean isFromMethod() {
		return fromMethod;
	}

}