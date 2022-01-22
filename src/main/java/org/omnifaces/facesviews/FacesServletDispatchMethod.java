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
package org.omnifaces.facesviews;

import javax.faces.webapp.FacesServlet;
import javax.servlet.FilterChain;

/**
 * The method used by FacesViews to invoke the FacesServlet.
 * <p>
 * For a guide on FacesViews, please see the <a href="package-summary.html">package summary</a>.
 *
 * @since 1.4
 * @author Arjan Tijms
 * @see FacesViews
 * @see FacesViewsForwardingFilter
 * @deprecated Since 2.6 As this is superfluous since Servlet 3.0.
 * It will default to DO_FILTER and automatically use FORWARD when resource is not mapped.
 */
@Deprecated // TODO: remove in OmniFaces 3.0.
public enum FacesServletDispatchMethod {

	/**
	 * With this method the {@link FacesViewsForwardingFilter} will use a plain
	 * {@link FilterChain#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse)} to invoke the
	 * {@link FacesServlet}. Using this method necessitates the FacesServlet to be mapped to the (extensionless)
	 * requested resource or to everything (/*).
	 * <p>
	 * The first method is relatively easy using the Servlet 3.0 programmatic registration, but may be troublesome
	 * for Servlet 2.5 or manual (declarative) registrations. Mapping the FacesServlet to everything (/*) is not
	 * recommenced, and mapping it to a path (/somepath/*) with only Facelets views in it pretty much ruins the entire
	 * effect of using FacesViews in the first place.
	 * <p>
	 * This is the default value.
	 */
	DO_FILTER,

	/**
	 * With this method the {@link FacesViewsForwardingFilter} will use a forward to invoke the {@link FacesServlet}.
	 * Using this method the FacesServlet does not have to be mapped to the (extensionless) requested resource or to
	 * everything (/*).
	 * <p>
	 * When forwarding any filters being used by the application do have to be taken into account. Filters defined to be
	 * run <b>AFTER</b> the FacesViewsForwardingFilter will <b>NOT RUN</b> when they are not (also) set to dispatch on
	 * <code>FORWARD</code>. Filters defined to be run <b>BEFORE</b> the FacesViewsForwardingFilter have to be careful
	 * in what they do; forwarding is defined to clear the existing response buffer.
	 */
	FORWARD;

}