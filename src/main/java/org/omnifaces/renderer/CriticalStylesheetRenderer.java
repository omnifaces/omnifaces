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

import static org.omnifaces.resourcehandler.DefaultResourceHandler.RES_NOT_FOUND;
import static org.omnifaces.util.Components.getAttribute;
import static org.omnifaces.util.Components.setAttribute;
import static org.omnifaces.util.FacesLocal.createResource;
import static org.omnifaces.util.Renderers.writeAttribute;

import java.io.IOException;

import jakarta.faces.application.Resource;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.render.FacesRenderer;
import jakarta.faces.render.Renderer;

import org.omnifaces.component.stylesheet.CriticalStylesheet;
import org.omnifaces.component.stylesheet.StylesheetFamily;
import org.omnifaces.resourcehandler.ResourceIdentifier;

/**
 * This renderer is the default renderer of {@link CriticalStylesheet}.
 *
 * @author Bauke Scholtz
 * @since 4.5
 */
@FacesRenderer(componentFamily=StylesheetFamily.COMPONENT_FAMILY, rendererType=CriticalStylesheetRenderer.RENDERER_TYPE)
public class CriticalStylesheetRenderer extends Renderer {

    // Public constants -----------------------------------------------------------------------------------------------

    /** The standard renderer type. */
    public static final String RENDERER_TYPE = "org.omnifaces.CriticalStylesheet";

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * Writes a <code>&lt;link rel="preload" as="style"&gt;</code> element with a <code>href</code> attribute
     * representing {@link Resource#getRequestPath()}. If the resource is not resolvable, then a
     * <code>RES_NOT_FOUND</code> will be written to the <code>href</code> attribute instead.
     */
    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        ResourceIdentifier id = new ResourceIdentifier(component);
        Resource resource = createResource(context, id);
        String href = (resource != null) ? resource.getRequestPath() : RES_NOT_FOUND;
        setAttribute(component, "href", href);

        ResponseWriter writer = context.getResponseWriter();
        writer.startElement("link", component);
        writeAttribute(writer, "rel", "preload");
        writer.writeURIAttribute("href", href, "href");
        writeAttribute(writer, "as", "style");
        writeAttribute(writer, component, "media");
        writeAttribute(writer, "onload", "this.onload=null;this.rel='stylesheet'");
    }

    /**
     * Ends the <code>&lt;link&gt;</code> and writes a <code>&lt;noscript&gt;</code> with a "plain vanilla"
     * <code>&lt;link rel="stylesheet"&gt;</code>
     */
    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        writer.endElement("link");
        writer.startElement("noscript", component);
        writer.startElement("link", component);
        writeAttribute(writer, "rel", "stylesheet");
        writer.writeURIAttribute("href", getAttribute(component, "href"), "href");
        writeAttribute(writer, component, "media");
        writer.endElement("noscript");

        context.getApplication().getResourceHandler().markResourceRendered(context, getAttribute(component, "name"), getAttribute(component, "library"));
    }

}
