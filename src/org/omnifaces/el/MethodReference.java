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
