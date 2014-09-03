/*
 * Copyright 2013 OmniFaces.
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

import static org.omnifaces.facesviews.FacesViews.getMappedPath;
import static org.omnifaces.facesviews.FacesViews.scanAndStoreViews;
import static org.omnifaces.util.Faces.getServletContext;
import static org.omnifaces.util.Faces.isDevelopment;

import java.net.MalformedURLException;
import java.net.URL;

import javax.faces.FacesException;
import javax.faces.context.ExternalContext;
import javax.faces.context.ExternalContextFactory;
import javax.faces.context.ExternalContextWrapper;

/**
 * External context factory that installs an external context which locates resources just
 * like the {@link FacesViewsResolver} does.
 * <p>
 * <b>This is only needed for JSF 2.0 implementations and is not needed for JSF 2.1+.</b>
 *
 * TODO: remove this?
 *
 * @since 1.6
 * @author Arjan Tijms
 *
 */
public class FacesViewsJSF2ExternalContextFactory extends ExternalContextFactory {

	private ExternalContextFactory parent;

	public FacesViewsJSF2ExternalContextFactory(ExternalContextFactory parent) {
		this.parent = parent;
	}

	@Override
	public ExternalContext getExternalContext(Object context, Object request, Object response) throws FacesException {
		return new FacesViewsJSF2ExternalContext(getWrapped().getExternalContext(context, request, response));
	}

	@Override
	public ExternalContextFactory getWrapped() {
		return parent;
	}

	public static class FacesViewsJSF2ExternalContext extends ExternalContextWrapper {

		private ExternalContext wrapped;

	    public FacesViewsJSF2ExternalContext(ExternalContext wrapped) {
	        this.wrapped = wrapped;
	    }

	    @Override
	    public URL getResource(String path) throws MalformedURLException {

	    	  URL resource = super.getResource(getMappedPath(path));

	          if (resource == null && isDevelopment()) {
	          	// If "resource" is null it means it wasn't found. Check if the resource was dynamically added by
	          	// scanning the faces-views location(s) again.
	          	scanAndStoreViews(getServletContext());
	          	resource = super.getResource(getMappedPath(path));
	          }

	          return resource;
	    }

	    @Override
	    public ExternalContext getWrapped() {
	        return wrapped;
	    }
	}

}