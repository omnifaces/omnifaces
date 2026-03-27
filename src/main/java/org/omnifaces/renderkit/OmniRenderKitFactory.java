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

import java.util.Iterator;

import jakarta.faces.context.FacesContext;
import jakarta.faces.render.RenderKit;
import jakarta.faces.render.RenderKitFactory;

/**
 * This render kit factory takes care that the {@link OmniRenderKit} is properly initialized.
 *
 * @author Bauke Scholtz
 * @since 5.0
 * @see OmniRenderKit
 */
public class OmniRenderKitFactory extends RenderKitFactory {

    public OmniRenderKitFactory(RenderKitFactory wrapped) {
        super(wrapped);
    }

    @Override
    public void addRenderKit(String renderKitId, RenderKit renderKit) {
        getWrapped().addRenderKit(renderKitId, renderKit);
    }

    @Override
    public RenderKit getRenderKit(FacesContext context, String renderKitId) {
        var renderKit = getWrapped().getRenderKit(context, renderKitId);
        return HTML_BASIC_RENDER_KIT.equals(renderKitId) ? new OmniRenderKit(renderKit) : renderKit;
    }

    @Override
    public Iterator<String> getRenderKitIds() {
        return getWrapped().getRenderKitIds();
    }

}
