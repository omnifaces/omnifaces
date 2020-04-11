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
package org.omnifaces.component.validator;

import java.io.IOException;

import jakarta.faces.application.Application;
import jakarta.faces.component.UIComponentBase;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.PostValidateEvent;
import jakarta.faces.event.PreValidateEvent;

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

	/**
	 * Returns {@link #COMPONENT_FAMILY}.
	 */
	@Override
	public String getFamily() {
		return COMPONENT_FAMILY;
	}

	/**
	 * Returns <code>true</code>.
	 */
	@Override
	public boolean getRendersChildren() {
		return true;
	}

	/**
	 * Calls {@link #validateHierarchy()}.
	 */
	@Override
	public void processDecodes(FacesContext context) {
		validateHierarchy();
	}

	/**
	 * Calls {@link #validateComponents(FacesContext)}.
	 */
	@Override
	public void processValidators(FacesContext context) {
		Application application = context.getApplication();
		application.publishEvent(context, PreValidateEvent.class, this);
		validateComponents(context);
		application.publishEvent(context, PostValidateEvent.class, this);
	}

	/**
	 * Does nothing.
	 */
	@Override
	public void processUpdates(FacesContext context) {
		// NOOP.
	}

	/**
	 * Calls {@link #validateHierarchy()}.
	 */
	@Override
	public void encodeChildren(FacesContext context) throws IOException {
		validateHierarchy();
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Validate our own component hierarchy.
	 * @throws IllegalStateException When component hierarchy is wrong.
	 */
	protected abstract void validateHierarchy();

	/**
	 * Perform the actual validation.
	 * @param context The faces context to work with.
	 */
	protected abstract void validateComponents(FacesContext context);

}