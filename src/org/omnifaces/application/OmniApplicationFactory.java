/*
 * Copyright 2013 OmniFaces.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnifaces.application;

import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;

/**
 * This application factory takes care that the {@link OmniApplication} is properly initialized.
 *
 * @author Radu Creanga <rdcrng@gmail.com>
 * @author Bauke Scholtz
 * @see OmniApplication
 * @since 1.6
 */
public class OmniApplicationFactory extends ApplicationFactory {

	// Variables ------------------------------------------------------------------------------------------------------

	private final ApplicationFactory wrapped;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new OmniFaces application factory around the given wrapped factory.
	 * @param wrapped The wrapped factory.
	 */
	public OmniApplicationFactory(ApplicationFactory wrapped) {
		this.wrapped = wrapped;
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns a new instance of {@link OmniApplication} which wraps the original application.
	 */
	@Override
	public Application getApplication() {
		return new OmniApplication(wrapped.getApplication());
	}

	/**
	 * Sets the given application instance as the current instance. If it's not an instance of {@link OmniApplication},
	 * then it will be wrapped by {@link OmniApplication}.
	 */
	@Override
	public void setApplication(Application application) {
		wrapped.setApplication(application instanceof OmniApplication ? application : new OmniApplication(application));
	}

	/**
	 * Returns the wrapped factory.
	 */
	@Override
	public ApplicationFactory getWrapped() {
		return wrapped;
	}

}