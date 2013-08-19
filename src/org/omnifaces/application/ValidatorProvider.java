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

import javax.faces.validator.Validator;

import org.omnifaces.util.Faces;

/**
 * An abstraction of validator provider. Concrete validator provider implementations (such as the one from CDI) must
 * store themselves in the JSF application scope under the name {@link #NAME}.
 *
 * @author Bauke Scholtz
 * @see OmniApplication
 * @since 1.6
 */
public abstract class ValidatorProvider {

	// Constants ------------------------------------------------------------------------------------------------------

	/**
	 * The name on which the validator provider implementation should be stored in the JSF application scope.
	 */
	public static final String NAME = "omnifaces_ValidatorProvider";

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the validator instance associated with the given validator ID,
	 * or <code>null</code> if there is none.
	 * @param validatorId The validator ID of the desired validator instance.
	 * @return the validator instance associated with the given validator ID,
	 * or <code>null</code> if there is none.
	 */
	public abstract Validator createValidator(String validatorId);

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the validator provider implementation from the JSF application scope,
	 * or <code>null</code> if there is none.
	 * @return The validator provider implementation from the JSF application scope,
	 * or <code>null</code> if there is none.
	 */
	public static ValidatorProvider getInstance() {
		return Faces.getApplicationAttribute(NAME);
	}

}