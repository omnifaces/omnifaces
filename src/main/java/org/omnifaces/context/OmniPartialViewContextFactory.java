/*
 * Copyright OmniFaces
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
package org.omnifaces.context;

import javax.faces.context.FacesContext;
import javax.faces.context.PartialViewContext;
import javax.faces.context.PartialViewContextFactory;

/**
 * This partial view context factory takes care that the {@link OmniPartialViewContext} is properly initialized.
 *
 * @author Bauke Scholtz
 * @see OmniPartialViewContext
 * @since 1.2
 */
public class OmniPartialViewContextFactory extends PartialViewContextFactory {

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new OmniFaces partial view context factory around the given wrapped factory.
	 * @param wrapped The wrapped factory.
	 */
	public OmniPartialViewContextFactory(PartialViewContextFactory wrapped) {
		super(wrapped);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns a new instance of {@link OmniPartialViewContext} which wraps the original partial view context.
	 */
	@Override
	public PartialViewContext getPartialViewContext(FacesContext context) {
		return new OmniPartialViewContext(getWrapped().getPartialViewContext(context));
	}

}