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
package org.omnifaces.test.resourcehandler.combinedresourcehandler;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.omnifaces.resourcehandler.CombinedResourceInputStream;
import org.omnifaces.util.Faces;

import jakarta.faces.application.Resource;

@ExtendWith(MockitoExtension.class)
class CombinedResourceInputStreamTest {

    @Mock
    private Resource resource1;

    @Mock
    private Resource resource2;

    @Test
    void testUseStrictWithDoubleQuotesIsStripped() throws IOException {
        assertCombinedOutput(
            "\"use strict\";\nvar a = 1;\n",
            "var b = 2;\n",
            "var a = 1;\n\r\nvar b = 2;\n\r\n"
        );
    }

    @Test
    void testUseStrictWithSingleQuotesIsStripped() throws IOException {
        assertCombinedOutput(
            "'use strict';\nvar a = 1;\n",
            "var b = 2;\n",
            "var a = 1;\n\r\nvar b = 2;\n\r\n"
        );
    }

    @Test
    void testUseStrictInBothResourcesIsStripped() throws IOException {
        assertCombinedOutput(
            "\"use strict\";\nvar a = 1;\n",
            "\"use strict\";\nvar b = 2;\n",
            "var a = 1;\n\r\nvar b = 2;\n\r\n"
        );
    }

    @Test
    void testUseStrictWithLeadingWhitespaceIsStripped() throws IOException {
        assertCombinedOutput(
            "  \t\"use strict\";\nvar a = 1;\n",
            "var b = 2;\n",
            "  \tvar a = 1;\n\r\nvar b = 2;\n\r\n"
        );
    }

    @Test
    void testUseStrictWithLeadingNewlineIsStripped() throws IOException {
        assertCombinedOutput(
            "\n\"use strict\";\nvar a = 1;\n",
            "var b = 2;\n",
            "\nvar a = 1;\n\r\nvar b = 2;\n\r\n"
        );
    }

    @Test
    void testNonUseStrictQuotedStringIsPreserved() throws IOException {
        assertCombinedOutput(
            "\"not strict\";\nvar a = 1;\n",
            "var b = 2;\n",
            "\"not strict\";\nvar a = 1;\n\r\nvar b = 2;\n\r\n"
        );
    }

    @Test
    void testNoUseStrictIsUnchanged() throws IOException {
        assertCombinedOutput(
            "var a = 1;\n",
            "var b = 2;\n",
            "var a = 1;\n\r\nvar b = 2;\n\r\n"
        );
    }

    @Test
    void testCssContentTypeDoesNotStrip() throws IOException {
        when(resource1.getInputStream()).thenReturn(toResourceStream("\"use strict\";\nbody {}\n"));
        when(resource2.getInputStream()).thenReturn(toResourceStream("div {}\n"));

        Set<Resource> resources = new LinkedHashSet<>();
        resources.add(resource1);
        resources.add(resource2);

        try (var mockedFaces = mockStatic(Faces.class)) {
            mockedFaces.when(Faces::getRequestDomainURL).thenReturn("http://localhost");

            try (var stream = new CombinedResourceInputStream(resources, "text/css")) {
                assertEquals("\"use strict\";\nbody {}\n\r\ndiv {}\n\r\n", new String(stream.readAllBytes(), UTF_8));
            }
        }
    }

    @Test
    void testUseStrictWithoutSemicolonAndNewline() throws IOException {
        assertCombinedOutput(
            "\"use strict\"\nvar a = 1;\n",
            "var b = 2;\n",
            "var a = 1;\n\r\nvar b = 2;\n\r\n"
        );
    }

    @Test
    void testUseStrictWithSemicolonAndSpacesBeforeNewline() throws IOException {
        assertCombinedOutput(
            "\"use strict\" ;  \nvar a = 1;\n",
            "var b = 2;\n",
            "var a = 1;\n\r\nvar b = 2;\n\r\n"
        );
    }

    @Test
    void testBulkReadDuringPreamble() throws IOException {
        when(resource1.getInputStream()).thenReturn(toResourceStream("\"use strict\";\nvar a = 1;\n"));
        when(resource2.getInputStream()).thenReturn(toResourceStream("var b = 2;\n"));

        Set<Resource> resources = new LinkedHashSet<>();
        resources.add(resource1);
        resources.add(resource2);

        try (var mockedFaces = mockStatic(Faces.class)) {
            mockedFaces.when(Faces::getRequestDomainURL).thenReturn("http://localhost");

            try (var stream = new CombinedResourceInputStream(resources, "application/javascript")) {
                byte[] buf = new byte[4096];
                var result = new StringBuilder();
                int read;

                while ((read = stream.read(buf, 0, buf.length)) != -1) {
                    result.append(new String(buf, 0, read, UTF_8));
                }

                assertEquals("var a = 1;\n\r\nvar b = 2;\n\r\n", result.toString());
            }
        }
    }

    @Test
    void testEmptyResource() throws IOException {
        assertCombinedOutput(
            "",
            "var b = 2;\n",
            "\r\nvar b = 2;\n\r\n"
        );
    }

    private void assertCombinedOutput(String content1, String content2, String expected) throws IOException {
        when(resource1.getInputStream()).thenReturn(toResourceStream(content1));
        when(resource2.getInputStream()).thenReturn(toResourceStream(content2));

        Set<Resource> resources = new LinkedHashSet<>();
        resources.add(resource1);
        resources.add(resource2);

        try (var mockedFaces = mockStatic(Faces.class)) {
            mockedFaces.when(Faces::getRequestDomainURL).thenReturn("http://localhost");

            try (var stream = new CombinedResourceInputStream(resources, "application/javascript")) {
                assertEquals(expected, new String(stream.readAllBytes(), UTF_8));
            }
        }
    }

    /**
     * Wraps content in a {@link FilterInputStream} so it is not a {@link ByteArrayInputStream}, which
     * {@link CombinedResourceInputStream} uses to distinguish CRLF separators from actual resource streams.
     */
    private static FilterInputStream toResourceStream(String content) {
        return new FilterInputStream(new ByteArrayInputStream(content.getBytes(UTF_8))) {};
    }
}
