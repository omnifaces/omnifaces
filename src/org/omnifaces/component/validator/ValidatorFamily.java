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
package org.omnifaces.component.validator;

import java.io.IOException;

import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;

/**
 * Base class which is to be shared between all components of the Validator family.
 *
 * @author Bauke Scholtz
 */
public abstract class ValidatorFamily extends UIComponentBase {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The standard component family. */
	public static final String COMPONENT_FAMILY = "org.omnifaces.component.validator";

	// UIComponent overrides ------------------------------------------------------------------------------------------

	@Override
	public String getFamily() {
		return COMPONENT_FAMILY;
	}

	@Override
	public boolean getRendersChildren() {
		return false;
	}

	@Override
	public void processDecodes(FacesContext context) {
		validateHierarchy();
	}

	@Override
	public void processValidators(FacesContext context) {
		validateComponents(context);
	}

	@Override
	public void processUpdates(FacesContext context) {
		// NOOP.
	}

	@Override
	public void encodeAll(FacesContext context) throws IOException {
		validateHierarchy();
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Validate our own component hierarchy.
	 * @throws IllegalArgumentException When component hierarchy is wrong.
	 */
	protected abstract void validateHierarchy() throws IllegalArgumentException;

	/**
	 * Perform the actual validation.
	 * @param context The faces context to work with.
	 */
	protected abstract void validateComponents(FacesContext context);

}