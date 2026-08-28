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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.omnifaces.renderer.CorsAwareResourceRenderer.DEFAULT_CROSSORIGIN;
import static org.omnifaces.renderer.CorsAwareResourceRenderer.getCrossoriginIfNecessary;

import java.util.HashMap;

import jakarta.faces.application.Resource;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;

class CorsAwareResourceRendererTest {

    private static final String ORIGIN = "https://example.com";
    private static final String REQUEST_URL = ORIGIN + "/context/page.xhtml";
    private static final String NO_CROSSORIGIN = "";

    @Test
    void sameOriginResourceGetsNoCrossorigin() {
        assertEquals(NO_CROSSORIGIN, getCrossorigin("/context/jakarta.faces.resource/script.js.xhtml?ln=library"));
        assertEquals(NO_CROSSORIGIN, getCrossorigin(ORIGIN + "/context/jakarta.faces.resource/script.js.xhtml"));
        assertEquals(NO_CROSSORIGIN, getCrossorigin("//example.com/context/script.js"));
    }

    @Test
    void anotherOriginResourceGetsCrossorigin() {
        assertEquals(DEFAULT_CROSSORIGIN, getCrossorigin("https://cdn.example.com/script.js"));
        assertEquals(DEFAULT_CROSSORIGIN, getCrossorigin("//cdn.example.com/script.js"));
    }

    /**
     * The host boundary must be recognized, else a host which merely starts with the host of the current request would be taken for the same origin.
     */
    @Test
    void hostStartingWithRequestHostGetsCrossorigin() {
        assertEquals(DEFAULT_CROSSORIGIN, getCrossorigin("https://example.com.example.org/script.js"));
        assertEquals(DEFAULT_CROSSORIGIN, getCrossorigin("//example.com.example.org/script.js"));
    }

    @Test
    void anotherSchemeOrPortGetsCrossorigin() {
        assertEquals(DEFAULT_CROSSORIGIN, getCrossorigin("http://example.com/script.js"));
        assertEquals(DEFAULT_CROSSORIGIN, getCrossorigin("https://example.com:8443/script.js"));
    }

    @Test
    void unresolvableResourceGetsNoCrossorigin() {
        assertEquals(NO_CROSSORIGIN, getCrossorigin(null));
        assertEquals(NO_CROSSORIGIN, getCrossorigin(""));
    }

    private static String getCrossorigin(String requestPath) {
        var request = mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));

        var externalContext = mock(ExternalContext.class);
        when(externalContext.getRequest()).thenReturn(request);
        when(externalContext.getApplicationMap()).thenReturn(new HashMap<>());

        var context = mock(FacesContext.class);
        when(context.getExternalContext()).thenReturn(externalContext);

        var resource = mock(Resource.class);
        when(resource.getRequestPath()).thenReturn(requestPath);

        return getCrossoriginIfNecessary(context, resource);
    }

}
