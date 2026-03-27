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
package org.omnifaces.renderkit;

import static org.omnifaces.util.Renderers.RENDERER_TYPE_CSS;
import static org.omnifaces.util.Renderers.RENDERER_TYPE_JS;
import static org.omnifaces.util.Utils.isOneOf;

import jakarta.faces.component.UIOutput;
import jakarta.faces.render.RenderKit;
import jakarta.faces.render.RenderKitWrapper;
import jakarta.faces.render.Renderer;

import org.omnifaces.component.stylesheet.StylesheetFamily;
import org.omnifaces.renderer.CorsAwareResourceRenderer;
import org.omnifaces.renderer.CriticalStylesheetRenderer;

/**
 * OmniFaces render kit. This render kit performs the following tasks:
 * <ol>
 * <li>Since 5.0: Wrap the default renderer of output scripts/stylesheets with {@link CorsAwareResourceRenderer}.
 * </ol>
 *
 * @author Bauke Scholtz
 * @since 5.0
 * @see OmniRenderKitFactory
 * @see CorsAwareResourceRenderer
 */
public class OmniRenderKit extends RenderKitWrapper {

    public OmniRenderKit(RenderKit wrapped) {
        super(wrapped);
    }

    @Override
    public Renderer<?> getRenderer(String family, String rendererType) {
        var renderer = super.getRenderer(family, rendererType);
        var corsSensitiveResource = UIOutput.COMPONENT_FAMILY.equals(family) && isOneOf(rendererType, RENDERER_TYPE_JS, RENDERER_TYPE_CSS) ||
            StylesheetFamily.COMPONENT_FAMILY.equals(family) && CriticalStylesheetRenderer.RENDERER_TYPE.equals(rendererType);
        return corsSensitiveResource ? new CorsAwareResourceRenderer(renderer) : renderer;
    }

}
