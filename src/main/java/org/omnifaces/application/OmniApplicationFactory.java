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
package org.omnifaces.application;

import jakarta.faces.application.Application;
import jakarta.faces.application.ApplicationFactory;
import jakarta.faces.application.ApplicationWrapper;

/**
 * This application factory takes care that the {@link OmniApplication} is properly initialized.
 *
 * @author Radu Creanga {@literal <rdcrng@gmail.com>}
 * @author Bauke Scholtz
 * @see OmniApplication
 * @since 1.6
 */
public class OmniApplicationFactory extends ApplicationFactory {

	// Variables ------------------------------------------------------------------------------------------------------

	private Application application;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new OmniFaces application factory around the given wrapped factory.
	 * @param wrapped The wrapped factory.
	 */
	public OmniApplicationFactory(ApplicationFactory wrapped) {
		super(wrapped);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns an instance of {@link OmniApplication} which wraps the original application.
	 */
	@Override
	public Application getApplication() {
		return (application == null) ? createOmniApplication(getWrapped().getApplication()) : application;
	}

	/**
	 * Sets the given application instance as the current instance. If it's not an instance of {@link OmniApplication},
	 * nor wraps the {@link OmniApplication}, then it will be wrapped by a new instance of {@link OmniApplication}.
	 */
	@Override
	public void setApplication(Application application) {
		getWrapped().setApplication(createOmniApplication(application));
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * If the given application not an instance of {@link OmniApplication}, nor wraps the {@link OmniApplication}, then
	 * it will be wrapped by a new instance of {@link OmniApplication} and set as the current instance and returned.
	 * Additionally, it will check if all Application implementations properly extend from ApplicationWrapper.
	 */
	private synchronized Application createOmniApplication(Application application) {
		Application newApplication = application;

		while (!(newApplication instanceof OmniApplication) && newApplication instanceof ApplicationWrapper) {
			newApplication = ((ApplicationWrapper) newApplication).getWrapped();
		}

		if (!(newApplication instanceof OmniApplication)) {
			newApplication = new OmniApplication(application);
		}

		this.application = newApplication;
		return this.application;
	}

}