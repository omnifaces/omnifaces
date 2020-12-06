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

import javax.faces.context.ExceptionHandlerFactory;

/**
 * Default implementation for {@link ExceptionHandlerFactory}, saving boilerplate to get hold of wrapped one.
 *
 * @author Bauke Scholtz
 * @since 2.0
 * @deprecated Since 3.9 (actually already since 3.0, but overlooked). Technical reason is that the originally intended
 * wrapper boilerplate was already integrated into JSF 2.3. Just extend from {@link ExceptionHandlerFactory} instead.
 */
@Deprecated
public abstract class DefaultExceptionHandlerFactory extends ExceptionHandlerFactory {

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Constructs a exception handler factory, wrapping the given exception handler factory.
	 * @param wrapped The wrapped exception handler factory.
	 */
	public DefaultExceptionHandlerFactory(ExceptionHandlerFactory wrapped) {
		super(wrapped);
	}

}