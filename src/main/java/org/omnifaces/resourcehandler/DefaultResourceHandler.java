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
package org.omnifaces.resourcehandler;

import static java.util.Arrays.fill;

import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.application.ResourceHandlerWrapper;

/**
 * A default {@link ResourceHandler} implementation, delegating both {@link #createResource(String)} and
 * {@link #createResource(String, String)} to {@link #createResource(String, String, String)}. Implementors should
 * only need to override {@link #createResource(String, String, String)}. Additionally, the constructor checks if the
 * wrapped resource handler has the {@link #createResource(String, String, String)} properly implemented, otherwise
 * fall back to either {@link #createResource(String, String)} or {@link #createResource(String)} on the wrapped
 * resource handler.
 *
 * @author Bauke Scholtz
 * @since 2.0
 */
public class DefaultResourceHandler extends ResourceHandlerWrapper {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final int ARGUMENT_COUNT_3 = 3;
	private static final int ARGUMENT_COUNT_2 = 2;
	private static final int ARGUMENT_COUNT_1 = 1;

	// Properties -----------------------------------------------------------------------------------------------------

	private ResourceHandler wrapped;
	private int argumentCount;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Creates a new instance of this unmapped resource handler which wraps the given resource handler. Additionally,
	 * check which <code>createResource</code> method is being declared on the wrapped resource handler.
	 * @param wrapped The resource handler to be wrapped.
	 */
	public DefaultResourceHandler(ResourceHandler wrapped) {
		this.wrapped = wrapped;
		Class<? extends ResourceHandler> cls = wrapped.getClass();

		for (int i = ARGUMENT_COUNT_3; i > ARGUMENT_COUNT_1; i--) {
			Class<?>[] paramTypes = new Class[i];
			fill(paramTypes, String.class);

			try {
				cls.getDeclaredMethod("createResource", paramTypes);
				argumentCount = i;
				break;
			}
			catch (NoSuchMethodException ignore) {
				continue;
			}
		}
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Delegate to {@link #createResource(String, String, String)} with <code>null</code> as library name and content
	 * type.
	 */
	@Override
	public Resource createResource(String resourceName) {
		return createResource(resourceName, null, null);
	}

	/**
	 * Delegate to {@link #createResource(String, String, String)} with <code>null</code> as content type.
	 */
	@Override
	public Resource createResource(String resourceName, String libraryName) {
		return createResource(resourceName, libraryName, null);
	}

	/**
	 * Delegate to the right <code>createResource()</code> method of the wrapped resource handler. Some resource handler
	 * implementations namely doesn't implement all the three <code>createResource()</code> methods.
	 */
	@Override
	public Resource createResource(String resourceName, String libraryName, String contentType) {
		switch (argumentCount) {
			case ARGUMENT_COUNT_3: return getWrapped().createResource(resourceName, libraryName, contentType);
			case ARGUMENT_COUNT_2: return getWrapped().createResource(resourceName, libraryName);
			default: return getWrapped().createResource(resourceName);
		}
	}

	@Override
	public ResourceHandler getWrapped() {
		return wrapped;
	}

}