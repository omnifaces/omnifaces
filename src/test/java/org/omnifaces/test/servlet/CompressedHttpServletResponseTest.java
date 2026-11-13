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
package org.omnifaces.test.servlet;

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.omnifaces.servlet.CompressedHttpServletResponse;
import org.omnifaces.servlet.CompressedHttpServletResponse.Algorithm;

class CompressedHttpServletResponseTest {

    @Test
    void testCreateOutputStreamRethrowsIOExceptionFromGetOutputStream() throws Exception {
        var response = mock(HttpServletResponse.class);
        var ioe = new IOException("test");
        when(response.getOutputStream()).thenThrow(ioe);

        var thrown = assertThrows(IOException.class, () -> Algorithm.GZIP.createOutputStream(response));
        assertSame(ioe, thrown);
    }

    @Test
    void testCreateOutputStreamRethrowsIOExceptionFromNewInstance() throws Exception {
        var ioe = new IOException("test");
        var brokenStream = new ServletOutputStream() {

            @Override
            public void write(int b) throws IOException {
                throw ioe;
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                throw ioe;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
                // NOOP.
            }

        };

        var response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(brokenStream);

        var thrown = assertThrows(IOException.class, () -> Algorithm.GZIP.createOutputStream(response));
        assertSame(ioe, thrown);
    }

    @Test
    void testVaryAcceptEncodingIsAddedWhenAbsent() throws Exception {
        assertEquals(List.of("Accept-Encoding"), compressAndGetVary(response -> {
        }, response -> {
        }));
    }

    @Test
    void testVaryAcceptEncodingIsAddedToVarySetOnWrapper() throws Exception {
        assertEquals(List.of("Cookie", "Accept-Encoding"), compressAndGetVary(response -> {
        }, response -> response.setHeader("Vary", "Cookie")));
    }

    @Test
    void testVaryAcceptEncodingIsAddedToVarySetBeforeWrapping() throws Exception {
        assertEquals(List.of("Cookie", "Accept-Encoding"), compressAndGetVary(response -> response.setHeader("Vary", "Cookie"), response -> {
        }));
    }

    @Test
    void testVaryAcceptEncodingIsNotDuplicated() throws Exception {
        assertEquals(List.of("Cookie, accept-encoding"), compressAndGetVary(response -> {
        }, response -> response.setHeader("Vary", "Cookie, accept-encoding")));
    }

    @Test
    void testVaryAcceptEncodingOnSeparateLineIsNotDuplicated() throws Exception {
        assertEquals(List.of("Cookie", "Accept-Encoding"), compressAndGetVary(response -> {
        }, response -> {
            response.addHeader("Vary", "Cookie");
            response.addHeader("Vary", "Accept-Encoding");
        }));
    }

    @Test
    void testVaryStarSetOnWrapperIsKept() throws Exception {
        assertEquals(List.of("*"), compressAndGetVary(response -> {
        }, response -> response.setHeader("Vary", "*")));
    }

    @Test
    void testVaryStarSetBeforeWrappingIsKept() throws Exception {
        assertEquals(List.of("*"), compressAndGetVary(response -> response.setHeader("Vary", "*"), response -> {
        }));
    }

    @Test
    void testVaryContainingStarIsKept() throws Exception {
        assertEquals(List.of("*", "Cookie"), compressAndGetVary(response -> {
        }, response -> {
            response.addHeader("Vary", "*");
            response.addHeader("Vary", "Cookie");
        }));
    }

    /**
     * Compresses a response whose headers are kept the way a servlet container keeps them, and returns its Vary header values.
     */
    private static List<String> compressAndGetVary(Consumer<HttpServletResponse> beforeWrapping, Consumer<HttpServletResponse> afterWrapping) throws Exception {
        Map<String, List<String>> headers = new TreeMap<>(CASE_INSENSITIVE_ORDER);
        var response = mock(HttpServletResponse.class);
        var outputStream = mock(ServletOutputStream.class);
        doAnswer(invocation -> headers.put(invocation.getArgument(0), new ArrayList<>(List.of((String) invocation.getArgument(1)))))
            .when(response).setHeader(anyString(), anyString());
        doAnswer(invocation -> headers.computeIfAbsent(invocation.getArgument(0), name -> new ArrayList<>()).add(invocation.getArgument(1)))
            .when(response).addHeader(anyString(), anyString());
        when(response.getHeaders(anyString())).thenAnswer(invocation -> headers.getOrDefault(invocation.getArgument(0), List.of()));
        when(response.getContentType()).thenReturn("text/html");
        when(response.getBufferSize()).thenReturn(8192);
        when(response.getOutputStream()).thenReturn(outputStream);

        beforeWrapping.accept(response);
        var compressedResponse = new CompressedHttpServletResponse(response, Algorithm.GZIP, 1, Set.of("text/html"));
        afterWrapping.accept(compressedResponse);
        compressedResponse.getOutputStream().write("compressible".getBytes());
        compressedResponse.close();

        assertEquals(List.of("gzip"), headers.get("Content-Encoding"));
        return headers.getOrDefault("Vary", List.of());
    }

}
