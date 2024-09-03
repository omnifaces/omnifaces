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
package org.omnifaces.test.resourcehandler.viewresourcehandler;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.omnifaces.test.OmniFacesIT.FacesConfig.withViewResourceHandler;
import static org.omnifaces.test.OmniFacesIT.WebXml.withViewResources;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Scanner;

import org.apache.http.HttpStatus;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.omnifaces.test.OmniFacesIT;

@DisabledIfSystemProperty(named = "profile.id", matches = "piranha-.*", disabledReason = "piranha returns different baseURL on getRequestURL (view-localhost instead of localhost)")
public class ViewResourceHandlerIT extends OmniFacesIT {

    private static final String EXPECTED_CONTENT_TYPE = "text/xml;charset=UTF-8";
    private static final String EXPECTED_XML_PROLOG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String EXPECTED_XML_BODY = ""
        + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
            + "<url><loc>{baseURL}entity.xhtml?id=1</loc><lastmod>2020-12-22T19:20:10Z</lastmod><changefreq>weekly</changefreq><priority>1.0</priority></url>"
            + "<url><loc>{baseURL}entity.xhtml?id=2</loc><lastmod>2020-12-22</lastmod><changefreq>weekly</changefreq><priority>1.0</priority></url>"
            + "<url><loc>{baseURL}entity.xhtml?id=3</loc><lastmod>2020-12-22T15:20:10" + ZoneId.systemDefault().getRules().getOffset(Instant.now()) + "</lastmod><changefreq>weekly</changefreq><priority>1.0</priority></url>"
            + "<url><loc>{baseURL}entity.xhtml?id=4</loc><lastmod>2020-12-22T15:20:10-04:00</lastmod><changefreq>weekly</changefreq><priority>1.0</priority></url>"
        + "</urlset>";

    @Deployment(testable=false)
    public static WebArchive createDeployment() {
        return buildWebArchive(ViewResourceHandlerIT.class)
            .withWebXml(withViewResources)
            .withFacesConfig(withViewResourceHandler)
            .createDeployment();
    }

    @Override
    @BeforeEach
    public void init() {
        // NOOP (there's no XHTML file in this test).
    }

    @Test
    @DisabledIfSystemProperty(named = "profile.id", matches = ".*-myfaces4", disabledReason = "URLs are for some reason mapped to .xml instead of .xhtml?")
    void test() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(baseURL + "sitemap.xml").openConnection();
            assertEquals(HttpStatus.SC_OK, connection.getResponseCode(), "Response code");
            assertEquals(EXPECTED_CONTENT_TYPE, connection.getHeaderField("Content-Type").replace(" ", ""), "Content type");

            String actualPageSource;

            try (Scanner scanner = new Scanner(connection.getInputStream(), UTF_8)) {
                actualPageSource = scanner.useDelimiter("\\A").next();
            }

            String actualXmlProlog = actualPageSource
                .substring(0, actualPageSource.indexOf("<!--")) // That XML comment is the generated license.txt comment.
                .trim();
            String actualXmlBody = actualPageSource
                .substring(actualPageSource.indexOf("-->") + 3)
                .replaceAll(">\\s+<", "><") // Get rid of whitespace between tags. This isn't consistent among servers.
                .trim();

            assertEquals(EXPECTED_XML_PROLOG, actualXmlProlog, "XML prolog");
            assertEquals(EXPECTED_XML_BODY.replace("{baseURL}", baseURL.toString()), actualXmlBody, "Page source");
        }
        catch (Exception e) {
            fail("Exception thrown: " + e);
        }
    }

}