/*
 * Copyright 2017 OmniFaces
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
package org.omnifaces.renderkit;

import java.util.Iterator;

import javax.faces.context.FacesContext;
import javax.faces.render.RenderKit;
import javax.faces.render.RenderKitFactory;

/**
 * This render kit factory needs to be registered as follows in <code>faces-config.xml</code> to get the
 * {@link Html5RenderKit} to run:
 * <pre>
 * &lt;factory&gt;
 *   &lt;render-kit-factory&gt;
 *     org.omnifaces.renderkit.Html5RenderKitFactory
 *   &lt;/render-kit-factory&gt;
 * &lt;/factory&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 * @since 1.1
 * @deprecated
 * @see Html5RenderKit
 */
@Deprecated
public class Html5RenderKitFactory extends RenderKitFactory {

	// Variables ------------------------------------------------------------------------------------------------------

	private RenderKitFactory wrapped;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new HTML5 render kit factory around the given wrapped factory.
	 * @param wrapped The wrapped factory.
	 */
	public Html5RenderKitFactory(RenderKitFactory wrapped) {
		this.wrapped = wrapped;
	}

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public void addRenderKit(String renderKitId, RenderKit renderKit) {
		wrapped.addRenderKit(renderKitId, renderKit);
	}

	/**
	 * If the given render kit ID equals to {@link RenderKitFactory#HTML_BASIC_RENDER_KIT}, then return a new
	 * {@link Html5RenderKit} instance which wraps the original render kit, else return the original render kit.
	 */
	@Override
	public RenderKit getRenderKit(FacesContext context, String renderKitId) {
		RenderKit renderKit = wrapped.getRenderKit(context, renderKitId);
		return (HTML_BASIC_RENDER_KIT.equals(renderKitId)) ? new Html5RenderKit(renderKit) : renderKit;
	}

	@Override
	public Iterator<String> getRenderKitIds() {
		return wrapped.getRenderKitIds();
	}

}