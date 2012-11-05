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
package org.omnifaces.util;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;

import javax.faces.FacesException;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialViewContext;
import javax.faces.context.PartialViewContextWrapper;

/**
 * Collection of JSF implementation and/or JSF component library specific hacks. So far now there are only RichFaces
 * specific hacks to get OmniFaces to work nicely together with RichFaces.
 *
 * @author Bauke Scholtz
 */
public final class Hacks {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final boolean RICHFACES_INSTALLED = initRichFacesInstalled();
	private static final String RICHFACES_PVC_CLASS_NAME =
		"org.richfaces.context.ExtendedPartialViewContextImpl";
	private static final String ERROR_RICHFACES_PVC_RENDERIDS =
		"Cannot obtain componentRenderIds property of RichFaces ExtendedPartialViewContextImpl instance '%s'.";
	private static final String ERROR_RICHFACES_PVC_WRAPPED_VC =
		"Cannot obtain wrappedViewContext property of RichFaces ExtendedPartialViewContextImpl instance '%s'.";
	private static final boolean RICHFACES_RESOURCE_OPTIMIZATION_ENABLED =
		RICHFACES_INSTALLED && Boolean.valueOf(Faces.getInitParameter("org.richfaces.resourceOptimization.enabled"));

	// Constructors/init ----------------------------------------------------------------------------------------------

	private Hacks() {
		//
	}

	private static boolean initRichFacesInstalled() {
		try {
			Class.forName(RICHFACES_PVC_CLASS_NAME);
			return true;
		}
		catch (ClassNotFoundException ignore) {
			return false;
		}
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Returns true if RichFaces is installed. That is, when the RichFaces specific ExtendedPartialViewContextImpl is
	 * present in the runtime classpath -which is present in RF 4.x. As this is usually auto-registered, we may safely
	 * assume that RichFaces is installed.
	 * @return Whether RichFaces is installed.
	 */
	public static boolean isRichFacesInstalled() {
		return RICHFACES_INSTALLED;
	}

	/**
	 * RichFaces PartialViewContext implementation does not extend from PartialViewContextWrapper. So a hack wherin the
	 * exact fully qualified class name needs to be known has to be used to properly extract it from the
	 * {@link FacesContext#getPartialViewContext()}.
	 */
	public static PartialViewContext getRichFacesPartialViewContext() {
		PartialViewContext context = Ajax.getContext();

		while (!context.getClass().getName().equals(RICHFACES_PVC_CLASS_NAME)
			&& context instanceof PartialViewContextWrapper)
		{
			context = ((PartialViewContextWrapper) context).getWrapped();
		}

		if (context.getClass().getName().equals(RICHFACES_PVC_CLASS_NAME)) {
			return context;
		}
		else {
			return null;
		}
	}

	/**
	 * RichFaces PartialViewContext implementation does have the getRenderIds() method properly implemented. So a hack
	 * wherin the exact internal field name needs to be known has to be used to properly extract it from the RichFaces
	 * PartialViewContext implementation.
	 */
	@SuppressWarnings("unchecked")
	public static Collection<String> getRichFacesRenderIds() {
		PartialViewContext richFacesContext = getRichFacesPartialViewContext();
		Collection<String> renderIds = null;

		if (richFacesContext != null) {
			try {
				Field componentRenderIds = richFacesContext.getClass().getDeclaredField("componentRenderIds");
				componentRenderIds.setAccessible(true);
				renderIds = (Collection<String>) componentRenderIds.get(richFacesContext);
			}
			catch (Exception e) {
				throw new FacesException(String.format(ERROR_RICHFACES_PVC_RENDERIDS, richFacesContext), e);
			}
		}

		if (renderIds != null) {
			return renderIds;
		}
		else {
			return Collections.emptyList();
		}
	}

	/**
	 * RichFaces PartialViewContext implementation does not have any getWrapped() method to return the wrapped
	 * PartialViewContext. So a reflection hack is necessary to return it.
	 */
	public static PartialViewContext getRichFacesWrappedPartialViewContext() {
		PartialViewContext richFacesContext = getRichFacesPartialViewContext();

		if (richFacesContext != null) {
			try {
				Field wrappedViewContext = richFacesContext.getClass().getDeclaredField("wrappedViewContext");
				wrappedViewContext.setAccessible(true);
				return (PartialViewContext) wrappedViewContext.get(richFacesContext);
			}
			catch (Exception e) {
				throw new FacesException(String.format(ERROR_RICHFACES_PVC_WRAPPED_VC, richFacesContext), e);
			}
		}

		return null;
	}

	/**
	 * RichFaces "resource optimization" do not support getURL() and getInputStream(). The combined resource handler
	 * has to manually create the URL based on getRequestPath() and the current request domain URL whenever RichFaces
	 * "resource optimization" is enabled. This field is package private because CombinedResourceInputStream also need
	 * to know about this.
	 */
	public static boolean isRichFacesResourceOptimizationEnabled() {
		return RICHFACES_RESOURCE_OPTIMIZATION_ENABLED;
	}

}