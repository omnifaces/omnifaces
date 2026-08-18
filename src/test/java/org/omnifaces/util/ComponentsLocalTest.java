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
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

    static Stream<Arguments> clientIds() {
        return Stream.of(
            arguments("form:table:0:input", "form:table:input"),
            arguments("form:outerTable:5:innerTable:12:input", "form:outerTable:innerTable:input"),
            arguments("form:input:button", "form:input:button"),
            arguments("form:table:99:", "form:table:"),
            arguments("0:table:input", "table:input"),
            arguments("form:table:123456:input", "form:table:input"),
            arguments("", ""),
            arguments("input", "input"),
            arguments("form1:table2:input3", "form1:table2:input3"),
            arguments("form1:table2:5:input3", "form1:table2:input3"),
            arguments("form1:outerTable2:7:innerTable3:99:input4", "form1:outerTable2:innerTable3:input4"),
            arguments("form:component123:button", "form:component123:button"),
            arguments("1form:2table:3:button4", "1form:2table:button4"),
            arguments("form:table123abc:0:input", "form:table123abc:input"),
            arguments("myForm1:dataTable2:15:column3:nested4:42:outputText5", "myForm1:dataTable2:column3:nested4:outputText5"),
            arguments("123form:456table:7:input", "123form:456table:input")
        );
    }

    /**
     * Only a path segment which consists entirely of digits is an iteration index, so digits anywhere within a component name must be left alone.
     */
    @ParameterizedTest(name = "[{index}] \"{0}\" is stripped to \"{1}\"")
    @MethodSource("clientIds")
    void testStripIterationIndex(String clientId, String expected) {
        assertEquals(expected, ComponentsLocal.stripIterationIndex(mockedFacesContext, clientId));
    }

}
