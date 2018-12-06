/*
 * Copyright 2018 OmniFaces
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
package org.omnifaces.facesviews;

/**
 * The action that is done when a request for a public path from which faces views where scanned is done.
 * <p>
 * For a guide on FacesViews, please see the <a href="package-summary.html">package summary</a>.
 *
 * @author Arjan Tijms
 * @since 1.4
 * @see FacesViewsForwardingFilter
 */
public enum PathAction {

	/**
	 * Send a 404 (not found), makes it look like e.g. "/path/foo.xhtml" never existed and there's only "/foo" and
	 * optionally "/foo.xhtml".
	 * <p>
	 * This is the default value.
	 */
	SEND_404,

	/**
	 * Redirects to the resource corresponding with the one that was scanned. e.g. "/path/foo.xml" redirects to "/foo".
	 */
	REDIRECT_TO_SCANNED_EXTENSIONLESS,

	/**
	 * No special action is taken. "/path/foo.xml" and "/foo" (and potentially "/foo.xhtml") will be accessible.
	 */
	PROCEED;

}