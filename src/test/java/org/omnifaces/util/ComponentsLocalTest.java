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
package org.omnifaces.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ComponentsLocalTest {

    @Mock
    private FacesContext mockedFacesContext;

    @Mock
    private ExternalContext mockedExternalContext;

    private Map<String, Object> mockedApplicationMap;

    @BeforeEach
    void setUp() {
        mockedApplicationMap = new HashMap<>();
        when(mockedExternalContext.getApplicationMap()).thenReturn(mockedApplicationMap);
        when(mockedFacesContext.getExternalContext()).thenReturn(mockedExternalContext);
        when(mockedFacesContext.getNamingContainerSeparatorChar()).thenReturn(':');
    }

    @Test
    void testStripIterationIndex_simpleCase() {
        // Given: A client ID with one iteration index
        var clientId = "form:table:0:input";

        // When: Stripping iteration index
        String result = ComponentsLocal.stripIterationIndex(mockedFacesContext, clientId);

        // Then: The iteration index should be removed
        assertEquals("form:table:input", result);
    }

    @Test
    void testStripIterationIndex_multipleIterationIndices() {
        // Given: A client ID with multiple iteration indices (nested UIData)
        var clientId = "form:outerTable:5:innerTable:12:input";

        // When: Stripping iteration indices
        String result = ComponentsLocal.stripIterationIndex(mockedFacesContext, clientId);

        // Then: All iteration indices should be removed
        assertEquals("form:outerTable:innerTable:input", result);
    }

    @Test
    void testStripIterationIndex_noIterationIndex() {
        // Given: A client ID without any iteration index
        var clientId = "form:input:button";

        // When: Stripping iteration index
        String result = ComponentsLocal.stripIterationIndex(mockedFacesContext, clientId);

        // Then: The client ID should remain unchanged
        assertEquals("form:input:button", result);
    }

    @Test
    void testStripIterationIndex_iterationIndexAtEnd() {
        // Given: A client ID ending with an iteration index
        var clientId = "form:table:99:";

        // When: Stripping iteration index
        String result = ComponentsLocal.stripIterationIndex(mockedFacesContext, clientId);

        // Then: The iteration index should be removed
        assertEquals("form:table:", result);
    }

    @Test
    void testStripIterationIndex_iterationIndexAtStart() {
        // Given: A client ID starting with an iteration index
        var clientId = "0:table:input";

        // When: Stripping iteration index
        String result = ComponentsLocal.stripIterationIndex(mockedFacesContext, clientId);

        // Then: The iteration index should be removed
        assertEquals("table:input", result);
    }

    @Test
    void testStripIterationIndex_largeIterationIndex() {
        // Given: A client ID with a large iteration number
        var clientId = "form:table:123456:input";

        // When: Stripping iteration index
        String result = ComponentsLocal.stripIterationIndex(mockedFacesContext, clientId);

        // Then: The iteration index should be removed
        assertEquals("form:table:input", result);
    }

    @Test
    void testStripIterationIndex_emptyString() {
        // Given: An empty client ID
        var clientId = "";

        // When: Stripping iteration index
        String result = ComponentsLocal.stripIterationIndex(mockedFacesContext, clientId);

        // Then: Should return empty string
        assertEquals("", result);
    }

    @Test
    void testStripIterationIndex_singleComponent() {
        // Given: A single component name without separators
        var clientId = "input";

        // When: Stripping iteration index
        String result = ComponentsLocal.stripIterationIndex(mockedFacesContext, clientId);

        // Then: Should return the same name
        assertEquals("input", result);
    }

    @Test
    void testStripIterationIndex_componentNamesWithDigits() {
        // Given: Component names containing digits (not iteration indices)
        var clientId = "form1:table2:input3";

        // When: Stripping iteration index
        String result = ComponentsLocal.stripIterationIndex(mockedFacesContext, clientId);

        // Then: Component names with digits should remain unchanged
        assertEquals("form1:table2:input3", result);
    }

    @Test
    void testStripIterationIndex_componentNamesWithDigitsAndIterationIndex() {
        // Given: Component names with digits AND an iteration index
        var clientId = "form1:table2:5:input3";

        // When: Stripping iteration index
        String result = ComponentsLocal.stripIterationIndex(mockedFacesContext, clientId);

        // Then: Only the iteration index should be removed, not the digits in component names
        assertEquals("form1:table2:input3", result);
    }

    @Test
    void testStripIterationIndex_componentNamesWithDigitsAndMultipleIterationIndices() {
        // Given: Component names with digits AND multiple iteration indices
        var clientId = "form1:outerTable2:7:innerTable3:99:input4";

        // When: Stripping iteration indices
        String result = ComponentsLocal.stripIterationIndex(mockedFacesContext, clientId);

        // Then: Only the iteration indices should be removed
        assertEquals("form1:outerTable2:innerTable3:input4", result);
    }

    @Test
    void testStripIterationIndex_allDigitComponentName() {
        // Given: A component name that is all digits (but part of the name, not an iteration index)
        var clientId = "form:component123:button";

        // When: Stripping iteration index
        String result = ComponentsLocal.stripIterationIndex(mockedFacesContext, clientId);

        // Then: The component name with digits should remain unchanged
        assertEquals("form:component123:button", result);
    }

    @Test
    void testStripIterationIndex_digitPrefixAndSuffix() {
        // Given: Component names with digit prefixes and suffixes
        var clientId = "1form:2table:3:button4";

        // When: Stripping iteration index
        String result = ComponentsLocal.stripIterationIndex(mockedFacesContext, clientId);

        // Then: Only pure numeric iteration index should be removed
        assertEquals("1form:2table:button4", result);
    }

    @Test
    void testStripIterationIndex_consecutiveDigitsInComponentName() {
        // Given: Component name with consecutive digits
        var clientId = "form:table123abc:0:input";

        // When: Stripping iteration index
        String result = ComponentsLocal.stripIterationIndex(mockedFacesContext, clientId);

        // Then: Only the iteration index (pure digits) should be removed
        assertEquals("form:table123abc:input", result);
    }

    @Test
    void testStripIterationIndex_mixedScenario() {
        // Given: Complex scenario with component names containing digits and iteration indices
        var clientId = "myForm1:dataTable2:15:column3:nested4:42:outputText5";

        // When: Stripping iteration indices
        String result = ComponentsLocal.stripIterationIndex(mockedFacesContext, clientId);

        // Then: Only iteration indices (15, 42) should be removed, component name digits remain
        assertEquals("myForm1:dataTable2:column3:nested4:outputText5", result);
    }

    @Test
    void testStripIterationIndex_startingWithDigitComponentName() {
        // Given: Component starting with digits followed by letters (not an iteration index)
        var clientId = "123form:456table:7:input";

        // When: Stripping iteration index
        String result = ComponentsLocal.stripIterationIndex(mockedFacesContext, clientId);

        // Then: Only the pure numeric iteration index should be removed
        assertEquals("123form:456table:input", result);
    }
}
