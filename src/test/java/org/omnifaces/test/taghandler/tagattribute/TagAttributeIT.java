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
package org.omnifaces.test.taghandler.tagattribute;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.omnifaces.test.OmniFacesIT;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Every tag file in this test app renders <code>(#{attr}:children)</code>, so a nested tag file which correctly sees no attribute of its own renders as
 * <code>(A:(:))</code> and one which inherits the enclosing attribute as <code>(A:(A:))</code>. Tag file <code>declared</code> declares its attribute with
 * <code>&lt;o:tagAttribute&gt;</code>, tag file <code>plain</code> does not.
 */
public class TagAttributeIT extends OmniFacesIT {

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return buildWebArchive(TagAttributeIT.class)
            .withWebXml(WebXml.withTaglib)
            .createDeployment();
    }

    /**
     * A declared attribute is bound to the tag file invocation which declares it, so a nested invocation of the same tag file sees null instead of the value
     * passed to the enclosing invocation.
     */
    @Test
    public void nestedTagFile() {
        assertEquals("(A:(:))", text("nestedTagFile"));
    }

    /**
     * The default value applies whenever the nested invocation was passed no attribute, even when the enclosing invocation has a value for the same attribute
     * name.
     */
    @Test
    public void nestedTagFileWithDefault() {
        assertEquals("(A:(DEF:))", text("nestedTagFileWithDefault"));
    }

    /**
     * An attribute passed to the nested invocation wins over both the enclosing value and the default.
     */
    @Test
    public void nestedTagFileWithOwnAttribute() {
        assertEquals("(A:(B:))", text("nestedTagFileWithOwnAttribute"));
    }

    /**
     * An attribute passed to the nested invocation wins even when it holds the same value as the enclosing one, so recognizing the enclosing declaration cannot
     * rely on the value alone.
     */
    @Test
    public void nestedTagFileWithSameAttribute() {
        assertEquals("(A:(A:))", text("nestedTagFileWithSameAttribute"));
    }

    /**
     * As {@link #nestedTagFileWithSameAttribute()}, for a value expression which parses to the same expression as the enclosing one.
     */
    @Test
    public void nestedTagFileWithSameExpression() {
        assertEquals("(A:(A:))", text("nestedTagFileWithSameExpression"));
    }

    /**
     * The attribute is cleared at every nesting level, not only at the first one.
     */
    @Test
    public void nestedTagFileThreeLevels() {
        assertEquals("(A:(:(:)))", text("nestedTagFileThreeLevels"));
    }

    /**
     * A tag file invocation is a scope boundary regardless of how it is reached, so an intermediate <code>&lt;ui:include&gt;</code> does not reopen the
     * enclosing attribute.
     */
    @Test
    public void nestedTagFileThroughInclude() {
        assertEquals("(A:(:))", text("nestedTagFileThroughInclude"));
    }

    /**
     * As {@link #nestedTagFileThroughInclude()}, for an intermediate <code>&lt;ui:decorate&gt;</code>.
     */
    @Test
    public void nestedTagFileThroughTemplate() {
        assertEquals("(A:(:))", text("nestedTagFileThroughTemplate"));
    }

    /**
     * As {@link #nestedTagFileThroughInclude()}, for an intermediate composite component.
     */
    @Test
    public void nestedTagFileThroughComposite() {
        assertEquals("(A:(:))", text("nestedTagFileThroughComposite"));
    }

    /**
     * A nested tag file declaring the same attribute name must not disturb the enclosing invocation, so the enclosing attribute still holds its own value in
     * the markup which follows the nested tag file.
     */
    @Test
    public void parentTagFile() {
        assertEquals("(A:(:):A)", text("parentTagFile"));
    }

    /**
     * As {@link #parentTagFile()}, and the nested default value must not become the enclosing value either.
     */
    @Test
    public void parentTagFileWithDefault() {
        assertEquals("(A:(DEF:):A)", text("parentTagFileWithDefault"));
    }

    /**
     * A declared attribute does not outlive its tag file invocation, so a sibling invocation starts out empty.
     */
    @Test
    public void siblingTagFile() {
        assertEquals("(A:)(:)", text("siblingTagFile"));
    }

    /**
     * The declaration shields against enclosing tag files which declare their own attributes, which is why every tag file in a nesting chain must declare its
     * attributes. Against an enclosing tag file which does not declare them, whether the value is inherited is up to the Faces implementation, and declaring
     * the attribute must not change that outcome either way.
     */
    @Test
    public void undeclaredParentTagFile() {
        assertEquals(text("undeclaredParentTagFileBaseline"), text("undeclaredParentTagFile"));
    }

    /**
     * As {@link #undeclaredParentTagFile()}, for a variable coming from the enclosing page rather than from an enclosing tag file.
     */
    @Test
    public void uiParam() {
        assertEquals(text("uiParamBaseline"), text("uiParam"));
    }

    /**
     * A declared <code>id</code> without a default is autogenerated per tag file invocation, so sibling invocations end up with distinct client IDs.
     */
    @Test
    public void siblingIds() {
        assertDistinct(clientIds("siblingIds"), 2);
    }

    /**
     * As {@link #siblingIds()}, and nesting is an invocation too, so all three invocations end up with distinct client IDs rather than sharing the outermost
     * one.
     */
    @Test
    public void nestedIds() {
        assertDistinct(clientIds("nestedIds"), 3);
    }

    /**
     * An explicitly passed <code>id</code> is used as is and is not inherited by a nested invocation which was passed none.
     */
    @Test
    public void explicitId() {
        List<String> clientIds = clientIds("explicitId");
        assertEquals("X", clientIds.get(0), "explicitly passed id is used as is");
        assertNotEquals(clientIds.get(0), clientIds.get(1), "nested id is not equal to the explicitly passed id");
    }

    /**
     * A nested tag file which is passed an attribute of its own still falls back to its default for the attributes it was not passed, rather than to the
     * autogenerated ID of the enclosing tag file.
     */
    @Test
    public void childOfGeneratedId() {
        assertEquals("[F_id|F_attr]", text("childOfGeneratedId"));
    }

    /**
     * As {@link #childOfGeneratedId()}, for an enclosing tag file which declares the same attribute name and was passed a value for it.
     */
    @Test
    public void childOfDeclaredAttribute() {
        assertEquals("(A:[F_id|F_attr])", text("childOfDeclaredAttribute"));
    }

    /**
     * As {@link #childOfGeneratedId()}, and each sibling invocation resolves its own defaults independently.
     */
    @Test
    public void siblingChildren() {
        assertEquals("[F_id|F_attr][G_id|G_attr]", text("siblingChildren"));
    }

    /**
     * A variable which an enclosing tag file set with <code>&lt;c:set&gt;</code> is not a tag attribute of that tag file, so declaring it in a nested tag file
     * must not shield it.
     */
    @Test
    public void localVariableOfParentTagFile() {
        assertEquals(text("localVariableOfParentTagFileBaseline"), text("localVariableOfParentTagFile"));
    }

    /**
     * A name which an enclosing tag file declared stays shielded for the remainder of that tag file, so setting it afterwards with <code>&lt;c:set&gt;</code>
     * does not expose it to a nested tag file either.
     */
    @Test
    public void declaredNameSetByParentTagFile() {
        assertEquals("[]", text("declaredNameSetByParentTagFile"));
    }

    private String text(String id) {
        return browser.findElement(By.id(id)).getText().replaceAll("\\s+", "");
    }

    private List<String> clientIds(String id) {
        return browser.findElement(By.id(id)).findElements(By.cssSelector(".identified > .clientId")).stream()
            .map(WebElement::getText)
            .collect(toList());
    }

    private static void assertDistinct(List<String> clientIds, int expectedSize) {
        assertEquals(expectedSize, clientIds.size(), "number of rendered tag files");
        assertEquals(expectedSize, new HashSet<>(clientIds).size(), "number of distinct client IDs: " + clientIds);
    }

}
