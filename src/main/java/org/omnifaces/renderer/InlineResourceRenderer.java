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
package org.omnifaces.renderer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.faces.application.Resource;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

import org.omnifaces.resourcehandler.CombinedResourceHandler;

/**
 * Base renderer which is to be shared between inline CSS and JS renderers.
 *
 * @author Bauke Scholtz
 * @since 1.2
 * @see CombinedResourceHandler
 */
public abstract class InlineResourceRenderer extends Renderer {

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns <code>true</code>.
	 */
	@Override
	public boolean getRendersChildren() {
		return true;
	}

	/**
	 * Obtain the resource, construct a {@link Reader} around it using the character encoding as obtained from the
	 * response writer and then invoke {@link #startElement(ResponseWriter, UIComponent)},
	 * {@link #writeResource(Reader, ResponseWriter)} and {@link #endElement(ResponseWriter)} in sequence.
	 */
	@Override
	public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
		ResponseWriter writer = context.getResponseWriter();
		String name = (String) component.getAttributes().get("name");
		String library = (String) component.getAttributes().get("library");
		Resource resource = context.getApplication().getResourceHandler().createResource(name, library);

		startElement(writer, component);

		try (Reader reader = new InputStreamReader(resource.getInputStream(), writer.getCharacterEncoding())) {
			writeResource(reader, writer);
		}

		endElement(writer);
	}

	/**
	 * Start the element.
	 * @param writer The response writer.
	 * @param component The {@link UIComponent} to which this element corresponds.
	 * @throws IOException When an I/O error occurs.
	 */
	public abstract void startElement(ResponseWriter writer, UIComponent component) throws IOException;

	/**
	 * Write the resource inline.
	 * @param reader The reader providing the resource content.
	 * @param writer The response writer where the resource content has to be written to.
	 * @throws IOException When an I/O error occurs.
	 */
	public abstract void writeResource(Reader reader, ResponseWriter writer) throws IOException;

	/**
	 * End the element.
	 * @param writer The response writer.
	 * @throws IOException When an I/O error occurs.
	 */
	public abstract void endElement(ResponseWriter writer) throws IOException;

}