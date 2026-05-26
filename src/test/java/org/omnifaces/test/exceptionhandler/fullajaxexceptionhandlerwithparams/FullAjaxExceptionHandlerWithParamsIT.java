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
package org.omnifaces.test.exceptionhandler.fullajaxexceptionhandlerwithparams;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omnifaces.test.OmniFacesIT.FacesConfig.withFullAjaxExceptionHandler;
import static org.omnifaces.test.OmniFacesIT.WebXml.withErrorPageAndParams;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

@DisabledIfSystemProperty(
    named = "profile.id", matches = "tomee-.*", disabledReason = "Bumps into OWB-1462 / TOMEE-4603: OpenWebBeans' NormalScopeProxyFactory generates the same proxy class name (org.apache.webbeans.custom.Map$$OwbNormalScopeProxy<hash>) for every @NormalScope bean of the same erased type, so the second Faces Map-typed implicit bean evaluated in a webapp (here #{param}, after #{requestScope} won the slot) hits LinkageError: attempted duplicate class definition. Fixed in TomEE 10.1.6 and 11.0.0 (both unreleased as of bump to 10.1.5); re-enable when 10.1.6 is on Maven Central."
)
public class FullAjaxExceptionHandlerWithParamsIT extends OmniFacesIT {

    @FindBy(id = "exception")
    private WebElement exception;

    @FindBy(id = "reason")
    private WebElement reason;

    @FindBy(id = "form:throwSomething")
    private WebElement throwSomething;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return buildWebArchive(FullAjaxExceptionHandlerWithParamsIT.class)
            .withFacesConfig(withFullAjaxExceptionHandler)
            .withWebXml(withErrorPageAndParams)
            .withPrimeFaces()
            .createDeployment();
    }

    @Test
    void errorPageReceivesQueryStringParams() {
        guardAjax(throwSomething::click);
        assertTrue(exception.getText().contains("throwSomething"));
        assertEquals("oopsie", reason.getText());
    }

}
