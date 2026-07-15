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
package org.omnifaces.test.component.tree;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class TreeIT extends OmniFacesIT {

    private static final List<String> EXPECTED_NODES_DEPTH_FIRST = List.of("A", "A1", "A2", "B", "B1");

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return createWebArchive(TreeIT.class);
    }

    @Test
    void testRendersAllNodesDepthFirst() {
        assertEquals(EXPECTED_NODES_DEPTH_FIRST, renderedNames());

        // The level attribute of o:treeNode must select the right template per tree node level.
        assertEquals("0", name("A").getAttribute("data-level"));
        assertEquals("0", name("B").getAttribute("data-level"));
        assertEquals("1plus", name("A1").getAttribute("data-level"));
        assertEquals("1plus", name("B1").getAttribute("data-level"));
    }

    @Test
    void testStatePreservedAcrossPostback() {
        assertEquals(EXPECTED_NODES_DEPTH_FIRST, renderedNames());

        value("A1").sendKeys("x1");
        value("B1").sendKeys("x2");
        guardHttp(() -> browser.findElement(By.id("form:submit")).click());

        assertEquals(EXPECTED_NODES_DEPTH_FIRST, renderedNames(), "Tree structure intact after postback re-render.");
        assertEquals("x1", value("A1").getAttribute("value"));
        assertEquals("x2", value("B1").getAttribute("value"));
    }

    private List<String> renderedNames() {
        return browser.findElements(By.cssSelector(".name")).stream().map(WebElement::getText).collect(toList());
    }

    private WebElement name(String nodeName) {
        return browser.findElement(By.cssSelector(".name[data-name='" + nodeName + "']"));
    }

    private WebElement value(String nodeName) {
        return browser.findElement(By.cssSelector("input.value[data-name='" + nodeName + "']"));
    }

}
