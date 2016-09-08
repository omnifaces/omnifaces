/*
 * Copyright 2016 OmniFaces
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

import static org.omnifaces.util.Faces.getServletContext;

import javax.faces.application.Application;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.PostConstructApplicationEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;
import javax.servlet.Filter;
import javax.servlet.ServletContextListener;

/**
 * Installs the {@link FacesViewsViewHandler}.
 * <p>
 * <i>Implementation note</i>: this needs to be done during the {@link PostConstructApplicationEvent}, in
 * which it's guaranteed that Faces initialization (typically done via a {@link ServletContextListener}) has
 * been done. Setting a view handler programmatically requires the Faces {@link Application} to be present
 * which isn't the case before Faces initialization has been done.
 * <p>
 * Additionally, the view handler needs to be set BEFORE the first faces request is processed. Putting
 * the view handler setting code in a {@link Filter#init(javax.servlet.FilterConfig)} method only works
 * when all init methods are called during startup, OR when the filter filters every request.
 * <p>
 * For a guide on FacesViews, please see the <a href="package-summary.html">package summary</a>.
 *
 * @author Arjan Tijms
 * @since 2.0
 * @see FacesViews
 * @see FacesViewsViewHandler
 */
public class FacesViewsViewHandlerInstaller implements SystemEventListener {

	@Override
	public boolean isListenerForSource(Object source) {
		return source instanceof Application;
	}

	@Override
	public void processEvent(SystemEvent event) throws AbortProcessingException {
		FacesViews.registerViewHander(getServletContext());
	}

}