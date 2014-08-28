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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.lang.reflect.Method;
import java.util.List;

import javax.el.MethodInfo;

/**
 * This encapsulates a base model object and one of its methods.
 *
 * @since 1.4
 */
public class MethodReference {

	private Object base;
	private Method method;
	private List<Object> actualParameters;
	// True if this method reference is from an actual method and not the getter from a property.
	boolean fromMethod;
	private MethodInfo methodInfo; 
	

	public MethodReference(Object base, Method method, Object[] actualParameters, boolean fromMethod) {
		this.base = base;
		this.method = method;
		if (actualParameters != null) {
			this.actualParameters = asList(actualParameters);
		} else {
			this.actualParameters = emptyList();
		}
		
		this.fromMethod = fromMethod;
		
		methodInfo =  new MethodInfo(method.getName(), method.getReturnType(), method.getParameterTypes());
	}

	public Object getBase() {
		return base;
	}

	public Method getMethod() {
		return method;
	}
	
	public List<Object> getActualParameters() {
		return actualParameters;
	}

	public boolean isFromMethod() {
		return fromMethod;
	}

	public MethodInfo getMethodInfo() {
		return methodInfo;
	}

}
