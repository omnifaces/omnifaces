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

/**
 * This encapsulates a base model object and one of its methods.
 *
 * @since 1.4
 */
public class MethodReference {

	private Object base;
	private Method method;

	public MethodReference(Object base, Method method) {
		this.base = base;
		this.method = method;
	}

	public Object getBase() {
		return base;
	}

	public Method getMethod() {
		return method;
	}

}
