/*
 * Copyright 2012 OmniFaces.
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
package org.omnifaces.exceptionhandler;

import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerFactory;

/**
 * This exception handler factory needs to be registered as follows in <tt>faces-config.xml</tt> to get the
 * {@link FullAjaxExceptionHandler} to run:
 * <pre>
 * &lt;factory&gt;
 *   &lt;exception-handler-factory&gt;
 *     org.omnifaces.exceptionhandler.FullAjaxExceptionHandlerFactory
 *   &lt;/exception-handler-factory&gt;
 * &lt;/factory&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 * @see FullAjaxExceptionHandler
 */
public class FullAjaxExceptionHandlerFactory extends ExceptionHandlerFactory {

	// Variables ------------------------------------------------------------------------------------------------------

	private ExceptionHandlerFactory wrapped;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new full ajax exception handler factory around the given wrapped factory.
	 * @param wrapped The wrapped factory.
	 */
	public FullAjaxExceptionHandlerFactory(ExceptionHandlerFactory wrapped) {
		this.wrapped = wrapped;
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns a new instance {@link FullAjaxExceptionHandler}.
	 */
	@Override
	public ExceptionHandler getExceptionHandler() {
		return new FullAjaxExceptionHandler(wrapped.getExceptionHandler());
	}

	/**
	 * Returns the wrapped factory.
	 */
	@Override
	public ExceptionHandlerFactory getWrapped() {
		return wrapped;
	}

}