/*
 * Copyright 2016 OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnifaces.context;

import javax.faces.FacesException;
import javax.faces.context.ExternalContext;
import javax.faces.context.ExternalContextFactory;

/**
 * This external context factory takes care that the {@link OmniExternalContext} is properly initialized.
 *
 * @author Bauke Scholtz
 * @see OmniExternalContext
 * @since 2.2
 */
public class OmniExternalContextFactory extends ExternalContextFactory {

	// Variables ------------------------------------------------------------------------------------------------------

	private ExternalContextFactory wrapped;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new OmniFaces external context factory around the given wrapped factory.
	 * @param wrapped The wrapped factory.
	 */
	public OmniExternalContextFactory(ExternalContextFactory wrapped) {
		this.wrapped = wrapped;
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns a new instance of {@link OmniExternalContext} which wraps the original external context.
	 */
	@Override
	public ExternalContext getExternalContext(Object context, Object request, Object response) throws FacesException {
		return new OmniExternalContext(wrapped.getExternalContext(context, request, response));
	}

	/**
	 * Returns the wrapped factory.
	 */
	@Override
	public ExternalContextFactory getWrapped() {
		return wrapped;
	}

}