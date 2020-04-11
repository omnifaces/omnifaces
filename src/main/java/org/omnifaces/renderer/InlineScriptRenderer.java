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
package org.omnifaces.renderer;

import java.io.IOException;
import java.io.Reader;

import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIOutput;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.render.FacesRenderer;

import org.omnifaces.resourcehandler.CombinedResourceHandler;

/**
 * This renderer enables rendering a JS resource inline.
 * This is internally only used by {@link CombinedResourceHandler}
 *
 * @author Bauke Scholtz
 * @since 1.2
 * @see CombinedResourceHandler
 */
@FacesRenderer(componentFamily=UIOutput.COMPONENT_FAMILY, rendererType=InlineScriptRenderer.RENDERER_TYPE)
public class InlineScriptRenderer extends InlineResourceRenderer {

	// Constants ------------------------------------------------------------------------------------------------------

	/** The standard renderer type. */
	public static final String RENDERER_TYPE = "org.omnifaces.InlineScript";

	private static final char[] END_SCRIPT = "/script>".toCharArray();

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public void startElement(ResponseWriter writer, UIComponent component) throws IOException {
		writer.startElement("script", component);
		writer.writeAttribute("type", "text/javascript", "type");
		writer.write("//<![CDATA[\n");
	}

	@Override
	public void writeResource(Reader reader, ResponseWriter writer) throws IOException {
		for (int c = reader.read(); c != -1; c = reader.read()) {
			writer.write(c);

			if (c == '<') {
				escapeEndScriptIfNecessary(reader, writer);
			}
		}
	}

	private void escapeEndScriptIfNecessary(Reader reader, ResponseWriter writer) throws IOException {
		int length = 0;

		for (int ch = reader.read(); ch != -1; ch = reader.read()) {
			if (Character.toLowerCase(ch) != END_SCRIPT[length]) {
				if (length > 0) {
					writer.write(END_SCRIPT, 0, length);
				}

				writer.write(ch);
				return;
			}

			length++;

			if (length == END_SCRIPT.length) {
				writer.write('\\'); // Escape closing script tags which may occur in JS literals.
				writer.write(END_SCRIPT);
				return;
			}
		}
	}

	@Override
	public void endElement(ResponseWriter writer) throws IOException {
		writer.write("\n//]]>");
		writer.endElement("script");
	}

}