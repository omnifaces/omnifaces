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
package org.omnifaces.exceptionhandler;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.omnifaces.exceptionhandler.FullAjaxExceptionHandler.parseExceptionTypesParam;
import static org.omnifaces.test.WebappClassLoaders.createWebappClassLoader;

import java.nio.file.Path;

import jakarta.servlet.ServletContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Exception types in context parameters usually refer to application classes, which are not necessarily visible to the class loader of OmniFaces itself, such
 * as in Quarkus dev mode.
 */
class FullAjaxExceptionHandlerTest {

    private static final String PARAM_NAME = "org.omnifaces.EXCEPTION_TYPES_TO_UNWRAP";
    private static final String EXCEPTION_CLASS_NAME = "com.example.WebappException";
    private static final String EXCEPTION_SOURCE = "package com.example; public class WebappException extends RuntimeException {}";

    @TempDir
    Path directory;

    private ClassLoader originalClassLoader;

    @BeforeEach
    void setUp() {
        originalClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @AfterEach
    void tearDown() {
        Thread.currentThread().setContextClassLoader(originalClassLoader);
    }

    @Test
    void exceptionTypeVisibleToContextClassLoaderOnlyIsParsed() throws Exception {
        var webappClassLoader = createWebappClassLoader(directory, EXCEPTION_CLASS_NAME, EXCEPTION_SOURCE);
        Thread.currentThread().setContextClassLoader(webappClassLoader);

        var types = parseExceptionTypesParam(createServletContext(EXCEPTION_CLASS_NAME), PARAM_NAME, null);

        assertArrayEquals(new Class<?>[] { webappClassLoader.loadClass(EXCEPTION_CLASS_NAME) }, types);
    }

    @Test
    void unknownExceptionTypeIsRejected() {
        var servletContext = createServletContext(EXCEPTION_CLASS_NAME);

        assertThrows(IllegalArgumentException.class, () -> parseExceptionTypesParam(servletContext, PARAM_NAME, null));
    }

    private static ServletContext createServletContext(String exceptionTypes) {
        var servletContext = mock(ServletContext.class);
        when(servletContext.getInitParameter(PARAM_NAME)).thenReturn(exceptionTypes);
        return servletContext;
    }

}
