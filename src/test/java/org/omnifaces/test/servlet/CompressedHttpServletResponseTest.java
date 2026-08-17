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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
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

}
