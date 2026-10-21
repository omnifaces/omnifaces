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
package org.omnifaces.cdi.viewscope;

import java.io.IOException;
import java.io.StringReader;

import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.FacesRenderer;

import org.omnifaces.cdi.ViewScoped;
import org.omnifaces.renderer.InlineScriptRenderer;

/**
 * This renderer renders the value of the given {@link UIOutput} as the inline script which initializes the unload of
 * the OmniFaces {@link ViewScoped} beans of the current view. Right before that, it puts the pending bean storage of
 * the current view in the active view scopes, see {@link ViewScopeStorageInSession}, because the client is then about
 * to receive the unload script which can destroy them. A failure later in the same response leaves them active until
 * they are evicted from the active view scopes or the HTTP session expires.
 * This is internally only used by {@link ViewScopeManager}.
 *
 * @author Bauke Scholtz
 * @since 3.14.28
 * @see ViewScopeManager
 */
@FacesRenderer(componentFamily=UIOutput.COMPONENT_FAMILY, rendererType=ViewScopeUnloadScriptRenderer.RENDERER_TYPE)
public class ViewScopeUnloadScriptRenderer extends InlineScriptRenderer {

	// Constants ------------------------------------------------------------------------------------------------------

	/** The standard renderer type. */
	public static final String RENDERER_TYPE = "org.omnifaces.ViewScopeUnloadScript";

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Activate the pending view scope of the current view and then write the value of the given {@link UIOutput} as
	 * inline script.
	 */
	@Override
	public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
		ViewScopeStorageInSession.activate(context);
		ResponseWriter writer = context.getResponseWriter();
		startElement(writer, component);
		writeResource(new StringReader(String.valueOf(((UIOutput) component).getValue())), writer);
		endElement(writer);
	}

}
