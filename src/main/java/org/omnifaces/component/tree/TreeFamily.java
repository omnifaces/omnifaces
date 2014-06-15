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
package org.omnifaces.component.tree;

import java.io.IOException;

import javax.faces.FacesException;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;

/**
 * Base class which is to be shared between all components of the Tree family.
 *
 * @author Bauke Scholtz
 */
public abstract class TreeFamily extends UIComponentBase {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The standard component family. */
	public static final String COMPONENT_FAMILY = "org.omnifaces.component.tree";

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * The base constructor sets the renderer type to <code>null</code> as the components of the Tree family does not
	 * render anything by themselves.
	 */
	public TreeFamily() {
		setRendererType(null);
	}

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
	 * Calls {@link #validateHierarchy()} and then {@link #process(FacesContext, PhaseId)} with
	 * {@link PhaseId#APPLY_REQUEST_VALUES}.
	 */
	@Override
	public void processDecodes(FacesContext context) {
		validateHierarchy();
		process(context, PhaseId.APPLY_REQUEST_VALUES);
	}

	/**
	 * Calls {@link #process(FacesContext, PhaseId)} with {@link PhaseId#PROCESS_VALIDATIONS}.
	 */
	@Override
	public void processValidators(FacesContext context) {
		process(context, PhaseId.PROCESS_VALIDATIONS);
	}

	/**
	 * Calls {@link #process(FacesContext, PhaseId)} with {@link PhaseId#UPDATE_MODEL_VALUES}.
	 */
	@Override
	public void processUpdates(FacesContext context) {
		process(context, PhaseId.UPDATE_MODEL_VALUES);
	}

	/**
	 * Calls {@link #validateHierarchy()} and then {@link #process(FacesContext, PhaseId)} with
	 * {@link PhaseId#RENDER_RESPONSE}.
	 */
	@Override
	public void encodeChildren(FacesContext context) throws IOException {
		validateHierarchy();
		process(context, PhaseId.RENDER_RESPONSE);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Validate the component hierarchy.
	 * @throws IllegalArgumentException When component hierarchy is wrong.
	 */
	protected abstract void validateHierarchy() throws IllegalArgumentException;

	/**
	 * Process the component according to the rules of the given phase ID.
	 * @param context The faces context to work with.
	 * @param phaseId The current phase ID.
	 */
	protected abstract void process(FacesContext context, PhaseId phaseId);

	/**
	 * Helper method to delegate the processing further to the {@link UIComponentBase} superclass which will handle
	 * all children.
	 * @param context The faces context to work with.
	 * @param phaseId The current phase ID.
	 */
	protected void processSuper(FacesContext context, PhaseId phaseId) {
		if (phaseId == PhaseId.APPLY_REQUEST_VALUES) {
			super.processDecodes(context);
		}
		else if (phaseId == PhaseId.PROCESS_VALIDATIONS) {
			super.processValidators(context);
		}
		else if (phaseId == PhaseId.UPDATE_MODEL_VALUES) {
			super.processUpdates(context);
		}
		else if (phaseId == PhaseId.RENDER_RESPONSE) {
			try {
				super.encodeChildren(context);
			}
			catch (IOException e) {
				throw new FacesException(e);
			}
		}
	}

}