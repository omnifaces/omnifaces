/*
 * Copyright 2021 OmniFaces
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
package org.omnifaces.renderer;

import static org.omnifaces.resourcehandler.DefaultResourceHandler.RES_NOT_FOUND;
import static org.omnifaces.util.FacesLocal.createResource;
import static org.omnifaces.util.Utils.isEmpty;

import java.io.IOException;
import java.util.Map;

import jakarta.faces.application.Resource;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.render.FacesRenderer;
import jakarta.faces.render.Renderer;

import org.omnifaces.component.script.DeferredScript;
import org.omnifaces.component.script.ScriptFamily;
import org.omnifaces.resourcehandler.CombinedResourceHandler;

/**
 * This renderer is the default renderer of {@link DeferredScript}. The rendering is extracted from the component so
 * that it can be reused by {@link CombinedResourceHandler}.
 *
 * @author Bauke Scholtz
 * @since 1.8
 */
@FacesRenderer(componentFamily=ScriptFamily.COMPONENT_FAMILY, rendererType=DeferredScriptRenderer.RENDERER_TYPE)
public class DeferredScriptRenderer extends Renderer {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The standard renderer type. */
	public static final String RENDERER_TYPE = "org.omnifaces.DeferredScript";

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Writes a <code>&lt;script&gt;</code> element which calls <code>OmniFaces.DeferredScript.add</code> with as
	 * arguments the script URL and, if any, the onbegin, onsuccess and/or onerror callbacks. If the script resource is
	 * not resolvable, then a <code>RES_NOT_FOUND</code> will be written to <code>src</code> attribute instead.
	 */
	@Override
	public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
		Map<String, Object> attributes = component.getAttributes();
		String library = (String) attributes.get("library");
		String name = (String) attributes.get("name");
		Resource resource = createResource(context, library, name);

		ResponseWriter writer = context.getResponseWriter();
		writer.startElement("script", component);
		writer.writeAttribute("type", "text/javascript", "type");

		if (resource != null) {
			writer.write("OmniFaces.DeferredScript.add('");
			writer.write(resource.getRequestPath());
			writer.write('\'');

			String onbegin = (String) attributes.get("onbegin");
			String onsuccess = (String) attributes.get("onsuccess");
			String onerror = (String) attributes.get("onerror");
			boolean hasOnbegin = !isEmpty(onbegin);
			boolean hasOnsuccess = !isEmpty(onsuccess);
			boolean hasOnerror = !isEmpty(onerror);

			if (hasOnbegin || hasOnsuccess || hasOnerror) {
				encodeFunctionArgument(writer, onbegin, hasOnbegin);
			}

			if (hasOnsuccess || hasOnerror) {
				encodeFunctionArgument(writer, onsuccess, hasOnsuccess);
			}

			if (hasOnerror) {
				encodeFunctionArgument(writer, onerror, true);
			}

			writer.write(");");
		}
		else {
			writer.writeURIAttribute("src", RES_NOT_FOUND, "src");
		}
	}

	private void encodeFunctionArgument(ResponseWriter writer, String function, boolean hasFunction) throws IOException {
		if (hasFunction) {
			writer.write(",function() {");
			writer.write(function);
			writer.write('}');
		}
		else {
			writer.write(",null");
		}
	}

	@Override
	public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
		ResponseWriter writer = context.getResponseWriter();
		writer.endElement("script");
	}

}