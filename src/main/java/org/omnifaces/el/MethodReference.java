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

import java.lang.reflect.Method;

import javax.el.MethodExpression;
import javax.el.MethodInfo;

/**
 * This encapsulates information about an EL method expression.
 *
 * @since 1.4
 */
public class MethodReference {

	private Object base;
	private Method method;
	private Object[] actualParameters;
	private boolean fromMethod;
	private MethodInfo methodInfo;

	public MethodReference(Object base, Method method, Object[] actualParameters, boolean fromMethod) {
		this.base = base;
		this.method = method;
		this.actualParameters = (actualParameters != null) ? actualParameters : new Object[0];
		this.fromMethod = fromMethod;
		methodInfo =  new MethodInfo(method.getName(), method.getReturnType(), method.getParameterTypes());
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

	/**
	 * Returns the standard EL {@link MethodInfo} of the {@link MethodExpression} where this {@link MethodReference}
	 * has been extracted from.
	 * @return The standard EL {@link MethodInfo}.
	 */
	public MethodInfo getMethodInfo() {
		return methodInfo;
	}

}