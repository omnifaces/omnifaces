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
package org.omnifaces.test.resourcehandler.combinedresourcecache;

import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.util.Collections.nCopies;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.omnifaces.test.OmniFacesIT.FacesConfig.withCombinedResourceHandler;
import static org.omnifaces.test.OmniFacesIT.WebXml.withCombinedResourceCacheTTL;
import static org.omnifaces.util.Utils.serializeURLSafe;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Verifies that the combined resource content cache, when activated, grows only for combined resource IDs which this deployment itself rendered. An ID taken
 * from the request URL is unauthenticated and can be minted in unlimited distinct forms which all name the same resources, so caching its content would let any
 * caller exhaust the heap.
 */
public class CombinedResourceCacheIT extends OmniFacesIT {

    private static final int FORGED_IDS = 10;

    @FindBy(css = "link[rel=stylesheet][href*='ln=omnifaces.combined']")
    private WebElement combinedStylesheet;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return buildWebArchive(CombinedResourceCacheIT.class)
            .withFacesConfig(withCombinedResourceHandler)
            .withWebXml(withCombinedResourceCacheTTL)
            .createDeployment();
    }

    @Test
    void renderedIdIsCachedAndForgedIdsAreNot() throws Exception {
        var client = HttpClient.newHttpClient();

        assertEquals(200, get(client, combinedStylesheet.getAttribute("href")), "rendered combined resource is served");
        assertEquals(1, getApplicationCacheSize(client), "rendered combined resource is cached");

        for (var repetitions = 2; repetitions <= FORGED_IDS + 1; repetitions++) {
            var forgedId = serializeURLSafe(String.join("|", nCopies(repetitions, "cacheable.css")));
            var forgedURL = baseURL + "jakarta.faces.resource/" + forgedId + ".css.xhtml?ln=omnifaces.combined";
            assertEquals(200, get(client, forgedURL), "forged combined resource is still served");
        }

        assertEquals(1, getApplicationCacheSize(client), "forged combined resources are not cached");
    }

    private int getApplicationCacheSize(HttpClient client) throws Exception {
        var response = client.send(HttpRequest.newBuilder(URI.create(baseURL + "probe")).build(), ofString());
        assertEquals(200, response.statusCode(), "probe is served");
        return Integer.parseInt(response.body().trim());
    }

    private static int get(HttpClient client, String url) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).build(), ofString()).statusCode();
    }

}
