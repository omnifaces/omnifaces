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
package org.omnifaces.test.csp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.omnifaces.test.OmniFacesIT.WebXml.withCspNonce;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;

public class CspNonceIT extends OmniFacesIT {

    private static final Pattern NONCE_IN_POLICY = Pattern.compile("'nonce-([^']+)'");
    private static final Pattern SCRIPT_ELEMENT = Pattern.compile("<script\\b[^>]*>");
    private static final Pattern NONCE_ATTRIBUTE = Pattern.compile("\\bnonce=\"([^\"]*)\"");

    @Deployment(testable=false)
    public static WebArchive createDeployment() {
        return buildWebArchive(CspNonceIT.class)
            .withWebXml(withCspNonce)
            .createDeployment();
    }

    /**
     * Every script element in the response must carry the CSP nonce of the response, else the browser blocks it. This
     * includes the inline scripts rendered by OmniFaces: the unload script of a view scoped bean, an onload script and a
     * deferred script.
     */
    @Test
    void everyScriptMustCarryCspNonceOfResponse() throws IOException {
        var connection = openConnection("CspNonceIT.xhtml", null);
        var policy = connection.getHeaderField("Content-Security-Policy");
        assumeTrue(policy != null, "the Faces implementation supports CSP nonce");
        Matcher policyNonce = NONCE_IN_POLICY.matcher(policy);
        assertTrue(policyNonce.find(), () -> "nonce in policy: " + policy);
        var nonce = policyNonce.group(1);

        var body = readResponseBody(connection);
        assertTrue(body.contains("OmniFaces.Unload.init("), "unload script is rendered");
        assertTrue(body.contains("OmniFaces.DeferredScript.add("), "deferred script is rendered");
        assertTrue(body.contains("cspNonceITOnload"), "onload script is rendered");

        Matcher scriptElement = SCRIPT_ELEMENT.matcher(body);

        while (scriptElement.find()) {
            var script = scriptElement.group();
            Matcher scriptNonce = NONCE_ATTRIBUTE.matcher(script);
            assertTrue(scriptNonce.find(), () -> "nonce in " + script);
            assertEquals(nonce, scriptNonce.group(1), () -> "nonce in " + script);
        }
    }

}
