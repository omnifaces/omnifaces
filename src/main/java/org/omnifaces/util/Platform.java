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
package org.omnifaces.util;

import java.util.Collection;
import java.util.Collections;

import jakarta.faces.webapp.FacesServlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;

/**
 * This class provides access to (Java EE 6) platform services from the view point of JSF.
 * <p>
 * Note that this utility class can only be used in a JSF environment and is thus not
 * a Java EE general way to obtain platform services.
 *
 * @since 1.6
 * @author Arjan Tijms
 */
public final class Platform {

	// Constructors ---------------------------------------------------------------------------------------------------

	private Platform() {
		// Hide constructor.
	}


	// FacesServlet ---------------------------------------------------------------------------------------------------

	/**
	 * Returns the {@link ServletRegistration} associated with the {@link FacesServlet}.
	 * @param servletContext The context to get the ServletRegistration from.
	 * @return ServletRegistration for FacesServlet, or <code>null</code> if the FacesServlet is not installed.
	 * @since 1.8
	 */
	public static ServletRegistration getFacesServletRegistration(ServletContext servletContext) {
		ServletRegistration facesServletRegistration = null;

		for (ServletRegistration registration : servletContext.getServletRegistrations().values()) {
			if (registration.getClassName().equals(FacesServlet.class.getName())) {
				facesServletRegistration = registration;
				break;
			}
		}

		return facesServletRegistration;
	}

	/**
	 * Returns the mappings associated with the {@link FacesServlet}.
	 * @param servletContext The context to get the {@link FacesServlet} from.
	 * @return The mappings associated with the {@link FacesServlet}, or an empty set.
	 * @since 2.5
	 */
	public static Collection<String> getFacesServletMappings(ServletContext servletContext) {
		ServletRegistration facesServlet = getFacesServletRegistration(servletContext);
		return (facesServlet != null) ? facesServlet.getMappings() : Collections.<String>emptySet();
	}

}