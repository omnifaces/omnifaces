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
package org.omnifaces.facesviews;

/**
 * The mode of the view handler with respect to constructing an action URL.
 * <p>
 * For a guide on FacesViews, please see the <a href="package-summary.html">package summary</a>.
 *
 * @author Arjan Tijms
 * @since 1.5
 * @see FacesViews
 * @see FacesViewsViewHandler
 */
public enum ViewHandlerMode {

	/**
	 * Takes the outcome from the parent view handler and strips the extension from it.
	 * <p>
	 * This is the default value.
	 */
	STRIP_EXTENSION_FROM_PARENT,

	/**
	 * The {@link FacesViewsViewHandler} constructs the action URL itself, but takes the query parameters (if any)
	 * from the outcome of the parent view handler.
	 */
	BUILD_WITH_PARENT_QUERY_PARAMETERS

}