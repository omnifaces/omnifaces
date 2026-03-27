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
package org.omnifaces.test.renderer.corsawareresourcerenderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.omnifaces.test.OmniFacesIT.FacesConfig.withCustomCDNResourceHandler;

import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class CorsAwareResourceRendererIT extends OmniFacesIT {

    @FindBy(css = "script[src]")
    private List<WebElement> scripts;

    @FindBy(css = "link[rel=stylesheet]")
    private List<WebElement> stylesheets;

    @FindBy(id = "bodyWithTargetBody")
    private WebElement bodyWithTargetBody;

    @FindBy(id = "headWithoutTarget")
    private WebElement headWithoutTarget;

    @FindBy(id = "headWithTarget")
    private WebElement headWithTarget;

    @FindBy(id = "bodyWithTargetHead")
    private WebElement bodyWithTargetHead;

    @FindBy(id = "bodyWithoutTarget")
    private WebElement bodyWithoutTarget;

    @FindBy(id = "deferredInHead")
    private WebElement deferredInHead;

    @FindBy(id = "deferredInBody")
    private WebElement deferredInBody;

    @FindBy(id = "nonAjax:submit")
    private WebElement nonAjaxSubmit;

    @FindBy(id = "nonAjax:rebuild")
    private WebElement nonAjaxRebuild;

    @FindBy(id = "ajax:submit")
    private WebElement ajaxSubmit;

    @FindBy(id = "ajax:rebuild")
    private WebElement ajaxRebuild;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return buildWebArchive(CorsAwareResourceRendererIT.class)
            .withFacesConfig(withCustomCDNResourceHandler)
            .createDeployment();
    }

    @Test
    void nonAjaxLocal() {
        open("CorsAwareResourceRendererIT.xhtml?skipCDN=true");
        verifyElements(false);
        guardHttp(nonAjaxSubmit::click);
        verifyElements(false);
        guardHttp(nonAjaxRebuild::click);
        verifyElements(false);
    }

    @Test
    void nonAjaxCDN() {
        open("CorsAwareResourceRendererIT.xhtml?skipCDN=false");
        verifyElements(true);
        guardHttp(nonAjaxSubmit::click);
        verifyElements(true);
        guardHttp(nonAjaxRebuild::click);
        verifyElements(true);
    }

    @Test
    void ajaxLocal() {
        open("CorsAwareResourceRendererIT.xhtml?skipCDN=true");
        verifyElements(false);
        guardAjax(ajaxSubmit::click);
        verifyElements(false);
        guardAjax(ajaxRebuild::click);
        verifyElements(false);
    }

    @Test
    void ajaxCDN() {
        open("CorsAwareResourceRendererIT.xhtml?skipCDN=false");
        verifyElements(true);
        guardAjax(ajaxSubmit::click);
        verifyElements(true);
        guardAjax(ajaxRebuild::click);
        verifyElements(true);
    }

    private void verifyElements(boolean cdn) {
        waitUntilTextContent(deferredInBody); // Wait until last o:deferredScript is finished.

        assertEquals(9, scripts.size());
        verifyResource(cdn, scripts.get(0), "headWithoutTarget.js", "sha384-gznUcovbufIIDvmyJg3HGej1em1Wg0KSPR14QNgpqu84TA8XWD8taMq0gDymjbjd");
        verifyResource(cdn, scripts.get(1), "omnifaces.js", null); // Integrity depends on OmniFaces JS source changes and therefore fluctuates during
                                                                   // development, so we skip integrity check on omnifaces.js.
        verifyResource(cdn, scripts.get(2), "faces.js", null); // Integrity depends on Faces impl being used by server and therefore fluctuates across test
                                                               // environments, so we skip integrity check on faces.js.
        verifyResource(cdn, scripts.get(3), "headWithTarget.js", "sha384-krDifaBExNHcMs9Yv1ZQAgmP53EhTO2OPqaBFT8MeGKADaqT+NVjuc8crvGE/qbW");
        verifyResource(cdn, scripts.get(4), "bodyWithTargetHead.js", "sha384-180HIFdg/5q9rsSGfQZ+CApl+wuRymzd+CNTzfce6o6LKSh0hrpSPQcAr5TQvA4Y");
        verifyResource(cdn, scripts.get(5), "deferredInHead.js", "sha384-6Hy/7a4f4PqRSr5HGMWEWaZjJgFR1zR/QrQxaVMSYgWBgvuyTlrdExzNH9DFfzIY");
        verifyResource(cdn, scripts.get(6), "deferredInBody.js", "sha384-/Uj31px3wJbXzk0r+YBFtkr4t+g1owQQyK3yrr20RrvAiJ7Ypa2IWWOX26JRZaig");
        verifyResource(cdn, scripts.get(7), "bodyWithoutTarget.js", "sha384-EMmkadXDdPdNjCflKfSSW8A9XMCMbiEmLsluVtWZqaQ3mK7jLI8NU2oJVcevT0Eu");
        verifyResource(cdn, scripts.get(8), "bodyWithTargetBody.js", "sha384-JnLkXmxPO/A8G7J/VtTg9CHTnnrTJPfDsjL3QQvhiAowR8WbPaUDkOpgAp044kIg");

        assertEquals(4, stylesheets.size());
        verifyResource(cdn, stylesheets.get(0), "main.css", "sha384-hmM7cEaWvNHIMK+DAetvH/W2YeFiZ4/TICjWYU6p6xAPd6jJXj56lPbAYz2CTeUQ");
        verifyResource(cdn, stylesheets.get(1), "screen.css", "sha384-hmM7cEaWvNHIMK+DAetvH/W2YeFiZ4/TICjWYU6p6xAPd6jJXj56lPbAYz2CTeUQ");
        verifyResource(cdn, stylesheets.get(2), "print.css", "sha384-AIsliUu+8ppE/NCo06ISdZKX7hhjAdYQJMrbMW/txDh/v0tKmoe6aFQzfV6K0L9g");
        verifyResource(cdn, stylesheets.get(3), "critical.css", "sha384-nU5EqlzYAANG4pJ2kuSgvHzDKJjS8V27+XyDben6m+nehM68hlUvEpU7fmey3W3w");

        assertEquals("1,bodyWithTargetBody", bodyWithTargetBody.getText());
        assertEquals("2,headWithoutTarget", headWithoutTarget.getText());
        assertEquals("3,headWithTarget", headWithTarget.getText());
        assertEquals("4,bodyWithTargetHead", bodyWithTargetHead.getText());
        assertEquals("5,bodyWithoutTarget", bodyWithoutTarget.getText());
        assertEquals("6,deferredInHead", deferredInHead.getText());
        assertEquals("7,deferredInBody", deferredInBody.getText());
    }

    private static void verifyResource(boolean cdn, WebElement element, String name, String integrity) {
        var src = element.getAttribute("script".equals(element.getTagName()) ? "src" : "href");
        assertEquals(name, src.split("/jakarta\\.faces\\.resource/", 2)[1].split(".xhtml", 2)[0]);
        assertEquals("anonymous", element.getAttribute("crossorigin"));

        if (integrity != null) {
            assertEquals(cdn ? integrity : "", element.getAttribute("integrity"));
        }
    }

}
