/*
 * Copyright 2020 OmniFaces
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
package org.omnifaces.exceptionhandler;

import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerFactory;

/**
 * This exception handler factory needs to be registered as follows in <code>faces-config.xml</code> to get the
 * {@link FacesMessageExceptionHandler} to run:
 * <pre>
 * &lt;factory&gt;
 *   &lt;exception-handler-factory&gt;
 *     org.omnifaces.exceptionhandler.FacesMessageExceptionHandlerFactory
 *   &lt;/exception-handler-factory&gt;
 * &lt;/factory&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 * @see FacesMessageExceptionHandler
 * @since 1.8
 */
public class FacesMessageExceptionHandlerFactory extends DefaultExceptionHandlerFactory {

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new full ajax exception handler factory around the given wrapped factory.
	 * @param wrapped The wrapped factory.
	 */
	public FacesMessageExceptionHandlerFactory(ExceptionHandlerFactory wrapped) {
		super(wrapped);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns a new instance of {@link FacesMessageExceptionHandler} which wraps the original exception handler.
	 */
	@Override
	public ExceptionHandler getExceptionHandler() {
		return new FacesMessageExceptionHandler(getWrapped().getExceptionHandler());
	}

}