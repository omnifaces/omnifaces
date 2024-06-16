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
package org.omnifaces.renderer;

import static org.omnifaces.util.FacesLocal.isOutputHtml5Doctype;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

import jakarta.faces.component.UIOutput;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.render.FacesRenderer;

import org.omnifaces.resourcehandler.CombinedResourceHandler;

/**
 * This renderer enables rendering a CSS resource inline.
 * This is internally only used by {@link CombinedResourceHandler}
 *
 * @author Bauke Scholtz
 * @since 1.2
 * @see CombinedResourceHandler
 */
@FacesRenderer(componentFamily=UIOutput.COMPONENT_FAMILY, rendererType=InlineStylesheetRenderer.RENDERER_TYPE)
public class InlineStylesheetRenderer extends InlineResourceRenderer {

    // Constants ------------------------------------------------------------------------------------------------------

    /** The standard renderer type. */
    public static final String RENDERER_TYPE = "org.omnifaces.InlineStylesheet";

    private static final int BUFFER_SIZE = 10240;

    // Actions --------------------------------------------------------------------------------------------------------

    @Override
    public void startElement(FacesContext context, ResponseWriter writer, UIOutput component) throws IOException {
        writer.startElement("style", component);

        if (!isOutputHtml5Doctype(context)) {
            writer.writeAttribute("type", "text/css", "type");
        }

        writer.write("/*<![CDATA[*/\n");
    }

    @Override
    public void writeResource(Reader reader, ResponseWriter writer) throws IOException {
        var buffer = CharBuffer.allocate(BUFFER_SIZE);

        while (reader.read(buffer) != -1) {
            buffer.flip();
            writer.append(buffer);
            buffer.clear();
        }
    }

    @Override
    public void endElement(ResponseWriter writer) throws IOException {
        writer.write("\n/*]]>*/");
        writer.endElement("style");
    }

}