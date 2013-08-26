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

import static org.omnifaces.util.Faces.evaluateExpressionGet;

/**
 * An abstraction of view scope provider. Concrete view scope provider implementations (such as the one from CDI) must
 * store themselves in the EL scope under the {@link #NAME}.
 *
 * @author Bauke Scholtz
 * @see ViewScopeEventListener
 * @since 1.6
 */
public abstract class ViewScopeProvider {

	// Constants ------------------------------------------------------------------------------------------------------

	/**
	 * The name on which the view scope provider implementation should be stored in the EL scope.
	 */
	public static final String NAME = "omnifaces_ViewScopeProvider";
	private static final String EL_NAME = String.format("#{%s}", NAME);

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * This method is invoked during view destroy by {@link ViewScopeEventListener}.
	 */
	public abstract void preDestroyView();

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the view scope provider implementation from the EL context,
	 * or <code>null</code> if there is none.
	 * @return The view scope provider implementation from the EL context,
	 * or <code>null</code> if there is none.
	 */
	public static ViewScopeProvider getInstance() {
		return evaluateExpressionGet(EL_NAME);
	}

}